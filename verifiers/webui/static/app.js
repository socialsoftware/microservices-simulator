const $ = (id) => document.getElementById(id);
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
const fmt = (n) => n == null ? '—' : Number(n).toLocaleString('en', {maximumFractionDigits: 2});
const short = (s) => s && /^[a-f0-9]{40,}$/.test(s) ? `${s.slice(0, 10)}…` : s;
const colors = ['#e98966','#7774c6','#5c9e8a','#639bc1'];
const labels = {DELETED_DEPENDENCY:'Deleted dependencies',FAILED_OPERATION_RESIDUAL:'Residual effects',UNRESOLVED_DELIVERED_EVENT:'Unresolved events',COMPENSATED_READ_EXPOSURE:'Compensated reads',LOST_COPIED_UPDATE:'Lost copied updates'};
const state = {catalog:null, jobs:[], view:'experiments', selected:new Set(), detail:null, details:new Map(), page:0, search:'', mode:'all', metric:'positives', tab:'runs', attemptPage:0};
let toastTimer, polling = false, detailsSequence = 0, pendingIndexRefresh = false;

async function api(path, body) {
  const options = body === undefined ? {} : {method:'POST',headers:{'Content-Type':'application/json','X-Workbench-Token':state.catalog?.token || ''},body:JSON.stringify(body)};
  const response = await fetch(path, options);
  const value = await response.json();
  if (!response.ok) throw Error(value.error || 'Request failed');
  return value;
}
function toast(message) { $('toast').textContent = message; $('toast').classList.remove('hidden'); clearTimeout(toastTimer); toastTimer = setTimeout(()=>$('toast').classList.add('hidden'),7000); }
function fail(error) { toast(error.message); }
function empty(title, text) { return `<div class="empty"><h2>${esc(title)}</h2><p>${esc(text)}</p></div>`; }
function modeTag(mode) { return `<span class="tag ${esc(mode)}">${mode === 'recorded' ? 'Recorded feedback' : mode === 'live' ? 'Live execution' : 'Mode not recorded'}</span>`; }
function metric(label, value, note, icon, green=false) {return `<div class="metric"><div class="metric-top">${label}<span class="metric-icon">${icon}</span></div><div class="metric-value numeric">${fmt(value)}</div><div class="metric-note ${green?'green':''}">${esc(note)}</div></div>`;}

function chart(runs, metricName = 'positives', width=700, height=240) {
  const pad={left:46,right:16,top:16,bottom:34}, w=width-pad.left-pad.right, h=height-pad.top-pad.bottom;
  const maxX=Math.max(1,...runs.map(r=>r.attempts));
  const maxY=Math.max(1,...runs.flatMap(r=>r.trajectory.map(p=>p[metricName] || 0)));
  const x=n=>pad.left+n/maxX*w, y=n=>pad.top+h-n/maxY*h;
  let contents='';
  for(let i=0;i<=4;i++){const n=maxY*i/4, yy=y(n);contents+=`<line x1="${pad.left}" x2="${width-pad.right}" y1="${yy}" y2="${yy}" stroke="#edf0f4"/><text x="${pad.left-10}" y="${yy+3}" text-anchor="end">${fmt(n)}</text>`;}
  for(let i=0;i<=4;i++){const n=Math.round(maxX*i/4);contents+=`<text x="${x(n)}" y="${height-12}" text-anchor="middle">${fmt(n)}</text>`;}
  runs.forEach((run,i)=>{
    // Keep all decisions in the exported data; sample only the visible polyline.
    const step=Math.max(1,Math.floor(run.trajectory.length/1600));
    const points=run.trajectory.filter((p,j)=>j%step===0 || j===run.trajectory.length-1);
    const line=points.map(p=>`${x(p.attempt)},${y(p[metricName] || 0)}`).join(' ');
    if(runs.length===1) contents+=`<polygon points="${x(0)},${y(0)} ${line} ${x(run.attempts)},${y(0)}" fill="${colors[i]}" opacity=".08"/>`;
    contents+=`<polyline points="${line}" fill="none" stroke="${colors[i]}" stroke-width="2.3" stroke-linecap="round" stroke-linejoin="round"/>`;
  });
  const names={positives:'Positive scenarios discovered',score:'Cumulative configured score',unknowns:'Unavailable scores'};
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 ${width} ${height}" role="img" aria-label="${names[metricName]} over scenario selections"><title>${names[metricName]} over scenario selections</title><rect width="${width}" height="${height}" fill="white"/>${contents}</svg>`;
}

