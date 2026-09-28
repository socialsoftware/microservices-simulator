"""Boundary tests for artifact interpretation, launch isolation and loopback API."""
import gzip
import json
from pathlib import Path
import subprocess
import sys
import tempfile
import threading
import time
import unittest
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from data import Catalog, CRITERIA, identifier, journal, normalize
from server import HERE, Jobs, Server
from worker import save


class WorkbenchTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.repo = Path(self.tmp.name)
        self.root = self.repo / 'verifiers/target'
        self.root.mkdir(parents=True)
        self.catalog = Catalog(self.repo)

    def tearDown(self):
        self.tmp.cleanup()

    def artifact(self, name, value):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        if path.suffix == '.gz':
            with gzip.open(path, 'wt') as stream:
                json.dump(value, stream)
        else:
            path.write_text(json.dumps(value))
        return path

    def config(self):
        self.artifact('prepared/map.json', {})
        self.artifact('prepared/reference.json', {})
        (self.root / 'prepared/catalogue').mkdir(exist_ok=True)
        raw = {'schemaVersion':'contextual-workload-allocation.v1', 'policy':'contextual-linucb',
               'policyParameters':{'ridge':2, 'exploration':1}, 'budget':5, 'seed':1,
               'fitness':{'policy':'weighted-criteria-v2', 'weights':dict.fromkeys(CRITERIA, 1)},
               'workloads':[{'name':'fixture', 'config':'map.json', 'catalogue':'catalogue', 'reference':'reference.json'}]}
        path = self.artifact('prepared/config.json', raw)
        self.catalog.scan()
        return path

    def test_unknown_score_never_becomes_zero(self):
        raw = {'mode':'RECORDED_FEEDBACK_SEQUENTIAL','decisions':[
            {'score':None, 'workload':'w'}, {'score':0, 'workload':'w'}, {'score':3, 'workload':'w'}]}
        path = self.artifact('run/results.json', raw)
        result = normalize(path, raw, True)
        self.assertEqual((result['attempts'],result['positives'],result['unknowns'],result['score']), (3,1,1,3))
        self.assertIsNone(result['attemptsDetail'][0]['score'])
        self.assertEqual(result['trajectory'][1]['unknowns'],1)
        self.assertEqual(result['mode'],'recorded')

    def test_fixed_run_uses_configured_fitness_not_I(self):
        raw = {'strategy':'ga','attempts':[{'I':4,'fitnessScore':0,'candidate':{'workload':'w','id':'s'},
                  'fitnessComponents':{'DELETED_DEPENDENCY':{'count':0}}}]}
        path = self.artifact('fixed/results.json',raw)
        value = normalize(path,raw,True)
        self.assertEqual(value['positives'],0)
        self.assertEqual(value['categories']['DELETED_DEPENDENCY']['count'],0)
        self.assertEqual(value['mode'],'live')

    def test_explicit_legacy_I_policy_preserves_historical_scores(self):
        raw = {'strategy':'ga', 'fitnessPolicy':'complete-impact-v2-object-count',
               'attempts':[{'I':2}, {'I':None}, {'I':0}]}
        path = self.artifact('legacy/results.json',raw)
        value = normalize(path,raw,True)
        self.assertEqual((value['positives'],value['unknowns'],value['score']),(1,1,2))
        self.assertEqual(value['fitness']['policy'],'complete-impact-v2-object-count')
        raw['fitness'] = {'policy':'weighted-criteria-v2'}
        self.assertEqual(normalize(path,raw)['unknowns'],3)

    def test_zero_allocation_workloads_remain_in_scope(self):
        raw = {'decisions':[{'score':0, 'workload':'a'}],
               'perWorkload':[{'workload':'a'},{'workload':'b','name':'Unselected'}]}
        path = self.artifact('run/results.json',raw)
        result = normalize(path,raw,True)
        self.assertEqual(result['workloadCount'],2)
        self.assertEqual(result['workloads'][1]['attempts'],0)

    def test_fixed_workload_id_does_not_prove_comparable_runtime(self):
        raw = {'strategy':'ga','fitness':{'policy':'weighted-criteria-v2'},'attempts':[]}
        path = self.artifact('fixed/results.json',raw)
        self.artifact('fixed/config.json',{'workload':'same-id','runtime':{'image':'different'}})
        self.assertIsNone(normalize(path,raw)['comparisonKey'])

    def test_job_result_change_keeps_id_but_updates_version(self):
        jobs=Jobs(self.repo,self.catalog);directory=jobs.root/'example';directory.mkdir()
        save(directory/'job.json',{'id':'example','state':'PAUSED','mode':'live','createdAt':0})
        (directory/'output').mkdir()
        result=directory/'output/results.json'
        result.write_text('{}')
        first=jobs.list()[0]
        result.write_text('{"new":true}')
        second=jobs.list()[0]
        self.assertEqual(first['resultId'],second['resultId'])
        self.assertNotEqual(first['resultUpdated'],second['resultUpdated'])

    def test_compressed_traces_are_discovered_without_fake_fitness(self):
        self.artifact('study/seed-01.json.gz', {'arm':'uniform','decisions':[{'score':2,'workload':'w'}]})
        self.catalog.scan()
        rows=self.catalog.snapshot()['runs']
        self.assertEqual(len(rows),1)
        self.assertIsNone(rows[0]['fitness'])
        self.assertIsNone(rows[0]['comparisonKey'])

    def test_non_search_results_and_attempt_directories_are_excluded(self):
        self.artifact('other/results.json',{'tests':'pass'})
        self.artifact('run/attempt-001/results.json',{'strategy':'ga','attempts':[]})
        self.catalog.scan()
        self.assertEqual(self.catalog.snapshot()['runs'],[])

    def test_scan_survives_bad_json(self):
        path=self.artifact('broken/results.json',{})
        path.write_text('{')
        self.artifact('good/results.json',{'strategy':'ga','attempts':[]})
        self.catalog.scan()
        self.assertEqual(len(self.catalog.runs),1)
        self.assertEqual(len(self.catalog.errors),1)

    def test_final_partial_journal_line_is_ignored(self):
        path=self.root/'decisions.jsonl'
        path.write_text('{"score": 1}\n{"score":')
        self.assertEqual(journal(path),[{'score':1}])
        path.write_text('{invalid}\n')
        with self.assertRaises(ValueError):journal(path)

    def test_launch_snapshot_resolves_paths_and_filters_policy_parameters(self):
        path=self.config(); original=path.read_bytes()
        jobs=Jobs(self.repo,self.catalog)
        config,mode,_=jobs.prepare({'configId':identifier(path),'policy':'uniform','budget':4})
        self.assertEqual(mode,'recorded')
        self.assertEqual(config['policyParameters'],{})
        self.assertTrue(Path(config['workloads'][0]['reference']).is_absolute())
        self.assertEqual(path.read_bytes(),original)

    def test_invalid_requests_are_rejected(self):
        path=self.config(); jobs=Jobs(self.repo,self.catalog)
        for change in [{'budget':True},{'budget':0},{'seed':-1},{'policy':'shell'},
                       {'weights':dict.fromkeys(CRITERIA,0)}, {'weights':dict.fromkeys(CRITERIA,float('nan'))},
                       {'workloads':[]}, {'workloads':[2]}, {'workloads':[0,0]}]:
            with self.subTest(change=change), self.assertRaises(ValueError):
                jobs.prepare({'configId':identifier(path),**change})

    def test_missing_input_fails_before_launch(self):
        path=self.config();(path.parent/'reference.json').unlink()
        with self.assertRaisesRegex(ValueError,'missing'):
            Jobs(self.repo,self.catalog).prepare({'configId':identifier(path)})

    def test_worker_completes_independently_and_retains_failure(self):
        directory=self.root/'detached';directory.mkdir()
        job={'id':'detached','mode':'recorded','state':'QUEUED','repo':str(self.repo),
             'createdAt':time.time(),'command':[sys.executable,'-c','print("retained output"); raise SystemExit(7)']}
        save(directory/'job.json',job)
        result=subprocess.run([sys.executable,str(HERE/'worker.py'),str(directory)],capture_output=True,timeout=10)
        self.assertEqual(result.returncode,0,result.stderr)
        stored=json.loads((directory/'job.json').read_text())
        self.assertEqual((stored['state'],stored['exitCode']),('FAILED',7))
        self.assertIn('retained output',(directory/'runner.log').read_text())

    def test_running_worker_blocks_second_launch_across_job_managers(self):
        path=self.config();jobs=Jobs(self.repo,self.catalog)
        directory=jobs.root/'running';directory.mkdir()
        job={'id':'running','mode':'recorded','state':'QUEUED','repo':str(self.repo),'createdAt':time.time(),
             'command':[sys.executable,'-c','import time; time.sleep(0.8)']}
        save(directory/'job.json',job)
        process=subprocess.Popen([sys.executable,str(HERE/'worker.py'),str(directory)])
        try:
            other=Jobs(self.repo,self.catalog)
            with self.assertRaisesRegex(ValueError,'already running'):
                other.launch({'configId':identifier(path)})
        finally:
            process.wait(timeout=10)
        self.assertEqual(jobs.list()[0]['state'],'COMPLETE')

    def test_http_requires_host_origin_and_token_for_mutations(self):
        self.config();jobs=Jobs(self.repo,self.catalog)
        server=Server(('127.0.0.1',0),self.catalog,jobs)
        thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start()
        base=f'http://127.0.0.1:{server.server_port}'
        try:
            with urlopen(base+'/api/catalog') as response:token=json.load(response)['token']
            for headers in [{'Content-Type':'application/json'},
                            {'Content-Type':'application/json','X-Workbench-Token':token,'Origin':'https://example.com'},
                            {'Content-Type':'application/json','X-Workbench-Token':token,'Host':'attacker.example'}]:
                with self.subTest(headers=headers), self.assertRaises(HTTPError) as context:
                    urlopen(Request(base+'/api/refresh',b'{}',headers))
                self.assertEqual(context.exception.code,403)
                context.exception.close()
            req=Request(base+'/api/refresh',b'{}',{'Content-Type':'application/json','X-Workbench-Token':token})
            with urlopen(req) as response:self.assertEqual(response.status,202)
            with self.assertRaises(HTTPError) as context:urlopen(base+'/../../server.py')
            self.assertEqual(context.exception.code,404)
            context.exception.close()
        finally:
            server.shutdown();thread.join();server.server_close()
            while self.catalog.scanning:time.sleep(.01)


if __name__=='__main__':
    unittest.main()
