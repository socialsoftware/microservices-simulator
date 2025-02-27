package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow

import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit

class PlanOrderTest extends SpockTest {

    def unitOfWorkService = Mock(SagaUnitOfWorkService)
    def unitOfWork = new SagaUnitOfWork(0, "TEST")
    def workflowFunctionality = Mock(WorkflowFunctionality)
    def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)
    def step1 = new SagaSyncStep("step1", { System.out.println("Step 1 executed") })
    def step2 = new SagaSyncStep("step2", { System.out.println("Step 2 executed") })
    def step3 = new SagaSyncStep("step3", { System.out.println("Step 3 executed") })
    def step4 = new SagaSyncStep("step4", { System.out.println("Step 4 executed") })
    def asyncStep1 = new SagaAsyncStep("SagaAsyncStep1", () -> CompletableFuture.runAsync(() -> {
        try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println("Async Step 1 executed");
    }))
    def asyncStep2 = new SagaAsyncStep("SagaAsyncStep2", () -> CompletableFuture.runAsync(() -> {
        try { TimeUnit.MILLISECONDS.sleep(200); } catch (InterruptedException e) { e.printStackTrace(); }
        System.out.println("Async Step 2 executed");
    }))

    def "test planOrder with no dependencies"() {
        given:
        def stepsWithNoDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithNoDependencies.put(step1, new ArrayList<>())
        stepsWithNoDependencies.put(step2, new ArrayList<>())
        stepsWithNoDependencies.put(step3, new ArrayList<>())

        when:
        def executionPlan = workflow.planOrder(stepsWithNoDependencies)

        then:
        executionPlan.getPlan().size() == 3
        executionPlan.getPlan().contains(step1)
        executionPlan.getPlan().contains(step2)
        executionPlan.getPlan().contains(step3)
    }

    def "test planOrder with linear dependencies"() {
        given:
        def stepsWithLinearDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithLinearDependencies.put(step1, new ArrayList<>())
        stepsWithLinearDependencies.put(step2, new ArrayList<>([step1]))
        stepsWithLinearDependencies.put(step3, new ArrayList<>([step2]))

        when:
        def executionPlan = workflow.planOrder(stepsWithLinearDependencies)

        then:
        executionPlan.getPlan() == [step1, step2, step3]
    }

    def "test planOrder with complex dependencies"() {
        given:
        def stepsWithComplexDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithComplexDependencies.put(step1, new ArrayList<>())
        stepsWithComplexDependencies.put(step2, new ArrayList<>([step1]))
        stepsWithComplexDependencies.put(step3, new ArrayList<>([step1]))
        stepsWithComplexDependencies.put(step4, new ArrayList<>([step2, step3]))

        when:
        def executionPlan = workflow.planOrder(stepsWithComplexDependencies)

        then:
        executionPlan.getPlan() == [step1, step2, step3, step4] || executionPlan.getPlan() == [step1, step3, step2, step4]
    }

    def "test planOrder with cyclic dependencies"() {
        given:
        def stepsWithCyclicDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithCyclicDependencies.put(step1, new ArrayList<>([step3]))
        stepsWithCyclicDependencies.put(step2, new ArrayList<>([step1]))
        stepsWithCyclicDependencies.put(step3, new ArrayList<>([step2]))

        when:
        workflow.planOrder(stepsWithCyclicDependencies)

        then:
        thrown(IllegalStateException)
    }
    
    def "test planOrder with async steps and no dependencies"() {
        given:
        def stepsWithNoDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithNoDependencies.put(asyncStep1, new ArrayList<>())
        stepsWithNoDependencies.put(asyncStep2, new ArrayList<>())

        when:
        def executionPlan = workflow.planOrder(stepsWithNoDependencies)

        then:
        executionPlan.getPlan().size() == 2
        executionPlan.getPlan().contains(asyncStep1)
        executionPlan.getPlan().contains(asyncStep2)
    }

    def "test planOrder with mixed sync and async steps and dependencies"() {
        given:
        def stepsWithMixedDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithMixedDependencies.put(step1, new ArrayList<>())
        stepsWithMixedDependencies.put(asyncStep1, new ArrayList<>([step1]))
        stepsWithMixedDependencies.put(step2, new ArrayList<>([asyncStep1]))
        stepsWithMixedDependencies.put(asyncStep2, new ArrayList<>([step2]))

        when:
        def executionPlan = workflow.planOrder(stepsWithMixedDependencies)

        then:
        executionPlan.getPlan() == [step1, asyncStep1, step2, asyncStep2]
    }

    def "test planOrder with mixed sync and async steps and complex dependencies"() {
        given:
        def stepsWithComplexMixedDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()

        stepsWithComplexMixedDependencies.put(step1, new ArrayList<>())
        stepsWithComplexMixedDependencies.put(asyncStep1, new ArrayList<>([step1]))
        stepsWithComplexMixedDependencies.put(step2, new ArrayList<>([asyncStep1]))
        stepsWithComplexMixedDependencies.put(step3, new ArrayList<>([asyncStep1]))
        stepsWithComplexMixedDependencies.put(asyncStep2, new ArrayList<>([step2, step3]))
        stepsWithComplexMixedDependencies.put(step4, new ArrayList<>([asyncStep2]))

        when:
        def executionPlan = workflow.planOrder(stepsWithComplexMixedDependencies)

        then:
        def validPlans = [
            [step1, asyncStep1, step2, step3, asyncStep2, step4],
            [step1, asyncStep1, step3, step2, asyncStep2, step4]
        ]
        validPlans.contains(executionPlan.getPlan())
    }

    def "test async steps run asynchronously"() {
        given:
        def stepsWithNoDependencies = new HashMap<FlowStep, ArrayList<FlowStep>>()
        stepsWithNoDependencies.put(asyncStep1, new ArrayList<>())
        stepsWithNoDependencies.put(asyncStep2, new ArrayList<>())

        when:
        def start = System.currentTimeMillis()
        workflow.planOrder(stepsWithNoDependencies).execute().join()
        def end = System.currentTimeMillis()
        def duration = end - start

        then:
        duration < 400 // Check if async steps are running in parallel
    }
}