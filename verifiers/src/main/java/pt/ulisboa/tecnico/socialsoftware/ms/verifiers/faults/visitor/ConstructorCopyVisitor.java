package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CopyContractArtifact;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Infers the bounded constructor-copy pattern qualified by the stale-write experiment. */
public final class ConstructorCopyVisitor {
    private static final Set<String> SCALARS = Set.of(
            "String", "Integer", "Long", "Boolean", "int", "long", "boolean",
            "java.lang.String", "java.lang.Integer", "java.lang.Long", "java.lang.Boolean");

    private final Map<String, SourceType> types = new java.util.TreeMap<>();

    public void visit(CompilationUnit unit, ApplicationAnalysisState state) {
        Path path = unit.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);
        unit.getTypes().stream()
                .filter(ClassOrInterfaceDeclaration.class::isInstance)
                .map(ClassOrInterfaceDeclaration.class::cast)
                .forEach(type -> types.put(fqn(unit, type), new SourceType(path, unit, type)));
    }

    public void finish(ApplicationAnalysisState state) {
        List<CopyContractArtifact.Contract> contracts = new ArrayList<>();
        for (Map.Entry<String, SourceType> entry : types.entrySet()) {
            SourceType target = entry.getValue();
            if (!target.type().isAnnotationPresent("Entity")) continue;
            target.type().getConstructors().stream()
                    .filter(constructor -> constructor.getParameters().size() == 1)
                    .forEach(constructor -> {
                        var parameter = constructor.getParameter(0);
                        String sourceName = resolve(target, parameter.getTypeAsString());
                        SourceType source = types.get(sourceName);
                        if (source == null || sourceName.equals(entry.getKey())) return;

                        Map<String, String> fields = new java.util.TreeMap<>();
                        List<CopyContractArtifact.Proof> proof = new ArrayList<>();
                        for (var statement : constructor.getBody().getStatements()) {
                            if (!(statement instanceof ExpressionStmt expressionStatement)
                                    || !(expressionStatement.getExpression() instanceof MethodCallExpr setter)
                                    || (setter.getScope().isPresent() && !setter.getScope().get().isThisExpr())
                                    || setter.getArguments().size() != 1
                                    || !(setter.getArgument(0) instanceof MethodCallExpr getter)
                                    || !getter.getArguments().isEmpty()
                                    || getter.getScope().isEmpty()
                                    || !getter.getScope().get().isNameExpr()
                                    || !getter.getScope().get().asNameExpr().getNameAsString()
                                            .equals(parameter.getNameAsString())) continue;
                            String from = getterField(source, getter.getNameAsString());
                            String to = setterField(target, setter.getNameAsString());
                            if (from == null || to == null || !scalar(source, from) || !scalar(target, to)) continue;
                            // Repeated copies from one source field are ambiguous at runtime; reject the constructor.
                            if (fields.putIfAbsent(from, to) != null) {
                                fields.clear();
                                proof.clear();
                                break;
                            }
                            proof.add(new CopyContractArtifact.Proof(from, to,
                                    statement.getBegin().map(position -> position.line).orElse(0),
                                    statement.toString()));
                        }
                        if (!fields.containsKey("aggregateId") || fields.size() < 2) return;
                        List<CopyContractArtifact.SourceFile> sourceFiles = sourceFiles(source, target);
                        if (sourceFiles.isEmpty()) return;
                        contracts.add(new CopyContractArtifact.Contract(sourceName, entry.getKey(), "aggregateId",
                                fields.get("aggregateId"), fields, proof, sourceFiles));
                    });
        }
        Map<String, CopyContractArtifact.Contract> unique = new java.util.TreeMap<>();
        Set<String> ambiguous = new java.util.HashSet<>();
        for (CopyContractArtifact.Contract contract : contracts) {
            String key = contract.sourceType() + "\u0000" + contract.targetType() + "\u0000" + contract.targetKey();
            if (ambiguous.contains(key)) continue;
            if (unique.putIfAbsent(key, contract) != null) {
                unique.remove(key);
                ambiguous.add(key);
            }
        }
        state.copyContractArtifact = new CopyContractArtifact(CopyContractArtifact.SCHEMA, null,
                CopyContractArtifact.LIMITATIONS, new ArrayList<>(unique.values()));
    }

    private String resolve(SourceType source, String name) {
        try {
            String resolved = source.type().getConstructors().stream()
                    .flatMap(constructor -> constructor.getParameters().stream())
                    .filter(parameter -> parameter.getTypeAsString().equals(name))
                    .findFirst().map(parameter -> parameter.getType().resolve().describe()).orElse(null);
            if (resolved != null && types.containsKey(resolved)) return resolved;
        } catch (RuntimeException ignored) {
            // Deterministic syntax fallbacks below keep incomplete classpaths explicit as no match.
        }
        if (types.containsKey(name)) return name;
        String local = source.unit().getPackageDeclaration().map(value -> value.getNameAsString() + ".").orElse("") + name;
        if (types.containsKey(local)) return local;
        List<String> matches = source.unit().getImports().stream()
                .map(value -> value.isAsterisk() ? value.getNameAsString() + "." + name : value.getNameAsString())
                .filter(value -> value.endsWith("." + name) && types.containsKey(value))
                .distinct().sorted().toList();
        return matches.size() == 1 ? matches.get(0) : "";
    }

    private static boolean scalar(SourceType source, String field) {
        return source.type().getFields().stream().flatMap(value -> value.getVariables().stream())
                .anyMatch(value -> value.getNameAsString().equals(field)
                        && SCALARS.contains(value.getTypeAsString()));
    }

    private static String getterField(SourceType source, String name) {
        var methods = source.type().getMethodsByName(name).stream()
                .filter(method -> method.getParameters().isEmpty()).toList();
        if (methods.size() != 1 || methods.get(0).getBody().isEmpty()) return null;
        var statements = methods.get(0).getBody().get().getStatements();
        if (statements.size() != 1 || !(statements.get(0) instanceof ReturnStmt returned)
                || returned.getExpression().isEmpty()) return null;
        return ownField(source, returned.getExpression().get());
    }

    private static String setterField(SourceType source, String name) {
        var methods = source.type().getMethodsByName(name).stream()
                .filter(method -> method.getParameters().size() == 1).toList();
        if (methods.size() != 1 || methods.get(0).getBody().isEmpty()) return null;
        var method = methods.get(0);
        var statements = method.getBody().get().getStatements();
        if (statements.size() != 1 || !(statements.get(0) instanceof ExpressionStmt expressionStatement)
                || !(expressionStatement.getExpression() instanceof AssignExpr assignment)
                || assignment.getOperator() != AssignExpr.Operator.ASSIGN
                || !assignment.getValue().isNameExpr()
                || !assignment.getValue().asNameExpr().getNameAsString()
                        .equals(method.getParameter(0).getNameAsString())) return null;
        if (assignment.getTarget().isNameExpr()
                && assignment.getTarget().asNameExpr().getNameAsString()
                        .equals(method.getParameter(0).getNameAsString())) return null;
        return ownField(source, assignment.getTarget());
    }

    private static String ownField(SourceType source, Expression expression) {
        String name = expression.isNameExpr() ? expression.asNameExpr().getNameAsString()
                : expression.isFieldAccessExpr() && expression.asFieldAccessExpr().getScope().isThisExpr()
                ? expression.asFieldAccessExpr().getNameAsString() : null;
        return name != null && source.type().getFieldByName(name).isPresent() ? name : null;
    }

    private static List<CopyContractArtifact.SourceFile> sourceFiles(SourceType source, SourceType target) {
        return java.util.stream.Stream.of(source.path(), target.path())
                .filter(java.util.Objects::nonNull).distinct().sorted()
                .map(path -> new CopyContractArtifact.SourceFile(stablePath(path), sha256(path))).toList();
    }

    private static String stablePath(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        for (int index = 0; index < normalized.getNameCount() - 2; index++) {
            if (normalized.getName(index).toString().equals("src")
                    && normalized.getName(index + 1).toString().equals("main")
                    && normalized.getName(index + 2).toString().equals("java")) {
                return normalized.subpath(index, normalized.getNameCount()).toString().replace('\\', '/');
            }
        }
        return normalized.getFileName().toString();
    }

    private static String sha256(Path path) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(path)));
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot hash copy-contract source " + path, exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String fqn(CompilationUnit unit, ClassOrInterfaceDeclaration type) {
        return unit.getPackageDeclaration().map(value -> value.getNameAsString() + ".").orElse("")
                + type.getNameAsString();
    }

    private record SourceType(Path path, CompilationUnit unit, ClassOrInterfaceDeclaration type) { }
}
