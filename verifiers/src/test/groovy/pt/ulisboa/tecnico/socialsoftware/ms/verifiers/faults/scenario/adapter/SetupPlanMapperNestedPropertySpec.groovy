package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.*

class SetupPlanMapperNestedPropertySpec extends VisitorTestSupport {
    def 'dummyapp nested references bind only the approved path to the exact producer'() {
        given:
        configureParser()
        def state = new ApplicationAnalysisState()
        def sources = parseAllDummyappFiles()
        sources.each { new WorkflowFunctionalityVisitor().visit(it, state) }
        sources.each { new WorkflowFunctionalityCreationSiteVisitor().visit(it, state) }
        def index = new GroovySourceIndex()
        index.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(index, state)
        def targets = state.groovyFullTraceResults.findAll {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.sourceMethodName() == 'nested result paths preserve the exact facade producer' &&
                    it.sourceBindingName() in ['supported', 'unsupported']
        }

        expect:
        targets.size() == 2
        targets.each { target ->
            def argument = target.constructorArguments()[1]
            def reference = argument.producerReference()
            assert reference != null
            def producer = state.groovyFacadeSetupActionTraces.find {
                it.sourceOccurrence() == reference.occurrenceId()
            }
            assert producer != null
            def plan = new SetupPlanMapper().map([producer],
                    [new SetupPlanMapper.ParticipantSource(target.sourceBindingName(), [argument])])
            assert plan.participantBindings().size() == 1
            def value = plan.participantBindings()[0].value()
            if (target.sourceBindingName() == 'supported') {
                assert reference.propertyPath() == ['quiz', 'aggregateId']
                assert value.kind() == SetupValueKind.ACTION_RESULT_PROPERTY
                assert value.actionId() == plan.actions()[0].actionId()
                assert value.propertyName() == 'quiz.aggregateId'
                assert value.blockers().isEmpty()
            } else {
                assert reference.propertyPath() == ['quiz', 'orderId']
                assert value.blockers().any { it.startsWith('UNSUPPORTED_SETUP_PROPERTY_PATH:') }
            }
        }
    }

    def 'parsed dummyapp collection binds an exact earlier result and excludes the measured target'() {
        given:
        configureParser()
        def state = new ApplicationAnalysisState()
        def sources = parseAllDummyappFiles()
        sources.each { new WorkflowFunctionalityVisitor().visit(it, state) }
        sources.each { new WorkflowFunctionalityCreationSiteVisitor().visit(it, state) }
        def index = new GroovySourceIndex()
        index.parse(resolveProjectPath('applications', 'dummyapp', 'src', 'test', 'groovy'))
        new GroovyConstructorInputTraceVisitor().visit(index, state)
        def feature = 'nested facade result collection feeds a later target without replay'
        def target = state.groovyFullTraceResults.find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.callContextMethodName() == feature &&
                    it.sagaClassFqn() == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        def producer = state.groovyFacadeSetupActionTraces.find {
            it.sourceClassFqn() == 'com.example.dummyapp.GroovySagaTracingSpec' &&
                    it.callContextMethodName() == feature && it.methodName() == 'createItem'
        }
        assert target.constructorArguments()[2].recipe().children()*.sourceReference().any()

        when:
        def plan = new SetupPlanMapper().map([producer], [
                new SetupPlanMapper.ParticipantSource('dummyapp-input', target.constructorArguments())
        ])

        then:
        target != null
        producer != null
        producer.occurrence().orderIndex() < target.occurrence().orderIndex()
        new SetupPlanValidator().validate(plan).valid()
        !plan.actions()*.sourceOccurrence().contains(target.occurrence().occurrenceId())
        def value = plan.participantBindings().find {
            it.inputVariantId() == 'dummyapp-input' && it.argumentIndex() == 2
        }.value()
        value.kind() == SetupValueKind.LIST
        value.elements()*.kind() == [SetupValueKind.ACTION_RESULT]
        value.elements()*.propertyName() == [null]
        value.elements()*.actionId() == [plan.actions().first().actionId()]
    }
}
