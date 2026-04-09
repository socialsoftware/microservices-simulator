package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import org.slf4j.LoggerFactory
import spock.lang.Shared

class WorkflowFunctionalityVisitorSpec extends VisitorTestSupport {

    @Shared ApplicationAnalysisState state = new ApplicationAnalysisState()
    @Shared ServiceVisitor serviceVisitor = new ServiceVisitor()
    @Shared CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor()
    @Shared WorkflowFunctionalityVisitor workflowVisitor = new WorkflowFunctionalityVisitor()
    @Shared ListAppender appender = new ListAppender()
    @Shared Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityVisitor) as Logger

    def setupSpec() {
        logger.addAppender(appender)
        appender.start()
        configureParser()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
    }

    def "WorkflowFunctionalityVisitor finds CreateItemFunctionalitySagas"() {
        expect:
        state.sagas.any { it.fqn.contains('CreateItemFunctionalitySagas') }
    }

    def "WorkflowFunctionalityVisitor finds field-injected workflows"() {
        expect:
        state.sagas.any { it.fqn.contains('CreateItemFieldInjectionFunctionalitySagas') }
    }

    def "CreateItemFunctionalitySagas has two steps"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }

        expect:
        saga != null
        saga.steps.size() == 2
    }

    def "getOrderStep is a READ on Order"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'getOrderStep' }

        expect:
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys.isEmpty()
        with(step.dispatches.first()) {
            aggregateName() == 'Order'
            accessPolicy() == AccessPolicy.READ
        }
    }

    def "createItemStep is a WRITE on Item"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'createItemStep' }

        expect:
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys == ['CreateItemFunctionalitySagas::getOrderStep'] as Set
        with(step.dispatches.first()) {
            aggregateName() == 'Item'
            accessPolicy() == AccessPolicy.WRITE
        }
    }

    def "compensation step is extracted with compensation phase"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemCompensationFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'createItemStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 2
        step.dispatches.count { it.phase() == DispatchPhase.FORWARD } == 1
        step.dispatches.count { it.phase() == DispatchPhase.COMPENSATION } == 1

        with(step.dispatches.find { it.phase() == DispatchPhase.COMPENSATION }) {
            aggregateName() == 'Item'
            accessPolicy() == AccessPolicy.WRITE
        }
    }

    def "WorkflowFunctionalityVisitor captures predecessor edges"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def rootStep = saga.steps.find { it.name == 'rootStep' }
        def prepareStep = saga.steps.find { it.name == 'prepareStep' }
        def splitStep = saga.steps.find { it.name == 'splitStep' }

        expect:
        saga != null
        rootStep.predecessorStepKeys.isEmpty()
        prepareStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
        splitStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
    }

    def "WorkflowFunctionalityVisitor captures fan-in dependencies"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def mergeStep = saga.steps.find { it.name == 'mergeStep' }

        expect:
        saga != null
        mergeStep.predecessorStepKeys == [
                'CreateItemDependencyGraphFunctionalitySagas::prepareStep',
                'CreateItemDependencyGraphFunctionalitySagas::splitStep'
        ] as Set
    }

    def "WorkflowFunctionalityVisitor ignores unresolved dependency references and warns"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemDependencyGraphFunctionalitySagas') }
        assert saga != null
        def conservativeStep = saga.steps.find { it.name == 'conservativeStep' }

        expect:
        conservativeStep.predecessorStepKeys == ['CreateItemDependencyGraphFunctionalitySagas::rootStep'] as Set
        appender.list.any { event ->
            event.level.toString() == 'WARN' &&
                    event.formattedMessage.contains('Unknown dependency reference')
        }
    }

    def cleanupSpec() {
        logger.detachAppender(appender)
    }

    def "fieldInjectedStep is a READ on Order"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemFieldInjectionFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'fieldInjectedStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        step.predecessorStepKeys.isEmpty()
        with(step.dispatches.first()) {
            aggregateName() == 'Order'
            accessPolicy() == AccessPolicy.READ
        }
    }

    def "looped static dispatch is STATIC_REPEAT with count 3"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemLoopedReadsFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'loopedStaticReadStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        with(step.dispatches.first().multiplicity()) {
            kind() == DispatchMultiplicityKind.STATIC_REPEAT
            staticCount() == 3
        }
    }

    def "looped runtime dispatch is PARAMETRIC_REPEAT"() {
        given:
        def saga = state.sagas.find { it.fqn.contains('CreateItemLoopedReadsFunctionalitySagas') }
        def step = saga.steps.find { it.name == 'loopedRuntimeReadStep' }

        expect:
        saga != null
        step != null
        step.dispatches.size() == 1
        with(step.dispatches.first().multiplicity()) {
            kind() == DispatchMultiplicityKind.PARAMETRIC_REPEAT
        }
    }
}
