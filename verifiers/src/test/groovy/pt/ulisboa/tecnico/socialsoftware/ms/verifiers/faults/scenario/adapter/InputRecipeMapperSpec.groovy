package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyAssignmentRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
import spock.lang.Specification

class InputRecipeMapperSpec extends Specification {

    def 'maps direct constructor arguments to schema-versioned typed literal recipe'() {
        given:
        def mapper = new InputRecipeMapper()

        when:
        def recipe = mapper.map([
                arg(1, 'name <- "17"', literal('"17"', 'java.lang.String'), 'java.lang.String'),
                arg(0, 'age <- 17', literal('17', 'java.lang.Integer'), 'java.lang.Integer')
        ], InputResolutionStatus.RESOLVED)

        then:
        recipe.schemaVersion() == InputRecipe.SCHEMA_VERSION
        recipe.executorReady()
        recipe.recipeFingerprint() == mapper.map([
                arg(1, 'name <- "17"', literal('"17"', 'java.lang.String'), 'java.lang.String'),
                arg(0, 'age <- 17', literal('17', 'java.lang.Integer'), 'java.lang.Integer')
        ], InputResolutionStatus.RESOLVED).recipeFingerprint()
        recipe.arguments()*.index() == [0, 1]
        recipe.arguments()[0].recipe().literalKind() == 'integer'
        recipe.arguments()[0].recipe().value() == 17L
        recipe.arguments()[1].recipe().literalKind() == 'string'
        recipe.arguments()[1].recipe().value() == '17'
    }

    def 'maps constructor target types and deterministic assignments'() {
        given:
        def mapper = new InputRecipeMapper()
        def dto = constructor('ItemDto', 'com.example.ItemDto', [], [
                assignment('named_argument', 'aggregateId', null, 0, 'aggregateId: 17', literal('17')),
                assignment('setter', 'orderId', 'setOrderId', 1, 'dto.setOrderId(23)', literal('23')),
                assignment('property', 'name', null, 2, 'dto.name = "sample"', literal('"sample"', 'java.lang.String'))
        ])

        when:
        def node = mapper.map([arg(0, 'dto <- new ItemDto()', dto, 'com.example.ItemDto')], InputResolutionStatus.RESOLVED)
                .arguments()[0]
                .recipe()

        then:
        node.kind() == 'constructor'
        node.targetTypeFqn() == 'com.example.ItemDto'
        node.executorReady()
        node.assignments()*.assignmentKind() == ['named_argument', 'setter', 'property']
        node.assignments()*.propertyName() == ['aggregateId', 'orderId', 'name']
        node.assignments()[1].sourceName() == 'setOrderId'
        node.assignments()[2].valueRecipe().literalKind() == 'string'
    }

    def 'marks missing target type and duplicate assignment writers as blockers'() {
        given:
        def mapper = new InputRecipeMapper()
        def dto = constructor('ItemDto', null, [], [
                assignment('setter', 'orderId', 'setOrderId', 0, 'dto.setOrderId(23)', literal('23')),
                assignment('property', 'orderId', null, 1, 'dto.orderId = 24', literal('24'))
        ])

        when:
        def node = mapper.map([arg(0, 'dto <- new ItemDto()', dto, null)], InputResolutionStatus.RESOLVED)
                .arguments()[0]
                .recipe()

        then:
        !node.executorReady()
        node.blockers().contains('MISSING_TARGET_TYPE')
        node.blockers().contains('AMBIGUOUS_MULTI_WRITER:orderId')
    }

