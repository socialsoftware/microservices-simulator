import unittest
from study import run_arm
from test_transfer import Fixture
from test_allocator import complete_result


class StudyTest(unittest.TestCase):
    def factory(self, fixture, unknown=False):
        def create():
            evaluator=fixture.factory()
            evaluator.revealed=evaluator.seen
            if unknown:
                original=evaluator.evaluate
                def evaluate(wid,candidate,attempt):
                    original(wid,candidate,attempt)
                    return complete_result(candidate,unknown=True)
                evaluator.evaluate=evaluate
            return evaluator
        return create

    def test_matched_fresh_sessions_and_exhaustion(self):
        f=Fixture()
        starts=[]
        for arm in ('round-robin','uniform','independent','progress','structural'):
            result=run_arm(f.workloads,self.factory(f),arm,7,100,[8,20,36])
            self.assertEqual(result['attempts'],36)
            self.assertEqual(len({(r['workload'],r['candidate']) for r in result['decisions']}),36)
            self.assertTrue(result['start']['allProgressZero'])
            self.assertEqual(result['unknowns'],0)
            starts.append(result['start']['stateSha256'])
        self.assertEqual(len(set(starts)),1)

    def test_unknowns_consume_budget_without_learning(self):
        f=Fixture()
        for arm in ('independent','progress','structural'):
            result=run_arm(f.workloads,self.factory(f,True),arm,3,8,[8])
            self.assertEqual((result['attempts'],result['unknowns'],result['positives']), (8,8,0))
            self.assertTrue(all(not d['modelUpdated'] for d in result['decisions']))
            self.assertTrue(all(d['score'] is None for d in result['decisions']))
            self.assertTrue(all(w['populationSize']==0 for w in result['perWorkload']))

    def test_observed_progress_is_previous_not_current(self):
        f=Fixture()
        result=run_arm(f.workloads,self.factory(f),'structural',2,8,[8])
        counts={}
        for row in result['decisions']:
            wid=row['workload']
            self.assertEqual(row['preUpdateProgress']['allocated'],counts.get(wid,0))
            counts[wid]=counts.get(wid,0)+1


if __name__=='__main__': unittest.main()
