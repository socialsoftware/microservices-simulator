#!/usr/bin/env python3
"""Small fixed qualification over retained packages; never rewrites historical runs."""
import argparse
from datetime import datetime, timezone
import hashlib
import json
from pathlib import Path
import subprocess
import time

ROOT = Path(__file__).resolve().parents[3]


def read(path):
    return json.loads(path.read_text())


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def container_path(path):
    return '/reports/' + str(path.resolve().relative_to(ROOT / 'verifiers/target'))


def plan(output):
    setup = read(ROOT / 'docs/verifiers-impl/evidence/space-map-setup-qualification-2026-09-07/summary.json')
    combined = read(ROOT / 'docs/verifiers-impl/evidence/combined-events-2026-09-06/plan.json')
    positive = ROOT / 'verifiers/target/impact-v2-qualification/run-03/package'
    checks = {r['name']: r for r in setup['runtimeChecks']}
    rows = []
    for name in ['w01-no-fault', 'update-tournament-no-fault', 'update-tournament-trigger-fault']:
        rows.append(dict(case=name, manifest=setup['qualifiedPackage']['runtimeManifest'],
                         scenario=checks[name]['faultScenarioId'], fixture='source-derived', observer=True,
                         baseline=f'verifiers/target/space-map-setup-qualification-03/runtime/{name}/execution.json'))
    case = next(r for r in combined['rows'] if r['caseId'] == 'answer-quiz-missing-tournament')
    rows.append(dict(case=case['caseId'], manifest=str(Path(combined['manifest']).relative_to(ROOT)),
                     scenario=case['faultScenarioId'], fixture='source-derived', observer=True,
                     baseline='docs/verifiers-impl/evidence/combined-events-2026-09-06/answer-quiz-missing-tournament/execution.json'))
    rows.append(dict(case='unresolved-question-positive',
                     manifest=str((positive / 'scenario-catalog-manifest.json').relative_to(ROOT)),
                     scenario=read(positive / 'selection.json')['cases']['unresolvedQuestionEvent']['faultScenarioId'],
                     fixture='existing-qualification-only-provider', observer=True,
                     baseline='verifiers/target/impact-v2-qualification/run-03/event-current.execution.json'))
    rows.append(dict(rows[0], case='w01-observer-off', observer=False, baseline=None))
    packages = {}
    for row in rows:
        parent = (ROOT / row['manifest']).parent
        packages[row['manifest']] = {p.name: digest(p) for p in sorted(parent.iterdir()) if p.is_file()}
    result = dict(selection='Fixed continuation cases plus existing positive/healthy/masking controls; no new outcomes used.',
                  fixtureNow=datetime.now(timezone.utc).replace(tzinfo=None).isoformat(timespec='seconds'),
                  rows=rows, packageHashes=packages)
    output.mkdir(parents=True, exist_ok=True)
    destination = output / 'selection.json'
    if destination.exists():
        raise SystemExit('Refusing to replace selection')
    destination.write_text(json.dumps(result, indent=2) + '\n')


def execute(output):
    selection = read(output / 'selection.json')
    verify_packages(selection)
    build = output / 'prepared-build'
    for row in selection['rows']:
        case_dir = output / 'runtime' / row['case']
        case_dir.mkdir(parents=True, exist_ok=False)
        opts = '-Xmx1536m -XX:MaxMetaspaceSize=512m'
        opts += ' -Dimpact.v2.fixture-now=' + selection['fixtureNow']
        if not row['observer']:
            opts += ' -Dmicroservices.simulator.impact.enabled=false'
        env = dict(BUILD_OUTPUT_DIR=container_path(build), RUNNER_KIND='generic',
                   PACKAGE_PATH=container_path(ROOT / row['manifest']), FAULT_SCENARIO_ID=row['scenario'],
                   EXECUTION_OUTPUT_PATH=container_path(case_dir / 'execution.json'),
                   IMPACT_OUTPUT_PATH=container_path(case_dir / 'impact-v1.json'), JAVA_TOOL_OPTIONS=opts)
        command = ['docker', 'compose', '-p', 'microservices-simulator', '-f', str(ROOT / 'docker-compose.yml'),
                   'run', '--rm', '--no-deps', '--pull', 'never', '-T']
        for key, value in env.items():
            command += ['-e', key + '=' + value]
        command += ['scenario-executor', 'bash', '/verifiers/experiments/impact-v2-broader/run-prepared.sh']
        start = time.monotonic()
        with (case_dir / 'container.log').open('w') as log:
            result = subprocess.run(command, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=180)
        (case_dir / 'process.json').write_text(json.dumps(dict(exitCode=result.returncode,
             elapsedSeconds=time.monotonic() - start, command=command), indent=2) + '\n')
        print(row['case'], result.returncode, flush=True)
    verify_packages(selection)


