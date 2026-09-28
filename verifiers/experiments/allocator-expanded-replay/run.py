"""Replay the master inventory with explicit preparation and measurement provenance.

Recorded results from historical measurement versions remain historical results. This
runner compares allocation on that frozen benchmark; it does not harmonise detectors.
"""
import argparse
import copy
import gzip
import hashlib
import itertools
import json
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
sys.path[:0] = [str(HERE.parent / 'allocator-component-pilot'),
               str(HERE.parent / 'allocator-order-hybrid-followup'),
               str(HERE.parent / 'allocator-transfer'),
               str(HERE.parent / 'fixed-workload-ga')]
from allocator import RecordedFeedbackEvaluator, UniformPolicy, structural_profile
from catalogue import RecordedDomain
from fitness import CRITERIA_V2, configuration
from inputs import load_inputs
from policies import policy
from study import run_arm
from search import vector_for

MASTER = ROOT / 'docs/verifiers-impl/evidence/workload-master-inventory-2026-09-25'
AUDIT = ROOT / 'verifiers/target/shared-setup-fix-2026-09-26'
OLD = ROOT / 'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json'
OLD_INVENTORY = ROOT / 'docs/verifiers-impl/evidence/transfer-inventory-2026-09-21/inventory.json'
ARMS = ('uniform', 'P-shared', 'S-cal', 'SP-cal', 'P-local', 'H0', 'H1-cal')
PROFILES = ('all-five', *CRITERIA_V2)


def read(path):
    return json.loads(Path(path).read_text())


def sha(path):
    with Path(path).open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def local(path):
    path = Path(path)
    if path.exists():
        return path.resolve()
    text = str(path)
    for marker in ('/verifiers/target/', '/reports/'):
        if marker in text:
            mapped = ROOT / 'verifiers/target' / text.split(marker, 1)[1]
            if mapped.exists():
                return mapped
    raise ValueError('Retained provenance file missing: ' + text)


def rows(path):
    return [json.loads(line) for line in Path(path).read_text().splitlines() if line.strip()]


def semantic_setup(row):
    setup = row.get('currentSetup')
    return None if setup is None else {k: v for k, v in setup.items() if k != 'id'}


def validate_enumeration(directory, domain):
    """Recheck retained generator responses even if the old package was archived."""
    count = read(directory / 'domain-count.json')
    requests = read(directory / 'requests.json')
    expected = {vector_for(g, domain.width) for g in itertools.product(*domain.coordinates)}
    if (not count['countComplete'] or count['anyTruncated']
            or count['capLimitedUniqueCandidateCount'] != len(domain.candidates)
            or len(requests) != len(expected) or {r['vector'] for r in requests} != expected):
        raise ValueError('Historical enumeration incomplete')
    for request in requests:
        response = request['response']
        ids = response['faultScenarioIds']
        actual = {alias for c in domain.resolve(request['vector']) for alias in c['aliases']}
        if (request.get('truncated') or request['process']['status'] != 'EXITED'
                or response['status'] not in ('PERSISTED', 'DEDUPLICATED')
                or response['workloadPlanId'] != domain.workload['id']
                or len(ids) != len(set(ids)) or len(ids) != int(response['uncappedScheduleCount'])
                or len(ids) != response['writtenScheduleCount'] or set(ids) != actual):
            raise ValueError('Historical generator response differs from recorded domain')


