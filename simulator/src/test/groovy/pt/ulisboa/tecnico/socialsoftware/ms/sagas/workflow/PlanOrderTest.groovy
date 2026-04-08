package pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow

import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaWorkflow

class PlanOrderTest extends SpockTest {

    def unitOfWorkService = Mock(SagaUnitOfWorkService)
    def unitOfWork = new SagaUnitOfWork(0, "TEST")
    def workflowFunctionality = Mock(WorkflowFunctionality)
    def workflow = new SagaWorkflow(workflowFunctionality, unitOfWorkService, unitOfWork)
    def step1 = new SagaStep("step1", { System.out.println("Step 1 executed") })
    def step2 = new SagaStep("step2", { System.out.println("Step 2 executed") })
    def step3 = new SagaStep("step3", { System.out.println("Step 3 executed") })
    def step4 = new SagaStep("step4", { System.out.println("Step 4 executed") })

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
}