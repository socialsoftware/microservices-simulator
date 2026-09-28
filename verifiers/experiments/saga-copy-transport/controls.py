"""Fresh regression of the ten established Quizzes histories under the new observer."""
from pathlib import Path
import shutil
from qualify import ROOT, OUT, EVIDENCE, ComposeRuntime, Runtime, batch, read, save, digest

BASE = ROOT / 'verifiers/target/lost-copied-update/integration-03'
DEST = OUT / 'controls'
MAIN = 'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.experiments.stalewrite.IntegratedCopiedUpdateExperiment'

def main():
    DEST.mkdir(exist_ok=True)
    (DEST / 'package').mkdir(exist_ok=True)
    shutil.copy2(BASE / 'copy-contracts.json', DEST / 'package/copy-contracts.json')
    rt = read(BASE / 'runtime.json')
    rt['executorMain'] = MAIN
    rt['classpath'] = batch.container(OUT / 'overlay') + ':' + rt['classpath']
    rt['hashes'].update(read(OUT / 'overlay-hashes.json'))
    rows = []
    for serialized in (False, True):
        rt['javaOptions'] = ['-Xmx1536m', '-XX:MaxMetaspaceSize=512m',
            '-Dlocal.messaging.serialize=' + str(serialized).lower(), '-Dexperiment.fixtureNow=2030-01-01T14:55:00']
        runtime = ComposeRuntime(rt)
        runtime.verify()
        for case in ['forward-stale', 'forward-fresh', 'recovery-stale', 'recovery-delayed-event', 'recovery-no-event']:
            label = case + '-' + str(serialized).lower()
            result = DEST / (label + '.json')
            if not result.exists():
                process = runtime.launch(DEST, label, MAIN, [case, '/out/' + result.name], 180)
                save(DEST / (label + '-process.json'), process)
                if process['status'] != 'EXITED':
                    raise RuntimeError(label + ': ' + str(process))
            new, old = read(result), read(BASE / result.name)
            assessment = new['copiedUpdateAssessment']
            expected = 1 if case in ('forward-stale', 'recovery-stale') else 0
            assert len(assessment['findings']) == expected, label
            assert not assessment['coverageGaps'], (label, assessment['coverageGaps'])
            assert new['status'] == old['status'], label
            rows.append({'case': label, 'findings': expected, 'gaps': 0,
                         'oldSha256': digest(BASE / result.name), 'newSha256': digest(result),
                         'newReport': str(result.relative_to(ROOT))})
            save(EVIDENCE / 'regression-controls.json', {'rows': rows, 'positive': sum(r['findings'] > 0 for r in rows),
                                                      'negative': sum(r['findings'] == 0 for r in rows)})
            print(label, expected, 'no gaps', flush=True)

if __name__ == '__main__': main()
