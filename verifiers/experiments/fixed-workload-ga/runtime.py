"""Existing package/generator/assessment contracts, isolated Docker attempts."""
import importlib.util
import json
import math
from pathlib import Path
import shutil
import subprocess
import sys
import time
import uuid

from search import candidate_key, fault_coordinates, stable
from fitness import configuration, count

HERE = Path(__file__).resolve().parent
spec = importlib.util.spec_from_file_location('batch_runner', HERE.parent / 'batch-execution/run.py')
batch = importlib.util.module_from_spec(spec)
spec.loader.exec_module(batch)
package, read, save, digest = batch.package, batch.read, batch.save, batch.digest


class IntegrityError(ValueError):
    pass


def copy_artifact(source, destination):
    """Independent snapshots; APFS clones avoid duplicating immutable package bytes."""
    if sys.platform == 'darwin':
        try:
            subprocess.run(['/bin/cp', '-c', str(source), str(destination)],
                           check=True, stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)
        except (OSError, subprocess.CalledProcessError):
            pass  # Other filesystems retain ordinary copy semantics.
        else:
            shutil.copystat(source, destination)
            return str(destination)
    return shutil.copy2(source, destination)


def validate_read(report, execution, directory, manifest):
    if report['schemaVersion'] != 'microservices-simulator.saga-read-exposure.v2':
        raise ValueError('Wrong read-exposure schema')
    for key in ('executionAttemptId', 'workloadPlanId', 'faultScenarioId', 'scheduleConformance'):
        if report.get(key) != execution.get(key):
            raise ValueError('Read report join mismatch: ' + key)
    if report['executionTerminalStatus'] != execution['terminalStatus']:
        raise ValueError('Read terminal status mismatch')
    if report['executionValidity'] not in ('COMPLETE', 'NOT_MEASURED', 'INCOMPLETE'):
        raise ValueError('Unknown read execution validity')
    coverage = report['collectionCoverage']
    if coverage not in ('COMPLETE_WITHIN_SCOPE', 'PARTIAL', 'UNAVAILABLE'):
        raise ValueError('Unknown read coverage')
    count = report['observedExposureCount']
    findings = report['findings']
    ids = [f['id'] for f in findings]
    if len(set(ids)) != len(ids) or any(f['verdict'] != 'OBSERVED' for f in findings):
        raise ValueError('Invalid exposure findings')
    if count is not None and (type(count) is not int or count != len(ids)):
        raise ValueError('Exposure count mismatch')
    if coverage == 'COMPLETE_WITHIN_SCOPE' and (report['gaps'] or count is None):
        raise ValueError('Incomplete evidence marked complete')
    for role, path in [('PACKAGE_MANIFEST', manifest), ('EXECUTION_REPORT', directory / 'execution-report.json')]:
        refs = [a for a in report['artifacts'] if a['role'] == role]
        if len(refs) != 1 or refs[0]['status'] != 'AVAILABLE' or refs[0]['sha256'] != digest(path):
            raise ValueError('Read artifact hash mismatch: ' + role)
    return {'A': count, 'AStatus': report['executionValidity'], 'ACoverage': coverage,
            'AFindings': findings, 'AGaps': report['gaps'], 'AScope': report['scope'],
            'AContracts': report['contracts'], 'AExcludedPaths': report['excludedPaths']}


