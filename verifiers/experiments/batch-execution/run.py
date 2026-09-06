#!/usr/bin/env python3
"""Frozen selection, prepared Docker build, fresh process/database per attempt."""
import argparse
from collections import Counter
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import signal
import statistics
import subprocess
import sys
import time
import uuid

ROOT = Path(__file__).resolve().parents[3]
TARGET = ROOT / 'verifiers/target'
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'impact-v2-broader'))
from qualification import package
from validate_assessment import validate_reports

DEFAULT_CASES = ['none', 'answer', 'quiz', 'answer-quiz', 'quiz-answer',
                 'answer-quiz-trigger-fault']
REPORTS = ['execution.json', 'impact-v1.json', 'execution.impact-v2.json']
LIMITS = {'MEDIUM_MEM_LIMIT': '3g', 'MEDIUM_MEM_RESERVATION': '512m', 'MEDIUM_CPUS': '2.0'}


def read(path):
    return json.loads(path.read_text())


def save(path, value):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + '\n')
    temporary.replace(path)


def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


def container(path):
    return '/reports/' + str(path.resolve().relative_to(TARGET.resolve()))


def semantic(execution, v1, v2):
    """Compare conditions/outcomes, not attempt-local UUIDs, timestamps or object IDs."""
    return {'terminalStatus': execution['terminalStatus'],
            'scheduleConformance': execution['scheduleConformance'],
            'assessmentStatus': v2['assessmentStatus'], 'completeScore': v2['completeScore'],
            'conditions': {c['category']: c['positiveObjectCount'] for c in v2['categoryResults']},
            'impactV1Status': v1['evaluationStatus'], 'impactV1Score': v1['impactScore'],
            'actions': [[a['kind'], a['status'], a['bodyOutcome'], a['commitOutcome']]
                        for a in execution['actualActions']],
            'finalLifecycleCounts': dict(Counter(
                s['identity']['aggregateType'] + ':' + s['lifecycleState'] for s in v2['finalState']))}


def reports(directory, row, manifest):
    execution, v1, v2 = [read(directory / name) for name in REPORTS]
    validate_reports(execution, v1, v2, row['workloadId'], row['faultScenarioId'], row['faultVector'])
    if execution['schemaVersion'] != 'microservices-simulator.scenario-execution-report.v5':
        raise ValueError('Wrong execution schema')
    if v1.get('schemaVersion') != 'microservices-simulator.scenario-impact-report.v1':
        raise ValueError('Wrong ImpactV1 schema')
    if execution['packageManifestPath'] != container(manifest):
        raise ValueError('Report does not identify the selected package')
    return execution, v1, v2


def source_hashes():
    hashes = {}
    for src, dest in [('simulator', 'simulator'), ('verifiers', 'verifiers'),
                      ('applications/quizzes', 'quizzes')]:
        for file in (ROOT / src).rglob('*'):
            rel = file.relative_to(ROOT / src)
            if file.is_file() and not set(rel.parts) & {'target', '.git', 'logs', 'experiments', '__pycache__'} and file.suffix != '.pyc':
                hashes[str(Path(dest) / rel)] = digest(file)
    return hashes


def build_hashes(build):
    return {str(p.relative_to(build)): digest(p) for p in sorted(build.rglob('*')) if p.is_file()}


def compose(name, env, command):
    cmd = ['docker', 'compose', '-p', 'microservices-simulator', '-f', str(ROOT / 'docker-compose.yml'),
           'run', '--rm', '--no-deps', '--pull', 'never', '-T', '--name', name]
    for key, value in env.items():
        cmd += ['-e', key + '=' + str(value)]
    return cmd + ['scenario-executor'] + command


