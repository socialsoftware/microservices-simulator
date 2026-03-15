package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.PropertyNode;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.SourceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.*;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext;

import java.nio.file.Path;
import java.util.*;

/**
 * Pass 4: Extracts test input seeds from Groovy/Spock test files.
 * <p>
 * Two-phase processing:
 * <ul>
 *     <li>Phase A ({@link #collectClass}): Collects all class nodes and their methods</li>
 *     <li>Phase B ({@link #analyzeCollectedClasses}): Analyzes test classes after all base classes are available</li>
 * </ul>
 */
public class SpockTestVisitor {
    private static final Logger logger = LoggerFactory.getLogger(SpockTestVisitor.class);
    private static final Set<String> SPOCK_LIFECYCLE_METHODS = Set.of(
            "setup", "cleanup", "setupSpec", "cleanupSpec");

    private final ApplicationAnalysisContext context;
    private final Map<String, Map<String, MethodNode>> classMethodRegistry = new LinkedHashMap<>();
    private final Map<String, ClassNode> classNodeRegistry = new LinkedHashMap<>();
    private final List<ClassNode> collectedClasses = new ArrayList<>();

    public SpockTestVisitor(ApplicationAnalysisContext context) {
        this.context = context;
    }

    // ── Phase A: Collect ───────────────────────────────────────────────

    /**
     * Collects a class node and its methods for later analysis.
     */
    public void collectClass(ClassNode node) {
        String className = node.getNameWithoutPackage();
        classNodeRegistry.put(className, node);

        Map<String, MethodNode> methods = new LinkedHashMap<>();
        for (MethodNode method : node.getMethods()) {
            if (!method.isSynthetic()) {
                methods.put(method.getName(), method);
            }
        }
        classMethodRegistry.put(className, methods);
        collectedClasses.add(node);
        logger.debug("Collected Groovy class: {}", className);
    }

    // ── Phase B: Analyze ───────────────────────────────────────────────

    /**
     * Analyzes all collected classes for saga constructor calls and input recipes.
     */
    public void analyzeCollectedClasses() {
        for (ClassNode node : collectedClasses) {
            Map<String, MethodNode> effectiveMethods = buildEffectiveMethods(node);
            if (isTestClass(node, effectiveMethods)) {
                analyzeTestClass(node, effectiveMethods);
            }
        }
    }

    private Map<String, MethodNode> buildEffectiveMethods(ClassNode node) {
        Map<String, MethodNode> effective = new LinkedHashMap<>();
        // Walk superclass chain, collecting inherited methods
        List<String> superChain = new ArrayList<>();
        ClassNode current = node.getSuperClass();
        while (current != null) {
            String superName = current.getNameWithoutPackage();
            if (classMethodRegistry.containsKey(superName)) {
                superChain.add(superName);
            }
            current = current.getSuperClass();
        }
        // Apply from most-ancestor to most-derived so overrides win
        Collections.reverse(superChain);
        for (String superName : superChain) {
            effective.putAll(classMethodRegistry.get(superName));
        }
        // Override with own methods
        effective.putAll(classMethodRegistry.getOrDefault(node.getNameWithoutPackage(), Map.of()));
        return effective;
    }

    private boolean isTestClass(ClassNode node, Map<String, MethodNode> methods) {
        if (node.isAbstract()) {
            return false;
        }
        return methods.containsKey("setup") || hasFeatureMethods(node);
    }

    private boolean hasFeatureMethods(ClassNode node) {
        Map<String, MethodNode> ownMethods = classMethodRegistry.getOrDefault(
                node.getNameWithoutPackage(), Map.of());
        return ownMethods.values().stream().anyMatch(this::isFeatureMethod);
    }

    private boolean isFeatureMethod(MethodNode method) {
        return method.isPublic() && !method.isStatic()
                && !SPOCK_LIFECYCLE_METHODS.contains(method.getName());
    }

    // ── Test class analysis ────────────────────────────────────────────

