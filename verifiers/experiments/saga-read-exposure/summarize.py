#!/usr/bin/env python3
"""Audit a completed fixed matrix; retain exact comparison differences and all timings."""
import argparse
import hashlib
import json
from pathlib import Path
import statistics


def read(path):
    return json.loads(path.read_text())


def normalize(value, path=(), changes=None, attempt=None):
    # Application data is compared verbatim, including its dates and relationships.
    changes = [] if changes is None else changes
    if isinstance(value, dict):
        result = {}
        for key, item in value.items():
            p = path + (key,)
            if 'applicationData' not in path and (key == 'executionAttemptId' or
                    (key == 'creationTimestamp' and path and path[-1] == 'frameworkMetadata')):
                changes.append({'path': '/'.join(map(str, p)), 'original': item})
                result[key] = '<volatile metadata>'
            elif path == ('sourceSetup',) and key == 'durationNanos':
                changes.append({'path': '/'.join(map(str, p)), 'original': item, 'reason': 'measured setup duration, outside action timing'})
                result[key] = '<measured setup duration>'
            elif path and path[0] == 'sourceSetup' and key == 'retainedResultId' and attempt \
                    and isinstance(item, str) and item.startswith(attempt + ':'):
                changes.append({'path': '/'.join(map(str, p)), 'original': item, 'reason': 'attempt namespace; retained setup action suffix preserved'})
                result[key] = '<volatile metadata>:' + item[len(attempt) + 1:]
            elif 'applicationData' not in path and key == 'exceptionMessage' and attempt and isinstance(item, str) \
                    and value.get('exceptionClass') == 'pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorInjectedFaultException' \
                    and item.startswith('Injected fault for scenario execution ' + attempt + ', plan '):
                changes.append({'path': '/'.join(map(str, p)), 'original': item, 'reason': 'attempt identifier in framework fault message'})
                result[key] = item.replace('Injected fault for scenario execution ' + attempt + ', plan ',
                                          'Injected fault for scenario execution <volatile metadata>, plan ', 1)
            else:
                result[key] = normalize(item, p, changes, attempt)
        return result
    if isinstance(value, list):
        return [normalize(item, path + (i,), changes, attempt) for i, item in enumerate(value)]
    return value


def differences(left, right, path=''):
    if type(left) is not type(right):
        return [{'path': path, 'left': left, 'right': right}]
    if isinstance(left, dict):
        result = []
        for key in sorted(left.keys() | right.keys()):
            if key not in left or key not in right:
                result.append({'path': path + '/' + key, 'left': left.get(key), 'right': right.get(key)})
            else:
                result.extend(differences(left[key], right[key], path + '/' + key))
        return result
    if isinstance(left, list):
        if len(left) != len(right):
            return [{'path': path, 'leftLength': len(left), 'rightLength': len(right)}]
        return [d for i, (a, b) in enumerate(zip(left, right)) for d in differences(a, b, path + '/' + str(i))]
    return [] if left == right else [{'path': path, 'left': left, 'right': right}]


