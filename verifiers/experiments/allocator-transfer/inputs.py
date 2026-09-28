"""Read compact, previously verified complete maps for the transfer experiment."""
import copy
import hashlib
import itertools
import json
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
from allocator import RecordedFeedbackEvaluator, structural_profile
from catalogue import RecordedDomain
from fitness import configuration
from search import vector_for


def read(path):
    return json.loads(Path(path).read_text())


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def load_inputs(manifest):
    spec = read(manifest)
    for name, expected in spec['files'].items():
        if sha(name) != expected:
            raise ValueError('Input changed: ' + name)
    interactions = {}
    for name in spec['interactionFiles']:
        for line in Path(name).read_text().splitlines():
            if not line.strip():
                continue
            row = json.loads(line)
            if row['id'] in interactions:
                if any(interactions[row['id']].get(k) != row.get(k)
                       for k in ('accesses', 'evidence')):
                    raise ValueError('Conflicting structural evidence: ' + row['id'])
            interactions[row['id']] = row
    workloads, domains, references, original_fitness = [], {}, {}, {}
    proof = []
    for row in spec['workloads']:
        wid = row['workload']
        path = Path(row['workloadFile'])
        records = [read(path)] if path.name == 'workload.json' else [
            json.loads(line) for line in path.read_text().splitlines() if line.strip()]
        matches = [w for w in records if w['id'] == wid]
        if len(matches) != 1:
            raise ValueError('Workload metadata not unique: ' + wid)
        workload = matches[0]
        # Reference candidate records are structural. Outcomes are passed only to
        # RecordedFeedbackEvaluator, never to profiles or the local GA domain.
        reference = read(row['reference'])
        domain = RecordedDomain(workload, reference['candidates'])
        if len(domain.candidates) != row['scenarios']:
            raise ValueError('Candidate count differs from inventory')
        if row.get('enumeration'):
            enum = Path(row['enumeration'])
            count = read(enum / 'domain-count.json')
            requests = read(enum / 'requests.json')
            if not count['countComplete'] or count['anyTruncated'] \
                    or count['capLimitedUniqueCandidateCount'] != len(domain.candidates):
                raise ValueError('Enumeration incomplete')
            expected = {vector_for(g, domain.width) for g in itertools.product(*domain.coordinates)}
            if len(requests) != len(expected) or {r['vector'] for r in requests} != expected:
                raise ValueError('Missing/repeated canonical vectors')
            by_vector = {}
            for request in requests:
                response = request['response']
                if request.get('truncated') or request['process']['status'] != 'EXITED' \
                        or response['status'] not in ('PERSISTED', 'DEDUPLICATED') \
                        or response['workloadPlanId'] != wid:
                    raise ValueError('Unqualified enumeration response')
                ids = response['faultScenarioIds']
                if len(ids) != len(set(ids)) or len(ids) != int(response['uncappedScheduleCount']) \
                        or len(ids) != response['writtenScheduleCount']:
                    raise ValueError('Truncated/duplicate candidate list')
                by_vector[request['vector']] = set(ids)
            for vector, ids in by_vector.items():
                actual = {alias for c in domain.resolve(vector) for alias in c['aliases']}
                if actual != ids:
                    raise ValueError('Reference candidates differ from enumeration')
        elif row['completenessBasis'] != 'PREVIOUS_VERIFIED_CREATION_BATCH':
            raise ValueError('No complete-map provenance')
        profile = structural_profile({'sagas': [{'fqn': p['saga']} for p in workload['participants']],
            'interactions': [interactions[i] for i in workload['interactions']]}, workload)
        ph = hashlib.sha256(json.dumps(profile, sort_keys=True).encode()).hexdigest()
        if ph != row['structuralProfileSha256']:
            raise ValueError('Structural profile differs from inventory')
        workloads.append({'id': wid, 'name': row['label'], 'domain': domain, 'profile': profile})
        domains[wid] = domain
        references[wid] = reference
        original_fitness[wid] = configuration(row['fitness'])
        proof.append({'workload': wid, 'candidates': len(domain.candidates),
                      'completeEnumerationRechecked': bool(row.get('enumeration')),
                      'profileSha256': ph})
    evaluator = RecordedFeedbackEvaluator(references, domains, original_fitness)
    def factory():
        fresh = copy.copy(evaluator)
        fresh.revealed = set()
        return fresh
    return workloads, factory, proof
