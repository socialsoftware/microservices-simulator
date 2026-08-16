package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files

class PrerequisiteScenarioGeneratorSpec extends Specification {
    def 'event descriptor preserves the selected causal placements with typed baseline bindings and stable ids'() {
        given:
        def fixture = fixture()
        def app = descriptorApplication([eventDescriptor()])
        def generator = new PrerequisiteScenarioGenerator()

        when:
        def first = generator.generate(app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())
        def second = generator.generate(app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())

        then:
        first.workloads().size() == 2
        first.workloads()*.deterministicId() == second.workloads()*.deterministicId()
        first.workloads().collect { it.forwardSchedule()*.runtimeStepName() }.unique() == [['a1', 'b1', 'a2']]
        first.workloads()*.prerequisiteBaseline()*.providerId().unique() == ['fixture-provider']
        first.workloads().collect { plan -> plan.normalSchedule()*.kind() as List } as Set == [
                [NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.EVENT_CONSEQUENCE, NormalActionKind.FORWARD],
                [NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.EVENT_CONSEQUENCE]
        ] as Set
        validAndSetupCandidate(first.workloads())
        first.workloads().every { plan ->
            plan.acceptedInputs().every { input ->
                input.inputRecipe().arguments()[0].recipe().kind() == 'baseline_binding'
            }
        }
    }

    def 'no-event descriptor selects exactly the source-derived base workload despite available event expansions'() {
        given:
        def fixture = fixture()
        def descriptor = eventDescriptor() + [
                id: 'fixture-no-event',
                selectionKind: 'NO_EVENT',
                expectedWorkloadCount: 1
        ]
        descriptor.remove('eventTypeFqn')
        descriptor.remove('eventHandlingClassFqn')
        descriptor.remove('eventHandlingMethodName')
        def app = descriptorApplication([descriptor])
        def generator = new PrerequisiteScenarioGenerator()

        when:
        def first = generator.generate(app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())
        def second = generator.generate(app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())

        then:
        first.workloads().size() == 1
        first.workloads()[0].deterministicId() == second.workloads()[0].deterministicId()
        first.workloads()[0].forwardSchedule()*.runtimeStepName() == ['a1', 'b1', 'a2']
        first.workloads()[0].eventConsequences().empty
        first.workloads()[0].normalSchedule()*.kind() == [
                NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.FORWARD
        ]
        first.workloads()[0].prerequisiteBaseline().requiredBindings()*.key() == ['a', 'b']
        validAndSetupCandidate(first.workloads())
    }

    @Unroll
    def 'descriptor rejects malformed event selection #description'() {
        given:
        def fixture = fixture()
        def app = descriptorApplication([descriptor])

        when:
        new PrerequisiteScenarioGenerator().generate(
                app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('Malformed prerequisite scenario descriptor')

        where:
        description                    | descriptor
        'missing selection kind'       | eventDescriptor() - [selectionKind: 'EVENT']
        'partial event route'          | eventDescriptor() - [eventHandlingMethodName: 'handle']
        'event fields on no-event'     | eventDescriptor() + [selectionKind: 'NO_EVENT']
    }

    def 'descriptor rejects the previous ambiguous schema version'() {
        given:
        def fixture = fixture()
        def app = descriptorApplication([eventDescriptor()], 'microservices-simulator.prerequisite-scenario-descriptor.v1')

        when:
        new PrerequisiteScenarioGenerator().generate(
                app, fixture.definitions, fixture.inputs, [fixture.event], new ScenarioGeneratorConfig())

        then:
        def failure = thrown(IllegalArgumentException)
        failure.message.contains('Unsupported prerequisite scenario descriptor schema')
    }

    private static boolean validAndSetupCandidate(List<WorkloadPlan> workloads) {
        workloads.every { new WorkloadPlanValidator().validate(it).valid() } && workloads.every { plan ->
            plan.acceptedInputs().every { input ->
                new ScenarioExecutorReadinessEvaluator().evaluate(input).materializable()
            }
        }
    }

    private static Map eventDescriptor() {
        [
                id: 'fixture-stale-read', providerId: 'fixture-provider', providerVersion: '1',
                participants: [
                        [sagaFqn: 'example.ASaga', bindings: [[argumentIndex: 0, key: 'a', typeFqn: String.name]]],
                        [sagaFqn: 'example.BSaga', bindings: [[argumentIndex: 0, key: 'b', typeFqn: String.name]]]
                ],
                forwardRuntimeOrder: ['a1', 'b1', 'a2'],
                selectionKind: 'EVENT',
                eventTypeFqn: 'example.Event', eventHandlingClassFqn: 'example.EventHandling',
                eventHandlingMethodName: 'handle', expectedWorkloadCount: 2
        ]
    }

    private static java.nio.file.Path descriptorApplication(List<Map> scenarios,
                                                              String schemaVersion = PrerequisiteScenarioGenerator.SCHEMA_VERSION) {
        def app = Files.createTempDirectory('prerequisite-scenario')
        def resources = app.resolve('src/test/resources')
        Files.createDirectories(resources)
        new ObjectMapper().writeValue(resources.resolve('verifier-prerequisite-scenarios.json').toFile(), [
                schemaVersion: schemaVersion,
                scenarios: scenarios
        ])
        app
    }

    private static Map fixture() {
        def definitions = [
                new SagaDefinition('example.ASaga', [step('example.ASaga', 'a1', 0, []), step('example.ASaga', 'a2', 1, ['a1'])], []),
                new SagaDefinition('example.BSaga', [step('example.BSaga', 'b1', 0, [])], [])
        ]
        def inputs = [input('input-a', 'example.ASaga'), input('input-b', 'example.BSaga')]
        def site = new EventEmissionSite(ScenarioIdGenerator.eventEmissionSiteId('example.Service', 'emit()', 0, 'example.Event'),
                'example.Service', 'emit()', 0, 'example.Event', ['direct'])
        def event = new EventConsequenceDefinition('example.BSaga', 'example.BSaga::b1', site,
                'example.EventHandling', 'handle', 'example.Handler', 'example.Processing', 'process',
                'example.Facade', 'invoke', 'example.DownstreamSaga',
                EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        [definitions: definitions, inputs: inputs, event: event]
    }

    private static StepDefinition step(String saga, String name, int order, List<String> predecessors) {
        def key = new AggregateKey('example.Aggregate', 'Aggregate', 'id', FootprintConfidence.EXACT)
        def access = name == 'a1' ? AccessMode.READ : AccessMode.WRITE
        new StepDefinition("${saga}::${name}".toString(), name, name, order, predecessors,
                [new StepFootprint(key, access, [])], [])
    }

    private static InputVariant input(String id, String saga) {
        def argument = new InputRecipeArgument(0, String.name, InputResolutionStatus.UNRESOLVED,
                false, ['runtime baseline'], 'fixture', InputRecipeNode.builder('unresolved').executorReady(false).build())
        def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, false, ['runtime baseline'], [argument])
        new InputVariant(id, saga, 'example.Spec', 'feature', id, InputResolutionStatus.REPLAYABLE,
                'source', 'provenance', [], [:], [], recipe)
    }
}
