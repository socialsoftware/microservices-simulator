"""Frozen, resumable runner for the allocator order/hybrid follow-up."""
import argparse
import gzip
import json
import shutil
import sys
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-followup-2026-09-21'
sys.path[:0] = [str(HERE), str(HERE.parent / 'fixed-workload-ga'),
                str(HERE.parent / 'allocator-transfer')]

from fitness import configuration
from inputs import load_inputs
from integration import policy, prepare
from provenance import frozen_collections, sha, validate_prior_reuse
from study import run_arm


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + '\n')


def load_frozen_design():
    protocol_path = EVIDENCE / 'protocol.json'
    manifest_path = EVIDENCE / 'manifest.json'
    protocol = json.loads(protocol_path.read_text())
    manifest = json.loads(manifest_path.read_text())
    if sha(protocol_path) != manifest['protocolSha256']:
        raise ValueError('Follow-up protocol differs from frozen manifest')
    for relative, expected in manifest['sourceSha256'].items():
        if sha(ROOT / relative) != expected:
            raise ValueError('Frozen source changed: ' + relative)
    for relative, expected in manifest['inputSha256'].items():
        if sha(ROOT / relative) != expected:
            raise ValueError('Frozen input/evidence changed: ' + relative)
    return protocol, manifest


def combinations(protocol, profiles, arms):
    requested_profiles = profiles or list(protocol['profiles'])
    if not set(requested_profiles) <= set(protocol['profiles']):
        raise ValueError('Profile outside frozen protocol')
    requested_arms = None if arms is None else list(arms)
    if requested_arms is not None and not set(requested_arms) <= set(protocol['arms']):
        raise ValueError('Arm outside frozen protocol')
    result = []
    for profile in requested_profiles:
        selected = requested_arms or protocol['freshRuns'][profile]
        if not set(selected) <= set(protocol['freshRuns'][profile]):
            forbidden = sorted(set(selected) - set(protocol['freshRuns'][profile]))
            raise ValueError(f'{profile} arms are reused or out of scope, not rerunnable: {forbidden}')
        result.extend((profile, arm) for arm in selected)
    return result


def main(argv=None):
    parser = argparse.ArgumentParser()
    parser.add_argument('--profiles', nargs='+')
    parser.add_argument('--arms', nargs='+')
    parser.add_argument('--seeds', nargs='+', type=int)
    parser.add_argument('--output', type=Path,
                        default=ROOT / 'verifiers/target/allocator-order-hybrid-followup-2026-09-21')
    args = parser.parse_args(argv)

    protocol, manifest = load_frozen_design()
    collections = frozen_collections(protocol)
    seeds = args.seeds or protocol['seeds']
    if not set(seeds) <= set(protocol['seeds']):
        raise ValueError('Seed outside frozen protocol')
    work = combinations(protocol, args.profiles, args.arms)
    prior_proof = validate_prior_reuse(protocol)

    input_manifest = ROOT / protocol['inputManifest']
    if sha(input_manifest) != protocol['inputManifestSha256']:
        raise ValueError('Changed complete-map input manifest')
    input_spec = json.loads(input_manifest.read_text())
    workloads, evaluator_factory, input_proof = load_inputs(input_manifest)
    by_id = {row['id']: row for row in workloads}
    if set(by_id) != {identity for rows in collections.values() for identity in rows}:
        raise ValueError('Loaded workloads differ from frozen collections')
    order_proof = None
    if any(arm in ('order', 'hybrid-order') for _, arm in work):
        order_proof = prepare(workloads, input_spec)

    args.output.mkdir(parents=True, exist_ok=True)
    write(args.output / 'input-validation.json', {
        'completeMaps': input_proof,
        'priorAllFiveReuse': prior_proof,
        'orderInputs': order_proof,
    })
    protocol_hash = sha(EVIDENCE / 'protocol.json')
    manifest_hash = sha(EVIDENCE / 'manifest.json')

    for profile_name, arm in work:
        fitness = configuration(protocol['profiles'][profile_name]['fitness'])
        for collection, identities in collections.items():
            rows = [by_id[workload_id] for workload_id in identities]
            folder = args.output / profile_name / collection / arm
            identity = {
                'protocol': protocol_hash,
                'manifest': manifest_hash,
                'inputManifest': protocol['inputManifestSha256'],
                'sourceSha256': manifest['sourceSha256'],
                'orderInputs': protocol['priorStudy']['evidenceSha256']['order-inputs.json'],
                'profile': profile_name,
                'fitness': fitness,
                'collection': collection,
                'arm': arm,
            }
            identity_path = folder / 'identity.json'
            if identity_path.exists() and json.loads(identity_path.read_text()) != identity:
                raise ValueError('Run identity differs: ' + str(folder))
            write(identity_path, identity)

            for seed in seeds:
                destination = folder / f'seed-{seed:02}.json.gz'
                receipt_path = destination.with_suffix('.receipt.json')
                if destination.exists():
                    receipt = json.loads(receipt_path.read_text())
                    if sha(destination) != receipt.get('sha256'):
                        raise ValueError('Changed completed output: ' + str(destination))
                    continue
                if shutil.disk_usage(args.output).free < 2 * 1024 ** 3:
                    raise ValueError('Disk headroom below 2 GiB')
                result = run_arm(
                    rows, evaluator_factory, arm, seed, protocol['budget'],
                    protocol['checkpoints'], fitness, policy)
                if not result['start']['allProgressZero'] \
                        or result['attempts'] != protocol['budget']:
                    raise ValueError('Unexpected start or incomplete budget')
                temporary = destination.with_suffix('.tmp')
                with temporary.open('wb') as stream:
                    with gzip.GzipFile(fileobj=stream, mode='wb', mtime=0) as compressed:
                        compressed.write(json.dumps(result, separators=(',', ':')).encode())
                temporary.rename(destination)
                write(receipt_path, {'sha256': sha(destination),
                                     'attempts': result['attempts'],
                                     'decisions': result['decisionSha256']})
                write(args.output / 'status.json', {
                    'stage': 'RUNNING', 'profile': profile_name,
                    'collection': collection, 'arm': arm, 'seed': seed})
            print(profile_name, collection, arm, 'complete', flush=True)

    # A long run is only valid if the frozen executable inputs remained stable.
    load_frozen_design()
    validate_prior_reuse(protocol)
    write(args.output / 'status.json', {
        'stage': 'REQUESTED_RUNS_COMPLETE',
        'profilesAndArms': work,
        'seeds': seeds,
    })


if __name__ == '__main__':
    main()
