#!/usr/bin/env python3
"""Compile the current diagnostic over a hash-verified prepared runtime and qualify it."""
import argparse, hashlib, json, shutil, subprocess, time
from pathlib import Path
HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
BASE=ROOT/'verifiers/target/empty-event-delivery/run-01'
PACKAGE=Path('/tmp/saga-read-exposure-28go53x8/ordinary-control-package')
MEMORY=Path('/tmp/saga-read-exposure-28go53x8/memory-tool/jol-cli-0.17-full.jar')
PREFIX='verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/'
def digest(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def save(p,v):p.write_text(json.dumps(v,indent=2)+'\n')
def verify(overlay):
    production=json.loads((BASE/'production-hashes.json').read_text())
    build=json.loads((BASE/'build-provenance.json').read_text())
    for row in production:
        if row['path'] not in overlay and digest(ROOT/row['path'])!=row['sha256']:
            raise ValueError('Unexpected production drift: '+row['path'])
    for path,h in build['files'].items():
        if digest(BASE/path)!=h:raise ValueError('Prepared build drift: '+path)
    return production,build

def main():
    p=argparse.ArgumentParser();p.add_argument('--output',type=Path,required=True);p.add_argument('--control-package',type=Path,default=PACKAGE);p.add_argument('--jol',type=Path,default=MEMORY);a=p.parse_args()
    out=a.output.resolve();out.mkdir(parents=True,exist_ok=False)
    sources=sorted((ROOT/PREFIX).glob('SagaRead*.java'))
    overlay={str(p.relative_to(ROOT)):digest(p) for p in sources}
    prod,build=verify(overlay)
    frozen=out/'source';frozen.mkdir();(frozen/'diagnostic').mkdir();(frozen/'update').mkdir();(frozen/'creation').mkdir()
    for source in sources:shutil.copy2(source,frozen/'diagnostic'/source.name)
    for name in ('SagaUpdateReadExperiment.java','integrated.sh'):shutil.copy2(HERE/name,frozen/'update'/name)
    for name in ('SagaReadExposureExperiment.java','OrdinaryExecutorControl.java'):
        shutil.copy2(HERE.parent/'saga-read-exposure'/name,frozen/'creation'/name)
    shutil.copytree(a.control_package.resolve(),out/'ordinary-control-package')
    manifest=json.loads((out/'ordinary-control-package/scenario-catalog-manifest.json').read_text())
    for value in manifest['files'].values():
        target=out/'ordinary-control-package'/value['path']
        if digest(target)!=value['sha256']:raise ValueError('Package artifact mismatch')
    shutil.copy2(a.jol.resolve(),out/'jol.jar')
    if digest(out/'jol.jar')!='ea8cf31b7dc6c18810ca7aeadcbe7a2b352fb250261b8060ce513e8a99ebcd12':raise ValueError('JOL mismatch')
    provenance=dict(image=build['image'],revision=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
        preparedBuildManifestSha256=digest(BASE/'build-provenance.json'),productionOverlay=overlay,
        unchangedProductionFilesVerified=len(prod)-len(set(overlay)&{r['path'] for r in prod}),
        preparedArtifactsVerified=len(build['files']),runnerSha256=digest(Path(__file__)),
        frozenSourceHashes={str(p.relative_to(out)):digest(p) for p in frozen.rglob('*') if p.is_file()},
        baselinePackageHashes={str(p.relative_to(out)):digest(p) for p in (out/'ordinary-control-package').iterdir() if p.is_file()},
        productionScoringModified=False,transportModes=[False,True])
    save(out/'provenance.json',provenance)
    command=['docker','run','--rm','--cpus','2','--memory','3g','--network','none','--entrypoint','bash',
        '-v',str(ROOT/'verifiers/target')+':/reports:ro','-v',str(out)+':/out','-w','/out',build['image'],'/out/source/update/integrated.sh']
    save(out/'command.json',command);start=time.monotonic()
    with (out/'runner.log').open('w') as log:r=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
    save(out/'exit.json',dict(returnCode=r.returncode,wallSeconds=time.monotonic()-start))
    verify(overlay)
    for path,h in overlay.items():
        if digest(ROOT/path)!=h:raise ValueError('Measured diagnostic changed during execution')
    for path,h in provenance['frozenSourceHashes'].items():
        if digest(out/path)!=h:raise ValueError('Frozen source changed')
    save(out/'artifact-hashes.json',{str(p.relative_to(out)):digest(p) for p in out.rglob('*') if p.is_file()})
    raise SystemExit(r.returncode)
if __name__=='__main__':main()
