#!/usr/bin/env python3
"""Bounded structural discovery, supported requests, fresh execution, auditable map."""
import argparse
import csv
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import itertools
import json
from pathlib import Path
import random
import shutil
import statistics
import sys
import time
import uuid

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'batch-execution'))
import run as batch

SEED = 9072026
STRATA = [
    ('AddStudent', 'RemoveStudentFromCourseExecution'),
    ('AddStudent', 'UpdateStudentName'),
    ('UpdateStudentName', 'AddParticipant'),
    ('RemoveStudentFromCourseExecution', 'UpdateStudentName'),
    ('GetCourseExecutionById', 'UpdateStudentName', 'AddParticipant'),
    ('AnonymizeStudent', 'GetCourseExecutionById', 'RemoveStudentFromCourseExecution'),
    ('AddParticipant', 'SolveQuizAsync'),
    ('AddStudent', 'AddParticipant', 'SolveQuizAsync'),
]


def names(workload):
    return tuple(p['saga'].split('.')[-1].removesuffix('FunctionalitySagas')
                 for p in workload['participants'])


def vectors(workload):
    steps = [a for a in workload['schedule'] if a['kind'] == 'step']
    slots = sorted(a['faultSlot'] for a in steps)
    if slots != list(range(len(steps))):
        raise ValueError('Fault slots must be contiguous')
    choices = [[None] + [a['faultSlot'] for a in steps if a['participant'] == p['id']]
               for p in workload['participants']]
    return sorted(''.join('1' if i in positions else '0' for i in slots)
                  for positions in itertools.product(*choices))


def sample_vectors(all_vectors, seed, cap=24):
    controls = sorted(v for v in all_vectors if v.count('1') <= 1)
    if len(controls) > cap:
        raise ValueError('Control coverage exceeds vector budget')
    remaining = sorted(set(all_vectors) - set(controls))
    return sorted(controls + random.Random(seed).sample(remaining, min(cap-len(controls), len(remaining))))


def sample_scenarios(rows, selected_vectors, seed, cap=32):
    ordered = sorted(rows, key=lambda r: (r['faultVector'], r['id']))
    if len({r['id'] for r in ordered}) != len(ordered):
        raise ValueError('Duplicate scenario')
    groups = {v: [r for r in ordered if r['faultVector'] == v] for v in selected_vectors}
    if any(not rs for rs in groups.values()) or len(groups) > cap:
        raise ValueError('Cannot cover every selected vector')
    if len(ordered) <= cap:
        return ordered
    rng = random.Random(seed)
    chosen = [rng.choice(groups[v]) for v in sorted(groups)]
    ids = {r['id'] for r in chosen}
    chosen += rng.sample([r for r in ordered if r['id'] not in ids], cap-len(chosen))
    return sorted(chosen, key=lambda r: (r['faultVector'], r['id']))


