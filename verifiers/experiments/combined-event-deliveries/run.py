#!/usr/bin/env python3
"""One frozen Docker build, generated package, eight fresh-process comparisons."""
import argparse
import concurrent.futures
import hashlib
import json
import os
import subprocess
from pathlib import Path
from freeze import select
from qualification import package
from validate import validate

ROOT = Path(__file__).resolve().parents[3]
TARGET = ROOT / 'verifiers/target'


def save(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')


def container(path):
    return '/reports/' + str(path.resolve().relative_to(TARGET.resolve()))


def compose(env, script):
    cmd = ['docker', 'compose', '-p', 'microservices-simulator', '-f', str(ROOT/'docker-compose.yml'),
           'run', '--rm', '--no-deps', '--pull', 'never', '-T']
    for key, value in env.items():
        cmd += ['-e', key+'='+str(value)]
    return cmd + ['scenario-executor', 'bash', '/verifiers/experiments/'+script]


def run_command(cmd, log):
    save(log.with_suffix('.command.json'), cmd)
    with log.open('w') as stream:
        return subprocess.run(cmd, cwd=ROOT, stdout=stream, stderr=subprocess.STDOUT,
            env={**os.environ, 'MEDIUM_MEM_LIMIT':'4g', 'MEDIUM_MEM_RESERVATION':'1g', 'MEDIUM_CPUS':'5.0'}).returncode


def main(output):
    output = output.resolve()
    output.mkdir(parents=True, exist_ok=False)
    hashes = {}
    for src, dest in [('simulator','simulator'), ('verifiers','verifiers'), ('applications/quizzes','quizzes')]:
        for file in (ROOT/src).rglob('*'):
            rel = file.relative_to(ROOT/src)
            if file.is_file() and not set(rel.parts) & {'target','.git','logs','experiments','__pycache__'} and file.suffix != '.pyc':
                hashes[str(Path(dest)/rel)] = hashlib.sha256(file.read_bytes()).hexdigest()
    save(output/'source-hashes.json', hashes)
    build = output/'prepared-build'
    env = {'BUILD_OUTPUT_DIR':container(build), 'JAVA_TOOL_OPTIONS':'-Xmx1536m -XX:MaxMetaspaceSize=512m'}
    if run_command(compose(env, 'impact-v2-broader/prepare-build.sh'), output/'build.log'):
        raise RuntimeError('Frozen Docker build failed; see build.log')
    for rel, expected in hashes.items():
        assert hashlib.sha256((build/'source'/rel).read_bytes()).hexdigest() == expected, rel
    print('Frozen build ready', flush=True)
    if run_command(compose({**env, 'GENERATION_OUTPUT_DIR':container(output/'generated')},
                           'combined-event-deliveries/generate-prepared.sh'), output/'generation.log'):
        raise RuntimeError('Generation failed; see generation.log')
    manifests = list((output/'generated').glob('*/scenario-catalog-manifest.json'))
    assert len(manifests) == 1
    manifest = manifests[0]
    select(manifest, output/'selection')
    selection = json.loads((output/'selection/broader-selection.json').read_text())
    plan = {'manifest':str(manifest), 'manifestSha256':selection['packageManifestSha256'],
            'packageHashes':package(manifest)['hashes'],
            'rows':selection['rows'], 'maxConcurrentAttempts':2}
    save(output/'plan.json', plan)
    def attempt(row):
        case = output/row['cohort']/'attempts'/row['caseId']
        case.mkdir(parents=True)
        cmd = compose({**env, 'RUNNER_KIND':'generic', 'PACKAGE_PATH':container(manifest),
                       'FAULT_SCENARIO_ID':row['faultScenarioId'], 'EXECUTION_OUTPUT_PATH':container(case/'execution.json'),
                       'IMPACT_OUTPUT_PATH':container(case/'impact-v1.json')}, 'impact-v2-broader/run-prepared.sh')
        code = run_command(cmd, case/'docker.log')
        (case/'process-exit-code').write_text(str(code)+'\n')
        print(row['caseId'], 'process exit', code, flush=True)
    with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
        list(pool.map(attempt, plan['rows']))
    assert hashlib.sha256(manifest.read_bytes()).hexdigest() == plan['manifestSha256']
    assert package(manifest)['hashes'] == plan['packageHashes']
    validate(output)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    main(parser.parse_args().output)
