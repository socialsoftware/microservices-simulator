#!/usr/bin/env python3
"""Run the bounded cross-workload allocator against real ScenarioExecutor attempts."""
import argparse
import copy
from contextlib import contextmanager
import fcntl
import json
import math
import os
from pathlib import Path
import shutil
import signal
import subprocess
import threading
import time

from allocator import (ADAPTIVE_UCB_POLICIES, LINEAR_UCB_POLICIES, SUPPORTED_POLICIES,
                       allocate, structural_profile)
from catalogue import load as load_catalogue
from fitness import WEIGHTED_V2, assess, configuration
from run import check_control, require_scored_control
from runtime import (IntegrityError, Runtime, batch, digest, package, read,
                     retained_attempt)


SCHEMA = 'contextual-workload-live-allocation.v1'
PROTOCOL_SCHEMA = 'resumable-live-allocation.v1'
DISPATCH_SCHEMA = 'live-allocation-dispatch.v1'
RECEIPT_SCHEMA = 'live-allocation-completion.v1'


class AmbiguousDispatchError(RuntimeError):
    pass


def _number(value, *, positive=False, nonnegative=False, label='value'):
    valid = type(value) in (int, float) and math.isfinite(value)
    if positive:
        valid = valid and value > 0
    if nonnegative:
        valid = valid and value >= 0
    if not valid:
        raise ValueError(f'{label} must be a finite ' +
                         ('positive' if positive else 'nonnegative') + ' number')
    return float(value)


def _resolve(base, value):
    path = Path(value)
    return str((path if path.is_absolute() else base / path).resolve())


def parse_configuration(path):
    """Read the strict live contract without changing the recorded allocator schema."""
    path = Path(path).resolve()
    value = read(path)
    allowed = {'schemaVersion', 'seed', 'budget', 'policy', 'policyParameters',
               'fitness', 'workloads'}
    if not isinstance(value, dict) or set(value) - allowed:
        raise ValueError('Unknown live allocator configuration fields')
    if value.get('schemaVersion') != SCHEMA:
        raise ValueError('Unsupported live allocator configuration schema')
    if type(value.get('seed')) is not int:
        raise ValueError('seed must be an integer')
    if type(value.get('budget')) is not int or value['budget'] < 1:
        raise ValueError('budget must be a positive integer')
    policy = value.get('policy')
    if policy not in SUPPORTED_POLICIES:
        raise ValueError('Unknown allocation policy')
    parameters = value.get('policyParameters', {})
    if not isinstance(parameters, dict):
        raise ValueError('policyParameters must be an object')
    expected = ({'exploration'} if policy in ADAPTIVE_UCB_POLICIES else
                {'exploration', 'ridge'} if policy in LINEAR_UCB_POLICIES else set())
    if set(parameters) - expected:
        raise ValueError('Unknown parameters for allocation policy')
    if policy in ADAPTIVE_UCB_POLICIES:
        parameters = {'exploration': _number(parameters.get('exploration', 1.0),
                                              nonnegative=True, label='exploration')}
    elif policy in LINEAR_UCB_POLICIES:
        parameters = {
            'exploration': _number(parameters.get('exploration', 1.0),
                                   nonnegative=True, label='exploration'),
            'ridge': _number(parameters.get('ridge', 1.0), positive=True, label='ridge')}
    else:
        parameters = {}
    fitness = configuration(value.get('fitness'))
    if fitness['policy'] != WEIGHTED_V2:
        raise ValueError('Live cross-workload allocation requires weighted-criteria-v2')
    rows = value.get('workloads')
    if not isinstance(rows, list) or not rows:
        raise ValueError('workloads must be a nonempty array')
    workloads = []
    for row in rows:
        required = {'config', 'catalogue', 'control'}
        if not isinstance(row, dict) or not required <= set(row) \
                or set(row) - {'name', *required}:
            raise ValueError('Each live workload requires config, catalogue and control paths')
        if any(not isinstance(row[field], str) or not row[field] for field in required):
            raise ValueError('Workload paths must be nonempty strings')
        name = row.get('name')
        if name is not None and (not isinstance(name, str) or not name.strip()):
            raise ValueError('Workload names must be nonempty strings')
        workloads.append({'name': name, **{field: _resolve(path.parent, row[field])
                                          for field in required}})
    return {'schemaVersion': SCHEMA, 'seed': value['seed'], 'budget': value['budget'],
            'policy': policy, 'policyParameters': parameters, 'fitness': fitness,
            'workloads': workloads, 'configurationPath': str(path),
            'configurationSha256': digest(path)}


