import copy
import unittest
from compare import classify
from run import matched


class ComparisonTest(unittest.TestCase):
    def result(self,status,event='COMPLETED',receiver='create-receiver'):
        return dict(comparable=True,targetStatus=status,eventStatus=event,receiverRole=receiver,publisherRole='create-publisher',invariantRejectionOnReceiver=status=='FAILED')

    def test_actual_delivery_contrast_is_separate_observation(self):
        self.assertEqual('ORDER_DEPENDENT_REJECTION',classify(self.result('COMPLETED'),self.result('FAILED')))

    def test_contrast_can_also_reject_the_early_order(self):
        self.assertEqual('ORDER_DEPENDENT_REJECTION',classify(self.result('FAILED'),self.result('COMPLETED')))

    def test_rejection_in_both_is_not_positive(self):
        self.assertEqual('REJECTED_IN_BOTH',classify(self.result('FAILED','NO_ELIGIBLE_SUBSCRIBER',None),self.result('FAILED','NO_ELIGIBLE_SUBSCRIBER',None)))

    def test_missing_or_failed_control_is_not_negative(self):
        self.assertEqual('NOT_COMPARABLE',classify({'comparable':False},self.result('FAILED')))

    def test_no_receiver_does_not_prove_delivery_related_rejection(self):
        self.assertEqual('NOT_COMPARABLE',classify(self.result('COMPLETED','NO_ELIGIBLE_SUBSCRIBER'),self.result('FAILED')))

    def test_unclassified_runtime_failure_is_not_an_application_rejection(self):
        failed=self.result('FAILED');failed['invariantRejectionOnReceiver']=False
        self.assertEqual('NOT_COMPARABLE',classify(self.result('COMPLETED'),failed))

    def test_other_receiver_cannot_stand_in_for_same_delivery(self):
        self.assertEqual('NOT_COMPARABLE',classify(self.result('COMPLETED',receiver='other'),self.result('FAILED')))

    def test_two_successes_have_no_rejection_contrast(self):
        self.assertEqual('NO_REJECTION_CONTRAST',classify(self.result('COMPLETED'),self.result('COMPLETED')))

    def workload(self):
        return dict(id='early',setup='same',participants=[{'input':'same'}],interactions=[],schedule=[
            {'id':'s1','kind':'step'},{'id':'e1','kind':'event','triggeringStep':'s1','route':'same'},
            {'id':'s2','kind':'step'},{'id':'s3','kind':'step'}])

    def test_only_event_movement_is_accepted(self):
        early=self.workload();late=copy.deepcopy(early);late['id']='late';event=late['schedule'].pop(1);late['schedule'].append(event)
        self.assertTrue(matched(early,late))
        for mutate in [lambda w:w.update(setup='other'),lambda w:w['participants'][0].update(input='other'),
                       lambda w:w['schedule'][-1].update(route='other'),lambda w:w['schedule'].reverse()]:
            altered=copy.deepcopy(late);mutate(altered)
            with self.assertRaises(ValueError):matched(early,altered)

    def test_identical_or_duplicate_action_schedules_are_rejected(self):
        early=self.workload()
        with self.assertRaises(ValueError):matched(early,copy.deepcopy(early))
        late=copy.deepcopy(early);late['schedule'].append(late['schedule'][1])
        with self.assertRaises(ValueError):matched(early,late)


if __name__=='__main__':unittest.main()
