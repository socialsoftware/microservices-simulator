#!/usr/bin/env python3
"""Compare the fixed 14 histories before/after command-scope classification."""
import copy
import hashlib
import json
import re
import sys
from pathlib import Path

before, after = (Path(p).resolve() for p in sys.argv[1:3])
read = lambda p: json.loads(p.read_text())
digest = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()


def normalize(value, attempt):
    if isinstance(value, dict):
        return {k: normalize(v, attempt) for k, v in value.items()}
    if isinstance(value, list):
        return [normalize(v, attempt) for v in value]
    return value.replace(attempt, "ATTEMPT") if isinstance(value, str) else value


def execution(value):
    value = copy.deepcopy(value)
    value['sourceSetup'].pop('durationNanos')
    for binding in value['sourceSetup']['participantBindings']:
        text = binding.get('resolvedValue')
        if isinstance(text, str) and re.fullmatch(
                r'pt\.ulisboa\.tecnico\.socialsoftware\.quizzes\.microservices\.tournament\.aggregate\.TournamentDto@[0-9a-f]+', text):
            binding['resolvedValue'] = text.split('@')[0] + '@OBJECT_IDENTITY'
    return value


rows = []
for case in read(after / 'selection.json'):
    label = case['label']
    old, new = before / label, after / label
    old_attempt = read(old / 'execution-report.json')['executionAttemptId']
    new_attempt = read(new / 'execution-report.json')['executionAttemptId']
    hashes = {}
    for name in ('execution-report.json', 'execution-report.impact.json', 'execution-report.impact-v2.json'):
        a, b = normalize(read(old / name), old_attempt), normalize(read(new / name), new_attempt)
        if name == 'execution-report.json':
            a, b = execution(a), execution(b)
        assert a == b, (label, name)
        hashes[name] = dict(before=digest(old / name), after=digest(new / name))
    name = 'execution-report.saga-read-exposure.json'
    a, b = normalize(read(old / name), old_attempt), normalize(read(new / name), new_attempt)
    assert a['observedExposureCount'] == b['observedExposureCount']
    assert b['collectionCoverage'] == 'COMPLETE_WITHIN_SCOPE' and not b['gaps']
    assert len(a['calls']) == len(b['calls'])
    changes = []
    for x, y in zip(a['calls'], b['calls']):
        if x == y:
            continue
        assert x['id'] == y['id'] and x['order'] == y['order']
        u, v = x['observation'], y['observation']
        assert u['commandType'].endswith('.CommitSagaCommand') and u['reader'] is None
        assert (u['outcome'], u['reason']) == ('DELIVERED_INVALID', 'MISSING_READER_ATTRIBUTION')
        assert (v['outcome'], v['reason']) == ('DELIVERED_UNMAPPED', 'NO_READ_ADAPTER')
        assert {k: z for k, z in u.items() if k not in ('outcome', 'reason')} == {
            k: z for k, z in v.items() if k not in ('outcome', 'reason')}
        changes.append(dict(callId=x['id'], command=u['commandType']))
    assert len(changes) == (1 if case['control'] else 0)
    assert [{k: v for k, v in f.items() if k != 'id'} for f in a['findings']] == [
        {k: v for k, v in f.items() if k != 'id'} for f in b['findings']]
    hashes[name] = dict(before=digest(old / name), after=digest(new / name))
    rows.append(dict(label=label, oldCoverage=a['collectionCoverage'], newCoverage=b['collectionCoverage'],
                     observedExposureCount=b['observedExposureCount'], changedCalls=changes, hashes=hashes))

assert len(rows) == 14 and sum(len(r['changedCalls']) for r in rows) == 6
assert sum(r['observedExposureCount'] for r in rows) == 3
result = dict(before=str(before), after=str(after), rows=rows, matchedReportPairs=42,
              normalization=['exact execution-attempt ID', 'sourceSetup.durationNanos',
                             'sourceSetup.participantBindings TournamentDto default Object.toString identity'],
              packageByteIdentical=all((after/'package'/p.name).read_bytes() == p.read_bytes()
                                      for p in (before/'package').iterdir() if p.is_file()))
assert result['packageByteIdentical']
(after / 'comparison.json').write_text(json.dumps(result, indent=2) + '\n')
print('14 histories: 42 equivalent execution/impact report pairs; six corrected calls; three unchanged exposures.')
