"""Opt-in component ablations for recorded-feedback allocation pilots."""
import math

from allocator import ContextualLinUcbPolicy, FeatureSpace, PROGRESS_FEATURES, ProgressFeatureSpace
from hybrid_linucb import HybridLinUcbPolicy, StructuralFeatureSpace


class BiasFeatureSpace:
    names = ('bias',)

    def vector(self, profile, progress):
        return [1.0]


class UnitStructureFeatureSpace:
    """Earlier pilot control; scales the intercept as well as structural tokens."""

    def __init__(self, profiles, include_progress=False):
        self.full = FeatureSpace(profiles)
        self.names = self.full.names if include_progress else [
            name for name in self.full.names if name not in PROGRESS_FEATURES]
        self.structural_count = len(self.full.names) - len(PROGRESS_FEATURES)
        self.include_progress = include_progress

    def vector(self, profile, progress):
        full = self.full.vector(profile, progress)
        structural = full[:self.structural_count]
        norm = math.sqrt(math.fsum(value * value for value in structural))
        if norm <= 0:
            raise ValueError('Structural vector has zero norm')
        scaled = [value / norm for value in structural]
        return scaled + full[self.structural_count:] if self.include_progress else scaled


class FixedBiasUnitStructureFeatureSpace(UnitStructureFeatureSpace):
    """Keep bias at one and give non-bias structural coordinates unit norm."""

    def vector(self, profile, progress):
        full = self.full.vector(profile, progress)
        static = full[1:self.structural_count]
        norm = math.sqrt(math.fsum(value * value for value in static))
        if norm <= 0:
            raise ValueError('Non-bias structural vector has zero norm')
        structural = [1.0, *(value / norm for value in static)]
        return structural + full[self.structural_count:] if self.include_progress else structural


class LocalProgressLinUcbPolicy:
    """One progress LinUCB per workload, with no cross-workload updates."""

    def __init__(self, workload_ids, exploration=1.0, ridge=1.0):
        self.models = {wid: ContextualLinUcbPolicy(ProgressFeatureSpace(), exploration, ridge)
                       for wid in sorted(workload_ids)}

    def select(self, active, states):
        best = None
        for wid in sorted(active):
            if wid not in self.models:
                raise ValueError('Unknown workload')
            _, choice, vector = self.models[wid].select({wid}, states)
            if best is None or choice['index'] > best[1]['index']:
                best = wid, choice, vector
        if best is None:
            raise ValueError('No active workload')
        wid, choice, vector = best
        return wid, choice, (wid, vector)

    def update(self, workload_id, context, reward):
        if not isinstance(context, tuple) or len(context) != 2 or context[0] != workload_id:
            raise ValueError('Local context/workload mismatch')
        return self.models[workload_id].update(workload_id, context[1], reward)

    def summary(self):
        return {'modelUpdates': sum(model.updates for model in self.models.values()),
                'localUpdates': {wid: model.updates for wid, model in self.models.items()},
                'sharedParameters': False}


def policy(name, states, seed):
    profiles = {wid: state['profile'] for wid, state in states.items()}
    if name == 'S-shared':
        return ContextualLinUcbPolicy(StructuralFeatureSpace(profiles), 1.0, 1.0)
    if name == 'S-unit':
        return ContextualLinUcbPolicy(UnitStructureFeatureSpace(profiles), 1.0, 1.0)
    if name == 'S-cal':
        return ContextualLinUcbPolicy(FixedBiasUnitStructureFeatureSpace(profiles),
                                     1.0 / math.sqrt(2.0), 1.0)
    if name == 'P-local':
        return LocalProgressLinUcbPolicy(states)
    if name == 'H0':
        return HybridLinUcbPolicy(BiasFeatureSpace(), ProgressFeatureSpace(), states,
                                 exploration=1.0, ridge=1.0)
    if name == 'P-shared':
        return ContextualLinUcbPolicy(ProgressFeatureSpace(), 1.0, 1.0)
    if name == 'SP-shared':
        return ContextualLinUcbPolicy(FeatureSpace(profiles), 1.0, 1.0)
    if name == 'SP-unit':
        return ContextualLinUcbPolicy(
            UnitStructureFeatureSpace(profiles, include_progress=True), 1.0, 1.0)
    if name == 'SP-cal':
        return ContextualLinUcbPolicy(
            FixedBiasUnitStructureFeatureSpace(profiles, include_progress=True),
            1.0 / math.sqrt(2.0), 1.0)
    if name == 'H1':
        return HybridLinUcbPolicy(StructuralFeatureSpace(profiles), ProgressFeatureSpace(),
                                 states, exploration=1.0, ridge=1.0)
    if name == 'H1-unit':
        return HybridLinUcbPolicy(UnitStructureFeatureSpace(profiles),
                                 ProgressFeatureSpace(), states, exploration=1.0, ridge=1.0)
    if name == 'H1-cal':
        return HybridLinUcbPolicy(FixedBiasUnitStructureFeatureSpace(profiles),
                                 ProgressFeatureSpace(), states,
                                 exploration=math.sqrt(2.0 / 3.0), ridge=1.0)
    raise ValueError('Unknown pilot arm')
