import math
import unittest

from seeded_ties import SeededTiePolicy


class Priorities:
    def select(self, active, states):
        assert len(active) == 1
        wid = next(iter(active))
        return wid, {'index': states[wid]}, ('context', wid)

    def update(self, wid, context, reward):
        assert context == ('context', wid)
        return reward is not None

    def summary(self):
        return {}


class SeededTiesTest(unittest.TestCase):
    def test_cold_ties_reproduce_and_do_not_favour_first_id(self):
        states = dict.fromkeys(('a', 'b', 'c'), 1.0)
        first = SeededTiePolicy(Priorities(), 29)
        second = SeededTiePolicy(Priorities(), 29)
        left = [first.select(set(states), states)[0] for _ in range(100)]
        right = [second.select(list(reversed(states)), states)[0] for _ in range(100)]
        self.assertEqual(left, right)
        self.assertEqual(set(left), set(states))
        counts = dict.fromkeys(states, 0)
        for seed in range(1000):
            counts[SeededTiePolicy(Priorities(), seed).select(set(states), states)[0]] += 1
        self.assertTrue(all(250 < count < 420 for count in counts.values()), counts)

    def test_unique_maximum_preserves_context_without_consuming_a_draw(self):
        policy = SeededTiePolicy(Priorities(), 11)
        selected, choice, context = policy.select({'a', 'b'}, {'a': 1, 'b': 2})
        self.assertEqual((selected, context, policy.draws), ('b', ('context', 'b'), 0))
        self.assertEqual(choice['tie']['candidates'], ['b'])
        self.assertFalse(policy.update(selected, context, None))
        self.assertTrue(policy.update(selected, context, 0))
        tied = dict.fromkeys(('a', 'b'), 1.0)
        self.assertEqual(policy.select(tied, tied), SeededTiePolicy(Priorities(), 11).select(tied, tied))

    def test_rounding_noise_is_a_tie_but_meaningful_priority_gap_is_not(self):
        policy = SeededTiePolicy(Priorities(), 1)
        _, choice, _ = policy.select({'a', 'b', 'c'},
            {'a': math.sqrt(2), 'b': math.nextafter(math.sqrt(2), math.inf), 'c': 1.4})
        self.assertEqual(choice['tie']['candidates'], ['a', 'b'])
        self.assertLessEqual(choice['tie']['selectedGap'], math.ulp(math.sqrt(2)))
        selected, _, _ = policy.select({'a', 'b'}, {'a': 1, 'b': 1 + 1e-8})
        self.assertEqual(selected, 'b')

    def test_invalid_priority_or_empty_selection_is_rejected(self):
        for value in [float('nan'), float('inf'), -float('inf')]:
            with self.assertRaises(ValueError):
                SeededTiePolicy(Priorities(), 1).select({'a'}, {'a': value})
        with self.assertRaises(ValueError):
            SeededTiePolicy(Priorities(), 1).select([], {})


if __name__ == '__main__':
    unittest.main()