function graphPanel(runs, compare=false) {
  return `<div class="panel ${compare?'compare-chart':''}"><div class="panel-head"><div><h3>Discovery over budget</h3><p>${compare?'Individual runs · no averaging across seeds':'A trace of what this search found, one selection at a time'}</p></div><div class="panel-actions"><select class="mini-select" id="chart-metric" aria-label="Chart metric"><option value="positives" ${state.metric==='positives'?'selected':''}>Positive scenarios</option><option value="score" ${state.metric==='score'?'selected':''}>Configured score</option><option value="unknowns" ${state.metric==='unknowns'?'selected':''}>Unavailable</option></select><button id="export-svg" class="icon-button" title="Download SVG" aria-label="Download chart as SVG">↓</button></div></div><div class="panel-body"><div class="chart" id="main-chart">${chart(runs,state.metric,compare?1000:700,compare?340:240)}</div><div class="legend">${runs.map((r,i)=>`<span><i style="background:${colors[i]}"></i>${esc(r.method)} · seed ${fmt(r.seed)}</span>`).join('')}</div><p class="chart-note">Scenario selections → · Unavailable scores stay unknown and consume budget.</p></div></div>`;
}
function workloadPanel(run) {
  const rows=run.workloads.slice(0,5), max=Math.max(1,...rows.map(w=>w.attempts));
  return `<div class="panel"><div class="panel-head"><div><h3>Where the budget went</h3><p>${fmt(run.workloadCount)} workloads · allocation by attempts</p></div><span class="tag">TOP ${rows.length}</span></div><div class="panel-body">${rows.map(w=>`<div class="workload-row"><div class="bar-label"><span title="${esc(w.id)}">${esc(short(w.name))}</span><b>${fmt(w.attempts)}</b></div><div class="bar-track"><div class="bar-fill" style="width:${w.attempts/max*100}%"></div></div></div>`).join('') || '<p class="subtle-note">No workload decisions retained.</p>'}<p class="subtle-note">Open a scenario below to inspect its retained evidence.</p></div></div>`;
}

function filteredRuns() {return (state.catalog?.runs || []).filter(r=>(state.mode==='all'||r.mode===state.mode)&&`${r.name} ${r.collection} ${r.method} ${r.path} ${r.seed}`.toLowerCase().includes(state.search.toLowerCase()));}
function runsTable() {
  const rows=filteredRuns(), page=rows.slice(state.page*12,state.page*12+12);
  return `<div class="panel"><div class="panel-head"><div><h3>Experiment history <span class="tag">${fmt(rows.length)}</span></h3><p>Select a run to explore. Check up to four to compare.</p></div><button class="text-button" id="go-compare">Compare selected (${state.selected.size}) ↗</button></div><div class="toolbar"><input class="search" id="run-search" type="search" placeholder="Search experiments, methods, collections…" aria-label="Search experiments" value="${esc(state.search)}"><select id="mode-filter" aria-label="Filter execution mode"><option value="all">All execution modes</option><option value="recorded" ${state.mode==='recorded'?'selected':''}>Recorded feedback</option><option value="live" ${state.mode==='live'?'selected':''}>Live execution</option><option value="unknown" ${state.mode==='unknown'?'selected':''}>Mode not recorded</option></select></div><div class="table-wrap"><table><thead><tr><th></th><th>Experiment</th><th>Method</th><th>Mode</th><th>Attempts</th><th>Positives</th><th>Unavailable</th><th></th></tr></thead><tbody>${page.map(r=>`<tr class="${state.detail?.id===r.id?'selected':''}"><td><input type="checkbox" aria-label="Compare ${esc(r.name)} ${esc(r.method)} seed ${r.seed}" data-compare="${r.id}" ${state.selected.has(r.id)?'checked':''}></td><td><button class="run-name" data-run="${r.id}" title="${esc(r.path)}">${esc(r.name)}</button><span class="run-meta">${esc(r.collection)} · seed ${fmt(r.seed)}</span></td><td><span class="method">${esc(r.method)}</span></td><td>${modeTag(r.mode)}</td><td class="numeric">${fmt(r.attempts)} <span class="run-meta">of ${fmt(r.budget)}</span></td><td class="numeric positive-text">${fmt(r.positives)}</td><td class="numeric">${fmt(r.unknowns)}</td><td><button class="text-button" data-run="${r.id}" aria-label="Open ${esc(r.name)}">↗</button></td></tr>`).join('')}</tbody></table></div>${!page.length?empty('No matching experiments','Try a different filter, refresh the index, or launch from a prepared configuration.'):''}<div class="pagination"><span>${rows.length?state.page*12+1:0}–${Math.min((state.page+1)*12,rows.length)} of ${fmt(rows.length)} runs</span><div><button id="prev-page" ${!state.page?'disabled':''}>← Previous</button><button id="next-page" ${(state.page+1)*12>=rows.length?'disabled':''}>Next →</button></div></div></div>`;
}

