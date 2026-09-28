"""Frozen scope-gap cohort; fresh Compose attempts, immutable old evidence, resumable output."""
import argparse
import collections
from concurrent.futures import ThreadPoolExecutor, as_completed
import copy
import json
import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(ROOT / 'verifiers/experiments/fixed-workload-ga'))
from runtime import Runtime, batch, read, save, digest, package, copy_artifact, retained_attempt
from fitness import assess, configuration, CRITERIA_V2
OUT = ROOT / 'verifiers/target/saga-copy-transport-2026-09-28-final'
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/saga-copy-transport-2026-09-28'
DIAG = ROOT / 'docs/verifiers-impl/evidence/workload-master-inventory-2026-09-25/measurement-diagnosis-2026-09-28.json'
SOURCES = [ROOT / 'simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/copiedupdate/CopiedUpdateSession.java',
           ROOT / 'verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/LostCopiedUpdateAssessor.java']
FIT = configuration({'policy': 'weighted-criteria-v2', 'weights': {c: 1 for c in CRITERIA_V2}})

class ComposeRuntime(Runtime):
    def command(self, out, name, main, args):
        ordinary = super().command(out, name, main, args)
        java = ordinary[ordinary.index(self.config['image']) + 1:]
        override = OUT / 'compose-runtime.json'
        # The base service supplies the user's mounted environment and resource limits.
        return ['docker', 'compose', '-p', 'microservices-simulator', '-f', str(ROOT / 'docker-compose.yml'),
                '-f', str(override), 'run', '--rm', '--no-deps', '--pull', 'never', '-T',
                '--name', name, '--entrypoint', 'java', '-v', str(out.resolve()) + ':/out',
                'scenario-executor', *java]


def freeze():
    OUT.mkdir(parents=True, exist_ok=True)
    EVIDENCE.mkdir(parents=True, exist_ok=True)
    if (EVIDENCE / 'selection.json').exists():
        return
    rows = [r for r in read(DIAG)['rows'] if r['lostGaps'] and
            all(g.startswith('MISSING_COMMAND_SCOPE:') for g in r['lostGaps'])]
    selected, excluded = [], []
    for r in sorted(rows, key=lambda r: (r['workload'], r['candidate'])):
        if 'Async' in r['family']:
            excluded.append({**r, 'exclusion': 'ASYNC_FAMILY_OUT_OF_SCOPE'})
            continue
        ref = ROOT / r['reference']
        assert digest(ref) == r['referenceSha256']
        old = read(ref)['observations'][r['candidate']]
        directory = Path(old['directory'])
        if not directory.exists():
            # Restored cluster archives retain the original logical directory in the row.
            candidates = list(ref.parent.rglob('attempt.json'))
            matches = [p.parent for p in candidates if read(p).get('candidate', {}).get('key') == r['candidate']]
            if len(matches) != 1:
                raise ValueError('Cannot uniquely locate old attempt: ' + r['candidate'])
            directory = matches[0]
        replay = read(directory / 'replay.json')
        assert package(directory / 'package/scenario-catalog-manifest.json')['hashes'] == old['packageHashes']
        selected.append({**r, 'oldDirectory': str(directory), 'runtimeHash': digest(directory / 'replay.json'),
                         'oldReportHashes': old['reportHashes'], 'candidateData': old['candidate']})
    save(EVIDENCE / 'selection.json', {'diagnosisSha256': digest(DIAG), 'selectionRule':
        'All command-scope-only rows, excluding any Async family, fixed before new rewards.',
        'selected': selected, 'excluded': excluded})


