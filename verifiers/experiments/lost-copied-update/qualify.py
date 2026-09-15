#!/usr/bin/env python3
"""Qualify the integrated observer/assessor using the five existing controlled histories.

This does not call the scenario generator; generated-workload qualification is separate.
Build simulator (install) and verifiers (package) first. Frozen Quizzes and its dependency
runtime come from the earlier proof; current simulator/verifier classes take precedence.
"""
import argparse
import json
from pathlib import Path
import shutil
import sys

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
TARGET = ROOT / 'verifiers/target'
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from runtime import Runtime, batch, digest, save


def prepare(out):
    old = json.loads((TARGET / 'inferred-stale-write/run-05/protocol.json').read_text())['runtime']
    runtime = {**old, 'hashes': {p: h for p, h in old['hashes'].items() if Path(p).is_relative_to(TARGET)}}
    out.mkdir(parents=True, exist_ok=False)
    for module in ('simulator', 'verifiers'):
        shutil.copytree(ROOT / module / 'target/classes', out / 'runtime' / module)
    shutil.copy2(ROOT / 'verifiers/target/verifiers-0.0.1-SNAPSHOT-copy-agent.jar', out / 'copy-agent.jar')
    prefix = batch.container(out)
    runtime['classpath'] = prefix + '/runtime/simulator:' + prefix + '/runtime/verifiers:' + old['classpath']
    runtime['lostCopiedUpdateSourceRoot'] = str(out / 'source/quizzes')
    runtime['lostCopiedUpdateAgent'] = {'path': str(out / 'copy-agent.jar'), 'sha256': digest(out / 'copy-agent.jar')}
    source = out / 'source'; source.mkdir()
    for module in ('simulator', 'verifiers'):
        shutil.copytree(ROOT / module / 'src/main', source / module)
    app = ROOT / 'applications/quizzes/src/main/java'
    # The runtime still contains the original application build: verify its source exactly.
    expected = json.loads((TARGET / 'inferred-stale-write/run-05/protocol.json').read_text())['applicationSourceHashes']
    actual = {str(f.relative_to(app)): digest(f) for f in app.rglob('*.java')}
    if expected != actual:
        raise ValueError('Quizzes business source differs from qualified frozen application')
    shutil.copytree(app, source / 'quizzes/src/main/java')
    shutil.copy2(HERE / 'ExtractContracts.java', source / 'ExtractContracts.java')
    harness = (HERE.parent / 'stale-write/StaleWriteExperiment.java').read_text()
    harness = harness.replace('StaleWriteExperiment', 'IntegratedCopiedUpdateExperiment')
    harness = harness.replace('    private final Evidence evidence = new Evidence();', '''    private final Evidence evidence = new Evidence();
    private static pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateSession copySession;
''')
    harness = harness.replace('try(var scope=ImpactEvidenceObserverHolder.install(evidence)) {', '''
        try {
            byte[] contracts = Files.readAllBytes(Path.of(System.getProperty("simulator.copied-update.contracts")));
            copySession = pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation.start(caseId,
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(contracts)));
        } catch(Exception failure) { throw new IllegalStateException(failure); }
        try(var scope=ImpactEvidenceObserverHolder.install(evidence)) {''')
    harness = harness.replace('add("WRITE",new ImpactEvidence.CommittedWrite(rows.size(),a,w));',
        'if(copySession!=null)copySession.committed(a,w);add("WRITE",new ImpactEvidence.CommittedWrite(rows.size(),a,w));')
    harness = harness.replace('if(e!=null)JSON.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),e.report);', '''
            if(e!=null) {
                if(copySession!=null) {
                    var trace=pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation.finish(copySession);
                    e.report.put("copiedUpdateTrace",trace);
                    var baseline=(ImpactEvidence.SnapshotBatch)e.report.get("persistentInitial");
                    e.report.put("copiedUpdateAssessment",new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.LostCopiedUpdateAssessor().assess(trace,baseline.aggregates()));
                }
                JSON.writerWithDefaultPrettyPrinter().writeValue(out.toFile(),e.report);
            }''')
    (source / 'IntegratedCopiedUpdateExperiment.java').write_text(harness)
    (out / 'classes').mkdir()
    mount = ['docker', 'run', '--rm', '--network', 'none', '-v', str(TARGET) + ':/reports:ro', '-v', str(out) + ':/out']
    run([*mount, '--entrypoint', 'javac', runtime['image'], '-cp', runtime['classpath'], '-d', '/out/classes',
         '/out/source/ExtractContracts.java', '/out/source/IntegratedCopiedUpdateExperiment.java'], out / 'compile.log')
    runtime['classpath'] = prefix + '/classes:' + runtime['classpath']
    run([*mount, '--entrypoint', 'java', runtime['image'], '-cp', runtime['classpath'],
         'qualification.copiedupdate.ExtractContracts', '/out/source/quizzes/src/main/java', '/out/copy-contracts.json'], out / 'extract.log')
    for directory in ('runtime', 'classes', 'source'):
        runtime['hashes'].update({str(f): digest(f) for f in (out / directory).rglob('*') if f.is_file()})
    save(out / 'runtime.json', runtime)
    return runtime


def run(command, log):
    result = batch.run_process(command, log, 240, lambda: None)
    if result.get('exitCode') != 0:
        raise RuntimeError(str(log) + ': ' + log.read_text()[-6000:])
    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--reuse', action='store_true')
    parser.add_argument('--prepare-only', action='store_true')
    parser.add_argument('--case', choices=['forward-stale', 'forward-fresh', 'recovery-stale', 'recovery-delayed-event', 'recovery-no-event'])
    args = parser.parse_args()
    out = args.output.resolve()
    runtime = json.loads((out / 'runtime.json').read_text()) if args.reuse else prepare(out)
    Runtime(runtime).verify()
    if args.prepare_only: return
    cases = [args.case] if args.case else ['forward-stale', 'forward-fresh', 'recovery-stale', 'recovery-delayed-event', 'recovery-no-event']
    for serialized in (False, True):
        for case in cases:
            label = case + '-' + str(serialized).lower()
            if (out / (label + '.json')).exists(): continue
            command = ['docker', 'run', '--rm', '--cpus', '2', '--memory', '3g', '--network', 'none',
                '-v', str(TARGET) + ':/reports:ro', '-v', str(out) + ':/out', '--entrypoint', 'java', runtime['image'],
                '-Xmx1536m', '-javaagent:/out/copy-agent.jar', '-Dsimulator.copied-update.contracts=/out/copy-contracts.json',
                '-Dsimulator.copied-update.source-root=/out/source/quizzes',
                '-Dlocal.messaging.serialize=' + str(serialized).lower(), '-Dexperiment.fixtureNow=2030-01-01T14:55:00',
                '-cp', runtime['classpath'],
                'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite.IntegratedCopiedUpdateExperiment',
                case, '/out/' + label + '.json']
            save(out / (label + '-command.json'), command)
            result = run(command, out / (label + '.log'))
            report = json.loads((out / (label + '.json')).read_text())
            assessment = report['copiedUpdateAssessment']
            print(label, 'findings=', len(assessment['findings']), 'gaps=', len(assessment['coverageGaps']), flush=True)
            save(out / (label + '-process.json'), result)
    Runtime(runtime).verify()


if __name__ == '__main__': main()
