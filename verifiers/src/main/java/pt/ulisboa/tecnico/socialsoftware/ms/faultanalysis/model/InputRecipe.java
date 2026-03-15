package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.util.ArrayList;
import java.util.List;

/**
 * How one test variable was built: its initial value, subsequent mutations (setter calls),
 * and which other variables it depends on.
 */
public class InputRecipe {
    private final String variableName;
    private final InputExpression initializer;
    private final List<InputExpression> mutations = new ArrayList<>();
    private final List<String> dependsOn = new ArrayList<>();

    public InputRecipe(String variableName, InputExpression initializer) {
        this.variableName = variableName;
        this.initializer = initializer;
    }

    public String getVariableName() {
        return variableName;
    }

    public InputExpression getInitializer() {
        return initializer;
    }

    public List<InputExpression> getMutations() {
        return mutations;
    }

    public List<String> getDependsOn() {
        return dependsOn;
    }

    public void addMutation(InputExpression mutation) {
        mutations.add(mutation);
    }

    public void addDependency(String varName) {
        if (!dependsOn.contains(varName)) {
            dependsOn.add(varName);
        }
    }
}
