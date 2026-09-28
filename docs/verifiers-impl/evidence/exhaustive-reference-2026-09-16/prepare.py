import concurrent.futures
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time

REPO = Path(__file__).resolve().parents[4]
OUT = REPO / 'verifiers/target/exhaustive-qualification-2026-09-16'
OUT.mkdir(parents=True, exist_ok=False)
TARGET = REPO / 'verifiers/target'
BASE = TARGET / 'ga-500x3-2026-09-15/ga-seed-11/config.json'
config = json.loads(BASE.read_text())
runtime = config['runtime']
source = OUT / 'source'; source.mkdir(exist_ok=True)
classes = OUT / 'classes'; classes.mkdir(exist_ok=True)
sources = [REPO / 'verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor' / f'{n}.java'
           for n in ['ImpactV2Assessor', 'ImpactV2EvidenceReport']]
sources.append(REPO / 'verifiers/experiments/workload-cohort-exploration/GenerateSelectedInputPackage.java')
for path in sources: shutil.copy2(path, source / path.name)
cmd = ['docker', 'run', '--rm', '--network', 'none', '-v', f'{TARGET}:/reports:ro',
       '-v', f'{OUT}:/out', runtime['image'], 'javac', '-parameters', '-cp', runtime['classpath'],
       '-d', '/out/classes', *['/out/source/' + p.name for p in sources]]
(OUT/'compile-command.json').write_text(json.dumps(cmd, indent=2))
with (OUT/'compile.log').open('w') as log: subprocess.run(cmd, stdout=log, stderr=log, check=True)
runtime['classpath'] = '/reports/' + str(classes.relative_to(TARGET)) + ':' + runtime['classpath']
for directory in [classes, source]:
    for p in directory.rglob('*'):
        if p.is_file(): runtime['hashes'][str(p)] = hashlib.sha256(p.read_bytes()).hexdigest()
(OUT/'runtime.json').write_text(json.dumps(runtime, indent=2))
inventory = json.loads((REPO/'docs/verifiers-impl/evidence/more-workloads-2026-09-15/inventory.json').read_text())['selected']
ids = dict(zip(inventory['participants'], inventory['inputIds']))
choices = {
    'add-update-leave': ['AddParticipantFunctionalitySagas', 'UpdateTournamentFunctionalitySagas', 'LeaveTournamentFunctionalitySagas'],
    'add-update-remove': ['AddParticipantFunctionalitySagas', 'UpdateTournamentFunctionalitySagas', 'RemoveTournamentFunctionalitySagas'],
}
(OUT/'selection.json').write_text(json.dumps({'candidates': choices, 'policy': 'exclusive-observed-fields-v2',
    'criteria': 'Prefer an exact 100-1000 candidate domain, source setup, valid SUCCESS/EXACT no-fault control with all five criteria complete; qualify structural fault boundaries before choosing. Preserve rejected candidates and unknowns. No GA outcomes or parameter tuning used for selection.',
    'generationCaps': {'workloads': 30000, 'schedules': 30000, 'recoveryPerVector': 500},
    'maxConcurrentContainers': 2}, indent=2))

def generate(label, names):
    out = OUT / label; out.mkdir()
    constraints = '+'.join(a+'<'+b for a,b in zip(names,names[1:]))
    args = ['docker','run','--rm','--network','none','-v',f'{TARGET}:/reports:ro',
            '-v',f'{REPO / "applications"}:/applications:ro','-v',f'{out}:/out',runtime['image'],
            'java','-Xmx1536m','-cp',runtime['classpath'],'GenerateSelectedInputPackage',
            '/applications','quizzes','/out/package',','.join(ids[n] for n in names),
            '30000','1','30000','1','16091','false','1',constraints]
    (out/'generation-command.json').write_text(json.dumps(args,indent=2))
    start=time.monotonic()
    with (out/'generation.log').open('w') as log: subprocess.run(args,stdout=log,stderr=log,check=True)
    proof=json.loads((out/'package/generation-proof.json').read_text())
    workloads=[json.loads(line) for line in (out/'package/workloads.jsonl').read_text().splitlines() if line.strip()]
    if len(workloads)!=1:raise ValueError('Expected a single forward workload')
    selected=dict(config,manifest=str(out/'package/scenario-catalog-manifest.json'),workload=workloads[0]['id'],
                  runtime=runtime,strategy='random',budget=12,seed=16091)
    (out/'config.json').write_text(json.dumps(selected,indent=2))
    print(label, workloads[0]['id'],round(time.monotonic()-start,1),flush=True)

with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
    list(pool.map(lambda item:generate(*item),choices.items()))
