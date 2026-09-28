"""Run the current Java assessor over retained evidence, then recompute unchanged fitness.

Requires the verifier build and dependency classpath described in README.md.
No application actions or adaptive search are executed. Original reports stay untouched.
"""
import collections
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

REPO = Path(__file__).resolve().parents[4]
CAMPAIGN = REPO / 'verifiers/target/ga-500x3-2026-09-15'
OUTPUT = (Path(sys.argv[1]).resolve() if len(sys.argv) > 1
          else REPO / 'verifiers/target/exclusive-field-residuals-2026-09-16')
sys.path.insert(0, str(REPO / 'verifiers/experiments/fixed-workload-ga'))
import fitness


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def normalized(value):
    if isinstance(value, dict):
        return {k: normalized(v) for k, v in value.items() if v is not None and not (k == 'affectedFields' and not v)}
    if isinstance(value, list):
        return [normalized(v) for v in value]
    return value


arms = [(p.parent.name, json.loads(p.read_text())) for p in sorted(CAMPAIGN.glob('*/results.json'))]
manifest, source_hashes = [], {}
for arm, results in arms:
    for a in results['attempts']:
        directory = Path(a['directory'])
        for filename, expected in a['reportHashes'].items():
            path = directory / filename
            assert digest(path) == expected, path
            source_hashes[str(path)] = expected
        manifest.append({'name': f'{arm}-attempt-{a["attempt"]:03d}',
                         'execution': str(directory / 'execution-report.json'),
                         'impact': str(directory / 'execution-report.impact-v2.json')})
assert len(manifest) == 3000
manifest_path = OUTPUT / 'inputs.json'
manifest_path.write_text(json.dumps(manifest, indent=2) + '\n')
java_home = Path(os.environ['JAVA_HOME'])
classpath = os.pathsep.join([str(REPO / 'verifiers/target/classes'), (OUTPUT / 'classpath.txt').read_text().strip()])
source = REPO / 'verifiers/experiments/recovered-creation-remnants/Reassess.java'
subprocess.run([str(java_home / 'bin/javac'), '-cp', classpath, '-d', str(OUTPUT / 'classes'), str(source)], check=True)
subprocess.run([str(java_home / 'bin/java'), '-Xmx1g', '-cp', str(OUTPUT / 'classes') + os.pathsep + classpath,
                'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.Reassess',
                str(manifest_path), str(OUTPUT / 'reassessment')], check=True)

summary = {'mode': 'OFFLINE_REASSESSMENT_NOT_A_NEW_SEARCH', 'attempts': 3000,
           'residualPolicy': 'exclusive-observed-fields-v2', 'originalReportHashesVerified': len(source_hashes),
           'oldCompleteScoresChanged': 0, 'otherPersistentCategoriesChanged': 0,
           'newCompleteScores': 0, 'newCompleteResiduals': 0, 'arms': []}
rows = []
unknowns = collections.Counter()
for arm, results in arms:
    counts = collections.Counter()
    for a in results['attempts']:
        name = f'{arm}-attempt-{a["attempt"]:03d}'
        before = json.loads((Path(a['directory']) / 'execution-report.impact-v2.json').read_text())
        after = json.loads((OUTPUT / 'reassessment' / (name + '.impact-v2.json')).read_text())
        assert after['residualAssessmentPolicy'] == summary['residualPolicy']
        bcat = {c['category']: c for c in before['categoryResults']}
        acat = {c['category']: c for c in after['categoryResults']}
        for category in ['DELETED_DEPENDENCY', 'UNRESOLVED_DELIVERED_EVENT']:
            summary['otherPersistentCategoriesChanged'] += normalized(bcat[category]) != normalized(acat[category])
        if before['assessmentStatus'] == 'COMPLETE':
            assert normalized(before['categoryResults']) == normalized(after['categoryResults']), name
        result = dict(a, impactCategories=after['categoryResults'], I=after['completeScore'])
        if a['status'] in ['COMPLETE', 'PARTIAL', 'UNAVAILABLE']:
            result['status'] = after['assessmentStatus']
        assessed = fitness.assess(result, fitness.configuration(results['fitness']))
        old, new = a['fitnessScore'], assessed['fitnessScore']
        summary['oldCompleteScoresChanged'] += old is not None and old != new
        summary['newCompleteScores'] += old is None and new is not None
        summary['newCompleteResiduals'] += (bcat['FAILED_OPERATION_RESIDUAL']['coverageStatus'] == 'PARTIAL'
                                            and acat['FAILED_OPERATION_RESIDUAL']['coverageStatus'] == 'COMPLETE')
        counts['oldUnavailable'] += old is None
        counts['newUnavailable'] += new is None
        counts['newPositive'] += new is not None and new > 0
        counts['newZero'] += new == 0
        for u in acat['FAILED_OPERATION_RESIDUAL']['unknownReasons']:
            unknowns[u['reason']] += 1
        rows.append({'name': name, 'key': a['key'], 'oldScore': old, 'newScore': new,
                     'unavailableReasons': assessed['fitnessUnavailableReasons']})
    summary['arms'].append({'arm': arm, **counts})
for path, expected in source_hashes.items():
    assert digest(Path(path)) == expected, path
assert summary['oldCompleteScoresChanged'] == 0
assert summary['otherPersistentCategoriesChanged'] == 0
summary['remainingResidualReasons'] = dict(unknowns)
summary['newUnavailable'] = sum(a['newUnavailable'] for a in summary['arms'])
summary['sources'] = {str(p.relative_to(REPO)): digest(p) for p in [
    source,
    REPO / 'verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2Assessor.java',
    REPO / 'verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/ImpactV2EvidenceReport.java']}
(OUTPUT / 'fitness-comparison.json').write_text(json.dumps({'summary': summary, 'attempts': rows}, indent=2) + '\n')
Path(__file__).with_name('reassessment-summary.json').write_text(json.dumps(summary, indent=2) + '\n')
print(json.dumps(summary, indent=2))
