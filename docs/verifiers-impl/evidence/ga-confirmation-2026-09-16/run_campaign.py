#!/usr/bin/env python3
"""Run the preselected second-workload confirmation without tuning search."""
from concurrent.futures import ThreadPoolExecutor
import hashlib
import json
from pathlib import Path
import subprocess
import sys
import time

REPO = Path(__file__).resolve().parents[4]
ROOT = REPO / 'verifiers/target/ga-confirmation-2026-09-16'
SEARCH = REPO / 'verifiers/experiments/fixed-workload-ga'
PYTHON = sys.executable


def read(path):
    return json.loads(path.read_text())


def save(path, data):
    path.write_text(json.dumps(data, indent=2, sort_keys=True) + '\n')


def check_source():
    for name, expected in read(ROOT / 'selection.json')['searchSourceHashes'].items():
        if hashlib.sha256(Path(name).read_bytes()).hexdigest() != expected:
            raise ValueError('Search source changed after selection: ' + name)


def execute(label, script, *arguments):
    check_source()
    command = [PYTHON, str(script), *map(str, arguments)]
    save(ROOT / f'{label}-command.json', command)
    with (ROOT / f'{label}.log').open('w') as log:
        subprocess.run(command, cwd=REPO, stdout=log, stderr=log, check=True)


def main():
    started = time.time()
    def status(stage, **extra):
        save(ROOT / 'status.json', {'stage': stage, 'startedAt': started,
                                   'updatedAt': time.time(), **extra})
    try:
        status('QUALIFYING')
        with ThreadPoolExecutor(max_workers=2) as pool:
            count = pool.submit(execute, 'count', REPO / 'verifiers/experiments/workload-cohort-exploration/count_domain.py',
                                '--config', ROOT / 'config.json', '--output', ROOT / 'domain')
            control = pool.submit(execute, 'control', SEARCH / 'run.py', 'control',
                                  '--config', ROOT / 'config.json', '--output', ROOT / 'control')
            count.result()
            control.result()
        inventory = read(ROOT / 'domain/domain-count.json')
        n = inventory['capLimitedUniqueCandidateCount']
        if not inventory['countComplete'] or inventory['anyTruncated'] or not 1 <= n <= 1000:
            raise ValueError('Domain outside predeclared complete-map qualification')
        sys.path.insert(0, str(SEARCH))
        from fitness import assess
        cfg = read(ROOT / 'config.json')
        control = read(ROOT / 'control/control.json')
        score = assess(control, cfg['fitness'])
        if (control['terminalStatus'], control['scheduleConformance'], score['fitnessScore']) != ('SUCCESS', 'EXACT', 0):
            raise ValueError('Require a successful exact score-zero no-fault control')
        save(ROOT / 'qualification.json', {'candidateCount': n, 'vectorCount': inventory['vectorDomainSize'],
             'countComplete': True, 'anyTruncated': False, 'controlStatus': control['terminalStatus'],
             'controlConformance': control['scheduleConformance'], 'controlFitness': score,
             'controlWallSeconds': control['wallSeconds']})
        status('MEASURING_REFERENCE', candidateCount=n)
        execute('reference', SEARCH / 'exhaustive_reference.py', '--config', ROOT / 'config.json',
                '--enumeration', ROOT / 'domain', '--control', ROOT / 'control/control.json',
                '--reuse', ROOT / 'control', '--output', ROOT / 'reference')
        status('COMPARING_UNIFORM_BASELINE', candidateCount=n)
        execute('uniform', SEARCH / 'uniform_reference.py', '--reference', ROOT / 'reference',
                '--output', ROOT / 'uniform')
        status('COMPARING_FROZEN_VARIANT', candidateCount=n)
        execute('variant', SEARCH / 'uniform_exploration.py', '--reference', ROOT / 'reference',
                '--uniform', ROOT / 'uniform', '--output', ROOT / 'variant')
        check_source()
        status('COMPLETE', candidateCount=n)
    except BaseException as error:
        status('FAILED', error=str(error))
        raise


if __name__ == '__main__':
    main()