    private void analyzeTestClass(ClassNode node, Map<String, MethodNode> effectiveMethods) {
        String testClassName = node.getNameWithoutPackage();
        logger.info("Analyzing test class: {}", testClassName);

        // Extract setup recipes
        Map<String, InputRecipe> setupRecipes = new LinkedHashMap<>();
        MethodNode setupMethod = effectiveMethods.get("setup");
        if (setupMethod != null && setupMethod.getCode() != null) {
            extractRecipes(setupMethod.getCode(), setupRecipes);
        }

        // Build field type map for indirect saga resolution
        Map<String, String> fieldTypes = buildFieldTypeMap(node);

        // Search for saga constructor calls in setup + each feature method
        if (setupMethod != null) {
            findSagaSeeds(setupMethod, "setup", node, testClassName,
                    setupRecipes, effectiveMethods, fieldTypes);
        }

        Map<String, MethodNode> ownMethods = classMethodRegistry.getOrDefault(testClassName, Map.of());
        for (MethodNode method : ownMethods.values()) {
            if (isFeatureMethod(method) && method.getCode() != null) {
                // Feature methods may also have local variable declarations
                Map<String, InputRecipe> combinedRecipes = new LinkedHashMap<>(setupRecipes);
                extractRecipes(method.getCode(), combinedRecipes);
                findSagaSeeds(method, method.getName(), node, testClassName,
                        combinedRecipes, effectiveMethods, fieldTypes);
            }
        }
    }

    // ── Recipe extraction ──────────────────────────────────────────────

    private void extractRecipes(Statement code, Map<String, InputRecipe> recipes) {
        if (!(code instanceof BlockStatement block)) {
            return;
        }
        for (Statement stmt : block.getStatements()) {
            processStatement(unwrapLabeledStatement(stmt), recipes);
        }
    }

    private void processStatement(Statement stmt, Map<String, InputRecipe> recipes) {
        if (!(stmt instanceof ExpressionStatement exprStmt)) {
            return;
        }
        Expression expr = exprStmt.getExpression();

        if (expr instanceof DeclarationExpression decl) {
            // def x = expr
            String varName = decl.getVariableExpression().getName();
            InputExpression init = convertExpression(decl.getRightExpression());
            InputRecipe recipe = new InputRecipe(varName, init);
            collectDependencies(init, recipe);
            recipes.put(varName, recipe);

        } else if (expr instanceof BinaryExpression bin
                && bin.getOperation().getText().equals("=")) {
            // x = expr (reassignment)
            Expression left = bin.getLeftExpression();
            if (left instanceof VariableExpression ve) {
                String varName = ve.getName();
                InputExpression init = convertExpression(bin.getRightExpression());
                InputRecipe recipe = new InputRecipe(varName, init);
                collectDependencies(init, recipe);
                recipes.put(varName, recipe);
            }

        } else if (expr instanceof MethodCallExpression mc) {
            // x.setFoo(val) → mutation on existing recipe
            String varName = extractScopeName(mc.getObjectExpression());
            String methodName = mc.getMethodAsString();
            if (varName != null && methodName != null && methodName.startsWith("set")
                    && recipes.containsKey(varName)) {
                InputExpression mutation = convertExpression(mc);
                recipes.get(varName).addMutation(mutation);
                // Add dependencies from mutation args
                collectDependencies(mutation, recipes.get(varName));
            }
        }
    }

    private Statement unwrapLabeledStatement(Statement stmt) {
        // Spock labels (given:, and:, when:, then:) are just metadata on statements.
        // The statement itself is the real content. No unwrapping needed in Groovy AST —
        // labels are stored as statementLabels on the Statement, not as wrapper nodes.
        return stmt;
    }

    // ── Saga seed discovery ────────────────────────────────────────────

    private void findSagaSeeds(MethodNode method, String methodName, ClassNode testClass,
                               String testClassName, Map<String, InputRecipe> recipes,
                               Map<String, MethodNode> effectiveMethods,
                               Map<String, String> fieldTypes) {
        if (method.getCode() == null) {
            return;
        }

        // Direct: new SagaClass(...)
        List<ConstructorCallExpression> directCalls = new ArrayList<>();
        findDirectSagaConstructors(method.getCode(), directCalls);

        for (ConstructorCallExpression call : directCalls) {
            String sagaClassName = call.getType().getNameWithoutPackage();
            SagaInputSeed seed = buildSeed(sagaClassName, testClassName, testClass, methodName,
                    true, call.getArguments(), recipes);
            context.inputSeeds.add(seed);
            logger.info("  Direct saga seed: {} in {}.{}", sagaClassName, testClassName, methodName);
        }

        // Indirect via SagaCreationSite: someFunctionalities.createMethod(...)
        List<MethodCallExpression> indirectCalls = new ArrayList<>();
        findIndirectSagaCalls(method.getCode(), fieldTypes, indirectCalls);

        for (MethodCallExpression call : indirectCalls) {
            String scopeType = resolveCallScopeType(call, fieldTypes);
            String calledMethod = call.getMethodAsString();
            Optional<String> sagaClassName = context.resolveSagaCreation(scopeType, calledMethod);
            sagaClassName.ifPresent(saga -> {
                SagaInputSeed seed = buildSeed(saga, testClassName, testClass, methodName,
                        false, call.getArguments(), recipes);
                context.inputSeeds.add(seed);
                logger.info("  Indirect saga seed: {} via {}.{} in {}.{}",
                        saga, scopeType, calledMethod, testClassName, methodName);
            });
        }

        // Indirect via helper methods: someMethod() that contains saga constructors
        findHelperMethodSagaCalls(method.getCode(), testClassName, testClass, methodName,
                recipes, effectiveMethods, fieldTypes);
    }

