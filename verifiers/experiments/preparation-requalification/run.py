"""Qualify the 17 reviewed recipes without overwriting historical evidence."""
import collections
import argparse
from concurrent.futures import ThreadPoolExecutor
import copy
import hashlib
import json
from pathlib import Path
import sys
import threading

ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'fixed-workload-ga'))
from runtime import Runtime, package, read, save, digest, retained_attempt
from fitness import assess, configuration, CRITERIA_V2

MASTER = ROOT / 'docs/verifiers-impl/evidence/workload-master-inventory-2026-09-25'
OUT = ROOT / 'verifiers/target/preparation-17-2026-09-26'
BASE = ROOT / 'verifiers/target/cluster-next-preparation-2026-09-19/e61e7630681d/package'
FIT = configuration({'policy': 'weighted-criteria-v2', 'weights': {c: 1 for c in CRITERIA_V2}})
SLOTS = None


def lines(path):
    return [json.loads(x) for x in path.read_text().splitlines() if x.strip()]


def write_lines(path, values):
    path.write_text(''.join(json.dumps(x, sort_keys=True) + '\n' for x in values))


def prepare():
    metadata = {r['id']: r for r in lines(MASTER / 'workloads.jsonl')}
    old = {r['workload']: r for r in read(MASTER.parent / 'transfer-inventory-2026-09-21/inventory.json')['rows']}
    audited = {r['workload']: r for r in read(OUT / 'wire-audit.json')['rows']}
    runtime = read(ROOT / 'verifiers/target/shared-setup-fix-2026-09-26/runtime.json')
    Runtime(runtime).verify()
    rows = []
    for wid, audit in audited.items():
        m = metadata[wid]
        historical = read(ROOT / m['reference'])
        observation = next(iter(historical['observations'].values()))
        if wid in old:
            source = BASE
            workload = read(Path(old[wid]['workloadFile']))
        else:
            source = Path(observation['directory']) / 'package'
            if not source.exists():
                source = ROOT / m['metadataSource']
                source = source.parent
            workload = next(w for w in lines(source / 'workloads.jsonl') if w['id'] == wid)
        # Restore exact static evidence, checking the original attempt's hashes.
        for role, name in [('inputs', 'inputs.jsonl'), ('setups', 'setups.jsonl'),
                           ('sagas', 'sagas.jsonl'), ('interactions', 'interactions.jsonl'),
                           ('copy-contracts', 'copy-contracts.json')]:
            if digest(source / name) != observation['packageHashes'][role]:
                raise ValueError('Historical artifact does not match its measurement: ' + wid + ' ' + role)
        selected_setup = next(s for s in lines(source / 'setups.jsonl') if s['id'] == workload['setup'])
        prefix = audit['currentSetupWire']
        # A prior real preflight rejected the asynchronous method. Preserve that
        # result; qualify the original standalone-query fixture separately.
        rejected_prefix = OUT / wid[:12] / 'unsupported-async-prefix/preflight.json'
        unsupported = rejected_prefix.exists()
        if unsupported:
            report = read(rejected_prefix)
            if report['workloads'][0]['status'] != 'SETUP_METHOD_NOT_AUTHORIZED':
                raise ValueError('Fallback requires the reviewed setup-method rejection')
        setup = copy.deepcopy(prefix if prefix and not unsupported else selected_setup)
        mode = 'CURRENT_SOURCE_PREFIX' if prefix and not unsupported else 'EXPLICIT_RETAINED_FIXTURE'
        helper_repair = None
        if wid.startswith('d53ab56d5d78'):
            # The source feature creates content before removing the first of
            # two executions. Its unassigned createQuestion helper was omitted
            # by extraction; expand that exact call in this experiment recipe.
            if len(setup['actions']) != 3:
                raise ValueError('Unexpected removal preparation prefix')
            action = next(copy.deepcopy(a) for s in lines(BASE / 'setups.jsonl')
                          for a in s['actions'] if '#createQuestion(' in a['call'])
            action['id'] = action['result'] = 'setup-action-4'
            action['arguments'][0]['action'] = 'setup-action-1'
            fields = action['arguments'][1]['fields']
            fields['topicDto']['elements'] = [{'kind': 'result', 'action': 'setup-action-2'}]
            expected = {'title': 'Title One', 'content': 'Content One'}
            if any(fields[k] != {'kind': 'literal', 'value': v} for k, v in expected.items()):
                raise ValueError('Question fixture differs from the source helper')
            setup['actions'].append(action)
            sources = [ROOT / 'applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/QuizzesSpockTest.groovy',
                       ROOT / 'applications/quizzes/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/quizzes/sagas/coordination/execution/RemoveCourseExecutionTest.groovy']
            helper_repair = {str(p): digest(p) for p in sources}
            mode = 'CURRENT_SOURCE_PREFIX_WITH_EXPLICIT_HELPER_REPAIR'
        # These retained fixtures define reduced workloads. They do not claim to
        # replay the assertion/workflow history of the original Spock feature.
        setup['bindings'] = [b for b in setup['bindings'] if b['input'] in m['inputs']]
        normalized_old = {**selected_setup, 'bindings': [b for b in selected_setup['bindings']
                                                       if b['input'] in m['inputs']]}
        same = {k: v for k, v in setup.items() if k != 'id'} == {
            k: v for k, v in normalized_old.items() if k != 'id'}
        setup['id'] = 'qualified-' + hashlib.sha256(json.dumps(setup, sort_keys=True).encode()).hexdigest()[:24]
        workload = {**workload, 'setup': setup['id']}
        directory = OUT / wid[:12]
        directory.mkdir(exist_ok=True)
        pack = directory / 'package'
        pack.mkdir(exist_ok=True)
        # Saga definitions include event/dependency closure beyond participants.
        write_lines(pack / 'sagas.jsonl', lines(source / 'sagas.jsonl'))
        write_lines(pack / 'inputs.jsonl', [i for i in lines(source / 'inputs.jsonl') if i['id'] in m['inputs']])
        write_lines(pack / 'setups.jsonl', [setup])
        write_lines(pack / 'workloads.jsonl', [workload])
        write_lines(pack / 'interactions.jsonl', [i for i in lines(source / 'interactions.jsonl')
                                                if i['id'] in workload['interactions']])
        write_lines(pack / 'fault-scenarios.jsonl', [{k: v for k, v in c.items() if k not in ('key', 'aliases')}
                                                   for c in historical['candidates'].values()])
        write_lines(pack / 'requests.jsonl', [])
        save(pack / 'copy-contracts.json', read(source / 'copy-contracts.json'))
        # Retain the static analysis accounting contract. The exact bounded
        # execution selection and its counts are owned by selection.json.
        save(pack / 'accounting.json', read(source / 'accounting.json'))
        roles = {'sagas': 'sagas.jsonl', 'inputs': 'inputs.jsonl', 'setups': 'setups.jsonl',
                 'workloads': 'workloads.jsonl', 'interactions': 'interactions.jsonl',
                 'faultScenarios': 'fault-scenarios.jsonl', 'requests': 'requests.jsonl',
                 'accounting': 'accounting.json', 'copy-contracts': 'copy-contracts.json'}
        save(pack / 'scenario-catalog-manifest.json', {'formatVersion': 1, 'files': {
            role: {'path': name, 'sha256': digest(pack / name)} for role, name in roles.items()}})
        package(pack / 'scenario-catalog-manifest.json')
        save(directory / 'config.json', {'workload': wid, 'manifest': str(pack / 'scenario-catalog-manifest.json'),
                                         'runtime': runtime, 'fitness': FIT, 'timeout': 180})
        rows.append({'workload': wid, 'family': m['sagaFamily'], 'mode': mode,
                     'samePreparation': same, 'oldActions': len(selected_setup['actions']),
                     'newActions': len(setup['actions']), 'historicalReference': m['reference'],
                     'unsupportedSourcePrefix': str(rejected_prefix) if unsupported else None,
                     'explicitHelperRepairSources': helper_repair,
                     'historicalReferenceSha256': digest(ROOT / m['reference']),
                     'restoredStaticSource': str(source), 'historicalSetupSha256': observation['packageHashes']['setups'],
                     'selectedSetupSha256': digest(pack / 'setups.jsonl'),
                     'candidateCount': len(historical['candidates']), 'directory': str(directory)})
    save(OUT / 'selection.json', rows)
    return rows, runtime


