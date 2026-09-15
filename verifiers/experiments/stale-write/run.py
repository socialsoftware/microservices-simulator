#!/usr/bin/env python3
"""Controlled stale-write investigation over the existing qualified Quizzes build."""
import argparse
import json
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from qualify import freeze_runtime
from runtime import Runtime, batch, digest, read, save

CASES = ('forward-stale', 'forward-fresh', 'recovery-stale',
         'recovery-delayed-event', 'recovery-no-event')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    out = args.output.resolve(); out.mkdir(parents=True, exist_ok=False)
    source = out / 'source'; source.mkdir()
    for p in HERE.glob('*'):
        if p.suffix in ('.java', '.py'):
            shutil.copy2(p, source / p.name)
    config = freeze_runtime()
    rt = Runtime(config); rt.verify()
    save(out / 'protocol.json', {'cases': CASES, 'transportModes': [False, True],
         'maxApplicationExecutions': 10, 'fixtureNow': '2030-01-01T14:55:00',
         'scope': 'Controlled real application steps and exact selected event handler; no generated package or new score.',
         'runtime': config, 'sourceHashes': {p.name: digest(p) for p in source.iterdir()}})
    mount = ['docker', 'run', '--rm', '--cpus', '2', '--memory', '3g', '--network', 'none',
             '-v', str(batch.TARGET) + ':/reports:ro', '-v', str(out) + ':/out']
    (out / 'classes').mkdir()
    command = [*mount, '--entrypoint', 'javac', config['image'], '-cp', config['classpath'],
               '-d', '/out/classes', '/out/source/StaleWriteExperiment.java']
    save(out / 'compile-command.json', command)
    result = batch.run_process(command, out / 'compile.log', 120, lambda: None)
    if result.get('exitCode') != 0:
        raise ValueError('Experiment compilation failed: ' + (out / 'compile.log').read_text())
    processes = []
    for mode in (False, True):
        for case in CASES:
            label = case + '-' + str(mode).lower()
            name = 'stale-write-' + label
            command = [*mount, '--name', name, '--entrypoint', 'java', config['image'],
                '-Xmx1536m', '-XX:MaxMetaspaceSize=512m', '-Dlocal.messaging.serialize=' + str(mode).lower(),
                '-Dexperiment.fixtureNow=2030-01-01T14:55:00', '-cp', '/out/classes:' + config['classpath'],
                'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite.StaleWriteExperiment',
                case, '/out/' + label + '.json']
            save(out / (label + '-command.json'), command)
            result = batch.run_process(command, out / (label + '.log'), 180,
                                       lambda: batch.remove_container(name))
            processes.append({'case': label, **result})
            save(out / 'processes.json', processes)
            if result.get('exitCode') != 0:
                raise ValueError('Experiment failed: ' + label)
            print('Completed ' + label, flush=True)
    rt.verify()
    for name, expected in read(out / 'protocol.json')['sourceHashes'].items():
        if digest(HERE / name) != expected:
            raise ValueError('Experiment source changed during execution')
    save(out / 'artifact-hashes.json', {str(p.relative_to(out)): digest(p)
         for p in out.rglob('*') if p.is_file()})


if __name__ == '__main__':
    main()
