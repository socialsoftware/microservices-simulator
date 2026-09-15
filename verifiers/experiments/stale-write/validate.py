#!/usr/bin/env python3
"""Validate the declared Quizzes histories; this is not a production anomaly detector."""
import argparse
import hashlib
import json
from pathlib import Path

CASES = ('forward-stale', 'forward-fresh', 'recovery-stale',
         'recovery-delayed-event', 'recovery-no-event')


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def topic(state, topic_id):
    return next(t for t in state['tournament']['topics'] if t['topicId'] == topic_id)


def projected_topic(snapshot, topic_id):
    return next(t for t in snapshot['applicationData']['tournamentTopics']
                if t['topicAggregateId'] == topic_id)


def validate(directory):
    hashes = json.loads((directory / 'artifact-hashes.json').read_text())
    rows = []
    for mode in ('false', 'true'):
        for case in CASES:
            name = case + '-' + mode + '.json'
            path = directory / name
            assert digest(path) == hashes[name], 'Report hash mismatch'
            d = json.loads(path.read_text())
            assert d['status'] == 'PASS' and all(c['passed'] for c in d['checks'])
            assert d['observerFailures'] == [] and d['readObserverFailures'] == []
            assert not any(o['kind'] == 'GAP' for o in d['observations'])
            t, topic_id = d['identities']['tournament'], d['identities']['topic']
            old = topic(d['initial'], topic_id)['name']
            final = topic(d['final'], topic_id)['name']
            renamed = case != 'recovery-no-event'
            positive = case in ('forward-stale', 'recovery-stale')
            assert d['final']['sourceTopic']['name'] == ('RENAMED TOPIC' if renamed else old)
            assert final == (old if positive or not renamed else 'RENAMED TOPIC')
            assert d['final']['tournament']['state'] == 'ACTIVE'
            assert d['final']['tournament']['sagaState'] == 'NOT_IN_SAGA'
            assert d['producerOutcome'] == ('COMPENSATED' if case.startswith('recovery') else 'COMMITTED')
            writes = [o['data'] for o in d['observations'] if o['kind'] == 'WRITE'
                      and o['data']['aggregate']['identity']['aggregateId'] == t]
            delivery = [w for w in writes if w['writer']['kind'] == 'EVENT_CONSUMER']
            assert len(delivery) == (1 if renamed else 0)
            if renamed:
                event = d['event']
                assert event['published'] and event['publisherAggregateId'] == topic_id
                assert delivery[0]['writer']['eventId'] == event['eventId']
                assert projected_topic(delivery[0]['aggregate'], topic_id)['topicName'] == 'RENAMED TOPIC'
                assert any(a['name'] == 'B:rename' and a['status'] == 'COMMITTED' for a in d['actions'])
                assert any(a['name'] == 'C:deliver' and a['status'] == 'COMMITTED' for a in d['actions'])
            regressions = []
            for before, after in zip(writes, writes[1:]):
                if before['writer']['kind'] == 'EVENT_CONSUMER' \
                        and after['writer']['sagaInstanceId'] == 'A' \
                        and projected_topic(before['aggregate'], topic_id)['topicName'] == 'RENAMED TOPIC' \
                        and projected_topic(after['aggregate'], topic_id)['topicName'] == old:
                    regressions.append({'beforeVersion': before['aggregate']['version'],
                        'afterVersion': after['aggregate']['version'], 'overwriter': after['writer']})
            assert len(regressions) == (1 if positive else 0)
            if positive:
                assert regressions[0]['overwriter']['phase'] == ('FORWARD' if case.startswith('forward') else 'RECOVERY')
                cached = d['cachedTopicDtos'] if case.startswith('forward') else d['savedOriginalDto']['topics']
                assert next(x for x in cached if x['aggregateId'] == topic_id)['name'] == old
            if case.startswith('recovery'):
                assert any(c['name'] == 'exact injected pre-body fault' and c['passed'] for c in d['checks'])
            else:
                assert not any(a['status'] == 'FAULT' for a in d['actions'])
            rows.append({'case': case, 'serialized': mode == 'true', 'status': 'PASS',
                'sourceTopicName': d['final']['sourceTopic']['name'], 'finalCopiedTopicName': final,
                'overwriteObserved': positive, 'overwrites': regressions,
                'readerScope': 'Topic DTO returns are currently unmapped; cached DTOs are experiment-only evidence.',
                'timeline': [{'writer': w['writer']['actionId'], 'phase': w['writer']['phase'],
                              'version': w['aggregate']['version'],
                              'name': projected_topic(w['aggregate'], topic_id)['topicName'],
                              'topicVersion': projected_topic(w['aggregate'], topic_id)['topicVersion']}
                             for w in writes],
                'report': str(path.resolve()), 'sha256': digest(path)})
    for case in CASES:
        pair = [r for r in rows if r['case'] == case]
        assert pair[0]['finalCopiedTopicName'] == pair[1]['finalCopiedTopicName']
        assert pair[0]['overwriteObserved'] == pair[1]['overwriteObserved']
    return {'status': 'PASS', 'validatorSha256': digest(Path(__file__)),
            'executions': 10, 'overwriteExecutions': 4,
            'controlExecutions': 6, 'productionDetectorImplemented': False,
            'productionScoreChanged': False, 'rows': rows}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--run', type=Path, required=True)
    p.add_argument('--output', type=Path, required=True)
    a = p.parse_args()
    result = validate(a.run)
    a.output.write_text(json.dumps(result, indent=2) + '\n')
    print('PASS: 10 executions; 4 stale-write witnesses and 6 controls')


if __name__ == '__main__':
    main()
