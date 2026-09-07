#!/usr/bin/env python3
"""Frozen-source, sequential Docker qualification; never uses shared Maven targets/cache."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import time
import urllib.request

IMAGE = 'sha256:0aab59d58bfe4f83e6bee2a4002a913dcbc26d861acee5f0327c053f12918282'
JOL_SHA = 'ea8cf31b7dc6c18810ca7aeadcbe7a2b352fb250261b8060ce513e8a99ebcd12'
JOL_URL = 'https://repo.maven.apache.org/maven2/org/openjdk/jol/jol-cli/0.17/jol-cli-0.17-full.jar'
REPO = Path(__file__).resolve().parents[3]
EXPERIMENT = Path('verifiers/experiments/saga-read-exposure')
JVM = ['-Xmx1536m', '-XX:MaxMetaspaceSize=512m']


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def write_json(path, value):
    Path(path).parent.mkdir(parents=True, exist_ok=True)
    Path(path).write_text(json.dumps(value, indent=2) + '\n')


def files(root):
    return sorted(p for p in root.rglob('*') if p.is_file()
                  and not any(x in p.relative_to(root).parts for x in ('target', '__pycache__'))
                  and not (p.relative_to(root).parts[:3] == ('applications', 'quizzes', 'logs')
                           and len(p.relative_to(root).parts) == 4
                           and p.name.startswith('app-test-') and p.suffix == '.log'))


def manifest(root):
    return {str(p.relative_to(root)): sha(p) for p in files(root)}


def docker(root, name):
    return ['docker', 'run', '--rm', '--name', name, '--cpus=2', '--memory=3g',
            '--mount', f'type=bind,src={root},dst={root}', '--workdir',
            str(root / 'docker-source/applications/quizzes'), '--entrypoint', '/bin/bash',
            '-e', 'MAVEN_CONFIG=' + str(root / 'docker-maven-config'),
            '-e', 'JAVA_TOOL_OPTIONS=' + ' '.join(JVM), IMAGE]


def run_logged(command, path):
    start = time.monotonic_ns()
    with Path(path).open('w') as log:
        result = subprocess.run(command, stdout=log, stderr=subprocess.STDOUT)
    return {'command': command, 'exitCode': result.returncode,
            'wallDurationNanos': time.monotonic_ns() - start, 'logSha256': sha(path)}


def prepare(root):
    source = root / 'docker-source'
    if source.exists():
        raise SystemExit(f'Refusing to overwrite existing frozen source: {source}')
    root.mkdir(parents=True, exist_ok=True)
    for module in ('simulator', 'verifiers', 'applications/quizzes'):
        dest = source / module
        dest.mkdir(parents=True)
        shutil.copy2(REPO / module / 'pom.xml', dest / 'pom.xml')
        shutil.copytree(REPO / module / 'src', dest / 'src',
                        ignore=shutil.ignore_patterns('target', '__pycache__', '*.pyc'))
    shutil.copytree(REPO / EXPERIMENT, source / EXPERIMENT,
                    ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
    (root / 'docker-build').mkdir(exist_ok=True)
    (root / 'docker-maven-repository').mkdir(exist_ok=True)
    tool = root / 'memory-tool/jol-cli-0.17-full.jar'
    tool.parent.mkdir(exist_ok=True)
    if not tool.exists():
        urllib.request.urlretrieve(JOL_URL, tool)
    if sha(tool) != JOL_SHA:
        raise SystemExit('JOL artifact hash mismatch')
    protocol = REPO / 'issues/2026-09-07-compensated-saga-read/M2-PROTOCOL.md'
    provenance = {'sourceRevision': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=REPO, text=True).strip(),
                  'sourceTreeState': 'measured files are authoritative; snapshot may include uncommitted feature files',
                  'image': IMAGE, 'cpus': 2, 'memoryLimit': '3g', 'jvmOptions': JVM,
                  'protocolSha256': sha(protocol), 'jolSha256': JOL_SHA,
                  'sourceFiles': manifest(source)}
    write_json(root / 'docker-build/source-manifest.json', provenance)
    command = docker(root, f'saga-read-build-{os.getpid()}') + [
        str(source / EXPERIMENT / 'prepare-build.sh'), str(root)]
    result = run_logged(command, root / 'docker-build/build.log')
    write_json(root / 'docker-build/build-result.json', result)
    if result['exitCode']:
        raise SystemExit(f'Build failed; retained log: {root / "docker-build/build.log"}')
    if provenance['sourceFiles'] != manifest(source):
        raise SystemExit('Frozen source changed during build')
    cp = (root / 'docker-build/runtime-classpath.txt').read_text().strip().split(':')
    artifacts = {str(Path(p)): sha(p) for p in sorted(set(cp)) if Path(p).is_file()}
    for folder in (root / 'docker-build/classes', source / 'simulator/target/classes',
                   source / 'verifiers/target/classes', source / 'applications/quizzes/target/classes',
                   source / 'applications/quizzes/target/test-classes'):
        artifacts.update({str(p): sha(p) for p in sorted(folder.rglob('*')) if p.is_file()})
    write_json(root / 'docker-build/runtime-artifacts.json', artifacts)
    print(json.dumps({'build': 'PASS', 'sourceFiles': len(provenance['sourceFiles']),
                      'sourceManifestSha256': sha(root / 'docker-build/source-manifest.json')}), flush=True)


def plan():
    jobs = []
    for case in ('split-start', 'early-compensation', 'producer-success'):
        for suffix, serialize in (('serialized-1', True), ('serialized-2', True), ('direct-1', False)):
            jobs.append({'id': case + '-' + suffix, 'case': case, 'serialize': serialize, 'enabled': True, 'kind': 'qualification'})
    jobs.append({'id': 'reader-only-direct-1', 'case': 'reader-only', 'serialize': False, 'enabled': True, 'kind': 'qualification'})
    for serialize in (True, False):
        jobs.append({'id': 'tournament-outer-' + ('serialized' if serialize else 'direct'),
                     'case': 'tournament-outer', 'serialize': serialize, 'enabled': True, 'kind': 'qualification'})
    for pair, modes in (('warmup', (False, True)), ('1', (True, False)), ('2', (False, True)), ('3', (True, False))):
        for enabled in modes:
            jobs.append({'id': f'cost-{pair}-' + ('on' if enabled else 'off'), 'case': 'reader-only',
                         'serialize': True, 'enabled': enabled, 'kind': 'warmup' if pair == 'warmup' else 'cost', 'pair': pair})
    for enabled in (False, True):
        jobs.append({'id': 'ordinary-' + ('on' if enabled else 'off'), 'case': 'ordinary',
                     'serialize': True, 'enabled': enabled, 'kind': 'ordinary'})
    return jobs


def run(root, run_id, package, scenario, selected):
    build = root / 'docker-build'
    if not (build / 'READY').exists():
        raise SystemExit('Prepared build is not ready')
    orchestration_hash = sha(Path(__file__))
    if orchestration_hash != sha(root / 'docker-source' / EXPERIMENT / 'qualification.py'):
        raise SystemExit('Executing orchestration script differs from prepared snapshot')
    source_proof = json.loads((build / 'source-manifest.json').read_text())
    if manifest(root / 'docker-source') != source_proof['sourceFiles']:
        raise SystemExit('Frozen source drift')
    artifacts = json.loads((build / 'runtime-artifacts.json').read_text())
    if any(not Path(p).is_file() or sha(p) != digest for p, digest in artifacts.items()):
        raise SystemExit('Prepared runtime artifact drift')
    output = root / 'reports/m2' / run_id
    output.mkdir(parents=True, exist_ok=False)
    jobs = plan()
    if selected:
        unknown = set(selected) - {j['id'] for j in jobs}
        if unknown:
            raise SystemExit(f'Unknown predeclared job ids: {sorted(unknown)}')
        jobs = [j for j in jobs if j['id'] in selected]
    package = package.resolve()
    if not package.is_relative_to(root):
        raise SystemExit('Copy package under private run root before executing')
    package_files = {str(package): sha(package)}
    for role, entry in json.loads(package.read_text())['files'].items():
        artifact = (package.parent / entry['path']).resolve()
        if not artifact.is_relative_to(package.parent) or sha(artifact) != entry['sha256']:
            raise SystemExit('Frozen package artifact mismatch: ' + role)
        package_files[str(artifact)] = entry['sha256']
    write_json(output / 'plan.json', {'jobs': jobs, 'image': IMAGE,
               'orchestrationScriptSha256': orchestration_hash,
               'sourceManifestSha256': sha(build / 'source-manifest.json'),
               'runtimeArtifactManifestSha256': sha(build / 'runtime-artifacts.json'),
               'packageManifest': str(package), 'packageManifestSha256': sha(package), 'scenarioId': scenario,
               'packageArtifactHashes': package_files,
               'clockFixture': 'DateHandler.now()=2030-01-01T12:00; Mockito CALLS_REAL_METHODS; same on/off'})
    cp = (build / 'runtime-classpath.txt').read_text().strip()
    completed = []
    for number, job in enumerate(jobs):
        directory = output / job['id']
        directory.mkdir()
        java = ['java', '-javaagent:' + str(root / 'memory-tool/jol-cli-0.17-full.jar'),
                '-Dmicroservices.simulator.impact.enabled=true', '-Dmicroservices.simulator.event-replay.enabled=true',
                '-Dsaga.read.experiment.buildManifest=' + str(build / 'source-manifest.json'), '-cp', cp]
        if job['kind'] == 'ordinary':
            java += ['pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.OrdinaryExecutorControl',
                     '--spring-application-class', 'pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator',
                     '--spring-profiles', 'test,sagas,local', '--application-base', 'quizzes', '--application-id', 'quizzes',
                     '--maven-profile', 'test-sagas', '--package-path', str(package), '--fault-scenario-id', scenario,
                     '--output-path', str(directory / 'execution-report.json'),
                     '--impact-output-path', str(directory / 'execution-report.impact.json'),
                     '--microservices.simulator.saga-read-exposure.enabled=' + str(job['enabled']).lower(),
                     '--verifiers.application.enabled=false', '--server.port=0']
        else:
            java += ['pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureExperiment', job['case'],
                     str(job['serialize']).lower(), str(job['enabled']).lower(), f'{run_id}:{job["id"]}', str(directory)]
        # Bash only dispatches a literal argument array; no interpolated shell program is evaluated.
        command = docker(root, f'saga-read-{os.getpid()}-{number}') + ['-c', 'exec "$@"', 'saga-read'] + java
        result = run_logged(command, directory / 'runtime.log')
        result['job'] = job
        result['artifacts'] = {p.name: sha(p) for p in sorted(directory.iterdir()) if p.is_file()}
        write_json(directory / 'run-result.json', result)
        completed.append({'id': job['id'], 'exitCode': result['exitCode'], 'wallDurationNanos': result['wallDurationNanos']})
        write_json(output / 'progress.json', completed)
        print(json.dumps(completed[-1]), flush=True)
        if result['exitCode']:
            raise SystemExit(f'Failed run retained at {directory}; fix and use a new run id')
    write_json(output / 'completion.json', {'status': 'COMMANDS_COMPLETED', 'runs': len(completed),
               'sourceUnchanged': manifest(root / 'docker-source') == source_proof['sourceFiles'],
               'packageArtifactsUnchanged': all(sha(p) == digest for p, digest in package_files.items()),
               'runtimeArtifactsUnchanged': all(sha(p) == digest for p, digest in artifacts.items())})


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('mode', choices=('plan', 'prepare', 'run'))
    parser.add_argument('--run-root', type=Path)
    parser.add_argument('--run-id', default='qualification-01')
    parser.add_argument('--package', type=Path)
    parser.add_argument('--scenario')
    parser.add_argument('--job', action='append', default=[])
    args = parser.parse_args()
    if args.mode == 'plan':
        print(json.dumps(plan(), indent=2)); return
    if not args.run_root:
        parser.error('--run-root is required')
    root = args.run_root.resolve()
    if root.is_relative_to(REPO) or root in REPO.parents or root == Path.home():
        parser.error('Use an independent task directory as --run-root')
    if args.mode == 'prepare':
        prepare(root)
    else:
        if not args.package or not args.scenario:
            parser.error('--package and --scenario are required')
        run(root, args.run_id, args.package, args.scenario, args.job)


if __name__ == '__main__':
    main()
