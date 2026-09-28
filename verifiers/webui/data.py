"""Read-only adapters for retained verifier experiments. No runner imports or execution."""
import gzip
import hashlib
import json
import math
import os
from pathlib import Path
import threading
import time

MAX_BYTES = 64 * 1024 * 1024
SCHEMAS = {'contextual-workload-allocation.v1': 'recorded',
           'contextual-workload-live-allocation.v1': 'live'}
POLICIES = ['uniform', 'round-robin', 'adaptive-ucb', 'progress-linucb',
            'contextual-linucb', 'adaptive-ucb-cooldown', 'progress-linucb-cooldown',
            'contextual-linucb-cooldown']
CRITERIA = ['DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL',
            'UNRESOLVED_DELIVERED_EVENT', 'COMPENSATED_READ_EXPOSURE', 'LOST_COPIED_UPDATE']


def read(path):
    opener = gzip.open if str(path).endswith('.gz') else open
    with opener(path, 'rb') as stream:
        raw = stream.read(MAX_BYTES + 1)
    if len(raw) > MAX_BYTES:
        raise ValueError('Artifact exceeds the 64 MiB viewer limit')
    return json.loads(raw)


def optional(path):
    try:
        value = read(path)
        return value if isinstance(value, dict) else {}
    except (OSError, ValueError):
        return {}


def identifier(path):
    return hashlib.sha256(str(path.resolve()).encode()).hexdigest()[:24]


def number(value):
    return value if type(value) in (int, float) and math.isfinite(value) else None


def journal(path):
    if not path.is_file():
        return []
    if path.stat().st_size > MAX_BYTES:
        raise ValueError('Journal exceeds the 64 MiB viewer limit')
    rows = []
    with path.open() as stream:
        for line in stream:
            try:
                rows.append(json.loads(line))
            except ValueError:
                # A producer may be in the middle of appending its last line.
                if line.endswith('\n'):
                    raise
    return rows


def normalize(path, raw, detail=False):
    """Preserve null feedback; do not reinterpret it as measured zero."""
    allocation = 'globalAttempts' in raw or isinstance(raw.get('decisions'), list)
    fixed = isinstance(raw.get('attempts'), list) and 'strategy' in raw
    if not allocation and not fixed:
        return None
    config = optional(path.parent / 'configuration.json') or optional(path.parent / 'config.json')
    job = optional(path.parent.parent / 'job.json') if path.parent.name == 'output' else {}
    rows = raw.get('decisions') if allocation else raw['attempts']
    if rows is None:
        rows = journal(path.parent / 'decisions.jsonl')
    rows = rows or []
    fitness = raw.get('fitness') or config.get('fitness')
    legacy_i = not fitness and raw.get('fitnessPolicy') == 'complete-impact-v2-object-count'
    if legacy_i:
        fitness = {'policy': 'complete-impact-v2-object-count', 'label': 'Legacy complete ImpactV2 object count (I)'}
    mode = raw.get('mode', '').lower()
    if 'recorded' in mode or (not mode and path.suffix == '.gz'):
        mode = 'recorded'
    elif fixed or 'live' in mode:
        mode = 'live'
    else:
        mode = 'unknown'
    scope = raw.get('inputs') or config.get('workloads') or config.get('workload')
    # Unknown provenance must never imply comparability.
    comparison = None
    # Fixed-workload historical artifacts do not uniformly retain a sealed
    # source/runtime/domain identity. A workload id alone is insufficient.
    if allocation and mode != 'unknown' and scope and fitness:
        comparison = hashlib.sha256(json.dumps([mode, fitness, scope], sort_keys=True).encode()).hexdigest()
    trajectory = [{'attempt': 0, 'score': 0, 'positives': 0, 'unknowns': 0}]
    total, positives, unknowns = 0, 0, 0
    attempts, workloads, categories = [], {}, {}
    for index, row in enumerate(rows, 1):
        score = number(row.get('score') if allocation else row.get('fitnessScore', row.get('I') if legacy_i else None))
        total += score if score is not None else 0
        positives += int(score is not None and score > 0)
        unknowns += int(score is None)
        trajectory.append({'attempt': index, 'score': total, 'positives': positives, 'unknowns': unknowns})
        candidate = row.get('candidate', {})
        candidate = candidate if isinstance(candidate, dict) else {}
        workload = row.get('workload') or candidate.get('workload') or config.get('workload') or 'unknown'
        w = workloads.setdefault(workload, {'id': workload, 'name': row.get('workloadName') or workload,
                                           'attempts': 0, 'positives': 0, 'unknowns': 0, 'score': 0})
        w['attempts'] += 1
        w['positives'] += int(score is not None and score > 0)
        w['unknowns'] += int(score is None)
        w['score'] += score if score is not None else 0
        components = row.get('fitnessComponents') or {}
        for name, component in components.items():
            value = number(component.get('count')) if isinstance(component, dict) else None
            if value is not None:
                c = categories.setdefault(name, {'count': 0, 'observations': 0})
                c['count'] += value
                c['observations'] += 1
        if detail:
            attempts.append({'attempt': index, 'scenario': row.get('scenarioId') or candidate.get('id'),
                             'workload': workload, 'name': w['name'], 'score': score,
                             'status': row.get('terminalStatus') or row.get('status'),
                             'conformance': row.get('scheduleConformance'),
                             'vector': candidate.get('faultVector'), 'actions': candidate.get('actions', []),
                             'components': components, 'raw': row})
    for retained in raw.get('perWorkload', []):
        workload = retained.get('workload')
        if workload and workload not in workloads:
            workloads[workload] = {'id': workload, 'name': retained.get('name') or workload,
                                   'attempts': 0, 'positives': 0, 'unknowns': 0, 'score': 0}
    count = len(rows)
    if not rows:
        count = raw.get('globalAttempts', 0)
        total = raw.get('cumulativeScore', 0)
        positives = raw.get('positiveDiscoveries', raw.get('positives', 0))
        unknowns = raw.get('unknowns', 0)
    result = {'id': identifier(path), 'name': job.get('name') or (path.parent.name if path.name == 'results.json' else path.stem.replace('.json', '')),
              'path': str(path), 'collection': 'Workbench' if job else path.parent.parent.name if path.name == 'results.json' else '/'.join(path.parts[-4:-2]),
              'method': raw.get('policy') or raw.get('arm') or raw.get('strategy', 'unknown'),
              'mode': mode, 'seed': raw.get('seed'), 'budget': raw.get('globalBudget', raw.get('budget')),
              'attempts': count, 'positives': positives, 'unknowns': unknowns, 'score': total,
              'stop': raw.get('stopReason', 'RETAINED'), 'updated': path.stat().st_mtime,
              'fitness': fitness, 'comparisonKey': comparison, 'workloadCount': len(workloads)}
    if detail:
        result.update(trajectory=trajectory, attemptsDetail=attempts,
                      workloads=sorted(workloads.values(), key=lambda w: (-w['attempts'], w['id'])),
                      categories=categories, configuration=config,
                      provenance={k: v for k, v in raw.items() if k not in ('attempts', 'decisions', 'perWorkload')})
    return result


