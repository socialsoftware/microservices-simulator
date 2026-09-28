"""Compare the UCB addendum with frozen formal results and classify missing feedback."""
from collections import Counter, defaultdict
import gzip
import json
from pathlib import Path
from statistics import mean, stdev

import ucb_addendum as ucb
from fitness import assess, components

ROOT = Path(__file__).resolve().parents[3]
EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/workload-master-inventory-2026-09-25'


def stats(xs):
    return {'mean': mean(xs), 'sd': stdev(xs) if len(xs) > 1 else 0,
            'min': min(xs), 'max': max(xs)}


def diagnosis(inventory, giants):
    rows = []
    for w in inventory['qualifiedWorkloads']:
        path = ROOT / w['reference']
        if ucb.run.sha(path) != w['referenceSha256']:
            raise ValueError('Reference changed: ' + str(path))
        for key, observation in ucb.run.read(path)['observations'].items():
            assessment = assess(observation, ucb.formal.fitness('all-five'))
            if assessment['fitnessScore'] is not None:
                continue
            missing = [c for c, v in assessment['fitnessComponents'].items() if v['count'] is None]
            gaps = sorted({g.get('message', g.get('reason', '?'))
                           for g in observation.get('lostCopiedUpdateCoverageGaps', [])})
            if 'EXECUTION_OR_ASSESSMENT_UNAVAILABLE' in assessment['fitnessUnavailableReasons']:
                group = 'EXECUTION_UNAVAILABLE'
            elif missing == ['LOST_COPIED_UPDATE']:
                group = 'COPY_PROVENANCE'
            elif missing == ['FAILED_OPERATION_RESIDUAL']:
                group = 'RESIDUAL_ATTRIBUTION'
            elif missing == ['COMPENSATED_READ_EXPOSURE']:
                group = 'READ_ATTRIBUTION'
            elif set(missing) == {'FAILED_OPERATION_RESIDUAL', 'COMPENSATED_READ_EXPOSURE'}:
                group = 'RESIDUAL_AND_READ_ATTRIBUTION'
            else:
                raise ValueError('Unclassified missing feedback')
            available = {c: v['count'] for c, v in assessment['fitnessComponents'].items()
                         if v['count'] is not None}
            rows.append({'workload': w['id'], 'family': w['family'], 'candidate': key,
                'reference': w['reference'], 'referenceSha256': w['referenceSha256'],
                'group': group, 'status': observation.get('status'),
                'missingCriteria': missing, 'lostGaps': gaps, 'availableCounts': available,
                'readGaps': observation.get('AGaps', []),
                'residualReasons': [g for c in observation.get('impactCategories', [])
                    if c['category'] == 'FAILED_OPERATION_RESIDUAL' for g in c.get('unknownReasons', [])]})
    cohorts = {}
    for name, subset, size in [('complete', rows, 18858),
                              ('without-three-giants', [r for r in rows if r['workload'] not in giants], 6396)]:
        copies = [r for r in subset if r['group'] == 'COPY_PROVENANCE']
        subgroups = Counter()
        for r in copies:
            if 'UNSUPPORTED_CONCURRENT_THREAD' in r['lostGaps']:
                subgroups['CONCURRENT_THREAD_WITH_POSSIBLE_OTHER_GAPS'] += 1
            elif r['lostGaps'] and all(g.startswith('MISSING_COMMAND_SCOPE:') for g in r['lostGaps']):
                subgroups['COMMAND_SCOPE_ONLY'] += 1
            elif r['lostGaps'] == ['IllegalStateException: Duplicate collection identity']:
                subgroups['DUPLICATE_COLLECTION_IDENTITY'] += 1
            else:
                raise ValueError('Unclassified lost-copy provenance gap')
        cohorts[name] = {'scenarios': size, 'jointUnavailable': len(subset),
            'disjointReasons': Counter(r['group'] for r in subset),
            'executionStatuses': Counter(r['status'] for r in subset if r['group'] == 'EXECUTION_UNAVAILABLE'),
            'copyProvenanceSubgroups': subgroups,
            'knownPositiveInAvailableCriteria': sum(any(v > 0 for v in r['availableCounts'].values()) for r in subset)}
    paths = [
        'simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/monitoring/copiedupdate/CopiedUpdateSession.java',
        'simulator/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/messaging/local/LocalCommandGateway.java',
        'applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/question/coordination/sagas/CreateQuestionFunctionalitySagas.java',
        'applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/quiz/coordination/sagas/CreateQuizFunctionalitySagas.java',
        'applications/quizzes/src/main/java/pt/ulisboa/tecnico/socialsoftware/quizzes/microservices/tournament/coordination/sagas/CreateTournamentAsyncFunctionalitySagas.java',
        'verifiers/src/main/java/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/SagaReadExposureAssessor.java']
    return {'scope': 'Retained observations and current source only; no remeasurement or score changes.',
        'sourceHashes': {p: ucb.run.sha(ROOT / p) for p in paths},
        'findings': {
            'commandScope': 'The gateway closes observation before returning. CreateQuestion then constructs QuestionCourse from the returned CourseDto; CreateQuiz similarly constructs QuizCourseExecution after send. CopiedUpdateSession rejects these supported constructor types when no command is open. The 274 scope-only cases include several families; these source paths demonstrate the limitation, not a runtime reconstruction of every occurrence. Historical gap records lack detailed context.',
            'concurrent': '239 observations report unsupported concurrent threads, sometimes with additional scope or input-origin gaps. The current observer rejects non-owner-thread hooks. Supporting concurrency alone is not proven to resolve all these cases.',
            'recovery': 'Skipped constructor/provenance rows cannot be restored by merely changing a score. A generic extension needs object origin, transport and persisted-placement proof plus fresh representative executions. Duplicate identities require an unambiguous occurrence contract; do not silently map them to zero.',
            'otherCriteria': 'Residual attribution remains incomplete with competing field/lifecycle writers; read attribution reports intervening writers. These require separate proof extensions, not the lost-copy fix.',
            'executionFailures': '471 retained attempts are classified as process failure, timeout or invalid report. This classification does not establish that every process failure is infrastructure-related; individual root causes require retained runtime reports/logs.',
            'benchmark': 'The replay measures discovery under the frozen detector, including its missing feedback. Available criterion positives in otherwise unavailable joint scores are retained, never supplied as complete joint rewards.'},
        'cohorts': cohorts, 'rows': rows}


