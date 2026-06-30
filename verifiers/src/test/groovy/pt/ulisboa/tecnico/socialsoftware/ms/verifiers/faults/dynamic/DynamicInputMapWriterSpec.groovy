package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class DynamicInputMapWriterSpec extends Specification {
    private static final String GENERATED_AT = '2026-05-12T00:00:00Z'
    private static final String TEST_CLASS = 'com.example.OrderSpec'
    private static final String OTHER_TEST_CLASS = 'com.example.OtherSpec'
    private static final String ORDER_SAGA = 'com.example.OrderSaga'
    private static final String PAYMENT_SAGA = 'com.example.PaymentSaga'
    private final ObjectMapper mapper = new ObjectMapper()

    @TempDir
    Path tempDir

    def 'writes all accepted inputs in a run scoped map'() {
        given:
        def writer = new DynamicInputMapWriter(mapper)
        def path = tempDir.resolve('dynamic-input-map.json')
        def acceptedInput = input('input-1', TEST_CLASS, ORDER_SAGA, [
                'arg[0]: 42',
                'arg[1]: dto <- new OrderDto()',
                'arg[2]: label <- "rush"'
        ])
        def otherClassInput = input('input-2', OTHER_TEST_CLASS, PAYMENT_SAGA, ['arg[0]: 99'])
        def repeatedAcceptedInput = input('input-1', TEST_CLASS, ORDER_SAGA, ['arg[0]: 42'])

        when:
        writer.write(path, [OTHER_TEST_CLASS, TEST_CLASS], [
                singlePlan('scenario-1', acceptedInput, ORDER_SAGA, ['com.example.OrderSaga::reserve#0', 'com.example.OrderSaga::confirm']),
                singlePlan('scenario-2', otherClassInput, PAYMENT_SAGA, ['com.example.PaymentSaga::charge']),
                singlePlan('scenario-3', repeatedAcceptedInput, ORDER_SAGA, ['com.example.OrderSaga::reserve#1'])
        ], GENERATED_AT)

        then:
        Files.exists(path)
        def json = mapper.readTree(Files.readString(path))
        json.path('schemaVersion').asText() == DynamicInputMapWriter.SCHEMA_VERSION
        json.path('generatedAt').asText() == GENERATED_AT
        json.path('testClassFqn').isMissingNode()
        json.path('selectedTestClassFqns')*.asText() == [TEST_CLASS, OTHER_TEST_CLASS]
        json.path('inputCount').asInt() == 2

        def entry = json.path('inputs').find { it.path('inputVariantId').asText() == 'input-1' }
        entry.path('inputVariantId').asText() == 'input-1'
        entry.path('sourceClassFqn').asText() == TEST_CLASS
        entry.path('sagaFqn').asText() == ORDER_SAGA
        entry.path('sourceMethodName').asText() == 'createsOrder'
        entry.path('callContextMethodName').asText() == 'setup'
        entry.path('inputRole').asText() == 'FIXTURE_PREREQUISITE'
        entry.path('fixtureOrigin').asText() == 'SETUP_HELPER'
        entry.path('owners')*.path('testMethodName')*.asText() == ['createsOrder']
        entry.path('resolutionStatus').asText() == 'RESOLVED'
        entry.path('sourceMode').asText() == 'SAGAS'
        entry.path('sourceModeConfidence').asText() == 'TYPE_EVIDENCE'
        entry.path('stepNameHints')*.asText() == ['confirm', 'reserve']
        entry.path('literalArgumentValueHints')*.asText() == ['42', 'rush']
        entry.path('constructorArgumentSummaries')*.asText().contains('arg[1]: dto <- new OrderDto()')
        entry.path('expectedCommands').isEmpty()
        entry.path('scenarioPlanIds')*.asText() == ['scenario-1', 'scenario-3']
        entry.path('stableSourceText').asText() == 'createOrder(42)'
        entry.path('provenanceText').asText() == 'OrderSpec.createsOrder'
        json.path('inputs').find { it.path('inputVariantId').asText() == 'input-2' }.path('sourceClassFqn').asText() == OTHER_TEST_CLASS
    }

    def 'collects aggregate type hints from conflict evidence for the selected input steps'() {
        given:
        def writer = new DynamicInputMapWriter(mapper)
        def leftInput = input('input-left', TEST_CLASS, ORDER_SAGA, ['arg[0]: 42'])
        def rightInput = input('input-right', TEST_CLASS, PAYMENT_SAGA, ['arg[0]: 42'])
        def leftInstance = new SagaInstance('saga-left', ORDER_SAGA, 'input-left', [])
        def rightInstance = new SagaInstance('saga-right', PAYMENT_SAGA, 'input-right', [])
        def leftStep = new ScheduledStep('scheduled-left', 'saga-left', ORDER_SAGA + '::reserve', 0, [])
        def rightStep = new ScheduledStep('scheduled-right', 'saga-right', PAYMENT_SAGA + '::charge', 1, [])
        def conflict = new ConflictEvidence(
                'conflict-1',
                'scheduled-left',
                'scheduled-right',
                new AggregateKey('Order', 'order', '42', FootprintConfidence.EXACT),
                new AggregateKey('Payment', 'payment', '42', FootprintConfidence.EXACT),
                AccessMode.WRITE,
                AccessMode.READ,
                ConflictKind.WRITE_READ,
                [])

        when:
        def map = writer.build([TEST_CLASS], [
                new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, 'scenario-multi', ScenarioKind.MULTI_SAGA,
                        [leftInstance, rightInstance], [leftInput, rightInput], [leftStep, rightStep], null, [conflict], [])
        ], GENERATED_AT)

        then:
        map.inputs()*.inputVariantId() == ['input-left', 'input-right']
        map.inputs().find { it.inputVariantId() == 'input-left' }.expectedAggregateTypes() == ['Order']
        map.inputs().find { it.inputVariantId() == 'input-right' }.expectedAggregateTypes() == ['Payment']
    }

    def 'selected classes are audit metadata and do not prune accepted inputs'() {
        given:
        def path = tempDir.resolve('empty/dynamic-input-map.json')

        when:
        new DynamicInputMapWriter(mapper).write(path, [TEST_CLASS], [
                singlePlan('scenario-1', input('input-1', OTHER_TEST_CLASS, ORDER_SAGA, ['arg[0]: 42']), ORDER_SAGA, ['reserve'])
        ], GENERATED_AT)

        then:
        def json = mapper.readTree(Files.readString(path))
        json.path('selectedTestClassFqns')*.asText() == [TEST_CLASS]
        json.path('inputCount').asInt() == 1
        json.path('inputs')[0].path('sourceClassFqn').asText() == OTHER_TEST_CLASS
    }

    private static ScenarioPlan singlePlan(String scenarioId, InputVariant input, String sagaFqn, List<String> stepIds) {
        def saga = new SagaInstance("${scenarioId}-saga".toString(), sagaFqn, input.deterministicId(), [])
        def steps = stepIds.withIndex().collect { String stepId, int index ->
            new ScheduledStep("${scenarioId}-step-${index}".toString(), saga.deterministicId(), stepId, index, [])
        }
        new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, scenarioId, ScenarioKind.SINGLE_SAGA, [saga], [input], steps, null, [], [])
    }

    private static InputVariant input(String id, String sourceClassFqn, String sagaFqn, List<String> constructorArgs) {
        new InputVariant(
                id,
                sagaFqn,
                sourceClassFqn,
                'createsOrder',
                'orderSaga',
                'setup',
                InputRole.FIXTURE_PREREQUISITE,
                FixtureOrigin.SETUP_HELPER,
                InputResolutionStatus.RESOLVED,
                SourceMode.SAGAS,
                SourceModeConfidence.TYPE_EVIDENCE,
                ['@ActiveProfiles sagas'],
                'createOrder(42)',
                'OrderSpec.createsOrder',
                [new InputOwner(sourceClassFqn, 'createsOrder')],
                constructorArgs,
                [orderId: '42'],
                [],
                null)
    }
}
