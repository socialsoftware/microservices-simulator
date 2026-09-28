"""Complete paired summaries for both frozen reward profiles and all checkpoints."""
import gzip
import hashlib
import json
import random
import statistics as statistics
import sys
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-followup-2026-09-21'
NEW_RUNS = ROOT / 'verifiers/target/allocator-order-hybrid-followup-2026-09-21'
OLD_RUNS = ROOT / 'verifiers/target/allocator-order-hybrid-2026-09-21'
sys.path[:0] = [str(HERE), str(HERE.parent / 'fixed-workload-ga')]

from search import stable
from provenance import frozen_collections, sha, validate_prior_reuse
from run_study import load_frozen_design


def quantile(values, fraction):
    ordered = sorted(values)
    position = (len(ordered) - 1) * fraction
    lower = int(position)
    return ordered[lower] + (ordered[min(lower + 1, len(ordered) - 1)]
                             - ordered[lower]) * (position - lower)


def distribution(values):
    return {'mean': statistics.mean(values),
            'p10': quantile(values, 0.1),
            'p90': quantile(values, 0.9)}


def paired(left, right, identity):
    differences = [a - b for a, b in zip(left, right)]
    seed = int(hashlib.sha256(identity.encode()).hexdigest()[:16], 16)
    rng = random.Random(seed)
    samples = [statistics.mean(rng.choices(differences, k=len(differences)))
               for _ in range(10000)]
    return {'meanDifference': statistics.mean(differences),
            'pairedBootstrap95': [quantile(samples, 0.025), quantile(samples, 0.975)],
            'wins': sum(value > 0 for value in differences),
            'ties': sum(value == 0 for value in differences),
            'losses': sum(value < 0 for value in differences)}


def _checkpoint(row, attempt):
    matches = [value for value in row['checkpoints'] if value['attempt'] == attempt]
    if len(matches) != 1:
        raise ValueError('Trace lacks an exact frozen checkpoint')
    return matches[0]


def _trace_path(profile, collection, arm, seed, protocol):
    if profile == 'all-five' and arm in protocol['reusedAllFiveArms']:
        return OLD_RUNS / collection / arm / f'seed-{seed:02}.json.gz'
    return NEW_RUNS / profile / collection / arm / f'seed-{seed:02}.json.gz'


def validate_new_identity(profile, collection, arm, protocol, manifest):
    if profile == 'all-five' and arm in protocol['reusedAllFiveArms']:
        return
    path = NEW_RUNS / profile / collection / arm / 'identity.json'
    identity = json.loads(path.read_text())
    expected = {
        'protocol': sha(EVIDENCE / 'protocol.json'),
        'manifest': sha(EVIDENCE / 'manifest.json'),
        'inputManifest': protocol['inputManifestSha256'],
        'sourceSha256': manifest['sourceSha256'],
        'orderInputs': protocol['priorStudy']['evidenceSha256']['order-inputs.json'],
        'profile': profile,
        'fitness': protocol['profiles'][profile]['fitness'],
        'collection': collection,
        'arm': arm,
    }
    if identity != expected:
        raise ValueError('Fresh run identity mismatch: ' + str(path.parent))