def qualify(row, runtime):
    directory = Path(row['directory'])
    rt = Runtime(runtime)
    if not (directory / 'preflight-process.json').exists():
        with SLOTS:
            process = rt.launch(directory, 'preflight', runtime['executorMain'], [*runtime['executorArgs'],
                '--package-path', '/out/package/scenario-catalog-manifest.json', '--preflight', 'true',
                '--preflight-worker', 'true', '--preflight-workload-ids', row['workload'],
                '--output-path', '/out/preflight.json'], 180)
        save(directory / 'preflight-process.json', process)
    process = read(directory / 'preflight-process.json')
    if not (directory / 'preflight.json').exists():
        return {**row, 'state': 'PREFLIGHT_PROCESS_FAILURE', 'process': process}
    report = read(directory / 'preflight.json')
    if len(report['workloads']) != 1 or report['workloads'][0]['workloadPlanId'] != row['workload']:
        raise ValueError('Preflight identity mismatch')
    if report['workloads'][0]['status'] != 'SETUP_READY':
        return {**row, 'state': 'PREPARATION_BLOCKED', 'preflight': report['workloads'][0]}
    historical = read(ROOT / row['historicalReference'])
    candidates = historical['candidates']
    ordered = sorted(candidates, key=lambda k: (bool(candidates[k]['faultVector'].strip('0')), k))
    def measure(number_key):
        number, key = number_key
        path = directory / f'attempt-{number:03d}/attempt.json'
        if path.exists():
            value = retained_attempt(path)
        else:
            with SLOTS:
                value = rt.evaluate(directory, candidates[key], number, 180)
        if value['packageHashes'] != package(directory / 'package/scenario-catalog-manifest.json')['hashes']:
            raise ValueError('Attempt was measured with another preparation package')
        if value['candidate'] != candidates[key]:
            raise ValueError('Attempt candidate changed')
        print(row['workload'][:12], number, len(ordered), value['status'],
              assess(value, FIT)['fitnessScore'], flush=True)
        return key, value
    with ThreadPoolExecutor(max_workers=2) as scenarios:
        observations = dict(scenarios.map(measure, enumerate(ordered, 1)))
    fresh = {'candidates': candidates, 'observations': observations,
             'fitness': {k: assess(v, FIT) for k, v in observations.items()}}
    save(directory / 'reference.json', fresh)
    scores = [v['fitnessScore'] for v in fresh['fitness'].values()]
    baseline = fresh['fitness'][ordered[0]]['fitnessScore']
    counts = {'positive': sum(s is not None and s > 0 for s in scores),
              'zero': scores.count(0), 'unavailable': scores.count(None)}
    changes = [k for k in candidates if fresh['fitness'][k] != assess(historical['observations'][k], FIT)]
    return {**row, 'state': 'QUALIFIED' if baseline is not None else 'CONTROL_MEASUREMENT_UNAVAILABLE',
            'preflightStatus': 'SETUP_READY', 'reference': str(directory / 'reference.json'),
            'referenceSha256': digest(directory / 'reference.json'), 'joint': counts,
            'controlScore': baseline, 'changedAssessmentKeys': changes,
            'criteria': {c: {'positive': sum(v['fitnessComponents'][c]['count'] is not None
                and v['fitnessComponents'][c]['count'] > 0 for v in fresh['fitness'].values())}
                for c in CRITERIA_V2}}


def main():
    global SLOTS
    parser = argparse.ArgumentParser()
    parser.add_argument('--workers', type=int, default=2)
    args = parser.parse_args()
    if args.workers not in (1, 2, 3, 4):
        raise ValueError('Use 1..4 bounded workers')
    SLOTS = threading.BoundedSemaphore(args.workers)
    rows, runtime = prepare()
    sessions = read(OUT / 'sessions.json') if (OUT / 'sessions.json').exists() else []
    sessions.append({'workers': args.workers, 'resumesCompletedEvidence': True})
    save(OUT / 'sessions.json', sessions)
    results = []
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        for result in pool.map(lambda r: qualify(r, runtime), rows):
            results.append(result)
            save(OUT / 'qualification.json', {'rows': results, 'complete': len(results) == len(rows)})
            print('QUALIFICATION', result['workload'][:12], result['state'], result.get('joint'), flush=True)
    Runtime(runtime).verify()


if __name__ == '__main__':
    main()