function attemptsTable(run) {
  const rows=run.attemptsDetail.slice(state.attemptPage*20,state.attemptPage*20+20);
  return `<div class="panel"><div class="panel-head"><div><h3>Scenario evidence</h3><p>Click a scenario to see its fault vector, action order and available observations.</p></div><a class="text-button" href="/api/export?id=${run.id}&format=csv">Export CSV ↓</a></div><div class="table-wrap"><table><thead><tr><th>Attempt</th><th>Scenario</th><th>Workload</th><th>Score</th><th>Outcome</th><th>Schedule</th></tr></thead><tbody>${rows.map(r=>`<tr><td>${r.attempt}</td><td><button class="run-name" data-attempt="${r.attempt-1}">${esc(short(r.scenario)||'No retained ID')}</button></td><td>${esc(short(r.name))}</td><td><span class="tag ${r.score==null?'unknown':r.score>0?'positive':''}">${r.score==null?'Unavailable':fmt(r.score)}</span></td><td>${esc(r.status||'Not retained')}</td><td>${esc(r.conformance||'Not retained')}</td></tr>`).join('')}</tbody></table></div><div class="pagination"><span>${fmt(run.attempts)} scenario selections</span><div><button id="prev-attempt" ${!state.attemptPage?'disabled':''}>← Previous</button><button id="next-attempt" ${(state.attemptPage+1)*20>=run.attempts?'disabled':''}>Next →</button></div></div></div>`;
}
function categoriesPanel(run) {
  const entries=Object.entries(run.categories);
  return `<div class="panel"><div class="panel-head"><div><h3>Observed impact components</h3><p>Sum of retained per-criterion counts across attempts; categories can overlap.</p></div></div>${entries.length?`<table><thead><tr><th>Criterion</th><th>Observed count</th><th>Attempts with an available count</th></tr></thead><tbody>${entries.map(([k,v])=>`<tr><td>${esc(labels[k]||k)}</td><td>${fmt(v.count)}</td><td>${fmt(v.observations)} / ${fmt(run.attempts)}</td></tr>`).join('')}</tbody></table>`:empty('Component counts are not in this trace','This allocation trace retains decision scores. It does not contain per-criterion observations; these cannot be reconstructed from the total score.')}</div>`;
}

