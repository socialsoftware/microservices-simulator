"""Verify immutable inputs and report before/after coverage without changing frozen maps."""
from collections import Counter
from pathlib import Path
import xml.etree.ElementTree as ET
from qualify import ROOT, OUT, EVIDENCE, SOURCES, DIAG, read, save, digest, package, retained_attempt, FIT, Runtime
from fitness import components, assess


def main():
    selection = read(EVIDENCE / 'selection.json')
    assert digest(DIAG) == selection['diagnosisSha256']
    assert read(OUT / 'source-hashes.json') == {str(p.relative_to(ROOT)): digest(p) for p in SOURCES}
    references, results, old_reports = {}, [], 0
    runtimes = set()
    for row in selection['selected']:
        ref = ROOT / row['reference']
        if str(ref) not in references:
            assert digest(ref) == row['referenceSha256']
            references[str(ref)] = read(ref)
        old = references[str(ref)]['observations'][row['candidate']]
        directory = Path(row['oldDirectory'])
        assert digest(directory / 'replay.json') == row['runtimeHash']
        for name, sha in row['oldReportHashes'].items():
            assert digest(directory / name) == sha, str(directory / name)
            old_reports += 1
        attempt = OUT / row['candidate'] / 'attempt-002/attempt.json'
        if not attempt.exists():
            continue
        runtime_path = attempt.parent.parent / 'runtime.json'
        runtime_hash = digest(runtime_path)
        if runtime_hash not in runtimes:
            Runtime(read(runtime_path)).verify()
            runtimes.add(runtime_hash)
        new = retained_attempt(attempt)
        assert new['candidate'] == row['candidateData']
        assert new['packageHashes'] == old['packageHashes']
        old_components, new_components = components(old), components(new)
        score = assess(new, FIT)
        complete = new['lostCopiedUpdateCoverage'] == 'COMPLETE_WITHIN_SCOPE' and new['lostCopiedUpdateValidity'] == 'COMPLETE'
        results.append({'candidate': row['candidate'], 'family': row['family'], 'workload': row['workload'],
                        'copyVerdict': ('POSITIVE' if new['lostCopiedUpdateCount'] else 'NEGATIVE') if complete else 'UNAVAILABLE',
                        'jointVerdict': 'UNAVAILABLE' if score['fitnessScore'] is None else 'POSITIVE' if score['fitnessScore'] > 0 else 'NEGATIVE',
                        'jointScore': score['fitnessScore'], 'executionStatus': new['status'],
                        'otherCriteriaUnchanged': old_components == new_components,
                        'executionUnchanged': all(old.get(k) == new.get(k) for k in ['status', 'terminalStatus', 'scheduleConformance']),
                        'oldReferenceSha256': row['referenceSha256'], 'newAttemptSha256': digest(attempt)})
    tests = []
    for name in ['CopiedUpdateSessionSpec', 'LostCopiedUpdateAssessorSpec', 'ConstructorCopyVisitorDummyappSpec', 'ConstructorCopyVisitorQuizzesSpec']:
        p = next((OUT / 'validation').glob('TEST-*.' + name + '.xml'))
        r = ET.parse(p).getroot()
        tests.append({'name': name, **{k: int(r.attrib[k]) for k in ['tests', 'failures', 'errors', 'skipped']}, 'reportSha256': digest(p)})
    summary = {'selected': len(selection['selected']), 'completed': len(results), 'excludedAsync': len(selection['excluded']),
               'oldReferencesVerified': len(references), 'oldReportHashesVerified': old_reports,
               'runtimeDescriptorsVerified': sorted(runtimes),
               'copy': dict(Counter(r['copyVerdict'] for r in results)),
               'joint': dict(Counter(r['jointVerdict'] for r in results)),
               'otherCriteriaChanged': sum(not r['otherCriteriaUnchanged'] for r in results),
               'executionChanged': sum(not r['executionUnchanged'] for r in results),
               'executionStatuses': dict(Counter(r['executionStatus'] for r in results)),
               'otherCriteriaChangedOnCompletedExecutions': sum(not r['otherCriteriaUnchanged']
                   for r in results if r['executionStatus'] == 'COMPLETE'),
               'tests': tests, 'sourceHashes': read(OUT / 'source-hashes.json'),
               'selectionSha256': digest(EVIDENCE / 'selection.json'), 'rows': results}
    save(EVIDENCE / 'audit.json', summary)
    print({k:v for k,v in summary.items() if k not in ['rows', 'tests', 'sourceHashes']})

if __name__ == '__main__': main()