def main():
    baseline = ucb.run.read(EVIDENCE / 'formal-replay-2026-09-28.json')
    inventory = ucb.run.read(EVIDENCE / 'master-inventory-2026-09-28.json')
    metadata = {w['id']: w for w in inventory['qualifiedWorkloads']}
    giants = {w['id'] for w in sorted(metadata.values(), key=lambda w: (-w['scenarios'], w['id']))[:3]}
    result = {'baselineSummarySha256': ucb.run.sha(EVIDENCE / 'formal-replay-2026-09-28.json'),
              'newRuns': 60, 'reusedBaselineRuns': 420, 'cohorts': {}}
    for cohort, short in [('complete', 'full'), ('without-three-giants', 'small')]:
        directory = ROOT / f'verifiers/target/allocator-ucb-{short}-2026-09-28'
        addendum = ucb.run.read(directory / 'results.json')
        old = baseline['cohorts'][cohort]
        rows, group = [], defaultdict(list)
        for r in addendum['rows']:
            if ucb.run.sha(r['path']) != r['traceSha256']:
                raise ValueError('UCB trace changed')
            v = ucb.read_trace(r['path'])
            selected = [w for w in v['perWorkload'] if w['allocations']]
            positive = [w for w in selected if w['positiveDiscoveries']]
            streak = maximum_streak = 0
            for d in v['decisions']:
                streak = streak + 1 if d['score'] is None else 0
                maximum_streak = max(maximum_streak, streak)
            row = {**r, 'path': str(Path(r['path']).relative_to(ROOT)),
                'positiveFamilies': len({metadata[w['workload']]['family'] for w in positive}),
                'selectedWorkloads': len(selected), 'positiveWorkloads': len(positive),
                'negativeChoices': v['attempts'] - v['positives'] - v['unknowns'],
                'giantChoices': sum(w['allocations'] for w in selected if w['workload'] in giants),
                'giantScore': sum(w['cumulativeScore'] for w in selected if w['workload'] in giants),
                'maximumUnknownStreak': maximum_streak, 'checkpoints': v['checkpoints']}
            rows.append(row)
            group[r['profile']].append(row)
        summaries, paired = [], []
        for profile, rr in group.items():
            metrics = ('score', 'positives', 'unknowns', 'negativeChoices', 'positiveFamilies',
                       'selectedWorkloads', 'positiveWorkloads', 'giantChoices', 'giantScore', 'maximumUnknownStreak')
            summaries.append({'profile': profile, 'arm': ucb.ARM,
                **{key: stats([r[key] for r in rr]) for key in metrics},
                'checkpoints': {str(n): {key: stats([next(c[field] for c in r['checkpoints'] if c['attempt'] == n)
                                                    for r in rr]) for key, field in
                                         [('score', 'cumulativeScore'), ('positives', 'positives')]}
                                for n in (1000, 2000, 4000, 8000) if n <= addendum['protocol']['budget']}})
            for arm in old['protocol']['arms']:
                other = {r['seed']: r for r in old['seedRows'] if r['profile'] == profile and r['arm'] == arm}
                paired.append({'profile': profile, 'arm': ucb.ARM, 'vs': arm,
                    'scoreDifference': stats([r['score'] - other[r['seed']]['score'] for r in rr]),
                    'scoreWinsTiesLosses': [sum(r['score'] > other[r['seed']]['score'] for r in rr),
                        sum(r['score'] == other[r['seed']]['score'] for r in rr),
                        sum(r['score'] < other[r['seed']]['score'] for r in rr)]})
        result['cohorts'][cohort] = {'protocol': addendum['protocol'], 'checks': addendum['checks'],
            'population': old['population'], 'summaries': [*old['summaries'], *summaries],
            'ucbPairedDifferences': paired, 'ucbSeedRows': rows}
    diag = diagnosis(inventory, giants)
    ucb.write(EVIDENCE / 'ucb-addendum-2026-09-28.json', result)
    ucb.write(EVIDENCE / 'measurement-diagnosis-2026-09-28.json', diag)
    for cohort, value in result['cohorts'].items():
        print(cohort)
        for row in value['summaries']:
            if row['profile'] == 'all-five':
                print(row['arm'], *(round(row[k]['mean'], 2) for k in ('score','positives','positiveFamilies','unknowns')))


if __name__ == '__main__':
    main()
