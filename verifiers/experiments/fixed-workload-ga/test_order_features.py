import json
from pathlib import Path
import tempfile
import unittest

from order_features import (ORDER_FEATURES, OrderFeatureCoverageError, OrderFeatureSpace,
                            augment_profile,
                            load_compatible_structural_metadata, order_profile)


def access(name, mode):
    return {'command': mode.title(), 'aggregate': {
        'name': name, 'mode': mode,
        'keyEvidence': {'kind': 'symbolic', 'value': 'same-id'}}}


def saga(name, step, mode, *, routes=(), limitations=()):
    return {'fqn': name, 'dependencies': [], 'steps': [{
        'id': step, 'commandAccesses': [access('Thing', mode)],
        'compensation': {'kind': 'none'}, 'eventRoutes': list(routes),
        'analysisLimitations': list(limitations)}]}


def interaction(evidence='symbolic'):
    return {'id': 'i', 'evidence': evidence, 'accesses': [
        {'saga': 'A', 'step': 'a#0', 'aggregate': 'Thing', 'mode': 'read',
         'keyEvidence': {'kind': 'symbolic', 'value': 'same-id'}},
        {'saga': 'B', 'step': 'b#0', 'aggregate': 'Thing', 'mode': 'write',
         'keyEvidence': {'kind': 'symbolic', 'value': 'same-id'}}]}


def workload(entries, interactions=('i',)):
    return {'id': 'w', 'participants': [
        {'id': 'pa', 'saga': 'A', 'input': 'a'},
        {'id': 'pb', 'saga': 'B', 'input': 'b'}],
        'interactions': list(interactions), 'schedule': entries}


def step(identity, participant, saga_step):
    return {'id': identity, 'kind': 'step', 'participant': participant,
            'sagaStep': saga_step, 'faultSlot': 0}