def prepare(args):
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if (out / 'structural-plan.json').exists():
        raise ValueError('Refusing to overwrite frozen structural selection')
    data = batch.package(args.manifest)
    shutil.copytree(args.manifest.parent, out / 'package')
    manifest = out / 'package' / args.manifest.name
    records = data['records']
    setups = {s['id']: s for s in records['setups']}
    eligible_ids = {s['workload'] for s in records['faultScenarios']}
    groups = defaultdict(list)
    for w in records['workloads']:
        setup = setups.get(w.get('setup'), {})
        if len(w['participants']) > 1 and setup.get('kind') == 'sourceDerived' \
                and setup.get('materializable') and w['id'] in eligible_ids:
            groups[names(w)].append(w)
    scopes, requests = [], []
    for index, stratum in enumerate(STRATA, 1):
        rows = groups.get(stratum, [])
        scope = {'caseGroup': f'w{index:02}', 'sagas': stratum, 'eligibleCount': len(rows),
                 'eligibleWorkloadIds': sorted(w['id'] for w in rows)}
        if not rows:
            scope['status'] = 'NO_ELIGIBLE_WORKLOAD'
            scopes.append(scope)
            continue
        maximum_events = max(sum(a['kind'] != 'step' for a in w['schedule']) for w in rows)
        preferred = sorted([w for w in rows if sum(a['kind'] != 'step' for a in w['schedule']) == maximum_events],
                           key=lambda w: w['id'])
        workload = random.Random(SEED + index).choice(preferred)
        all_vectors = vectors(workload)
        chosen = sample_vectors(all_vectors, SEED + index)
        scope.update(status='SELECTED', workload=workload, maximumEvents=maximum_events,
                     preferredCount=len(preferred), allCanonicalVectors=all_vectors, selectedVectors=chosen)
        scopes.append(scope)
        requests.extend({'manifest': batch.container(manifest), 'workloadId': workload['id'], 'vector': v}
                        for v in chosen)
    prior = batch.read(batch.TARGET / 'batch-execution/selection.json')
    source_hashes = batch.source_hashes()
    drift = sorted(p for p in source_hashes.keys() | prior['sourceHashes'].keys()
                   if source_hashes.get(p) != prior['sourceHashes'].get(p))
    if any('/src/main/' in p or p.endswith('pom.xml') for p in drift):
        raise ValueError('Production/build source drift requires a new snapshot')
    plan = {'schema': 'space-map-structural.v1', 'frozenAt': datetime.now(timezone.utc).isoformat(),
            'seed': SEED, 'build': str(args.build.resolve()), 'manifest': str(manifest),
            'sourceHashes': prior['sourceHashes'], 'checkoutSourceDrift': drift,
            'buildHashes': batch.build_hashes(args.build), 'initialPackageHashes': data['hashes'],
            'generationAccounting': records['accounting'],
            'eligibleStrata': [{'sagas': key, 'count': len(value)} for key, value in sorted(groups.items())],
            'scopes': scopes, 'maxVectors': 24, 'recoveryCap': 10000, 'maxScenarios': 32,
            'timeoutSeconds': 120, 'maxConcurrency': 2, 'maxRepeatsPerWorkload': 3}
    batch.save(out / 'structural-plan.json', plan)
    batch.save(out / 'requests.json', requests)
    print('Frozen', len(scopes), 'strata;', len(requests), 'requests', flush=True)
    started = time.perf_counter()
    name = 'space-map-requests-' + uuid.uuid4().hex[:10]
    result = batch.run_process(batch.compose(name, {
        'BUILD_OUTPUT_DIR': batch.container(args.build), 'REQUESTS_PATH': batch.container(out/'requests.json'),
        'RESULTS_PATH': batch.container(out/'request-results.jsonl'),
        'JAVA_TOOL_OPTIONS': '-Xmx1536m -XX:MaxMetaspaceSize=512m'},
        ['bash', '/verifiers/experiments/space-map/request-prepared.sh']),
        out/'requests.log', 3600, lambda: batch.remove_container(name))
    batch.save(out/'request-process.json', result)
    if result['status'] != 'EXITED':
        raise ValueError('Request batch failed; preserve diagnostics, no execution')
    responses = [json.loads(line) for line in (out/'request-results.jsonl').read_text().splitlines()]
    if len(responses) != len(requests):
        raise ValueError('Missing request responses')
    data = batch.package(manifest)
    selected_rows = []
    for index, scope in enumerate(scopes, 1):
        if scope['status'] != 'SELECTED':
            continue
        wid = scope['workload']['id']
        results = [r for r in responses if r['workloadPlanId'] == wid]
        scope['requests'] = results
        good_vectors = sorted(r['assignedVector'] for r in results if r['status'] in ('PERSISTED', 'DEDUPLICATED'))
        scope['failedVectors'] = sorted(set(scope['selectedVectors']) - set(good_vectors))
        rows = [r for r in data['records']['faultScenarios'] if r['workload'] == wid and r['faultVector'] in good_vectors]
        scope['generatedCandidates'] = rows
        chosen = sample_scenarios(rows, good_vectors, SEED + index) if rows else []
        scope['selectedScenarioIds'] = [r['id'] for r in chosen]
        for number, row in enumerate(chosen, 1):
            selected_rows.append({'caseId': f'{scope["caseGroup"]}-{number:02}', 'caseGroup': scope['caseGroup'],
                                 'workloadId': wid, 'faultScenarioId': row['id'], 'faultVector': row['faultVector']})
    plan.update(schema='space-map-experiment.v1', scopes=scopes, rows=selected_rows,
                preparationSeconds=time.perf_counter()-started, packageHashes=data['hashes'],
                frozenAt=datetime.now(timezone.utc).isoformat(),
                structuralPlanSha256=batch.digest(out/'structural-plan.json'))
    batch.save(out/'experiment.json', plan)
    print('Frozen', len(selected_rows), 'discovery executions', flush=True)


