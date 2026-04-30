package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ApplicationsFileTreeParserSpec extends Specification {

    private static Path resolveProjectPath(String first, String... more) {
        def direct = Path.of(first, *more)
        if (Files.isDirectory(direct)) {
            return direct
        }

        return Path.of('..').resolve(Path.of(first, *more)).normalize()
    }

    def 'parser discovers dummy app java and groovy files'() {
        given:
        def parser = new ApplicationsFileTreeParser()
        def root = resolveProjectPath('applications', 'dummyapp')

        when:
        parser.parse(root)

        then:
        parser.javaFilePaths.keySet() == ([
            'com.example.dummyapp.DummyAggregate',
            'com.example.dummyapp.DummyApp',
            'com.example.dummyapp.item.aggregate.Item',
            'com.example.dummyapp.item.aggregate.ItemDto',
            'com.example.dummyapp.item.aggregate.ItemRepository',
            'com.example.dummyapp.item.service.ItemService',
            'com.example.dummyapp.item.commands.GetItemCommand',
            'com.example.dummyapp.item.commands.CreateItemCommand',
            'com.example.dummyapp.item.commands.UpdateItemCommand',
            'com.example.dummyapp.item.commands.DeleteItemCommand',
            'com.example.dummyapp.item.commandHandler.ItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.DelegatingItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.ConstantAggregateTypeItemCommandHandler',
            'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemFieldInjectionFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemDependencyGraphFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemLoopedReadsFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemCompensationFunctionalitySagas',
            'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade',
            'com.example.dummyapp.item.service.AliasUnitOfWorkItemService',
            'com.example.dummyapp.item.service.GetterBasedUnitOfWorkItemService',
            'com.example.dummyapp.item.service.HelperMarkChangedItemService',
            'com.example.dummyapp.order.aggregate.Order',
            'com.example.dummyapp.order.aggregate.OrderDto',
            'com.example.dummyapp.order.aggregate.OrderRepository',
            'com.example.dummyapp.order.service.OrderService',
            'com.example.dummyapp.order.service.OrderServiceApi',
            'com.example.dummyapp.order.commands.GetOrderCommand',
            'com.example.dummyapp.order.commands.PlaceOrderCommand',
            'com.example.dummyapp.order.commands.CancelOrderCommand',
            'com.example.dummyapp.order.commandHandler.OrderCommandHandler',
            'com.example.dummyapp.order.commandHandler.InterfaceInjectedOrderCommandHandler',
            'com.example.dummyapp.order.commandHandler.CtorInterfaceInjectedOrderCommandHandler',
            'com.example.dummyapp.shared.service.AmbiguousServiceApi',
            'com.example.dummyapp.shared.service.AmbiguousServiceImplA',
            'com.example.dummyapp.shared.service.AmbiguousServiceImplB',
            'com.example.dummyapp.shared.service.InterfaceOnlyService',
            'com.example.dummyapp.shared.service.InterfaceOnlyServiceApi',
            'com.example.dummyapp.shared.service.SubstringTrapService',
            'com.example.dummyapp.shared.fake.OnlyLooksLikeUnitOfWorkService',
            'com.example.dummyapp.shared.commands.DoSomethingCommand',
            'com.example.dummyapp.shared.commands.InterfaceOnlyCommand',
            'com.example.dummyapp.shared.commands.PingCommand',
            'com.example.dummyapp.shared.commandHandler.AmbiguousCommandHandler',
            'com.example.dummyapp.shared.commandHandler.InterfaceOnlyCommandHandler',
            'com.example.dummyapp.shared.commandHandler.SubstringTrapCommandHandler',
            'com.example.dummyapp.item.commands.ProcessItemCommand',
            'com.example.dummyapp.item.commands.LookupItemCommand',
            'com.example.dummyapp.item.service.OverloadedItemService',
            'com.example.dummyapp.item.commandHandler.OverloadedItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.OverloadedDelegateItemCommandHandler',
            'com.example.dummyapp.item.commands.AliasWriteItemCommand',
            'com.example.dummyapp.item.commands.GetterWriteItemCommand',
            'com.example.dummyapp.item.commands.HelperWriteItemCommand',
            'com.example.dummyapp.item.commandHandler.AliasUnitOfWorkItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.GetterBasedUnitOfWorkItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.HelperMarkChangedItemCommandHandler',
            'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas',
            'com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade',
        ] as Set)

        and:
        parser.groovyFilePaths.keySet() ==~ [
            'com.example.dummyapp.DummyAppSpec',
            'com.example.dummyapp.GroovyNestedFacadeTracingSpec',
            'com.example.dummyapp.GroovySagaTracingSpec',
            'com.example.dummyapp.GroovyTccSourceModeTracingSpec'
        ]
    }

    def 'parser resolves correct file paths'() {
        given:
        def parser = new ApplicationsFileTreeParser()
        def root = resolveProjectPath('applications', 'dummyapp')

        when:
        parser.parse(root)

        then:
        parser.javaFilePaths['com.example.dummyapp.DummyAggregate'].toString().endsWith(
                'applications/dummyapp/src/main/java/com/example/dummyapp/DummyAggregate.java')
        parser.javaFilePaths['com.example.dummyapp.DummyApp'].toString().endsWith(
                'applications/dummyapp/src/main/java/com/example/dummyapp/DummyApp.java')
        parser.groovyFilePaths['com.example.dummyapp.DummyAppSpec'].toString().endsWith(
                'applications/dummyapp/src/test/groovy/com/example/dummyapp/DummyAppSpec.groovy')
        parser.groovyFilePaths['com.example.dummyapp.GroovySagaTracingSpec'].toString().endsWith(
                'applications/dummyapp/src/test/groovy/com/example/dummyapp/GroovySagaTracingSpec.groovy')
        parser.groovyFilePaths['com.example.dummyapp.GroovyTccSourceModeTracingSpec'].toString().endsWith(
                'applications/dummyapp/src/test/groovy/com/example/dummyapp/GroovyTccSourceModeTracingSpec.groovy')
    }

    def 'parser rejects non-directory path'() {
        given:
        def parser = new ApplicationsFileTreeParser()
        def badPath = Path.of('nonexistent')

        when:
        parser.parse(badPath)

        then:
        thrown(IllegalArgumentException)
    }

    def 'parser selects java classes by application base dir'() {
        given:
        def parser = new ApplicationsFileTreeParser()
        def applicationsRoot = resolveProjectPath('applications')

        when:
        parser.parse(applicationsRoot)
        def classes = parser.getJavaFilePathsForApplication(applicationsRoot, 'dummyapp')

        then:
        classes.keySet() == ([
            'com.example.dummyapp.DummyAggregate',
            'com.example.dummyapp.DummyApp',
            'com.example.dummyapp.item.aggregate.Item',
            'com.example.dummyapp.item.aggregate.ItemDto',
            'com.example.dummyapp.item.aggregate.ItemRepository',
            'com.example.dummyapp.item.service.ItemService',
            'com.example.dummyapp.item.commands.GetItemCommand',
            'com.example.dummyapp.item.commands.CreateItemCommand',
            'com.example.dummyapp.item.commands.UpdateItemCommand',
            'com.example.dummyapp.item.commands.DeleteItemCommand',
            'com.example.dummyapp.item.commandHandler.ItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.DelegatingItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.ConstantAggregateTypeItemCommandHandler',
            'com.example.dummyapp.item.coordination.CreateItemFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemFieldInjectionFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemDependencyGraphFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemLoopedReadsFunctionalitySagas',
            'com.example.dummyapp.item.coordination.CreateItemCompensationFunctionalitySagas',
            'com.example.dummyapp.item.coordination.ItemFunctionalitiesFacade',
            'com.example.dummyapp.item.service.AliasUnitOfWorkItemService',
            'com.example.dummyapp.item.service.GetterBasedUnitOfWorkItemService',
            'com.example.dummyapp.item.service.HelperMarkChangedItemService',
            'com.example.dummyapp.order.aggregate.Order',
            'com.example.dummyapp.order.aggregate.OrderDto',
            'com.example.dummyapp.order.aggregate.OrderRepository',
            'com.example.dummyapp.order.service.OrderService',
            'com.example.dummyapp.order.service.OrderServiceApi',
            'com.example.dummyapp.order.commands.GetOrderCommand',
            'com.example.dummyapp.order.commands.PlaceOrderCommand',
            'com.example.dummyapp.order.commands.CancelOrderCommand',
            'com.example.dummyapp.order.commandHandler.OrderCommandHandler',
            'com.example.dummyapp.order.commandHandler.InterfaceInjectedOrderCommandHandler',
            'com.example.dummyapp.order.commandHandler.CtorInterfaceInjectedOrderCommandHandler',
            'com.example.dummyapp.shared.service.AmbiguousServiceApi',
            'com.example.dummyapp.shared.service.AmbiguousServiceImplA',
            'com.example.dummyapp.shared.service.AmbiguousServiceImplB',
            'com.example.dummyapp.shared.service.InterfaceOnlyService',
            'com.example.dummyapp.shared.service.InterfaceOnlyServiceApi',
            'com.example.dummyapp.shared.service.SubstringTrapService',
            'com.example.dummyapp.shared.fake.OnlyLooksLikeUnitOfWorkService',
            'com.example.dummyapp.shared.commands.DoSomethingCommand',
            'com.example.dummyapp.shared.commands.InterfaceOnlyCommand',
            'com.example.dummyapp.shared.commands.PingCommand',
            'com.example.dummyapp.shared.commandHandler.AmbiguousCommandHandler',
            'com.example.dummyapp.shared.commandHandler.InterfaceOnlyCommandHandler',
            'com.example.dummyapp.shared.commandHandler.SubstringTrapCommandHandler',
            'com.example.dummyapp.item.commands.ProcessItemCommand',
            'com.example.dummyapp.item.commands.LookupItemCommand',
            'com.example.dummyapp.item.service.OverloadedItemService',
            'com.example.dummyapp.item.commandHandler.OverloadedItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.OverloadedDelegateItemCommandHandler',
            'com.example.dummyapp.item.commands.AliasWriteItemCommand',
            'com.example.dummyapp.item.commands.GetterWriteItemCommand',
            'com.example.dummyapp.item.commands.HelperWriteItemCommand',
            'com.example.dummyapp.item.commandHandler.AliasUnitOfWorkItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.GetterBasedUnitOfWorkItemCommandHandler',
            'com.example.dummyapp.item.commandHandler.HelperMarkChangedItemCommandHandler',
            'com.example.dummyapp.order.coordination.CreateOrderFunctionalitySagas',
            'com.example.dummyapp.order.coordination.OrderFunctionalitiesFacade',
        ] as Set)
    }
}
