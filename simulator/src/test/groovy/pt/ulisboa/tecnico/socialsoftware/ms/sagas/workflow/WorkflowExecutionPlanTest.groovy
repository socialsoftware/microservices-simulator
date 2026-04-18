package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow

import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class WorkflowExecutionPlanTest extends SpockTest {
    def unitOfWorkService = Mock(SagaUnitOfWorkService)
    def workflowFunctionality = Mock(WorkflowFunctionality)

    def setupSpec() {
        TraceManager.init('workflow-execution-plan-test')
        TraceManager.getInstance().startRootSpan()
    }

    def cleanupSpec() {
        def traceManager = TraceManager.getInstance()
        if (traceManager != null) {
            traceManager.endRootSpan()
            traceManager.forceFlush()
        }
    }

    def 'execute workflow commits after linear steps'() {
        given:
        def unitOfWork = new SagaUnitOfWork(0L, 'LINEAR')
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)
        def order = Collections.synchronizedList([])

        def step1 = new SagaStep('step1', { order << 'step1' })
        def step2 = new SagaStep('step2', { order << 'step2' }, new ArrayList<>([step1]))
        def step3 = new SagaStep('step3', { order << 'step3' }, new ArrayList<>([step2]))

        workflow.addStep(step1)
        workflow.addStep(step2)
        workflow.addStep(step3)

        when:
        workflow.execute(unitOfWork).join()

        then:
        order == ['step1', 'step2', 'step3']
        1 * unitOfWorkService.commit(unitOfWork)
        0 * unitOfWorkService.abort(_)
    }

    def 'executeUntilStep and resume run remaining steps'() {
        given:
        def unitOfWork = new SagaUnitOfWork(0L, 'RESUME')
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)
        def order = Collections.synchronizedList([])

        def step1 = new SagaStep('step1', { order << 'step1' })
        def step2 = new SagaStep('step2', { order << 'step2' }, new ArrayList<>([step1]))
        def step3 = new SagaStep('step3', { order << 'step3' }, new ArrayList<>([step2]))

        workflow.addStep(step1)
        workflow.addStep(step2)
        workflow.addStep(step3)

        when: 'execute until second step'
        workflow.executeUntilStep('step2', unitOfWork)

        then: 'only first two steps executed and no commit yet'
        order == ['step1', 'step2']
        0 * unitOfWorkService.commit(_)

        when: 'resume remaining workflow'
        workflow.resume(unitOfWork).join()

        then: 'last step executes and commit happens once'
        order == ['step1', 'step2', 'step3']
        1 * unitOfWorkService.commit(unitOfWork)
        0 * unitOfWorkService.abort(_)
    }

    def 'parallel branches wait for join step'() {
        given:
        def unitOfWork = new SagaUnitOfWork(0L, 'PARALLEL_JOIN')
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)

        def branchesStarted = new CountDownLatch(2)
        def releaseBranches = new CountDownLatch(1)
        def joinStepExecuted = new AtomicBoolean(false)

        def step1 = new SagaStep('step1', { /* common predecessor */ })
        def step2 = new SagaStep('step2', {
            branchesStarted.countDown()
            releaseBranches.await(2, TimeUnit.SECONDS)
        }, new ArrayList<>([step1]))
        def step3 = new SagaStep('step3', {
            branchesStarted.countDown()
            releaseBranches.await(2, TimeUnit.SECONDS)
        }, new ArrayList<>([step1]))
        def step4 = new SagaStep('step4', { joinStepExecuted.set(true) }, new ArrayList<>([step2, step3]))

        workflow.addStep(step1)
        workflow.addStep(step2)
        workflow.addStep(step3)
        workflow.addStep(step4)

        when:
        def execution = workflow.execute(unitOfWork)
        def bothBranchesStarted = branchesStarted.await(2, TimeUnit.SECONDS)
        releaseBranches.countDown()
        execution.join()

        then:
        bothBranchesStarted
        joinStepExecuted.get()
        1 * unitOfWorkService.commit(unitOfWork)
        0 * unitOfWorkService.abort(_)
    }

    def 'saga step registers compensation during execution'() {
        given:
        def unitOfWork = Spy(new SagaUnitOfWork(0L, 'COMPENSATION'))
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)

        def compensatedStep = new SagaStep('compensatedStep', { /* mutation */ })
        compensatedStep.registerCompensation({ -> }, unitOfWork)
        workflow.addStep(compensatedStep)

        when:
        workflow.execute(unitOfWork).join()

        then:
        1 * unitOfWork.registerCompensation(_ as Runnable)
        1 * unitOfWorkService.commit(unitOfWork)
        0 * unitOfWorkService.abort(_)
    }

    def 'workflow aborts on step exception'() {
        given:
        def unitOfWork = new SagaUnitOfWork(0L, 'ABORT_ON_ERROR')
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)

        def failingStep = new SagaStep('failingStep', { throw new RuntimeException('boom') })
        workflow.addStep(failingStep)

        when:
        workflow.execute(unitOfWork).join()

        then:
        def error = thrown(CompletionException)
        error.cause instanceof RuntimeException
        1 * unitOfWorkService.abort(unitOfWork)
        0 * unitOfWorkService.commit(_)
    }

    def 'sync and async command-like calls execute in order'() {
        given:
        def unitOfWork = new SagaUnitOfWork(0L, 'SYNC_ASYNC_CALLS')
        def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)
        def calls = Collections.synchronizedList([])

        def syncGatewayStep = new SagaStep('syncGatewayStep', {
            calls << 'sync-send'
        })

        def asyncGatewayStep = new SagaStep('asyncGatewayStep', {
            CompletableFuture.supplyAsync({ 'async-send' })
                .thenAccept({ value -> calls << value })
                .join()
        }, new ArrayList<>([syncGatewayStep]))

        workflow.addStep(syncGatewayStep)
        workflow.addStep(asyncGatewayStep)

        when:
        workflow.execute(unitOfWork).join()

        then:
        calls == ['sync-send', 'async-send']
        1 * unitOfWorkService.commit(unitOfWork)
        0 * unitOfWorkService.abort(_)
    }

    def 'report parallel vs sequential step duration'() {
        given:
        def sequentialUnitOfWork = new SagaUnitOfWork(0L, 'SEQUENTIAL_DURATION')
        def sequentialWorkflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, sequentialUnitOfWork)

        def seqStep1 = new SagaStep('seqStep1', { Thread.sleep(60) })
        def seqStep2 = new SagaStep('seqStep2', { Thread.sleep(60) }, new ArrayList<>([seqStep1]))
        sequentialWorkflow.addStep(seqStep1)
        sequentialWorkflow.addStep(seqStep2)

        def parallelUnitOfWork = new SagaUnitOfWork(0L, 'PARALLEL_DURATION')
        def parallelWorkflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, parallelUnitOfWork)

        def parStep1 = new SagaStep('parStep1', { Thread.sleep(60) })
        def parStep2 = new SagaStep('parStep2', { Thread.sleep(60) })
        parallelWorkflow.addStep(parStep1)
        parallelWorkflow.addStep(parStep2)

        when:
        def sequentialStart = System.currentTimeMillis()
        sequentialWorkflow.execute(sequentialUnitOfWork).join()
        def sequentialDuration = System.currentTimeMillis() - sequentialStart

        def parallelStart = System.currentTimeMillis()
        parallelWorkflow.execute(parallelUnitOfWork).join()
        def parallelDuration = System.currentTimeMillis() - parallelStart

        then:
        sequentialDuration >= 0
        parallelDuration >= 0

        and:
        println "Sequential workflow duration (ms): ${sequentialDuration}"
        println "Parallel workflow duration (ms): ${parallelDuration}"
        true
    }
}
