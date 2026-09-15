#!/usr/bin/env python3
"""Bounded source-derived scheduling qualification with explicit existing fixture inputs."""
import argparse, importlib.util, json, shutil, subprocess, sys
from pathlib import Path
HERE=Path(__file__).resolve().parent
spec=importlib.util.spec_from_file_location('integrated',HERE.parent/'qualify-integrated.py')
q=importlib.util.module_from_spec(spec);spec.loader.exec_module(q)
p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--package',type=Path);a=p.parse_args()
out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
overlay={str(p.relative_to(q.ROOT)):q.digest(p) for p in (q.ROOT/q.PREFIX).glob('SagaRead*.java')}
executor=q.ROOT/q.PREFIX/'ScenarioExecutor.java'
overlay[str(executor.relative_to(q.ROOT))]=q.digest(executor)
materializer=q.ROOT/q.PREFIX/'ScenarioMaterializer.java'
overlay[str(materializer.relative_to(q.ROOT))]=q.digest(materializer)
prod,build=q.verify(overlay)
shutil.copytree(HERE,out/'source',ignore=shutil.ignore_patterns('__pycache__'))
(out/'diagnostic').mkdir()
for source in (q.ROOT/q.PREFIX).glob('SagaRead*.java'):shutil.copy2(source,out/'diagnostic'/source.name)
shutil.copy2(executor,out/'diagnostic'/executor.name)
shutil.copy2(materializer,out/'diagnostic'/materializer.name)
shutil.copy2(HERE.parent.parent/'saga-read-exposure/OrdinaryExecutorControl.java',out/'source')
provenance=dict(image=build['image'],productionOverlay=overlay,preparedBuildHash=q.digest(q.BASE/'build-provenance.json'),sourceHashes={str(x.relative_to(out)):q.digest(x) for x in out.rglob('*') if x.is_file()})
q.save(out/'provenance.json',provenance)
def launch(script,label):
 cmd=['docker','run','--rm','--cpus','2','--memory','3g','--network','none','--entrypoint','bash','-v',str(q.ROOT/'verifiers/target')+':/reports:ro','-v',str(out)+':/out','-v',str(out/'source')+':/experiment:ro',build['image'],'/experiment/'+script]
 q.save(out/(label+'-command.json'),cmd)
 with (out/(label+'.log')).open('w') as f:r=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
 if r.returncode:raise SystemExit(r.returncode)
if a.package:
 shutil.copytree(a.package.resolve(),out/'package')
 manifest=json.loads((out/'package/scenario-catalog-manifest.json').read_text())
 for artifact in manifest['files'].values():
  assert q.digest(out/'package'/artifact['path'])==artifact['sha256']
 q.save(out/'package-reuse.json',dict(source=str(a.package.resolve()),sha256=q.digest(a.package/'scenario-catalog-manifest.json')))
else:launch('generate.sh','generation')
subprocess.run([sys.executable,str(out/'source/select_cases.py'),str(out)],check=True)
launch('execute.sh','execution')
q.verify(overlay)
for path,h in overlay.items():assert q.digest(q.ROOT/path)==h
for path,h in provenance['sourceHashes'].items():assert q.digest(out/path)==h
subprocess.run([sys.executable,str(out/'source/validate.py'),str(out)],check=True)
q.save(out/'artifact-hashes.json',{str(x.relative_to(out)):q.digest(x) for x in out.rglob('*') if x.is_file() and x.name!='artifact-hashes.json'})
print(out)
