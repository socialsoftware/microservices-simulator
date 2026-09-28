"""Explicit-fitness continuation of the frozen cross-workload allocator study."""
import copy
import hashlib
import sys
import time
from pathlib import Path


HERE = Path(__file__).resolve().parent
sys.path[:0] = [str(HERE), str(HERE.parent / 'fixed-workload-ga'),
                str(HERE.parent / 'allocator-transfer')]

from transfer import (_active, _compact_workload_rows, _new_states, _observe,
                      _session_start_evidence)
from search import stable


def digest(value):
    return hashlib.sha256(stable(value).encode()).hexdigest()


def run_arm(workloads, factory, arm, seed, budget, checkpoints, fitness, make_policy):
    """Run one cold arm with the selected reward inside every local GA session."""
    states = _new_states(workloads, seed, budget, fitness)
    evaluator = factory()
    policy = make_policy(arm, states, seed)
    start = _session_start_evidence(states)
    decisions, checkpoint_rows, seen = [], [], set()
    positives = unknown = 0
    total = 0.0
    overhead = 0.0
    while len(decisions) < budget:
        before_selection = time.perf_counter()
        active = _active(states)
        if not active:
            break
        workload_id, choice, context = policy.select(active, states)
        state = states[workload_id]
        candidate = state['session'].ask()
        if candidate is None:
            overhead += time.perf_counter() - before_selection
            continue
        before_progress = copy.deepcopy(state['progress'])
        overhead += time.perf_counter() - before_selection

        result = evaluator.evaluate(workload_id, candidate, len(decisions) + 1)
        before_update = time.perf_counter()
        item = state['session'].tell(candidate, result)
        reward = item['fitnessScore']
        _observe(state['progress'], reward)
        updated = policy.update(workload_id, context, reward)
        overhead += time.perf_counter() - before_update

        identity = (workload_id, candidate['key'])
        if identity in seen:
            raise ValueError('Repeated measured candidate')
        seen.add(identity)
        if reward is None:
            unknown += 1
        else:
            total += reward
            positives += reward > 0
        attempt = len(decisions) + 1
        decisions.append({
            'decision': attempt,
            'workload': workload_id,
            'candidate': candidate['key'],
            'scenarioId': candidate['id'],
            'score': reward,
            'operator': item['operator'],
            'preUpdateProgress': before_progress,
            'contextSha256': digest(context),
            'choice': choice,
            'modelUpdated': updated,
            'positives': positives,
            'cumulativeScore': total,
            'unknowns': unknown,
        })
        if attempt in checkpoints:
            checkpoint_rows.append({'attempt': attempt, 'positives': positives,
                                    'cumulativeScore': total, 'unknowns': unknown})

    if len(evaluator.revealed) != len(decisions):
        raise ValueError('Feedback reveal count differs from selected attempts')
    return {
        'arm': arm,
        'seed': seed,
        'budget': budget,
        'fitness': fitness,
        'attempts': len(decisions),
        'start': start,
        'positives': positives,
        'cumulativeScore': total,
        'unknowns': unknown,
        'checkpoints': checkpoint_rows,
        'selectionAndUpdateSeconds': overhead,
        'perWorkload': _compact_workload_rows(states),
        'decisions': decisions,
        'decisionSha256': digest(decisions),
    }