class Catalog:
    def __init__(self, repo, roots=None):
        self.repo = Path(repo).resolve()
        self.roots = [Path(p).resolve() for p in (roots or [self.repo / 'verifiers/target'])]
        self.runs = {}
        self.configs = {}
        self.cache = {}
        self.scanning = False
        self.scanned_at = None
        self.errors = []
        self.lock = threading.RLock()

    def start_scan(self):
        with self.lock:
            if self.scanning:
                return
            self.scanning = True
        threading.Thread(target=self.scan, daemon=True).start()

    def scan(self):
        runs, configs, errors = {}, {}, []
        try:
            for root in self.roots:
                for base, dirs, files in os.walk(root, followlinks=False):
                    p = Path(base)
                    depth = len(p.relative_to(root).parts)
                    dirs[:] = sorted(d for d in dirs if depth < 7 and not d.startswith(('.', 'attempt-', 'request-', 'package'))
                                     and d not in ('attempts', 'classes', 'test-classes', 'prepared-build', 'node_modules',
                                                   'generated-sources', 'surefire-reports', 'maven-status'))
                    for name in sorted(files):
                        path = p / name
                        is_result = name == 'results.json' or (name.startswith('seed-') and name.endswith(('.json', '.json.gz')))
                        is_config = (name.endswith('.json') and ('config' in name or name.startswith('live-'))
                                     and not path.is_relative_to(self.repo / 'verifiers/target/webui/jobs'))
                        if not (is_result or is_config) or path.is_symlink():
                            continue
                        try:
                            stat = path.stat()
                            key = (str(path), stat.st_mtime_ns, stat.st_size)
                            if key in self.cache:
                                kind, item = self.cache[key]
                                if kind == 'config' and item:
                                    item = self.config_summary(path, read(path))
                            else:
                                raw = read(path)
                                if not isinstance(raw, dict):
                                    continue
                                item = normalize(path, raw) if is_result else None
                                kind = 'run'
                                if item is None and raw.get('schemaVersion') in SCHEMAS:
                                    kind, item = 'config', self.config_summary(path, raw)
                                self.cache[key] = kind, item
                            if item:
                                (runs if kind == 'run' else configs)[item['id']] = item
                        except (OSError, ValueError, TypeError, KeyError) as error:
                            errors.append({'path': str(path), 'error': str(error)})
            with self.lock:
                self.runs, self.configs = runs, configs
                self.errors = errors
                self.scanned_at = time.time()
        finally:
            self.scanning = False

    def config_summary(self, path, raw):
        workloads = raw.get('workloads')
        if not isinstance(workloads, list) or not workloads:
            raise ValueError('Configuration has no workloads')
        missing = []
        for row in workloads:
            for field in ('config', 'catalogue', 'control' if SCHEMAS[raw['schemaVersion']] == 'live' else 'reference'):
                ref = Path(row.get(field, '__missing__'))
                if not (path.parent / ref).exists():
                    missing.append(str(ref))
        return {'id': identifier(path), 'name': str(path.relative_to(self.repo)) if path.is_relative_to(self.repo) else str(path),
                'path': str(path), 'mode': SCHEMAS[raw['schemaVersion']], 'policy': raw.get('policy'),
                'budget': raw.get('budget', 60), 'seed': raw.get('seed', 1), 'fitness': raw.get('fitness'),
                'workloads': [row.get('name', Path(row.get('config', '')).parent.name) for row in workloads],
                'missing': missing, 'ready': not missing}

    def snapshot(self):
        with self.lock:
            return {'runs': sorted(self.runs.values(), key=lambda r: -r['updated']),
                    'configs': sorted(self.configs.values(), key=lambda r: (not r['ready'], r['name'])),
                    'scanning': self.scanning, 'scannedAt': self.scanned_at, 'errors': self.errors[:20],
                    'errorCount': len(self.errors)}

    def detail(self, run_id):
        with self.lock:
            row = self.runs.get(run_id)
        if not row:
            raise ValueError('Unknown experiment')
        path = Path(row['path'])
        return normalize(path, read(path), detail=True)
