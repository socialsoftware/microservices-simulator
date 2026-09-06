#!/usr/bin/env python3
"""Online budgeted deterministic/random baselines within fixed workload horizons."""
import argparse
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
import json
import math
from pathlib import Path
import platform
import random
import shutil
import subprocess
import sys
import time
import uuid

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'batch-execution'))
import run as batch
from qualification import canonical_vectors, package, select_workload

SEEDS = [11, 29, 47, 71, 101]
WORKLOADS = [
    ('benchmark', '435ac86830d1ad7d778abe6dd4e046f728c9f3fd695106657aa430bc2d1710fc', 29, 12),
    ('create-quiz', 'a8aa42c2fc0d5b41eb5e644a6fda8fbcba2ff3b199223cc549106b5ef896e5c7', 4, 4),
    ('anonymize-event', 'c2559b797958f4f0c185007cedcc1da6a2eb1b76ef558a593d12d699dec9f60b', 3, 3)]


def orders(rows, budget, seeds):
    if not rows or type(budget) is not int or not 1 <= budget <= len(rows):
        raise ValueError('Budget must be between 1 and the eligible candidate count')
    ids = [r['faultScenarioId'] for r in sorted(rows, key=lambda r: (r['faultVector'], r['faultScenarioId']))]
    if len(set(ids)) != len(ids):
        raise ValueError('Duplicate eligible candidate')
    if not seeds or len(set(seeds)) != len(seeds) or any(type(s) is not int for s in seeds):
        raise ValueError('Explicit distinct integer seeds required')
    runs = [{'strategy': 'deterministic', 'seed': None, 'id': 'deterministic', 'order': ids}]
    for seed in seeds:
        shuffled = ids.copy()
        random.Random(seed).shuffle(shuffled)
        runs.append({'strategy': 'random', 'seed': seed, 'id': f'random-{seed}', 'order': shuffled})
    return [{**run, 'budget': budget, 'selected': run['order'][:budget]} for run in runs]