def freeze(out):
    before = {r['workload']: r for r in read(AUDIT / 'setup-audit-before.json')['rows']}
    after = {r['workload']: r for r in read(AUDIT / 'setup-audit-after.json')['rows']}
    master = rows(MASTER / 'workloads.jsonl')
    receipt = read(MASTER / 'measurement-2026-09-26.json')
    # The receipt identifies the four known failed preparations; no score filter
    # determines admission. Other changed source recipes wait for qualification.
    diagnostic_ids = set(receipt['preparationDiagnosticWorkloads'])
    if len(diagnostic_ids) != 4:
        raise ValueError('Diagnostic identities differ from reviewed preparation audit')
    admission = []
    for r in master:
        wid = r['id']
        if wid in diagnostic_ids:
            state = 'KNOWN_PREPARATION_DIAGNOSTIC'
        elif (semantic_setup(before[wid]) != semantic_setup(after[wid])
              and after[wid].get('sameSetup') is not True):
            state = 'PREPARATION_REQUALIFICATION_PENDING'
        else:
            state = 'RECORDED_BENCHMARK_ADMITTED'
        admission.append({**r, 'admission': state,
                          'storedSetupCompared': after[wid].get('sameSetup') is not None})
    old = read(OLD)
    partitions = []
    accepted = {r['id'] for r in admission if r['admission'] == 'RECORDED_BENCHMARK_ADMITTED'}
    old_subset = {**old, 'workloads': [r for r in old['workloads'] if r['workload'] in accepted]}
    partitions.append(old_subset)
    old_ids = {r['workload'] for r in old['workloads']}
    # The old transfer experiment selected 81 of the 96 workloads. Its manifest
    # must not silently drop the remaining 15 from the expanded benchmark.
    for historical in read(OLD_INVENTORY)['rows']:
        wid = historical['workload']
        if wid not in accepted or wid in old_ids:
            continue
        reference = local(historical['reference'])
        metadata = local(historical['workloadFile'])
        interaction_path = metadata.parent / 'interactions.jsonl'
        enum_candidates = [reference.parent / 'catalogue', reference.parent.parent / 'catalogue',
                           metadata.parent.parent]
        enum = next((p for p in enum_candidates if (p / 'domain-count.json').exists()
                     and read(p / 'domain-count.json')['countComplete']
                     and not read(p / 'domain-count.json')['anyTruncated']
                     and read(p / 'domain-count.json')['capLimitedUniqueCandidateCount']
                         == historical['scenarios']), None)
        files = [reference, metadata, interaction_path]
        if enum:
            files.extend([enum / 'domain-count.json', enum / 'requests.json',
                          *sorted((enum / 'package').glob('*.json*'))])
        partitions.append({'kind': 'historical-recorded-domain',
                           'files': {str(p): sha(p) for p in files},
                           'workloads': [{**historical, 'enumeration': str(enum) if enum else None,
                               'coverage': 'FULL_ENUMERATION_RECHECK' if enum else 'RECORDED_DOMAIN_ONLY',
                               'interactionFile': str(interaction_path),
                               'fitness': configuration({'policy': 'weighted-criteria-v2',
                                             'weights': {c: 1 for c in CRITERIA_V2}})}]})
    for r in admission:
        if r['id'] not in accepted or r['baseline96']:
            continue
        reference = local(ROOT / r['reference'])
        protocol_path = reference.parent / 'protocol.json'
        if protocol_path.exists():
            protocol = read(protocol_path)
            enum = local(protocol['enumeration'])
            for name, expected in protocol['enumerationHashes'].items():
                if sha(enum / name) != expected:
                    raise ValueError('Enumeration provenance changed: ' + str(enum / name))
            count = protocol['fullCatalogueCount']
            fitness = protocol['config']['fitness']
        else:
            # Two recovered discovery maps retain the catalogue seal and config
            # directly rather than a resumable measurement protocol.
            protocol_path = reference.parent / 'config.json'
            enum = reference.parent
            protocol = read(protocol_path)
            seal = read(enum / 'catalogue.json')
            for name, expected in seal['files'].items():
                if sha(enum / name) != expected:
                    raise ValueError('Catalogue seal changed: ' + str(enum / name))
            count = seal['candidateCount']
            fitness = protocol['fitness']
        if count != r['scenarios']:
            raise ValueError('Recorded selection is a partial catalogue: ' + r['id'])
        metadata = local(ROOT / r['metadataSource'])
        workload = next(w for w in rows(metadata) if w['id'] == r['id'])
        interaction_path = metadata.parent / 'interactions.jsonl'
        interactions = {i['id']: i for i in rows(interaction_path)}
        profile = structural_profile({'sagas': [{'fqn': p['saga']} for p in workload['participants']],
                                      'interactions': [interactions[i] for i in workload['interactions']]}, workload)
        files = [metadata, interaction_path, reference, protocol_path,
                 enum / 'domain-count.json', enum / 'requests.json']
        partitions.append({'files': {str(p): sha(p) for p in files},
                           'interactionFiles': [str(interaction_path)], 'workloads': [{
                               'workload': r['id'], 'workloadFile': str(metadata),
                               'reference': str(reference), 'enumeration': str(enum),
                               'scenarios': r['scenarios'], 'label': r['sagaFamily'] + ' ' + r['id'][:10],
                               'fitness': fitness,
                               'structuralProfileSha256': hashlib.sha256(
                                   json.dumps(profile, sort_keys=True).encode()).hexdigest()}]})
    frozen = {'purpose': 'Expanded recorded benchmark; historical measurement versions retained',
              'admissionRule': 'Preparation changes pending qualification; no outcome-based filtering',
              'sourceHashes': {str(p): sha(p) for p in (MASTER / 'workloads.jsonl', OLD, OLD_INVENTORY,
                                  AUDIT / 'setup-audit-before.json', AUDIT / 'setup-audit-after.json',
                                  MASTER / 'measurement-2026-09-26.json')},
              'admission': admission, 'partitions': partitions}
    out.mkdir(parents=True, exist_ok=True)
    (out / 'inputs.json').write_text(json.dumps(frozen, indent=2) + '\n')
    return frozen


