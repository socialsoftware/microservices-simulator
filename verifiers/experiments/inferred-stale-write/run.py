#!/usr/bin/env python3
"""Freeze the existing runtime; extract copy contracts and observe real controlled executions."""
import argparse
import json
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from qualify import freeze_runtime
from runtime import Runtime, batch, digest, save


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--output', type=Path, required=True)
    p.add_argument('--prepare-only', action='store_true')
    p.add_argument('--reuse-prepared', action='store_true')
    a = p.parse_args()
    out = a.output.resolve()
    if a.reuse_prepared:
        config = json.loads((out / 'protocol.json').read_text())['runtime']
    else:
        out.mkdir(parents=True, exist_ok=False)
        config = freeze_runtime()
    rt = Runtime(config); rt.verify()
    mount = ['docker', 'run', '--rm', '--cpus', '2', '--memory', '3g', '--network', 'none',
             '-v', str(batch.TARGET) + ':/reports:ro', '-v', str(out) + ':/out']
    def run(args, label, timeout=120):
        result = batch.run_process(args, out / (label + '.log'), timeout, lambda: None)
        if result.get('exitCode') != 0:
            raise RuntimeError(label + ': ' + (out / (label + '.log')).read_text()[-8000:])
    if not a.reuse_prepared:
        source = out / 'source'; source.mkdir()
        for f in HERE.glob('*'):
            if f.suffix in ('.java', '.py'): shutil.copy2(f, source / f.name)
        fixtures = ROOT / 'applications/dummyapp/src/main/java/com/example/dummyapp/inferredcopy'
        shutil.copytree(fixtures, out / 'fixture-source')
        for f in fixtures.glob('*.java'): shutil.copy2(f, source / f.name)
        app = ROOT / 'applications/quizzes/src/main/java'
        shutil.copytree(app, out / 'application-source')
        base = HERE.parent / 'stale-write/StaleWriteExperiment.java'
        harness = base.read_text().replace('StaleWriteExperiment', 'InferredStaleWriteExperiment')
        harness = harness.replace('try(var scope=ImpactEvidenceObserverHolder.install(evidence)) {',
                                  'experiment.inferred.Probe.start();\n        try(var scope=ImpactEvidenceObserverHolder.install(evidence)) {')
        harness = harness.replace('add("WRITE",new ImpactEvidence.CommittedWrite(rows.size(),a,w));',
                                  'experiment.inferred.Probe.committed(a,w);add("WRITE",new ImpactEvidence.CommittedWrite(rows.size(),a,w));')
        harness = harness.replace('if(e!=null)JSON.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),e.report);',
                                  'experiment.inferred.Probe.dump(Path.of(out.toString()+".trace.json"));\n            if(e!=null)JSON.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),e.report);')
        (source / 'InferredStaleWriteExperiment.java').write_text(harness)
        (out / 'classes').mkdir()
        run([*mount, '--entrypoint', 'javac', config['image'], '-cp', config['classpath'], '-d', '/out/classes',
             *['/out/source/' + f.name for f in source.glob('*.java')]], 'compile')
        (out / 'manifest.mf').write_text('Premain-Class: experiment.inferred.Agent\n\n')
        run([*mount, '--entrypoint', 'jar', config['image'], 'cfm', '/out/agent.jar', '/out/manifest.mf',
             '-C', '/out/classes', 'experiment'], 'jar')
        run([*mount, '--entrypoint', 'java', config['image'], '-cp', '/out/classes:' + config['classpath'],
             'experiment.inferred.ExtractCopies', '/out/application-source', '/out/contracts.json'], 'extract')
        save(out / 'protocol.json', {'runtime': config, 'scope': 'Automatic direct-copy inference plus runtime provenance; controlled application histories.',
             'applicationSourceHashes': {str(f.relative_to(out / 'application-source')): digest(f) for f in (out / 'application-source').rglob('*.java')},
             'originalHarnessHash': digest(base), 'sourceHashes': {f.name: digest(f) for f in source.iterdir()},
             'contractsHash': digest(out / 'contracts.json'), 'agentHash': digest(out / 'agent.jar')})
        print((out / 'extract.log').read_text(), flush=True)
    if a.prepare_only: return
    for mode in (False, True):
        for case in ('forward-stale', 'forward-fresh', 'recovery-stale', 'recovery-delayed-event', 'recovery-no-event'):
            label = case + '-' + str(mode).lower()
            if (out / (label + '.json')).exists(): continue
            command = [*mount, '--entrypoint', 'java', config['image'], '-Xmx1536m', '-XX:MaxMetaspaceSize=512m',
                 '-javaagent:/out/agent.jar', '-Dexperiment.copyContracts=/out/contracts.json',
                 '-Dlocal.messaging.serialize=' + str(mode).lower(), '-Dexperiment.fixtureNow=2030-01-01T14:55:00',
                 '-cp', '/out/classes:' + config['classpath'],
                 'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite.InferredStaleWriteExperiment',
                 case, '/out/' + label + '.json']
            save(out / (label + '-command.json'), command)
            run(command, label, 180)
            print('Completed ' + label, flush=True)
    rt.verify()
    save(out / 'artifact-hashes.json', {str(f.relative_to(out)): digest(f) for f in out.rglob('*') if f.is_file() and f.name != 'artifact-hashes.json'})


if __name__ == '__main__': main()