def prepare(args):
    start = time.perf_counter()
    output = args.output.resolve()
    batch.container(output)
    output.mkdir(parents=True, exist_ok=False)
    snapshot = batch.read(args.snapshot_selection)
    batch.verify(snapshot, check_checkout=False)
    old = package(Path(snapshot['manifest']))
    folder = output / 'package'
    folder.mkdir()
    for path in [old['manifestPath'], *old['paths'].values()]:
        shutil.copyfile(path, folder / path.name)
    manifest = folder / old['manifestPath'].name
    data = package(manifest)
    workload = select_workload(data)
    existing = {s['faultVector'] for s in data['records']['faultScenarios'] if s['workload'] == workload['id']}
    missing = sorted(set(canonical_vectors(workload)) - existing)
    # Declare every request before it can affect the copied package.
    batch.save(output / 'requests.json', {'workloadId': workload['id'], 'missingVectors': missing,
        'sourcePackageHashes': old['hashes'], 'recoveryScheduleCap': 20})
    for vector in missing:
        name = 'baseline-request-' + uuid.uuid4().hex[:12]
        command = batch.compose(name, {'BUILD_OUTPUT_DIR': batch.container(Path(snapshot['build'])),
            'JAVA_TOOL_OPTIONS': '-Xmx1536m -XX:MaxMetaspaceSize=512m',
            'PACKAGE_PATH': batch.container(manifest), 'WORKLOAD_PLAN_ID': workload['id'],
            'FAULT_VECTOR': vector}, ['bash', '/verifiers/experiments/search-baselines/request-prepared.sh'])
        result = batch.run_process(command, output / f'request-{vector}.log', 180, lambda: batch.remove_container(name))
        batch.save(output / f'request-{vector}.json', result)
        if result['status'] != 'EXITED':
            raise ValueError('Request failed: ' + vector)
    data = package(manifest)
    scopes = []
    for name, wid, count, budget in WORKLOADS:
        workload = next(w for w in data['records']['workloads'] if w['id'] == wid)
        candidates = [s for s in data['records']['faultScenarios'] if s['workload'] == wid]
        if len(candidates) != count:
            raise ValueError(f'{name}: expected structural universe {count}, found {len(candidates)}')
        keys = {(s['faultVector'], json.dumps(s['actions'], sort_keys=True)) for s in candidates}
        if len(keys) != len(candidates):
            raise ValueError('Duplicate vector/action sequence in ' + name)
        rows = [{'caseId': s['id'], 'faultScenarioId': s['id'], 'workloadId': wid,
                 'faultVector': s['faultVector'], 'actions': s['actions']} for s in candidates]
        if name == 'benchmark':
            # Structural identity only: this selection has no score labels.
            known_ids = {r['faultScenarioId'] for r in batch.read(args.benchmark_selection)['rows']}
            if known_ids != {r['faultScenarioId'] for r in rows}:
                raise ValueError('Current benchmark differs from corrected 29-scenario structural reference')
        scopes.append({'name': name, 'workloadId': wid, 'setupId': workload['setup'],
            'participants': workload['participants'], 'schedule': workload['schedule'],
            'candidateCount': len(rows), 'vectors': sorted({r['faultVector'] for r in rows}),
            'candidateRows': sorted(rows, key=lambda r: (r['faultVector'], r['faultScenarioId'])),
            'runs': orders(rows, budget, args.seeds)})
    batch.verify(snapshot, check_checkout=False)
    frozen = {k: snapshot[k] for k in ('build', 'buildHashes', 'sourceHashes')}
    frozen.update({'schema': 'fixed-workload-search.v1', 'manifest': str(manifest), 'packageHashes': data['hashes'],
        'snapshotSelection': str(args.snapshot_selection.resolve()), 'snapshotSelectionSha256': batch.digest(args.snapshot_selection),
        'scopes': scopes, 'seeds': args.seeds, 'python': platform.python_version(),
        'deterministicOrdering': '(faultVector, faultScenarioId)', 'randomOrdering': 'random.Random(seed).shuffle(deterministicOrder)',
        'plannedAttempts': sum(r['budget'] for s in scopes for r in s['runs']),
        'positiveDefinition': 'valid completed execution; ImpactV2 COMPLETE and completeScore > 0',
        'acceptedConformance': ['EXACT', 'DEVIATED'], 'timeoutSeconds': 180, 'maxConcurrentStrategyRuns': 2,
        'selectionReason': 'Known corrected two-participant recovery benchmark; previously setup-qualified creation and fixed-event update workloads. No new scores used.',
        'universeBoundary': 'All persisted scenarios for these three exact workloads after requesting only missing canonical benchmark vectors. Single workloads use zero and single-point faults. Benchmark permits at most one fault per participant; recovery cap 20. Catalogue caps are inherited and recorded in package accounting; no full binary/catalogue claim.',
        'preparationSeconds': time.perf_counter() - start})
    batch.save(output / 'experiment.json', frozen)
    print(f'Frozen {len(scopes)} scopes, {sum(s["candidateCount"] for s in scopes)} candidates, {frozen["plannedAttempts"]} online attempts', flush=True)


def discovery(attempts):
    positives, curve, first = set(), [], None
    cumulative = 0.0
    for index, attempt in enumerate(attempts, 1):
        cumulative += attempt['wallSeconds']
        positive = attempt['status'] == 'COMPLETE' and attempt['score'] is not None and attempt['score'] > 0
        if positive:
            positives.add(attempt['caseId'])
            if first is None:
                first = index
        curve.append({'attempts': index, 'distinctPositiveScenarios': len(positives),
                      'processWallSeconds': cumulative, 'status': attempt['status']})
    return {'firstPositiveAttempt': first,
            'firstPositiveResult': 'FOUND' if first is not None else 'NOT_FOUND_WITHIN_BUDGET',
            'firstPositiveProcessWallSeconds': curve[first - 1]['processWallSeconds'] if first else None,
            'distinctPositiveScenarioIds': sorted(positives), 'curve': curve,
            'statuses': dict(Counter(a['status'] for a in attempts)),
            'assessments': dict(Counter(a.get('observed', {}).get('assessmentStatus', 'NO_VALID_REPORT') for a in attempts))}