    private void findDirectSagaConstructors(Statement code, List<ConstructorCallExpression> results) {
        code.visit(new CodeVisitorSupport() {
            @Override
            public void visitConstructorCallExpression(ConstructorCallExpression call) {
                String typeName = call.getType().getNameWithoutPackage();
                if (context.getSagaClassNames().contains(typeName)) {
                    results.add(call);
                }
                super.visitConstructorCallExpression(call);
            }
        });
    }

    private void findIndirectSagaCalls(Statement code, Map<String, String> fieldTypes,
                                       List<MethodCallExpression> results) {
        code.visit(new CodeVisitorSupport() {
            @Override
            public void visitMethodCallExpression(MethodCallExpression call) {
                String scopeType = resolveCallScopeType(call, fieldTypes);
                String calledMethod = call.getMethodAsString();
                if (scopeType != null && calledMethod != null
                        && context.resolveSagaCreation(scopeType, calledMethod).isPresent()) {
                    results.add(call);
                }
                super.visitMethodCallExpression(call);
            }
        });
    }

    private void findHelperMethodSagaCalls(Statement code, String testClassName, ClassNode testClass,
                                           String testMethodName, Map<String, InputRecipe> recipes,
                                           Map<String, MethodNode> effectiveMethods,
                                           Map<String, String> fieldTypes) {
        Set<String> visited = new HashSet<>();
        code.visit(new CodeVisitorSupport() {
            @Override
            public void visitMethodCallExpression(MethodCallExpression call) {
                String calledMethodName = call.getMethodAsString();
                Expression scope = call.getObjectExpression();
                // Only check implicit-this or explicit-this calls
                if (calledMethodName != null && isThisScope(scope)
                        && effectiveMethods.containsKey(calledMethodName)
                        && visited.add(calledMethodName)) {
                    MethodNode helperMethod = effectiveMethods.get(calledMethodName);
                    if (helperMethod.getCode() != null) {
                        // Recursively search helper method for saga constructors
                        List<ConstructorCallExpression> nested = new ArrayList<>();
                        findDirectSagaConstructors(helperMethod.getCode(), nested);
                        for (ConstructorCallExpression nestedCall : nested) {
                            String sagaClassName = nestedCall.getType().getNameWithoutPackage();
                            SagaInputSeed seed = buildSeed(sagaClassName, testClassName, testClass,
                                    testMethodName, true, nestedCall.getArguments(), recipes);
                            context.inputSeeds.add(seed);
                            logger.info("  Helper saga seed: {} via {}() in {}.{}",
                                    sagaClassName, calledMethodName, testClassName, testMethodName);
                        }
                    }
                }
                super.visitMethodCallExpression(call);
            }
        });
    }

    // ── Seed building ──────────────────────────────────────────────────

    private SagaInputSeed buildSeed(String sagaClassName, String testClassName,
                                    ClassNode testClass, String testMethodName,
                                    boolean direct, Expression argsExpr,
                                    Map<String, InputRecipe> allRecipes) {
        List<SagaConstructorArg> constructorArgs = new ArrayList<>();
        List<Expression> argList = unwrapArguments(argsExpr);

        for (int i = 0; i < argList.size(); i++) {
            InputExpression converted = convertExpression(argList.get(i));
            boolean infra = isInfrastructureArg(argList.get(i));
            constructorArgs.add(new SagaConstructorArg(i, converted, infra));
        }

        // Trace variable dependencies transitively
        Map<String, InputRecipe> neededRecipes = traceNeededRecipes(constructorArgs, allRecipes);

        Path testFile = null; // Not available from ClassNode alone; can be set from file path later

        return new SagaInputSeed(sagaClassName, testClassName, testFile, testMethodName,
                direct, constructorArgs, neededRecipes);
    }

