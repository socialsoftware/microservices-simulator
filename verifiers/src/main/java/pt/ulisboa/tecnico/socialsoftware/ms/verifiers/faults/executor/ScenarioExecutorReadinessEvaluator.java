package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScenarioExecutorReadinessEvaluator {
    public Readiness evaluate(InputVariant input) {
        InputRecipe recipe = input == null ? null : input.inputRecipe();
        if (recipe == null) {
            return new Readiness(false, false, List.of("MISSING_INPUT_RECIPE"), List.of());
        }

        List<String> blockers = new ArrayList<>();
        List<String> runtimeOwnedResolutions = new ArrayList<>();
        recipe.arguments().stream()
                .sorted(Comparator.comparingInt(InputRecipeArgument::index))
                .forEach(argument -> evaluateArgument(argument, blockers, runtimeOwnedResolutions));

        return new Readiness(blockers.isEmpty(), recipe.executorReady(), blockers, runtimeOwnedResolutions);
    }

    private void evaluateArgument(InputRecipeArgument argument, List<String> blockers, List<String> runtimeOwnedResolutions) {
        String type = argument.expectedTypeFqn();
        if (ScenarioExecutorMaterializationPolicy.isRuntimeOwned(type)) {
            runtimeOwnedResolutions.add(type);
            return;
        }
        if (!argument.executorReady()) {
            addArgumentBlockers(argument, blockers, "UNRESOLVED_ARGUMENT");
            return;
        }
        evaluateNode(argument.recipe(), blockers);
    }

    private void evaluateNode(InputRecipeNode node, List<String> blockers) {
        if (node == null) {
            blockers.add("MISSING_RECIPE");
            return;
        }
        String kind = node.kind() == null ? "" : node.kind();
        switch (kind) {
            case "literal" -> {
            }
            case "placeholder" -> {
                if (!ScenarioExecutorMaterializationPolicy.isRuntimeOwned(node.expectedTypeFqn())) {
                    addNodeBlockers(node, blockers, "UNRESOLVED_PLACEHOLDER");
                }
            }
            case "constructor" -> evaluateConstructor(node, blockers);
            case "collection" -> evaluateCollection(node, blockers);
            case "local_transform" -> evaluateLocalTransform(node, blockers);
            case "helper_result" -> evaluateNode(node.resultRecipe(), blockers);
            case "property_access" -> evaluatePropertyAccess(node, blockers);
            case "call_result" -> addNodeBlockers(node, blockers, "UNSUPPORTED_CALL_RESULT");
            case "unresolved" -> addNodeBlockers(node, blockers, "UNRESOLVED_VALUE");
            default -> addNodeBlockers(node, blockers, "UNSUPPORTED_RECIPE_KIND");
        }
    }

    private void evaluateConstructor(InputRecipeNode node, List<String> blockers) {
        if (node.targetTypeFqn() == null) {
            blockers.add("MISSING_TARGET_TYPE");
            return;
        }
        for (InputRecipeArgument child : node.arguments().stream().sorted(Comparator.comparingInt(InputRecipeArgument::index)).toList()) {
            if (!child.executorReady()) {
                addArgumentBlockers(child, blockers, "UNRESOLVED_CONSTRUCTOR_ARGUMENT");
                return;
            }
            int before = blockers.size();
            evaluateNode(child.recipe(), blockers);
            if (blockers.size() > before) {
                return;
            }
        }
        for (InputRecipeAssignment assignment : node.assignments().stream().sorted(Comparator.comparingInt(InputRecipeAssignment::orderIndex)).toList()) {
            int before = blockers.size();
            evaluateNode(assignment.valueRecipe(), blockers);
            if (blockers.size() > before) {
                blockers.subList(before, blockers.size()).clear();
                blockers.add("UNMATERIALIZABLE_ASSIGNMENT");
                return;
            }
        }
    }

    private void evaluateCollection(InputRecipeNode node, List<String> blockers) {
        for (InputRecipeMapEntry entry : node.entries().stream().sorted(Comparator.comparingInt(InputRecipeMapEntry::index)).toList()) {
            int before = blockers.size();
            evaluateNode(entry.keyRecipe(), blockers);
            if (blockers.size() > before) return;
            evaluateNode(entry.valueRecipe(), blockers);
            if (blockers.size() > before) return;
        }
        for (InputRecipeNode element : node.elements()) {
            int before = blockers.size();
            evaluateNode(element, blockers);
            if (blockers.size() > before) return;
        }
    }

    private void evaluateLocalTransform(InputRecipeNode node, List<String> blockers) {
        if (!"toSet".equals(node.transformName())) {
            blockers.add("UNSUPPORTED_TRANSFORM");
            return;
        }
        int before = blockers.size();
        evaluateNode(node.receiver(), blockers);
        if (blockers.size() == before && node.receiver() != null && "literal".equals(node.receiver().kind())
                && !(node.receiver().value() instanceof java.util.Collection<?>)) {
            blockers.add("UNSUPPORTED_TRANSFORM_RECEIVER");
        }
    }

    private void evaluatePropertyAccess(InputRecipeNode node, List<String> blockers) {
        int before = blockers.size();
        evaluateNode(node.receiver(), blockers);
        if (blockers.size() > before) {
            blockers.subList(before, blockers.size()).clear();
            blockers.add("UNMATERIALIZABLE_RECEIVER");
        }
    }

    private void addArgumentBlockers(InputRecipeArgument argument, List<String> blockers, String fallback) {
        if (argument.blockers().isEmpty()) {
            blockers.add(fallback);
        } else {
            blockers.addAll(argument.blockers());
        }
    }

    private void addNodeBlockers(InputRecipeNode node, List<String> blockers, String fallback) {
        if (node.blockers().isEmpty()) {
            blockers.add(fallback);
        } else {
            blockers.addAll(node.blockers());
        }
    }

    public record Readiness(
            boolean materializable,
            boolean staticRecipeReady,
            List<String> blockers,
            List<String> runtimeOwnedResolutions) {
        public Readiness {
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            runtimeOwnedResolutions = runtimeOwnedResolutions == null ? List.of() : List.copyOf(runtimeOwnedResolutions);
        }
    }
}