def check_plan(experiment):
    if experiment.get('schema') != 'fixed-workload-search.v1':
        raise ValueError('Unsupported experiment schema')
    if not math.isfinite(experiment['timeoutSeconds']) or experiment['timeoutSeconds'] <= 0 \
            or experiment['maxConcurrentStrategyRuns'] not in (1, 2):
        raise ValueError('Finite positive timeout and concurrency 1/2 required')
    if experiment['acceptedConformance'] != ['EXACT', 'DEVIATED']:
        raise ValueError('Unsupported conformance policy')
    if len({s['workloadId'] for s in experiment['scopes']}) != len(experiment['scopes']):
        raise ValueError('Workloads must have distinct scopes')
    for scope in experiment['scopes']:
        expected = orders(scope['candidateRows'], scope['runs'][0]['budget'], experiment['seeds'])
        if scope['runs'] != expected:
            raise ValueError('Frozen strategy order/budget changed')
        if any(r['workloadId'] != scope['workloadId'] for r in scope['candidateRows']):
            raise ValueError('Mixed workload universe')
    if experiment['plannedAttempts'] != sum(r['budget'] for s in experiment['scopes'] for r in s['runs']):
        raise ValueError('Planned attempt count mismatch')


def check_package(experiment):
    data = package(Path(experiment['manifest']))
    for scope in experiment['scopes']:
        workload = next(w for w in data['records']['workloads'] if w['id'] == scope['workloadId'])
        if workload['setup'] != scope['setupId'] or workload['schedule'] != scope['schedule']:
            raise ValueError('Workload setup/horizon differs from frozen package')
        actual = {s['id']: (s['faultVector'], s['actions']) for s in data['records']['faultScenarios']
                  if s['workload'] == scope['workloadId']}
        declared = {r['faultScenarioId']: (r['faultVector'], r['actions']) for r in scope['candidateRows']}
        if actual != declared:
            raise ValueError('Eligible universe differs from declared persisted workload scenarios')


def run_arm(output, experiment, scope, strategy):
    started = time.perf_counter()
    directory = output / scope['name'] / strategy['id']
    directory.mkdir(parents=True)
    batch.save(directory / 'plan.json', strategy)
    rows = {r['faultScenarioId']: r for r in scope['candidateRows']}
    attempts = []
    for rank, scenario_id in enumerate(strategy['selected'], 1):
        result = batch.attempt(directory, experiment, rows[scenario_id], rank, 1,
                               experiment['timeoutSeconds'], mode='assessment')
        attempts.append(result)
        batch.save(directory / 'results.json', {'scope': scope['name'], 'strategy': strategy['id'],
            'attempts': attempts, 'completed': len(attempts) == strategy['budget'],
            'wallSeconds': time.perf_counter() - started, **discovery(attempts)})
    print(f'{scope["name"]}/{strategy["id"]}: first={discovery(attempts)["firstPositiveAttempt"]}, '
          f'positives={len(discovery(attempts)["distinctPositiveScenarioIds"])}', flush=True)
    return str(directory)