def _durable_text(path, content):
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(path.name + '.tmp')
    with temporary.open('w') as stream:
        stream.write(content)
        stream.flush()
        os.fsync(stream.fileno())
    os.replace(temporary, path)
    descriptor = os.open(path.parent, os.O_RDONLY)
    try:
        os.fsync(descriptor)
    finally:
        os.close(descriptor)


def durable_save(path, value):
    _durable_text(path, json.dumps(value, indent=2, sort_keys=True) + '\n')


def durable_journal(path, rows):
    _durable_text(path, ''.join(json.dumps(row, sort_keys=True, separators=(',', ':')) + '\n'
                                for row in rows))


@contextmanager
def exclusive(out):
    with (out / '.lock').open('a') as lock:
        try:
            fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError:
            raise ValueError('Live allocation already running') from None
        try:
            yield
        finally:
            fcntl.flock(lock, fcntl.LOCK_UN)


def _source_paths():
    here = Path(__file__).resolve().parent
    validators = here.parent / 'impact-v2-broader'
    return [here / name for name in ('live_allocate.py', 'allocator.py',
            'allocation_cooldown.py', 'catalogue.py', 'fitness.py', 'runtime.py',
            'search.py', 'run.py')] + [Path(batch.__file__).resolve(),
            validators / 'qualification.py', validators / 'validate_assessment.py']


def verified_control(config, control_path):
    """Recompute control feedback from its attested attempt, not copied summary fields."""
    control_hash = check_control(config, control_path)
    baseline = read(control_path)
    try:
        attempt_path = Path(baseline['directory']) / 'attempt.json'
        raw = read(attempt_path)
        replay = read(attempt_path.parent / 'replay.json')
        measured = retained_attempt(attempt_path)
    except (KeyError, OSError, TypeError, ValueError) as error:
        raise IntegrityError('Cannot verify retained control attempt: ' + str(error)) from None
    if baseline != raw:
        raise IntegrityError('Control summary differs from its retained attempt')
    if replay.get('runtime') != config['runtime'] \
            or replay.get('candidate') != raw.get('candidate') \
            or replay.get('packageHashes') != raw.get('packageHashes'):
        raise IntegrityError('Control attempt runtime, candidate or package join differs')
    fields = ('candidate', 'packageHashes', 'reportHashes', 'status', 'terminalStatus',
              'scheduleConformance', 'I', 'impactCategories', 'A', 'AStatus',
              'ACoverage', 'AGaps', 'lostCopiedUpdateCount',
              'lostCopiedUpdateValidity', 'lostCopiedUpdateCoverage',
              'lostCopiedUpdateCoverageGaps')
    if any(raw.get(field) != measured.get(field) for field in fields):
        raise IntegrityError('Control summary disagrees with verified raw reports')
    return control_hash, measured


