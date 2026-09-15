#!/usr/bin/env python3
"""Compile only the experiment against hash-verified current prepared application classes."""
import argparse, hashlib, json, subprocess, time
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE=ROOT/'verifiers/target/empty-event-delivery/run-01'
def digest(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def save(p,v): p.write_text(json.dumps(v,indent=2)+'\n')
def verify():
    prod=json.loads((BASE/'production-hashes.json').read_text())
    build=json.loads((BASE/'build-provenance.json').read_text())
    for r in prod:
        if digest(ROOT/r['path'])!=r['sha256']:raise ValueError('Production drift: '+r['path'])
    for path,h in build['files'].items():
        if digest(BASE/path)!=h:raise ValueError('Prepared build drift: '+path)
    return prod,build

def main():
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);a=p.parse_args()
    out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    prod,build=verify()
    image=build['image']; subprocess.run(['docker','image','inspect',image],check=True,stdout=subprocess.DEVNULL)
    tools={str(p.relative_to(ROOT)):digest(p) for p in HERE.iterdir() if p.suffix in ('.java','.py','.sh')}
    save(out/'provenance.json',dict(revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        image=image,productionHashes=prod,preparedBuildManifestSha256=digest(BASE/'build-provenance.json'),toolHashes=tools,
        fixtureNow='2030-01-01T14:55:00',freshJvmPerCase=True,cpus=2,memory='3g',scoreEvaluated=False))
    command=['docker','run','--rm','--cpus','2','--memory','3g','--network','none',
        '--entrypoint','bash','-v',str(ROOT/'verifiers/target')+':/reports:ro',
        '-v',str(HERE)+':/experiment:ro','-v',str(out)+':/out','-w','/out',image,'/experiment/run.sh']
    save(out/'command.json',command);start=time.monotonic()
    with (out/'runner.log').open('w') as log:
        r=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
    save(out/'exit.json',dict(returnCode=r.returncode,wallSeconds=time.monotonic()-start))
    verify()
    save(out/'artifact-hashes.json',{str(p.relative_to(out)):digest(p) for p in out.rglob('*') if p.is_file()})
    raise SystemExit(r.returncode)
if __name__=='__main__':main()
