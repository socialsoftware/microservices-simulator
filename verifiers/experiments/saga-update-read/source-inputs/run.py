#!/usr/bin/env python3
"""Freeze explicit verifier changes; generate and execute the existing successful test pair."""
import argparse, importlib.util, json, shutil, subprocess, sys, time
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('qualification',HERE.parent/'qualify-integrated.py')
q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--overlay',type=Path,required=True);p.add_argument('--generation-only',action='store_true');a=p.parse_args()
out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
paths=json.loads(a.overlay.read_text())
assert paths and len(paths)==len(set(paths))
assert all(s.startswith(('verifiers/src/main/java/','simulator/src/main/java/')) and s.endswith('.java') for s in paths)
overlay={s:q.digest(q.ROOT/s) for s in paths};prod,build=q.verify(overlay)
shutil.copytree(HERE,out/'source',ignore=shutil.ignore_patterns('__pycache__'))
shutil.copy2(HERE.parent/'generated/select_cases.py',out/'source/select_cases.py')
shutil.copy2(HERE.parent.parent/'saga-read-exposure/OrdinaryExecutorControl.java',out/'source')
for relative in paths:
 target=out/'production'/relative;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(q.ROOT/relative,target)
provenance=dict(image=build['image'],productionOverlay=overlay,preparedBuildHash=q.digest(q.BASE/'build-provenance.json'),
    sharedRunnerHash=q.digest(HERE.parent/'qualify-integrated.py'),
    sourceHashes={str(x.relative_to(out)):q.digest(x) for x in out.rglob('*') if x.is_file()})
q.save(out/'provenance.json',provenance)
def launch(script,label):
 cmd=['docker','run','--rm','--cpus','2','--memory','3g','--network','none','--entrypoint','bash',
      '-v',str(q.ROOT/'verifiers/target')+':/reports:ro','-v',str(out)+':/out',
      '-v',str(out/'source')+':/experiment:ro',build['image'],'/experiment/'+script]
 q.save(out/(label+'-command.json'),cmd);start=time.monotonic()
 with (out/(label+'.log')).open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 q.save(out/(label+'-exit.json'),dict(returnCode=r.returncode,wallSeconds=time.monotonic()-start))
 if r.returncode:raise SystemExit(r.returncode)
launch('compile-generate.sh','generation')
if a.generation_only:
 q.verify(overlay)
 for path,h in overlay.items():assert q.digest(q.ROOT/path)==h
 for path,h in provenance['sourceHashes'].items():assert q.digest(out/path)==h
 q.save(out/'artifact-hashes.json',{str(x.relative_to(out)):q.digest(x) for x in out.rglob('*') if x.is_file() and x.name!='artifact-hashes.json'})
 print(out)
 raise SystemExit(0)
subprocess.run([sys.executable,str(out/'source/select_cases.py'),str(out)],check=True)
package_hashes={str(p.relative_to(out)):q.digest(p) for p in (out/'package').iterdir() if p.is_file()}
q.save(out/'package-hashes-before.json',package_hashes)
launch('execute.sh','execution')
q.verify(overlay)
for path,h in overlay.items():assert q.digest(q.ROOT/path)==h
for path,h in provenance['sourceHashes'].items():assert q.digest(out/path)==h
for path,h in package_hashes.items():assert q.digest(out/path)==h
subprocess.run([sys.executable,str(out/'source/validate.py'),str(out)],check=True)
q.save(out/'artifact-hashes.json',{str(x.relative_to(out)):q.digest(x) for x in out.rglob('*') if x.is_file() and x.name!='artifact-hashes.json'})
print(out)
