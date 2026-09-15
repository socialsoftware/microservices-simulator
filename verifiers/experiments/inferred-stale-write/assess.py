#!/usr/bin/env python3
"""Assess instrumented copy histories using inferred contracts, without application names."""
import argparse
import json
import re
from pathlib import Path


class AbsentEntry(Exception):
    pass


def locate(data, path):
    """Runtime paths carry collection identity fields inferred from constructor copies."""
    if not path.startswith('$.'):
        raise ValueError('Unsupported root path')
    for part in path[2:].split('.'):
        match = re.fullmatch(r'([^\[]+)(?:\[([^=]+)=(.*?)\])?', part)
        if not match:
            raise ValueError('Unsupported path segment')
        name, key, value = match.groups()
        data = data[name]
        if key is not None:
            found = [v for v in data if str(v.get(key)) == value]
            if not found:
                raise AbsentEntry('No prior collection entry to overwrite')
            if len(found) != 1:
                raise ValueError('Missing or ambiguous collection key')
            data = found[0]
    return data


def actor(writer):
    if not writer or not writer.get('sagaInstanceId'):
        return None
    return (writer.get('executionAttemptId'), writer.get('sagaInstanceId'))


def assess(trace, baseline):
    events = trace['events']
    indexed = {e['order']: e for e in events}
    writes = [e for e in events if e['kind'] == 'COMMITTED_WRITE']
    findings, gaps = [], list(trace['gaps'])
    for event in events:
        if event['kind'] != 'REGISTERED_COPY':
            continue
        registered = event['data']
        construction = indexed.get(registered['copyOrder'])
        if not construction or construction['kind'] != 'CONSTRUCTOR_COPY':
            gaps.append('Registered object has no constructor-origin observation')
            continue
        copy = construction['data']
        read = indexed.get(copy['readOrder'])
        command = indexed.get(copy['call'])
        if not read or read['kind'] != 'RESPONSE' or not command or command['kind'] != 'COMMAND_INPUT' \
                or not read['order'] < command['order'] < construction['order']:
            gaps.append('Missing or unordered response/input/copy chain')
            continue
        matches = [w for w in writes if w['data']['aggregate']['identity']['aggregateId'] == registered['aggregateId']
                   and w['data']['aggregate']['version'] == registered['version']
                   and w['data']['writer'] == registered['writer']]
        if len(matches) != 1:
            gaps.append('Registered copy has no unique matching committed write')
            continue
        after = matches[0]
        if after['order'] <= construction['order']:
            gaps.append('Persistence precedes constructor observation')
            continue
        owner = actor(after['data']['writer'])
        if owner is None or owner != actor(copy['readWriter']):
            continue
        history = [w for w in writes if w['data']['aggregate']['identity'] == after['data']['aggregate']['identity']
                   and w['order'] < after['order']]
        if not history:
            continue
        foreign = history[-1]
        if actor(foreign['data']['writer']) in (None, owner) or foreign['order'] <= copy['readOrder']:
            continue
        contract = next(c for c in trace['contracts'] if c['sourceType'] == copy['sourceType'] and c['targetType'] == copy['targetType'])
        metadata = {contract['fields'].get(f) for f in ('aggregateId', 'version')}
        try:
            before_values = locate(foreign['data']['aggregate']['applicationData'], registered['path'])
            after_values = locate(after['data']['aggregate']['applicationData'], registered['path'])
            preceding = [w for w in history if w['order'] < foreign['order']]
            prior_snapshot = preceding[-1]['data']['aggregate'] if preceding else next(
                s for s in baseline if s['identity'] == after['data']['aggregate']['identity'])
            prior_values = locate(prior_snapshot['applicationData'], registered['path'])
        except AbsentEntry:
            continue
        except (KeyError, ValueError, StopIteration, TypeError):
            gaps.append('Unavailable persisted path for constructed copy')
            continue
        for field, old in registered['values'].items():
            if field in metadata:
                continue
            if field not in prior_values or field not in before_values or field not in after_values:
                continue
            if prior_values[field] == old and before_values[field] != old and after_values[field] == old:
                findings.append({'aggregate': after['data']['aggregate']['identity'],
                    'fieldPath': registered['path'] + '.' + field,
                    'oldValue': old, 'overwrittenValue': before_values[field],
                    'phase': after['data']['writer']['phase'], 'readOrder': copy['readOrder'],
                    'inputOrder': copy['call'], 'constructorOrder': registered['copyOrder'],
                    'foreignWriteOrder': foreign['order'], 'overwriteOrder': after['order'],
                    'beforeVersion': foreign['data']['aggregate']['version'], 'afterVersion': after['data']['aggregate']['version']})
    # One observed copied field per overwrite, even if runtime aliases visit the same object twice.
    unique = {(json.dumps(f['aggregate'], sort_keys=True), f['fieldPath'], f['afterVersion']): f for f in findings}
    return {'findings': list(unique.values()), 'coverageGaps': sorted(set(gaps)),
            'scope': 'Observed unchanged scalar copies through inferred constructors; not complete lost-update detection.'}


def main():
    p = argparse.ArgumentParser()
    p.add_argument('--run', type=Path, required=True)
    args = p.parse_args()
    rows = []
    for path in sorted(args.run.glob('*.json.trace.json')):
        report = json.loads(Path(str(path).removesuffix('.trace.json')).read_text())
        result = assess(json.loads(path.read_text()), report['persistentInitial']['aggregates'])
        rows.append({'case': report['caseId'], 'serialized': report['serialized'], **result})
    out = {'executions': len(rows), 'positiveExecutions': sum(bool(r['findings']) for r in rows), 'rows': rows}
    (args.run / 'assessment.json').write_text(json.dumps(out, indent=2) + '\n')
    print(json.dumps(out, indent=2))


if __name__ == '__main__': main()