class OrderFeatureTest(unittest.TestCase):
    def setUp(self):
        self.metadata = {'sagas': [saga('A', 'a#0', 'read'), saga('B', 'b#0', 'write')],
                         'interactions': [interaction()]}

    def test_identical_structure_with_different_order_has_different_features(self):
        read_first = order_profile(workload([
            step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')]), self.metadata)
        write_first = order_profile(workload([
            step('s1', 'pb', 'b#0'), step('s2', 'pa', 'a#0')]), self.metadata)
        self.assertEqual(read_first['features'][
            'order:type-potential:read-before-write'], 0.5)
        self.assertEqual(write_first['features'][
            'order:type-potential:write-before-read'], 0.5)
        base = {'counts': {'sagas': 2, 'pairs': 1, 'interactions': 1, 'events': 0},
                'groups': {'saga': ['saga:A', 'saga:B'], 'pair': ['saga-pair:A|B'],
                           'interaction': ['interaction:same'], 'event': []}}
        left = augment_profile(base, read_first)
        right = augment_profile(base, write_first)
        space = OrderFeatureSpace({'left': left, 'right': right})
        progress = {'allocated': 0, 'known': 0, 'unknown': 0, 'positives': 0,
                    'scoreSum': 0.0, 'bestScore': None}
        self.assertNotEqual(space.vector(left, progress), space.vector(right, progress))

    def test_same_aggregate_type_creates_only_a_labelled_type_level_potential(self):
        profile = order_profile(workload([
            step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')], interactions=()),
            self.metadata)
        self.assertEqual(profile['features'][
            'order:type-potential:read-before-write'], 0.5)
        self.assertFalse(profile['coverage']['objectIdentityClaim'])
        self.assertEqual(profile['coverage']['semantics'],
                         'aggregate-type-potential-only')

    def test_different_aggregate_types_do_not_create_a_potential_pair(self):
        left = saga('A', 'a#0', 'read')
        right = saga('B', 'b#0', 'write')
        right['steps'][0]['commandAccesses'][0]['aggregate']['name'] = 'OtherThing'
        profile = order_profile(workload([
            step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')], interactions=()),
            {'sagas': [left, right], 'interactions': []})
        self.assertTrue(all(value == 0.0 for value in profile['features'].values()))

    def test_interaction_evidence_does_not_gate_scheduled_access_context(self):
        metadata = {**self.metadata, 'interactions': [interaction('typeOnly')]}
        profile = order_profile(workload([
            step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')]), metadata)
        self.assertEqual(profile['features'][
            'order:type-potential:read-before-write'], 0.5)

    def test_event_position_uses_mapped_receiver_mode_without_object_join(self):
        route = {'id': 'a#0/event#0', 'event': 'Changed', 'downstreamSaga': 'C'}
        metadata = {'sagas': [saga('A', 'a#0', 'read', routes=(route,)),
                              saga('B', 'b#0', 'write'), saga('C', 'c#0', 'write')],
                    'interactions': [interaction()]}
        plan = workload([step('s1', 'pa', 'a#0'),
                         {'id': 'e1', 'kind': 'event', 'route': 'a#0/event#0',
                          'triggeringStep': 's1'},
                         step('s2', 'pb', 'b#0')])
        profile = order_profile(plan, metadata)
        self.assertEqual(profile['features'][
            'order:event-type-potential:read-before-write'], 0.5)
        self.assertEqual(profile['features'][
            'order:event-type-potential:write-before-write'], 0.5)
        self.assertFalse(profile['coverage']['objectIdentityClaim'])

    def test_event_receiver_must_resolve_before_event_features_are_available(self):
        route = {'id': 'a#0/event#0', 'event': 'Changed', 'downstreamSaga': 'missing'}
        metadata = {'sagas': [saga('A', 'a#0', 'read', routes=(route,)),
                              saga('B', 'b#0', 'write')], 'interactions': []}
        plan = workload([step('s1', 'pa', 'a#0'),
                         {'id': 'e1', 'kind': 'event', 'route': 'a#0/event#0',
                          'triggeringStep': 's1'},
                         step('s2', 'pb', 'b#0')], interactions=())
        with self.assertRaises(OrderFeatureCoverageError):
            order_profile(plan, metadata)
        partial = order_profile(plan, metadata, strict=False)
        self.assertEqual(partial['counts']['mappedEvents'], 0)
        self.assertTrue(any(row['kind'] == 'EVENT_RECEIVER_SAGA_NOT_FOUND'
                            for row in partial['coverage']['gaps']))

    def test_missing_access_provenance_fails_strict_and_is_visible_in_coverage(self):
        missing = saga('A', 'a#0', 'read')
        del missing['steps'][0]['commandAccesses']
        broken = {'sagas': [missing, saga('B', 'b#0', 'write')],
                  'interactions': [interaction()]}
        plan = workload([step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')])
        with self.assertRaises(OrderFeatureCoverageError):
            order_profile(plan, broken)
        partial = order_profile(plan, broken, strict=False)
        self.assertEqual(partial['coverage']['status'], 'PARTIAL')
        self.assertTrue(any(row['kind'] == 'MISSING_COMMAND_ACCESSES'
                            for row in partial['coverage']['gaps']))
        with self.assertRaises(OrderFeatureCoverageError):
            augment_profile({'counts': {}, 'groups': {}}, partial)
        self.assertEqual(set(augment_profile({'counts': {}, 'groups': {}}, partial,
                                             allow_partial=True)['orderFeatures']),
                         set(ORDER_FEATURES))

    def test_known_analysis_limitation_is_audited_without_hiding_proven_access(self):
        limited = {'sagas': [saga('A', 'a#0', 'read', limitations=('UNRESOLVED',)),
                             saga('B', 'b#0', 'write')],
                   'interactions': [interaction()]}
        profile = order_profile(workload([
            step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0')]), limited)
        self.assertEqual(profile['coverage']['status'], 'COMPLETE')
        self.assertEqual(profile['coverage']['staticKnowledge'], 'PARTIAL')
        self.assertTrue(profile['coverage']['limitations'])
        self.assertEqual(profile['features'][
            'order:type-potential:read-before-write'], 0.5)

    def test_counts_are_bounded_and_read_between_foreign_writes_is_explicit(self):
        metadata = {'sagas': [saga('A', 'a#0', 'write'),
                              saga('B', 'b#0', 'read'),
                              saga('C', 'c#0', 'write')], 'interactions': []}
        plan = {'id': 'w', 'participants': [
            {'id': 'pa', 'saga': 'A'}, {'id': 'pb', 'saga': 'B'},
            {'id': 'pc', 'saga': 'C'}], 'interactions': [], 'schedule': [
                step('s1', 'pa', 'a#0'), step('s2', 'pb', 'b#0'),
                step('s3', 'pc', 'c#0')]}
        profile = order_profile(plan, metadata)
        name = 'order:type-potential:read-between-foreign-writes'
        self.assertEqual(profile['counts']['rawPatterns'][name], 1)
        self.assertEqual(profile['features'][name], 0.5)
        self.assertTrue(all(0.0 <= value < 1.0 for value in profile['features'].values()))


class MetadataCompatibilityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)

    def tearDown(self):
        self.temp.cleanup()

    @staticmethod
    def sha(path):
        import hashlib
        return hashlib.sha256(path.read_bytes()).hexdigest()

    def package(self, name, saga_name='A'):
        root = self.root / name
        root.mkdir()
        sagas = root / 'sagas.jsonl'
        interactions = root / 'interactions.jsonl'
        sagas.write_text(json.dumps(saga(saga_name, 'a#0', 'read')) + '\n')
        interactions.write_text(json.dumps(interaction()) + '\n')
        manifest = {'formatVersion': 1, 'files': {
            'sagas': {'path': sagas.name, 'sha256': self.sha(sagas)},
            'interactions': {'path': interactions.name,
                             'sha256': self.sha(interactions)}}}
        (root / 'scenario-catalog-manifest.json').write_text(json.dumps(manifest))
        return interactions

    def test_loads_only_exactly_compatible_package_roles(self):
        left = self.package('left')
        right = self.package('right')
        metadata = load_compatible_structural_metadata([left, right])
        self.assertEqual(metadata['source']['compatibility'], 'EXACT_PACKAGE_ROLE_HASHES')
        self.assertEqual(len(metadata['sagas']), 1)

    def test_rejects_different_saga_source_even_with_same_interactions(self):
        left = self.package('left')
        right = self.package('right', saga_name='different')
        with self.assertRaisesRegex(ValueError, 'incompatible'):
            load_compatible_structural_metadata([left, right])


if __name__ == '__main__':
    unittest.main()