def run(args):
    started = time.perf_counter()
    experiment = batch.read(args.experiment)
    check_plan(experiment)
    output = args.output.resolve()
    batch.container(output)
    output.mkdir(parents=True, exist_ok=False)
    batch.save(output / 'experiment.json', experiment)
    batch.verify(experiment, check_checkout=False)
    check_package(experiment)
    dependencies = batch.dependencies(Path(experiment['build']), output / 'dependencies-before.log')
    image = subprocess.check_output(['docker', 'image', 'inspect', 'scenario-executor:latest', '--format', '{{.Id}}'], text=True).strip()
    scripts = [Path(__file__), Path(batch.__file__), batch.ROOT / 'verifiers/experiments/impact-v2-broader/run-prepared.sh',
               batch.ROOT / 'verifiers/experiments/impact-v2-broader/validate_assessment.py']
    context = {'experimentSha256': batch.digest(output / 'experiment.json'), 'imageId': image,
        'dependencies': dependencies, 'scriptHashes': {str(p): batch.digest(p) for p in scripts},
        'checkoutRevisionAtStart': subprocess.check_output(['git', 'rev-parse', 'HEAD'], text=True).strip(),
        'resourceLimits': batch.LIMITS, 'snapshotAuthoritative': True}
    batch.save(output / 'context.json', context)
    with ThreadPoolExecutor(max_workers=experiment['maxConcurrentStrategyRuns']) as pool:
        futures = [pool.submit(run_arm, output, experiment, scope, strategy)
                   for scope in experiment['scopes'] for strategy in scope['runs']]
        directories = [f.result() for f in futures]
    errors = []
    try:
        batch.verify(experiment, check_checkout=False)
        if batch.dependencies(Path(experiment['build']), output / 'dependencies-after.log') != dependencies:
            errors.append('Maven dependency drift')
        if subprocess.check_output(['docker', 'image', 'inspect', 'scenario-executor:latest', '--format', '{{.Id}}'], text=True).strip() != image:
            errors.append('Image drift')
        for path, expected in context['scriptHashes'].items():
            if batch.digest(Path(path)) != expected:
                errors.append('Runner/validator drift: ' + path)
    except Exception as failure:
        errors.append(str(failure))
    current = batch.source_hashes()
    changed = sorted(k for k in current.keys() | experiment['sourceHashes'].keys() if current.get(k) != experiment['sourceHashes'].get(k))
    batch.save(output / 'completion.json', {'completed': True, 'armDirectories': directories,
        'wallSeconds': time.perf_counter() - started, 'integrityErrors': errors,
        'checkoutSourceDifferencesFromSnapshot': changed,
        'checkoutDifferencePolicy': 'Informational; measured snapshot hashes remain authoritative.'})
    summary = summarize(output)
    return 0 if summary['validation'] == 'PASS' else 1


