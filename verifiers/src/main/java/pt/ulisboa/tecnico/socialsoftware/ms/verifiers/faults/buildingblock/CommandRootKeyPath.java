package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import com.github.javaparser.ast.body.ConstructorDeclaration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A bounded semantic path from one public command constructor to
 * {@code Command.rootAggregateId}.
 */
public record CommandRootKeyPath(
        String constructorSignature,
        Integer constructorParameterIndex,
        List<String> propertyPath,
        String literalText) {

    public CommandRootKeyPath {
        propertyPath = propertyPath == null ? List.of() : List.copyOf(propertyPath);
        if ((constructorParameterIndex == null) == (literalText == null)) {
            throw new IllegalArgumentException("A command root-key path must have exactly one source");
        }
    }

    public static CommandRootKeyPath parameter(String signature, int parameterIndex,
                                                List<String> propertyPath) {
        return new CommandRootKeyPath(signature, parameterIndex, propertyPath, null);
    }

    public static CommandRootKeyPath literal(String signature, String literalText) {
        return new CommandRootKeyPath(signature, null, List.of(), literalText);
    }

    public static CommandRootKeyPath baseCommand(String signature) {
        return parameter(signature, 2, List.of());
    }

    public CommandRootKeyPath withSignature(String signature) {
        return new CommandRootKeyPath(signature, constructorParameterIndex, propertyPath, literalText);
    }

    public static String signature(ConstructorDeclaration constructor) {
        try {
            return constructor.resolve().getQualifiedSignature();
        } catch (Exception ignored) {
            // Preserve a stable source-only fallback for partially resolved fixtures.
        }
        String owner = constructor.findAncestor(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration.class)
                .flatMap(com.github.javaparser.ast.body.ClassOrInterfaceDeclaration::getFullyQualifiedName)
                .orElse(constructor.getNameAsString());
        String parameterTypes = constructor.getParameters().stream()
                .map(parameter -> {
                    try {
                        return parameter.getType().resolve().describe();
                    } catch (Exception exception) {
                        return parameter.getTypeAsString();
                    }
                })
                .collect(Collectors.joining(","));
        return owner + "(" + parameterTypes + ")";
    }
}
