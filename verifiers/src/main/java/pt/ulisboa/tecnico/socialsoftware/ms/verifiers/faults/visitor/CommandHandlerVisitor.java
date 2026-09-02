package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandHandlerBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandRootKeyPath;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.ServiceBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Analyses CommandHandler subclasses to extract command dispatch mappings.
 * <p>
 * Phase A: Collect injected service fields (both @Autowired field and constructor injection).
 *          Types are resolved to FQN via the symbol solver before lookup against state.services.
 * <p>
 * Phase B: Extract the aggregate type name from getAggregateTypeName() method.
 * <p>
 * Phase C: For each private handler method with a Command parameter, find the first service call
 *          and resolve it to a CommandDispatchInfo. Handles both plain scope (itemService.x())
 *          and field-access scope (this.itemService.x()).
 */
public class CommandHandlerVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandlerVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            if (TypeUtils.isSubclassOf(decl, Command.class)) {
                indexCommandRootKeyPaths(decl, state);
            }
            if (!TypeUtils.isSubclassOf(decl, CommandHandler.class)) {
                return;
            }

            String packageName = decl.resolve().getPackageName();
            String fqn = decl.getFullyQualifiedName().orElseGet(() -> {
                logger.warn("Could not get FQN for {}", decl.getNameAsString());
                return decl.getNameAsString();
            });
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);

            // Phase A: Build map of field-name → ServiceBuildingBlock for injected services
            Map<String, ServiceBuildingBlock> serviceFields = resolveServiceFields(decl, state);

            // Phase B: Extract aggregate type name from getAggregateTypeName()
            Optional<String> aggregateTypeName = extractAggregateTypeName(decl);

            CommandHandlerBuildingBlock block = new CommandHandlerBuildingBlock(
                    filePath, packageName, fqn, aggregateTypeName.orElse(null));

            // Phase C: Map command types to CommandDispatchInfo
            mapCommandsToDispatchInfo(decl, block, serviceFields, aggregateTypeName.orElse(null));

            state.commandHandlers.add(block);
            logger.info("CommandHandler {}: {}", fqn, block.getCommandDispatch());
        });
    }

    /**
     * Indexes command constructor semantics independently of handler discovery order.
     * The complete command-handler pass finishes before workflow analysis starts, so a
     * handler may be visited before or after the corresponding command declaration.
     */
    private void indexCommandRootKeyPaths(ClassOrInterfaceDeclaration declaration,
                                          ApplicationAnalysisState state) {
        String commandTypeFqn = declaration.getFullyQualifiedName().orElse(null);
        if (commandTypeFqn == null) {
            return;
        }

        declaration.getConstructors().stream()
                .filter(ConstructorDeclaration::isPublic)
                .forEach(constructor -> resolveCommandRootKeyPath(constructor, new LinkedHashSet<>())
                        .map(path -> path.withSignature(CommandRootKeyPath.signature(constructor)))
                        .ifPresent(path -> state.addCommandRootKeyPath(commandTypeFqn, path)));
    }

    private Optional<CommandRootKeyPath> resolveCommandRootKeyPath(
            ConstructorDeclaration constructor,
            Set<ConstructorDeclaration> activePath) {
        if (!activePath.add(constructor)) {
            return Optional.empty();
        }

        try {
            Optional<ExplicitConstructorInvocationStmt> invocation = constructor.getBody().getStatements().stream()
                    .filter(statement -> statement.isExplicitConstructorInvocationStmt())
                    .map(statement -> statement.asExplicitConstructorInvocationStmt())
                    .findFirst();
            if (invocation.isEmpty()) {
                return Optional.empty();
            }

            if (!invocation.get().isThis()) {
                if (invocation.get().getArguments().size() != 3 || !resolvesCommandBaseConstructor(invocation.get())) {
                    return Optional.empty();
                }
                return pathFromConstructorExpression(
                        invocation.get().getArgument(2), constructor, CommandRootKeyPath.signature(constructor));
            }

            ConstructorDeclaration target = resolveDelegatedConstructor(invocation.get());
            if (target == null) {
                return Optional.empty();
            }
            Optional<CommandRootKeyPath> downstream = resolveCommandRootKeyPath(target, activePath);
            if (downstream.isEmpty()) {
                return Optional.empty();
            }
            if (downstream.get().literalText() != null) {
                return Optional.of(CommandRootKeyPath.literal(
                        CommandRootKeyPath.signature(constructor), downstream.get().literalText()));
            }

            int targetIndex = downstream.get().constructorParameterIndex();
            if (targetIndex < 0 || targetIndex >= invocation.get().getArguments().size()) {
                return Optional.empty();
            }
            Optional<CommandRootKeyPath> upstream = pathFromConstructorExpression(
                    invocation.get().getArgument(targetIndex), constructor, CommandRootKeyPath.signature(constructor));
            if (upstream.isEmpty() || upstream.get().literalText() != null) {
                return upstream;
            }

            List<String> combinedProperties = new java.util.ArrayList<>(upstream.get().propertyPath());
            combinedProperties.addAll(downstream.get().propertyPath());
            if (combinedProperties.size() > 1) {
                return Optional.empty();
            }
            return Optional.of(CommandRootKeyPath.parameter(
                    CommandRootKeyPath.signature(constructor),
                    upstream.get().constructorParameterIndex(),
                    combinedProperties));
        } finally {
            activePath.remove(constructor);
        }
    }

    private boolean resolvesCommandBaseConstructor(ExplicitConstructorInvocationStmt invocation) {
        try {
            return invocation.resolve().declaringType().getQualifiedName().equals(Command.class.getName());
        } catch (Exception exception) {
            logger.debug("Could not resolve command superclass constructor '{}': {}",
                    invocation, exception.getMessage());
            return false;
        }
    }

    private Optional<CommandRootKeyPath> pathFromConstructorExpression(
            Expression expression,
            ConstructorDeclaration constructor,
            String signature) {
        Expression unwrapped = expression;
        while (unwrapped.isEnclosedExpr()) {
            unwrapped = unwrapped.asEnclosedExpr().getInner();
        }
        if (unwrapped.isNullLiteralExpr()) {
            return Optional.empty();
        }
        if (isSupportedLiteralExpression(unwrapped)) {
            return Optional.of(CommandRootKeyPath.literal(signature, unwrapped.toString()));
        }

        Optional<ParameterExpressionPath> parameterPath = directParameterExpressionPath(unwrapped, constructor);
        return parameterPath.map(path -> CommandRootKeyPath.parameter(
                signature, path.parameterIndex(), path.propertyPath()));
    }

    private boolean isSupportedLiteralExpression(Expression expression) {
        Expression unwrapped = expression;
        while (unwrapped.isEnclosedExpr()) {
            unwrapped = unwrapped.asEnclosedExpr().getInner();
        }
        if (unwrapped.isLiteralExpr()) {
            return true;
        }
        if (!unwrapped.isUnaryExpr()) {
            return false;
        }
        var unary = unwrapped.asUnaryExpr();
        if (unary.getOperator() != UnaryExpr.Operator.PLUS
                && unary.getOperator() != UnaryExpr.Operator.MINUS) {
            return false;
        }
        Expression operand = unary.getExpression();
        return operand.isIntegerLiteralExpr()
                || operand.isLongLiteralExpr()
                || operand.isDoubleLiteralExpr();
    }

    private Optional<ParameterExpressionPath> directParameterExpressionPath(
            Expression expression,
            ConstructorDeclaration constructor) {
        if (expression.isNameExpr()) {
            Integer index = constructorParameterIndex(expression.asNameExpr(), constructor);
            return index == null
                    ? Optional.empty()
                    : Optional.of(new ParameterExpressionPath(index, List.of()));
        }

        if (expression.isMethodCallExpr()) {
            MethodCallExpr call = expression.asMethodCallExpr();
            if (!call.getArguments().isEmpty() || call.getScope().isEmpty()) {
                return Optional.empty();
            }
            String property = getterProperty(call.getNameAsString());
            if (property == null) {
                return Optional.empty();
            }
            return directParameterExpressionPath(call.getScope().orElseThrow(), constructor)
                    .filter(path -> path.propertyPath().isEmpty())
                    .map(path -> new ParameterExpressionPath(path.parameterIndex(), List.of(property)));
        }

        if (expression.isFieldAccessExpr()) {
            FieldAccessExpr access = expression.asFieldAccessExpr();
            return directParameterExpressionPath(access.getScope(), constructor)
                    .filter(path -> path.propertyPath().isEmpty())
                    .map(path -> new ParameterExpressionPath(
                            path.parameterIndex(), List.of(access.getNameAsString())));
        }

        return Optional.empty();
    }

    private Integer constructorParameterIndex(NameExpr name, ConstructorDeclaration constructor) {
        try {
            return name.resolve().toAst()
                    .filter(Parameter.class::isInstance)
                    .map(Parameter.class::cast)
                    .filter(constructor.getParameters()::contains)
                    .map(constructor.getParameters()::indexOf)
                    .filter(index -> index >= 0)
                    .orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private String getterProperty(String methodName) {
        String suffix;
        if (methodName.matches("get[A-Z].*")) {
            suffix = methodName.substring(3);
        } else if (methodName.matches("is[A-Z].*")) {
            suffix = methodName.substring(2);
        } else {
            return null;
        }
        return Character.toLowerCase(suffix.charAt(0)) + suffix.substring(1);
    }

    private ConstructorDeclaration resolveDelegatedConstructor(ExplicitConstructorInvocationStmt invocation) {
        try {
            return invocation.resolve().toAst()
                    .filter(ConstructorDeclaration.class::isInstance)
                    .map(ConstructorDeclaration.class::cast)
                    .orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private record ParameterExpressionPath(int parameterIndex, List<String> propertyPath) { }

    /**
     * Phase A: Resolves service fields from both @Autowired field injection and constructor injection.
     * Types are resolved to their FQN via the symbol solver before lookup, so that a field declared
     * as {@code ItemService itemService} correctly matches the FQN-keyed service map entry.
     * Also falls back to the interface-to-service index for interface-typed injection points.
     */
    private Map<String, ServiceBuildingBlock> resolveServiceFields(ClassOrInterfaceDeclaration decl,
                                                                   ApplicationAnalysisState state) {
        Map<String, ServiceBuildingBlock> serviceTypeMap = new LinkedHashMap<>();
        state.services.forEach(s -> serviceTypeMap.put(s.getFqn(), s));

        Map<String, ServiceBuildingBlock> serviceFields = new LinkedHashMap<>();

        // Field injection
        decl.getFields().forEach(field ->
                lookupServiceByType(field.getCommonType(), serviceTypeMap, state)
                        .ifPresent(sb -> field.getVariables()
                                .forEach(var -> serviceFields.put(var.getNameAsString(), sb))));

        // Constructor injection
        decl.getConstructors().forEach(ctor ->
                ctor.getParameters().forEach(param ->
                        lookupServiceByType(param.getType(), serviceTypeMap, state)
                                .ifPresent(sb -> {
                                    String paramName = param.getNameAsString();
                                    ctor.findAll(AssignExpr.class).forEach(assign ->
                                            assign.getValue().ifNameExpr(nameExpr -> {
                                                if (nameExpr.getNameAsString().equals(paramName)) {
                                                    extractAssignedFieldName(assign.getTarget())
                                                            .ifPresent(fieldName ->
                                                                    serviceFields.put(fieldName, sb));
                                                }
                                            }));
                                })));

        return serviceFields;
    }

    /**
     * Resolves a type to a ServiceBuildingBlock.
     * Tries the concrete FQN first, then falls back to the interface-to-service index.
     * Logs a WARN and returns empty if the interface has multiple implementations.
     */
    private Optional<ServiceBuildingBlock> lookupServiceByType(Type type,
                                                                Map<String, ServiceBuildingBlock> serviceTypeMap,
                                                                ApplicationAnalysisState state) {
        String typeFqn = resolveTypeFqn(type);

        ServiceBuildingBlock direct = serviceTypeMap.get(typeFqn);
        if (direct != null) return Optional.of(direct);

        List<ServiceBuildingBlock> impls = state.interfaceToServices.getOrDefault(typeFqn, List.of());
        if (impls.size() == 1) return Optional.of(impls.get(0));
        if (impls.size() > 1) {
            logger.warn("Ambiguous interface injection: '{}' is implemented by {} @Service classes — " +
                        "injection point skipped. If this is intentional (e.g. Spring profiles), " +
                        "resolve by reading application.properties/yaml (not yet supported).",
                        typeFqn, impls.size());
        }
        return Optional.empty();
    }

    /**
     * Resolves a type to its fully-qualified name using the symbol solver.
     * Falls back to the simple name string if resolution fails (e.g., missing classpath entry).
     */
    private String resolveTypeFqn(Type type) {
        try {
            return type.resolve().describe();
        } catch (Exception e) {
            logger.debug("Could not resolve type '{}': {}", type.asString(), e.getMessage());
            return type.asString();
        }
    }

    /**
     * Extracts the field name from an assignment target (handles both FieldAccessExpr and NameExpr).
     */
    private Optional<String> extractAssignedFieldName(Expression target) {
        if (target.isFieldAccessExpr()) {
            return Optional.of(target.asFieldAccessExpr().getNameAsString());
        }
        if (target.isNameExpr()) {
            return Optional.of(target.asNameExpr().getNameAsString());
        }
        return Optional.empty();
    }

    /**
     * Phase B: Extracts the aggregate type name from the getAggregateTypeName() method.
     * Handles both direct string literals ({@code return "Item";}) and references to
     * {@code private static final String} constants ({@code return AGGREGATE_TYPE;}).
     */
    private Optional<String> extractAggregateTypeName(ClassOrInterfaceDeclaration decl) {
        return decl.getMethods().stream()
                .filter(m -> m.getNameAsString().equals("getAggregateTypeName"))
                .findFirst()
                .flatMap(m -> m.findFirst(ReturnStmt.class))
                .flatMap(ReturnStmt::getExpression)
                .flatMap(expr -> {
                    if (expr.isStringLiteralExpr()) {
                        return Optional.of(expr.asStringLiteralExpr().asString());
                    }
                    if (expr.isNameExpr()) {
                        return resolveStaticFinalStringField(decl, expr.asNameExpr().getNameAsString());
                    }
                    if (expr.isFieldAccessExpr()) {
                        return resolveStaticFinalStringField(decl, expr.asFieldAccessExpr().getNameAsString());
                    }
                    return Optional.empty();
                });
    }

    /**
     * Finds a {@code private static final String} field in {@code decl} with the given name
     * and returns its string literal initializer value.
     */
    private Optional<String> resolveStaticFinalStringField(ClassOrInterfaceDeclaration decl, String name) {
        return decl.getFields().stream()
                .filter(f -> f.isStatic() && f.isFinal())
                .flatMap(f -> f.getVariables().stream())
                .filter(v -> v.getNameAsString().equals(name))
                .findFirst()
                .flatMap(VariableDeclarator::getInitializer)
                .filter(Expression::isStringLiteralExpr)
                .map(init -> init.asStringLiteralExpr().asString());
    }

    /**
     * Phase C: For each private handler method with a Command subtype parameter, map the command type
     * to its dispatch info by finding the first service call in the method body.
     * Handles both plain scope ({@code service.method()}) and field-access scope
     * ({@code this.service.method()}).
     */
    private void mapCommandsToDispatchInfo(ClassOrInterfaceDeclaration decl, CommandHandlerBuildingBlock block,
                                           Map<String, ServiceBuildingBlock> serviceFields,
                                           String aggregateName) {
        decl.getMethods().stream()
                .filter(MethodDeclaration::isPrivate)
                .forEach(method -> {
                    method.getParameters().stream()
                            .filter(p -> TypeUtils.isSubtypeOf(p.getType(), Command.class))
                            .findFirst()
                            .ifPresent(commandParam -> {
                                resolveCommandTypeFqn(commandParam.getType()).ifPresent(commandType ->
                                        findFirstServiceCall(method, serviceFields)
                                                .or(() -> findFirstServiceCallViaDelegateMethod(method, decl, serviceFields))
                                                .ifPresent(call -> {
                                                    Expression scope = call.getScope().get();
                                                    String fieldName = scope.isFieldAccessExpr()
                                                            ? scope.asFieldAccessExpr().getNameAsString()
                                                            : scope.asNameExpr().getNameAsString();
                                                    ServiceBuildingBlock serviceBB = serviceFields.get(fieldName);
                                                    String serviceMethodSignature = TypeUtils.buildCallSignature(call);

                                                    block.addCommandDispatch(commandType,
                                                            new CommandDispatchInfo(
                                                                    serviceBB,
                                                                    serviceMethodSignature,
                                                                    aggregateName));
                                                }));
                            });
                });
    }

    /**
     * Resolves a command type to its fully-qualified name.
     * If resolution fails, the command is skipped instead of falling back to a simple name.
     */
    private Optional<String> resolveCommandTypeFqn(Type type) {
        try {
            return Optional.of(type.resolve().describe());
        } catch (Exception e) {
            logger.warn("Could not resolve command type '{}': {}", type.asString(), e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Looks one level into private helper methods in the same class.
     * Depth limit: 1. No recursive descent.
     */
    private Optional<MethodCallExpr> findFirstServiceCallViaDelegateMethod(
            MethodDeclaration method,
            ClassOrInterfaceDeclaration decl,
            Map<String, ServiceBuildingBlock> serviceFields) {

        return method.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope()
                        .map(Expression::isThisExpr)
                        .orElse(true))
                .map(call -> resolveDelegateMethod(decl, call)
                        .flatMap(delegate -> findFirstServiceCall(delegate, serviceFields)))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<MethodDeclaration> resolveDelegateMethod(ClassOrInterfaceDeclaration decl,
                                                              MethodCallExpr call) {
        String callSignature = TypeUtils.buildCallSignature(call);
        return decl.getMethods().stream()
                .filter(m -> m.isPrivate() && TypeUtils.buildSignature(m).equals(callSignature))
                .findFirst();
    }

    /**
     * Finds the first method call whose scope is a known service field.
     * Handles both plain NameExpr ({@code service.method()}) and
     * FieldAccessExpr ({@code this.service.method()}) scopes.
     */
    private Optional<MethodCallExpr> findFirstServiceCall(MethodDeclaration method,
                                                          Map<String, ServiceBuildingBlock> serviceFields) {
        return method.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope()
                        .filter(scope -> {
                            if (scope.isNameExpr()) {
                                return serviceFields.containsKey(scope.asNameExpr().getNameAsString());
                            }
                            if (scope.isFieldAccessExpr()) {
                                return serviceFields.containsKey(scope.asFieldAccessExpr().getNameAsString());
                            }
                            return false;
                        })
                        .isPresent())
                .findFirst();
    }
}