def qualify_frozen(out, frozen, qualification):
    """Add reviewed, freshly measured preparation versions to a new freeze."""
    receipt = read(qualification)
    if not receipt['complete'] or len(receipt['rows']) != 17:
        raise ValueError('The 17-workload preparation review is incomplete')
    by_id = {r['workload']: r for r in receipt['rows']}
    pending = {r['id'] for r in frozen['admission']
               if r['admission'] == 'PREPARATION_REQUALIFICATION_PENDING'}
    if set(by_id) != pending:
        raise ValueError('Qualification does not match the pending selection')
    frozen['sourceHashes'][str(qualification.resolve())] = sha(qualification)
    admitted = []
    for metadata in frozen['admission']:
        proof = by_id.get(metadata['id'])
        if proof is None:
            continue
        metadata['preparationQualification'] = proof
        if proof['state'] != 'QUALIFIED':
            metadata['admission'] = 'PREPARATION_REVIEWED_BLOCKED'
            continue
        ref_path = Path(proof['reference'])
        if sha(ref_path) != proof['referenceSha256']:
            raise ValueError('Qualified reference changed')
        reference = read(ref_path)
        original = read(ROOT / proof['historicalReference'])
        if (sha(ROOT / proof['historicalReference']) != proof['historicalReferenceSha256']
                or reference['candidates'] != original['candidates']
                or len(reference['observations']) != proof['candidateCount']):
            raise ValueError('Qualification changed candidate structure or coverage')
        directory = ref_path.parent
        workload_file = directory / 'package/workloads.jsonl'
        interaction_file = directory / 'package/interactions.jsonl'
        workload = next(w for w in rows(workload_file) if w['id'] == metadata['id'])
        interaction = {i['id']: i for i in rows(interaction_file)}
        profile = structural_profile({'sagas': [{'fqn': p['saga']} for p in workload['participants']],
            'interactions': [interaction[i] for i in workload['interactions']]}, workload)
        files = [ref_path, workload_file, interaction_file, directory / 'config.json',
                 directory / 'preflight.json', *sorted((directory / 'package').glob('*'))]
        frozen['partitions'].append({'kind': 'qualified-recorded-domain',
            'files': {str(p): sha(p) for p in files}, 'workloads': [{
                'workload': metadata['id'], 'workloadFile': str(workload_file),
                'interactionFile': str(interaction_file), 'reference': str(ref_path),
                'enumeration': None, 'coverage': 'RETAINED_COMPLETE_DOMAIN_FRESH_EXECUTION',
                'scenarios': proof['candidateCount'], 'label': metadata['sagaFamily'],
                'fitness': configuration({'policy': 'weighted-criteria-v2',
                                         'weights': {c: 1 for c in CRITERIA_V2}}),
                'structuralProfileSha256': hashlib.sha256(json.dumps(profile, sort_keys=True).encode()).hexdigest()}]})
        metadata['historicalReference'] = metadata['reference']
        metadata['reference'] = str(ref_path)
        metadata['referenceSha256'] = sha(ref_path)
        metadata['joint'] = proof['joint']
        rt = read(directory / 'config.json')['runtime']
        metadata['runtime'] = {'signature': hashlib.sha256(json.dumps(rt, sort_keys=True).encode()).hexdigest(),
            'config': str(directory / 'config.json'), 'image': rt['image'], 'hashCount': len(rt['hashes'])}
        metadata['admission'] = 'RECORDED_BENCHMARK_ADMITTED'
        admitted.append(metadata['id'])
    frozen['preparationQualification'] = {'receipt': str(qualification.resolve()), 'admitted': admitted}
    frozen['admissionRule'] = 'Reviewed explicit fixtures and current source prefixes; no outcome-based filtering'
    (out / 'inputs.json').write_text(json.dumps(frozen, indent=2) + '\n')
    return frozen


