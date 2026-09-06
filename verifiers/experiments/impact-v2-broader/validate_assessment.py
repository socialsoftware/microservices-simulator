"""Report contract checks shared by both qualification cohorts; no expected scores."""
CATEGORIES = ('DELETED_DEPENDENCY', 'FAILED_OPERATION_RESIDUAL', 'UNRESOLVED_DELIVERED_EVENT')
STATUSES = ('COMPLETE', 'PARTIAL', 'INVALID', 'UNAVAILABLE')


def require(condition, message):
    if not condition:
        raise ValueError(message)


def integer(value):
    return type(value) is int and value >= 0


def identity(value):
    require(isinstance(value, dict), 'Missing aggregate identity')
    require(isinstance(value.get('aggregateType'), str) and value['aggregateType'], 'Missing aggregate type')
    require(type(value.get('aggregateId')) is int, 'Missing aggregate ID')
    return value['aggregateType'], value['aggregateId']


def validate_reports(execution, v1, v2, workload_id, scenario_id, vector):
    require(execution.get('executionAttemptId'), 'Missing attempt ID')
    require(execution.get('workloadPlanId') == workload_id, 'Selected workload mismatch')
    require(execution.get('faultScenarioId') == scenario_id, 'Selected scenario mismatch')
    require(execution.get('assignedVector') == vector, 'Selected vector mismatch')
    for report in (v1, v2):
        for field in ('executionAttemptId', 'workloadPlanId', 'faultScenarioId'):
            require(report.get(field) == execution.get(field), f'Report join mismatch: {field}')
    require(v2.get('schemaVersion') == 'microservices-simulator.scenario-impact-v2-assessment.v1', 'Wrong V2 schema')
    require(v2.get('packageManifestPath') == execution.get('packageManifestPath'), 'Package identity mismatch')
    require(v2.get('executionTerminalStatus') == execution.get('terminalStatus'), 'Terminal status mismatch')
    require(v2.get('scheduleConformance') == execution.get('scheduleConformance'), 'Conformance mismatch')
    for field in ('completeScore', 'observedAffectedObjectCount'):
        require(field in v2, f'Missing nullable field {field}')
    status = v2.get('assessmentStatus')
    require(status in STATUSES, 'Unknown assessment status')
    categories = v2.get('categoryResults')
    require(isinstance(categories, list) and [c.get('category') for c in categories] == list(CATEGORIES), 'Invalid category shape/order')
    affected = set()
    for category in categories:
        require(integer(category.get('candidateCount')), 'Invalid candidate count')
        require(isinstance(category.get('candidates'), list) and len(category['candidates']) == category['candidateCount'], 'Candidate count mismatch')
        require(integer(category.get('positiveObjectCount')), 'Invalid positive count')
        require(isinstance(category.get('findings'), list), 'Missing findings')
        require(isinstance(category.get('unknownReasons'), list), 'Missing unknown reasons')
        candidates = {identity(c['aggregate']) for c in category['candidates']}
        positive = set()
        for finding in category['findings']:
            require(finding.get('category') == category['category'], 'Finding category mismatch')
            positive.add(identity(finding.get('affectedObject')))
        require(positive <= candidates, 'Finding outside candidate set')
        require(len(positive) == category['positiveObjectCount'], 'Category distinct-object count mismatch')
        affected |= positive
        if status == 'COMPLETE':
            require(category.get('coverageStatus') == 'COMPLETE' and not category['unknownReasons'], 'Incomplete category marked complete')
    if status in ('COMPLETE', 'PARTIAL'):
        require(integer(v2['observedAffectedObjectCount']) and v2['observedAffectedObjectCount'] == len(affected), 'Observed distinct-object union mismatch')
        require(v2.get('horizon') == 'FINAL_SCHEDULED_ACTION', 'Valid score without final horizon')
        require(execution.get('terminalStatus') in ('SUCCESS', 'COMPENSATED', 'PARTIAL_COMPENSATED'), 'Valid score on incomplete execution')
    else:
        require(v2['observedAffectedObjectCount'] is None, 'Invalid/unavailable must have null observed count')
    if status == 'COMPLETE':
        require(integer(v2['completeScore']) and v2['completeScore'] == len(affected), 'Complete score mismatch')
        require(v2.get('collectionStatus') == 'OBSERVED' and v2.get('coverageGaps') == [], 'Complete assessment with collection gaps')
    else:
        require(v2['completeScore'] is None, 'Non-complete must have null score')
    return status
