#!/usr/bin/env python3
"""Freeze and generate a new comparison without rebuilding campaign dependencies."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import time


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--build', required=True, type=Path)
    ap.add_argument('--output', required=True, type=Path)
    args = ap.parse_args()
    root = Path(__file__).resolve().parents[3]
    build, out = args.build.resolve(), args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    proof = json.loads((build / 'build-proof.json').read_text())
    mismatches = [p for p, h in proof['sourceHashes'].items() if digest(root / p) != h]
    if mismatches:
        raise ValueError(f'Stale isolated verifier build: {mismatches}')
    src = Path(__file__).with_name('PruningAudit.java')
    frozen = out / src.name
    frozen.write_bytes(src.read_bytes())
    (out / 'classes').mkdir()
    cp = (build / 'classpath.txt').read_text().strip()
    java = Path('/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin')
    hashes = {str(p.relative_to(root)): digest(p)
              for sub in ('applications/quizzes/src', 'simulator/src/main', 'verifiers/src/main')
              for p in (root / sub).rglob('*') if p.is_file()}
    manifest = {'at': time.time(), 'sourceHashes': hashes, 'generatorHash': digest(src),
        'buildProofHash': digest(build / 'build-proof.json'), 'classpath': cp,
        'dependencyHashes': {str(p): digest(p) for p in (build / 'deps').glob('*.jar')}}
    (out / 'generation-manifest.json').write_text(json.dumps(manifest, indent=2))
    with (out / 'compile.log').open('w') as log:
        subprocess.run([str(java / 'javac'), '--release', '21', '-cp', cp, '-d',
                        str(out / 'classes'), str(frozen)], stdout=log, stderr=subprocess.STDOUT, check=True)
    with (out / 'generation.log').open('w') as log:
        subprocess.run(['/usr/bin/nice', '-n', '10', str(java / 'java'), '-Xmx1536m',
            '-XX:ActiveProcessorCount=2', '-cp', str(out / 'classes') + ':' + cp,
            'PruningAudit', str(root / 'applications'), 'quizzes', str(out)],
            stdout=log, stderr=subprocess.STDOUT, check=True)
    assert all(digest(root / p) == h for p, h in hashes.items()), 'Source changed during generation'
    print(out)


if __name__ == '__main__':
    main()
