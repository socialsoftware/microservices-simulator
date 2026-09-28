"""Checks for the three opt-in component ablations."""
import sys
import unittest
from pathlib import Path
import math

HERE = Path(__file__).resolve().parent
sys.path[:0] = [str(HERE), str(HERE.parent / 'fixed-workload-ga')]

from allocator import ProgressFeatureSpace
from policies import BiasFeatureSpace, LocalProgressLinUcbPolicy, policy


def state():
    return {'profile': {'counts': {'sagas': 1, 'pairs': 0, 'interactions': 0,
                                   'events': 0},
                        'groups': {'saga': ['saga:A'], 'pair': [],
                                   'interaction': [], 'event': []}},
            'progress': {'allocated': 0, 'known': 0, 'unknown': 0, 'positives': 0,
                         'scoreSum': 0.0, 'bestScore': None}}


class PolicyTests(unittest.TestCase):
    def test_local_update_does_not_train_another_workload(self):
        states = {'a': state(), 'b': state()}
        model = LocalProgressLinUcbPolicy(states)
        wid, _, context = model.select(states, states)
        self.assertEqual('a', wid)
        self.assertTrue(model.update('a', context, 2.0))
        self.assertEqual({'a': 1, 'b': 0}, model.summary()['localUpdates'])
        with self.assertRaises(ValueError):
            model.update('b', context, 2.0)
        self.assertEqual(0, model.models['b'].updates)

    def test_hybrid_ablation_keeps_local_progress_identical(self):
        states = {'a': state(), 'b': state()}
        h0 = policy('H0', states, 1)
        h1 = policy('H1', states, 1)
        self.assertEqual(list(BiasFeatureSpace.names), list(h0.shared_feature_space.names))
        self.assertEqual(ProgressFeatureSpace().names,
                         h0.local_feature_space.names)
        self.assertEqual(h0.local_feature_space.names, h1.local_feature_space.names)
        self.assertEqual(h0.context('a', states['a'])['local'],
                         h1.context('a', states['a'])['local'])
        self.assertGreater(len(h1.shared_feature_space.names),
                           len(h0.shared_feature_space.names))

    def test_structure_only_excludes_progress(self):
        states = {'a': state()}
        model = policy('S-shared', states, 1)
        original = model.feature_space.vector(states['a']['profile'], states['a']['progress'])
        states['a']['progress'].update({'allocated': 3, 'known': 3, 'positives': 2,
                                       'scoreSum': 4.0, 'bestScore': 3.0})
        self.assertEqual(original, model.feature_space.vector(
            states['a']['profile'], states['a']['progress']))

    def test_unit_structure_matches_progress_cold_start_scale(self):
        states = {'a': state(), 'b': state()}
        for arm in ('S-unit', 'SP-unit', 'H1-unit'):
            model = policy(arm, states, 1)
            space = model.shared_feature_space if arm == 'H1-unit' else model.feature_space
            vector = space.vector(states['a']['profile'], states['a']['progress'])
            self.assertAlmostEqual(1.0, math.sqrt(sum(v * v for v in vector)))

    def test_calibrated_comparisons_keep_bias_and_match_initial_bonus(self):
        states = {'a': state(), 'b': state()}
        models = {arm: policy(arm, states, 1)
                  for arm in ('P-shared', 'S-cal', 'SP-cal', 'H0', 'H1-cal')}
        for wid in states:
            choices = {arm: model.select({wid}, states)[1] for arm, model in models.items()}
            self.assertAlmostEqual(choices['P-shared']['uncertainty'],
                                   choices['S-cal']['uncertainty'])
            self.assertAlmostEqual(choices['P-shared']['uncertainty'],
                                   choices['SP-cal']['uncertainty'])
            self.assertAlmostEqual(choices['H0']['uncertainty'],
                                   choices['H1-cal']['uncertainty'])
            for arm in ('S-cal', 'SP-cal', 'H1-cal'):
                model = models[arm]
                space = model.shared_feature_space if arm == 'H1-cal' else model.feature_space
                self.assertEqual(1.0, space.vector(states[wid]['profile'],
                                                   states[wid]['progress'])[0])

    def test_observed_zero_trains_and_missing_score_does_not(self):
        states = {'a': state(), 'b': state()}
        for arm in ('P-shared', 'S-cal', 'SP-cal', 'P-local', 'H0', 'H1-cal'):
            model = policy(arm, states, 1)
            wid, _, context = model.select({'a', 'b'}, states)
            self.assertFalse(model.update(wid, context, None), arm)
            self.assertTrue(model.update(wid, context, 0.0), arm)
            self.assertEqual(1, model.summary()['modelUpdates'], arm)

    def test_shared_and_local_progress_agree_with_one_workload(self):
        states = {'a': state()}
        shared = policy('P-shared', states, 1)
        local = policy('P-local', states, 1)
        for reward in (0.0, 2.0, None, 1.0):
            _, shared_choice, shared_context = shared.select({'a'}, states)
            _, local_choice, local_context = local.select({'a'}, states)
            for key in ('estimate', 'uncertainty'):
                self.assertAlmostEqual(shared_choice[key], local_choice[key])
            shared.update('a', shared_context, reward)
            local.update('a', local_context, reward)
            progress = states['a']['progress']
            progress['allocated'] += 1
            if reward is None:
                progress['unknown'] += 1
            else:
                progress['known'] += 1
                progress['scoreSum'] += reward
                progress['bestScore'] = reward if progress['bestScore'] is None else max(
                    progress['bestScore'], reward)
                progress['positives'] += int(reward > 0)


if __name__ == '__main__':
    unittest.main()
