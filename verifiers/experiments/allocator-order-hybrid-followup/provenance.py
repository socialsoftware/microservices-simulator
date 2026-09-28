"""Hash and receipt checks for read-only reuse of the preceding study."""
import hashlib
import json
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
PREVIOUS_EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'
PREVIOUS_RUNS = ROOT / 'verifiers/target/allocator-order-hybrid-2026-09-21'


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def _stable(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':'))


def frozen_collections(protocol):
    """Resolve the exact 81 identities through the hash-pinned prior protocol."""
    path = ROOT / protocol['priorStudy']['protocol']
    if sha(path) != protocol['priorStudy']['protocolSha256']:
        raise ValueError('Previous protocol changed')
    previous = json.loads(path.read_text())
    collections = previous['collections']
    if protocol.get('collections') != collections:
        raise ValueError('Embedded follow-up collections differ from the pinned prior protocol')
    if {name: len(values) for name, values in collections.items()} \
            != protocol['collectionCounts']:
        raise ValueError('Previous collection counts differ from follow-up declaration')
    return collections


def validate_prior_reuse(protocol):
    """Validate every reused identity, receipt and trace, then return one seal."""
    prior = protocol['priorStudy']
    protocol_path = ROOT / prior['protocol']
    if sha(protocol_path) != prior['protocolSha256']:
        raise ValueError('Previous protocol changed')
    previous_protocol = json.loads(protocol_path.read_text())
    for name, expected in prior['evidenceSha256'].items():
        if sha(PREVIOUS_EVIDENCE / name) != expected:
            raise ValueError('Previous evidence changed: ' + name)
    if previous_protocol['arms'] != protocol['reusedAllFiveArms'] \
            or previous_protocol['seeds'] != protocol['seeds'] \
            or previous_protocol['budget'] != protocol['budget'] \
            or previous_protocol['checkpoints'] != protocol['checkpoints']:
        raise ValueError('Previous run design differs from reuse declaration')
    if previous_protocol['collections'] != frozen_collections(protocol):
        raise ValueError('Previous workload collections differ from follow-up')

    rows = []
    for collection in previous_protocol['collections']:
        for arm in previous_protocol['arms']:
            folder = PREVIOUS_RUNS / collection / arm
            identity_path = folder / 'identity.json'
            identity = json.loads(identity_path.read_text())
            if identity.get('protocol') != prior['protocolSha256'] \
                    or identity.get('manifest') != protocol['inputManifestSha256'] \
                    or identity.get('collection') != collection or identity.get('arm') != arm:
                raise ValueError('Previous run identity differs: ' + str(folder))
            rows.append({'kind': 'identity', 'collection': collection, 'arm': arm,
                         'path': str(identity_path.relative_to(ROOT)),
                         'sha256': sha(identity_path)})
            for seed in previous_protocol['seeds']:
                trace = folder / f'seed-{seed:02}.json.gz'
                receipt_path = trace.with_suffix('.receipt.json')
                receipt = json.loads(receipt_path.read_text())
                if sha(trace) != receipt.get('sha256') \
                        or receipt.get('attempts') != previous_protocol['budget']:
                    raise ValueError('Previous trace receipt differs: ' + str(trace))
                rows.append({'kind': 'trace', 'collection': collection, 'arm': arm,
                             'seed': seed, 'path': str(trace.relative_to(ROOT)),
                             'sha256': sha(trace),
                             'receipt': str(receipt_path.relative_to(ROOT)),
                             'receiptSha256': sha(receipt_path)})
    seal = hashlib.sha256(_stable(rows).encode()).hexdigest()
    if len(rows) != prior['reuseRows'] or seal != prior['reuseAggregateSha256']:
        raise ValueError('Previous trace set changed')
    return {'rows': len(rows), 'aggregateSha256': seal,
            'traceCount': sum(row['kind'] == 'trace' for row in rows),
            'identityCount': sum(row['kind'] == 'identity' for row in rows)}