    private Map<String, InputRecipe> traceNeededRecipes(List<SagaConstructorArg> args,
                                                        Map<String, InputRecipe> allRecipes) {
        Set<String> needed = new LinkedHashSet<>();
        for (SagaConstructorArg arg : args) {
            collectVariableRefs(arg.expression(), needed);
        }

        // Transitively expand
        Set<String> expanded = new LinkedHashSet<>();
        Queue<String> queue = new LinkedList<>(needed);
        while (!queue.isEmpty()) {
            String var = queue.poll();
            if (expanded.add(var)) {
                InputRecipe recipe = allRecipes.get(var);
                if (recipe != null) {
                    for (String dep : recipe.getDependsOn()) {
                        queue.add(dep);
                    }
                }
            }
        }

        // Collect recipes in dependency order
        Map<String, InputRecipe> result = new LinkedHashMap<>();
        for (String var : expanded) {
            if (allRecipes.containsKey(var)) {
                result.put(var, allRecipes.get(var));
            }
        }
        return result;
    }

    // ── Expression conversion ──────────────────────────────────────────

    /**
     * Converts a Groovy AST expression to an InputExpression tree.
     */
    InputExpression convertExpression(Expression expr) {
        if (expr instanceof ConstructorCallExpression ctor) {
            return convertConstructorCall(ctor);
        }
        if (expr instanceof MethodCallExpression mc) {
            return convertMethodCall(mc);
        }
        if (expr instanceof PropertyExpression prop) {
            return convertPropertyExpression(prop);
        }
        if (expr instanceof VariableExpression ve) {
            return convertVariableExpression(ve);
        }
        if (expr instanceof ConstantExpression ce) {
            return new InputExpression.Literal(ce.getValue());
        }
        if (expr instanceof ListExpression list) {
            List<InputExpression> elements = list.getExpressions().stream()
                    .map(this::convertExpression)
                    .toList();
            return new InputExpression.ListExpr(elements);
        }
        if (expr instanceof DeclarationExpression decl) {
            return convertExpression(decl.getRightExpression());
        }
        if (expr instanceof CastExpression cast) {
            return convertExpression(cast.getExpression());
        }
        // Fallback for unknown expression types
        return new InputExpression.Literal(expr.getText());
    }

    private InputExpression convertConstructorCall(ConstructorCallExpression ctor) {
        String typeName = ctor.getType().getNameWithoutPackage();
        List<InputExpression> positionalArgs = new ArrayList<>();
        Map<String, InputExpression> namedArgs = new LinkedHashMap<>();

        Expression argsExpr = ctor.getArguments();
        if (argsExpr instanceof TupleExpression tuple) {
            for (Expression e : tuple.getExpressions()) {
                if (e instanceof NamedArgumentListExpression namedArgList) {
                    for (MapEntryExpression entry : namedArgList.getMapEntryExpressions()) {
                        String key = entry.getKeyExpression().getText();
                        namedArgs.put(key, convertExpression(entry.getValueExpression()));
                    }
                } else {
                    positionalArgs.add(convertExpression(e));
                }
            }
        } else if (argsExpr instanceof ArgumentListExpression argList) {
            for (Expression e : argList.getExpressions()) {
                positionalArgs.add(convertExpression(e));
            }
        }

        return new InputExpression.ConstructorCall(typeName, positionalArgs, namedArgs);
    }

    private InputExpression convertMethodCall(MethodCallExpression mc) {
        String methodName = mc.getMethodAsString();
        String scope = extractScopeName(mc.getObjectExpression());
        if (scope == null) {
            scope = mc.getObjectExpression().getText();
        }

        // Normalize getAggregateId() / isX() with no args to AggregateIdRef / GetterCall
        List<Expression> args = unwrapArguments(mc.getArguments());
        if (args.isEmpty()) {
            if ("getAggregateId".equals(methodName)) {
                return new InputExpression.AggregateIdRef(scope);
            }
            if (methodName != null && (methodName.startsWith("get") || methodName.startsWith("is"))) {
                String property = methodName.startsWith("get")
                        ? decapitalize(methodName.substring(3))
                        : decapitalize(methodName.substring(2));
                return new InputExpression.GetterCall(scope, property);
            }
        }

        List<InputExpression> convertedArgs = args.stream()
                .map(this::convertExpression)
                .toList();
        return new InputExpression.MethodCall(scope, methodName, convertedArgs);
    }