def load_inputs(config):
    """Qualify every explicit workload without exposing any candidate outcome."""
    workloads, runtimes = [], {}
    identities, names = set(), set()
    for index, row in enumerate(config['workloads'], 1):
        config_path = Path(row['config'])
        catalogue_path = Path(row['catalogue'])
        control_path = Path(row['control'])
        map_config = read(config_path)
        workload_id = map_config.get('workload')
        if not isinstance(workload_id, str) or not workload_id or workload_id in identities:
            raise ValueError('Workload identities must be unique nonempty strings')
        map_fitness = configuration(map_config.get('fitness'))
        if map_fitness['policy'] != WEIGHTED_V2:
            raise ValueError('Live workload configuration must use weighted-criteria-v2')
        control_hash, baseline = verified_control(map_config, control_path)
        try:
            require_scored_control(baseline, config['fitness'])
        except ValueError as error:
            raise ValueError(str(error) + ': ' + workload_id) from None
        source_hashes = package(Path(map_config['manifest']))['hashes']
        domain, seal = load_catalogue(catalogue_path, map_config, source_hashes)
        data = package(catalogue_path / 'package/scenario-catalog-manifest.json')
        if domain.workload['id'] != workload_id or seal['workload'] != workload_id:
            raise IntegrityError('Catalogue/workload identity mismatch')
        runtime = Runtime(map_config['runtime'])
        runtime.verify()
        name = row['name'] or workload_id
        if name in names:
            raise ValueError('Workload names must be unique')
        root_name = f'workload-{index:03d}'
        provenance = {
            'workload': workload_id, 'name': name,
            'config': str(config_path), 'configSha256': digest(config_path),
            'catalogue': str(catalogue_path),
            'catalogueSha256': digest(catalogue_path / 'catalogue.json'),
            'cataloguePackageHashes': seal['packageHashes'],
            'candidateCount': len(domain.candidates),
            'control': str(control_path), 'controlSha256': control_hash,
            'runtime': copy.deepcopy(map_config['runtime']), 'outputRoot': root_name}
        workloads.append({'id': workload_id, 'name': name, 'domain': domain,
                          'profile': structural_profile(data['records'], domain.workload),
                          'provenance': provenance, 'mapConfig': map_config,
                          'cataloguePath': catalogue_path, 'outputRootName': root_name})
        runtimes[workload_id] = runtime
        identities.add(workload_id); names.add(name)
    return workloads, runtimes


def protocol_identity(config, workloads):
    sources = _source_paths()
    return {'schemaVersion': PROTOCOL_SCHEMA,
            'configuration': config,
            'sourceHashes': {str(path): digest(path) for path in sources},
            'workloads': [row['provenance'] for row in workloads],
            'controlGate': {
                'required': 'valid measured no-fault outcome with complete enabled weighted score',
                'evidenceApplicationExecutions': len({row['provenance']['controlSha256']
                                                      for row in workloads}),
                'applicationExecutionsByThisCommand': 0,
                'outsideGlobalSearchBudget': True}}


def _execution_root(out, workload):
    return out / 'workloads' / workload['outputRootName']


def initialize_output(out, workloads):
    (out / 'dispatches').mkdir()
    (out / 'receipts').mkdir()
    (out / 'source').mkdir()
    (out / 'workloads').mkdir()
    for workload in workloads:
        root = _execution_root(out, workload)
        root.mkdir()
        shutil.copytree(workload['cataloguePath'] / 'package', root / 'package')
        if package(root / 'package/scenario-catalog-manifest.json')['hashes'] \
                != workload['provenance']['cataloguePackageHashes']:
            raise IntegrityError('Catalogue package changed while copying')
    for path in _source_paths():
        name = 'batch-run.py' if path == Path(batch.__file__).resolve() else path.name
        shutil.copy2(path, out / 'source' / name)


def verify_output(out, workloads):
    for workload in workloads:
        root = _execution_root(out, workload)
        if package(root / 'package/scenario-catalog-manifest.json')['hashes'] \
                != workload['provenance']['cataloguePackageHashes']:
            raise IntegrityError('Live allocation package changed: ' + workload['id'])


def _strict_read(path, label):
    try:
        return read(path)
    except (OSError, ValueError, TypeError) as error:
        raise IntegrityError(f'Invalid {label}: {path.name}: {error}') from None