def validate_lost_copied_updates(report, execution):
    if report['schemaVersion'] != 'lost-copied-updates.v1':
        raise ValueError('Wrong lost-copied-update schema')
    for key in ('executionAttemptId', 'workloadPlanId', 'faultScenarioId', 'scheduleConformance'):
        if report.get(key) != execution.get(key):
            raise ValueError('Lost-copied-update report join mismatch: ' + key)
    if report.get('executionTerminalStatus') != execution.get('terminalStatus'):
        raise ValueError('Lost-copied-update terminal status mismatch')
    validity = report['validity']
    if validity not in ('COMPLETE', 'UNAVAILABLE'):
        raise ValueError('Unknown lost-copied-update validity')
    coverage = report['coverage']
    if coverage not in ('COMPLETE_WITHIN_SCOPE', 'INCOMPLETE', 'UNAVAILABLE'):
        raise ValueError('Unknown lost-copied-update coverage')
    observed = report['count']
    findings = report['findings']
    gaps = report['coverageGaps']
    if not count(observed) or not isinstance(findings, list) or not isinstance(gaps, list):
        raise ValueError('Invalid lost-copied-update evidence')
    ids = [finding.get('findingId') for finding in findings if isinstance(finding, dict)]
    if len(ids) != len(findings) or any(not isinstance(identifier, str) or not identifier for identifier in ids) \
            or len(set(ids)) != len(ids) or observed != len(findings):
        raise ValueError('Lost-copied-update grouped count mismatch')
    if validity == 'COMPLETE' and coverage == 'COMPLETE_WITHIN_SCOPE' and gaps:
        raise ValueError('Incomplete lost-copied-update evidence marked complete')
    return {'lostCopiedUpdateCount': observed, 'lostCopiedUpdateValidity': validity,
            'lostCopiedUpdateCoverage': coverage, 'lostCopiedUpdateFindings': findings,
            'lostCopiedUpdateCoverageGaps': gaps}


def retained_attempt(path):
    """Read measured feedback without executing Java or requiring the old Python runner."""
    result = read(path)
    directory = path.parent
    for name, expected in result['reportHashes'].items():
        if digest(directory / name) != expected:
            raise IntegrityError('Retained report changed: ' + name)
    manifest = directory / 'package/scenario-catalog-manifest.json'
    if package(manifest)['hashes'] != result['packageHashes']:
        raise IntegrityError('Retained package snapshot changed')
    # Infrastructure failures have no usable feedback; their null result is preserved.
    if result['status'] not in ('COMPLETE', 'PARTIAL', 'UNAVAILABLE'):
        return {**result, 'I': None, 'A': None}
    execution = read(directory / 'execution-report.json')
    v1 = read(directory / 'execution-report.impact.json')
    v2 = read(directory / 'execution-report.impact-v2.json')
    c = result['candidate']
    batch.validate_reports(execution, v1, v2, c['workload'], c['id'], c['faultVector'])
    status, measured_i = batch.assessment_result(execution, v2)
    if status != result['status'] or measured_i != result['I'] \
            or v2['categoryResults'] != result['impactCategories']:
        raise IntegrityError('Retained feedback disagrees with reports')
    measured = {**result, 'terminalStatus': execution['terminalStatus'],
                'scheduleConformance': execution['scheduleConformance']}
    try:
        if 'execution-report.saga-read-exposure.json' not in result['reportHashes']:
            raise ValueError('No retained read-report hash')
        measured.update(validate_read(read(directory / 'execution-report.saga-read-exposure.json'),
                                       execution, directory, manifest))
    except (ValueError, KeyError, OSError, TypeError) as error:
        measured.update(A=None, AStatus='INVALID_OR_MISSING_REPORT', ACoverage='UNAVAILABLE', AError=str(error))
    try:
        name = 'execution-report-lost-copied-updates.json'
        if name not in result['reportHashes']:
            raise ValueError('No retained lost-copied-update report hash')
        measured.update(validate_lost_copied_updates(read(directory / name), execution))
    except (ValueError, KeyError, OSError, TypeError) as error:
        measured.update(lostCopiedUpdateCount=None, lostCopiedUpdateValidity='INVALID_OR_MISSING_REPORT',
                        lostCopiedUpdateCoverage='UNAVAILABLE', lostCopiedUpdateCoverageGaps=[],
                        lostCopiedUpdateError=str(error))
    return measured