def run_process(command, log, timeout, cleanup=None):
    """Bound the CLI and terminate only this attempt's named container on timeout."""
    started = time.perf_counter()
    save(log.with_suffix('.command.json'), command)
    result = {'status': 'PROCESS_FAILURE', 'exitCode': None}
    with log.open('w') as stream:
        process = subprocess.Popen(command, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT,
                                   start_new_session=True, env={**os.environ, **LIMITS})
        try:
            result['exitCode'] = process.wait(timeout=timeout)
            result['status'] = 'EXITED' if result['exitCode'] == 0 else 'PROCESS_FAILURE'
        except subprocess.TimeoutExpired:
            result['status'] = 'TIMEOUT'
            if cleanup:
                try:
                    result['cleanup'] = cleanup()
                except Exception as failure:
                    result['cleanup'] = str(failure)
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
        except BaseException:
            if cleanup:
                cleanup()
            os.killpg(process.pid, signal.SIGKILL)
            process.wait()
            raise
    result['wallSeconds'] = time.perf_counter() - started
    return result


def remove_container(name):
    result = subprocess.run(['docker', 'rm', '-f', name], capture_output=True, text=True, timeout=20)
    return {'exitCode': result.returncode, 'output': result.stdout + result.stderr}


def dependencies(build, output):
    """Fingerprint the external Maven jars actually referenced by the frozen classpaths."""
    name = 'batch-check-' + uuid.uuid4().hex[:12]
    command = compose(name, {'BUILD_OUTPUT_DIR': container(build)}, ['bash', '-c',
        'set -euo pipefail; java -version; '
        'printf "%s\\n%s\\n" "$(cat "$BUILD_OUTPUT_DIR/quizzes-classpath.txt")" "$(cat "$BUILD_OUTPUT_DIR/verifiers-classpath.txt")" '
        '| tr ":\\n" "\\n" | sort -u | while IFS= read -r jar; do '
        'if [ -n "$jar" ]; then sha256sum "$jar"; fi; done'])
    result = run_process(command, output, 120, lambda: remove_container(name))
    if result['status'] != 'EXITED':
        raise ValueError('Dependency fingerprint failed: ' + str(result))
    # Ignore Docker progress lines; retain exact Java version and jar hashes.
    lines = output.read_text().splitlines()
    hashes = sorted(line for line in lines if len(line) > 66 and line[64:66] == '  '
                    and all(c in '0123456789abcdef' for c in line[:64]))
    verify_prepared_jars(build, hashes)
    return hashes


def verify_prepared_jars(build, hashes):
    """The shared Maven cache must use this snapshot's simulator/verifier artifacts."""
    for module in ('simulator', 'verifiers'):
        matched = False
        for line in hashes:
            jar = build / 'source' / module / 'target' / Path(line[66:]).name
            if jar.is_file():
                matched = True
                if digest(jar) != line[:64]:
                    raise ValueError('Maven cache differs from prepared artifact: ' + str(jar))
        if not matched:
            raise ValueError('No prepared dependency matched for ' + module)


def freeze(args):
    reference = args.reference_run.resolve()
    old = read(reference / 'plan.json')
    manifest = Path(old['manifest'])
    build = reference / 'prepared-build'
    data = package(manifest)
    if data['hashes']['manifest'] != old['manifestSha256']:
        raise ValueError('Reference package changed; regenerate, do not migrate selections')
    hashes = read(reference / 'source-hashes.json')
    if hashes != source_hashes():
        raise ValueError('Current sources differ from prepared build provenance; rebuild/regenerate')
    for rel, expected in hashes.items():
        if digest(build / 'source' / rel) != expected:
            raise ValueError('Prepared source changed: ' + rel)
    if (build / 'status').read_text().strip() != 'ready':
        raise ValueError('Build is not ready')
    rows = []
    if len(set(args.cases)) != len(args.cases):
        raise ValueError('Duplicate case selection')
    for case in args.cases:
        matches = [r for r in old['rows'] if r['caseId'] == case]
        if len(matches) != 1:
            raise ValueError('Case must exist exactly once: ' + case)
        row = matches[0].copy()
        directory = reference / row['cohort'] / 'attempts' / case
        execution, v1, v2 = reports(directory, row, manifest)
        if v2['assessmentStatus'] != 'COMPLETE':
            raise ValueError('Selection requires a previously complete case: ' + case)
        row['expected'] = semantic(execution, v1, v2)
        row['referenceReportHashes'] = {name: digest(directory / name) for name in REPORTS}
        rows.append(row)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    if args.output.exists():
        raise ValueError('Refusing to overwrite selection')
    save(args.output, {'schema': 'prepared-batch-selection.v1', 'referenceRun': str(reference),
        'manifest': str(manifest), 'build': str(build), 'packageHashes': data['hashes'],
        'sourceHashes': hashes, 'buildHashes': build_hashes(build), 'rows': rows,
        'selectionRule': 'Explicit known cases and prior semantic expectations frozen before new outcomes.'})
    print(f'Frozen {len(rows)} cases: {args.output}', flush=True)


