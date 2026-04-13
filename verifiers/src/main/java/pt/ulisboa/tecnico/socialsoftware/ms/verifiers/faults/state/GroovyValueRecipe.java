package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyValueRecipe(GroovyValueKind kind, String text, List<GroovyValueRecipe> children) {
}