    def 'maps collections maps and local transforms with readiness propagation'() {
        given:
        def mapper = new InputRecipeMapper()
        def list = new GroovyValueRecipe(GroovyValueKind.COLLECTION_LITERAL, 'list', [literal('1'), literal('2')])
        def map = new GroovyValueRecipe(GroovyValueKind.COLLECTION_LITERAL, 'map', [literal('"primary"'), literal('1'), literal('"secondary"'), literal('2')])
        def toSet = new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM, 'toSet', [list])
        def asSet = new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM, 'as Set', [list])
        def unresolvedTransform = new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM, 'toSet', [unresolved('runtimeIds')])

        when:
        def recipe = mapper.map([
                arg(0, 'ids <- [1, 2]', list, 'java.util.List'),
                arg(1, 'idMap <- [primary: 1]', map, 'java.util.Map'),
                arg(2, 'ids.toSet()', toSet, 'java.util.Set'),
                arg(3, 'ids as Set', asSet, 'java.util.Set'),
                arg(4, 'runtimeIds.toSet()', unresolvedTransform, 'java.util.Set')
        ], InputResolutionStatus.PARTIAL)

        then:
        recipe.arguments()[0].recipe().collectionKind() == 'list'
        recipe.arguments()[0].recipe().elements()*.value() == [1L, 2L]
        recipe.arguments()[1].recipe().collectionKind() == 'map'
        recipe.arguments()[1].recipe().entries()*.index() == [0, 1]
        recipe.arguments()[1].recipe().entries()[0].keyRecipe().value() == 'primary'
        recipe.arguments()[2].recipe().kind() == 'local_transform'
        recipe.arguments()[2].recipe().transformName() == 'toSet'
        recipe.arguments()[2].recipe().executorReady()
        recipe.arguments()[3].recipe().targetType() == 'Set'
        !recipe.arguments()[4].executorReady()
        recipe.arguments()[4].blockers().contains('TRANSFORM_RECEIVER_NOT_READY')
        !recipe.executorReady()
        recipe.blockers().contains('INPUT_STATUS_PARTIAL')
    }

    def 'maps helper results property access call results and placeholders without facade-specific node kinds'() {
        given:
        def mapper = new InputRecipeMapper()
        def receiver = constructor('RuntimeGateway', 'com.example.RuntimeGateway')
        def runtimeCall = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'runtimeGateway.loadExternalDto(9)',
                [receiver],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'com.example.ItemDto',
                        null,
                        new GroovyRuntimeCallRecipe('runtimeGateway', 'loadExternalDto', [new GroovyRuntimeCallArgument(0, '9', literal('9'))], 'runtimeGateway.loadExternalDto(9)')))
        def propertyAccess = new GroovyValueRecipe(GroovyValueKind.PROPERTY_ACCESS, 'aggregateId', [runtimeCall])
        def helper = new GroovyValueRecipe(GroovyValueKind.HELPER_CALL_RESULT, 'buildItemDto', [propertyAccess])
        def placeholder = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE,
                'commandGateway',
                [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER,
                        'pt.example.CommandGateway',
                        'injectable:Spec::commandGateway',
                        null))
        def missingReceiverCall = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'createItem(dto)',
                [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'com.example.ItemDto',
                        null,
                        new GroovyRuntimeCallRecipe(null, 'createItem', [], 'createItem(dto)')))

        when:
        def recipe = mapper.map([
                arg(0, 'aggregateId <- helper', helper, 'java.lang.Integer'),
                arg(1, 'commandGateway <- field', placeholder, 'pt.example.CommandGateway'),
                arg(2, 'result <- createItem(dto)', missingReceiverCall, 'com.example.ItemDto')
        ], InputResolutionStatus.PARTIAL)
        def nodes = flatten(recipe)

        then:
        nodes*.kind().containsAll(['helper_result', 'property_access', 'call_result', 'placeholder'])
        !nodes*.kind().contains('facade_call')
        nodes.find { it.kind() == 'helper_result' }.resultRecipe().kind() == 'property_access'
        nodes.find { it.kind() == 'call_result' && it.methodName() == 'loadExternalDto' }.callArguments()[0].recipe().value() == 9L
        nodes.find { it.kind() == 'placeholder' }.placeholderPurpose() == 'injectable'
        nodes.find { it.kind() == 'call_result' && it.methodName() == 'createItem' }.blockers().contains('MISSING_CALL_RECEIVER')
    }

    private static GroovyTraceArgument arg(int index, String provenance, GroovyValueRecipe recipe, String expectedType = null) {
        new GroovyTraceArgument(index, provenance, recipe, expectedType)
    }

    private static GroovyValueRecipe literal(String text, String expectedType = null) {
        new GroovyValueRecipe(GroovyValueKind.LITERAL,
                text,
                [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, expectedType, null, null))
    }

    private static GroovyValueRecipe unresolved(String text) {
        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE,
                text,
                [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.UNKNOWN_UNRESOLVED, null, null, null))
    }

    private static GroovyValueRecipe constructor(String text, String targetTypeFqn, List<GroovyValueRecipe> children = [], List<GroovyAssignmentRecipe> assignments = []) {
        new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR,
                text,
                children,
                new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, targetTypeFqn, null, null, assignments))
    }

    private static GroovyAssignmentRecipe assignment(String kind, String property, String sourceName, int order, String sourceText, GroovyValueRecipe value) {
        new GroovyAssignmentRecipe(kind, property, sourceName, order, sourceText, value, null)
    }

    private static List flatten(InputRecipe recipe) {
        recipe.arguments().collectMany { flattenNode(it.recipe()) }
    }

    private static List flattenNode(node) {
        if (node == null) {
            return []
        }
        def children = []
        children.addAll(node.arguments().collectMany { flattenNode(it.recipe()) })
        children.addAll(node.assignments().collectMany { flattenNode(it.valueRecipe()) })
        children.addAll(node.elements().collectMany { flattenNode(it) })
        children.addAll(node.entries().collectMany { flattenNode(it.keyRecipe()) + flattenNode(it.valueRecipe()) })
        children.addAll(node.callArguments().collectMany { flattenNode(it.recipe()) })
        children.addAll(flattenNode(node.receiver()))
        children.addAll(flattenNode(node.resultRecipe()))
        [node] + children
    }
}
