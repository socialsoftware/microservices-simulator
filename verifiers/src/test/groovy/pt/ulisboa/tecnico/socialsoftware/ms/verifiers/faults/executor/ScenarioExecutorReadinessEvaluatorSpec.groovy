package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant
import spock.lang.Specification

class ScenarioExecutorReadinessEvaluatorSpec extends Specification {

    def 'runtime-owned blocked recipe becomes materializable'() {
        given:
        def input = input(recipe(false, ['UNRESOLVED_VARIABLE'], [
                arg(0, ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK_SERVICE, false, ['UNRESOLVED_VARIABLE'], unresolved()),
                arg(1, ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK, false, ['UNRESOLVED_VARIABLE'], unresolved()),
                arg(2, ScenarioExecutorMaterializationPolicy.COMMAND_GATEWAY, false, ['UNRESOLVED_VARIABLE'], unresolved())
        ]))

        when:
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(input)

        then:
        readiness.materializable()
        !readiness.staticRecipeReady()
        readiness.blockers().isEmpty()
        readiness.runtimeOwnedResolutions() == [
                ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK_SERVICE,
                ScenarioExecutorMaterializationPolicy.SAGA_UNIT_OF_WORK,
                ScenarioExecutorMaterializationPolicy.COMMAND_GATEWAY]
    }

    def 'non-runtime-owned blocked argument remains blocked'() {
        given:
        def input = input(recipe(false, ['UNRESOLVED_VARIABLE'], [
                arg(0, Integer.name, false, ['UNRESOLVED_VARIABLE'], unresolved())
        ]))

        when:
        def readiness = new ScenarioExecutorReadinessEvaluator().evaluate(input)

        then:
        !readiness.materializable()
        !readiness.staticRecipeReady()
        readiness.blockers() == ['UNRESOLVED_VARIABLE']
        readiness.runtimeOwnedResolutions().isEmpty()
    }

    private static InputVariant input(InputRecipe recipe) {
        new InputVariant('input-1', 'saga.A', 'Spec', 'source', 'saga', InputResolutionStatus.RESOLVED,
                'new Saga()', 'saga', [], [:], [], recipe)
    }

    private static InputRecipe recipe(boolean executorReady, List<String> blockers, List<InputRecipeArgument> arguments) {
        new InputRecipe(InputRecipe.SCHEMA_VERSION, null, executorReady, blockers, arguments)
    }

    private static InputRecipeArgument arg(int index, String type, boolean executorReady, List<String> blockers, InputRecipeNode node) {
        new InputRecipeArgument(index, type, InputResolutionStatus.UNRESOLVED, executorReady, blockers, 'spec', node)
    }

    private static InputRecipeNode unresolved() {
        InputRecipeNode.builder('unresolved').executorReady(false).blockers(['UNRESOLVED_VARIABLE']).build()
    }
}
