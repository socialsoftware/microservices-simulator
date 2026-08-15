package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.*

import java.nio.file.Path
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState

class EventConsequenceVisitorDummyappSpec extends VisitorTestSupport {

    def setupSpec() {
        configureParser()
    }

    def 'extracts one direct dummyapp producer emission and selected saga consumer route'() {
        given:
        def state = new ApplicationAnalysisState()
        def cus = parseAllDummyappFiles()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def bridgeVisitor = new EventHandlingBridgeVisitor()
        def consequenceVisitor = new EventConsequenceVisitor()

        when:
        cus.each { indexVisitor.visit(it, state) }
        cus.each { serviceVisitor.visit(it, state) }
        cus.each { commandHandlerVisitor.visit(it, state) }
        cus.each { workflowVisitor.visit(it, state) }
        cus.each { creationSiteVisitor.visit(it, state) }
        cus.each { bridgeVisitor.visit(it, state) }
        bridgeVisitor.finish(state)
        cus.each { consequenceVisitor.visit(it, state) }
        consequenceVisitor.finish(state)

        then:
        state.eventConsequenceDiagnostics == [
                'MULTIPLE_EMISSIONS_UNSUPPORTED:com.example.dummyapp.item.coordination.CreateItemLoopedReadsFunctionalitySagas::loopedRuntimeReadStep',
                'MULTIPLE_EMISSIONS_UNSUPPORTED:com.example.dummyapp.item.coordination.CreateItemLoopedReadsFunctionalitySagas::loopedStaticReadStep'
        ]
        def matching = state.eventConsequenceCandidates.findAll {
            it.triggerSagaFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas' &&
                    it.triggerStepKey().endsWith('::getOrderStep')
        }
        matching.size() == 1
        def candidate = matching.first()
        candidate.triggerSagaFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        candidate.triggerStepKey() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas::getOrderStep'
        candidate.emissionSite().sourceServiceClassFqn() == 'com.example.dummyapp.order.service.OrderService'
        candidate.emissionSite().sourceServiceMethodSignature().startsWith('getOrder(')
        candidate.emissionSite().eventTypeFqn() == 'com.example.dummyapp.events.ItemRenamedEvent'
        candidate.selectedConsumerRoute().eventHandlingClassFqn() ==
                'com.example.dummyapp.item.notification.handling.DummyEventHandling'
        candidate.selectedConsumerRoute().eventHandlingMethodName() == 'handleItemRenamedEvents'
        candidate.selectedConsumerRoute().sagaClassFqn() ==
                'com.example.dummyapp.item.coordination.RenameItemFromEventFunctionalitySagas'

        when: 'the same event has a genuinely distinct globally selected route'
        state.eventDrivenFunctionalityInvocations.add(new EventDrivenFunctionalityInvocation(
                'com.example.dummyapp.item.notification.handling.AlternateDummyEventHandling',
                'handleItemRenamedEvents',
                'com.example.dummyapp.events.ItemRenamedEvent',
                'com.example.dummyapp.item.notification.handling.handlers.AlternateItemRenamedEventHandler',
                'com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing',
                'processItemRenamedEvent',
                'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade',
                'cancelOrderFromItem',
                'com.example.dummyapp.order.coordination.CancelOrderFromItemFunctionalitySagas',
                [], []))
        consequenceVisitor.finish(state)

        then:
        state.eventConsequenceCandidates.findAll {
            it.triggerStepKey() ==
                    'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas::getOrderStep'
        }*.selectedConsumerRoute()*.eventHandlingClassFqn().toSet() == [
                'com.example.dummyapp.item.notification.handling.DummyEventHandling',
                'com.example.dummyapp.item.notification.handling.AlternateDummyEventHandling'
        ] as Set

        when: 'the same event routes recursively to its producer saga'
        state.eventDrivenFunctionalityInvocations.add(new EventDrivenFunctionalityInvocation(
                'com.example.dummyapp.item.notification.handling.RecursiveDummyEventHandling',
                'handleRecursively',
                'com.example.dummyapp.events.ItemRenamedEvent',
                'com.example.dummyapp.item.notification.handling.handlers.ItemRenamedEventHandler',
                'com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing',
                'processItemRenamedEvent',
                'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade',
                'createItem',
                'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas',
                [], []))
        consequenceVisitor.finish(state)

        then:
        state.eventConsequenceDiagnostics.any {
            it.startsWith('RECURSIVE_EVENT_ROUTE_UNSUPPORTED:com.example.dummyapp.item.notification.handling.RecursiveDummyEventHandling')
        }
        !state.eventConsequenceCandidates.any {
            it.selectedConsumerRoute().eventHandlingClassFqn() ==
                    'com.example.dummyapp.item.notification.handling.RecursiveDummyEventHandling'
        }
    }