def _numbered(paths, prefix):
    values = {}
    for path in paths:
        try:
            number = int(path.stem.split('-')[-1])
        except (ValueError, IndexError):
            raise IntegrityError(f'Invalid {prefix} file name: {path.name}') from None
        if number in values:
            raise IntegrityError(f'Duplicate {prefix} number: {number}')
        values[number] = path
    return values


def _attempt_path(out, workload, decision):
    return _execution_root(out, workload) / f'attempt-{decision:03d}' / 'attempt.json'


def validate_attempt(path, workload, candidate):
    replay = _strict_read(path.parent / 'replay.json', 'attempt replay')
    if replay.get('runtime') != workload['mapConfig']['runtime'] \
            or replay.get('candidate') != candidate \
            or replay.get('packageHashes') != workload['provenance']['cataloguePackageHashes']:
        raise IntegrityError('Attempt runtime, candidate or package differs')
    measured = retained_attempt(path)
    if measured.get('candidate') != candidate \
            or measured.get('packageHashes') != workload['provenance']['cataloguePackageHashes']:
        raise IntegrityError('Attempt feedback identity differs')
    return measured


def unavailable_observation(candidate, dispatch):
    return {'candidate': candidate, 'status': 'AMBIGUOUS_DISPATCH', 'I': None,
            'A': None, 'AStatus': 'UNAVAILABLE', 'ACoverage': 'UNAVAILABLE', 'AGaps': [],
            'lostCopiedUpdateCount': None, 'lostCopiedUpdateValidity': 'UNAVAILABLE',
            'lostCopiedUpdateCoverage': 'UNAVAILABLE',
            'lostCopiedUpdateCoverageGaps': [], 'terminalStatus': None,
            'scheduleConformance': None, 'impactCategories': [],
            'resolution': 'EXPLICITLY_MARKED_SPENT_WITHOUT_RETRY',
            'dispatchSha256': digest(dispatch)}


def _receipt(out, dispatch_path, workload, observation, outcome, attempt_path=None):
    decision = _strict_read(dispatch_path, 'dispatch')['decision']
    value = {'schemaVersion': RECEIPT_SCHEMA, 'decision': decision,
             'workload': workload['id'], 'candidate': observation['candidate'],
             'dispatchSha256': digest(dispatch_path), 'outcome': outcome,
             'completedAt': time.time()}
    if outcome == 'VERIFIED_ATTEMPT':
        value.update(attemptPath=str(attempt_path), attemptSha256=digest(attempt_path))
    else:
        value['observation'] = observation
    durable_save(out / 'receipts' / f'decision-{decision:06d}.json', value)
    return value


def _journal(out):
    path = out / 'decisions.jsonl'
    if not path.exists():
        return []
    rows = []
    try:
        for line in path.read_text().splitlines():
            if line.strip():
                rows.append(json.loads(line))
    except (OSError, ValueError, TypeError) as error:
        raise IntegrityError('Decision journal is corrupt: ' + str(error)) from None
    if any(row.get('decision') != index for index, row in enumerate(rows, 1)):
        raise IntegrityError('Decision journal is not a contiguous prefix')
    return rows