def attempt(output, selection, row, repetition, concurrency, timeout):
    started = time.perf_counter()
    directory = output / f'c{concurrency}' / f'r{repetition:02d}-{row["caseId"]}'
    directory.mkdir(parents=True)
    name = 'batch-' + uuid.uuid4().hex
    result = {'caseId': row['caseId'], 'repetition': repetition, 'concurrency': concurrency,
              'containerName': name, 'startedAt': datetime.now(timezone.utc).isoformat(),
              'directory': str(directory), 'status': 'RUNNING', 'score': None}
    save(directory / 'attempt.json', result)
    env = {'BUILD_OUTPUT_DIR': container(Path(selection['build'])), 'RUNNER_KIND': 'generic',
           'JAVA_TOOL_OPTIONS': '-Xmx1536m -XX:MaxMetaspaceSize=512m',
           'PACKAGE_PATH': container(Path(selection['manifest'])), 'FAULT_SCENARIO_ID': row['faultScenarioId'],
           'EXECUTION_OUTPUT_PATH': container(directory / REPORTS[0]),
           'IMPACT_OUTPUT_PATH': container(directory / REPORTS[1])}
    command = compose(name, env, ['bash', '/verifiers/experiments/impact-v2-broader/run-prepared.sh'])
    try:
        result.update(run_process(command, directory / 'docker.log', timeout, lambda: remove_container(name)))
        if result['status'] == 'EXITED':
            result['status'] = 'INVALID_REPORT'
            execution, v1, v2 = reports(directory, row, Path(selection['manifest']))
            result['executionAttemptId'] = execution['executionAttemptId']
            result['observed'] = semantic(execution, v1, v2)
            result['status'] = 'PASS' if result['observed'] == row['expected'] else 'SEMANTIC_DIVERGENCE'
            result['score'] = v2['completeScore']
            result['sourceSetupSeconds'] = execution['sourceSetup']['durationNanos'] / 1e9
            result['prerequisiteSetupSeconds'] = execution['prerequisiteSetup']['durationNanos'] / 1e9
    except Exception as failure:
        if result['status'] == 'RUNNING':
            result['status'] = 'INFRASTRUCTURE_FAILURE'
        result['error'] = str(failure)
    result.setdefault('wallSeconds', time.perf_counter() - started)
    result['reportHashes'] = {name: digest(directory / name) for name in REPORTS if (directory / name).is_file()}
    save(directory / 'attempt.json', result)
    print(f'c{concurrency} r{repetition} {row["caseId"]}: {result["status"]} {result.get("wallSeconds", 0):.2f}s', flush=True)
    return result


def verify(selection):
    if source_hashes() != selection['sourceHashes']:
        raise ValueError('Source drift')
    if package(Path(selection['manifest']))['hashes'] != selection['packageHashes']:
        raise ValueError('Package drift')
    if build_hashes(Path(selection['build'])) != selection['buildHashes']:
        raise ValueError('Prepared build drift')


