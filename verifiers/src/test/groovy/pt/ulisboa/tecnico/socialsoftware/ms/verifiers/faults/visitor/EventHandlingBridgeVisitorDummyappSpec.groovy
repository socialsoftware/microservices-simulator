package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventDrivenArgumentSourceKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState

class EventHandlingBridgeVisitorDummyappSpec extends VisitorTestSupport {

    def setupSpec() {
        configureParser()
    }

    def 'extracts dummyapp event handling to saga bridge'() {
        given:
        def state = new ApplicationAnalysisState()
        def indexVisitor = new CommandHandlerIndexVisitor()
        def serviceVisitor = new ServiceVisitor()
        def commandHandlerVisitor = new CommandHandlerVisitor()
        def workflowVisitor = new WorkflowFunctionalityVisitor()
        def creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()
        def eventBridgeVisitor = new EventHandlingBridgeVisitor()
        def cus = parseAllDummyappFiles()

        when:
        cus.each { cu -> indexVisitor.visit(cu, state) }
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
        cus.each { cu -> creationSiteVisitor.visit(cu, state) }
        cus.each { cu -> eventBridgeVisitor.visit(cu, state) }
        eventBridgeVisitor.finish(state)

        then:
        def bridge = state.eventDrivenFunctionalityInvocations.find {
            it.eventHandlingClassFqn() == 'com.example.dummyapp.item.notification.handling.DummyEventHandling' &&
                    it.eventHandlingMethodName() == 'handleItemRenamedEvents' &&
                    it.sagaClassFqn() == 'com.example.dummyapp.item.coordination.RenameItemFromEventFunctionalitySagas'
        }
        bridge != null
        bridge.eventTypeFqn() == 'com.example.dummyapp.events.ItemRenamedEvent'
        bridge.eventHandlerClassFqn() == 'com.example.dummyapp.item.notification.handling.handlers.ItemRenamedEventHandler'
        bridge.eventProcessingClassFqn() == 'com.example.dummyapp.item.coordination.eventProcessing.ItemEventProcessing'
        bridge.eventProcessingMethodName() == 'processItemRenamedEvent'
        bridge.facadeClassFqn() == 'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade'
        bridge.facadeMethodName() == 'renameItemFromEvent'
        bridge.argumentSources()*.kind().containsAll([
                EventDrivenArgumentSourceKind.INJECTABLE_FIELD,
                EventDrivenArgumentSourceKind.EVENT_SUBSCRIBER_AGGREGATE_ID,
                EventDrivenArgumentSourceKind.EVENT_FIELD,
                EventDrivenArgumentSourceKind.RUNTIME_CALL
        ])
        bridge.argumentSources()*.provenance().any { it.contains('EVENT_FIELD:ItemRenamedEvent.updatedName') }
        bridge.argumentSources()*.provenance().any { it.contains('EVENT_FIELD:ItemRenamedEvent.publisherAggregateId') }
        bridge.argumentSources()*.provenance().any { it.contains('EVENT_FIELD:ItemRenamedEvent.publisherAggregateVersion') }
        bridge.resolutionNotes().any { it.contains('resolved via event handler DummyEventHandling.handleItemRenamedEvents()') }
    }
}
