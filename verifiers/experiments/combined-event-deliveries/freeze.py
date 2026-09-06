#!/usr/bin/env python3
"""Freeze a small source-fixture comparison before observing runtime outcomes."""
import argparse
import hashlib
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'impact-v2-broader'))
from qualification import package

SAGA = 'pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.RemoveCourseExecutionFunctionalitySagas'
FIXTURE = 'pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.answer.RemoveCourseExecutionQuizAnswerReceiverTest'


def select(manifest, output):
    data = package(manifest)
    inputs = [i for i in data['records']['inputs'] if i['saga'] == SAGA
              and i['source']['testClass'] == FIXTURE
              and i['source']['testRole'] == 'featureUnderTest']
    if len(inputs) != 1:
        raise ValueError('Expected exactly one declared removal input')
    saga = next(s for s in data['records']['sagas'] if s['fqn'] == SAGA)
    routes = {r['id']: r['eventHandlingClass'].split('.')[-1]
              for step in saga['steps'] for r in step['eventRoutes']}
    labels = {'QuizAnswerEventHandling': 'answer', 'QuizEventHandling': 'quiz',
              'TournamentEventHandling': 'tournament'}
    cases = [('none', (), '000'), ('answer', ('answer',), '000'),
             ('quiz', ('quiz',), '000'), ('answer-quiz', ('answer', 'quiz'), '000'),
             ('quiz-answer', ('quiz', 'answer'), '000'),
             ('answer-quiz-trigger-fault', ('answer', 'quiz'), '001'),
             ('quiz-answer-trigger-fault', ('quiz', 'answer'), '001'),
             ('answer-quiz-missing-tournament', ('answer', 'quiz', 'tournament'), '000')]
    rows = []
    for name, order, vector in cases:
        candidates = []
        for workload in data['records']['workloads']:
            if len(workload['participants']) != 1 or workload['participants'][0]['input'] != inputs[0]['id']:
                continue
            actual = tuple(labels[routes[a['route']]] for a in workload['schedule'] if a['kind'] == 'event')
            if actual == order:
                candidates.append(workload)
        if len(candidates) != 1:
            raise ValueError(f'{name}: expected one generated workload, found {len(candidates)}')
        workload = candidates[0]
        scenarios = [s for s in data['records']['faultScenarios']
                     if s['workload'] == workload['id'] and s['faultVector'] == vector]
        if not scenarios:
            raise ValueError(f'{name}: missing generated vector {vector}')
        # Recovery variants may differ; retain a deterministic exact persisted variant.
        scenario = min(scenarios, key=lambda s: s['id'])
        rows.append({'cohort': 'broader', 'caseId': name, 'role': 'control' if vector == '000' else 'trigger-fault',
                     'saga': SAGA, 'inputId': inputs[0]['id'], 'workloadId': workload['id'],
                     'faultScenarioId': scenario['id'], 'faultVector': vector,
                     'deliveryOrder': list(order), 'setupId': workload['setup']})
    if len({row['setupId'] for row in rows}) != 1:
        raise ValueError('Comparisons must use the same setup')
    output.mkdir(parents=True, exist_ok=False)
    common = {'schema': 'combined-event-deliveries-selection.v1',
              'packageManifestSha256': data['hashes']['manifest'],
              'selectionRule': 'Exact source fixture and route orders; no filtering by observed outcomes.'}
    for filename, selected in [('benchmark-selection.json', []), ('broader-selection.json', rows)]:
        (output / filename).write_text(json.dumps({**common, 'rows': selected}, indent=2) + '\n')
    print(f'Frozen {len(rows)} cases from one source input/setup.')


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--manifest', type=Path, required=True)
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    select(args.manifest, args.output)
