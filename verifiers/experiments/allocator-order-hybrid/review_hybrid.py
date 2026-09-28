"""Independent NumPy oracle: one full joint ridge system, no Schur updates."""
import json
import math
import sys
from pathlib import Path
import numpy as np
sys.path.insert(0,str(Path(__file__).resolve().parent.parent/'fixed-workload-ga'))
from hybrid_linucb import HybridLinUcbPolicy

class Fixed:
    def __init__(self,n,field): self.names=list(range(n));self.field=field
    def vector(self,profile,progress):return profile[self.field]


def check():
    errors=[]
    for ridge in (.2,1.,3.):
        policy=HybridLinUcbPolicy(Fixed(4,'z'),Fixed(3,'x'),['a','b','c'],exploration=.7,ridge=ridge)
        A=np.eye(13)*ridge;b=np.zeros(13)
        rng=np.random.default_rng(20260921)
        for n in range(40):
            wid=['a','b','c'][n%3]
            z=rng.uniform(-1,1,4);x=rng.uniform(-1,1,3)
            state={'profile':{'z':z.tolist(),'x':x.tolist()},'progress':{}}
            ctx=policy.context(wid,state)
            reward=None if n%9==0 else float(rng.uniform(0,5))
            policy.update(wid,ctx,reward)
            if reward is not None:
                v=np.zeros(13);v[:4]=z;offset=4+3*['a','b','c'].index(wid);v[offset:offset+3]=x
                A+=np.outer(v,v);b+=v*reward
            theta=np.linalg.solve(A,b)
            for i,qid in enumerate(('a','b','c')):
                zz=rng.uniform(-1,1,4);xx=rng.uniform(-1,1,3)
                qs={'profile':{'z':zz.tolist(),'x':xx.tolist()},'progress':{}}
                _,choice,_=policy.select({qid},{qid:qs})
                v=np.zeros(13);v[:4]=zz;v[4+3*i:7+3*i]=xx
                estimate=float(v@theta);uncertainty=.7*math.sqrt(float(v@np.linalg.solve(A,v)))
                errors.extend((abs(choice['estimate']-estimate),abs(choice['uncertainty']-uncertainty)))
                coeff=policy.coefficients(qid)
                errors.extend(np.abs(np.array(coeff['shared'])-theta[:4]).tolist())
                errors.extend(np.abs(np.array(coeff['local'])-theta[4+3*i:7+3*i]).tolist())
    assert max(errors)<1e-10,max(errors)
    result={'oracle':'Independent direct full joint ridge solve (NumPy)',
            'ridges':[.2,1,3],'updatesPerRidge':40,'scoredContexts':360,
            'includesNullAndUnselectedWorkloads':True,'maximumAbsoluteError':max(errors)}
    path=Path(__file__).resolve().parents[3]/'docs/verifiers-impl/evidence/allocator-order-hybrid-2026-09-21/hybrid-independent-review.json'
    path.write_text(json.dumps(result,indent=2)+'\n')
    print(result)

if __name__=='__main__':check()
