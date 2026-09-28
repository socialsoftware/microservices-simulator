import collections
import csv
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]
TARGET = ROOT / 'verifiers/target'
OUT = Path(__file__).resolve().parent
OLD = ROOT / 'docs/verifiers-impl/evidence/transfer-inventory-2026-09-21/inventory.json'
CRITERIA = ('DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT', 'COMPENSATED_READ_EXPOSURE', 'LOST_COPIED_UPDATE')
COMPLETE = {'COMPLETE', 'COMPLETE_WITHIN_SCOPE'}


def rel(path):
    return str(Path(path).resolve().relative_to(ROOT))


def digest(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def stable(x):
    return json.dumps(x, sort_keys=True, ensure_ascii=False, separators=(',', ':'))


def runtime_info(path, wid):
    for base in (path.parent, path.parent.parent, path.parent.parent.parent):
        for name in ('protocol.json', 'config.json'):
            q = base / name
            if not q.exists():
                continue
            try:
                data = json.loads(q.read_text())
            except (ValueError, OSError):
                continue
            config = data.get('config', data)
            if config.get('workload') != wid or not isinstance(config.get('runtime'), dict):
                continue
            rt = config['runtime']
            return {'signature': hashlib.sha256(json.dumps(rt, sort_keys=True).encode()).hexdigest(), 'image': rt.get('image'),
                    'hashCount': len(rt.get('hashes', {})), 'config': rel(q)}
    return {'signature': None, 'image': None, 'hashCount': None, 'config': None}


def workload_record(path, wid):
    for base in (path.parent, path.parent.parent, path.parent.parent.parent, path.parent.parent.parent.parent):
        q = base / 'workload.json'
        if q.exists():
            try:
                data = json.loads(q.read_text())
                if data.get('id') == wid:
                    return data, rel(q)
            except (ValueError, OSError):
                pass
        q = base / 'package/workloads.jsonl'
        if q.exists():
            try:
                with q.open() as f:
                    for line in f:
                        data = json.loads(line)
                        if data.get('id') == wid:
                            return data, rel(q)
            except (ValueError, OSError):
                pass
    return None, None


def source_tests(record_source, input_ids):
    if not record_source or not record_source.endswith('workloads.jsonl'):
        return []
    path = ROOT / record_source
    inputs_file = path.with_name('inputs.jsonl')
    if not inputs_file.exists():
        return []
    sources = []
    with inputs_file.open() as f:
        for line in f:
            item = json.loads(line)
            if item.get('id') not in input_ids:
                continue
            source = item.get('source') or {}
            if source.get('testClass') and source.get('method'):
                sources.append({'test': source['testClass'].rsplit('.', 1)[-1] + '.' + source['method'],
                                'role': source.get('testRole')})
    feature_sources = [x['test'] for x in sources if x['role'] == 'featureUnderTest']
    return sorted(set(feature_sources or [x['test'] for x in sources]))


def criterion_state(component):
    if not isinstance(component, dict):
        return 'unavailable'
    count = component.get('count')
    if isinstance(count, (int, float)) and count > 0:
        return 'positive'
    if count == 0 and component.get('coverage') in COMPLETE:
        return 'zero'
    return 'unavailable'


def counters(values):
    values = list(values)
    return {x: sum(v == x for v in values) for x in ('positive', 'zero', 'unavailable')}

old_rows = {x['workload']: x for x in json.loads(OLD.read_text())['rows']}
refs_by_wid = collections.defaultdict(list)
errors = []
for path in sorted(TARGET.rglob('*reference*.json')):
    if not path.is_file():
        continue
    try:
        data = json.loads(path.read_text())
    except (ValueError, OSError):
        continue
    if not (isinstance(data, dict) and {'candidates', 'fitness', 'observations'} <= data.keys()):
        continue
    candidates, fitness, observations = (data[x] for x in ('candidates', 'fitness', 'observations'))
    if set(candidates) != set(fitness) or set(candidates) != set(observations):
        errors.append({'reference': rel(path), 'problem': 'candidate/fitness/observation keys differ'})
        continue
    ids = {c['workload'] for c in candidates.values()}
    if len(ids) != 1:
        errors.append({'reference': rel(path), 'problem': f'{len(ids)} workload ids'})
        continue
    wid = next(iter(ids))
    info = {'path': rel(path), 'sha256': digest(path), 'scenarios': len(candidates),
            'runtime': runtime_info(path, wid), 'data': data}
    refs_by_wid[wid].append(info)

for wid, row in old_rows.items():
    matches = [r for r in refs_by_wid.get(wid, []) if r['path'] == rel(row['reference'])]
    if len(matches) != 1 or matches[0]['sha256'] != row['referenceSha256'] or matches[0]['scenarios'] != row['scenarios']:
        errors.append({'workload': wid, 'problem': 'baseline inventory reference/hash/count mismatch'})

OUT.mkdir(parents=True, exist_ok=True)
workloads = []
scenario_rows = []
reference_rows = []
conflicts = []
for wid in sorted(refs_by_wid):
    refs = refs_by_wid[wid]
    allkeys = set().union(*(set(r['data']['candidates']) for r in refs))
    maxn = max(r['scenarios'] for r in refs)
    eligible = [r for r in refs if r['scenarios'] == maxn]
    if len(allkeys) != maxn:
        errors.append({'workload': wid, 'problem': 'no single complete reference contains all local candidate keys'})
        continue
    preferred = rel(old_rows[wid]['reference']) if wid in old_rows else None
    selected = next((r for r in eligible if r['path'] == preferred), sorted(eligible, key=lambda r: r['path'])[0])
    raw = selected['data']
    for r in refs:
        reference_rows.append({'workload': wid, 'reference': r['path'], 'sha256': r['sha256'],
                               'scenarios': r['scenarios'], 'selected': r is selected, 'runtime': r['runtime']})
    duplicate_count = 0
    for key in sorted(allkeys):
        versions = [(r['path'], r['data']['fitness'][key]) for r in refs if key in r['data']['fitness']]
        if len(versions) > 1:
            duplicate_count += 1
        distinct = {stable(f) for _, f in versions}
        if len(distinct) > 1:
            conflicts.append({'workload': wid, 'candidateKey': key, 'selectedReference': selected['path'],
                              'versions': [{'reference': p, 'fitness': f} for p, f in versions]})
        candidate, f, observation = raw['candidates'][key], raw['fitness'][key], raw['observations'][key]
        cs = {name: criterion_state(f['fitnessComponents'].get(name)) for name in CRITERIA}
        score = f.get('fitnessScore')
        state = 'positive' if isinstance(score, (int, float)) and score > 0 else 'zero' if score == 0 else 'unavailable'
        scenario_rows.append({'workload': wid, 'candidateKey': key, 'scenarioId': candidate['id'],
                              'faultVector': candidate['faultVector'], 'noFault': not any(x == '1' for x in candidate['faultVector']),
                              'score': score, 'scoreState': state, 'attemptStatus': observation.get('status'),
                              'terminalStatus': observation.get('terminalStatus'),
                              'scheduleConformance': observation.get('scheduleConformance'),
                              'unavailableReasons': f.get('fitnessUnavailableReasons', []),
                              'criteria': {name: {'state': cs[name], 'count': f['fitnessComponents'][name].get('count'),
                                                   'coverage': f['fitnessComponents'][name].get('coverage')} for name in CRITERIA},
                              'reference': selected['path']})
    row_scenarios = [x for x in scenario_rows if x['workload'] == wid]
    prior = old_rows.get(wid)
    if prior and prior.get('runtimeSignature'):
        if selected['runtime']['signature'] and selected['runtime']['signature'] != prior['runtimeSignature']:
            errors.append({'workload': wid, 'problem': 'selected runtime signature disagrees with baseline inventory'})
        elif not selected['runtime']['signature']:
            selected['runtime']['signature'] = prior['runtimeSignature']
            selected['runtime']['signatureSource'] = rel(OLD)
    record, record_source = workload_record(ROOT / selected['path'], wid)
    if prior:
        sagas = prior['sagas']
        inputs = prior['inputs']
        event_count = len(prior.get('events', []))
        schedule_steps = prior.get('scheduleSteps')
    elif record:
        sagas = sorted(x['saga'].rsplit('.', 1)[-1].removesuffix('FunctionalitySagas') for x in record['participants'])
        inputs = [x['input'] for x in record['participants']]
        event_count = sum(x.get('kind') == 'event' for x in record.get('schedule', []))
        schedule_steps = sum(x.get('kind') == 'step' for x in record.get('schedule', []))
    else:
        errors.append({'workload': wid, 'problem': 'saga family metadata missing'})
        sagas, inputs, event_count, schedule_steps = [], [], None, None
    tests = source_tests(record_source, set(inputs))
    if prior and record:
        rec_sagas = sorted(x['saga'].rsplit('.', 1)[-1].removesuffix('FunctionalitySagas') for x in record['participants'])
        if sorted(sagas) != rec_sagas:
            errors.append({'workload': wid, 'problem': 'saga names disagree between baseline and workload record'})
    score_counts = counters(x['scoreState'] for x in row_scenarios)
    crit_counts = {name: counters(x['criteria'][name]['state'] for x in row_scenarios) for name in CRITERIA}
    controls = [x for x in row_scenarios if x['noFault']]
    workloads.append({'id': wid, 'label': prior['label'] if prior else None,
                      'sourceTests': tests, 'sagas': sorted(sagas), 'sagaFamily': ' + '.join(sorted(sagas)),
                      'inputs': inputs, 'plannedEventActions': event_count, 'scheduledSteps': schedule_steps,
                      'scenarios': len(row_scenarios), 'joint': score_counts, 'criteria': crit_counts,
                      'attemptStatuses': dict(collections.Counter(x['attemptStatus'] for x in row_scenarios)),
                      'noFaultScenarios': len(controls), 'noFaultPositive': sum(x['scoreState'] == 'positive' for x in controls),
                      'reference': selected['path'], 'referenceSha256': selected['sha256'],
                      'referenceCount': len(refs), 'duplicateCandidateKeys': duplicate_count,
                      'conflictingCandidateKeys': sum(x['workload'] == wid for x in conflicts),
                      'runtime': selected['runtime'], 'metadataSource': rel(OLD) if prior else record_source,
                      'baseline96': bool(prior)})

for name, rows in [('workloads.jsonl', workloads),
                   ('references.jsonl', reference_rows), ('conflicts.jsonl', conflicts)]:
    with (OUT / name).open('w') as f:
        for row in rows:
            f.write(stable(row) + '\n')
(OUT / 'scenarios.jsonl.gz').write_bytes(gzip.compress(
    ''.join(stable(row) + '\n' for row in scenario_rows).encode(), compresslevel=9, mtime=0))
(OUT / 'scenarios.jsonl').unlink(missing_ok=True)

columns = ['id', 'label', 'sourceTests', 'sagaFamily', 'scenarios', 'baseline96', 'runtimeSignature',
           'jointPositive', 'jointZero', 'jointUnavailable', 'noFaultPositive']
for name in CRITERIA:
    columns.extend((name + 'Positive', name + 'Zero', name + 'Unavailable'))
with (OUT / 'workloads.csv').open('w', newline='') as f:
    writer = csv.DictWriter(f, fieldnames=columns)
    writer.writeheader()
    for row in sorted(workloads, key=lambda row: (row['sagaFamily'], row['id'])):
        flat = {'id': row['id'], 'label': row['label'] or '',
                'sourceTests': '; '.join(row['sourceTests']),
                'sagaFamily': row['sagaFamily'], 'scenarios': row['scenarios'],
                'baseline96': row['baseline96'], 'runtimeSignature': row['runtime']['signature'],
                'jointPositive': row['joint']['positive'], 'jointZero': row['joint']['zero'],
                'jointUnavailable': row['joint']['unavailable'], 'noFaultPositive': row['noFaultPositive']}
        for name in CRITERIA:
            for state in ('Positive', 'Zero', 'Unavailable'):
                flat[name + state] = row['criteria'][name][state.lower()]
        writer.writerow(flat)
with (OUT / 'positive-workloads.csv').open('w', newline='') as f:
    columns = ['criterion', 'workload', 'sagaFamily', 'positiveScenarios',
               'totalScenarios', 'sourceTests', 'reference']
    writer = csv.DictWriter(f, fieldnames=columns)
    writer.writeheader()
    positives = []
    for row in workloads:
        for name in CRITERIA:
            count = row['criteria'][name]['positive']
            if count:
                positives.append({'criterion': name, 'workload': row['id'],
                                  'sagaFamily': row['sagaFamily'], 'positiveScenarios': count,
                                  'totalScenarios': row['scenarios'],
                                  'sourceTests': '; '.join(row['sourceTests']),
                                  'reference': row['reference']})
    writer.writerows(sorted(positives, key=lambda row: (
        row['criterion'], -row['positiveScenarios'], row['workload'])))
family = collections.defaultdict(list)
for row in workloads:
    family[row['sagaFamily']].append(row)
with (OUT / 'families.csv').open('w', newline='') as f:
    columns = ['sagaFamily', 'workloads', 'scenarios', 'jointPositive', 'jointUnavailable']
    columns.extend(name + 'Positive' for name in CRITERIA)
    writer = csv.DictWriter(f, fieldnames=columns)
    writer.writeheader()
    for saga_family, members in sorted(family.items()):
        flat = {'sagaFamily': saga_family, 'workloads': len(members),
                'scenarios': sum(x['scenarios'] for x in members),
                'jointPositive': sum(x['joint']['positive'] for x in members),
                'jointUnavailable': sum(x['joint']['unavailable'] for x in members)}
        for name in CRITERIA:
            flat[name + 'Positive'] = sum(x['criteria'][name]['positive'] for x in members)
        writer.writerow(flat)
summary = {'schemaVersion': 'thesis-workload-master-inventory.v1',
           'scope': 'Locally accessible recorded reference sets with matching candidate, observation and fitness keys; all workload IDs retained, no claim of full catalogue coverage or scientific sample trimming.',
           'workloads': len(workloads), 'baseline96Retained': sum(x['baseline96'] for x in workloads),
           'references': len(reference_rows), 'distinctScenarios': len(scenario_rows),
           'sagaFamilies': len(family), 'joint': counters(x['scoreState'] for x in scenario_rows),
           'criteria': {name: counters(x['criteria'][name]['state'] for x in scenario_rows) for name in CRITERIA},
           'workloadsWithPositive': {name: sum(w['criteria'][name]['positive'] > 0 for w in workloads) for name in CRITERIA},
           'familiesWithPositive': {name: sum(any(w['criteria'][name]['positive'] > 0 for w in group) for group in family.values()) for name in CRITERIA},
           'duplicateReferences': len(reference_rows) - len(workloads), 'conflictingCandidateKeys': len(conflicts),
           'attemptStatuses': dict(collections.Counter(x['attemptStatus'] for x in scenario_rows)),
           'noFaultScoreStates': dict(collections.Counter(x['scoreState'] for x in scenario_rows if x['noFault'])),
           'runtimeGroups': dict(collections.Counter(x['runtime']['signature'] for x in workloads)),
           'largestThreeScenarioCount': sum(x['scenarios'] for x in sorted(workloads, key=lambda x: -x['scenarios'])[:3]),
           'errors': errors}
(OUT / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, sort_keys=True, indent=2) + '\n')
print(json.dumps(summary, ensure_ascii=False, sort_keys=True, indent=2))
