"""Summarize the frozen input-cap run; generate a sensitivity figure."""
from pathlib import Path
import hashlib
import json
import shutil
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

root = Path(__file__).resolve().parents[4]
out = Path(__file__).resolve().parent
raw = root/'verifiers/target/generation-comparison-cap10-2026-09-18'
old = root/'verifiers/target/generation-comparison-2026-09-18-run1'
assert json.loads((raw/'run-status.json').read_text())['stage'] == 'COMPLETE'
data = json.loads((raw/'summary.json').read_text())
proof = {'baselineAgreement': {}, 'inputPopulationNested': True, 'rawArtifacts': {}}
for cap in [1,3]:
    for name in [f'inputs-cap-{cap}.json', f'rows-cap-{cap}.jsonl']:
        a,b = ((p/name).read_bytes() for p in [old,raw])
        assert a == b
        proof['baselineAgreement'][name] = hashlib.sha256(b).hexdigest()
for lo,hi in [(1,3),(3,10)]:
    a,b = [json.loads((raw/f'inputs-cap-{c}.json').read_text())['idsBySaga'] for c in [lo,hi]]
    assert a.keys() == b.keys()
    assert all(set(a[k]) <= set(b[k]) for k in a)
for name in ['summary.json','run-status.json','inputs-cap-1.json','inputs-cap-3.json','inputs-cap-10.json','accounting-cross-check.json','verification.jsonl','verification-selection.json','manifest.json','initial-reporting-failure.json']:
    shutil.copy2(raw/name,out/name)
for p in sorted(raw.glob('rows-cap-*.jsonl')):
    proof['rawArtifacts'][p.name] = hashlib.sha256(p.read_bytes()).hexdigest()
(out/'comparison-proof.json').write_text(json.dumps(proof,indent=2)+'\n')
lines=['# Quizzes input-cap sensitivity','',
'All combinations of 2–4 distinct Saga types among the same 37 types. Input caps 1, 3 and 10 use nested, deterministic source-derived populations: 37, 89 and 220 variants. Of 817 policy/source-mode eligible variants, 597 remain excluded by cap 10. Acceptance does not prove runtime setup readiness.','',
'## Results','',
'| Input cap | Sagas | Input combinations before pruning | After pruning | Input combinations removed | Forward orders retained after pruning + compression |',
'| ---: | ---: | ---: | ---: | ---: | ---: |']
for r in data['rows']:
    a,b,f,c = [int(r[x]) for x in ['allTuples','broadTuples','allFullOrders','broadCompressed']]
    lines.append(f"| {r['inputCap']} | {r['sagaCount']} | {a:,} | {b:,} | {100*(1-b/a):.2f}% | {100*c/f:.2f}% |")
lines += ['',
'At cap 10, pruning plus compression retains 8.74%, 2.13% and 2.31% of the respective brute-force forward-order spaces for two, three and four Sagas. For four Sagas, the retained fraction rises from 1.21% at cap 3 to 2.31% at cap 10. Reduction remains large, but its magnitude depends on the admitted inputs. The absolute compressed four-Saga space grows from 13,162,997,272,310 to 252,788,942,287,136 forward orders. These are mathematical counts, not materialized or executed scenarios.','',
'## Validation and provenance','',
'- Caps 1 and 3 reproduce the earlier input files and every Saga-set count row byte-for-byte.',
'- 26 complete-enumeration cases at cap 10 passed count, uniqueness, per-Saga order and whole conflict-anchor-order preservation checks; all 3,996 pair accounting comparisons agree with the production calculator.',
'- 223,443 Saga-set rows cover all 74,481 sets at each of three caps. No sampling of Saga sets at these sizes.',
'- Current application, simulator and verifier source hashes stayed unchanged during the run. Only experiment cap configuration/reporting was generalized; no production behavior changed.',
'- Raw data and logs: `verifiers/target/generation-comparison-cap10-2026-09-18/`. Source/build fingerprints, input IDs, summary, checks and raw row hashes are retained here.',
'- Overall local turnaround was about 82 seconds, including recovering plotting with an existing Python environment after the bundled Python lacked matplotlib. This is an operational duration, not an isolated performance benchmark. Counts were not rerun.',
'- Static forward orders only: faults, recovery and event expansion are not counted. Structural checks do not replace runtime outcome-preservation experiments.',
'- Full eligible-input counting and broad runtime executability coverage remain unmeasured.','',
'Reproduce with the generation-comparison runner using `--input-caps 1 3 10` and a Python environment containing matplotlib; use a fresh output directory. `summarize.py` reconstructs this table and figure from the named frozen run.','']
(out/'RESULTS.md').write_text('\n'.join(lines))
plt.rcParams.update({'font.size':10,'axes.spines.top':False,'axes.spines.right':False,'pdf.fonttype':42})
fig,axes=plt.subplots(1,2,figsize=(10,4.2),layout='constrained')
for k,color in [(2,'#286ba4'),(3,'#17856c'),(4,'#bd7024')]:
    rows=[r for r in data['rows'] if r['sagaCount']==k]
    for ax,num,den in [(axes[0],'broadTuples','allTuples'),(axes[1],'broadCompressed','allFullOrders')]:
        ys=[100*int(r[num])/int(r[den]) for r in rows]
        ax.plot([r['inputCap'] for r in rows],ys,'o-',label=f'{k} Sagas',color=color)
        ax.set_xticks([1,3,10]);ax.set_xlabel('Maximum input variants per Saga');ax.grid(alpha=.15)
axes[0].set_title('Input combinations retained by pruning');axes[0].set_ylabel('% of all input combinations');axes[0].set_ylim(bottom=0)
axes[1].set_title('Forward orders retained after both reductions');axes[1].set_ylabel('% of all forward orders');axes[1].set_ylim(bottom=0)
axes[0].legend(frameon=False);fig.suptitle('Quizzes: sensitivity to the input limit')
for ext in ['png','pdf']:fig.savefig(out/f'input-cap-sensitivity.{ext}',dpi=180)
plt.close(fig)
