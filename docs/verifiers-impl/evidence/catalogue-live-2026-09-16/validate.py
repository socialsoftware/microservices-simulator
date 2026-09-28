"""Validate the bounded live integration against the retained 72-case reference."""
import json
from pathlib import Path
import sys

REPO = Path(__file__).resolve().parents[4]
sys.path.insert(0, str(REPO / 'verifiers/experiments/fixed-workload-ga'))
from catalogue import enumerated_domain
from search import run
from runtime import digest

ROOT = REPO / 'verifiers/target/catalogue-live-2026-09-16'
REFERENCE = REPO / 'verifiers/target/ga-confirmation-2026-09-16/reference/reference.json'

def read(path): return json.loads(path.read_text())

def main():
    ref = read(REFERENCE)
    summary = {'referenceSha256': digest(REFERENCE), 'arms': {},
               'retainedTraceValidation': read(ROOT / 'replay-validation.json')}
    for strategy in ['ga','random']:
        config = read(ROOT / f'{strategy}-config.json')
        domain, _ = enumerated_domain(ROOT / 'catalogue', config['workload'])
        assert set(domain.candidates) == set(ref['candidates'])
        live = read(ROOT / strategy / 'results.json')
        recorded = run(domain, lambda c,n: ref['observations'][c['key']], strategy=strategy,
                       seed=config['seed'], budget=config['budget'], population=config['population'],
                       mutation=config['mutation'], stall_limit=config['stallLimit'],
                       fitness=config['fitness'], exploration='uniform-unseen')
        fields = ['key','fitnessScore','fitnessComponents','operator','parents','genes','replacedRecovery']
        assert [[a[k] for k in fields] for a in live['attempts']] == [[a[k] for k in fields] for a in recorded['attempts']]
        for a in live['attempts']:
            expected = ref['observations'][a['key']]
            assert all(a.get(k)==expected.get(k) for k in ['status','terminalStatus','scheduleConformance'])
        assert len(live['attempts']) == config['budget']
        assert len({a['key'] for a in live['attempts']}) == config['budget']
        assert live['integrity']=='PASS' and live['generationRequests']==0
        summary['arms'][strategy] = {k:live[k] for k in ['positiveScenarios','nullFitnessAttempts','duplicates',
              'stopReason','samplingPolicy','candidateDomain','applicationSeconds','commandWallSeconds','catalogue']}
        summary['arms'][strategy].update(attempts=len(live['attempts']), recordedChoicesAndFeedbackMatch=True,
                                        resultSha256=digest(ROOT / strategy / 'results.json'))
    summary['enumeration'] = read(ROOT / 'catalogue/domain-count.json')
    (Path(__file__).parent / 'validation.json').write_text(json.dumps(summary,indent=2)+'\n')
    print(json.dumps(summary,indent=2))

if __name__=='__main__': main()
