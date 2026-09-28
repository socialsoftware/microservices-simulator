#!/usr/bin/env python3
"""One-worker, resumable comparison over a completely enumerated forward-order set."""
import argparse
import fcntl
import json
import os
from pathlib import Path
import shutil
import sys
import time

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from runtime import Runtime, package, read, save, digest, retained_attempt
from fitness import components


def schedule_key(workload):
    roles = {p['id']: (p['saga'], p['input']) for p in workload['participants']}
    return json.dumps([(roles[s['participant']], s['sagaStep']) for s in workload['schedule']], sort_keys=True)


def fault_key(workload, scenario):
    roles = {p['id']: p['saga'] for p in workload['participants']}
    return sorted([roles[s['participant']], s['sagaStep']] for s in workload['schedule']
                  if scenario['faultVector'][s['faultSlot']] == '1')


def inventory(root):
    full = package(root / 'full/scenario-catalog-manifest.json')
    compressed = package(root / 'compressed/scenario-catalog-manifest.json')
    workloads = {w['id']: w for w in full['records']['workloads']}
    kept = {schedule_key(w) for w in compressed['records']['workloads']}
    assert len(workloads) == 6 and len(kept) == 3
    assert kept <= {schedule_key(w) for w in workloads.values()}
    # The same inputs, setup and copy contracts must underlie both alternatives.
    for name in ('inputs', 'setups', 'copy-contracts'):
        assert full['hashes'][name] == compressed['hashes'][name], name
    # Workload/scenario IDs are content based: retained scenarios must be exact members.
    by_id = {s['id']: s for s in full['records']['faultScenarios']}
    for s in compressed['records']['faultScenarios']:
        assert by_id.get(s['id']) == s, 'Compressed scenario not an exact full-set member'
    candidates = sorted(by_id.values(), key=lambda c: ('1' in c['faultVector'], c['workload'], c['id']))
    return [{'number': i, 'candidate': c, 'retained': schedule_key(workloads[c['workload']]) in kept,
             'faults': fault_key(workloads[c['workload']], c),
             'forwardOrder': json.loads(schedule_key(workloads[c['workload']]))}
            for i, c in enumerate(candidates, 1)]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('root', type=Path)
    ap.add_argument('--controls-only', action='store_true')
    args = ap.parse_args()
    root = args.root.resolve()
    run = root / 'run'
    run.mkdir(exist_ok=True)
    lock = (run / '.lock').open('w')
    fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
    status = {'pid': os.getpid(), 'stage': 'VERIFYING', 'updatedAt': time.time()}
    save(run / 'status.json', status)
    try:
        rows = inventory(root)
        if (run / 'inventory.json').exists():
            assert read(run / 'inventory.json') == rows, 'Inventory changed'
        else:
            save(run / 'inventory.json', rows)
        if not (run / 'package').exists():
            shutil.copytree(root / 'full', run / 'package')
        assert package(run / 'package/scenario-catalog-manifest.json')['hashes'] == package(root / 'full/scenario-catalog-manifest.json')['hashes']
        runtime = Runtime(read(run / 'runtime.json'))
        runtime.verify()
        # Seal the measurement implementation and runtime configuration before observing outcomes.
        sources = [HERE / 'run.py', HERE / 'analyze.py', HERE.parent / 'fixed-workload-ga/runtime.py',
                   HERE.parent / 'fixed-workload-ga/fitness.py', HERE.parent / 'batch-execution/run.py',
                   HERE.parent / 'impact-v2-broader/qualification.py', run / 'runtime.json']
        seal = {str(p): digest(p) for p in sources}
        if (run / 'measurement-seal.json').exists():
            assert read(run / 'measurement-seal.json') == seal, 'Measurement implementation changed'
        else:
            save(run / 'measurement-seal.json', seal)
        completed = 0
        for row in rows:
            c, number = row['candidate'], row['number']
            control = '1' not in c['faultVector']
            if args.controls_only and not control:
                break
            path = run / f'attempt-{number:03d}/attempt.json'
            if path.exists():
                result = retained_attempt(path)
                assert result['candidate'] == c
            else:
                # Never overwrite an interrupted attempt or silently substitute another scenario.
                attempt = path.parent
                if attempt.exists():
                    saved = run / 'interrupted'
                    saved.mkdir(exist_ok=True)
                    attempt.rename(saved / f'{attempt.name}-{time.time_ns()}')
                if shutil.disk_usage(root).free < 4 * 1024**3:
                    status.update(stage='PAUSED_LOW_DISK', completed=completed, total=len(rows), updatedAt=time.time())
                    save(run / 'status.json', status)
                    return
                status.update(stage='CONTROLS' if control else 'MEASURING', current=number,
                              completed=completed, total=len(rows), updatedAt=time.time())
                save(run / 'status.json', status)
                result = runtime.evaluate(run, c, number, 240)
            completed += 1
            if control:
                values = components(result, True)
                if result.get('terminalStatus') != 'SUCCESS' or result.get('scheduleConformance') != 'EXACT' \
                        or any(v['count'] != 0 for v in values.values()):
                    raise RuntimeError(f'Control {number} not successful, exact and fully zero: {values}')
            elif result['status'] in ('INFRASTRUCTURE_FAILURE', 'INVALID_REPORT'):
                raise RuntimeError(f'Attempt {number}: {result["status"]}; retained for diagnosis')
            from analyze import summarize
            summarize(root)
        runtime.verify()
        assert {str(p): digest(p) for p in sources} == seal, 'Measurement sources changed during execution'
        status.update(stage='CONTROLS_PASSED' if args.controls_only else 'COMPLETE',
                      completed=completed, total=len(rows), updatedAt=time.time())
        save(run / 'status.json', status)
    except BaseException as error:
        status.update(stage='STOPPED', error=str(error), updatedAt=time.time())
        save(run / 'status.json', status)
        raise


if __name__ == '__main__':
    main()