def summarize(output):
    experiment, context, completion = [batch.read(output / name) for name in ('experiment.json', 'context.json', 'completion.json')]
    check_plan(experiment)
    check_package(experiment)
    if batch.digest(output / 'experiment.json') != context['experimentSha256']:
        raise ValueError('Experiment identity changed')
    identities, summaries, repeatability = set(), [], []
    retained = 0
    for scope in experiment['scopes']:
        repeated = defaultdict(list)
        for strategy in scope['runs']:
            directory = output / scope['name'] / strategy['id']
            if batch.read(directory / 'plan.json') != strategy:
                raise ValueError('Arm plan drift')
            arm = batch.read(directory / 'results.json')
            attempts = arm['attempts']
            if [a['caseId'] for a in attempts] != strategy['selected']:
                raise ValueError('Attempt order/count differs from frozen budget prefix')
            for attempt in attempts:
                base = Path(attempt['directory'])
                if batch.read(base / 'attempt.json') != attempt:
                    raise ValueError('Attempt evidence mismatch')
                if attempt['status'] == 'COMPLETE' and 'observed' not in attempt:
                    raise ValueError('Complete attempt without validated report evidence')
                if attempt['status'] != 'COMPLETE' and attempt['score'] is not None:
                    raise ValueError('Unavailable assessment cannot have a search score')
                for name, expected in attempt['reportHashes'].items():
                    if batch.digest(base / name) != expected:
                        raise ValueError('Report hash mismatch')
                if 'observed' in attempt:
                    row = next(r for r in scope['candidateRows'] if r['faultScenarioId'] == attempt['caseId'])
                    execution, v1, v2 = batch.reports(base, row, Path(experiment['manifest']))
                    if batch.semantic(execution, v1, v2) != attempt['observed']:
                        raise ValueError('Observed semantic evidence mismatch')
                    if batch.assessment_result(execution, v2) != (attempt['status'], attempt['score']):
                        raise ValueError('Search assessment/score differs from validated reports')
                    identifier = execution['executionAttemptId']
                    if identifier in identities:
                        raise ValueError('Reused execution attempt ID')
                    identities.add(identifier)
                repeated[attempt['caseId']].append(attempt)
            retained += len(attempts)
            derived = discovery(attempts)
            if any(arm[k] != value for k, value in derived.items()):
                raise ValueError('Discovery summary mismatch')
            summaries.append({'scope': scope['name'], 'workloadId': scope['workloadId'], 'strategy': strategy['strategy'],
                'seed': strategy['seed'], 'budget': strategy['budget'], 'wallSeconds': arm['wallSeconds'],
                'evidence': str(directory / 'results.json'), **derived})
        for row in scope['candidateRows']:
            actual = repeated[row['faultScenarioId']]
            observed = [json.dumps(a['observed'], sort_keys=True) for a in actual if 'observed' in a]
            repeatability.append({'scope': scope['name'], 'scenarioId': row['faultScenarioId'],
                'attemptCount': len(actual), 'completeCount': sum(a['status'] == 'COMPLETE' for a in actual),
                'semanticVariants': len(set(observed)),
                'status': 'UNOBSERVED' if not actual else 'INSUFFICIENT_REPEATS' if len(actual) < 2 else
                    'STABLE_COMPLETE' if len(set(observed)) == 1 and all(a['status'] == 'COMPLETE' for a in actual)
                    else 'DIVERGENT_OR_INCOMPLETE'})
    invalid = sum(n for arm in summaries for status, n in arm['statuses'].items() if status != 'COMPLETE')
    divergent = any(r['status'] == 'DIVERGENT_OR_INCOMPLETE' for r in repeatability)
    passed = not completion['integrityErrors'] and retained == experiment['plannedAttempts'] and not invalid and not divergent
    summary = {'validation': 'PASS' if passed else 'COMPLETED_WITH_ISSUES', 'plannedAttempts': experiment['plannedAttempts'],
        'retainedAttempts': retained, 'uniqueExecutionAttemptIds': len(identities), 'nonCompleteAttempts': invalid,
        'wallSeconds': completion['wallSeconds'], 'integrityErrors': completion['integrityErrors'],
        'arms': summaries, 'repeatability': repeatability,
        'meaning': 'Distinct positive FaultScenarios within each fixed workload; not severity or distinct defects. All attempts are new online executions.'}
    batch.save(output / 'summary.json', summary)
    lines = ['# Online discovery by attempt budget', '', '| Workload | Strategy | Seed | Budget | First positive | Positive scenarios | COMPLETE | Wall s |',
             '| --- | --- | --- | ---: | --- | ---: | ---: | ---: |']
    for arm in summaries:
        first = arm['firstPositiveAttempt'] if arm['firstPositiveAttempt'] is not None else 'not found'
        lines.append(f'| {arm["scope"]} | {arm["strategy"]} | {arm["seed"]} | {arm["budget"]} | {first} | '
            f'{len(arm["distinctPositiveScenarioIds"])} | {arm["statuses"].get("COMPLETE", 0)} | {arm["wallSeconds"]:.2f} |')
    for scope in experiment['scopes']:
        selected = [a for a in summaries if a['scope'] == scope['name']]
        lines += ['', '## ' + scope['name'], '', 'Cumulative distinct positive scenario IDs; every attempt consumes budget.', '',
                  '| Budget | ' + ' | '.join(a['strategy'] + (f' {a["seed"]}' if a['seed'] is not None else '') for a in selected) + ' |',
                  '| ---: | ' + ' | '.join('---:' for _ in selected) + ' |']
        for i in range(selected[0]['budget']):
            lines.append('| ' + str(i + 1) + ' | ' + ' | '.join(str(a['curve'][i]['distinctPositiveScenarios']) for a in selected) + ' |')
    (output / 'discovery.md').write_text('\n'.join(lines) + '\n')
    print(f'{summary["validation"]}: {retained} real attempts, {invalid} non-complete', flush=True)
    return summary


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    p = commands.add_parser('prepare')
    p.add_argument('--snapshot-selection', type=Path, required=True)
    p.add_argument('--benchmark-selection', type=Path, required=True)
    p.add_argument('--seeds', type=int, nargs='+', default=SEEDS)
    p.add_argument('--output', type=Path, required=True)
    r = commands.add_parser('run')
    r.add_argument('--experiment', type=Path, required=True)
    r.add_argument('--output', type=Path, required=True)
    s = commands.add_parser('summarize')
    s.add_argument('--run', type=Path, required=True)
    args = parser.parse_args()
    if args.command == 'prepare':
        prepare(args)
        return 0
    if args.command == 'run':
        return run(args)
    return 0 if summarize(args.run.resolve())['validation'] == 'PASS' else 1


if __name__ == '__main__':
    sys.exit(main())