class Runtime:
    def __init__(self, descriptor):
        self.config = descriptor

    def verify(self):
        if not self.config['image'].startswith('sha256:') or not self.config['hashes']:
            raise IntegrityError('Runtime requires immutable image and file hashes')
        for path, expected in self.config['hashes'].items():
            if digest(Path(path)) != expected:
                raise IntegrityError('Runtime drift: ' + path)
        agent = self.config.get('lostCopiedUpdateAgent')
        if agent is not None:
            if not isinstance(agent, dict) or set(agent) != {'path', 'sha256'}:
                raise IntegrityError('Lost-copied-update agent requires path and sha256')
            path = Path(agent['path'])
            try:
                batch.container(path)
            except (TypeError, ValueError):
                raise IntegrityError('Lost-copied-update agent must be under verifiers/target') from None
            if digest(path) != agent['sha256']:
                raise IntegrityError('Lost-copied-update agent drift: ' + str(path))
        source_root = self.config.get('lostCopiedUpdateSourceRoot')
        if source_root is not None:
            if not isinstance(source_root, str) or not source_root:
                raise IntegrityError('Lost-copied-update source root requires a path')
            path = Path(source_root)
            try:
                batch.container(path)
            except (TypeError, ValueError):
                raise IntegrityError('Lost-copied-update source root must be under verifiers/target') from None
            if not path.is_dir():
                raise IntegrityError('Lost-copied-update source root is not a directory: ' + str(path))

    def command(self, out, name, main, args):
        c = self.config
        java_options = list(c['javaOptions'])
        agent = c.get('lostCopiedUpdateAgent')
        if agent is not None and main == c['executorMain']:
            java_options += ['-javaagent:' + batch.container(Path(agent['path'])),
                '-Dsimulator.copied-update.contracts=/out/package/copy-contracts.json']
            source_root = c.get('lostCopiedUpdateSourceRoot')
            if source_root is not None:
                java_options.append('-Dsimulator.copied-update.source-root=' +
                                    batch.container(Path(source_root)))
        return ['docker', 'run', '--rm', '--name', name, '--cpus', str(c.get('cpus', 2)),
                '--memory', c.get('memory', '3g'), '--network', 'none', '--entrypoint', 'java',
                '-v', str(batch.TARGET) + ':/reports:ro', '-v', str(out.resolve()) + ':/out',
                c['image'], *java_options, '-cp', c['classpath'], main, *args]

    def launch(self, out, label, main, args, timeout):
        name = 'workload-search-' + uuid.uuid4().hex[:16]
        command = self.command(out, name, main, args)
        result = batch.run_process(command, out / (label + '.log'), timeout,
                                   lambda: batch.remove_container(name))
        return {**result, 'containerName': name}

    def request(self, out, vector, workload, cap, number, timeout):
        label = f'request-{number:03d}'
        result = self.launch(out, label,
            'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.FaultScenarioRequestCli',
            ['--manifest-path', '/out/package/scenario-catalog-manifest.json',
             '--workload-plan-id', workload, '--fault-vector', vector,
             '--recovery-schedule-cap', str(cap)], timeout)
        # The CLI writes a JSON result; tolerate framework log prefixes, never invent a result.
        content = (out / (label + '.log')).read_text()
        decoder = json.JSONDecoder()
        parsed = None
        for offset, char in enumerate(content):
            if char == '{':
                try:
                    value, _ = decoder.raw_decode(content[offset:])
                    if isinstance(value, dict) and 'faultScenarioIds' in value:
                        parsed = value
                except ValueError:
                    pass
        return result, parsed

    def evaluate(self, out, candidate, number, timeout):
        directory = out / f'attempt-{number:03d}'
        directory.mkdir(exist_ok=False)
        manifest = out / 'package/scenario-catalog-manifest.json'
        before = package(manifest)['hashes']
        shutil.copytree(manifest.parent, directory / 'package', copy_function=copy_artifact)
        args = [*self.config['executorArgs'], '--package-path', '/out/package/scenario-catalog-manifest.json',
                '--fault-scenario-id', candidate['id'], '--output-path',
                f'/out/attempt-{number:03d}/execution-report.json', '--impact-output-path',
                f'/out/attempt-{number:03d}/execution-report.impact.json']
        if self.config.get('lostCopiedUpdateAgent') is not None and not any(
                option.startswith('--microservices.simulator.lost-copied-update.enabled=') for option in args):
            args.append('--microservices.simulator.lost-copied-update.enabled=true')
        result = {'I': None, 'A': None, 'AStatus': 'UNAVAILABLE', 'ACoverage': 'UNAVAILABLE',
                  'lostCopiedUpdateCount': None, 'lostCopiedUpdateValidity': 'UNAVAILABLE',
                  'lostCopiedUpdateCoverage': 'UNAVAILABLE', 'lostCopiedUpdateCoverageGaps': [],
                  'status': 'INFRASTRUCTURE_FAILURE', 'directory': str(directory),
                  'candidate': candidate, 'packageHashes': before}
        save(directory / 'replay.json', {'runtime': self.config, 'candidate': candidate,
             'packageHashes': before, 'command': self.command(out, 'replay-' + uuid.uuid4().hex[:12],
                                                            self.config['executorMain'], args)})
        try:
            process = self.launch(out, f'attempt-{number:03d}', self.config['executorMain'], args, timeout)
            result.update(process)
            if process['status'] == 'EXITED':
                result['status'] = 'INVALID_REPORT'
                execution = read(directory / 'execution-report.json')
                v1 = read(directory / 'execution-report.impact.json')
                v2 = read(directory / 'execution-report.impact-v2.json')
                batch.validate_reports(execution, v1, v2, candidate['workload'], candidate['id'], candidate['faultVector'])
                if execution['schemaVersion'] not in batch.EXECUTION_SCHEMAS or \
                        v1['schemaVersion'] != 'microservices-simulator.scenario-impact-report.v1' or \
                        execution['packageManifestPath'] != '/out/package/scenario-catalog-manifest.json':
                    raise ValueError('Unexpected report schema/package')
                result['status'], result['I'] = batch.assessment_result(execution, v2)
                result.update(executionAttemptId=execution['executionAttemptId'],
                    terminalStatus=execution['terminalStatus'], scheduleConformance=execution['scheduleConformance'],
                    impactCategories=v2['categoryResults'], impactCoverageGaps=v2['coverageGaps'])
                try:
                    result.update(validate_read(read(directory / 'execution-report.saga-read-exposure.json'),
                                                execution, directory, manifest))
                except (ValueError, KeyError, OSError, TypeError) as error:
                    result.update(A=None, AStatus='INVALID_OR_MISSING_REPORT', ACoverage='UNAVAILABLE', AError=str(error))
                try:
                    result.update(validate_lost_copied_updates(
                        read(directory / 'execution-report-lost-copied-updates.json'), execution))
                except (ValueError, KeyError, OSError, TypeError) as error:
                    result.update(lostCopiedUpdateCount=None,
                        lostCopiedUpdateValidity='INVALID_OR_MISSING_REPORT',
                        lostCopiedUpdateCoverage='UNAVAILABLE', lostCopiedUpdateCoverageGaps=[],
                        lostCopiedUpdateError=str(error))
        except (ValueError, KeyError, OSError, TypeError) as error:
            result.update(I=None, status='INVALID_REPORT', error=str(error))
        if package(manifest)['hashes'] != before:
            raise IntegrityError('Executor modified package')
        result['reportHashes'] = {p.name: digest(p) for p in directory.glob('*.json') if p.name != 'attempt.json'}
        save(directory / 'attempt.json', result)
        print(f'{out.name} #{number}: {result["status"]} I={result["I"]} A={result["A"]}', flush=True)
        return result


