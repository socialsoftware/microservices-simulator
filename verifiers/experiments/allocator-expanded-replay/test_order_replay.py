"""Contract checks for the paired order ablation, independent of benchmark scores."""
import copy
import math
import unittest

import order_replay as o
from policies import FixedBiasUnitStructureFeatureSpace
from transfer import _zero_progress


class OrderReplayTests(unittest.TestCase):
    def setUp(self):
        base = {'counts': {'sagas': 2, 'pairs': 1, 'interactions': 1, 'events': 0},
                'groups': {'sagas': ['saga:A', 'saga:B']},
                'orderFeatures': dict.fromkeys(o.order.ORDER_FEATURES, 0.0)}
        self.profiles = {'a': copy.deepcopy(base), 'b': copy.deepcopy(base)}
        self.profiles['a']['orderFeatures'][o.order.ORDER_FEATURES[0]] = .5
        self.profiles['b']['orderFeatures'][o.order.ORDER_FEATURES[1]] = .5
        self.states = {wid: {'profile': p, 'progress': _zero_progress()}
                       for wid, p in self.profiles.items()}

    def test_order_separates_identical_structure_without_larger_cold_bonus(self):
        s = FixedBiasUnitStructureFeatureSpace(self.profiles)
        so = o.FixedBiasUnitStructureOrderSpace(self.profiles)
        self.assertEqual(s.vector(self.profiles['a'], _zero_progress()),
                         s.vector(self.profiles['b'], _zero_progress()))
        self.assertNotEqual(so.vector(self.profiles['a'], _zero_progress()),
                            so.vector(self.profiles['b'], _zero_progress()))
        for arm, parent in o.PARENTS.items():
            model = o.factory(arm, self.states, 29)
            old = o.m.factory(parent, self.states, 29)
            for wid in self.states:
                choice = model.select({wid}, self.states)[1]
                previous = old.select({wid}, self.states)[1]
                self.assertAlmostEqual(choice['uncertainty'], previous['uncertainty'], places=12)

    def test_local_hybrid_progress_is_identical_after_nonzero_history(self):
        self.states['a']['progress'] = {'allocated': 13, 'known': 10, 'unknown': 3,
                                      'positives': 4, 'scoreSum': 29, 'bestScore': 17}
        new = o.factory('H1+O', self.states, 1).select({'a'}, self.states)[2]
        old = o.m.factory('H1-cal', self.states, 1).select({'a'}, self.states)[2]
        self.assertEqual(new['local'], old['local'])
        self.assertNotEqual(new['shared'], old['shared'])
        s = FixedBiasUnitStructureFeatureSpace(self.profiles, True)
        so = o.FixedBiasUnitStructureOrderSpace(self.profiles, True)
        self.assertEqual(s.vector(self.profiles['a'], self.states['a']['progress'])[-6:],
                         so.vector(self.profiles['a'], self.states['a']['progress'])[-6:])

    def test_absent_or_invalid_order_is_rejected_not_silently_zero(self):
        space = o.FixedBiasUnitStructureOrderSpace(self.profiles)
        invalid = copy.deepcopy(self.profiles['a'])
        del invalid['orderFeatures']
        with self.assertRaises(ValueError): space.vector(invalid, _zero_progress())
        for v in (math.nan, math.inf, -1, 1, True):
            invalid = copy.deepcopy(self.profiles['a'])
            invalid['orderFeatures'][o.order.ORDER_FEATURES[0]] = v
            with self.assertRaises(ValueError): space.vector(invalid, _zero_progress())

    def test_unknown_feedback_does_not_update_either_model(self):
        for arm in o.PARENTS:
            model = o.factory(arm, self.states, 1)
            wid, before, context = model.select({'a'}, self.states)
            summary = model.summary()
            self.assertFalse(model.update(wid, context, None))
            self.assertEqual(summary, model.summary())
            self.assertEqual(before, model.select({'a'}, self.states)[1])


if __name__ == '__main__': unittest.main()