function experiments() {
  const r=state.detail;
  if (!r) return runsTable();
  return `<div class="run-summary"><div><h2 class="run-title">${esc(r.collection)} <span class="tag">${esc(r.name)}</span></h2><p>${esc(r.method)} · seed ${fmt(r.seed)} · ${esc(r.stop)} &nbsp; ${modeTag(r.mode)} &nbsp; <span class="tag">${esc(r.fitness?.label || r.fitness?.policy || 'Fitness not retained')}</span></p></div><div class="panel-actions"><button class="secondary" id="show-config">Configuration</button><a class="secondary" href="/api/export?id=${r.id}&format=json">Export data ↓</a></div></div><div class="metrics">${metric('Scenario selections',r.attempts,`Budget: ${fmt(r.budget)}`,'⌁')}${metric('Positive scenarios',r.positives,'Available configured score > 0','↗',true)}${metric('Configured score',r.score,'Cumulative across selected scenarios','∑')}${metric('Unavailable scores',r.unknowns,'Missing evidence is not a zero','◌')}</div><div class="chart-grid">${graphPanel([r])}${workloadPanel(r)}</div><div class="detail-tabs"><button data-tab="runs" class="${state.tab==='runs'?'active':''}">All experiments</button><button data-tab="attempts" class="${state.tab==='attempts'?'active':''}">Scenario evidence</button><button data-tab="impact" class="${state.tab==='impact'?'active':''}">Impact breakdown</button></div>${state.tab==='attempts'?attemptsTable(r):state.tab==='impact'?categoriesPanel(r):runsTable()}`;
}
function compare() {
  const runs=[...state.selected].map(id=>state.details.get(id)).filter(Boolean);
  if (!runs.length) return empty('Choose runs to compare','In Experiments, select up to four runs using the checkboxes. Their individual discovery curves will appear here.');
  const compatible = runs.every(r=>r.comparisonKey && r.comparisonKey===runs[0].comparisonKey);
  return `<div>${runs.map(r=>`<span class="compare-chip">${esc(r.method)} · seed ${fmt(r.seed)}<button data-remove="${r.id}" aria-label="Remove ${esc(r.method)}">×</button></span>`).join('')}</div>${!compatible?'<div class="notice">Comparison context differs or is incomplete. These traces may use different workloads, evidence, weights or execution modes. Curves are displayed separately; no ranking or aggregate conclusion is inferred.</div>':''}${graphPanel(runs,true)}<div class="panel"><div class="panel-head"><div><h3>Run context</h3><p>Check the scope and scoring policy before interpreting differences.</p></div></div><table><thead><tr><th>Method / seed</th><th>Mode</th><th>Fitness</th><th>Attempts</th><th>Positives</th><th>Score</th><th>Unavailable</th></tr></thead><tbody>${runs.map(r=>`<tr><td>${esc(r.method)} / ${fmt(r.seed)}<small class="run-meta">${esc(r.collection)}</small></td><td>${modeTag(r.mode)}</td><td>${esc(r.fitness?.policy||'Not retained')}<small class="run-meta" title="${esc(JSON.stringify(r.fitness?.weights))}">${esc(r.fitness?.weights?Object.entries(r.fitness.weights).map(([k,v])=>`${labels[k]||k}: ${v}`).join(' · '):'Weights unavailable')}</small></td><td>${fmt(r.attempts)}</td><td>${fmt(r.positives)}</td><td>${fmt(r.score)}</td><td>${fmt(r.unknowns)}</td></tr>`).join('')}</tbody></table></div>`;
}
function jobs() {
  if(!state.jobs.length) return empty('Your next experiment starts here','Launch from a prepared configuration. Execution runs in a separate process and keeps going when you close the browser.');
  return state.jobs.map(j=>`<article class="panel job-card"><div class="job-header"><div><h3>${esc(j.name)}</h3><p>${modeTag(j.mode)} &nbsp; Seed ${fmt(j.seed)} · budget ${fmt(j.budget)}</p></div><span class="tag ${esc(j.state)}">${esc(j.state)}</span></div>${j.progress?.completedDecisions!=null?`<div class="progress-bar"><div style="width:${Math.min(100,j.progress.completedDecisions/j.budget*100)}%"></div></div><p>${fmt(j.progress.completedDecisions)} decisions completed · ${fmt(j.progress.inFlight)} in flight</p>`:`<p>${j.state==='RUNNING'?'Validating inputs or running. Recorded-feedback results are published at completion.':`Started ${new Date(j.createdAt*1000).toLocaleString()}`}</p>`}${j.latestDecision?`<p>${fmt(j.latestDecision.positiveDiscoveries)} positives · cumulative score ${fmt(j.latestDecision.cumulativeScore)} · ${fmt(j.latestDecision.unknowns)} unavailable</p>`:''}${j.error?`<div class="error">${esc(j.error)}</div>`:''}<div class="job-actions"><button class="secondary" data-log="${j.id}">View log</button>${j.canPause?`<button class="secondary" data-action="pause" data-job="${j.id}">Pause after current attempt</button>`:''}${j.canResume?`<button class="primary" data-action="resume" data-job="${j.id}">Resume</button>`:''}${j.resultId?`<button class="primary" data-result="${j.resultId}">Explore results ↗</button>`:''}</div></article>`).join('');
}