def restore(out, workloads):
    """Validate receipts and salvage a completed last dispatch, never redispatch it."""
    by_id = {row['id']: row for row in workloads}
    dispatches = _numbered((out / 'dispatches').glob('decision-*.json'), 'dispatch')
    receipts = _numbered((out / 'receipts').glob('decision-*.json'), 'receipt')
    if set(receipts) != set(range(1, len(receipts) + 1)):
        raise IntegrityError('Completion receipts are not contiguous')
    if set(dispatches) not in (set(receipts), set(receipts) | {len(receipts) + 1}):
        raise IntegrityError('Dispatch history is not a completed prefix plus at most one pending')
    completed = []
    for decision in range(1, len(receipts) + 1):
        dispatch_path, receipt_path = dispatches[decision], receipts[decision]
        dispatch = _strict_read(dispatch_path, 'dispatch')
        receipt = _strict_read(receipt_path, 'receipt')
        if dispatch.get('schemaVersion') != DISPATCH_SCHEMA or dispatch.get('decision') != decision:
            raise IntegrityError('Invalid dispatch identity')
        if receipt.get('schemaVersion') != RECEIPT_SCHEMA or receipt.get('decision') != decision \
                or receipt.get('dispatchSha256') != digest(dispatch_path):
            raise IntegrityError('Invalid completion receipt identity')
        workload = by_id.get(dispatch.get('workload'))
        if workload is None or receipt.get('workload') != workload['id'] \
                or receipt.get('candidate') != dispatch.get('candidate'):
            raise IntegrityError('Receipt workload/candidate differs from dispatch')
        if receipt.get('outcome') == 'VERIFIED_ATTEMPT':
            attempt_path = _attempt_path(out, workload, decision)
            if receipt.get('attemptPath') != str(attempt_path) or not attempt_path.exists() \
                    or receipt.get('attemptSha256') != digest(attempt_path):
                raise IntegrityError('Completed attempt missing or changed')
            observation = validate_attempt(attempt_path, workload, dispatch['candidate'])
        elif receipt.get('outcome') == 'AMBIGUOUS_SPENT':
            observation = receipt.get('observation')
            if observation != unavailable_observation(dispatch['candidate'], dispatch_path):
                raise IntegrityError('Ambiguous-spent observation changed')
        else:
            raise IntegrityError('Unknown completion receipt outcome')
        completed.append({'dispatch': dispatch, 'receipt': receipt,
                          'observation': observation})
    pending = None
    number = len(receipts) + 1
    if number in dispatches:
        dispatch_path = dispatches[number]
        dispatch = _strict_read(dispatch_path, 'pending dispatch')
        workload = by_id.get(dispatch.get('workload'))
        if dispatch.get('schemaVersion') != DISPATCH_SCHEMA or dispatch.get('decision') != number \
                or workload is None:
            raise IntegrityError('Invalid pending dispatch')
        attempt_path = _attempt_path(out, workload, number)
        if attempt_path.exists():
            try:
                observation = validate_attempt(attempt_path, workload, dispatch['candidate'])
            except (IntegrityError, ValueError, KeyError, OSError, TypeError) as error:
                pending = {'dispatch': dispatch, 'path': dispatch_path,
                           'error': 'Attempt exists but is not a verified completion: ' + str(error)}
            else:
                receipt = _receipt(out, dispatch_path, workload, observation,
                                   'VERIFIED_ATTEMPT', attempt_path)
                completed.append({'dispatch': dispatch, 'receipt': receipt,
                                  'observation': observation})
        else:
            pending = {'dispatch': dispatch, 'path': dispatch_path,
                       'error': 'No verified attempt result exists'}
    journal = _journal(out)
    if len(journal) > len(completed):
        raise IntegrityError('Decision journal is ahead of completion receipts')
    return completed, journal, pending


def check_orphans(out, workloads):
    """Do not resolve or continue while an old isolated executor may still run."""
    roots = {str(_execution_root(out, row).resolve()) for row in workloads}
    try:
        ids = subprocess.run(['docker', 'ps', '-q'], check=True, capture_output=True,
                             text=True, timeout=20).stdout.split()
        if not ids:
            return
        containers = json.loads(subprocess.run(['docker', 'inspect', *ids], check=True,
                                capture_output=True, text=True, timeout=20).stdout)
    except (OSError, subprocess.SubprocessError, ValueError) as error:
        raise ValueError('Cannot prove that an interrupted executor stopped: ' + str(error)) from None
    names = [row.get('Name', '<unnamed>') for row in containers
             if any(mount.get('Source') in roots for mount in row.get('Mounts', []))]
    if names:
        raise ValueError('Previous live attempts still running: ' + ', '.join(names))


