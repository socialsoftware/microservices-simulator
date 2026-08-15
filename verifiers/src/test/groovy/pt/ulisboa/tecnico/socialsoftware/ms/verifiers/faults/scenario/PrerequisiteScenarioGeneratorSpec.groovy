package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.Specification

import java.nio.file.Files

class PrerequisiteScenarioGeneratorSpec extends Specification {
    def 'descriptor adds only the two selected causal placements with typed baseline bindings'() {
        given:
        def app = Files.createTempDirectory('prerequisite-scenario')
        def resources = app.resolve('src/test/resources')
        Files.createDirectories(resources)
        new ObjectMapper().writeValue(resources.resolve('verifier-prerequisite-scenarios.json').toFile(), [
                schemaVersion: PrerequisiteScenarioGenerator.SCHEMA_VERSION,
                scenarios: [[
                        id: 'fixture-stale-read', providerId: 'fixture-provider', providerVersion: '1',
                        participants: [
                                [sagaFqn: 'example.ASaga', bindings: [[argumentIndex: 0, key: 'a', typeFqn: String.name]]],
                                [sagaFqn: 'example.BSaga', bindings: [[argumentIndex: 0, key: 'b', typeFqn: String.name]]]
                        ],
                        forwardRuntimeOrder: ['a1', 'b1', 'a2'],
                        eventTypeFqn: 'example.Event', eventHandlingClassFqn: 'example.EventHandling',
                        eventHandlingMethodName: 'handle', expectedWorkloadCount: 2
                ]]
        ])
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

        when:
        def result = new PrerequisiteScenarioGenerator().generate(app, definitions, inputs, [event], new ScenarioGeneratorConfig())

        then:
        result.workloads().size() == 2
        result.workloads().collect { it.forwardSchedule()*.runtimeStepName() }.unique() == [['a1', 'b1', 'a2']]
        result.workloads()*.prerequisiteBaseline()*.providerId().unique() == ['fixture-provider']
        result.workloads().collect { plan -> plan.normalSchedule()*.kind() as List } as Set == [
                [NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.EVENT_CONSEQUENCE, NormalActionKind.FORWARD],
                [NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.FORWARD, NormalActionKind.EVENT_CONSEQUENCE]
        ] as Set
        result.workloads().every { new WorkloadPlanValidator().validate(it).valid() }
        result.workloads().every { plan ->
            plan.acceptedInputs().every { input -> new ScenarioExecutorReadinessEvaluator().evaluate(input).materializable() }
        }
        result.workloads().every { plan ->
            plan.acceptedInputs().every { input ->
                input.inputRecipe().arguments()[0].recipe().kind() == 'baseline_binding'
            }
        }
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
