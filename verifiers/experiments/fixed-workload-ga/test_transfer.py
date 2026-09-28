from collections import defaultdict
import unittest

from allocator import allocate
from catalogue import RecordedDomain
from fitness import CRITERIA_V2, PERSISTENT, configuration
from search import candidate_key
from transfer import DEFAULT_CHECKPOINTS, run_transfer


FITNESS = configuration({'policy': 'weighted-criteria-v2',
                         'weights': {name: 1 for name in CRITERIA_V2}})


def profile(*sagas):
    sagas = sorted(sagas)
    pairs = [f'saga-pair:{left}|{right}' for index, left in enumerate(sagas)
             for right in sagas[index + 1:]]
    return {'counts': {'sagas': len(sagas), 'pairs': len(pairs),
                       'interactions': 0, 'events': 0},
            'groups': {'saga': ['saga:' + saga for saga in sagas],
                       'pair': pairs, 'interaction': [], 'event': []}}


class TrackingDomain(RecordedDomain):
    def __init__(self, workload, candidates, events):
        super().__init__(workload, candidates)
        self.events = events

    def sample_unseen(self, rng, seen):
        selected = super().sample_unseen(rng, seen)
        self.events.append(('choice', self.workload['id'], selected['key']))
        return selected


def workload(workload_id, count, sagas, events):
    participants = [{'id': f'p{index}', 'saga': saga, 'input': f'i{index}'}
                    for index, saga in enumerate(sagas, 1)]
    schedule = [{'id': f's{index}', 'kind': 'step', 'participant': row['id'],
                 'faultSlot': index - 1} for index, row in enumerate(participants, 1)]
    definition = {'id': workload_id, 'participants': participants,
                  'interactions': [], 'schedule': schedule}
    candidates = {}
    width = len(participants)
    for index in range(count):
        vector = format(index % (2 ** width), f'0{width}b')
        actions = [{'step': 's1'}, {'compensate': f'c{index}'}]
        key = candidate_key(workload_id, vector, actions)
        candidates[key] = {'id': f'{workload_id}-s{index}', 'workload': workload_id,
                           'faultVector': vector, 'actions': actions, 'key': key,
                           'aliases': [f'{workload_id}-s{index}']}
    return {'id': workload_id, 'name': workload_id,
            'domain': TrackingDomain(definition, candidates, events),
            'profile': profile(*sagas)}


def result(candidate, score):
    categories = [{'category': name,
                   'positiveObjectCount': score if name == PERSISTENT[0] else 0,
                   'coverageStatus': 'COMPLETE', 'unknownReasons': []}
                  for name in PERSISTENT]
    return {'candidate': candidate, 'status': 'COMPLETE',
            'terminalStatus': 'COMPENSATED', 'scheduleConformance': 'EXACT',
            'I': score, 'impactCategories': categories,
            'A': 0, 'AStatus': 'COMPLETE', 'ACoverage': 'COMPLETE_WITHIN_SCOPE',
            'AGaps': [], 'lostCopiedUpdateCount': 0,
            'lostCopiedUpdateValidity': 'COMPLETE',
            'lostCopiedUpdateCoverage': 'COMPLETE_WITHIN_SCOPE',
            'lostCopiedUpdateCoverageGaps': []}


class Evaluator:
    mode = 'SYNTHETIC_RECORDED_FEEDBACK'

    def __init__(self, scores, events):
        self.scores = scores
        self.events = events
        self.seen = set()

    def evaluate(self, workload_id, candidate, attempt):
        identity = (workload_id, candidate['key'])
        if identity in self.seen:
            raise AssertionError('candidate repeated within one evaluator phase')
        if not self.events or self.events[-1] != ('choice', workload_id, candidate['key']):
            raise AssertionError('feedback requested before the candidate choice')
        self.events.append(('feedback', workload_id, candidate['key']))
        self.seen.add(identity)
        return result(candidate, self.scores.get(identity, 0))


class Fixture:
    def __init__(self):
        self.events = []
        self.workloads = [
            workload('train-a', 8, ('shared',), self.events),
            workload('train-b', 8, ('source-only',), self.events),
            workload('test-a', 10, ('shared', 'target'), self.events),
            workload('test-b', 10, ('target-only',), self.events)]
        self.scores = {}
        for row in self.workloads:
            for index, key in enumerate(sorted(row['domain'].candidates)):
                self.scores[(row['id'], key)] = 3 if row['id'] == 'train-a' else index % 3
        self.evaluators = []

    def factory(self):
        evaluator = Evaluator(self.scores, self.events)
        self.evaluators.append(evaluator)
        return evaluator

    def run(self, seed=7):
        return run_transfer(self.workloads, self.factory,
                            ['train-a', 'train-b'], ['test-a', 'test-b'], seed,
                            train_budget=8, test_budget=8, checkpoints=(2, 5, 8))


def choices(result):
    return [(row['workload'], row['candidate']) for row in result['decisions']]


