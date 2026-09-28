"""Prospective common tie handling; historical policy implementations stay reproducible."""
import hashlib
import math
import random

RULE = 'seeded-near-maximum-v1'
REL_TOL = ABS_TOL = 1e-12


class SeededTiePolicy:
    """Wrap a pure adaptive selector without changing its scores or updates.

    All six experimental adaptive policies score a singleton without learning
    or consuming randomness. Only priorities within the declared rounding
    tolerance of the maximum participate in a uniform, seeded tie draw.
    """

    def __init__(self, model, seed):
        if type(seed) is not int:
            raise ValueError('Tie seed must be an integer')
        self.model = model
        namespace = f'{RULE}:{seed}'.encode()
        self.rng = random.Random(int.from_bytes(hashlib.sha256(namespace).digest(), 'big'))
        self.draws = 0

    def select(self, active, states):
        rows = []
        for wid in sorted(active):
            chosen, choice, context = self.model.select({wid}, states)
            if chosen != wid or not math.isfinite(choice['index']):
                raise ValueError('Adaptive singleton selector returned an invalid priority')
            rows.append((wid, choice, context))
        if not rows:
            raise ValueError('No active workload')
        maximum = max(row[1]['index'] for row in rows)
        tied = [row for row in rows if math.isclose(row[1]['index'], maximum,
                                                   rel_tol=REL_TOL, abs_tol=ABS_TOL)]
        if len(tied) > 1:
            self.draws += 1
            wid, choice, context = self.rng.choice(tied)
        else:
            wid, choice, context = tied[0]
        return wid, {**choice, 'tie': {'rule': RULE, 'candidates': [r[0] for r in tied],
            'maximumIndex': maximum, 'selectedGap': maximum - choice['index'],
            'draw': self.draws if len(tied) > 1 else None}}, context

    def update(self, workload_id, context, reward):
        return self.model.update(workload_id, context, reward)

    def summary(self):
        return {**self.model.summary(), 'tieRule': RULE, 'tieDraws': self.draws,
                'tieRelativeTolerance': REL_TOL, 'tieAbsoluteTolerance': ABS_TOL}


def make_policy(base_factory, arm, states, seed):
    model = base_factory(arm, states, seed)
    return model if arm == 'uniform' else SeededTiePolicy(model, seed)