def signature(result):
    return json.dumps([result['status'], result['score'], result.get('observed', {}).get('conditions')], sort_keys=True)


def representatives(rows, results):
    by_case = {r['caseId']: r for r in rows}
    grouped = defaultdict(dict)
    for result in sorted(results, key=lambda r: by_case[r['caseId']]['faultScenarioId']):
        row = by_case[result['caseId']]
        grouped[row['caseGroup']].setdefault(signature(result), row)
    return [groups[key] for _, groups in sorted(grouped.items()) for key in sorted(groups)[:3]]


def validate_plan(plan):
    data = batch.package(Path(plan['manifest']))
    workloads = {w['id']: w for w in data['records']['workloads']}
    expected_rows = []
    for index, scope in enumerate(plan['scopes'], 1):
        if scope['status'] != 'SELECTED':
            continue
        workload = scope['workload']
        if workloads.get(workload['id']) != workload:
            raise ValueError('Fixed workload changed')
        if vectors(workload) != scope['allCanonicalVectors'] or \
                sample_vectors(vectors(workload), SEED+index) != scope['selectedVectors']:
            raise ValueError('Vector selection changed')
        good = sorted(set(scope['selectedVectors']) - set(scope['failedVectors']))
        candidates = [r for r in data['records']['faultScenarios']
                      if r['workload'] == workload['id'] and r['faultVector'] in good]
        if candidates != scope['generatedCandidates']:
            raise ValueError('Generated candidate universe changed')
        selected = sample_scenarios(candidates, good, SEED+index) if candidates else []
        if [r['id'] for r in selected] != scope['selectedScenarioIds']:
            raise ValueError('Scenario sampling changed')
        expected_rows.extend({'caseId': f'{scope["caseGroup"]}-{n:02}', 'caseGroup': scope['caseGroup'],
                              'workloadId': workload['id'], 'faultScenarioId': r['id'], 'faultVector': r['faultVector']}
                             for n, r in enumerate(selected, 1))
    if expected_rows != plan['rows']:
        raise ValueError('Discovery rows changed')