class TransferTest(unittest.TestCase):
    def test_one_round_robin_history_updates_both_warm_models(self):
        fixture = Fixture()
        output = fixture.run()
        training = output['training']
        self.assertEqual([row['workload'] for row in training['decisions']],
                         ['train-a', 'train-b'] * 4)
        for row in training['decisions']:
            self.assertEqual(row['preUpdateProgress']['allocated'], row['localAttempt'] - 1)
            self.assertEqual(row['preUpdateProgress']['known'], row['localAttempt'] - 1)
        self.assertEqual(output['trainingModelEvidence']['structural']['updates'], 8)
        self.assertEqual(output['trainingModelEvidence']['progress']['updates'], 8)
        self.assertEqual(len(fixture.evaluators), 5)
        self.assertEqual({workload_id for workload_id, _ in fixture.evaluators[0].seen},
                         {'train-a', 'train-b'})
        self.assertTrue(all({workload_id for workload_id, _ in evaluator.seen}
                            <= {'test-a', 'test-b'}
                            for evaluator in fixture.evaluators[1:]))
        self.assertTrue(all(arm['commonTrainingHistorySha256'] == training['historySha256']
                            for arm in output['targetResults']))

    def test_only_model_state_crosses_into_warm_fresh_target_arms(self):
        output = Fixture().run()
        arms = {row['arm']: row for row in output['targetResults']}
        for arm in arms.values():
            self.assertTrue(arm['targetStart']['allProgressZero'])
            self.assertEqual((arm['targetStart']['totalAttempts'],
                              arm['targetStart']['totalSeen'],
                              arm['targetStart']['totalParents']), (0, 0, 0))
        self.assertEqual(arms['structural-warm']['transferredState'],
                         ['inverse', 'response', 'theta', 'updates'])
        self.assertEqual(arms['progress-warm']['trainingModelUpdates'], 8)
        self.assertEqual(arms['structural-cold']['trainingModelUpdates'], 0)
        self.assertEqual(arms['progress-cold']['trainingModelUpdates'], 0)
        self.assertNotEqual(arms['structural-warm']['modelStart']['stateSha256'],
                            arms['structural-cold']['modelStart']['stateSha256'])
        self.assertEqual(len({arm['targetStart']['stateSha256'] for arm in arms.values()}), 1)
        for arm in arms.values():
            self.assertEqual(arm['totalModelUpdates'],
                             arm['trainingModelUpdates'] + arm['targetModelUpdates'])

    def test_split_and_feature_vocabulary_are_outcome_free_and_frozen_together(self):
        output = Fixture().run()
        self.assertTrue(output['split']['disjoint'])
        self.assertFalse(output['inputEvidence']['outcomesUsedForFeatures'])
        structural = output['featureSpaces']['structural']
        self.assertIn('saga:source-only', structural['names'])
        self.assertIn('saga:target-only', structural['names'])
        self.assertEqual(structural['profileIds'],
                         ['test-a', 'test-b', 'train-a', 'train-b'])
        self.assertEqual(output['featureSpaces']['progress']['dimension'], 7)
        self.assertEqual(len(output['featureSpaces']['progress']['names']), 7)

    def test_cold_arms_match_regular_allocator_with_combined_vocabulary(self):
        fixture = Fixture()
        output = fixture.run(seed=11)
        arms = {row['arm']: row for row in output['targetResults']}
        tests = [row for row in fixture.workloads if row['id'].startswith('test-')]
        for family, policy in [('structural', 'contextual-linucb'),
                               ('progress', 'progress-linucb')]:
            regular = allocate(tests, fixture.factory(), policy=policy,
                               parameters={'exploration': 1.0, 'ridge': 1.0},
                               seed=11, budget=8, fitness=FITNESS)
            self.assertEqual(choices(arms[f'{family}-cold']), choices(regular))

    def test_candidates_are_unique_and_seeded_local_sequences_have_common_prefixes(self):
        output = Fixture().run(seed=13)
        phases = [output['training'], *output['targetResults']]
        for phase in phases:
            identities = [(row['workload'], row['candidate']) for row in phase['decisions']]
            self.assertEqual(len(identities), len(set(identities)))
        by_arm = {}
        for arm in output['targetResults']:
            grouped = defaultdict(list)
            for row in arm['decisions']:
                grouped[row['workload']].append(row['candidate'])
            by_arm[arm['arm']] = grouped
        for workload_id in ('test-a', 'test-b'):
            sequences = [grouped[workload_id] for grouped in by_arm.values()]
            for left in sequences:
                for right in sequences:
                    shared = min(len(left), len(right))
                    self.assertEqual(left[:shared], right[:shared])

    def test_feedback_is_requested_only_after_each_choice(self):
        fixture = Fixture()
        fixture.run()
        self.assertEqual(len(fixture.events) % 2, 0)
        for index in range(0, len(fixture.events), 2):
            chosen, feedback = fixture.events[index:index + 2]
            self.assertEqual(chosen[0], 'choice')
            self.assertEqual(feedback[0], 'feedback')
            self.assertEqual(chosen[1:], feedback[1:])

    def test_initial_ranking_and_compact_model_evidence_are_reported(self):
        output = Fixture().run()
        self.assertEqual(output['training']['phase'], 'training')
        for arm in output['targetResults']:
            self.assertEqual(arm['phase'], 'target')
            self.assertEqual([row['rank'] for row in arm['initialRanking']], [1, 2])
            self.assertEqual(arm['initialTopChoice'], arm['initialRanking'][0]['workload'])
            self.assertNotIn('inverse', arm['modelStart'])
            self.assertNotIn('theta', arm['decisions'][0])
        self.assertEqual(output['protocol']['checkpoints'], [2, 5, 8])
        self.assertEqual(DEFAULT_CHECKPOINTS, (16, 32, 64, 128, 256))

    def test_rejects_overlap_and_out_of_range_checkpoints(self):
        fixture = Fixture()
        with self.assertRaises(ValueError):
            run_transfer(fixture.workloads, fixture.factory, ['train-a'], ['train-a'], 1,
                         train_budget=1, test_budget=1, checkpoints=(1,))
        with self.assertRaises(ValueError):
            run_transfer(fixture.workloads, fixture.factory, ['train-a'], ['test-a'], 1,
                         train_budget=1, test_budget=1, checkpoints=(2,))


if __name__ == '__main__':
    unittest.main()
