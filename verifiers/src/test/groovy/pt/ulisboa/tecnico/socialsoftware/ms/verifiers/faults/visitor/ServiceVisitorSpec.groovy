package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import spock.lang.Shared

class ServiceVisitorSpec extends VisitorTestSupport {

    @Shared ApplicationAnalysisState state = new ApplicationAnalysisState()
    @Shared CommandHandlerIndexVisitor indexVisitor = new CommandHandlerIndexVisitor()
    @Shared ServiceVisitor visitor = new ServiceVisitor()

    def setupSpec() {
        configureParser()
        def cus = parseAllDummyappFiles()
        cus.each { cu -> indexVisitor.visit(cu, state) }
        cus.each { cu -> visitor.visit(cu, state) }
    }

    // -----------------------------------------------------------------------
    // Item domain — Dev A: constructor injection, SLF4J
    // -----------------------------------------------------------------------

    def "ItemService is detected as a service"() {
        expect:
        state.services.any { it.fqn.contains('ItemService') }
    }

    def "ItemService.getItem is classified as READ"() {
        given:
        def svc = state.services.find { it.fqn.contains('ItemService') }
        expect:
        svc.getAccessPolicy('getItem') == AccessPolicy.READ
    }

    def "ItemService.createItem is classified as WRITE via NameExpr unitOfWorkService.registerChanged"() {
        given:
        def svc = state.services.find { it.fqn.contains('ItemService') }
        expect:
        svc.getAccessPolicy('createItem') == AccessPolicy.WRITE
    }

    def "ItemService.updateItem is classified as WRITE via FieldAccessExpr this.unitOfWorkService.registerChanged"() {
        // Regression: FieldAccessExpr scope (this.field) must also be detected
        given:
        def svc = state.services.find { it.fqn.contains('ItemService') }
        expect:
        svc.getAccessPolicy('updateItem') == AccessPolicy.WRITE
    }

    def "ItemService.deleteItem is classified as WRITE"() {
        given:
        def svc = state.services.find { it.fqn.contains('ItemService') }
        expect:
        svc.getAccessPolicy('deleteItem') == AccessPolicy.WRITE
    }

    def "AliasUnitOfWorkItemService.aliasRegisterChanged is classified as WRITE through a local alias"() {
        given:
        def svc = state.services.find { it.fqn.contains('AliasUnitOfWorkItemService') }
        expect:
        svc.getAccessPolicy('aliasRegisterChanged') == AccessPolicy.WRITE
    }

    def "GetterBasedUnitOfWorkItemService.getterRegisterChanged is classified as WRITE through a getter"() {
        given:
        def svc = state.services.find { it.fqn.contains('GetterBasedUnitOfWorkItemService') }
        expect:
        svc.getAccessPolicy('getterRegisterChanged') == AccessPolicy.WRITE
    }

    def "HelperMarkChangedItemService.helperRegisterChanged is classified as WRITE through one helper method"() {
        given:
        def svc = state.services.find { it.fqn.contains('HelperMarkChangedItemService') }
        expect:
        svc.getAccessPolicy('helperRegisterChanged') == AccessPolicy.WRITE
    }

    // -----------------------------------------------------------------------
    // Order domain — Dev B: @Autowired-only injection, JUL logger
    // -----------------------------------------------------------------------

    def "OrderService is detected despite @Autowired-only injection"() {
        expect:
        state.services.any { it.fqn.contains('OrderService') }
    }

    def "OrderService.getOrder is classified as READ"() {
        given:
        def svc = state.services.find { it.fqn.contains('OrderService') }
        expect:
        svc != null
        svc.getAccessPolicy('getOrder') == AccessPolicy.READ
    }

    def "OrderService.placeOrder is classified as WRITE"() {
        given:
        def svc = state.services.find { it.fqn.contains('OrderService') }
        expect:
        svc != null
        svc.getAccessPolicy('placeOrder') == AccessPolicy.WRITE
    }

    def "OrderService.cancelOrder is classified as WRITE"() {
        given:
        def svc = state.services.find { it.fqn.contains('OrderService') }
        expect:
        svc != null
        svc.getAccessPolicy('cancelOrder') == AccessPolicy.WRITE
    }

    // -----------------------------------------------------------------------
    // Domain service vs coordination facade distinction
    // -----------------------------------------------------------------------

    def "does not classify coordination facade as a domain service"() {
        // OrderFunctionalitiesFacade is @Service + SagaUnitOfWorkService but has no
        // CommandHandler dispatch path. The pipeline must not admit it to state.services.
        expect:
        !state.services.any { it.fqn.contains('OrderFunctionalitiesFacade') }
    }

    def "does not classify a lookalike UnitOfWorkService helper as a domain service"() {
        expect:
        !state.services.any { it.fqn.contains('SubstringTrapService') }
    }

    def "classifies command-handler dispatch target as a domain service"() {
        // OrderService is the concrete service injected by OrderCommandHandler.
        // It must appear in state.services after the pipeline runs.
        expect:
        state.services.any { it.fqn.contains('OrderService') }
    }
}
