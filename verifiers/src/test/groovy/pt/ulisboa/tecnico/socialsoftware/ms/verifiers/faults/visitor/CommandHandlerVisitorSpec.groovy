package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState
import spock.lang.Shared

/**
 * Spock specs for CommandHandlerVisitor against the dummyapp fixture.
 *
 * Setup: three-phase pipeline mirrors production order —
 *   1. CommandHandlerIndexVisitor (collect dispatch-target FQNs)
 *   2. ServiceVisitor (admit only domain services)
 *   3. CommandHandlerVisitor (build dispatch map)
 */
class CommandHandlerVisitorSpec extends VisitorTestSupport {

    @Shared ApplicationAnalysisState state = new ApplicationAnalysisState()
    @Shared CommandHandlerIndexVisitor indexVisitor = new CommandHandlerIndexVisitor()
    @Shared ServiceVisitor serviceVisitor = new ServiceVisitor()
    @Shared CommandHandlerVisitor commandHandlerVisitor = new CommandHandlerVisitor()

    def setupSpec() {
        configureParser()
        def cus = parseAllDummyappFiles()
        // Phase 1: collect dispatch-target FQNs so ServiceVisitor admits only domain services
        cus.each { cu -> indexVisitor.visit(cu, state) }
        // Phase 2: classify domain services
        cus.each { cu -> serviceVisitor.visit(cu, state) }
        // Phase 3: build command dispatch map
        cus.each { cu -> commandHandlerVisitor.visit(cu, state) }
    }

    // -----------------------------------------------------------------------
    // Item domain — ItemCommandHandler: @Autowired service, switch with default, SLF4J
    // -----------------------------------------------------------------------

    def "CommandHandlerVisitor finds ItemCommandHandler"() {
        expect:
        state.commandHandlers.any { it.fqn.endsWith('.ItemCommandHandler') }
    }