def load_trace(profile, collection, arm, seed, protocol):
    path = _trace_path(profile, collection, arm, seed, protocol)
    receipt_path = path.with_suffix('.receipt.json')
    receipt = json.loads(receipt_path.read_text())
    if sha(path) != receipt.get('sha256') or receipt.get('attempts') != protocol['budget']:
        raise ValueError('Trace receipt mismatch: ' + str(path))
    with gzip.open(path, 'rt') as stream:
        row = json.load(stream)
    if row['seed'] != seed or row['arm'] != arm or row['attempts'] != protocol['budget'] \
            or not row['start']['allProgressZero']:
        raise ValueError('Trace identity/start mismatch: ' + str(path))
    if profile != 'all-five' or arm == 'hybrid-order':
        if row.get('fitness') != protocol['profiles'][profile]['fitness']:
            raise ValueError('Trace reward profile mismatch: ' + str(path))
    identities = [(item['workload'], item['candidate']) for item in row['decisions']]
    if len(set(identities)) != row['attempts']:
        raise ValueError('Trace repeats a candidate: ' + str(path))
    if sum(item['score'] is None for item in row['decisions']) != row['unknowns'] \
            or sum(item['score'] is not None and item['score'] > 0
                   for item in row['decisions']) != row['positives'] \
            or sum(item['score'] or 0 for item in row['decisions']) != row['cumulativeScore'] \
            or hashlib.sha256(stable(row['decisions']).encode()).hexdigest() \
            != row['decisionSha256']:
        raise ValueError('Trace totals/digest mismatch: ' + str(path))
    if [value['attempt'] for value in row['checkpoints']] != protocol['checkpoints']:
        raise ValueError('Trace checkpoints differ from protocol: ' + str(path))
    return row


def summarize():
    protocol, manifest = load_frozen_design()
    collections = frozen_collections(protocol)
    prior_proof = validate_prior_reuse(protocol)
    loaded = {}
    result = {
        'schema': 'allocator-order-hybrid-followup-summary.v1',
        'protocolSha256': sha(EVIDENCE / 'protocol.json'),
        'manifestSha256': sha(EVIDENCE / 'manifest.json'),
        'priorAllFiveReuse': prior_proof,
        'primaryEndpoint': protocol['primaryEndpoint'],
        'profiles': {},
    }
    for profile in protocol['profiles']:
        loaded[profile] = {}
        profile_result = {'collections': {}}
        for collection in collections:
            loaded[profile][collection] = {}
            arms = {}
            for arm in protocol['arms']:
                validate_new_identity(profile, collection, arm, protocol, manifest)
                records = [load_trace(profile, collection, arm, seed, protocol)
                           for seed in protocol['seeds']]
                loaded[profile][collection][arm] = records
                arms[arm] = {
                    'endpoint': {metric: distribution([row[metric] for row in records])
                                 for metric in ('positives', 'cumulativeScore', 'unknowns',
                                                'selectionAndUpdateSeconds')},
                    'checkpoints': [
                        {'attempt': checkpoint,
                         **{metric: distribution([
                             _checkpoint(row, checkpoint)[metric] for row in records])
                            for metric in ('positives', 'cumulativeScore', 'unknowns')}}
                        for checkpoint in protocol['checkpoints']],
                }

            contrasts = {}
            for left, right in protocol['contrasts']:
                checkpoints = []
                for checkpoint in protocol['checkpoints']:
                    metrics = {}
                    for metric in ('positives', 'cumulativeScore'):
                        left_values = [_checkpoint(row, checkpoint)[metric]
                                       for row in loaded[profile][collection][left]]
                        right_values = [_checkpoint(row, checkpoint)[metric]
                                        for row in loaded[profile][collection][right]]
                        identity = f'{profile}/{collection}/{left}/{right}/{checkpoint}/{metric}'
                        metrics[metric] = paired(left_values, right_values, identity)
                    checkpoints.append({'attempt': checkpoint, **metrics})
                contrasts[f'{left} minus {right}'] = {
                    'primaryAt256': checkpoints[-1],
                    'horizonSensitivity': checkpoints,
                }
            profile_result['collections'][collection] = {
                'arms': arms,
                'contrasts': contrasts,
            }
        result['profiles'][profile] = profile_result

    (EVIDENCE / 'summary.json').write_text(json.dumps(result, indent=2) + '\n')
    return result


if __name__ == '__main__':
    summary = summarize()
    for profile, profile_result in summary['profiles'].items():
        for collection, collection_result in profile_result['collections'].items():
            print(profile, collection)
            for arm, values in collection_result['arms'].items():
                endpoint = values['endpoint']
                print(arm, round(endpoint['positives']['mean'], 2),
                      round(endpoint['cumulativeScore']['mean'], 2))