def verify_packages(selection):
    for manifest, files in selection['packageHashes'].items():
        for filename, expected in files.items():
            assert digest((ROOT / manifest).parent / filename) == expected, filename


def summarize(output):
    selection = read(output / 'selection.json')
    verify_packages(selection)
    rows = []
    attempts = set()
    for case in selection['rows']:
        path = output / 'runtime' / case['case']
        execution = read(path / 'execution.json')
        impact = read(path / 'execution.impact-v2.json')
        assert execution['schemaVersion'] == 'microservices-simulator.scenario-execution-report.v6'
        assert read(path / 'process.json')['exitCode'] == 0
        assert execution['faultScenarioId'] == case['scenario']
        assert execution['executionAttemptId'] not in attempts
        attempts.add(execution['executionAttemptId'])
        events = [a for a in execution['actualActions'] if a['kind'] == 'EVENT_CONSEQUENCE']
        empty = [a for a in events if a['status'] == 'NO_ELIGIBLE_SUBSCRIBER']
        for action in empty:
            assert action['eventEvidence']['eventId'] is not None
            assert action['eventEvidence']['subscriberAggregateId'] is None
            assert action['bodyOutcome'] == 'NOT_RUN'
        baseline = None
        if case.get('baseline'):
            old_path = ROOT / case['baseline']
            old = read(old_path)
            assert old['faultScenarioId'] == execution['faultScenarioId']
            assert old['workloadPlanId'] == execution['workloadPlanId']
            baseline = dict(path=case['baseline'], sha256=digest(old_path),
                            terminalStatus=old['terminalStatus'], conformance=old['scheduleConformance'],
                            hardStopReason=old.get('hardStopReason'))
        rows.append(dict(case=case['case'], fixture=case['fixture'], observer=case['observer'], baseline=baseline,
             terminalStatus=execution['terminalStatus'], conformance=execution['scheduleConformance'],
             hardStopReason=execution.get('hardStopReason'), eventStatuses=[a['status'] for a in events],
             actionTrace=[[a['kind'], a['status'], a['bodyOutcome'], a['commitOutcome']]
                          for a in execution['actualActions']],
             actualDeliveries=len(impact['eventDeliveries']), emptySelections=len(empty),
             assessment=impact['assessmentStatus'], score=impact['completeScore'],
             categories={c['category']: c['positiveObjectCount'] for c in impact['categoryResults']},
             coverageGaps=len(impact['coverageGaps']),
             hashes={p.name: digest(p) for p in sorted(path.glob('*.json'))}))
    by_case = {r['case']: r for r in rows}
    for row in rows:
        assert row['terminalStatus'] in ['SUCCESS', 'COMPENSATED'], row
        assert row['conformance'] == 'EXACT' and row['hardStopReason'] is None, row
        assert row['assessment'] == ('COMPLETE' if row['observer'] else 'UNAVAILABLE'), row
    w01 = by_case['w01-no-fault']
    assert w01['emptySelections'] == 2 and w01['actualDeliveries'] == 0 and w01['score'] == 0
    mixed = by_case['answer-quiz-missing-tournament']
    assert mixed['eventStatuses'] == ['COMPLETED', 'COMPLETED', 'NO_ELIGIBLE_SUBSCRIBER']
    assert mixed['actualDeliveries'] == 2 and mixed['score'] == 0
    healthy = by_case['update-tournament-no-fault']
    assert healthy['actualDeliveries'] == 1 and healthy['score'] == 0
    masked = by_case['update-tournament-trigger-fault']
    assert masked['eventStatuses'] == ['MASKED_BY_TRIGGER_FAULT'] and masked['score'] == 0
    positive = by_case['unresolved-question-positive']
    assert positive['actualDeliveries'] == 1 and positive['score'] == 1
    assert positive['categories']['UNRESOLVED_DELIVERED_EVENT'] == 1
    off = by_case['w01-observer-off']
    assert off['score'] is None and off['actionTrace'] == w01['actionTrace']
    result = dict(schemaVersion='empty-event-delivery-qualification.v1', passed=True, attempts=len(rows), rows=rows)
    (output / 'comparison.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('action', choices=['plan', 'run', 'summarize'])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    {'plan': plan, 'run': execute, 'summarize': summarize}[args.action](args.output.resolve())
