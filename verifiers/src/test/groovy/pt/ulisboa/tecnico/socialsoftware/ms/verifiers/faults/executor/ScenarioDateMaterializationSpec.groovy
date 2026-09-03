package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadExecutionShape
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.CurrentPackageFixture
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory
import spock.lang.Specification

import java.nio.file.Files

import java.time.LocalDateTime

class ScenarioDateMaterializationSpec extends Specification {

    def 'a mapped Quizzes-style DateHandler expression passes readiness and materializes to an ISO string'() {
        given:
        def recipe = mapDateRecipe()
        def input = input(recipe)

        when:
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(input)
        def materialized = new ScenarioMaterializer().materialize(input, null, 'date')

        then:
        readiness.materializable()
        readiness.blockers().isEmpty()
        materialized.success()
        materialized.values().size() == 1
        materialized.values()[0] ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z/
    }

    def 'DateHandler conversion rejects a receiver that is not a LocalDateTime'() {
        given:
        def receiver = InputRecipeNode.builder('literal')
                .executorReady(true)
                .value('not a date')
                .build()
        def transform = InputRecipeNode.builder('local_transform')
                .executorReady(true)
                .transformName('DateHandler.toISOString')
                .receiver(receiver)
                .build()
        def argument = new InputRecipeArgument(0, String.name, InputResolutionStatus.RESOLVED,
                true, [], 'date', transform)
        def input = input(new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument]))

        when:
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(input)
        def materialized = new ScenarioMaterializer().materialize(input, null, 'date')

        then:
        !readiness.materializable()
        readiness.blockers() == ['UNSUPPORTED_TRANSFORM_RECEIVER']
        !materialized.success()
        materialized.blockers()*.reason() == ['UNSUPPORTED_TRANSFORM_RECEIVER']
    }

    def 'current package round-trip restores the compact date recipe for execution'() {
        given:
        def original = input(mapDateRecipe())
        def workload = workload(original, 'workload-date')
        def packageInfo = CurrentPackageFixture.write([workload], [])

        when:
        def reader = new ScenarioCatalogPackageReader()
        def staticContents = reader.readCurrentStatic(packageInfo.manifest)
        def contents = reader.readCurrentForExecution(packageInfo.manifest)
        def restored = contents.workloadPlans()[0].acceptedInputs()[0]
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(contents.workloadPlans()[0], restored)
        def materialized = new ScenarioMaterializer().materialize(restored, null, 'date')

        then:
        staticContents.inputFacts()[0].path('arguments')[0].path('value').path('receiver').path('kind').asText() == 'relativeDateTime'
        staticContents.inputFacts()[0].path('arguments')[0].path('value').path('receiver').path('anchor').asText() == 'now'
        staticContents.inputFacts()[0].path('arguments')[0].path('value').path('receiver').path('offset').asText() == 'PT1H25M'
        restored.inputRecipe().arguments()[0].recipe().receiver().kind() == 'relative_date_time'
        restored.inputRecipe().arguments()[0].recipe().receiver().offset() == 'PT1H25M'
        readiness.materializable()
        materialized.success()
        materialized.values()[0] ==~ /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}(\.\d+)?Z/
    }

    def 'current package round-trip keeps an unsupported date form blocked'() {
        given:
        def original = input(mapUnsupportedDateRecipe())
        def workload = workload(original, 'workload-unsupported-date')
        def packageInfo = CurrentPackageFixture.write([workload], [])

        when:
        def reader = new ScenarioCatalogPackageReader()
        def staticContents = reader.readCurrentStatic(packageInfo.manifest)
        def contents = reader.readCurrentForExecution(packageInfo.manifest)
        def restored = contents.workloadPlans()[0].acceptedInputs()[0]
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(contents.workloadPlans()[0], restored)
        def materialized = new ScenarioMaterializer().materialize(restored, null, 'date')

        then:
        staticContents.inputFacts()[0].path('materializable').asBoolean() == false
        staticContents.inputFacts()[0].path('blockers')[0].path('reason').asText() == 'unsupportedTransformReceiver'
        restored.inputRecipe().arguments()[0].recipe().receiver().kind() == 'runtime'
        !readiness.materializable()
        readiness.blockers() == ['UNRESOLVED_ARGUMENT']
        !materialized.success()
        materialized.blockers()*.reason() == ['UNRESOLVED_ARGUMENT']
    }

    private static InputRecipe mapDateRecipe() {
        def mapperType = Class.forName('pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.InputRecipeMapper')
        def mapperConstructor = mapperType.getDeclaredConstructor()
        mapperConstructor.accessible = true
        def mapper = mapperConstructor.newInstance()
        def mapMethod = mapperType.getDeclaredMethod('map', List, InputResolutionStatus)
        mapMethod.accessible = true
        def now = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'DateHandler.now()', [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'java.time.LocalDateTime', null,
                        new GroovyRuntimeCallRecipe('DateHandler', 'now', [], 'DateHandler.now()')))
        def plusHours = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'DateHandler.now().plusHours(1)', [now],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'java.time.LocalDateTime', null,
                        new GroovyRuntimeCallRecipe('DateHandler.now()', 'plusHours',
                                [new GroovyRuntimeCallArgument(0, '1', literal('1'))],
                                'DateHandler.now().plusHours(1)')))
        def plusMinutes = new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'DateHandler.now().plusHours(1).plusMinutes(25)', [plusHours],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'java.time.LocalDateTime', null,
                        new GroovyRuntimeCallRecipe('DateHandler.now().plusHours(1)', 'plusMinutes',
                                [new GroovyRuntimeCallArgument(0, '25', literal('25'))],
                                'DateHandler.now().plusHours(1).plusMinutes(25)')))
        def transform = new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM,
                'DateHandler.toISOString', [plusMinutes])
        def traceArgument = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument(
                0, 'endDate <- DateHandler.toISOString(TIME_4)', transform, String.name)
        mapMethod.invoke(mapper, [[traceArgument], InputResolutionStatus.RESOLVED] as Object[])
    }

    private static GroovyValueRecipe literal(String value) {
        new GroovyValueRecipe(GroovyValueKind.LITERAL, value, [])
    }

    private static GroovyValueRecipe dateHandlerNow() {
        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                'DateHandler.now()', [],
                new GroovyValueMetadata(GroovyValueResolutionCategory.RUNTIME_CALL,
                        'java.time.LocalDateTime', null,
                        new GroovyRuntimeCallRecipe('DateHandler', 'now', [], 'DateHandler.now()')))
    }

    private static InputRecipe mapUnsupportedDateRecipe() {
        def mapperType = Class.forName('pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.InputRecipeMapper')
        def mapperConstructor = mapperType.getDeclaredConstructor()
        mapperConstructor.accessible = true
        def mapper = mapperConstructor.newInstance()
        def mapMethod = mapperType.getDeclaredMethod('map', List, InputResolutionStatus)
        mapMethod.accessible = true
        def transform = new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM,
                'DateHandler.toISOString', [dateHandlerNow()])
        def traceArgument = new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument(
                0, 'endDate <- DateHandler.toISOString(TIME_NOW)', transform, String.name)
        mapMethod.invoke(mapper, [[traceArgument], InputResolutionStatus.RESOLVED] as Object[])
    }

    private static WorkloadPlan workload(InputVariant input, String id) {
        def participant = new SagaInstance('instance-1', input.sagaFqn(), input.deterministicId(), [])
        def step = new ScheduledStep('scheduled-1', 'instance-1', "${input.sagaFqn()}::date#0", 0, 'date', [])
        new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, id, ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, [participant], [input], [step], [], [], [], [], [], [])
    }

    private static InputVariant input(InputRecipe recipe) {
        new InputVariant('input-1', 'saga.Date', 'Spec', 'date', 'saga',
                InputResolutionStatus.RESOLVED, 'date', 'date', [], [:], [], recipe)
    }
}
