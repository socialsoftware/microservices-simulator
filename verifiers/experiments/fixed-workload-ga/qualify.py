#!/usr/bin/env python3
"""The declared 53-attempt Quizzes qualification; IDs live here, never in the GA."""
import argparse
import importlib.util
from pathlib import Path
import sys

from runtime import batch, digest, package, prepare, read, save
from run import control, execute, verify_source

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TARGET = ROOT / 'verifiers/target'
BASE = TARGET / 'empty-event-delivery/run-01'
OVERLAY = TARGET / 'saga-update-read/read-scope-01'
BENCHMARK = '435ac86830d1ad7d778abe6dd4e046f728c9f3fd695106657aa430bc2d1710fc'
UPDATE = 'ee0db0e388e10f506b3633d47f3b908dc44ec46e6b4e312b166e32ffd3efedd6'


def freeze_runtime():
    provenance = read(OVERLAY / 'provenance.json')
    spec = importlib.util.spec_from_file_location('read_qualification', HERE.parent / 'saga-update-read/qualify-integrated.py')
    q = importlib.util.module_from_spec(spec); spec.loader.exec_module(q)
    _, build = q.verify(provenance['productionOverlay'])
    if digest(BASE / 'build-provenance.json') != provenance['preparedBuildHash']:
        raise ValueError('Prepared build provenance changed')
    hashes = {str(BASE / p): h for p, h in build['files'].items()}
    for p, h in read(OVERLAY / 'artifact-hashes.json').items():
        if p.startswith(('classes/', 'production/', 'source/')):
            if digest(OVERLAY / p) != h:
                raise ValueError('Qualified overlay changed: ' + p)
            hashes[str(OVERLAY / p)] = h
    for p, h in provenance['productionOverlay'].items():
        if digest(ROOT / p) != h:
            raise ValueError('Current overlay source changed: ' + p)
        hashes[str(ROOT / p)] = h
    prefix = batch.container(BASE / 'prepared-build')
    classpath = ':'.join([batch.container(OVERLAY / 'classes'), prefix + '/source/quizzes/target/classes',
        prefix + '/source/quizzes/target/test-classes', prefix + '/source/verifiers/target/classes',
        (BASE / 'prepared-build/quizzes-classpath.txt').read_text().strip(),
        (BASE / 'prepared-build/verifiers-classpath.txt').read_text().strip()])
    for entry in set(classpath.split(':')):
        path = TARGET / entry.removeprefix('/reports/')
        if not entry.startswith('/reports/') or not path.exists():
            raise ValueError('Unfrozen runtime classpath entry: ' + entry)
        if path.is_file():
            hashes[str(path)] = digest(path)
        else:
            for p in path.rglob('*'):
                if p.is_file():
                    hashes[str(p)] = digest(p)
    for p in list(HERE.glob('*.py')) + [HERE.parent / 'batch-execution/run.py',
        HERE.parent / 'impact-v2-broader/qualification.py', HERE.parent / 'impact-v2-broader/validate_assessment.py']:
        hashes[str(p)] = digest(p)
    return {'image': build['image'], 'hashes': hashes, 'classpath': classpath,
        'clock': 'OrdinaryExecutorControl: 2030-01-01T12:00',
        'javaOptions': ['-Xmx1536m', '-XX:MaxMetaspaceSize=512m', '-Dlocal.messaging.serialize=true'],
        'executorMain': 'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.OrdinaryExecutorControl',
        'executorArgs': ['--spring-application-class', 'pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator',
            '--spring-profiles', 'test,sagas,local', '--application-base', 'quizzes', '--application-id', 'quizzes',
            '--maven-profile', 'test-sagas', '--microservices.simulator.saga-read-exposure.enabled=true',
            '--verifiers.application.enabled=false', '--server.port=0']}


def main():
    p = argparse.ArgumentParser(); p.add_argument('--output', type=Path, required=True)
    a = p.parse_args(); out = a.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    runtime = freeze_runtime()
    configs = {name: {'manifest': str(manifest), 'workload': workload, 'runtime': runtime,
                     'recoveryCap': 20, 'timeout': 180}
        for name, manifest, workload in [
            ('benchmark', TARGET / 'search-baselines/prepared/package/scenario-catalog-manifest.json', BENCHMARK),
            ('update', OVERLAY / 'package/scenario-catalog-manifest.json', UPDATE)]}
    save(out / 'protocol.json', {'configs': configs, 'seeds': [11, 29], 'budgetPerArm': 12,
         'maxApplicationExecutions': 53, 'selection': 'Preselected in the approved plan; no outcome-driven substitution'})
    for name, config in configs.items():
        control(config, out / (name + '-control'))
    for seed in (11, 29):
        for strategy in ('ga', 'random'):
            config = {**configs['benchmark'], 'strategy': strategy, 'seed': seed, 'budget': 12}
            execute(config, out / f'{strategy}-{seed}', out / 'benchmark-control/control.json')
    rt, domain = prepare(configs['update'], out / 'update-probes')
    selected = [r for r in read(OVERLAY / 'selection.json') if r['position'] == 5 and not r['control']]
    if len(selected) != 3:
        raise ValueError('Expected three update/read recovery witnesses')
    cs = domain.resolve(selected[0]['vector'])
    by_id = {alias: c for c in cs for alias in c['aliases']}
    results = [rt.evaluate(out / 'update-probes', by_id[row['scenario']], n, 180)
               for n, row in enumerate(selected, 1)]
    domain.verify(); rt.verify(); verify_source(configs['update'], out / 'update-probes')
    save(out / 'update-probes/results.json', results)
    print('Qualification complete: ' + str(out), flush=True)


if __name__ == '__main__':
    main()
