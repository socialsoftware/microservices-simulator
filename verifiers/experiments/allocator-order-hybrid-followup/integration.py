"""Opt-in policies and read-only order metadata for the bounded follow-up."""
import copy
import importlib.util
import json
import sys
from pathlib import Path


HERE = Path(__file__).resolve().parent
ROOT = HERE.parents[2]
FIXED = HERE.parent / 'fixed-workload-ga'
TRANSFER = HERE.parent / 'allocator-transfer'
PREVIOUS_EXPERIMENT = HERE.parent / 'allocator-order-hybrid'
sys.path[:0] = [str(HERE), str(FIXED), str(TRANSFER)]

from allocator import ContextualLinUcbPolicy, PROGRESS_FEATURES, ProgressFeatureSpace
from hybrid_linucb import HybridLinUcbPolicy, StructuralFeatureSpace
from order_features import (OrderFeatureSpace, augment_profile,
                            load_compatible_structural_metadata, order_profile)


PREVIOUS_EVIDENCE = ROOT / 'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'


def _previous_study():
    name = '_allocator_order_hybrid_previous_study'
    if name not in sys.modules:
        spec = importlib.util.spec_from_file_location(name, PREVIOUS_EXPERIMENT / 'study.py')
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        sys.modules[name] = module
    return sys.modules[name]


class StructuralOrderFeatureSpace:
    """Existing structure plus order in the shared vector, with no progress."""

    def __init__(self, profiles):
        self.full = OrderFeatureSpace(profiles)
        self.positions = [index for index, name in enumerate(self.full.names)
                          if name not in PROGRESS_FEATURES]
        self.names = [self.full.names[index] for index in self.positions]

    def vector(self, profile, progress):
        full = self.full.vector(profile, progress)
        return [full[index] for index in self.positions]


def prepare(workloads, manifest):
    """Recreate and verify the prior order inputs without rewriting them."""
    metadata = load_compatible_structural_metadata(manifest['interactionFiles'])
    evidence = {'metadata': metadata['source'], 'workloads': {}}
    for row in workloads:
        order = order_profile(row['domain'].workload, metadata, strict=True)
        row['orderProfile'] = augment_profile(row['profile'], order)
        evidence['workloads'][row['id']] = order
    frozen_path = PREVIOUS_EVIDENCE / 'order-inputs.json'
    frozen = json.loads(frozen_path.read_text())
    if evidence != frozen:
        raise ValueError('Recreated order metadata/profile identity differs from frozen evidence')
    return {'path': str(frozen_path.relative_to(ROOT)),
            'workloads': len(evidence['workloads']),
            'metadata': copy.deepcopy(evidence['metadata'])}


def policy(arm, states, seed):
    """Construct one of the eight frozen arms from fresh state."""
    if arm in ('round-robin', 'uniform', 'independent', 'progress', 'structural'):
        return _previous_study().baseline_policy(arm, states, seed)
    if arm == 'order':
        for state in states.values():
            state['profile'] = state['orderProfile']
        profiles = {workload_id: state['profile'] for workload_id, state in states.items()}
        return ContextualLinUcbPolicy(OrderFeatureSpace(profiles), 1.0, 1.0)
    if arm == 'hybrid':
        profiles = {workload_id: state['profile'] for workload_id, state in states.items()}
        return HybridLinUcbPolicy(StructuralFeatureSpace(profiles), ProgressFeatureSpace(),
                                 states, exploration=1.0, ridge=1.0)
    if arm == 'hybrid-order':
        for state in states.values():
            state['profile'] = state['orderProfile']
        profiles = {workload_id: state['profile'] for workload_id, state in states.items()}
        return HybridLinUcbPolicy(StructuralOrderFeatureSpace(profiles),
                                 ProgressFeatureSpace(), states,
                                 exploration=1.0, ridge=1.0)
    raise ValueError('Unknown follow-up arm: ' + arm)
