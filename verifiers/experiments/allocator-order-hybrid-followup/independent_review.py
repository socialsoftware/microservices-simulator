"""Root review of receipts, raw reward counts, progress and common local GA paths."""
import collections
import gzip
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-followup-2026-09-21'
OLD = ROOT / 'verifiers/target/allocator-order-hybrid-2026-09-21'
NEW = ROOT / 'verifiers/target/allocator-order-hybrid-followup-2026-09-21'


def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def read(p): return json.loads(p.read_text())
def digest(value): return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(',', ':')).encode()).hexdigest()


def raw_score(observation, weights):
    """Independent literal count/coverage calculation, no production assess call."""
    if observation.get('status') not in ('COMPLETE', 'PARTIAL', 'UNAVAILABLE') \
            or observation.get('terminalStatus') not in ('SUCCESS', 'COMPENSATED', 'PARTIAL_COMPENSATED') \
            or observation.get('scheduleConformance') not in ('EXACT', 'DEVIATED'):
        return None
    total = 0
    for criterion, weight in weights.items():
        if weight == 0:
            continue
        if criterion == 'COMPENSATED_READ_EXPOSURE':
            complete = (observation.get('AStatus') == 'COMPLETE'
                        and observation.get('ACoverage') == 'COMPLETE_WITHIN_SCOPE'
                        and observation.get('AGaps') == [])
            count = observation.get('A')
        elif criterion == 'LOST_COPIED_UPDATE':
            complete = (observation.get('lostCopiedUpdateValidity') == 'COMPLETE'
                        and observation.get('lostCopiedUpdateCoverage') == 'COMPLETE_WITHIN_SCOPE'
                        and observation.get('lostCopiedUpdateCoverageGaps') == [])
            count = observation.get('lostCopiedUpdateCount')
        else:
            rows = [c for c in observation.get('impactCategories', []) if c.get('category') == criterion]
            row = rows[0] if len(rows) == 1 else {}
            complete = row.get('coverageStatus') == 'COMPLETE' and row.get('unknownReasons') == []
            count = row.get('positiveObjectCount')
        if not complete or type(count) is not int or count < 0:
            return None
        total += weight * count
    return total


def trace_path(profile, collection, arm, seed, protocol):
    folder = OLD / collection / arm if profile == 'all-five' and arm in protocol['reusedAllFiveArms'] \
        else NEW / profile / collection / arm
    return folder / f'seed-{seed:02}.json.gz'


def main():
    p = read(OUT / 'protocol.json')
    manifest = read(ROOT / p['inputManifest'])
    for path, expected in manifest['files'].items():
        assert sha(Path(path)) == expected, path
    for path, expected in read(OUT / 'prior-artifact-hashes.json').items():
        assert sha(ROOT / path) == expected, ('prior file changed', path)
    refs = {w['workload']: read(Path(w['reference'])) for w in manifest['workloads']}
    runs = fresh = selections = prefixes = 0
    for profile, spec in p['profiles'].items():
        weights = spec['fitness']['weights']
        for collection, ids in p['collections'].items():
            for seed in p['seeds']:
                starts = []
                paths = collections.defaultdict(list)
                for arm in p['arms']:
                    path = trace_path(profile, collection, arm, seed, p)
                    receipt = read(path.with_suffix('.receipt.json'))
                    assert sha(path) == receipt['sha256']
                    row = json.load(gzip.open(path, 'rt'))
                    assert row['arm'] == arm and row['seed'] == seed
                    assert row['attempts'] == len(row['decisions']) == p['budget'] == receipt['attempts']
                    assert digest(row['decisions']) == row['decisionSha256'] == receipt['decisions']
                    assert row['start']['allProgressZero']
                    starts.append(row['start']['stateSha256'])
                    reused = profile == 'all-five' and arm in p['reusedAllFiveArms']
                    if not reused:
                        assert row['fitness'] == spec['fitness']
                        identity = read(path.parent / 'identity.json')
                        assert identity['profile'] == profile and identity['arm'] == arm
                        assert identity['collection'] == collection and identity['fitness'] == spec['fitness']
                        assert identity['protocol'] == sha(OUT / 'protocol.json')
                        assert identity['manifest'] == sha(OUT / 'manifest.json')
                        for source, expected in identity['sourceSha256'].items():
                            assert sha(ROOT / source) == expected, source
                    local = collections.defaultdict(list)
                    progress = {wid: {'allocated': 0, 'known': 0, 'unknown': 0, 'positives': 0,
                                      'scoreSum': 0., 'bestScore': None} for wid in ids}
                    positive = unknown = 0
                    total = 0.
                    for number, d in enumerate(row['decisions'], 1):
                        assert d['decision'] == number and d['workload'] in ids
                        wid, key = d['workload'], d['candidate']
                        candidate = refs[wid]['candidates'][key]
                        assert candidate['id'] == d['scenarioId']
                        expected = raw_score(refs[wid]['observations'][key], weights)
                        assert d['score'] == expected, (profile, wid, key, d['score'], expected)
                        assert d['preUpdateProgress'] == progress[wid]
                        pr = progress[wid]
                        pr['allocated'] += 1
                        if expected is None:
                            unknown += 1
                            pr['unknown'] += 1
                            assert not d['modelUpdated']
                        else:
                            positive += expected > 0
                            total += expected
                            pr['known'] += 1
                            pr['positives'] += expected > 0
                            pr['scoreSum'] += expected
                            pr['bestScore'] = expected if pr['bestScore'] is None else max(expected, pr['bestScore'])
                        assert (d['positives'], d['cumulativeScore'], d['unknowns']) == (positive, total, unknown)
                        if number in p['checkpoints']:
                            ck = next(c for c in row['checkpoints'] if c['attempt'] == number)
                            assert (ck['positives'], ck['cumulativeScore'], ck['unknowns']) == (positive, total, unknown)
                        local[wid].append((key, expected, d['operator']))
                    assert (row['positives'], row['cumulativeScore'], row['unknowns']) == (positive, total, unknown)
                    for wid, history in local.items():
                        assert len({h[0] for h in history}) == len(history)
                        paths[wid].append(history)
                    runs += 1
                    fresh += not reused
                    selections += row['attempts']
                assert len(set(starts)) == 1, (profile, collection, seed)
                for histories in paths.values():
                    longest = max(histories, key=len)
                    for history in histories:
                        assert history == longest[:len(history)]
                        prefixes += 1
    result = {'status': 'PASS', 'searchesVerified': runs, 'freshSearches': fresh,
              'reusedSearches': runs - fresh, 'recordedSelectionsIncludingReuse': selections,
              'newApplicationExecutions': 0, 'localGAPrefixesMatched': prefixes,
              'rawCriterionRewardsIndependentlyRecomputed': True,
              'everyPreUpdateProgressAndCheckpointVerified': True,
              'priorSourceAndEvidenceHashesUnchanged': True}
    (OUT / 'independent-verification.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result))

if __name__ == '__main__': main()
