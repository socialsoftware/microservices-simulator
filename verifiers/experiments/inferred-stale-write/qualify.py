#!/usr/bin/env python3
"""Run the dummyapp-first Spock checks against the frozen experimental classes."""
import argparse
import json
from pathlib import Path
import shutil
import sys

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
sys.path.insert(0,str(HERE.parent/'fixed-workload-ga'))
from runtime import batch


def main():
    p=argparse.ArgumentParser();p.add_argument('--run',type=Path,required=True);args=p.parse_args()
    out=args.run.resolve();config=json.loads((out/'protocol.json').read_text())['runtime']
    mount=['docker','run','--rm','--network','none','-v',str(batch.TARGET)+':/reports:ro','-v',str(out)+':/out']
    cp='/out/classes:'+config['classpath']
    def run(arguments,label):
        r=batch.run_process([*mount,'--entrypoint','java',config['image'],*arguments],out/(label+'.log'),120,lambda:None)
        if r.get('exitCode')!=0:raise RuntimeError((out/(label+'.log')).read_text()[-6000:])
    run(['-cp',cp,'experiment.inferred.ExtractCopies','/out/fixture-source','/out/fixture-contracts.json'],'extract-fixture')
    run(['-javaagent:/out/agent.jar','-Dexperiment.copyContracts=/out/fixture-contracts.json','-cp',cp,
         'experiment.inferred.ProbeControls','/out/controls'],'probe-controls')
    spec=ROOT/'verifiers/src/test/groovy/pt/ulisboa/tecnico/socialsoftware/ms/verifiers/faults/executor/InferredCopyExperimentSpec.groovy'
    shutil.copy2(spec,out/spec.name)
    run(['-cp',cp,'org.codehaus.groovy.tools.FileSystemCompiler','-d','/out/test-classes','/out/'+spec.name],'compile-spec')
    (out/'RunSpecs.groovy').write_text('''
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
def listener = new SummaryGeneratingListener()
def request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass('pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.InferredCopyExperimentSpec')).build()
LauncherFactory.create().execute(request, listener)
listener.summary.printTo(new PrintWriter(System.out))
listener.summary.printFailuresTo(new PrintWriter(System.out))
if(listener.summary.testsFailedCount || listener.summary.testsSucceededCount != 13) System.exit(1)
''')
    run(['-Dinferred.copy.proof=/out','-cp','/out/test-classes:'+cp,'groovy.ui.GroovyMain','/out/RunSpecs.groovy'],'spock')
    print((out/'spock.log').read_text())


if __name__=='__main__':main()
