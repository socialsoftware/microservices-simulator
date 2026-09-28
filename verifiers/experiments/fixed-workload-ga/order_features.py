"""Outcome-free ordinary-order features for recorded allocator experiments.

The extractor is deliberately separate from ``allocator.py``.  A caller must opt in,
record coverage, and inject the augmented profiles into an experimental FeatureSpace.
"""
from copy import deepcopy
import hashlib
import json
import math
from pathlib import Path


SCHEMA = 'allocator-order-features.v1'
SUPPORTED_ACCESS_MODES = frozenset(('read', 'write'))
ORDER_FEATURES = (
    'order:type-potential:read-before-write',
    'order:type-potential:write-before-read',
    'order:type-potential:write-before-write',
    'order:type-potential:read-between-foreign-writes',
    'order:event-type-potential:read-before-write',
    'order:event-type-potential:write-before-read',
    'order:event-type-potential:write-before-write',
)


class OrderFeatureCoverageError(ValueError):
    """Raised when strict extraction encounters incomplete access provenance."""

    def __init__(self, profile):
        self.profile = profile
        gaps = profile.get('coverage', {}).get('gaps', [])
        super().__init__(f'Order feature access coverage is {profile["coverage"]["status"]}: '
                         f'{len(gaps)} gap(s)')