function render() {
  document.querySelectorAll('[data-view]').forEach(b=>b.classList.toggle('active',b.dataset.view===state.view));
  const texts={experiments:['Experiments','Experiment workspace','From fault injection to evidence. All your experiments in one place.'],compare:['Compare runs','Put your experiments in perspective','Compare discovery curves while keeping scope, scoring and execution mode visible.'],jobs:['Live activity','Experiments in motion','Follow progress, inspect logs, and return when the evidence is ready.']};
  const [crumb,title,subtitle]=texts[state.view];$('breadcrumb').textContent=crumb;$('page-title').textContent=title;$('page-subtitle').textContent=subtitle;
  $('nav-count').textContent=fmt(state.catalog?.runs.length);$('compare-count').textContent=state.selected.size;$('job-count').textContent=state.jobs.filter(j=>j.running).length;
  $('content').innerHTML=state.view==='experiments'?experiments():state.view==='compare'?compare():jobs();
}
async function loadDetail(id) {
  const detail=await api(`/api/run?id=${encodeURIComponent(id)}`);state.details.set(id,detail);return detail;
}
async function openRun(id) {
  const sequence=++detailsSequence;
  const detail=await loadDetail(id);
  if(sequence!==detailsSequence)return;
  state.detail=detail;state.attemptPage=0;state.view='experiments';render();window.scrollTo({top:0,behavior:'smooth'});
}
function showEvidence(title,html) { $('detail-title').textContent=title;$('detail-content').innerHTML=html;$('detail-dialog').showModal(); }
function showAttempt(index) {
  const a=state.detail.attemptsDetail[index];
  showEvidence(`Scenario · attempt ${a.attempt}`,`<div class="evidence-grid"><div><small>SCENARIO</small>${esc(a.scenario||'Not retained')}</div><div><small>WORKLOAD</small>${esc(a.name)}</div><div><small>CONFIGURED SCORE</small>${a.score==null?'Unavailable':fmt(a.score)}</div><div><small>FAULT VECTOR</small>${esc(a.vector||'Not retained in this trace')}</div></div>${a.actions.length?`<h3>Persisted action order</h3><div class="action-chain">${a.actions.map(action=>`<span class="${action.compensate?'compensate':''}">${esc(Object.entries(action).map(([k,v])=>`${k} ${v}`).join(' · '))}</span>`).join('')}</div>`:''}<h3>Retained observation</h3><pre class="code-block">${esc(JSON.stringify(a.raw,null,2))}</pre>`);
}

