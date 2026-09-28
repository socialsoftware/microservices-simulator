// Exercise the shipped polling function with in-memory API/DOM boundaries.
// No browser, server, or external experiment is started.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const source = fs.readFileSync(__dirname + '/static/app.js', 'utf8');
const pollSource = source.slice(source.indexOf('async function poll()'), source.indexOf('\ndocument.addEventListener'));

function fixture() {
  const run = {id:'r', updated:1};
  const job = {id:'j', resultId:'r', resultUpdated:1};
  const state = {catalog:{scannedAt:1,runs:[run]},jobs:[job],selected:new Set(['r']),
    detail:run, details:new Map([['r',run]]),view:'experiments'};
  const calls = [];
  const context = {state,polling:false,pendingIndexRefresh:false,fmt:String,
    response:{scannedAt:1,runs:[run],configs:[],scanning:true},jobResponse:[{...job,resultUpdated:2}],
    $:()=>({textContent:''}),render:()=>{},openRun:()=>{},
    loadDetail:async id=>{calls.push('detail:'+id); const fresh={id,updated:2};state.details.set(id,fresh);return fresh;},
    api:async path=>{calls.push(path);return path==='/api/catalog'?context.response:path==='/api/jobs'?context.jobResponse:{};}};
  vm.createContext(context);vm.runInContext(pollSource,context);
  return {context,calls,state,poll:()=>vm.runInContext('poll()',context)};
}

(async()=>{
  const f=fixture();
  await f.poll();
  assert.equal(f.calls.includes('/api/refresh'),false);
  assert.equal(f.context.pendingIndexRefresh,true);
  f.context.response.scanning=false;
  await f.poll();
  assert.equal(f.calls.filter(p=>p==='/api/refresh').length,1);
  assert.equal(f.context.pendingIndexRefresh,false);
  f.context.response={...f.context.response,scannedAt:2,runs:[{id:'r',updated:2}]};
  await f.poll();
  assert.equal(f.state.detail.updated,2);
  assert.equal(f.state.details.get('r').updated,2);
  assert.equal(f.calls.filter(p=>p==='detail:r').length,1);
  console.log('PASS: result update during scan is deferred; active and compared details refresh.');
})().catch(error=>{console.error(error);process.exitCode=1;});
