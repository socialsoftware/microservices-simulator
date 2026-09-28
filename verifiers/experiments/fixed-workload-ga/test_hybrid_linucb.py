import copy
import math
import unittest

from allocator import ProgressFeatureSpace
from hybrid_linucb import HybridLinUcbPolicy, StructuralFeatureSpace


class SharedSpace:
    names = ['z0', 'z1']

    @staticmethod
    def vector(profile, progress):
        return profile['z']


class LocalSpace:
    names = ['x0', 'x1']

    @staticmethod
    def vector(profile, progress):
        return progress['x']


def solve(matrix, vector):
    size = len(vector)
    augmented = [list(matrix[row]) + [vector[row]] for row in range(size)]
    for column in range(size):
        pivot = max(range(column, size), key=lambda row: abs(augmented[row][column]))
        augmented[column], augmented[pivot] = augmented[pivot], augmented[column]
        scale = augmented[column][column]
        if abs(scale) < 1e-14:
            raise AssertionError('singular fixture')
        augmented[column] = [value / scale for value in augmented[column]]
        for row in range(size):
            if row == column:
                continue
            factor = augmented[row][column]
            augmented[row] = [value - factor * pivot_value for value, pivot_value in
                              zip(augmented[row], augmented[column])]
    return [row[-1] for row in augmented]


def inverse(matrix):
    size = len(matrix)
    columns = [solve(matrix, [1.0 if row == column else 0.0
                              for row in range(size)])
               for column in range(size)]
    return [[columns[column][row] for column in range(size)] for row in range(size)]


def dot(left, right):
    return math.fsum(a * b for a, b in zip(left, right))


def direct_block(observations, workload_ids, ridge=1.0):
    shared, local = 2, 2
    offsets = {workload_id: shared + index * local
               for index, workload_id in enumerate(sorted(workload_ids))}
    dimension = shared + len(workload_ids) * local
    matrix = [[ridge if row == column else 0.0 for column in range(dimension)]
              for row in range(dimension)]
    response = [0.0] * dimension
    for workload_id, z, x, reward in observations:
        vector = list(z) + [0.0] * (dimension - shared)
        offset = offsets[workload_id]
        vector[offset:offset + local] = x
        for row in range(dimension):
            response[row] += reward * vector[row]
            for column in range(dimension):
                matrix[row][column] += vector[row] * vector[column]
    return solve(matrix, response), inverse(matrix), offsets


class HybridLinUcbTest(unittest.TestCase):
    ids = ('a', 'b')

    def policy(self):
        return HybridLinUcbPolicy(SharedSpace(), LocalSpace(), self.ids,
                                  exploration=1.0, ridge=1.0)

    @staticmethod
    def context(workload_id, z, x):
        return {'workload': workload_id, 'shared': list(z), 'local': list(x)}

    def test_matches_direct_block_ridge_coefficients_mean_and_uncertainty(self):
        observations = [
            ('a', [1.0, 0.2], [1.0, 0.0], 2.0),
            ('b', [0.4, 1.0], [1.0, 0.5], 1.0),
            ('a', [0.8, 0.6], [1.0, 0.7], 3.0),
            ('b', [1.0, 0.3], [1.0, 1.0], 0.5)]
        policy = self.policy()
        for workload_id, z, x, reward in observations:
            self.assertTrue(policy.update(workload_id,
                                          self.context(workload_id, z, x), reward))

        theta, covariance, offsets = direct_block(observations, self.ids)
        for workload_id, z, x in [('a', [0.7, 0.9], [1.0, 0.3]),
                                  ('b', [0.2, 1.0], [1.0, 0.8])]:
            context = self.context(workload_id, z, x)
            index, estimate, uncertainty = policy._score(workload_id, context)
            vector = list(z) + [0.0] * 4
            offset = offsets[workload_id]
            vector[offset:offset + 2] = x
            expected_mean = dot(theta, vector)
            transformed = [dot(row, vector) for row in covariance]
            expected_uncertainty = math.sqrt(dot(vector, transformed))
            self.assertAlmostEqual(estimate, expected_mean, places=11)
            self.assertAlmostEqual(uncertainty, expected_uncertainty, places=11)
            self.assertAlmostEqual(index, expected_mean + expected_uncertainty, places=11)
            coefficients = policy.coefficients(workload_id)
            self.assertSequenceAlmostEqual(coefficients['shared'], theta[:2])
            self.assertSequenceAlmostEqual(coefficients['local'],
                                           theta[offset:offset + 2])

    def assertSequenceAlmostEqual(self, left, right):
        self.assertEqual(len(left), len(right))
        for actual, expected in zip(left, right):
            self.assertAlmostEqual(actual, expected, places=11)

    def test_null_reward_does_not_update_any_state(self):
        policy = self.policy()
        before = (copy.deepcopy(policy.shared_inverse),
                  copy.deepcopy(policy.shared_response),
                  copy.deepcopy(policy.shared_theta),
                  {name: (copy.deepcopy(state.inverse), copy.deepcopy(state.cross),
                          copy.deepcopy(state.response), state.updates)
                   for name, state in policy.locals.items()}, policy.updates)
        self.assertFalse(policy.update('a', self.context('a', [1, 0], [1, 0]), None))
        after = (policy.shared_inverse, policy.shared_response, policy.shared_theta,
                 {name: (state.inverse, state.cross, state.response, state.updates)
                  for name, state in policy.locals.items()}, policy.updates)
        self.assertEqual(after, before)

    def test_local_parameters_and_identity_are_workload_specific(self):
        policy = self.policy()
        context = self.context('a', [1, 0], [1, 1])
        policy.update('a', context, 4.0)
        self.assertEqual(policy.summary()['localUpdates'], {'a': 1, 'b': 0})
        self.assertNotEqual(policy.coefficients('a')['local'], [0.0, 0.0])
        self.assertEqual(policy.coefficients('b')['local'], [0.0, 0.0])
        with self.assertRaisesRegex(ValueError, 'identity mismatch'):
            policy.update('b', context, 1.0)

    def test_cold_start_is_deterministic(self):
        policy = self.policy()
        states = {workload_id: {'profile': {'z': [1.0, 0.0]},
                                'progress': {'x': [1.0, 0.0]}}
                  for workload_id in self.ids}
        choices = [policy.select({'b', 'a'}, states)[0] for _ in range(3)]
        self.assertEqual(choices, ['a', 'a', 'a'])

    def test_structural_space_preserves_original_structure_and_partitions_progress(self):
        profile = {'counts': {'sagas': 1, 'pairs': 0, 'interactions': 0, 'events': 0},
                   'groups': {'saga': ['saga:A'], 'pair': [],
                              'interaction': [], 'event': []}}
        space = StructuralFeatureSpace({'w': profile})
        self.assertIn('bias', space.names)
        self.assertIn('saga:A', space.names)
        self.assertFalse(any(name.startswith('progress:') for name in space.names))
        local = ProgressFeatureSpace()
        self.assertEqual(local.names[0], 'bias')
        self.assertTrue(all(name == 'bias' or name.startswith('progress:')
                            for name in local.names))


if __name__ == '__main__':
    unittest.main()