class LiveEvaluator:
    mode = 'SCENARIO_EXECUTOR_LIVE_SEQUENTIAL'

    def __init__(self, out, workloads, runtimes, completed, budget):
        self.out = out
        self.workload_list = workloads
        self.workloads = {row['id']: row for row in workloads}
        self.runtimes = runtimes
        self.completed = completed
        self.budget = budget
        self.replayed = 0
        self.launched = 0

    def evaluate(self, workload_id, candidate, attempt):
        if attempt <= len(self.completed):
            retained = self.completed[attempt - 1]
            dispatch = retained['dispatch']
            if dispatch['workload'] != workload_id or dispatch['candidate'] != candidate:
                raise IntegrityError('Deterministic replay selected a different workload/candidate')
            self.replayed += 1
            return copy.deepcopy(retained['observation'])
        if attempt != len(self.completed) + self.launched + 1:
            raise IntegrityError('Live attempt numbering diverged')
        workload = self.workloads[workload_id]
        runtime = self.runtimes[workload_id]
        # Runtime artifacts may be edited while a long allocation is paused between
        # decisions. Refuse the next dispatch before it can execute changed code.
        runtime.verify()
        dispatch_path = self.out / 'dispatches' / f'decision-{attempt:06d}.json'
        dispatch = {'schemaVersion': DISPATCH_SCHEMA, 'decision': attempt,
                    'workload': workload_id, 'candidate': candidate,
                    'timeoutSeconds': workload['mapConfig'].get('timeout', 180),
                    'createdAt': time.time()}
        durable_save(dispatch_path, dispatch)
        self.launched += 1
        _write_status(self.out, 'RUNNING', attempt - 1, self.budget, in_flight=1,
                      pendingDecision=attempt)
        root = _execution_root(self.out, workload)
        runtime.evaluate(
            root, candidate, attempt, workload['mapConfig'].get('timeout', 180))
        attempt_path = _attempt_path(self.out, workload, attempt)
        observation = validate_attempt(attempt_path, workload, candidate)
        if observation.get('status') in ('TIMEOUT', 'PROCESS_FAILURE', 'INFRASTRUCTURE_FAILURE'):
            # A timeout cleanup or a failed Docker client does not by itself prove
            # the server-side executor stopped. Check before allowing another call.
            check_orphans(self.out, self.workload_list)
        _receipt(self.out, dispatch_path, workload, observation,
                 'VERIFIED_ATTEMPT', attempt_path)
        return observation


def _semantic_decision(row):
    return {key: value for key, value in row.items()
            if key not in ('selectionOverheadMicros', 'updateOverheadMicros')}


def _write_status(out, stage, completed, budget, in_flight=0, **extra):
    durable_save(out / 'status.json', {'stage': stage, 'completedDecisions': completed,
                 'globalBudget': budget, 'inFlight': in_flight,
                 'updatedAt': time.time(), **extra})


def summary(result):
    control = result['controlGate']
    lines = ['# Live cross-workload allocation', '',
        f"Mode: `{result['mode']}`; policy: `{result['policy']}`; "
        f"global attempts: {result['globalAttempts']}/{result['globalBudget']}; "
        f"stop: `{result['stopReason']}`.", '',
        f"Observed positive scenarios: {result['positiveDiscoveries']}; "
        f"cumulative configured score: {result['cumulativeScore']}; "
        f"unknown scores: {result['unknowns']}.", '',
        f"Qualified controls represented {control['evidenceApplicationExecutions']} retained "
        'application execution(s), outside the global search budget; this command ran no controls.', '',
        '| Workload | Attempts | Positives | Score | Unknown | State |',
        '| --- | ---: | ---: | ---: | ---: | --- |']
    for row in result['perWorkload']:
        lines.append(f"| {row['name']} | {row['allocations']} | {row['positiveDiscoveries']} | "
                     f"{row['cumulativeScore']} | {row['unknowns']} | {row['localStopReason']} |")
    lines += ['', 'Completed decisions are durable and replayed deterministically on resume. '
              'Unavailable feedback consumes its decision and updates neither GA parents nor an adaptive model.']
    return '\n'.join(lines) + '\n'


