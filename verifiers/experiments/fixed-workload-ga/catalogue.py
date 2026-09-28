"""Complete candidate structure shared by live and recorded-feedback search."""
from itertools import product
import time

from runtime import package, read, save, digest, IntegrityError
from search import candidate_key, fault_coordinates, stable, vector_for


class RecordedDomain:
    """Contains candidate structure only; measured scores live outside this object."""
    def __init__(self, workload, candidates):
        self.workload = workload
        self.coordinates = fault_coordinates(workload)
        self.width = sum(len(c) - 1 for c in self.coordinates)
        self.candidates = candidates
        self.by_vector = {}
        for candidate in candidates.values():
            self.by_vector.setdefault(candidate['faultVector'], []).append(candidate)

    def resolve(self, vector):
        return sorted(self.by_vector.get(vector, []), key=lambda c: stable(c['actions']))

    def exhausted(self, seen):
        return set(self.candidates) <= seen

    def sample_unseen(self, rng, seen):
        """Uniform finite-catalogue exploration; no feedback is stored in this domain."""
        remaining = sorted(set(self.candidates) - seen)
        return self.candidates[rng.choice(remaining)]


def enumerated_domain(directory, workload_id):
    inventory = read(directory / 'domain-count.json')
    if not inventory['countComplete'] or inventory['anyTruncated']:
        raise ValueError('A complete, uncapped enumeration is required')
    data = package(directory / 'package/scenario-catalog-manifest.json')
    workload = next(w for w in data['records']['workloads'] if w['id'] == workload_id)
    scenarios = {s['id']: s for s in data['records']['faultScenarios']}
    candidates, vectors = {}, set()
    for request in read(directory / 'requests.json'):
        response = request['response']
        vector = request['vector']
        if vector in vectors or request.get('truncated') or request['process']['status'] != 'EXITED':
            raise ValueError('Invalid vector enumeration')
        vectors.add(vector)
        if response['status'] not in ('PERSISTED', 'DEDUPLICATED') or response['workloadPlanId'] != workload_id:
            raise ValueError('Unqualified generator response')
        ids = response['faultScenarioIds']
        if len(ids) != int(response['uncappedScheduleCount']) or len(ids) != response['writtenScheduleCount']:
            raise ValueError('Enumeration count mismatch')
        for scenario_id in ids:
            c = dict(scenarios[scenario_id])
            if c['workload'] != workload_id or c['faultVector'] != vector:
                raise ValueError('Scenario identity mismatch')
            c['key'] = candidate_key(workload_id, vector, c['actions'])
            c['aliases'] = [scenario_id]
            candidates.setdefault(c['key'], c)
    domain = RecordedDomain(workload, candidates)
    if vectors != {vector_for(g, domain.width) for g in product(*domain.coordinates)}:
        raise ValueError('Missing canonical fault vectors')
    if len(candidates) != inventory['capLimitedUniqueCandidateCount']:
        raise ValueError('Distinct candidate count mismatch')
    return domain, data['hashes']


def build(domain, runtime):
    """Enumerate before any measured search; never silently accept a capped vector."""
    started = time.monotonic()
    keys = set()
    try:
        for genes in product(*domain.coordinates):
            candidates = domain.resolve(vector_for(genes, domain.width))
            request = domain.requests[-1]
            response = request.get('response') or {}
            if request['process']['status'] != 'EXITED' or response.get('status') not in ('PERSISTED', 'DEDUPLICATED'):
                raise ValueError('Catalogue generation failed for vector ' + request['vector'])
            if request.get('truncated') or int(response['uncappedScheduleCount']) != response['writtenScheduleCount']:
                raise ValueError('Catalogue truncated: increase recoveryCap and generate a new catalogue')
            keys.update(c['key'] for c in candidates)
        data = domain.verify()
        runtime.verify()
        save(domain.out / 'domain-count.json', {
            'countComplete': True, 'anyTruncated': False,
            'capLimitedUniqueCandidateCount': len(keys),
            'vectorDomainSize': len(domain.requests),
            'generatorWallSeconds': sum(r['process']['wallSeconds'] for r in domain.requests),
            'enumerationWallSeconds': time.monotonic() - started})
        enumerated_domain(domain.out, domain.workload['id'])
        seal = {'schemaVersion': 'complete-workload-catalogue.v1',
                'workload': domain.workload['id'], 'packageHashes': data['hashes'],
                'candidateCount': len(keys), 'files': {
                    name: digest(domain.out / name) for name in
                    ('config.json', 'source-package-hashes.json', 'domain-count.json', 'requests.json')}}
        save(domain.out / 'catalogue.json', seal)
        return seal
    except (ValueError, KeyError, OSError) as error:
        save(domain.out / 'catalogue-failure.json', {'error': str(error),
             'enumerationWallSeconds': time.monotonic() - started})
        raise


def load(directory, config, source_hashes):
    """Validate provenance and structural completeness; no feedback files are read."""
    seal = read(directory / 'catalogue.json')
    if seal['schemaVersion'] != 'complete-workload-catalogue.v1' or seal['workload'] != config['workload']:
        raise IntegrityError('Catalogue identity mismatch')
    names = {'config.json', 'source-package-hashes.json', 'domain-count.json', 'requests.json'}
    if set(seal['files']) != names or any(digest(directory / n) != seal['files'][n] for n in names):
        raise IntegrityError('Catalogue evidence changed')
    original = read(directory / 'config.json')
    for field in ('workload', 'runtime', 'recoveryCap'):
        if original[field] != config[field]:
            raise ValueError('Catalogue scope differs: ' + field)
    if read(directory / 'source-package-hashes.json') != source_hashes:
        raise IntegrityError('Catalogue source package differs')
    domain, hashes = enumerated_domain(directory, config['workload'])
    if hashes != seal['packageHashes'] or len(domain.candidates) != seal['candidateCount']:
        raise IntegrityError('Catalogue package changed')
    return domain, seal