def execute(args):
    plan = batch.read(args.plan)
    batch.verify(plan, check_checkout=False)
    validate_plan(plan)
    out = args.output.resolve()
    out.mkdir(parents=True)
    started = time.perf_counter()
    dependencies = batch.dependencies(Path(plan['build']), out/'dependencies-before.log')
    script_hashes = {str(p.relative_to(batch.ROOT)): batch.digest(p) for p in
                     [Path(__file__), HERE/'RequestBatch.java', HERE/'request-prepared.sh',
                      HERE.parent/'batch-execution/run.py', HERE.parent/'impact-v2-broader/run-prepared.sh',
                      HERE.parent/'impact-v2-broader/validate_assessment.py']}
    def attempt(row, repetition):
        return batch.attempt(out, plan, row, repetition, 2, plan['timeoutSeconds'], mode='assessment')
    with ThreadPoolExecutor(max_workers=2) as pool:
        discovery = list(pool.map(lambda row: attempt(row, 1), plan['rows']))
    batch.save(out/'discovery.json', discovery)
    repeat_rows = representatives(plan['rows'], discovery)
    batch.save(out/'repeat-plan.json', repeat_rows)
    with ThreadPoolExecutor(max_workers=2) as pool:
        repeats = list(pool.map(lambda row: attempt(row, 2), repeat_rows))
    batch.save(out/'repeats.json', repeats)
    batch.verify(plan, check_checkout=False)
    after = batch.dependencies(Path(plan['build']), out/'dependencies-after.log')
    if after != dependencies:
        raise ValueError('Dependency drift')
    for path, digest in script_hashes.items():
        if batch.digest(batch.ROOT/path) != digest:
            raise ValueError('Script drift: '+path)
    batch.save(out/'proof.json', {'experimentSha256': batch.digest(args.plan), 'scriptHashes': script_hashes,
                               'dependencyHashes': dependencies, 'wallSeconds': time.perf_counter()-started,
                               'snapshotVerifiedBeforeAndAfter': True})
    summarize(args.plan, out)


