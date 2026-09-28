"""Frozen, resumable local comparisons with bounded compact artifacts."""
import argparse
import gzip
import hashlib
import json
import shutil
import sys
from pathlib import Path
from study import run_arm, baseline_policy
from inputs import load_inputs, sha

ROOT = Path(__file__).resolve().parents[3]
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'


def policy_factory(arm, states, seed):
    if arm not in ('order', 'hybrid'):
        return baseline_policy(arm, states, seed)
    from integration import experimental_policy
    return experimental_policy(arm, states, seed)


def write(path, value):
    path.write_text(json.dumps(value, indent=2) + '\n')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--arms', nargs='+')
    ap.add_argument('--seeds', nargs='+', type=int)
    ap.add_argument('--output', type=Path, default=ROOT / 'verifiers/target/allocator-order-hybrid-2026-09-21')
    args = ap.parse_args()
    protocol = json.loads((EVIDENCE / 'protocol.json').read_text())
    arms = args.arms or protocol['arms']
    seeds = args.seeds or protocol['seeds']
    if not set(arms) <= set(protocol['arms']) or not set(seeds) <= set(protocol['seeds']):
        raise ValueError('Arms/seeds outside protocol')
    manifest = ROOT / protocol['inputManifest']
    if sha(manifest) != protocol['inputManifestSha256']:
        raise ValueError('Changed input manifest')
    workloads, factory, proof = load_inputs(manifest)
    by_id = {w['id']: w for w in workloads}
    if any(a in ('order','hybrid') for a in arms):
        from integration import prepare
        prepare(workloads)
    args.output.mkdir(parents=True, exist_ok=True)
    write(args.output / 'input-validation.json', proof)
    sources = [Path(__file__), Path(__file__).with_name('study.py')]
    sources += list((ROOT / 'verifiers/experiments/fixed-workload-ga').glob('*.py'))
    sources += list((ROOT / 'verifiers/experiments/allocator-transfer').glob('*.py'))
    if any(a in ('order','hybrid') for a in arms):
        sources += [Path(__file__).with_name('integration.py')]
    source_hashes = {str(p.relative_to(ROOT)): sha(p) for p in sources if not p.name.startswith('test_')}
    for collection, ids in protocol['collections'].items():
        rows = [by_id[wid] for wid in ids]
        for arm in arms:
            # Identity freezes only dependencies used by this arm; new modules do not
            # invalidate unchanged baselines when subsequently introduced.
            excluded = ('order_features.py','hybrid_linucb.py','integration.py')
            dependencies = {p:h for p,h in source_hashes.items()
                            if arm in ('order','hybrid') or not p.endswith(excluded)}
            identity = {'protocol': sha(EVIDENCE / 'protocol.json'),
                        'manifest': sha(manifest), 'sources': dependencies,
                        'collection': collection, 'arm': arm}
            folder = args.output / collection / arm
            folder.mkdir(parents=True, exist_ok=True)
            ident_path = folder / 'identity.json'
            if ident_path.exists() and json.loads(ident_path.read_text()) != identity:
                raise ValueError('Run identity differs: '+str(folder))
            write(ident_path, identity)
            for seed in seeds:
                dest = folder / f'seed-{seed:02}.json.gz'
                receipt = dest.with_suffix('.receipt.json')
                if dest.exists():
                    rec = json.loads(receipt.read_text())
                    if sha(dest) != rec['sha256']:
                        raise ValueError('Changed completed output')
                    continue
                if shutil.disk_usage(args.output).free < 2*1024**3:
                    raise ValueError('Disk headroom below 2 GiB')
                result = run_arm(rows, factory, arm, seed, protocol['budget'], protocol['checkpoints'], policy_factory)
                if not result['start']['allProgressZero'] or result['attempts'] != protocol['budget']:
                    raise ValueError('Unexpected start or incomplete budget')
                tmp = dest.with_suffix('.tmp')
                with tmp.open('wb') as stream:
                    with gzip.GzipFile(fileobj=stream,mode='wb',mtime=0) as zipped:
                        zipped.write(json.dumps(result,separators=(',',':')).encode())
                tmp.rename(dest)
                write(receipt, {'sha256':sha(dest),'attempts':result['attempts'],
                    'decisions':result['decisionSha256']})
                write(args.output / 'status.json', {'stage':'RUNNING','collection':collection,'arm':arm,'seed':seed})
            print(collection, arm, 'complete', flush=True)
    for p,h in source_hashes.items():
        if sha(ROOT/p) != h:
            raise ValueError('Source changed during run: '+p)
    write(args.output / 'status.json', {'stage':'REQUESTED_ARMS_COMPLETE','arms':arms,'seeds':seeds})


if __name__ == '__main__':
    main()
