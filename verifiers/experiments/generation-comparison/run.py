#!/usr/bin/env python3
"""Run a fingerprinted static comparison without touching campaign dependencies."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import time


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write(path, data):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(data, indent=2) + '\n')
    temporary.replace(path)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--build', type=Path, required=True)
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--plot-python', required=True)
    ap.add_argument('--input-caps', type=int, nargs='+', default=[1, 3])
    args = ap.parse_args()
    if any(cap < 1 for cap in args.input_caps):
        ap.error('--input-caps values must be positive')
    caps = sorted(set(args.input_caps))
    root = Path(__file__).resolve().parents[3]
    build = args.build.resolve()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=False)
    source = Path(__file__).with_name('GenerationComparison.java')
    proof = json.loads((build / 'build-proof.json').read_text())
    mismatches = [p for p, h in proof['sourceHashes'].items()
                  if not (root / p).is_file() or digest(root / p) != h]
    if mismatches:
        raise RuntimeError(f'Isolated verifier build is stale: {mismatches}')
    classes = out / 'classes'
    classes.mkdir()
    cp = (build / 'classpath.txt').read_text().strip()
    java_home = Path('/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home/bin')
    fingerprints = {}
    for subtree in ['applications/quizzes/src', 'simulator/src/main', 'verifiers/src/main',
                    'verifiers/experiments/generation-comparison']:
        for path in sorted((root / subtree).rglob('*')):
            if path.is_file() and '__pycache__' not in path.parts:
                fingerprints[str(path.relative_to(root))] = digest(path)
    manifest = {'sourceHashes': fingerprints, 'buildProof': proof,
                'javaVersion': subprocess.check_output([str(java_home/'java'), '-version'], stderr=subprocess.STDOUT, text=True),
                'dependencyHashes': {str(p): digest(p) for p in sorted((build/'deps').glob('*.jar'))},
                'gitHead': subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),
                'scope': 'Static forward schedule shapes; not runtime impact or performance',
                'inputCaps': caps, 'startedAt': time.time(), 'pid': os.getpid()}
    write(out / 'manifest.json', manifest)
    write(out / 'run-status.json', {'stage': 'COMPILING', 'pid': os.getpid()})
    try:
        with (out/'compile.log').open('w') as log:
            subprocess.run([str(java_home/'javac'), '--release','21','-cp',cp,'-d',str(classes),str(source)],
                           cwd=root,stdout=log,stderr=subprocess.STDOUT,check=True)
        command = ['/usr/bin/nice','-n','10',str(java_home/'java'),'-Xmx1536m','-XX:ActiveProcessorCount=2',
                   '-cp',str(classes)+os.pathsep+cp,'GenerationComparison',str(root/'applications'),'quizzes',str(out),','.join(map(str,caps))]
        manifest['command'] = command
        write(out/'manifest.json',manifest)
        write(out/'run-status.json',{'stage':'RUNNING','pid':os.getpid()})
        with (out/'generation.log').open('w') as log:
            subprocess.run(command,cwd=root,stdout=log,stderr=subprocess.STDOUT,check=True)
        changed = [p for p,h in fingerprints.items() if not (root/p).is_file() or digest(root/p)!=h]
        if changed:
            raise RuntimeError(f'Sources changed during run: {changed}')
        write(out/'run-status.json',{'stage':'REPORTING','pid':os.getpid()})
        with (out/'analysis.log').open('w') as log:
            subprocess.run([args.plot_python,str(Path(__file__).with_name('analyze.py')),str(out)],
                           cwd=root,stdout=log,stderr=subprocess.STDOUT,check=True)
        write(out/'run-status.json',{'stage':'COMPLETE','sourceHashesUnchanged':True,'finishedAt':time.time()})
    except BaseException as error:
        write(out/'run-status.json',{'stage':'FAILED','error':str(error),'at':time.time()})
        raise

if __name__ == '__main__':
    main()