    def "ItemCommandHandler aggregate type name is Item"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.ItemCommandHandler') }
        expect:
        handler != null
        handler.aggregateTypeName == 'Item'
    }

    def "ItemCommandHandler maps GetItemCommand to ItemService"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.ItemCommandHandler') }
        def dispatch = handler.commandDispatch['com.example.dummyapp.item.commands.GetItemCommand']
        expect:
        dispatch != null
        dispatch.serviceMethodName() == 'getItem'
        dispatch.serviceClassName().contains('ItemService')
        dispatch.accessPolicy() == AccessPolicy.READ
    }

    def "ItemCommandHandler maps CreateItemCommand to ItemService"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.ItemCommandHandler') }
        def dispatch = handler.commandDispatch['com.example.dummyapp.item.commands.CreateItemCommand']
        expect:
        dispatch != null
        dispatch.serviceMethodName() == 'createItem'
        dispatch.serviceClassName().contains('ItemService')
        dispatch.accessPolicy() == AccessPolicy.WRITE
    }

    def "CommandHandlerVisitor handles @Autowired service injection in ItemCommandHandler"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.ItemCommandHandler') }
        expect:
        handler.commandDispatch.size() > 0
        handler.commandDispatch.values().every { dispatch ->
            dispatch.serviceClassName().contains('ItemService')
        }
    }

    // -----------------------------------------------------------------------
    // Order domain — OrderCommandHandler: constructor injection, no default branch, JUL
    // -----------------------------------------------------------------------

    def "CommandHandlerVisitor finds OrderCommandHandler"() {
        expect:
        state.commandHandlers.any { it.fqn.endsWith('.OrderCommandHandler') }
    }

    def "OrderCommandHandler aggregate type name is Order"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.OrderCommandHandler') }
        expect:
        handler != null
        handler.aggregateTypeName == 'Order'
    }

    def "OrderCommandHandler maps PlaceOrderCommand to OrderService"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.OrderCommandHandler') }
        def dispatch = handler.commandDispatch['com.example.dummyapp.order.commands.PlaceOrderCommand']
        expect:
        dispatch != null
        dispatch.serviceMethodName() == 'placeOrder'
        dispatch.serviceClassName().contains('OrderService')
        dispatch.accessPolicy() == AccessPolicy.WRITE
    }

    def "CommandHandlerVisitor handles constructor-injected service in OrderCommandHandler"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.OrderCommandHandler') }
        expect:
        handler.commandDispatch.size() > 0
        handler.commandDispatch.values().every { dispatch ->
            dispatch.serviceClassName().contains('OrderService')
        }
    }

    // -----------------------------------------------------------------------
    // Interface injection — field injection (OrderServiceApi → OrderService)
    // -----------------------------------------------------------------------

    def "maps command dispatched through interface field-injected service"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.InterfaceInjectedOrderCommandHandler') }
        def dispatch = handler?.commandDispatch['com.example.dummyapp.order.commands.CancelOrderCommand']
        expect:
        handler != null
        dispatch != null
        dispatch.serviceMethodName() == 'cancelOrder'
        dispatch.serviceClassName().contains('OrderService')
        dispatch.accessPolicy() == AccessPolicy.WRITE
    }

    // -----------------------------------------------------------------------
    // Interface injection — constructor injection (OrderServiceApi → OrderService)
    // -----------------------------------------------------------------------

    def "maps command dispatched through interface constructor-injected service"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.CtorInterfaceInjectedOrderCommandHandler') }
        def dispatch = handler?.commandDispatch['com.example.dummyapp.order.commands.GetOrderCommand']
        expect:
        handler != null
        dispatch != null
        dispatch.serviceMethodName() == 'getOrder'
        dispatch.serviceClassName().contains('OrderService')
        dispatch.accessPolicy() == AccessPolicy.READ
    }

    def "maps command dispatched through interface-only service implementation"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.InterfaceOnlyCommandHandler') }
        def dispatch = handler?.commandDispatch['com.example.dummyapp.shared.commands.InterfaceOnlyCommand']

        expect:
        handler != null
        dispatch != null
        dispatch.serviceMethodName() == 'loadInterfaceOnly'
        dispatch.serviceClassName() == 'com.example.dummyapp.shared.service.InterfaceOnlyService'
        dispatch.accessPolicy() == AccessPolicy.READ
    }

    def "service registry admits single implementation interface-only dispatch target"() {
        expect:
        state.services*.fqn.contains('com.example.dummyapp.shared.service.InterfaceOnlyService')
        state.interfaceToServices['com.example.dummyapp.shared.service.InterfaceOnlyServiceApi']*.fqn == [
                'com.example.dummyapp.shared.service.InterfaceOnlyService'
        ]
    }

    // -----------------------------------------------------------------------
    // Interface injection — ambiguous (two implementations → skipped)
    // -----------------------------------------------------------------------

    def "skips dispatch when interface has multiple implementations"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.AmbiguousCommandHandler') }
        expect:
        handler != null
        handler.commandDispatch.isEmpty()
        !state.services*.fqn.contains('com.example.dummyapp.shared.service.AmbiguousServiceImplA')
        !state.services*.fqn.contains('com.example.dummyapp.shared.service.AmbiguousServiceImplB')
    }

    // -----------------------------------------------------------------------
    // Helper delegation — handler calls a private helper that calls the service
    // -----------------------------------------------------------------------

    def "maps command dispatched through one local helper method"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.DelegatingItemCommandHandler') }
        def dispatch = handler?.commandDispatch['com.example.dummyapp.item.commands.CreateItemCommand']
        expect:
        handler != null
        dispatch != null
        dispatch.serviceMethodName() == 'createItem'
        dispatch.serviceClassName().contains('ItemService')
        dispatch.accessPolicy() == AccessPolicy.WRITE
    }

    def "maps command through overloaded private helper by signature"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.OverloadedDelegateItemCommandHandler') }
        def dispatch = handler?.commandDispatch['com.example.dummyapp.item.commands.CreateItemCommand']
        expect:
        handler != null
        dispatch != null
        dispatch.serviceMethodName() == 'createItem'
        dispatch.serviceClassName().contains('ItemService')
        dispatch.accessPolicy() == AccessPolicy.WRITE
    }

    // -----------------------------------------------------------------------
    // Constant aggregate type — getAggregateTypeName returns a static final field
    // -----------------------------------------------------------------------

    def "extracts aggregate type name from constant field"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.ConstantAggregateTypeItemCommandHandler') }
        expect:
        handler != null
        handler.aggregateTypeName == 'Item'
    }

    // -----------------------------------------------------------------------
    // Overloaded service methods — two commands → same method name, different signature
    // -----------------------------------------------------------------------

    def "distinguishes overloaded service methods by signature"() {
        given:
        def handler = state.commandHandlers.find { it.fqn.endsWith('.OverloadedItemCommandHandler') }
        def writeDispatch = handler?.commandDispatch['com.example.dummyapp.item.commands.ProcessItemCommand']
        def readDispatch = handler?.commandDispatch['com.example.dummyapp.item.commands.LookupItemCommand']
        expect:
        handler != null
        writeDispatch != null
        writeDispatch.serviceMethodName() == 'processItem'
        writeDispatch.serviceClassName().contains('OverloadedItemService')
        writeDispatch.accessPolicy() == AccessPolicy.WRITE
        readDispatch != null
        readDispatch.serviceMethodName() == 'processItem'
        readDispatch.serviceClassName().contains('OverloadedItemService')
        readDispatch.accessPolicy() == AccessPolicy.READ
    }

    // -----------------------------------------------------------------------
    // Service registry integrity
    // -----------------------------------------------------------------------

    def "service registry contains only dispatch-target services for dummyapp"() {
        // Every service in state.services must appear as the target of at least one
        // CommandDispatchInfo. Coordination facades and ambiguous-interface impls
        // must not appear here.
        given:
        def dispatchedServiceFqns = state.commandHandlers
            .collectMany { it.commandDispatch.values() }
            .collect { it.serviceClassName() }
            .toSet()
        expect:
        state.services.every { svc -> dispatchedServiceFqns.contains(svc.fqn) }
    }
}