def _sha256(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def _json_lines(path):
    rows = []
    for number, line in enumerate(Path(path).read_text().splitlines(), 1):
        if line.strip():
            value = json.loads(line)
            if not isinstance(value, dict):
                raise ValueError(f'JSONL row {number} is not an object: {path}')
            rows.append(value)
    return rows


def _manifest_file(package_directory, manifest, role):
    descriptor = manifest.get('files', {}).get(role)
    if not isinstance(descriptor, dict) or set(descriptor) != {'path', 'sha256'} \
            or not all(isinstance(descriptor.get(name), str) and descriptor[name]
                       for name in ('path', 'sha256')):
        raise ValueError(f'Package manifest lacks a complete {role} descriptor')
    path = (package_directory / descriptor['path']).resolve()
    if path.parent != package_directory.resolve():
        raise ValueError(f'Package {role} path leaves its package directory')
    if _sha256(path) != descriptor['sha256']:
        raise ValueError(f'Package {role} hash mismatch')
    return path, descriptor['sha256']


def load_compatible_structural_metadata(interaction_files):
    """Load Saga/access records co-generated with pinned interaction files.

    Each interaction file must be the interactions role of an adjacent package
    manifest.  All supplied packages must declare the same Saga and interaction
    hashes.  The caller still needs to pin the manifest/Saga hashes in its frozen
    experiment inputs: an adjacent manifest proves co-generation compatibility,
    but is not an independent trust anchor.
    """
    paths = [Path(path).resolve() for path in interaction_files]
    if not paths:
        raise ValueError('At least one interaction file is required')
    sources = []
    for interaction_path in paths:
        manifest_path = interaction_path.with_name('scenario-catalog-manifest.json')
        manifest = json.loads(manifest_path.read_text())
        if manifest.get('formatVersion') != 1:
            raise ValueError('Unsupported scenario catalogue manifest')
        declared_interactions, interaction_sha = _manifest_file(
            interaction_path.parent, manifest, 'interactions')
        saga_path, saga_sha = _manifest_file(interaction_path.parent, manifest, 'sagas')
        if declared_interactions != interaction_path:
            raise ValueError('Input is not the package-declared interaction file')
        sources.append({'manifest': str(manifest_path), 'sagas': str(saga_path),
                        'interactions': str(interaction_path), 'manifestSha256': _sha256(manifest_path),
                        'sagasSha256': saga_sha, 'interactionsSha256': interaction_sha})
    fingerprints = {(row['sagasSha256'], row['interactionsSha256']) for row in sources}
    if len(fingerprints) != 1:
        raise ValueError('Structural metadata comes from incompatible package generations')

    sagas = _json_lines(sources[0]['sagas'])
    interactions = _json_lines(sources[0]['interactions'])
    return {'sagas': sagas, 'interactions': interactions,
            'source': {'compatibility': 'EXACT_PACKAGE_ROLE_HASHES',
                       'sagasSha256': sources[0]['sagasSha256'],
                       'interactionsSha256': sources[0]['interactionsSha256'],
                       'packages': sources}}


def _unique_by(rows, field, label):
    if not isinstance(rows, list):
        raise ValueError(f'{label} records must be an array')
    result = {}
    for row in rows:
        identity = row.get(field) if isinstance(row, dict) else None
        if not isinstance(identity, str) or not identity or identity in result:
            raise ValueError(f'{label} identities must be unique nonempty strings')
        result[identity] = row
    return result


def _gap(kind, **details):
    return {'kind': kind, **details}


def _step_accesses(saga, step_id, gaps, limitations, *, subject):
    steps = saga.get('steps')
    if not isinstance(steps, list):
        gaps.append(_gap('MISSING_SAGA_STEPS', subject=subject, saga=saga.get('fqn')))
        return None, None
    matches = [step for step in steps if isinstance(step, dict) and step.get('id') == step_id]
    if len(matches) != 1:
        gaps.append(_gap('STEP_PROVENANCE_NOT_UNIQUE', subject=subject,
                         saga=saga.get('fqn'), sagaStep=step_id, matches=len(matches)))
        return None, None
    step = matches[0]
    recorded_limitations = step.get('analysisLimitations')
    if not isinstance(recorded_limitations, list):
        gaps.append(_gap('MISSING_ANALYSIS_LIMITATIONS', subject=subject,
                         saga=saga.get('fqn'), sagaStep=step_id))
        recorded_limitations = []
    for limitation in recorded_limitations:
        limitations.append({'kind': 'STEP_ANALYSIS_LIMITATION', 'subject': subject,
                            'saga': saga.get('fqn'), 'sagaStep': step_id,
                            'limitation': str(limitation)})
    accesses = step.get('commandAccesses')
    if not isinstance(accesses, list):
        gaps.append(_gap('MISSING_COMMAND_ACCESSES', subject=subject,
                         saga=saga.get('fqn'), sagaStep=step_id))
        return step, None
    parsed = []
    for index, access in enumerate(accesses):
        aggregate = access.get('aggregate') if isinstance(access, dict) else None
        name = aggregate.get('name') if isinstance(aggregate, dict) else None
        mode = aggregate.get('mode') if isinstance(aggregate, dict) else None
        if not isinstance(name, str) or not name or mode not in SUPPORTED_ACCESS_MODES:
            gaps.append(_gap('INVALID_COMMAND_ACCESS', subject=subject,
                             saga=saga.get('fqn'), sagaStep=step_id, accessIndex=index))
            continue
        parsed.append({'aggregate': name, 'mode': mode,
                       'keyEvidence': aggregate.get('keyEvidence')})
    return step, parsed


def _coverage_status(gaps, valid_feature_sources):
    if not gaps:
        return 'COMPLETE'
    return 'PARTIAL' if valid_feature_sources else 'UNAVAILABLE'


def order_profile(workload, structural_metadata, *, strict=True):
    """Extract finite type-level access-order features from one WorkloadPlan.

    Same aggregate *type* creates only a potential pair.  No feature asserts that
    two accesses address the same object or that an order causes an anomaly.
    """
    if not isinstance(workload, dict) or not isinstance(structural_metadata, dict):
        raise ValueError('Workload and structural metadata must be objects')
    workload_id = workload.get('id')
    if not isinstance(workload_id, str) or not workload_id:
        raise ValueError('Workload identity must be a nonempty string')
    sagas = _unique_by(structural_metadata.get('sagas'), 'fqn', 'Saga')
    participants = _unique_by(workload.get('participants'), 'id', 'Participant')
    schedule = workload.get('schedule')
    if not isinstance(schedule, list) or not schedule:
        raise ValueError('Workload schedule must be a nonempty array')
    schedule_ids = _unique_by(schedule, 'id', 'Schedule')
    if len(schedule_ids) != len(schedule):
        raise AssertionError('unreachable duplicate schedule validation')

    gaps, limitations = [], []
    ordinary_accesses = []
    for position, entry in enumerate(schedule):
        kind = entry.get('kind')
        if kind not in ('step', 'event'):
            gaps.append(_gap('UNSUPPORTED_SCHEDULE_KIND', scheduleEntry=entry['id'], value=kind))
            continue
        if kind == 'event':
            continue
        participant = participants.get(entry.get('participant'))
        if participant is None:
            gaps.append(_gap('UNKNOWN_SCHEDULE_PARTICIPANT', scheduleEntry=entry['id']))
            continue
        saga_fqn, step_id = participant.get('saga'), entry.get('sagaStep')
        saga = sagas.get(saga_fqn)
        if saga is None:
            gaps.append(_gap('MISSING_SAGA_RECORD', scheduleEntry=entry['id'], saga=saga_fqn))
            continue
        _, accesses = _step_accesses(saga, step_id, gaps, limitations,
                                     subject='scheduled-step')
        if accesses is not None:
            # A command list can repeat the same analyzer footprint.  The schedule
            # orders steps, not commands inside a step, so retain one mode/type fact.
            unique = {(access['aggregate'], access['mode']) for access in accesses}
            ordinary_accesses.extend({'position': position, 'entry': entry['id'],
                                      'participant': entry['participant'],
                                      'aggregate': aggregate, 'mode': mode}
                                     for aggregate, mode in sorted(unique))

    raw = {name: 0 for name in ORDER_FEATURES}
    for left_index, left in enumerate(ordinary_accesses):
        for right in ordinary_accesses[left_index + 1:]:
            if left['position'] >= right['position'] \
                    or left['participant'] == right['participant'] \
                    or left['aggregate'] != right['aggregate']:
                continue
            pattern = left['mode'] + '-before-' + right['mode']
            name = 'order:type-potential:' + pattern
            if name in raw:
                raw[name] += 1

    reads = [row for row in ordinary_accesses if row['mode'] == 'read']
    writes = [row for row in ordinary_accesses if row['mode'] == 'write']
    for read in reads:
        before = [row for row in writes if row['aggregate'] == read['aggregate']
                  and row['participant'] != read['participant']
                  and row['position'] < read['position']]
        after = [row for row in writes if row['aggregate'] == read['aggregate']
                 and row['participant'] != read['participant']
                 and row['position'] > read['position']]
        raw['order:type-potential:read-between-foreign-writes'] += len(before) * len(after)

    mapped_events = 0
    for position, entry in enumerate(schedule):
        if entry.get('kind') != 'event':
            continue
        route_id, trigger_id = entry.get('route'), entry.get('triggeringStep')
        trigger = schedule_ids.get(trigger_id)
        if trigger is None or trigger.get('kind') != 'step':
            gaps.append(_gap('EVENT_TRIGGER_STEP_NOT_FOUND', scheduleEntry=entry['id']))
            continue
        participant = participants.get(trigger.get('participant'))
        producer = sagas.get(participant.get('saga')) if participant else None
        if producer is None:
            gaps.append(_gap('EVENT_PRODUCER_SAGA_NOT_FOUND', scheduleEntry=entry['id']))
            continue
        producer_steps = [step for step in producer.get('steps', [])
                          if isinstance(step, dict) and step.get('id') == trigger.get('sagaStep')]
        routes = [route for step in producer_steps for route in step.get('eventRoutes', [])
                  if isinstance(route, dict) and route.get('id') == route_id]
        if len(producer_steps) != 1 or len(routes) != 1:
            gaps.append(_gap('EVENT_ROUTE_PROVENANCE_NOT_UNIQUE', scheduleEntry=entry['id'],
                             route=route_id, matches=len(routes)))
            continue
        downstream = sagas.get(routes[0].get('downstreamSaga'))
        if downstream is None:
            gaps.append(_gap('EVENT_RECEIVER_SAGA_NOT_FOUND', scheduleEntry=entry['id'],
                             route=route_id))
            continue
        receiver_modes = set()
        steps = downstream.get('steps')
        if not isinstance(steps, list):
            gaps.append(_gap('MISSING_SAGA_STEPS', subject='event-receiver',
                             saga=downstream.get('fqn')))
            continue
        for step in steps:
            _, accesses = _step_accesses(downstream, step.get('id'), gaps, limitations,
                                         subject='event-receiver')
            if accesses is not None:
                receiver_modes.update((access['aggregate'], access['mode'])
                                      for access in accesses)
        if not receiver_modes:
            limitations.append(_gap('NO_RECORDED_EVENT_RECEIVER_ACCESS',
                                    scheduleEntry=entry['id'], route=route_id,
                                    saga=downstream.get('fqn')))
            continue
        mapped_events += 1
        for aggregate, mode in receiver_modes:
            for ordinary in ordinary_accesses:
                if aggregate != ordinary['aggregate'] or position == ordinary['position']:
                    continue
                first, second = ((mode, ordinary['mode']) if position < ordinary['position']
                                 else (ordinary['mode'], mode))
                name = 'order:event-type-potential:' + first + '-before-' + second
                if name in raw:
                    raw[name] += 1

    features = {name: value / (1.0 + value) for name, value in raw.items()}
    valid_sources = len(ordinary_accesses) + mapped_events
    status = _coverage_status(gaps, valid_sources)
    static_knowledge = 'PARTIAL' if limitations else 'COMPLETE_WITHIN_EXTRACTED_SCOPE'
    profile = {
        'schema': SCHEMA,
        'workload': workload_id,
        'counts': {'scheduledAccesses': len(ordinary_accesses),
                   'rawPatterns': raw,
                   'scheduledEvents': sum(entry.get('kind') == 'event' for entry in schedule),
                   'mappedEvents': mapped_events},
        'features': features,
        'coverage': {'status': status, 'staticKnowledge': static_knowledge,
                     'gaps': sorted(gaps, key=lambda row: json.dumps(row, sort_keys=True)),
                     'limitations': sorted(limitations,
                                           key=lambda row: json.dumps(row, sort_keys=True)),
                     'outcomeInputs': False,
                     'semantics': 'aggregate-type-potential-only',
                     'objectIdentityClaim': False,
                     'anomalyClaim': False,
                     'absenceMeaning': 'no-recorded-access-not-proved-absence'}
    }
    if strict and gaps:
        raise OrderFeatureCoverageError(profile)
    return profile


def augment_profile(structural_profile, order, *, allow_partial=False):
    """Add extracted normalized counts to an allocator structural profile."""
    if not isinstance(structural_profile, dict) or not isinstance(order, dict) \
            or order.get('schema') != SCHEMA:
        raise ValueError('Invalid structural/order profile')
    coverage = order.get('coverage', {})
    if coverage.get('gaps') and not allow_partial:
        raise OrderFeatureCoverageError(order)
    result = deepcopy(structural_profile)
    if 'orderFeatures' in result or set(order.get('features', {})) != set(ORDER_FEATURES):
        raise ValueError('Structural profile has incompatible order features')
    result['orderFeatures'] = dict(order['features'])
    return result


def augment_profiles(structural_profiles, order_profiles, *, allow_partial=False):
    """Augment a complete, identity-matched profile mapping deterministically."""
    if set(structural_profiles) != set(order_profiles):
        raise ValueError('Structural and order profile identities differ')
    return {workload_id: augment_profile(structural_profiles[workload_id],
                                         order_profiles[workload_id],
                                         allow_partial=allow_partial)
            for workload_id in sorted(structural_profiles)}


class OrderFeatureSpace:
    """Existing FeatureSpace plus seven bounded type-level order coordinates."""

    def __init__(self, profiles):
        from allocator import FeatureSpace
        self.base = FeatureSpace(profiles)
        self.names = [*self.base.names, *ORDER_FEATURES]

    def vector(self, profile, progress):
        features = profile.get('orderFeatures') if isinstance(profile, dict) else None
        if not isinstance(features, dict) or set(features) != set(ORDER_FEATURES):
            raise ValueError('Profile lacks the complete order feature vector')
        values = [features[name] for name in ORDER_FEATURES]
        if any(type(value) not in (int, float) or not math.isfinite(value)
               or value < 0.0 or value >= 1.0 for value in values):
            raise ValueError('Order features must be finite normalized counts')
        return [*self.base.vector(profile, progress), *map(float, values)]