def execute(config_path, out, resume=False, stop=None):
    config = parse_configuration(config_path)
    out = Path(out).resolve()
    out.mkdir(parents=True, exist_ok=resume)
    with exclusive(out):
        workloads, runtimes = load_inputs(config)
        identity = protocol_identity(config, workloads)
        if resume:
            if _strict_read(out / 'protocol.json', 'protocol') != identity:
                raise IntegrityError('Live allocation inputs, runtime or runner changed; cannot resume')
            verify_output(out, workloads)
            check_orphans(out, workloads)
        else:
            initialize_output(out, workloads)
            durable_save(out / 'configuration.json', config)
            durable_save(out / 'protocol.json', identity)
        completed, old_journal, pending = restore(out, workloads)
        if pending is not None:
            _write_status(out, 'REVIEW_REQUIRED', len(completed), config['budget'],
                          in_flight=None, possibleInFlight=True,
                          pendingDecision=pending['dispatch']['decision'], error=pending['error'])
            raise AmbiguousDispatchError(
                f"Decision {pending['dispatch']['decision']} has an ambiguous dispatch. "
                'Inspect it, ensure no executor remains, then use resolve-spent explicitly.')
        if resume:
            (out / 'PAUSE').unlink(missing_ok=True)
        sessions = _strict_read(out / 'sessions.json', 'sessions') \
            if (out / 'sessions.json').exists() else []
        session = {'startedAt': time.time(), 'resumed': resume,
                   'alreadyCompleted': len(completed)}
        sessions.append(session); durable_save(out / 'sessions.json', sessions)
        evaluator = LiveEvaluator(out, workloads, runtimes, completed, config['budget'])
        effective = []
        stop = stop or threading.Event()
        _write_status(out, 'RUNNING', len(completed), config['budget'])

        def stopping():
            return evaluator.replayed >= len(completed) \
                and (stop.is_set() or (out / 'PAUSE').exists())

        def emit(decision):
            index = decision['decision'] - 1
            if index < len(old_journal):
                if _semantic_decision(old_journal[index]) != _semantic_decision(decision):
                    raise IntegrityError('Deterministic decision replay differs from journal')
                effective.append(old_journal[index])
            else:
                effective.append(decision)
                durable_journal(out / 'decisions.jsonl', effective)
            _write_status(out, 'RUNNING', len(effective), config['budget'])

        try:
            result = allocate(workloads, evaluator, policy=config['policy'],
                parameters=config['policyParameters'], seed=config['seed'],
                budget=config['budget'], fitness=config['fitness'], emit=emit,
                stop_requested=stopping)
            if len(effective) != len(completed) + evaluator.launched:
                raise IntegrityError('Decision journal/completion receipt count differs')
            verify_output(out, workloads)
            for runtime in runtimes.values():
                runtime.verify()
            result['decisions'] = effective
            result['selectionOverheadMicros'] = sum(
                row['selectionOverheadMicros'] for row in effective)
            result['updateOverheadMicros'] = sum(
                row['updateOverheadMicros'] for row in effective)
            result['inputs'] = [row['provenance'] for row in workloads]
            result['configurationPath'] = config['configurationPath']
            result['configurationSha256'] = config['configurationSha256']
            result['controlGate'] = identity['controlGate']
            verified = sum(
                row['receipt']['outcome'] == 'VERIFIED_ATTEMPT' for row in completed) \
                + evaluator.launched
            ambiguous = sum(
                row['receipt']['outcome'] == 'AMBIGUOUS_SPENT' for row in completed)
            # A valid receipt can record a timeout or a failed Docker launch.
            # Count returned attempt results, not presumed application executions.
            result['verifiedAttemptResults'] = verified
            result['ambiguousDispatchesMarkedSpent'] = ambiguous
            result['executionEquivalentBudgetConsumed'] = result['globalAttempts']
            result['integrity'] = 'PASS'
            serializable = {key: value for key, value in result.items() if key != 'decisions'}
            durable_save(out / 'results.json', serializable)
            _durable_text(out / 'SUMMARY.md', summary(serializable))
            stage = 'PAUSED' if result['stopReason'] == 'PAUSED' else 'COMPLETE'
            _write_status(out, stage, result['globalAttempts'], config['budget'],
                          stopReason=result['stopReason'])
            return serializable
        except Exception as error:
            dispatch_count = len(list((out / 'dispatches').glob('decision-*.json')))
            receipt_count = len(list((out / 'receipts').glob('decision-*.json')))
            stage = 'REVIEW_REQUIRED' if dispatch_count > receipt_count else 'FAILED'
            _write_status(out, stage, receipt_count, config['budget'],
                          in_flight=None if stage == 'REVIEW_REQUIRED' else 0,
                          possibleInFlight=stage == 'REVIEW_REQUIRED', error=str(error),
                          pendingDecision=dispatch_count if stage == 'REVIEW_REQUIRED' else None)
            raise
        finally:
            session.update(finishedAt=time.time(), newDispatches=evaluator.launched,
                           status=_strict_read(out / 'status.json', 'status'))
            durable_save(out / 'sessions.json', sessions)