    def 'rejects wrong event receiver wrong unit of work and mixed compensation origin'() {
        given:
        def state = new ApplicationAnalysisState()
        def cus = parseAllDummyappFiles()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def bridgeVisitor = new EventHandlingBridgeVisitor()
        def consequenceVisitor = new EventConsequenceVisitor()
        cus.each { indexVisitor.visit(it, state) }
        cus.each { serviceVisitor.visit(it, state) }
        cus.each { commandHandlerVisitor.visit(it, state) }
        cus.each { workflowVisitor.visit(it, state) }
        cus.each { creationSiteVisitor.visit(it, state) }
        cus.each { bridgeVisitor.visit(it, state) }
        bridgeVisitor.finish(state)

        and:
        def orderService = state.services.find {
            it.fqn == 'com.example.dummyapp.order.service.OrderService'
        }
        def handler = new CommandHandlerBuildingBlock(
                Path.of('dummyapp-negative-handler.java'), 'fixture', 'fixture.NegativeHandler', 'Order')
        def wrongReceiverSignature = orderService.methodAccessPolicies.keySet().find {
            it.startsWith('emitThroughWrongReceiver(')
        }
        def wrongUnitOfWorkSignature = orderService.methodAccessPolicies.keySet().find {
            it.startsWith('emitWithWrongUnitOfWork(')
        }
        handler.addCommandDispatch('fixture.WrongReceiverCommand',
                new CommandDispatchInfo(orderService, wrongReceiverSignature, 'Order'))
        handler.addCommandDispatch('fixture.WrongUnitOfWorkCommand',
                new CommandDispatchInfo(orderService, wrongUnitOfWorkSignature, 'Order'))
        state.commandHandlers.add(handler)

        def saga = new SagaFunctionalityBuildingBlock(
                Path.of('dummyapp-negative-saga.java'), 'fixture', 'fixture.NegativeEventSaga')
        saga.addStep(singleDispatchStep('fixture.NegativeEventSaga', 'wrongReceiverStep',
                'fixture.WrongReceiverCommand', DispatchPhase.FORWARD))
        saga.addStep(singleDispatchStep('fixture.NegativeEventSaga', 'wrongUnitOfWorkStep',
                'fixture.WrongUnitOfWorkCommand', DispatchPhase.FORWARD))
        def mixed = singleDispatchStep('fixture.NegativeEventSaga', 'mixedCompensationStep',
                'com.example.dummyapp.order.commands.GetOrderCommand', DispatchPhase.FORWARD)
        mixed.addDispatch(new StepDispatchFootprint(
                'fixture.NegativeEventSaga::mixedCompensationStep',
                'com.example.dummyapp.order.commands.GetOrderCommand', 'Order', AccessPolicy.READ,
                DispatchPhase.COMPENSATION,
                new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1)))
        saga.addStep(mixed)
        state.sagas.add(saga)

        when:
        cus.each { consequenceVisitor.visit(it, state) }
        consequenceVisitor.finish(state)

        then:
        state.eventConsequenceDiagnostics.containsAll([
                'UNSUPPORTED_EVENT_RECEIVER:fixture.NegativeEventSaga::wrongReceiverStep',
                'UNSUPPORTED_EVENT_UNIT_OF_WORK_ARGUMENT:fixture.NegativeEventSaga::wrongUnitOfWorkStep',
                'COMPENSATION_ORIGIN_EVENT_UNSUPPORTED:fixture.NegativeEventSaga::mixedCompensationStep'
        ])
        !state.eventConsequenceCandidates.any { it.triggerSagaFqn() == 'fixture.NegativeEventSaga' }
    }

    private static SagaStepBuildingBlock singleDispatchStep(String sagaFqn,
                                                            String stepName,
                                                            String commandFqn,
                                                            DispatchPhase phase) {
        def stepKey = sagaFqn + '::' + stepName
        def step = new SagaStepBuildingBlock(
                Path.of('dummyapp-negative-saga.java'), 'fixture', stepKey, stepName)
        step.addDispatch(new StepDispatchFootprint(
                stepKey, commandFqn, 'Order', AccessPolicy.READ, phase,
                new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1)))
        step
    }
}
