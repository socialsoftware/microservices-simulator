package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

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
        state.sagaCreationSites.any { site ->
            site.classFqn() == 'com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade' &&
                    site.methodName() == 'createOrder' &&
                    site.sagaClassFqn() == 'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas'
        }
    }

    def "does not report saga classes as creation sites"() {
        expect:
        state.sagaCreationSites.size() == 1
        state.sagaCreationSites.every { site ->
            !state.sagas.any { saga -> saga.fqn == site.classFqn() }
        }
    }
}