function fillPreset() {
  const preset=state.catalog.configs.find(c=>c.id===$('preset').value);
  if(!preset){$('launch').disabled=true;$('preset-info').textContent='No supported allocation configurations were found. See verifiers/webui/README.md for supported inputs.';return;}
  $('launch').disabled=!preset.ready;$('policy').value=preset.policy;$('budget').value=preset.budget;$('seed').value=preset.seed;
  $('preset-info').textContent=preset.ready?`${preset.workloads.length} workloads · referenced input paths found. Runner qualification happens at launch.`:`${preset.missing.length} referenced paths are missing on this machine.`;
  $('workload-options').innerHTML=preset.workloads.map((w,i)=>`<label><input type="checkbox" name="workload" value="${i}" checked>${esc(w)}</label>`).join('');
  $('weight-options').innerHTML=state.catalog.criteria.map(k=>`<label>${esc(labels[k])}<input name="weight-${k}" aria-label="${esc(labels[k])} weight" type="number" min="0" step="any" value="${preset.fitness?.weights?.[k]??0}" required></label>`).join('');
  $('launch-review').textContent=preset.mode==='live'?'Live execution · launches real isolated ScenarioExecutor attempts through Docker. Existing controls and runtime hashes must qualify. The browser can be closed while it runs.':'Recorded feedback · searches existing measured maps. It reveals recorded outcomes as scenarios are selected; it does not execute the application.';
  $('launch-error').classList.add('hidden');
}
function formBody() {
  return {name:$('experiment-name').value,configId:$('preset').value,policy:$('policy').value,budget:Number($('budget').value),seed:Number($('seed').value),workloads:[...document.querySelectorAll('[name=workload]:checked')].map(e=>Number(e.value)),weights:Object.fromEntries(state.catalog.criteria.map(k=>[k,Number(document.querySelector(`[name="weight-${k}"]`).value)]))};
}
function launchError(e) { $('launch-error').textContent=e.message;$('launch-error').classList.remove('hidden'); }
function openLaunch() {
  if(!state.catalog) return;
  $('preset').innerHTML=state.catalog.configs.map(c=>`<option value="${c.id}">${c.ready?'':'[Missing inputs] '}${esc(c.name)}</option>`).join('');
  $('policy').innerHTML=state.catalog.policies.map(p=>`<option>${esc(p)}</option>`).join('');fillPreset();$('launch-dialog').showModal();
}
async function refresh() { await api('/api/refresh',{});await poll();toast('Artifact index is refreshing.'); }
async function poll() {
  if(polling)return;polling=true;
  try {
    const [catalog,jobRows]=await Promise.all([api('/api/catalog'),api('/api/jobs')]);
    const changed=!state.catalog || catalog.scannedAt!==state.catalog.scannedAt;
    const published=jobRows.some(j=>j.resultId && j.resultUpdated!==state.jobs.find(old=>old.id===j.id)?.resultUpdated);
    pendingIndexRefresh ||= published;
    if(changed){
      const visibleIds=new Set([...state.selected,...(state.detail?[state.detail.id]:[])]);
      for(const id of visibleIds){
        const current=catalog.runs.find(r=>r.id===id), cached=state.details.get(id);
        if(current && current.updated!==cached?.updated){const fresh=await loadDetail(id);if(state.detail?.id===id)state.detail=fresh;}
        if(!current){state.selected.delete(id);state.details.delete(id);if(state.detail?.id===id)state.detail=null;}
      }
    }
    state.catalog=catalog;state.jobs=jobRows;
    $('index-state').textContent=catalog.scanning?'Indexing retained artifacts…':`${fmt(catalog.runs.length)} retained runs · ${fmt(catalog.configs.filter(c=>c.ready).length)} prepared configurations${catalog.errorCount?` · ${catalog.errorCount} artifacts could not be read`:''} · Refresh to discover new external results`;
    if(changed && !state.detail && catalog.runs.length) {
      const preferred=catalog.runs.find(r=>r.mode==='recorded' && r.attempts>0) || catalog.runs[0];
      await openRun(preferred.id);
    } else if(changed || state.view==='jobs') render();
    $('job-count').textContent=jobRows.filter(j=>j.running).length;
    if(pendingIndexRefresh && !catalog.scanning){await api('/api/refresh',{});pendingIndexRefresh=false;}
  } catch(e) { $('index-state').textContent=`Connection unavailable: ${e.message}. Retrying…`; }
  finally {polling=false;}
}

