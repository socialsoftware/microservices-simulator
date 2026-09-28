"""Matched initially untrained allocation comparisons; retained feedback only."""
import copy
import hashlib
import json
import math
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / 'fixed-workload-ga'))
sys.path.insert(0, str(HERE.parent / 'allocator-transfer'))
from allocator import (AdaptiveUcbPolicy, ContextualLinUcbPolicy, FeatureSpace,
                       ProgressFeatureSpace, RoundRobinPolicy, UniformPolicy)
from transfer import (_new_states, _active, _observe, _fitness,
                      _compact_workload_rows, _session_start_evidence)
from search import stable


def digest(value):
    return hashlib.sha256(stable(value).encode()).hexdigest()


def baseline_policy(name, states, seed):
    profiles = {wid: s['profile'] for wid, s in states.items()}
    if name == 'round-robin':
        return RoundRobinPolicy(states)
    if name == 'uniform':
        return UniformPolicy(seed)
    if name == 'independent':
        return AdaptiveUcbPolicy(1.0)
    if name == 'progress':
        return ContextualLinUcbPolicy(ProgressFeatureSpace(), 1.0, 1.0)
    if name == 'structural':
        return ContextualLinUcbPolicy(FeatureSpace(profiles), 1.0, 1.0)
    raise ValueError(name)


def run_arm(workloads, factory, arm, seed, budget, checkpoints, make_policy=baseline_policy):
    states = _new_states(workloads, seed, budget, _fitness())
    evaluator = factory()
    policy = make_policy(arm, states, seed)
    start = _session_start_evidence(states)
    decisions, checkpoint_rows, seen = [], [], set()
    positives = unknown = 0
    total = 0.0
    overhead = 0.0
    while len(decisions) < budget:
        t = time.perf_counter()
        active = _active(states)
        if not active:
            break
        wid, choice, context = policy.select(active, states)
        state = states[wid]
        candidate = state['session'].ask()
        if candidate is None:
            overhead += time.perf_counter() - t
            continue
        before = copy.deepcopy(state['progress'])
        # Context is constructed BEFORE feedback/progress mutation.
        overhead += time.perf_counter() - t
        result = evaluator.evaluate(wid, candidate, len(decisions) + 1)
        t = time.perf_counter()
        item = state['session'].tell(candidate, result)
        reward = item['fitnessScore']
        _observe(state['progress'], reward)
        updated = policy.update(wid, context, reward)
        overhead += time.perf_counter() - t
        identity = (wid, candidate['key'])
        if identity in seen:
            raise ValueError('Repeated measured candidate')
        seen.add(identity)
        if reward is None:
            unknown += 1
        else:
            total += reward
            positives += reward > 0
        n = len(decisions) + 1
        decisions.append({'decision': n, 'workload': wid, 'candidate': candidate['key'],
                          'scenarioId': candidate['id'], 'score': reward,
                          'operator': item['operator'], 'preUpdateProgress': before,
                          'contextSha256': digest(context), 'choice': choice,
                          'modelUpdated': updated, 'positives': positives,
                          'cumulativeScore': total, 'unknowns': unknown})
        if n in checkpoints:
            checkpoint_rows.append({'attempt': n, 'positives': positives,
                                    'cumulativeScore': total, 'unknowns': unknown})
    if len(evaluator.revealed) != len(decisions):
        raise ValueError('Feedback reveal count differs from selected attempts')
    return {'arm': arm, 'seed': seed, 'budget': budget, 'attempts': len(decisions),
            'start': start, 'positives': positives, 'cumulativeScore': total,
            'unknowns': unknown, 'checkpoints': checkpoint_rows,
            'selectionAndUpdateSeconds': overhead,
            'perWorkload': _compact_workload_rows(states),
            'decisions': decisions, 'decisionSha256': digest(decisions)}