def resolve_spent(out, decision):
    out = Path(out).resolve()
    with exclusive(out):
        protocol = _strict_read(out / 'protocol.json', 'protocol')
        config = protocol['configuration']
        workloads, _ = load_inputs(config)
        if protocol_identity(config, workloads) != protocol:
            raise IntegrityError('Live allocation inputs, runtime or runner changed')
        check_orphans(out, workloads)
        completed, _, pending = restore(out, workloads)
        if pending is None or pending['dispatch']['decision'] != decision:
            raise ValueError('Decision is not the current ambiguous dispatch')
        dispatch = pending['dispatch']
        workload = next(row for row in workloads if row['id'] == dispatch['workload'])
        observation = unavailable_observation(dispatch['candidate'], pending['path'])
        _receipt(out, pending['path'], workload, observation, 'AMBIGUOUS_SPENT')
        _write_status(out, 'PAUSED', len(completed) + 1, config['budget'],
                      stopReason='AMBIGUOUS_DISPATCH_MARKED_SPENT')
        return read(out / 'status.json')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='command', required=True)
    for name in ('run', 'resume'):
        command = sub.add_parser(name)
        command.add_argument('--config', type=Path, required=True)
        command.add_argument('--output', type=Path, required=True)
    for name in ('pause', 'status'):
        command = sub.add_parser(name)
        command.add_argument('--output', type=Path, required=True)
    command = sub.add_parser('resolve-spent')
    command.add_argument('--output', type=Path, required=True)
    command.add_argument('--decision', type=int, required=True)
    args = parser.parse_args()
    if args.command == 'pause':
        if not (args.output / 'protocol.json').exists():
            parser.error('Not a live allocation directory')
        _durable_text(args.output / 'PAUSE', '')
        print('Pause requested. Wait for status PAUSED or COMPLETE before stopping the terminal.')
    elif args.command == 'status':
        print(json.dumps(_strict_read(args.output / 'status.json', 'status'), indent=2))
    elif args.command == 'resolve-spent':
        print(json.dumps(resolve_spent(args.output, args.decision), indent=2))
    else:
        stop = threading.Event()
        for sig in (signal.SIGINT, signal.SIGTERM):
            signal.signal(sig, lambda *_: stop.set())
        try:
            result = execute(args.config, args.output, resume=args.command == 'resume', stop=stop)
        except AmbiguousDispatchError as error:
            parser.exit(2, str(error) + '\n')
        print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
