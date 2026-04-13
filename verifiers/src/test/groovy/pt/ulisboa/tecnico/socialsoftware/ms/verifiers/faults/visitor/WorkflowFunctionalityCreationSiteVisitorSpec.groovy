package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSourceKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import spock.lang.Shared

class WorkflowFunctionalityCreationSiteVisitorSpec extends VisitorTestSupport {

    @Shared ApplicationAnalysisState state = new ApplicationAnalysisState()
    @Shared ServiceVisitor serviceVisitor = new ServiceVisitor()
    @Shared CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor()
    @Shared WorkflowFunctionalityVisitor workflowVisitor = new WorkflowFunctionalityVisitor()
    @Shared WorkflowFunctionalityCreationSiteVisitor creationSiteVisitor = new WorkflowFunctionalityCreationSiteVisitor()

    def setupSpec() {
        configureParser()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
        cus.each { cu -> workflowVisitor.visit(cu, state) }
        cus.each { cu -> creationSiteVisitor.visit(cu, state) }
    }

    def "detects the order functionality saga creation site"() {
        expect:
        def site = state.sagaCreationSites.find { site ->
            site.classFqn() == 'com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade' &&
                    site.methodName() == 'createOrder' &&
                    site.sagaClassFqn() == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }

        site != null
        site.argumentSources().size() == 4
        site.argumentSources().collect { it.kind() } == [
                WorkflowCreationArgumentSourceKind.FIELD_REFERENCE,
                WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE,
                WorkflowCreationArgumentSourceKind.METHOD_PARAMETER,
                WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE
        ]
        site.argumentSources()[0].name() == 'sagaUnitOfWorkService'
        site.argumentSources()[1].name() == 'unitOfWork'
        site.argumentSources()[1].text() == 'sagaUnitOfWorkService.createUnitOfWork("createOrder")'
        site.argumentSources()[2].parameterIndex() == 0
        site.argumentSources()[2].name() == 'customerId'
        site.argumentSources()[3].kind() == WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE
        site.argumentSources()[3].name() == 'customerIdCopy'
        site.argumentSources()[3].text() == 'customerId'
    }

    def "detects the item functionality saga creation site"() {
        expect:
        def site = state.sagaCreationSites.find { site ->
            site.classFqn() == 'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade' &&
                    site.methodName() == 'createItem' &&
                    site.sagaClassFqn() == 'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas'
        }

        site != null
        site.argumentSources().size() == 4
        site.argumentSources().collect { it.kind() } == [
                WorkflowCreationArgumentSourceKind.FIELD_REFERENCE,
                WorkflowCreationArgumentSourceKind.METHOD_PARAMETER,
                WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE,
                WorkflowCreationArgumentSourceKind.FIELD_REFERENCE
        ]
        site.argumentSources()[0].name() == 'sagaUnitOfWorkService'
        site.argumentSources()[1].parameterIndex() == 0
        site.argumentSources()[1].name() == 'itemDto'
        site.argumentSources()[2].name() == 'unitOfWork'
        site.argumentSources()[2].text() == 'sagaUnitOfWorkService.createUnitOfWork("createItem")'
        site.argumentSources()[3].name() == 'commandGateway'
    }

    def "does not report saga classes as creation sites"() {
        expect:
        state.sagaCreationSites.size() == 2
        state.sagaCreationSites.every { site ->
            !state.sagas.any { saga -> saga.fqn == site.classFqn() }
        }
    }
}
