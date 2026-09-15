#!/usr/bin/env python3
"""Small fresh-process observation-cost check; not a general performance benchmark."""
import argparse
import json
from pathlib import Path
import sys

from qualify import run, TARGET
sys.path.insert(0, str(Path(__file__).resolve().parent.parent / 'fixed-workload-ga'))
from runtime import Runtime, save
from validate import comparable


def main():
    p = argparse.ArgumentParser(); p.add_argument('--run', type=Path, required=True); a = p.parse_args()
    root = a.run.resolve(); config = json.loads((root / 'runtime.json').read_text())
    Runtime(config).verify()
    out = root / 'measurement'; out.mkdir(exist_ok=False)
    source = (root / 'source/IntegratedCopiedUpdateExperiment.java').read_text()
    old = 'copySession = pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation.start(caseId,'
    assert source.count(old) == 1
    source = source.replace(old, 'copySession = Boolean.getBoolean("qualification.disable") ? null : pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation.start(caseId,')
    (out / 'IntegratedCopiedUpdateExperiment.java').write_text(source)
    (out / 'classes').mkdir()
    mount = ['docker', 'run', '--rm', '--cpus', '2', '--memory', '3g', '--network', 'none',
             '-v', str(TARGET) + ':/reports:ro', '-v', str(root) + ':/out']
    run([*mount, '--entrypoint', 'javac', config['image'], '-cp', config['classpath'], '-d', '/out/measurement/classes',
         '/out/measurement/IntegratedCopiedUpdateExperiment.java'], out / 'compile.log')
    rows = []
    for pair, order in enumerate([(False, True), (True, False)]):
        values = []
        for enabled in order:
            label = f'pair-{pair}-' + ('enabled' if enabled else 'disabled')
            java = ['-Xmx1536m', '-Dlocal.messaging.serialize=true', '-Dexperiment.fixtureNow=2030-01-01T14:55:00',
                    '-Dsimulator.copied-update.contracts=/out/copy-contracts.json', '-Dsimulator.copied-update.source-root=/out/source/quizzes']
            if enabled: java += ['-javaagent:/out/copy-agent.jar']
            else: java += ['-Dqualification.disable=true']
            result = run([*mount, '--entrypoint', 'java', config['image'], *java,
                '-cp', '/out/measurement/classes:' + config['classpath'],
                'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite.IntegratedCopiedUpdateExperiment',
                'forward-stale', '/out/measurement/' + label + '.json'], out / (label + '.log'))
            report = json.loads((out / (label + '.json')).read_text()); assert report['status'] == 'PASS'
            values.append(comparable(report))
            rows.append({'pair': pair, 'enabled': enabled, 'process': result,
                         'traceBytes': len(json.dumps(report.get('copiedUpdateTrace', {})).encode()) if enabled else 0})
            print(label, result, flush=True)
        assert values[0] == values[1], 'Instrumentation changed business observations'
    save(out / 'summary.json', {'rows': rows, 'businessObservationsPreserved': True,
         'scope': 'Two paired fresh JVM/container launches; wall time includes Docker, JVM and Spring startup. Order reversed in second pair. Descriptive smoke check, not an isolated or statistically established instrumentation overhead.'})
    Runtime(config).verify()


if __name__ == '__main__': main()