def load(out, frozen):
    for path, expected in frozen['sourceHashes'].items():
        if sha(path) != expected:
            raise ValueError('Freeze source changed: ' + path)
    workloads, references, original = [], {}, {}
    proof = []
    for index, part in enumerate(frozen['partitions']):
        path = out / f'partition-{index:03d}.json'
        path.write_text(json.dumps(part) + '\n')
        if part.get('kind') in ('historical-recorded-domain', 'qualified-recorded-domain'):
            for name, expected in part['files'].items():
                if sha(name) != expected:
                    raise ValueError('Historical recorded input changed: ' + name)
            current, verified = [], []
            for row in part['workloads']:
                workload = next(w for w in rows(row['workloadFile']) if w['id'] == row['workload'])
                ref = read(row['reference'])
                domain = RecordedDomain(workload, ref['candidates'])
                if len(domain.candidates) != row['scenarios']:
                    raise ValueError('Historical candidate count mismatch')
                if row['enumeration']:
                    validate_enumeration(Path(row['enumeration']), domain)
                interaction = {i['id']: i for i in rows(row['interactionFile'])}
                profile = structural_profile({'sagas': [{'fqn': p['saga']} for p in workload['participants']],
                    'interactions': [interaction[i] for i in workload['interactions']]}, workload)
                ph = hashlib.sha256(json.dumps(profile, sort_keys=True).encode()).hexdigest()
                if ph != row['structuralProfileSha256']:
                    raise ValueError('Historical structural profile changed')
                current.append({'id': row['workload'], 'name': row['label'], 'domain': domain,
                                'profile': profile})
                verified.append({'workload': row['workload'], 'candidates': len(domain.candidates),
                                 'completeEnumerationRechecked': bool(row['enumeration']),
                                 'coverage': row['coverage'], 'profileSha256': ph})
        else:
            current, _, verified = load_inputs(path)
        workloads.extend(current)
        proof.extend(verified)
        for row in part['workloads']:
            references[row['workload']] = read(row['reference'])
            original[row['workload']] = configuration(row['fitness'])
    expected = {r['id'] for r in frozen['admission'] if r['admission'] == 'RECORDED_BENCHMARK_ADMITTED'}
    if {r['id'] for r in workloads} != expected or len(workloads) != len(expected):
        raise ValueError('Loaded workloads differ from admission receipt')
    evaluator = RecordedFeedbackEvaluator(references, {r['id']: r['domain'] for r in workloads}, original)
    def factory():
        fresh = copy.copy(evaluator)
        fresh.revealed = set()
        return fresh
    (out / 'validation.json').write_text(json.dumps(proof, indent=2) + '\n')
    return workloads, factory


def make_policy(arm, states, seed):
    return UniformPolicy(seed) if arm == 'uniform' else policy(arm, states, seed)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--freeze', action='store_true')
    parser.add_argument('--preparation-qualification', type=Path)
    parser.add_argument('--validate-only', action='store_true')
    parser.add_argument('--budget', type=int, default=128)
    parser.add_argument('--seeds', nargs='+', type=int, default=[1])
    parser.add_argument('--arms', nargs='+', choices=ARMS, default=list(ARMS))
    parser.add_argument('--profiles', nargs='+', choices=PROFILES, default=['all-five'])
    args = parser.parse_args()
    if args.budget < 1 or args.budget > 8000:
        raise ValueError('Budget must be 1..8000')
    frozen = freeze(args.output) if args.freeze else read(args.output / 'inputs.json')
    if args.preparation_qualification:
        if not args.freeze:
            raise ValueError('Preparation qualification requires a new freeze')
        frozen = qualify_frozen(args.output, frozen, args.preparation_qualification)
    workloads, factory = load(args.output, frozen)
    print('VALIDATED', len(workloads), 'workloads', sum(len(w['domain'].candidates) for w in workloads),
          'candidates', flush=True)
    if args.validate_only:
        return
    for profile in args.profiles:
        fit = configuration({'policy': 'weighted-criteria-v2', 'weights': {
            c: int(profile == 'all-five' or c == profile) for c in CRITERIA_V2}})
        for seed in args.seeds:
            for arm in args.arms:
                name = f'{profile}-{arm}-seed{seed}-budget{args.budget}.json.gz'
                path = args.output / name
                if path.exists():
                    raise ValueError('Existing run retained; choose a new output directory: ' + str(path))
                started = time.monotonic()
                run = run_arm(workloads, factory, arm, seed, args.budget,
                              [n for n in (128, 256, 512, 1024, 2048, 4096, 8000, args.budget)
                               if n <= args.budget], fit, make_policy)
                run['inputSha256'] = sha(args.output / 'inputs.json')
                run['runnerSourceHashes'] = {str(p): sha(p) for p in [HERE / 'run.py',
                    HERE.parent / 'allocator-component-pilot/policies.py',
                    HERE.parent / 'allocator-order-hybrid-followup/study.py',
                    *sorted((HERE.parent / 'fixed-workload-ga').glob('*.py'))]}
                run['wallSeconds'] = time.monotonic() - started
                with gzip.open(path, 'wt') as stream:
                    json.dump(run, stream)
                print(profile, arm, seed, run['attempts'], round(run['wallSeconds'], 2), flush=True)


if __name__ == '__main__':
    main()
