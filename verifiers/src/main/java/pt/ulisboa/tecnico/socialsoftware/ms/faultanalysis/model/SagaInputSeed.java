package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Complete extracted input for one saga invocation found in a Spock test.
 * Contains the constructor arguments and all recipes (variable-build instructions)
 * transitively needed to replay the saga creation.
 */
public class SagaInputSeed {
    private final String sagaClassName;
    private final String testClassName;
    private final Path testFile;
    private final String testMethodName;
    private final boolean directConstruction;
    private final List<SagaConstructorArg> constructorArgs;
    private final Map<String, InputRecipe> recipes;

    public SagaInputSeed(String sagaClassName, String testClassName, Path testFile,
                         String testMethodName, boolean directConstruction,
                         List<SagaConstructorArg> constructorArgs,
                         Map<String, InputRecipe> recipes) {
        this.sagaClassName = sagaClassName;
        this.testClassName = testClassName;
        this.testFile = testFile;
        this.testMethodName = testMethodName;
        this.directConstruction = directConstruction;
        this.constructorArgs = constructorArgs;
        this.recipes = recipes;
    }

    public String getSagaClassName() {
        return sagaClassName;
    }

    public String getTestClassName() {
        return testClassName;
    }

    public Path getTestFile() {
        return testFile;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public boolean isDirectConstruction() {
        return directConstruction;
    }

    public List<SagaConstructorArg> getConstructorArgs() {
        return constructorArgs;
    }

    public Map<String, InputRecipe> getRecipes() {
        return recipes;
    }
}