def summarize(plan_path, out):
    plan = batch.read(plan_path)
    batch.verify(plan, check_checkout=False)
    validate_plan(plan)
    discovery, repeats = batch.read(out/'discovery.json'), batch.read(out/'repeats.json')
    rows = {r['caseId']: r for r in plan['rows']}
    if len(discovery) != len(rows) or {r['caseId'] for r in discovery} != set(rows):
        raise ValueError('Discovery coverage mismatch')
    if batch.read(out/'repeat-plan.json') != representatives(plan['rows'], discovery):
        raise ValueError('Repeat selection changed')
    attempt_ids = []
    for result in discovery + repeats:
        directory = Path(result['directory'])
        if batch.read(directory/'attempt.json') != result:
            raise ValueError('Attempt metadata mismatch')
        for filename, digest in result['reportHashes'].items():
            if batch.digest(directory/filename) != digest:
                raise ValueError('Report hash mismatch')
        if result.get('observed'):
            e, v1, v2 = batch.reports(directory, rows[result['caseId']], Path(plan['manifest']))
            if batch.semantic(e, v1, v2) != result['observed'] or batch.assessment_result(e, v2) != (result['status'], result['score']):
                raise ValueError('Semantic/assessment mismatch')
            attempt_ids.append(e['executionAttemptId'])
        elif result['score'] is not None:
            raise ValueError('Invalid attempt retains score')
    if len(set(attempt_ids)) != len(attempt_ids):
        raise ValueError('Reused execution attempt')
    scopes = []
    csv_rows = []
    for result in discovery + repeats:
        row = rows[result['caseId']]
        observed = result.get('observed', {})
        csv_rows.append({**row, 'repetition': result['repetition'], 'status': result['status'],
                         'score': result['score'], 'terminalStatus': observed.get('terminalStatus'),
                         'scheduleConformance': observed.get('scheduleConformance'),
                         'conditions': json.dumps(observed.get('conditions'), sort_keys=True),
                         'wallSeconds': result['wallSeconds'], 'error': result.get('error'),
                         'executionAttemptId': result.get('executionAttemptId'),
                         'directory': result['directory']})
    with (out/'attempts.csv').open('w', newline='') as stream:
        writer = csv.DictWriter(stream, fieldnames=list(csv_rows[0]))
        writer.writeheader()
        writer.writerows(csv_rows)
    for scope in plan['scopes']:
        found = [r for r in discovery if rows[r['caseId']]['caseGroup'] == scope['caseGroup']]
        complete = [r for r in found if r['status'] == 'COMPLETE']
        by_vector = defaultdict(list)
        for result in complete:
            by_vector[rows[result['caseId']]['faultVector']].append(result)
        recovery_differences = {v: [r['caseId'] for r in rs] for v, rs in by_vector.items()
                                if len({signature(r) for r in rs}) > 1}
        checks = []
        for result in repeats:
            if rows[result['caseId']]['caseGroup'] == scope['caseGroup']:
                original = next(r for r in found if r['caseId'] == result['caseId'])
                checks.append({'caseId': result['caseId'], 'stableSignature': signature(original) == signature(result),
                               'stableSemantic': original.get('observed') == result.get('observed'),
                               'firstStatus': original['status'], 'repeatStatus': result['status']})
        scopes.append({'caseGroup': scope['caseGroup'], 'sagas': scope['sagas'],
                       'workloadId': scope.get('workload', {}).get('id'), 'attempts': len(found),
                       'statuses': dict(Counter(r['status'] for r in found)),
                       'scoreDistribution': dict(Counter(str(r['score']) for r in complete)),
                       'conditionSignatures': dict(Counter(json.dumps(r['observed']['conditions'], sort_keys=True) for r in complete)),
                       'distinctPositiveScenarios': sum(r['score'] > 0 for r in complete),
                       'sampledRecoveryOutcomeDifferences': recovery_differences,
                       'canonicalVectors': len(scope.get('allCanonicalVectors', [])),
                       'requestedVectors': len(scope.get('selectedVectors', [])),
                       'generatedScenarios': len(scope.get('generatedCandidates', [])),
                       'uncappedRequestedRecoveries': sum(int(r['uncappedScheduleCount']) for r in scope.get('requests', []) if r['uncappedScheduleCount'] is not None),
                       'recoveryCappedRequests': sum(int(r['uncappedScheduleCount']) > r['writtenScheduleCount'] for r in scope.get('requests', []) if r['uncappedScheduleCount'] is not None),
                       'sumAttemptSeconds': sum(r['wallSeconds'] for r in found), 'repeats': checks})
    summary = {'discoveryAttempts': len(discovery), 'repeatAttempts': len(repeats),
               'uniqueExecutionAttemptIds': len(attempt_ids), 'scopes': scopes,
               'statuses': dict(Counter(r['status'] for r in discovery)),
               'medianAttemptSeconds': statistics.median(r['wallSeconds'] for r in discovery+repeats),
               'wallSeconds': batch.read(out/'proof.json')['wallSeconds']}
    batch.save(out/'summary.json', summary)
    lines = ['# Bounded structural space map', '',
             '| Group | Sagas | Sample / generated | Complete | Positive scenarios | Scores |',
             '|---|---|---:|---:|---:|---|']
    for s in scopes:
        lines.append(f'| {s["caseGroup"]} | {" + ".join(s["sagas"])} | {s["attempts"]} / {s["generatedScenarios"]} | '
                     f'{s["statuses"].get("COMPLETE", 0)} | {s["distinctPositiveScenarios"]} | {s["scoreDistribution"]} |')
    lines += ['', 'Discovery and representative repeats are separate. Positive scenarios are not distinct defects.',
              f'Campaign: {len(discovery)} discovery + {len(repeats)} repeat attempts; {summary["wallSeconds"]:.2f} seconds.',
              'See summary.json and attempts.csv for retained failures, condition signatures, costs and repeatability.']
    (out/'map.md').write_text('\n'.join(lines)+'\n')
    print(json.dumps(summary, indent=2), flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    subs = parser.add_subparsers(dest='command', required=True)
    prep = subs.add_parser('prepare')
    for key in ('manifest', 'build', 'output'):
        prep.add_argument('--'+key, required=True, type=Path)
    run = subs.add_parser('run')
    run.add_argument('--plan', required=True, type=Path)
    run.add_argument('--output', required=True, type=Path)
    report = subs.add_parser('summarize')
    report.add_argument('--plan', required=True, type=Path)
    report.add_argument('--output', required=True, type=Path)
    args = parser.parse_args()
    if args.command == 'prepare': prepare(args)
    elif args.command == 'run': execute(args)
    else: summarize(args.plan, args.output)
