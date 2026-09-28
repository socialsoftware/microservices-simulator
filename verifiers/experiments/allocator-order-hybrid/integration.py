"""Opt-in adapters; original workload profiles and allocator defaults stay intact."""
import json
from pathlib import Path
from allocator import ContextualLinUcbPolicy, ProgressFeatureSpace
from hybrid_linucb import HybridLinUcbPolicy, StructuralFeatureSpace
from order_features import (load_compatible_structural_metadata, order_profile,
                            augment_profile, OrderFeatureSpace)

ROOT=Path(__file__).resolve().parents[3]
EVIDENCE=ROOT/'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21'


def prepare(workloads):
    manifest=json.loads((ROOT/'docs/verifiers-impl/evidence/allocator-transfer-2026-09-21/inputs.json').read_text())
    metadata=load_compatible_structural_metadata(manifest['interactionFiles'])
    evidence={'metadata':metadata['source'],'workloads':{}}
    for row in workloads:
        order=order_profile(row['domain'].workload,metadata,strict=True)
        row['orderProfile']=augment_profile(row['profile'],order)
        evidence['workloads'][row['id']]=order
    p=EVIDENCE/'order-inputs.json'
    if p.exists() and json.loads(p.read_text())!=evidence:
        raise ValueError('Order metadata/profile identity changed')
    p.write_text(json.dumps(evidence,indent=2)+'\n')
    return evidence


def experimental_policy(arm,states,seed):
    if arm=='order':
        # States are newly constructed per arm. Keep source rows/profiles unchanged.
        for state in states.values():
            state['profile']=state['orderProfile']
        return ContextualLinUcbPolicy(OrderFeatureSpace({w:s['profile'] for w,s in states.items()}),1.,1.)
    if arm=='hybrid':
        return HybridLinUcbPolicy(StructuralFeatureSpace({w:s['profile'] for w,s in states.items()}),
                                 ProgressFeatureSpace(),states,exploration=1.,ridge=1.)
    raise ValueError(arm)