def run(args):
    started = time.perf_counter()
    selection = read(args.selection)
    if selection['schema'] != 'prepared-batch-selection.v1' or not selection['rows']:
        raise ValueError('Empty/unsupported selection')
    args.output = args.output.resolve()
    container(args.output)  # Require mounted output before any launch.
    args.output.mkdir(parents=True, exist_ok=False)
    save(args.output / 'selection.json', selection)
    verify(selection)
    image = subprocess.check_output(['docker', 'image', 'inspect', 'scenario-executor:latest',
                                     '--format', '{{.Id}}'], text=True).strip()
    deps = dependencies(Path(selection['build']), args.output / 'dependencies-before.log')
    if not deps:
        raise ValueError('No dependency hashes captured')
    preparation = time.perf_counter() - started
    plan = {'selectionSha256': digest(args.output / 'selection.json'), 'concurrency': args.concurrency,
            'repetitions': args.repetitions, 'timeoutSeconds': args.timeout,
            'plannedAttempts': len(selection['rows']) * args.repetitions * len(args.concurrency),
            'globalTimeLimit': None, 'retries': 0, 'resourceLimitsPerContainer': LIMITS,
            'imageId': image, 'dependencyHashes': deps, 'preparationSeconds': preparation,
            'buildAndGeneration': 'Reused, not timed in this campaign',
            'launcherSha256': digest(ROOT / 'verifiers/experiments/impact-v2-broader/run-prepared.sh'),
            'startedAt': datetime.now(timezone.utc).isoformat()}
    save(args.output / 'plan.json', plan)
    results, groups = [], []
    for concurrency in args.concurrency:
        group_start = time.perf_counter()
        jobs = [(row, repetition) for repetition in range(1, args.repetitions + 1) for row in selection['rows']]
        with ThreadPoolExecutor(max_workers=concurrency) as pool:
            futures = [pool.submit(attempt, args.output, selection, row, repetition, concurrency, args.timeout)
                       for row, repetition in jobs]
            group_results = [future.result() for future in futures]
        elapsed = time.perf_counter() - group_start
        results.extend(group_results)
        groups.append({'concurrency': concurrency, 'wallSeconds': elapsed,
                       'attempts': len(group_results), 'statuses': dict(Counter(r['status'] for r in group_results)),
                       'attemptsPerHour': len(group_results) * 3600 / elapsed,
                       'validCompletePerHour': sum(r['status'] == 'PASS' for r in group_results) * 3600 / elapsed,
                       'medianAttemptSeconds': statistics.median([r['wallSeconds'] for r in group_results if 'wallSeconds' in r])
                           if any('wallSeconds' in r for r in group_results) else None})
        save(args.output / 'results.json', {'groups': groups, 'attempts': results, 'validation': 'IN_PROGRESS'})
    errors = []
    try:
        verify(selection)
        if dependencies(Path(selection['build']), args.output / 'dependencies-after.log') != deps:
            errors.append('Dependency drift')
        if subprocess.check_output(['docker', 'image', 'inspect', 'scenario-executor:latest', '--format', '{{.Id}}'], text=True).strip() != image:
            errors.append('Image drift')
        if digest(ROOT / 'verifiers/experiments/impact-v2-broader/run-prepared.sh') != plan['launcherSha256']:
            errors.append('Launcher drift')
    except Exception as failure:
        errors.append(str(failure))
    ids = [r['executionAttemptId'] for r in results if 'executionAttemptId' in r]
    if len(set(ids)) != len(ids):
        errors.append('Duplicate execution attempt identities')
    passed = not errors and all(r['status'] == 'PASS' for r in results)
    save(args.output / 'results.json', {'validation': 'PASS' if passed else 'FAIL',
         'integrityErrors': errors, 'planSha256': digest(args.output / 'plan.json'),
         'totalWallSeconds': time.perf_counter() - started, 'groups': groups, 'attempts': results})
    return 0 if passed else 1


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest='command', required=True)
    f = commands.add_parser('freeze')
    f.add_argument('--reference-run', type=Path, required=True)
    f.add_argument('--cases', nargs='+', default=DEFAULT_CASES)
    f.add_argument('--output', type=Path, required=True)
    r = commands.add_parser('run')
    r.add_argument('--selection', type=Path, required=True)
    r.add_argument('--output', type=Path, required=True)
    r.add_argument('--concurrency', type=int, nargs='+', choices=[1, 2], default=[1, 2])
    r.add_argument('--repetitions', type=int, default=3)
    r.add_argument('--timeout', type=float, default=180)
    args = parser.parse_args()
    if args.command == 'freeze':
        freeze(args)
        return 0
    if args.repetitions < 1 or args.timeout <= 0 or len(set(args.concurrency)) != len(args.concurrency):
        parser.error('Positive repetitions/timeout and distinct concurrency values required')
    return run(args)


if __name__ == '__main__':
    sys.exit(main())