document.addEventListener('click',async event=>{
  const b=event.target.closest('button');if(!b)return;
  try {
    if(b.dataset.view){state.view=b.dataset.view;render();}
    if(b.dataset.close)$(b.dataset.close).close();
    if(b.dataset.run)await openRun(b.dataset.run);
    if(b.dataset.tab){state.tab=b.dataset.tab;render();}
    if(b.dataset.attempt!==undefined)showAttempt(Number(b.dataset.attempt));
    if(b.dataset.remove){state.selected.delete(b.dataset.remove);render();}
    if(b.dataset.log){const log=await api(`/api/log?id=${encodeURIComponent(b.dataset.log)}`);showEvidence('Runner log · latest 32 KiB',`<pre class="code-block">${esc(log.text||'Waiting for runner output…')}</pre>`);}
    if(b.dataset.action){b.disabled=true;const r=await api('/api/action',{id:b.dataset.job,action:b.dataset.action});toast(r.message);await poll();}
    if(b.dataset.result){if(!state.catalog.runs.some(r=>r.id===b.dataset.result)){await refresh();toast('Results are being indexed. Open them again in a moment.');}else await openRun(b.dataset.result);}
    if(b.id==='go-compare'){state.view='compare';render();}
    if(b.id==='prev-page'){state.page--;render();}if(b.id==='next-page'){state.page++;render();}
    if(b.id==='prev-attempt'){state.attemptPage--;render();}if(b.id==='next-attempt'){state.attemptPage++;render();}
    if(b.id==='show-config')showEvidence('Configuration & provenance',`<pre class="code-block">${esc(JSON.stringify({source:state.detail.path,configuration:state.detail.configuration,provenance:state.detail.provenance},null,2))}</pre>`);
    if(b.id==='export-svg'){
      const svg=$('main-chart').querySelector('svg').cloneNode(true);
      const runs=state.view==='compare'?[...state.selected].map(id=>state.details.get(id)).filter(Boolean):[state.detail];
      const box=svg.getAttribute('viewBox').split(' ').map(Number), oldHeight=box[3];
      box[3]+=35+runs.length*17;svg.setAttribute('viewBox',box.join(' '));
      svg.querySelector('rect').setAttribute('height',box[3]);
      runs.forEach((run,i)=>{const text=document.createElementNS('http://www.w3.org/2000/svg','text');text.setAttribute('x','46');text.setAttribute('y',String(oldHeight+18+i*17));text.setAttribute('fill',colors[i]);text.textContent=`${run.method} · seed ${run.seed} · ${run.mode} · ${run.collection}`;svg.append(text);});
      svg.querySelectorAll('text').forEach(t=>{t.setAttribute('font-family','Arial, sans-serif');t.setAttribute('font-size','10');t.setAttribute('fill','#707889');});
      const url=URL.createObjectURL(new Blob([new XMLSerializer().serializeToString(svg)],{type:'image/svg+xml'}));const a=document.createElement('a');a.href=url;a.download=`discovery-${state.metric}.svg`;a.click();setTimeout(()=>URL.revokeObjectURL(url),1000);
    }
  }catch(e){fail(e);}
});
document.addEventListener('change',async event=>{
  const el=event.target;
  try{
    if(el.dataset.compare){
      if(el.checked){if(state.selected.size>=4){el.checked=false;return toast('Compare up to four runs at a time.');}state.selected.add(el.dataset.compare);await loadDetail(el.dataset.compare);}else state.selected.delete(el.dataset.compare);
      render();
    }
    if(el.id==='mode-filter'){state.mode=el.value;state.page=0;render();}
    if(el.id==='chart-metric'){state.metric=el.value;render();}
  }catch(e){fail(e);}
});
document.addEventListener('input',event=>{
  if(event.target.id==='run-search'){
    const position=event.target.selectionStart;state.search=event.target.value;state.page=0;render();$('run-search').focus();$('run-search').setSelectionRange(position,position);
  }
});
$('new-experiment').onclick=openLaunch;$('refresh').onclick=()=>refresh().catch(fail);$('preset').onchange=fillPreset;
$('select-workloads').onclick=()=>document.querySelectorAll('[name=workload]').forEach(e=>e.checked=true);
$('validate').onclick=async()=>{try{const result=await api('/api/validate',formBody());$('launch-error').classList.add('hidden');$('launch-review').textContent=result.message;}catch(e){launchError(e);}};
$('launch-form').onsubmit=async event=>{event.preventDefault();$('launch').disabled=true;try{await api('/api/launch',formBody());$('launch-dialog').close();state.view='jobs';await poll();render();toast('Experiment launched. You can close the browser and return later.');}catch(e){launchError(e);}finally{$('launch').disabled=false;}};
poll();setInterval(poll,4000);