class Domain:
    def __init__(self, out, runtime, workload_id, cap, timeout=180):
        if type(cap) is not int or cap < 1:
            raise ValueError('Positive recovery cap required')
        self.out, self.runtime, self.cap, self.timeout = out, runtime, cap, timeout
        self.manifest = out / 'package/scenario-catalog-manifest.json'
        data = package(self.manifest)
        matches = [w for w in data['records']['workloads'] if w['id'] == workload_id]
        if len(matches) != 1:
            raise ValueError('Workload must exist exactly once')
        self.workload = matches[0]
        setup = self.workload.get('setup')
        if setup and not any(s['id'] == setup for s in data['records']['setups']):
            raise ValueError('Missing workload setup')
        self.coordinates = fault_coordinates(self.workload)
        self.width = sum(len(c) - 1 for c in self.coordinates)
        self.fixed = {k: v for k, v in data['hashes'].items()
                      if k not in ('manifest', 'faultScenarios', 'requests', 'accounting')}
        self.cache, self.requests = {}, []
        save(out / 'scope.json', {'workload': self.workload, 'coordinates': self.coordinates,
             'vectorDomainSize': math.prod(map(len, self.coordinates)), 'recoveryCap': cap,
             'inheritedGeneration': data['records']['accounting'].get('configuration'),
             'fixedPackageHashes': self.fixed})

    def verify(self):
        data = package(self.manifest)
        if any(data['hashes'].get(k) != v for k, v in self.fixed.items()):
            raise IntegrityError('Fixed workload/setup/package content changed')
        return data

    def resolve(self, vector):
        if vector in self.cache:
            return self.cache[vector]
        if len(vector) != self.width or set(vector) - {'0', '1'} or \
                any(sum(vector[s] == '1' for s in c if s is not None) > 1 for c in self.coordinates):
            raise ValueError('Noncanonical vector')
        before = self.verify()['hashes']
        process, response = self.runtime.request(self.out, vector, self.workload['id'], self.cap,
                                                 len(self.requests) + 1, self.timeout)
        after = self.verify()
        record = {'vector': vector, 'process': process, 'response': response,
                  'before': before, 'after': after['hashes']}
        self.requests.append(record)
        save(self.out / 'requests.json', self.requests)
        if response and response['status'] in ('INTEGRITY_FAILURE', 'PERSISTENCE_FAILED'):
            raise IntegrityError('Generator integrity/persistence failure: ' + stable(response))
        candidates = []
        success = response and response['status'] in ('PERSISTED', 'DEDUPLICATED')
        if success and process['status'] == 'EXITED':
            if response['workloadPlanId'] != self.workload['id'] or response['assignedVector'] != vector \
                    or response['recoveryScheduleCap'] != self.cap:
                raise IntegrityError('Generator request join/cap mismatch')
            ids = response['faultScenarioIds']
            if len(ids) != response['writtenScheduleCount'] or len(ids) > self.cap \
                    or int(response['uncappedScheduleCount']) < len(ids):
                raise IntegrityError('Invalid generator counts')
            groups = {}
            scenarios = {s['id']: s for s in after['records']['faultScenarios']}
            for scenario_id in ids:
                c = dict(scenarios[scenario_id])
                if c['workload'] != self.workload['id'] or c['faultVector'] != vector:
                    raise IntegrityError('Generator returned an unrelated scenario')
                key = candidate_key(c['workload'], vector, c['actions'])
                if key not in groups:
                    c.update(key=key, aliases=[])
                    groups[key] = c
                groups[key]['aliases'].append(scenario_id)
            candidates = sorted(groups.values(), key=lambda c: stable(c['actions']))
            # Include historical aliases, but never expand beyond returned executable content.
            for c in scenarios.values():
                key = candidate_key(c['workload'], c['faultVector'], c['actions'])
                if key in groups and c['id'] not in groups[key]['aliases']:
                    groups[key]['aliases'].append(c['id'])
            for c in candidates:
                c['aliases'].sort()
            record['truncated'] = int(response['uncappedScheduleCount']) > len(ids)
            save(self.out / 'requests.json', self.requests)
        self.cache[vector] = candidates
        return candidates

    def exhausted(self, seen):
        # A rejected/timed-out request is not proof of an empty vector domain.
        return len(self.cache) == math.prod(map(len, self.coordinates)) and \
            all(self.cache.values()) and \
            all(c['key'] in seen for cs in self.cache.values() for c in cs)


def prepare(config, out):
    config = {**config, 'fitness': configuration(config.get('fitness'))}
    out.mkdir(parents=True, exist_ok=False)
    source = Path(config['manifest']).resolve()
    original = package(source)
    runtime = Runtime(config['runtime'])
    runtime.verify()
    shutil.copytree(source.parent, out / 'package', copy_function=copy_artifact)
    save(out / 'config.json', config)
    save(out / 'source-package-hashes.json', original['hashes'])
    return runtime, Domain(out, runtime, config['workload'], config['recoveryCap'], config.get('timeout', 180))
