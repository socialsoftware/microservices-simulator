"""Verify and present the frozen all-eligible-input comparison."""
from pathlib import Path
import hashlib
import json
import shutil
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

root = Path(__file__).resolve().parents[4]
out = Path(__file__).resolve().parent
raw = root/'verifiers/target/generation-comparison-all-inputs-2026-09-18'
old = root/'verifiers/target/generation-comparison-cap10-2026-09-18'
status=json.loads((raw/'run-status.json').read_text())
assert status['stage'] == 'COMPLETE'
data = json.loads((raw/'summary.json').read_text())
populations={c:json.loads((raw/f'inputs-cap-{c}.json').read_text()) for c in [1,3,10,1000]}
full=populations[1000]
assert full['normalization']['inputVariantsCapped'] == 0
assert sum(map(len,full['idsBySaga'].values())) == full['normalization']['inputVariantsAccepted']
proof = {'baselineAgreement': {}, 'inputPopulationNested': True, 'allEligibleInputsRetained': True, 'rawArtifacts': {}}
for cap in [1,3,10]:
    for name in [f'inputs-cap-{cap}.json', f'rows-cap-{cap}.jsonl']:
        a,b = ((p/name).read_bytes() for p in [old,raw])
        if name.endswith('.jsonl'):
            assert a == b
            comparison = 'byte-identical rows'
        else:
            assert json.loads(a) == json.loads(b)
            comparison = 'equal JSON values (object key order can differ)'
        proof['baselineAgreement'][name] = {'sha256':hashlib.sha256(b).hexdigest(),'comparison':comparison}
for lo,hi in [(1,3),(3,10),(10,1000)]:
    a,b = [populations[c]['idsBySaga'] for c in [lo,hi]]
    assert a.keys() == b.keys()
    assert all(set(a[k]) <= set(b[k]) for k in a)
for name in ['summary.json','run-status.json',*[f'inputs-cap-{c}.json' for c in populations], 'accounting-cross-check.json','verification.jsonl','verification-selection.json','manifest.json']:
    shutil.copy2(raw/name,out/name)
for p in sorted(raw.glob('rows-cap-*.jsonl')):
    proof['rawArtifacts'][p.name] = hashlib.sha256(p.read_bytes()).hexdigest()
# Independent elementary-symmetric sum of per-Saga input population sizes.
proof['independentAllTupleCounts'] = {}
for cap,pop in populations.items():
    totals = [1,0,0,0,0]
    for inputs in pop['idsBySaga'].values():
        for k in range(4,0,-1):
            totals[k] += len(inputs)*totals[k-1]
    for r in data['rows']:
        if r['inputCap'] == cap:
            assert int(r['allTuples']) == totals[r['sagaCount']]
    proof['independentAllTupleCounts'][cap] = {k:str(totals[k]) for k in [2,3,4]}
(out/'comparison-proof.json').write_text(json.dumps(proof,indent=2)+'\n')
lines=['# Quizzes: all eligible source inputs','',
f"All combinations of 2–4 distinct Saga types among {len(full['idsBySaga'])} types, using all {full['normalization']['inputVariantsAccepted']} inputs accepted by the declared Saga source-mode and RESOLVED_OR_REPLAYABLE policy. The configured cap of 1,000 exceeds the extracted population: the normalizer records **zero inputs removed by the cap**. This is the complete eligible population in this source snapshot, not every possible application input or a guarantee of runtime preparation.",'',
'## Input-limit sensitivity','',
'| Input cap | Sagas | Input combinations before pruning | After pruning | Input combinations removed | Forward orders retained after pruning + compression |',
'| --- | ---: | ---: | ---: | ---: | ---: |']
for r in data['rows']:
    a,b,f,c = [int(r[x]) for x in ['allTuples','broadTuples','allFullOrders','broadCompressed']]
    label='All eligible' if r['inputCap']==1000 else str(r['inputCap'])
    lines.append(f"| {label} | {r['sagaCount']} | {a:,} | {b:,} | {100*(1-b/a):.2f}% | {100*c/f:.2f}% |")
lines+=['','## Complete-population counts','','| Sagas | All forward orders | After pruning | After pruning and compression |','| ---: | ---: | ---: | ---: |']
for r in data['rows']:
    if r['inputCap']==1000:
        lines.append('| '+' | '.join(str(r[k]) for k in ['sagaCount','allFullOrders','broadFullOrders','broadCompressed'])+' |')
elapsed=status['finishedAt']-json.loads((raw/'manifest.json').read_text())['startedAt']
checks=data['verificationCases']; comparisons=data['accountingComparisons']
lines+=['','## Evidence and scope','',
'- Limits 1, 3 and 10 reproduce the previous input identities/accounting as equal JSON values and every Saga-set count row byte-for-byte; input populations are nested. JSON object key order differs between JVM launches but values do not.',
f'- {checks} complete-enumeration checks at the full eligible population passed count, uniqueness, per-Saga order and whole conflict-anchor-order preservation; {comparisons} comparisons agree with the production accounting calculator.',
'- All 74,481 Saga sets are counted at each of four input limits. No sampling of Saga sets or cap on the computed number of forward orders.',
f'- The local run finished in {elapsed:.1f} seconds including compilation, extraction, counting, checks and reporting. This is elapsed operational turnaround, including any laptop suspension; not an isolated performance benchmark or CPU time.',
'- These are static forward orders. Faults, recovery, events and prerequisite expansion are not included; accepted recipes can still be unsupported by the executor.',
'- Structural preservation checks concern the extracted conflict model. Runtime preservation of findings and executability require application runs.',
'- No application, simulator or production verifier behavior was modified. Source hashes remained unchanged during the run.',
'- Raw rows and logs: `verifiers/target/generation-comparison-all-inputs-2026-09-18/`. This folder retains manifests, exact input identities, checks, summary and row hashes.',
'- Reproduce with the generation-comparison runner using `--input-caps 1 3 10 1000`, a current isolated build, matplotlib-enabled Python, and a fresh output directory. Always verify `inputVariantsCapped == 0` before calling the last population complete.','']
(out/'RESULTS.md').write_text('\n'.join(lines))
plt.rcParams.update({'font.size':10,'axes.spines.top':False,'axes.spines.right':False,'pdf.fonttype':42})
fig,axes=plt.subplots(1,2,figsize=(10,4.2),layout='constrained')
for k,color in [(2,'#286ba4'),(3,'#17856c'),(4,'#bd7024')]:
    rows=[r for r in data['rows'] if r['sagaCount']==k]
    for ax,num,den in [(axes[0],'broadTuples','allTuples'),(axes[1],'broadCompressed','allFullOrders')]:
        ys=[100*int(r[num])/int(r[den]) for r in rows]
        ax.plot(range(4),ys,'o-',label=f'{k} Sagas',color=color)
        ax.set_xticks(range(4),['1','3','10','All eligible']);ax.set_xlabel('Input variants admitted per Saga');ax.grid(alpha=.15)
axes[0].set_title('Input combinations retained by pruning');axes[0].set_ylabel('% of all input combinations');axes[0].set_ylim(bottom=0)
axes[1].set_title('Forward orders retained after both reductions');axes[1].set_ylabel('% of all forward orders');axes[1].set_ylim(bottom=0)
axes[0].legend(frameon=False);fig.suptitle('Quizzes: from bounded inputs to the full eligible population')
for ext in ['png','pdf']:fig.savefig(out/f'input-cap-sensitivity.{ext}',dpi=180)
plt.close(fig)
print(json.dumps({'elapsedSeconds':elapsed,'inputs':full['normalization'],'checks':checks,'comparisons':comparisons}))