    private InputExpression convertPropertyExpression(PropertyExpression prop) {
        String scope = extractScopeName(prop.getObjectExpression());
        if (scope == null) {
            scope = prop.getObjectExpression().getText();
        }
        String property = prop.getPropertyAsString();

        if ("aggregateId".equals(property)) {
            return new InputExpression.AggregateIdRef(scope);
        }
        return new InputExpression.GetterCall(scope, property);
    }

    private InputExpression convertVariableExpression(VariableExpression ve) {
        String name = ve.getName();
        if ("this".equals(name)) {
            return new InputExpression.VariableRef(name);
        }
        if (isConstantName(name)) {
            return new InputExpression.ConstantRef(name);
        }
        return new InputExpression.VariableRef(name);
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Map<String, String> buildFieldTypeMap(ClassNode node) {
        Map<String, String> fieldTypes = new LinkedHashMap<>();
        // Walk superclass chain
        ClassNode current = node;
        while (current != null) {
            // Check properties first (Groovy property declarations like "Type name")
            for (PropertyNode prop : current.getProperties()) {
                String typeName = prop.getType().getNameWithoutPackage();
                if (!"Object".equals(typeName)) {
                    fieldTypes.putIfAbsent(prop.getName(), typeName);
                }
            }
            // Also check fields directly (for explicit field declarations)
            for (FieldNode field : current.getFields()) {
                String typeName = field.getType().getNameWithoutPackage();
                if (!"Object".equals(typeName)) {
                    fieldTypes.putIfAbsent(field.getName(), typeName);
                }
            }
            String superName = current.getSuperClass() != null
                    ? current.getSuperClass().getNameWithoutPackage() : null;
            current = superName != null ? classNodeRegistry.get(superName) : null;
        }
        return fieldTypes;
    }

    private String resolveCallScopeType(MethodCallExpression call, Map<String, String> fieldTypes) {
        String scopeName = extractScopeName(call.getObjectExpression());
        if (scopeName == null) {
            return null;
        }
        return fieldTypes.get(scopeName);
    }

    private boolean isInfrastructureArg(Expression expr) {
        if (expr instanceof VariableExpression ve) {
            return isInfrastructureName(ve.getName());
        }
        return false;
    }

    private static boolean isInfrastructureName(String name) {
        return name.startsWith("unitOfWork") || "commandGateway".equals(name);
    }

    private static boolean isConstantName(String name) {
        return name.matches("[A-Z][A-Z0-9_]*");
    }

    private static boolean isThisScope(Expression scope) {
        return scope instanceof VariableExpression ve && "this".equals(ve.getName());
    }

    private static String extractScopeName(Expression scope) {
        if (scope instanceof VariableExpression ve) {
            return "this".equals(ve.getName()) ? "this" : ve.getName();
        }
        return null;
    }

    private static List<Expression> unwrapArguments(Expression argsExpr) {
        if (argsExpr instanceof ArgumentListExpression argList) {
            return argList.getExpressions();
        }
        if (argsExpr instanceof TupleExpression tuple) {
            return tuple.getExpressions();
        }
        return List.of();
    }

    private void collectDependencies(InputExpression expr, InputRecipe recipe) {
        collectVariableRefs(expr, new LinkedHashSet<>()).forEach(recipe::addDependency);
    }

    private Set<String> collectVariableRefs(InputExpression expr, Set<String> refs) {
        switch (expr) {
            case InputExpression.VariableRef ref -> {
                if (!"this".equals(ref.name())) {
                    refs.add(ref.name());
                }
            }
            case InputExpression.AggregateIdRef ref -> refs.add(ref.variableName());
            case InputExpression.GetterCall ref -> refs.add(ref.scope());
            case InputExpression.MethodCall mc -> {
                if (!"this".equals(mc.scope())) {
                    refs.add(mc.scope());
                }
                mc.args().forEach(a -> collectVariableRefs(a, refs));
            }
            case InputExpression.ConstructorCall cc -> {
                cc.args().forEach(a -> collectVariableRefs(a, refs));
                cc.namedArgs().values().forEach(a -> collectVariableRefs(a, refs));
            }
            case InputExpression.ListExpr list -> list.elements().forEach(e -> collectVariableRefs(e, refs));
            case InputExpression.Literal ignored -> {}
            case InputExpression.ConstantRef ignored -> {}
        }
        return refs;
    }

    private static String decapitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }
}