def prepare():
    freeze()
    selected = read(EVIDENCE / 'selection.json')['selected']
    base = read(Path(selected[0]['oldDirectory']) / 'replay.json')['runtime']
    Runtime(base).verify()
    overlay = OUT / 'overlay'
    if not (OUT / 'source-hashes.json').exists():
        overlay.mkdir(exist_ok=True)
        src = OUT / 'source'; src.mkdir(exist_ok=True)
        files = []
        for p in SOURCES:
            q = src / p.name; shutil.copy2(p, q); files.append(str(q))
        cp = base['classpath'].replace('/reports/', str(ROOT / 'verifiers/target') + '/')
        subprocess.run(['/opt/homebrew/opt/openjdk@21/bin/javac', '-cp', cp, '-d', str(overlay), *files], check=True)
        save(OUT / 'source-hashes.json', {str(p.relative_to(ROOT)): digest(p) for p in SOURCES})
        save(OUT / 'overlay-hashes.json', {str(p): digest(p) for p in sorted(overlay.rglob('*.class'))})
    assert read(OUT / 'source-hashes.json') == {str(p.relative_to(ROOT)): digest(p) for p in SOURCES}
    save(OUT / 'compose-runtime.json', {'services': {'scenario-executor': {'image': base['image'], 'pull_policy': 'never', 'environment': {'SPRING_PROFILES': None}}}})
    for r in selected:
        out = OUT / r['candidate']
        if (out / 'runtime.json').exists():
            continue
        olddir = Path(r['oldDirectory'])
        assert digest(olddir / 'replay.json') == r['runtimeHash']
        rt = read(olddir / 'replay.json')['runtime']
        assert rt['image'] == base['image']
        rt['classpath'] = batch.container(overlay) + ':' + rt['classpath']
        rt['hashes'] = {**rt['hashes'], **read(OUT / 'overlay-hashes.json')}
        out.mkdir(exist_ok=True)
        shutil.copytree(olddir / 'package', out / 'package', copy_function=copy_artifact, dirs_exist_ok=True)
        signature = hashlib.sha256(json.dumps(rt, sort_keys=True).encode()).hexdigest()
        shared = OUT / ('runtime-' + signature + '.json')
        if not shared.exists(): save(shared, rt)
        os.link(shared, out / 'runtime.json')


def run(limit, smoke):
    prepare()
    rows = read(EVIDENCE / 'selection.json')['selected']
    if smoke:
        # Structural representatives, selected before looking at fresh results.
        families = ['CreateQuiz + RemoveStudentFromCourseExecution', 'CreateCourseExecution + CreateQuestion',
                    'CreateTopic + UpdateTopic']
        rows = [min((r for r in rows if r['family'] == f),
                    key=lambda r: (r['candidateData']['faultVector'].count('1'), r['candidate'])) for f in families]
        save(EVIDENCE / 'smoke-controls-selection.json', rows)
    pending = []
    verified = set()
    for row in rows:
        out = OUT / row['candidate']
        if (out / 'attempt-002/attempt.json').exists():
            retained_attempt(out / 'attempt-002/attempt.json')
            continue
        rt = ComposeRuntime(read(out / 'runtime.json'))
        signature = digest(out / 'runtime.json')
        if signature not in verified:
            rt.verify(); verified.add(signature)
        if (out / 'attempt-002').exists():
            raise RuntimeError('Interrupted attempt retained; inspect before explicit retry: ' + str(out))
        pending.append((out, rt, row))
        if limit and len(pending) >= limit:
            break
    def execute(item):
        out, rt, row = item
        if shutil.disk_usage(ROOT).free < 2 * 1024**3:
            raise RuntimeError('Stopped before next attempt: less than 2 GiB free')
        return rt.evaluate(out, row['candidateData'], 2, 180)
    with ThreadPoolExecutor(max_workers=1 if smoke else 3) as pool:
        for result in pool.map(execute, pending):
            summarize()


def summarize():
    selection = read(EVIDENCE / 'selection.json')
    results = []
    references = {}
    for row in selection['selected']:
        path = OUT / row['candidate'] / 'attempt-002/attempt.json'
        if not path.exists():
            continue
        new = read(path)
        if row['reference'] not in references:
            references[row['reference']] = read(ROOT / row['reference'])
        old = references[row['reference']]['observations'][row['candidate']]
        scores = assess(new, FIT)
        results.append({'workload': row['workload'], 'family': row['family'], 'candidate': row['candidate'],
                        'oldReference': row['reference'], 'oldReferenceSha256': row['referenceSha256'],
                        'newAttempt': str(path.relative_to(ROOT)), 'newAttemptSha256': digest(path),
                        'status': new['status'], 'oldCopyCoverage': old['lostCopiedUpdateCoverage'],
                        'copyCoverage': new['lostCopiedUpdateCoverage'], 'copyCount': new['lostCopiedUpdateCount'],
                        'copyGaps': new['lostCopiedUpdateCoverageGaps'], 'fitness': scores})
    save(EVIDENCE / 'results.json', {'selected': len(selection['selected']), 'excludedAsync': len(selection['excluded']),
                                  'completed': len(results), 'rows': results})
    print('Completed', len(results), 'of', len(selection['selected']), flush=True)

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('stage', choices=['prepare', 'smoke', 'run', 'summary'])
    parser.add_argument('--limit', type=int)
    args = parser.parse_args()
    if args.stage == 'prepare': prepare()
    elif args.stage == 'summary': summarize()
    else: run(args.limit, args.stage == 'smoke')
