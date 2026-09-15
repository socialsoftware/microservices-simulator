"""User-selected search preferences over existing, independently assessed criteria."""
import math

LEGACY = 'complete-impact-v2-object-count'
WEIGHTED = 'weighted-criteria-v1'
WEIGHTED_V2 = 'weighted-criteria-v2'
PERSISTENT = ('DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT')
READ = 'COMPENSATED_READ_EXPOSURE'
LOST_COPIED_UPDATE = 'LOST_COPIED_UPDATE'
CRITERIA = (*PERSISTENT, READ)
CRITERIA_V2 = (*CRITERIA, LOST_COPIED_UPDATE)


def configuration(value=None):
    """Freeze an explicit policy; omitted configuration reproduces historical I fitness."""
    if value is None:
        return {'policy': LEGACY}
    if not isinstance(value, dict):
        raise ValueError('fitness must be an object')
    if value.get('policy') == LEGACY and set(value) == {'policy'}:
        return {'policy': LEGACY}
    policy = value.get('policy')
    if policy not in (WEIGHTED, WEIGHTED_V2) or set(value) != {'policy', 'weights'}:
        raise ValueError('Unknown fitness policy or fields')
    weights = value['weights']
    criteria = CRITERIA if policy == WEIGHTED else CRITERIA_V2
    if not isinstance(weights, dict) or set(weights) != set(criteria):
        raise ValueError(f'Specify all {len(criteria)} criterion weights explicitly; use zero to exclude one')
    for weight in weights.values():
        try:
            valid = type(weight) in (int, float) and math.isfinite(weight) and weight >= 0
        except OverflowError:
            valid = False
        if not valid:
            raise ValueError('Weights must be finite nonnegative numbers')
    if not any(weights.values()):
        raise ValueError('At least one criterion weight must be positive')
    return {'policy': policy, 'weights': {name: weights[name] for name in criteria}}


def count(value):
    return type(value) is int and value >= 0


def components(result, include_lost_copied_update=False):
    """Preserve lower bounds but only expose complete counts to weighted fitness.

    Runtime report validation owns identity joins and within-category deduplication.
    Category coverage already incorporates the collector gaps relevant to that check.
    """
    values = {}
    for name in PERSISTENT:
        matches = [c for c in result.get('impactCategories', []) if c.get('category') == name]
        category = matches[0] if len(matches) == 1 else {}
        observed = category.get('positiveObjectCount')
        available = category.get('coverageStatus') == 'COMPLETE' \
            and category.get('unknownReasons') == [] and count(observed)
        values[name] = {'observedCount': observed, 'count': observed if available else None,
                        'coverage': category.get('coverageStatus', 'UNAVAILABLE')}
    observed = result.get('A')
    available = result.get('AStatus') == 'COMPLETE' \
        and result.get('ACoverage') == 'COMPLETE_WITHIN_SCOPE' \
        and result.get('AGaps') == [] and count(observed)
    values[READ] = {'observedCount': observed, 'count': observed if available else None,
                    'coverage': result.get('ACoverage', 'UNAVAILABLE')}
    if include_lost_copied_update:
        observed = result.get('lostCopiedUpdateCount')
        available = result.get('lostCopiedUpdateValidity') == 'COMPLETE' \
            and result.get('lostCopiedUpdateCoverage') == 'COMPLETE_WITHIN_SCOPE' \
            and result.get('lostCopiedUpdateCoverageGaps') == [] and count(observed)
        values[LOST_COPIED_UPDATE] = {
            'observedCount': observed, 'count': observed if available else None,
            'coverage': result.get('lostCopiedUpdateCoverage', 'UNAVAILABLE')}
    return values


def assess(result, config):
    values = components(result, config['policy'] == WEIGHTED_V2)
    reasons = []
    if config['policy'] == LEGACY:
        score = result.get('I')
        if score is not None and not count(score):
            raise ValueError('I must be a nonnegative integer or null')
        if score is None:
            reasons.append('COMPLETE_I_UNAVAILABLE')
    else:
        # Invalid attempts never become parents, even if a sidecar has positive findings.
        valid = result.get('status') in ('COMPLETE', 'PARTIAL', 'UNAVAILABLE') \
            and result.get('terminalStatus') in ('SUCCESS', 'COMPENSATED', 'PARTIAL_COMPENSATED') \
            and result.get('scheduleConformance') in ('EXACT', 'DEVIATED')
        if not valid:
            reasons.append('EXECUTION_OR_ASSESSMENT_UNAVAILABLE')
        enabled = [name for name, weight in config['weights'].items() if weight > 0]
        reasons.extend(name + '_INCOMPLETE' for name in enabled if values[name]['count'] is None)
        score = None
        if not reasons:
            try:
                score = math.fsum(config['weights'][name] * values[name]['count'] for name in enabled)
                if not math.isfinite(score):
                    raise OverflowError()
            except OverflowError:
                score = None
                reasons.append('WEIGHTED_SCORE_OVERFLOW')
    return {'fitnessScore': score, 'fitnessComponents': values, 'fitnessUnavailableReasons': reasons}