def compare(a, b, filename):
    ca, cb = [], []
    raw_a, raw_b = read(a / filename), read(b / filename)
    left = normalize(raw_a, changes=ca, attempt=raw_a.get('executionAttemptId'))
    right = normalize(raw_b, changes=cb, attempt=raw_b.get('executionAttemptId'))
    delta = differences(left, right)
    return {'artifact': filename, 'equal': not delta, 'differences': delta,
            'normalizations': {'on': ca, 'off': cb}}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('run_directory', type=Path)
    args = parser.parse_args()
    root = args.run_directory.resolve()
    plan, completion = read(root / 'plan.json'), read(root / 'completion.json')
    private_root = root.parents[2]
    source_manifest = private_root / 'docker-build/source-manifest.json'
    source = private_root / 'docker-source'
    frozen = read(source_manifest)['sourceFiles']
    original_files_equal = all((source / p).is_file() and hashlib.sha256((source / p).read_bytes()).hexdigest() == h
                               for p, h in frozen.items())
    added = [p for p in source.rglob('*') if p.is_file() and str(p.relative_to(source)) not in frozen
             and not any(part in ('target', '__pycache__') for part in p.relative_to(source).parts)]
    allowed_logs = all(p.relative_to(source).parts[:3] == ('applications', 'quizzes', 'logs')
                       and len(p.relative_to(source).parts) == 4 and p.name.startswith('app-test-') and p.suffix == '.log'
                       for p in added)
    source_audit = {'originalCompletionSourceUnchanged': completion['sourceUnchanged'],
                    'frozenManifestHashMatchesPlan': hashlib.sha256(source_manifest.read_bytes()).hexdigest() == plan['sourceManifestSha256'],
                    'measuredFileCount': len(frozen), 'allMeasuredFilesUnchanged': original_files_equal,
                    'additionsOnlyInDeclaredRuntimeLogNamespace': allowed_logs,
                    'addedRuntimeLogs': {str(p.relative_to(source)): hashlib.sha256(p.read_bytes()).hexdigest() for p in added}}
    checks, samples, comparisons = [], [], []
    checks.append({'name': 'complete fixed matrix', 'passed': completion['runs'] == 22 and len(plan['jobs']) == 22})
    checks.append({'name': 'frozen source and artifacts', 'passed': original_files_equal and allowed_logs
                   and source_audit['frozenManifestHashMatchesPlan'] and completion['runtimeArtifactsUnchanged']})
    checks.append({'name': 'copied package unchanged', 'passed': completion['packageArtifactsUnchanged']})
    for job in plan['jobs']:
        directory = root / job['id']
        result = read(directory / 'run-result.json')
        checks.append({'name': job['id'] + ' exit', 'passed': result['exitCode'] == 0})
        for name, digest in result['artifacts'].items():
            actual = hashlib.sha256((directory / name).read_bytes()).hexdigest()
            checks.append({'name': job['id'] + ' hash ' + name, 'passed': actual == digest})
        sidecar = directory / 'execution-report.saga-read-exposure.json'
        checks.append({'name': job['id'] + ' opt-in persistence', 'passed': sidecar.exists() == job['enabled']})
        if job['kind'] == 'ordinary':
            execution = read(directory / 'execution-report.json')
            checks.append({'name': job['id'] + ' successful exact control',
                           'passed': execution['terminalStatus'] == 'SUCCESS' and execution['scheduleConformance'] == 'EXACT'})
        if job['kind'] != 'ordinary':
            experiment = read(directory / 'experiment.json')
            checks.extend({'name': job['id'] + ': ' + c['name'], 'passed': c['passed']} for c in experiment['checks'])
            samples.append({'id': job['id'], 'kind': job['kind'], 'pair': job.get('pair'),
                            'enabled': job['enabled'], 'actionDurationNanos': experiment['actionDurationNanos'],
                            'finishDurationNanos': experiment['finishDurationNanos'],
                            'diagnosticAssessmentDurationNanos': experiment['diagnosticAssessmentDurationNanos'],
                            'wallDurationNanos': result['wallDurationNanos'],
                            'memory': experiment['memory'], 'diagnostic': experiment.get('diagnostic')})
        if sidecar.exists():
            report = read(sidecar)
            expected = 1 if job['case'] in ('split-start', 'reader-only') else 0
            checks.append({'name': job['id'] + ' exposure count', 'passed': report['observedExposureCount'] == expected})
            checks.append({'name': job['id'] + ' usable declared scope', 'passed': report['collectionCoverage'] == 'COMPLETE_WITHIN_SCOPE'})
            if job['kind'] == 'ordinary':
                matched = []
                for call in report['calls']:
                    observation = call['observation']
                    if observation['outcome'] != 'DELIVERED' or observation.get('contract', {}).get('id') != 'quizzes.saga-local.quiz-by-id.outer':
                        continue
                    baseline = any(r['identity'] == observation['identity'] and r['version'] == observation['version']
                                   and r['runtimeType'] == observation['contract']['runtimeType'] for r in report['baseline'])
                    negative = any(a['callId'] == call['id'] and a['verdict'] == 'NOT_OBSERVED'
                                   and a['reason'] == 'REVISION_PREEXISTS_MEASUREMENT' for a in report['assessments'])
                    if baseline and negative:
                        matched.append(call['id'])
                checks.append({'name': job['id'] + ' actual setup-backed Quiz delivery', 'passed': bool(matched)})
            for reference in report['artifacts']:
                if reference['status'] == 'AVAILABLE':
                    checks.append({'name': job['id'] + ' reference ' + reference['role'],
                                   'passed': hashlib.sha256(Path(reference['path']).read_bytes()).hexdigest() == reference['sha256']})
    for pair in ('warmup', '1', '2', '3', 'ordinary'):
        prefix = 'ordinary' if pair == 'ordinary' else 'cost-' + pair
        on, off = root / (prefix + '-on'), root / (prefix + '-off')
        for filename in ('execution-report.json', 'execution-report.impact.json', 'execution-report.impact-v2.json'):
            value = compare(on, off, filename)
            comparisons.append({'pair': pair, **value})
            checks.append({'name': prefix + ' equivalent ' + filename, 'passed': value['equal']})
    costs = []
    for pair in ('1', '2', '3'):
        on = next(s for s in samples if s['id'] == 'cost-' + pair + '-on')
        off = next(s for s in samples if s['id'] == 'cost-' + pair + '-off')
        costs.append({'pair': pair, 'onNanos': on['actionDurationNanos'], 'offNanos': off['actionDurationNanos'],
                      'differenceNanos': on['actionDurationNanos'] - off['actionDurationNanos']})
    summary = {'status': 'PASS' if all(c['passed'] for c in checks) else 'FAIL', 'checks': checks,
               'sourceAudit': source_audit,
               'analysisScriptPath': str(Path(__file__).resolve()),
               'analysisScriptSha256': hashlib.sha256(Path(__file__).read_bytes()).hexdigest(),
               'runPlanSha256': hashlib.sha256((root / 'plan.json').read_bytes()).hexdigest(),
               'samples': samples, 'equivalence': comparisons, 'costPairs': costs,
               'medianPairedDifferenceNanos': statistics.median(c['differenceNanos'] for c in costs),
               'timingInterpretation': 'Three prespecified fresh-JVM pairs; initial pair excluded, not per-JVM steady-state JIT warmup; no statistical significance claim',
               'memoryInterpretation': 'Inclusive graph reachable from diagnostic collector and report; includes shared objects, not exclusive retained heap or peak allocation'}
    (root / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n')
    print(json.dumps({'status': summary['status'], 'checks': len(checks), 'failed': [c['name'] for c in checks if not c['passed']]}))
    raise SystemExit(0 if summary['status'] == 'PASS' else 1)


if __name__ == '__main__':
    main()
