package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.FieldNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.ModuleNode;
import org.codehaus.groovy.ast.expr.BinaryExpression;
import org.codehaus.groovy.ast.expr.ConstantExpression;
import org.codehaus.groovy.ast.expr.ConstructorCallExpression;
import org.codehaus.groovy.ast.expr.DeclarationExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyImportMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceClassMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GroovyConstructorInputTraceVisitor {

    private static final Set<String> TRACKED_WORKFLOW_METHODS = Set.of(
            "executeWorkflow",
            "executeUntilStep",
            "resumeWorkflow"
    );

    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");
    private static final int MAX_TRACE_DEPTH = 12;

    public void visit(GroovySourceIndex sourceIndex, ApplicationAnalysisState state) {
        Objects.requireNonNull(sourceIndex, "sourceIndex cannot be null");
        Objects.requireNonNull(state, "state cannot be null");

        Map<String, GroovySourceClassMetadata> metadataByClassFqn = sourceIndex.getClassesByFqn();
        Map<Path, List<Map.Entry<String, GroovySourceClassMetadata>>> metadataBySourceFile = new LinkedHashMap<>();
        metadataByClassFqn.entrySet().forEach(entry ->
                metadataBySourceFile.computeIfAbsent(entry.getValue().sourceFile(), ignored -> new ArrayList<>()).add(entry));

        Map<String, ClassNode> classNodesByFqn = new LinkedHashMap<>();
        metadataBySourceFile.forEach((sourceFile, metadataEntries) -> {
            ModuleNode moduleNode = parseSourceFile(sourceFile);
            if (moduleNode == null) {
                return;
            }

            moduleNode.getClasses().forEach(classNode -> classNodesByFqn.put(classNode.getName(), classNode));
        });

        metadataByClassFqn.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(entry -> {
                    String classFqn = entry.getKey();
                    if (!isSourceBackedSpecificationClass(classFqn, metadataByClassFqn, sourceIndex)) {
                        return;
                    }

                    List<InheritanceLayer> hierarchy = resolveSourceBackedHierarchy(classFqn,
                            metadataByClassFqn, classNodesByFqn, sourceIndex);
                    if (hierarchy.isEmpty()) {
                        return;
                    }

                    traceClass(classFqn, hierarchy, state);
                });
    }

    private void traceClass(String traceSourceClassFqn,
                            List<InheritanceLayer> hierarchy,
                            ApplicationAnalysisState state) {
        InheritanceLayer targetLayer = hierarchy.get(hierarchy.size() - 1);
        Map<String, TraceBuilder> classFieldScopes = new LinkedHashMap<>();
        Map<String, Expression> classFieldExpressionScopes = new LinkedHashMap<>();
        List<TraceBuilder> tracedBuilders = new ArrayList<>();
        Map<String, List<MethodResolutionContext>> methodsByName = buildHelperMethodsByName(hierarchy);
        Map<String, Integer> fieldNameCounts = buildFieldNameCounts(hierarchy);
        Map<String, Map<String, String>> visibleFieldKeysByClassFqn = buildVisibleFieldKeysByClassFqn(hierarchy);

        hierarchy.forEach(layer -> declaredFields(layer.classNode()).forEach(field -> {
            Expression initialExpression = field.getInitialExpression();
            if (initialExpression == null) {
                return;
            }

            String fieldContextName = fieldContextName(layer.classFqn(), field.getName(),
                    fieldNameCounts.getOrDefault(field.getName(), 0) > 1);

            handleAssignment(field.getName(), initialExpression, traceSourceClassFqn,
                    layer.classNode(), layer.metadata(), state,
                    classFieldScopes, classFieldScopes,
                    classFieldExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName,
                    fieldContextName, null);
        }));

        List<MethodResolutionContext> methodsToTrace = new ArrayList<>();
        hierarchy.forEach(layer -> declaredMethods(layer.classNode()).stream()
                .filter(this::isFixtureMethod)
                .forEach(method -> methodsToTrace.add(new MethodResolutionContext(method, layer))));
        declaredMethods(targetLayer.classNode()).stream()
                .filter(this::isTargetMethod)
                .filter(method -> !isFixtureMethod(method))
                .forEach(method -> methodsToTrace.add(new MethodResolutionContext(method, targetLayer)));

        methodsToTrace.forEach(methodContext -> {
            Statement code = methodContext.methodNode().getCode();
            if (!(code instanceof BlockStatement blockStatement)) {
                return;
            }

            Map<String, TraceBuilder> methodScopes = buildMethodScopes(
                    methodContext.layer().classFqn(), visibleFieldKeysByClassFqn, classFieldScopes);
            Map<String, Expression> methodExpressionScopes = buildMethodExpressionScopes(
                    methodContext.layer().classFqn(), visibleFieldKeysByClassFqn, classFieldExpressionScopes);

            traceBlock(blockStatement, traceSourceClassFqn,
                    methodContext.layer().classNode(), methodContext.layer().metadata(), state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName,
                    methodContext.methodNode().getName());
        });

        tracedBuilders.forEach(builder -> state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                builder.sourceClassFqn,
                builder.sourceMethodName,
                builder.sagaClassFqn,
                builder.buildTraceText()
        )));
    }

    private void traceBlock(BlockStatement blockStatement,
                            String traceSourceClassFqn,
                            ClassNode classNode,
                            GroovySourceClassMetadata metadata,
                            ApplicationAnalysisState state,
                            Map<String, TraceBuilder> methodScopes,
                            Map<String, TraceBuilder> classFieldScopes,
                            Map<String, Expression> methodExpressionScopes,
                            Map<String, Expression> classFieldExpressionScopes,
                            Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                            List<TraceBuilder> tracedBuilders,
                            Map<String, List<MethodResolutionContext>> methodsByName,
                            String methodName) {
        for (Statement statement : blockStatement.getStatements()) {
            if (statement instanceof BlockStatement nestedBlock) {
                traceBlock(nestedBlock, traceSourceClassFqn, classNode, metadata, state,
                        methodScopes, classFieldScopes,
                        methodExpressionScopes, classFieldExpressionScopes,
                        visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
                continue;
            }

            if (statement instanceof ExpressionStatement expressionStatement) {
                Expression expression = expressionStatement.getExpression();
                traceExpression(expression, traceSourceClassFqn, classNode, metadata, state,
                        methodScopes, classFieldScopes,
                        methodExpressionScopes, classFieldExpressionScopes,
                        visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName,
                        statement.getStatementLabel());
            }
        }
    }

    private void traceExpression(Expression expression,
                                 String traceSourceClassFqn,
                                 ClassNode classNode,
                                 GroovySourceClassMetadata metadata,
                                 ApplicationAnalysisState state,
                                 Map<String, TraceBuilder> methodScopes,
                                 Map<String, TraceBuilder> classFieldScopes,
                                 Map<String, Expression> methodExpressionScopes,
                                 Map<String, Expression> classFieldExpressionScopes,
                                 Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                 List<TraceBuilder> tracedBuilders,
                                 Map<String, List<MethodResolutionContext>> methodsByName,
                                 String methodName,
                                 String label) {
        if (expression instanceof DeclarationExpression declarationExpression) {
            String variableName = resolveDeclaredVariableName(declarationExpression.getLeftExpression());
            if (variableName == null) {
                return;
            }

            handleAssignment(variableName, declarationExpression.getRightExpression(), traceSourceClassFqn,
                    classNode, metadata,
                    state, methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName,
                    label);
            return;
        }

        if (expression instanceof BinaryExpression binaryExpression && isAssignment(binaryExpression)) {
            String variableName = resolveAssignedVariableName(binaryExpression.getLeftExpression());
            if (variableName == null) {
                return;
            }

            handleAssignment(variableName, binaryExpression.getRightExpression(), traceSourceClassFqn,
                    classNode, metadata,
                    state, methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName,
                    label);
            return;
        }

        if (expression instanceof MethodCallExpression methodCallExpression) {
            traceWorkflowCall(methodCallExpression, methodScopes, label);
        }
    }

    private void handleAssignment(String variableName,
                                  Expression rightExpression,
                                  String traceSourceClassFqn,
                                  ClassNode classNode,
                                  GroovySourceClassMetadata metadata,
                                  ApplicationAnalysisState state,
                                  Map<String, TraceBuilder> methodScopes,
                                  Map<String, TraceBuilder> classFieldScopes,
                                  Map<String, Expression> methodExpressionScopes,
                                  Map<String, Expression> classFieldExpressionScopes,
                                  Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                  List<TraceBuilder> tracedBuilders,
                                  Map<String, List<MethodResolutionContext>> methodsByName,
                                  String methodName,
                                  String label) {
        String visibleClassFieldKey = resolveVisibleFieldKey(visibleFieldKeysByClassFqn,
                classNode.getName(), variableName);

        if (rightExpression instanceof VariableExpression variableExpression) {
            TraceBuilder traced = methodScopes.get(variableExpression.getName());
            if (traced != null) {
                methodScopes.put(variableName, traced);
                if (visibleClassFieldKey != null) {
                    classFieldScopes.put(visibleClassFieldKey, traced);
                }

                methodExpressionScopes.put(variableName, rightExpression);
                if (visibleClassFieldKey != null) {
                    classFieldExpressionScopes.put(visibleClassFieldKey, rightExpression);
                }
                return;
            }
        }

        ConstructorResolution constructorResolution = resolveSagaConstructor(rightExpression, classNode, metadata, state,
                methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                visibleFieldKeysByClassFqn, new ArrayDeque<>(), 0);

        if (constructorResolution != null) {
            String sagaClassFqn = constructorResolution.sagaClassFqn();

            TraceBuilder builder = new TraceBuilder(traceSourceClassFqn,
                    contextName(methodName, variableName), sagaClassFqn);
            builder.appendContextLabel(label);
            builder.appendConstructorLine(formatAssignmentText(variableName, sagaClassFqn, null));
            appendConstructorArgumentLines(builder, constructorResolution.constructorExpression(),
                    constructorResolution.resolutionClassNode(), constructorResolution.resolutionMetadata(), state,
                    constructorResolution.expressionScopes(), classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn);
            constructorResolution.resolutionNotes().forEach(builder::appendDetailLine);
            registerConstructorTrace(builder, state);
            tracedBuilders.add(builder);

            methodScopes.put(variableName, builder);
            if (visibleClassFieldKey != null) {
                classFieldScopes.put(visibleClassFieldKey, builder);
            }
        }

        methodExpressionScopes.put(variableName, rightExpression);
        if (visibleClassFieldKey != null) {
            classFieldExpressionScopes.put(visibleClassFieldKey, rightExpression);
        }
    }

    private void traceWorkflowCall(MethodCallExpression methodCallExpression,
                                   Map<String, TraceBuilder> methodScopes,
                                   String label) {
        String calledMethod = methodCallExpression.getMethodAsString();
        if (calledMethod == null || !TRACKED_WORKFLOW_METHODS.contains(calledMethod)) {
            return;
        }

        String scopeName = resolveScopeName(methodCallExpression.getObjectExpression()).orElse(null);
        if (scopeName == null) {
            return;
        }

        TraceBuilder builder = methodScopes.get(scopeName);
        if (builder == null) {
            return;
        }

        builder.appendContextLabel(label);
        builder.appendCallLine(scopeName + "." + calledMethod + "(...)");
    }

    private void registerConstructorTrace(TraceBuilder builder, ApplicationAnalysisState state) {
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                builder.sourceClassFqn,
                builder.sourceMethodName,
                builder.sagaClassFqn
        ));
    }

    private String fieldContextName(String ownerClassFqn, String fieldName, boolean disambiguate) {
        if (disambiguate) {
            return "field:" + simpleName(ownerClassFqn) + "#" + fieldName;
        }

        return "field:" + fieldName;
    }

    private String contextName(String methodName, String variableName) {
        return methodName;
    }

    private Map<String, Integer> buildFieldNameCounts(List<InheritanceLayer> hierarchy) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        hierarchy.forEach(layer -> declaredFields(layer.classNode())
                .forEach(field -> counts.merge(field.getName(), 1, Integer::sum)));
        return counts;
    }

    private Map<String, Map<String, String>> buildVisibleFieldKeysByClassFqn(List<InheritanceLayer> hierarchy) {
        Map<String, Map<String, String>> visibleByClass = new LinkedHashMap<>();

        for (int index = 0; index < hierarchy.size(); index++) {
            Map<String, String> visibleForClass = new LinkedHashMap<>();
            for (int layerIndex = 0; layerIndex <= index; layerIndex++) {
                InheritanceLayer layer = hierarchy.get(layerIndex);
                for (FieldNode field : declaredFields(layer.classNode())) {
                    visibleForClass.put(field.getName(), fieldKey(layer.classFqn(), field.getName()));
                }
            }

            visibleByClass.put(hierarchy.get(index).classFqn(), visibleForClass);
        }

        return visibleByClass;
    }

    private Map<String, TraceBuilder> buildMethodScopes(String classFqn,
                                                        Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                        Map<String, TraceBuilder> classFieldScopes) {
        Map<String, TraceBuilder> methodScopes = new LinkedHashMap<>();
        visibleFieldKeysByClassFqn.getOrDefault(classFqn, Map.of()).forEach((fieldName, fieldKey) -> {
            TraceBuilder builder = classFieldScopes.get(fieldKey);
            if (builder != null) {
                methodScopes.put(fieldName, builder);
            }
        });
        return methodScopes;
    }

    private Map<String, Expression> buildMethodExpressionScopes(String classFqn,
                                                                Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                                Map<String, Expression> classFieldExpressionScopes) {
        Map<String, Expression> methodExpressionScopes = new LinkedHashMap<>();
        mergeVisibleFieldExpressions(methodExpressionScopes, classFqn,
                visibleFieldKeysByClassFqn, classFieldExpressionScopes);
        return methodExpressionScopes;
    }

    private void mergeVisibleFieldExpressions(Map<String, Expression> targetScopes,
                                              String classFqn,
                                              Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                              Map<String, Expression> classFieldExpressionScopes) {
        visibleFieldKeysByClassFqn.getOrDefault(classFqn, Map.of()).forEach((fieldName, fieldKey) -> {
            Expression expression = classFieldExpressionScopes.get(fieldKey);
            if (expression != null) {
                targetScopes.put(fieldName, expression);
            }
        });
    }

    private String resolveVisibleFieldKey(Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                          String classFqn,
                                          String fieldName) {
        if (classFqn == null || fieldName == null) {
            return null;
        }

        return visibleFieldKeysByClassFqn
                .getOrDefault(classFqn, Map.of())
                .get(fieldName);
    }

    private String fieldKey(String ownerClassFqn, String fieldName) {
        return ownerClassFqn + "#" + fieldName;
    }

    private String formatAssignmentText(String variableName, String sagaClassFqn, String fallbackText) {
        return variableName + " = new " + simpleName(sagaClassFqn) + "(...)";
    }

    private void appendConstructorArgumentLines(TraceBuilder builder,
                                                ConstructorCallExpression constructorExpression,
                                                ClassNode classNode,
                                                GroovySourceClassMetadata metadata,
                                                ApplicationAnalysisState state,
                                                Map<String, Expression> methodExpressionScopes,
                                                Map<String, Expression> classFieldExpressionScopes,
                                                Map<String, List<MethodResolutionContext>> methodsByName,
                                                Map<String, Map<String, String>> visibleFieldKeysByClassFqn) {
        List<Expression> argumentExpressions = extractArguments(constructorExpression.getArguments());
        for (int index = 0; index < argumentExpressions.size(); index++) {
            Expression argumentExpression = argumentExpressions.get(index);
            String provenance = describeExpressionProvenance(argumentExpression, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, new ArrayDeque<>(), new HashSet<>(), 0);
            builder.appendInputLine("arg[" + index + "]: " + provenance);
        }
    }

    private ConstructorResolution resolveSagaConstructor(Expression expression,
                                                         ClassNode classNode,
                                                         GroovySourceClassMetadata metadata,
                                                         ApplicationAnalysisState state,
                                                         Map<String, Expression> methodExpressionScopes,
                                                         Map<String, Expression> classFieldExpressionScopes,
                                                         Map<String, List<MethodResolutionContext>> methodsByName,
                                                         Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                         Deque<String> helperCallStack,
                                                         int depth) {
        if (expression == null || depth > MAX_TRACE_DEPTH) {
            return null;
        }

        if (expression instanceof ConstructorCallExpression constructorCallExpression) {
            String sagaClassFqn = resolveSagaClassFqn(constructorCallExpression.getType().getName(), metadata, state)
                    .orElse(null);
            if (sagaClassFqn == null) {
                return null;
            }

            return new ConstructorResolution(sagaClassFqn, constructorCallExpression,
                    new LinkedHashMap<>(methodExpressionScopes), List.of(), classNode, metadata);
        }

        if (expression instanceof VariableExpression variableExpression) {
            Expression resolvedExpression = resolveExpressionFromScopes(variableExpression.getName(),
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression == null || isSelfReference(variableExpression, resolvedExpression)) {
                return null;
            }

            return resolveSagaConstructor(resolvedExpression, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, helperCallStack, depth + 1);
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            String scopeName = resolveScopeName(propertyExpression).orElse(null);
            if (scopeName == null) {
                return null;
            }

            Expression resolvedExpression = resolveExpressionFromScopes(scopeName,
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression == null) {
                return null;
            }

            return resolveSagaConstructor(resolvedExpression, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, helperCallStack, depth + 1);
        }

        if (expression instanceof MethodCallExpression methodCallExpression && isLocalHelperCall(methodCallExpression)) {
            MethodResolutionContext helperMethodContext = resolveLocalHelperMethod(methodCallExpression, methodsByName)
                    .orElse(null);
            if (helperMethodContext == null) {
                return null;
            }
            MethodNode helperMethod = helperMethodContext.methodNode();

            String helperKey = helperMethodContext.layer().classFqn() + "#" + helperMethod.getTypeDescriptor();
            if (helperCallStack.contains(helperKey)) {
                return null;
            }

            HelperReturnResolution helperReturn = resolveHelperReturn(methodCallExpression, helperMethodContext,
                    methodExpressionScopes, classFieldExpressionScopes, visibleFieldKeysByClassFqn);
            if (helperReturn == null) {
                return null;
            }

            helperCallStack.push(helperKey);
            ConstructorResolution helperResolution = resolveSagaConstructor(helperReturn.returnExpression(),
                    helperMethodContext.layer().classNode(), helperMethodContext.layer().metadata(), state,
                    helperReturn.helperExpressionScopes(), classFieldExpressionScopes,
                    methodsByName, visibleFieldKeysByClassFqn, helperCallStack, depth + 1);
            helperCallStack.pop();

            if (helperResolution == null) {
                return null;
            }

            List<String> notes = new ArrayList<>();
            notes.add("resolved via helper " + helperMethod.getName() + "(...)");
            notes.addAll(helperResolution.resolutionNotes());

            return new ConstructorResolution(helperResolution.sagaClassFqn(),
                    helperResolution.constructorExpression(),
                    helperResolution.expressionScopes(), notes,
                    helperResolution.resolutionClassNode(), helperResolution.resolutionMetadata());
        }

        return null;
    }

    private String describeExpressionProvenance(Expression expression,
                                                ClassNode classNode,
                                                GroovySourceClassMetadata metadata,
                                                ApplicationAnalysisState state,
                                                Map<String, Expression> methodExpressionScopes,
                                                Map<String, Expression> classFieldExpressionScopes,
                                                Map<String, List<MethodResolutionContext>> methodsByName,
                                                Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                Deque<String> helperCallStack,
                                                Set<String> visitedVariables,
                                                int depth) {
        if (expression == null) {
            return "(unknown)";
        }

        if (depth > MAX_TRACE_DEPTH) {
            return expression.getText() + " [unresolved depth-limit]";
        }

        if (expression instanceof ConstantExpression constantExpression) {
            return constantExpression.getText();
        }

        if (expression instanceof ConstructorCallExpression constructorCallExpression) {
            List<String> nestedArgumentProvenance = extractArguments(constructorCallExpression.getArguments()).stream()
                    .map(argument -> describeExpressionProvenance(argument, classNode, metadata, state,
                            methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                            visibleFieldKeysByClassFqn, helperCallStack, visitedVariables, depth + 1))
                    .toList();
            return "new " + constructorCallExpression.getType().getNameWithoutPackage() + "(" +
                    String.join(", ", nestedArgumentProvenance) + ")";
        }

        if (expression instanceof VariableExpression variableExpression) {
            String variableName = variableExpression.getName();
            if ("this".equals(variableName) || "super".equals(variableName)) {
                return variableName;
            }

            Expression resolvedExpression = resolveExpressionFromScopes(variableName,
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression == null) {
                return variableName + " [unresolved source-backed variable]";
            }
            if (isSelfReference(variableExpression, resolvedExpression)) {
                return variableName + " [unresolved self-reference]";
            }
            if (!visitedVariables.add(variableName)) {
                return variableName + " [unresolved cyclic reference]";
            }

            String resolvedProvenance = describeExpressionProvenance(resolvedExpression,
                    classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, helperCallStack, visitedVariables, depth + 1);
            visitedVariables.remove(variableName);

            return variableName + " <- " + resolvedProvenance;
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            String receiver = describeExpressionProvenance(propertyExpression.getObjectExpression(),
                    classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, helperCallStack, visitedVariables, depth + 1);
            String propertyName = propertyExpression.getPropertyAsString();
            if (propertyName == null || propertyName.isBlank()) {
                propertyName = propertyExpression.getProperty().getText();
            }
            return receiver + "." + propertyName;
        }

        if (expression instanceof MethodCallExpression methodCallExpression) {
            String calledMethod = methodCallExpression.getMethodAsString();
            if (calledMethod == null) {
                return methodCallExpression.getText() + " [unresolved dynamic-method-call]";
            }

            if (isAccessorMethod(methodCallExpression)) {
                String receiver = describeExpressionProvenance(methodCallExpression.getObjectExpression(),
                        classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn, helperCallStack, visitedVariables, depth + 1);
                String accessorName = accessorPropertyName(calledMethod).orElse(calledMethod + "()");
                return receiver + "." + accessorName;
            }

            if (isLocalHelperCall(methodCallExpression)) {
                MethodResolutionContext helperMethodContext = resolveLocalHelperMethod(methodCallExpression, methodsByName)
                        .orElse(null);
                if (helperMethodContext == null) {
                    return calledMethod + "(...) [unresolved local-helper-method]";
                }
                MethodNode helperMethod = helperMethodContext.methodNode();

                String helperKey = helperMethodContext.layer().classFqn() + "#" + helperMethod.getTypeDescriptor();
                if (helperCallStack.contains(helperKey)) {
                    return calledMethod + "(...) [unresolved helper-cycle]";
                }

                HelperReturnResolution helperReturn = resolveHelperReturn(methodCallExpression, helperMethodContext,
                        methodExpressionScopes, classFieldExpressionScopes, visibleFieldKeysByClassFqn);
                if (helperReturn == null) {
                    return calledMethod + "(...) [unresolved local-helper-return]";
                }

                helperCallStack.push(helperKey);
                String helperProvenance = describeExpressionProvenance(helperReturn.returnExpression(),
                        helperMethodContext.layer().classNode(), helperMethodContext.layer().metadata(), state,
                        helperReturn.helperExpressionScopes(), classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn, helperCallStack, visitedVariables, depth + 1);
                helperCallStack.pop();
                return calledMethod + "(...) <- " + helperProvenance;
            }

            return methodCallExpression.getText() + " [unresolved external/runtime edge]";
        }

        return expression.getText();
    }

    private HelperReturnResolution resolveHelperReturn(MethodCallExpression callExpression,
                                                       MethodResolutionContext helperMethodContext,
                                                       Map<String, Expression> callerExpressionScopes,
                                                       Map<String, Expression> classFieldExpressionScopes,
                                                       Map<String, Map<String, String>> visibleFieldKeysByClassFqn) {
        MethodNode helperMethod = helperMethodContext.methodNode();
        Statement code = helperMethod.getCode();
        if (!(code instanceof BlockStatement helperBlock)) {
            return null;
        }

        List<Expression> callArguments = extractArguments(callExpression.getArguments());
        Parameter[] parameters = helperMethod.getParameters();
        if (parameters == null || parameters.length != callArguments.size()) {
            return null;
        }

        Map<String, Expression> helperExpressionScopes = new LinkedHashMap<>(callerExpressionScopes);
        mergeVisibleFieldExpressions(helperExpressionScopes,
                helperMethodContext.layer().classFqn(),
                visibleFieldKeysByClassFqn,
                classFieldExpressionScopes);

        for (int i = 0; i < parameters.length; i++) {
            helperExpressionScopes.put(parameters[i].getName(), callArguments.get(i));
        }

        MethodBodyScanResult bodyScanResult = scanMethodBody(helperBlock, helperExpressionScopes);
        if (bodyScanResult.ambiguousControlFlow()) {
            return null;
        }

        List<Expression> returnExpressions = new ArrayList<>(bodyScanResult.returnExpressions());
        if (returnExpressions.isEmpty() && bodyScanResult.lastExpression() != null) {
            returnExpressions.add(bodyScanResult.lastExpression());
        }

        if (returnExpressions.size() != 1) {
            return null;
        }

        return new HelperReturnResolution(returnExpressions.get(0), helperExpressionScopes);
    }

    private MethodBodyScanResult scanMethodBody(BlockStatement blockStatement,
                                                Map<String, Expression> helperExpressionScopes) {
        List<Expression> returnExpressions = new ArrayList<>();
        Expression lastExpression = null;
        boolean ambiguousControlFlow = false;

        for (Statement statement : blockStatement.getStatements()) {
            if (statement instanceof BlockStatement nestedBlock) {
                MethodBodyScanResult nestedResult = scanMethodBody(nestedBlock, helperExpressionScopes);
                returnExpressions.addAll(nestedResult.returnExpressions());
                if (nestedResult.lastExpression() != null) {
                    lastExpression = nestedResult.lastExpression();
                }
                ambiguousControlFlow = ambiguousControlFlow || nestedResult.ambiguousControlFlow();
                continue;
            }

            if (statement instanceof ExpressionStatement expressionStatement) {
                Expression expression = expressionStatement.getExpression();
                captureScopedAssignment(expression, helperExpressionScopes);
                lastExpression = expression;
                continue;
            }

            if (statement instanceof ReturnStatement returnStatement) {
                if (returnStatement.getExpression() != null) {
                    returnExpressions.add(returnStatement.getExpression());
                    lastExpression = returnStatement.getExpression();
                }
                continue;
            }

            ambiguousControlFlow = true;
        }

        return new MethodBodyScanResult(returnExpressions, lastExpression, ambiguousControlFlow);
    }

    private void captureScopedAssignment(Expression expression,
                                         Map<String, Expression> helperExpressionScopes) {
        if (expression instanceof DeclarationExpression declarationExpression) {
            String variableName = resolveDeclaredVariableName(declarationExpression.getLeftExpression());
            if (variableName != null) {
                helperExpressionScopes.put(variableName, declarationExpression.getRightExpression());
            }
            return;
        }

        if (expression instanceof BinaryExpression binaryExpression && isAssignment(binaryExpression)) {
            String variableName = resolveAssignedVariableName(binaryExpression.getLeftExpression());
            if (variableName != null) {
                helperExpressionScopes.put(variableName, binaryExpression.getRightExpression());
            }
        }
    }

    private Expression resolveExpressionFromScopes(String variableName,
                                                   Map<String, Expression> methodExpressionScopes,
                                                   Map<String, Expression> classFieldExpressionScopes) {
        if (methodExpressionScopes.containsKey(variableName)) {
            return methodExpressionScopes.get(variableName);
        }

        if (classFieldExpressionScopes.containsKey(variableName)) {
            return classFieldExpressionScopes.get(variableName);
        }

        List<Expression> compositeMatches = classFieldExpressionScopes.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().endsWith("#" + variableName))
                .map(Map.Entry::getValue)
                .toList();
        if (compositeMatches.size() == 1) {
            return compositeMatches.get(0);
        }

        return null;
    }

    private boolean isSelfReference(VariableExpression variableExpression, Expression resolvedExpression) {
        return resolvedExpression instanceof VariableExpression resolvedVariable
                && Objects.equals(variableExpression.getName(), resolvedVariable.getName());
    }

    private boolean isLocalHelperCall(MethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return false;
        }

        if (methodCallExpression.isImplicitThis()) {
            return true;
        }

        Expression objectExpression = methodCallExpression.getObjectExpression();
        return objectExpression instanceof VariableExpression variableExpression
                && "this".equals(variableExpression.getName());
    }

    private Optional<MethodResolutionContext> resolveLocalHelperMethod(MethodCallExpression methodCallExpression,
                                                                       Map<String, List<MethodResolutionContext>> methodsByName) {
        String methodName = methodCallExpression.getMethodAsString();
        if (methodName == null) {
            return Optional.empty();
        }

        List<MethodResolutionContext> methods = methodsByName.getOrDefault(methodName, List.of()).stream()
                .filter(methodContext -> methodContext != null
                        && methodContext.methodNode() != null
                        && methodContext.methodNode().getCode() != null)
                .toList();
        if (methods.isEmpty()) {
            return Optional.empty();
        }

        int argumentCount = extractArguments(methodCallExpression.getArguments()).size();
        List<MethodResolutionContext> arityMatches = methods.stream()
                .filter(methodContext -> methodContext.methodNode().getParameters() != null
                        && methodContext.methodNode().getParameters().length == argumentCount)
                .toList();

        if (arityMatches.size() != 1) {
            return Optional.empty();
        }

        return Optional.of(arityMatches.get(0));
    }

    private List<Expression> extractArguments(Expression argumentsExpression) {
        if (argumentsExpression == null) {
            return List.of();
        }

        if (argumentsExpression instanceof TupleExpression tupleExpression) {
            return tupleExpression.getExpressions();
        }

        return List.of(argumentsExpression);
    }

    private boolean isAccessorMethod(MethodCallExpression methodCallExpression) {
        String methodName = methodCallExpression.getMethodAsString();
        if (methodName == null || !extractArguments(methodCallExpression.getArguments()).isEmpty()) {
            return false;
        }

        return accessorPropertyName(methodName).isPresent();
    }

    private Optional<String> accessorPropertyName(String methodName) {
        if (methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }

        if (methodName.startsWith("get") && methodName.length() > 3) {
            return Optional.of(decapitalize(methodName.substring(3)));
        }

        if (methodName.startsWith("is") && methodName.length() > 2) {
            return Optional.of(decapitalize(methodName.substring(2)));
        }

        return Optional.empty();
    }

    private String decapitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private boolean isSourceBackedSpecificationClass(String classFqn,
                                                     Map<String, GroovySourceClassMetadata> metadataByClassFqn,
                                                     GroovySourceIndex sourceIndex) {
        Set<String> visited = new LinkedHashSet<>();
        String currentClassFqn = classFqn;

        while (currentClassFqn != null && visited.add(currentClassFqn)) {
            GroovySourceClassMetadata metadata = metadataByClassFqn.get(currentClassFqn);
            if (metadata == null) {
                return false;
            }

            if (isDirectSpecification(metadata)) {
                return true;
            }

            currentClassFqn = sourceIndex.getSourceBackedSuperclassFqn(currentClassFqn);
        }

        return false;
    }

    private List<InheritanceLayer> resolveSourceBackedHierarchy(String classFqn,
                                                                Map<String, GroovySourceClassMetadata> metadataByClassFqn,
                                                                Map<String, ClassNode> classNodesByFqn,
                                                                GroovySourceIndex sourceIndex) {
        Deque<InheritanceLayer> hierarchy = new ArrayDeque<>();
        Set<String> visited = new LinkedHashSet<>();
        String currentClassFqn = classFqn;

        while (currentClassFqn != null && visited.add(currentClassFqn)) {
            GroovySourceClassMetadata metadata = metadataByClassFqn.get(currentClassFqn);
            ClassNode classNode = classNodesByFqn.get(currentClassFqn);
            if (metadata == null || classNode == null) {
                return List.of();
            }

            hierarchy.addFirst(new InheritanceLayer(currentClassFqn, classNode, metadata));
            currentClassFqn = sourceIndex.getSourceBackedSuperclassFqn(currentClassFqn);
        }

        return List.copyOf(hierarchy);
    }

    private Map<String, List<MethodResolutionContext>> buildHelperMethodsByName(List<InheritanceLayer> hierarchy) {
        Map<String, Map<String, MethodResolutionContext>> byNameAndSignature = new LinkedHashMap<>();

        for (int index = hierarchy.size() - 1; index >= 0; index--) {
            InheritanceLayer layer = hierarchy.get(index);
            for (MethodNode method : declaredMethods(layer.classNode())) {
                if (method.getCode() == null) {
                    continue;
                }

                String methodName = method.getName();
                if (methodName == null) {
                    continue;
                }

                byNameAndSignature
                        .computeIfAbsent(methodName, ignored -> new LinkedHashMap<>())
                        .putIfAbsent(helperSignatureKey(method), new MethodResolutionContext(method, layer));
            }
        }

        Map<String, List<MethodResolutionContext>> byName = new LinkedHashMap<>();
        byNameAndSignature.forEach((methodName, methodsBySignature) ->
                byName.put(methodName, List.copyOf(methodsBySignature.values())));
        return byName;
    }

    private List<FieldNode> declaredFields(ClassNode classNode) {
        return classNode.getFields().stream()
                .filter(field -> isDeclaredOn(classNode, field.getDeclaringClass()))
                .toList();
    }

    private List<MethodNode> declaredMethods(ClassNode classNode) {
        return classNode.getMethods().stream()
                .filter(method -> isDeclaredOn(classNode, method.getDeclaringClass()))
                .toList();
    }

    private boolean isDeclaredOn(ClassNode ownerClassNode, ClassNode declaringClassNode) {
        if (ownerClassNode == null || declaringClassNode == null) {
            return false;
        }

        return Objects.equals(ownerClassNode.getName(), declaringClassNode.getName());
    }

    private String helperSignatureKey(MethodNode method) {
        if (method == null) {
            return "(unknown)";
        }

        StringBuilder signature = new StringBuilder();
        signature.append(method.getName()).append("(");

        Parameter[] parameters = method.getParameters();
        if (parameters != null) {
            for (int i = 0; i < parameters.length; i++) {
                if (i > 0) {
                    signature.append(",");
                }

                if (parameters[i] == null || parameters[i].getType() == null) {
                    signature.append("?");
                } else {
                    signature.append(parameters[i].getType().getName());
                }
            }
        }

        signature.append(")");
        return signature.toString();
    }

    private boolean isFixtureMethod(MethodNode method) {
        if (method == null) {
            return false;
        }

        String methodName = method.getName();
        return "setup".equals(methodName) || "setupSpec".equals(methodName);
    }

    private boolean isDirectSpecification(GroovySourceClassMetadata metadata) {
        String declaredSuperclassName = metadata.declaredSuperclassName();
        return "Specification".equals(declaredSuperclassName)
                || "spock.lang.Specification".equals(declaredSuperclassName);
    }

    private boolean isTargetMethod(org.codehaus.groovy.ast.MethodNode method) {
        if (method == null || method.getCode() == null || method.getParameters().length != 0) {
            return false;
        }

        String methodName = method.getName();
        if ("setup".equals(methodName) || "setupSpec".equals(methodName)) {
            return true;
        }

        return !method.isSynthetic()
                && methodName != null
                && !JAVA_IDENTIFIER.matcher(methodName).matches();
    }

    private Optional<String> resolveSagaClassFqn(String constructorTypeName,
                                                 GroovySourceClassMetadata metadata,
                                                 ApplicationAnalysisState state) {
        if (constructorTypeName == null || constructorTypeName.isBlank()) {
            return Optional.empty();
        }

        if (state.hasSagaFqn(constructorTypeName)) {
            return Optional.of(constructorTypeName);
        }

        if (constructorTypeName.contains(".")) {
            return state.hasSagaFqn(constructorTypeName) ? Optional.of(constructorTypeName) : Optional.empty();
        }

        List<String> candidates = new ArrayList<>();

        if (metadata.packageName() != null && !metadata.packageName().isBlank()) {
            candidates.add(metadata.packageName() + "." + constructorTypeName);
        }

        for (GroovyImportMetadata importMetadata : metadata.imports()) {
            if (importMetadata.staticImport() || importMetadata.importedType() == null || importMetadata.importedType().isBlank()) {
                continue;
            }

            if (importMetadata.star()) {
                candidates.add(importMetadata.importedType() + "." + constructorTypeName);
                continue;
            }

            String importedSimpleName = simpleName(importMetadata.importedType());
            if (constructorTypeName.equals(importMetadata.alias())
                    || constructorTypeName.equals(importedSimpleName)
                    || constructorTypeName.equals(importMetadata.importedType())) {
                candidates.add(importMetadata.importedType());
            }
        }

        List<String> simpleNameMatches = state.sagas.stream()
                .map(saga -> saga.getFqn())
                .filter(fqn -> simpleName(fqn).equals(constructorTypeName))
                .collect(Collectors.toList());
        if (simpleNameMatches.size() == 1) {
            candidates.add(simpleNameMatches.get(0));
        }

        return candidates.stream()
                .filter(state::hasSagaFqn)
                .findFirst();
    }

    private String simpleName(String fqn) {
        if (fqn == null) {
            return null;
        }

        int lastDot = fqn.lastIndexOf('.');
        return lastDot < 0 ? fqn : fqn.substring(lastDot + 1);
    }

    private Optional<String> resolveScopeName(Expression expression) {
        if (expression instanceof VariableExpression variableExpression) {
            return Optional.of(variableExpression.getName());
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            Expression objectExpression = propertyExpression.getObjectExpression();
            if (objectExpression instanceof VariableExpression variableExpression
                    && "this".equals(variableExpression.getName())) {
                return Optional.ofNullable(propertyExpression.getPropertyAsString());
            }
        }

        return Optional.empty();
    }

    private String resolveDeclaredVariableName(Expression leftExpression) {
        if (leftExpression instanceof VariableExpression variableExpression) {
            return variableExpression.getName();
        }

        return resolveAssignedVariableName(leftExpression);
    }

    private String resolveAssignedVariableName(Expression leftExpression) {
        if (leftExpression instanceof VariableExpression variableExpression) {
            return variableExpression.getName();
        }

        if (leftExpression instanceof PropertyExpression propertyExpression) {
            Expression objectExpression = propertyExpression.getObjectExpression();
            if (objectExpression instanceof VariableExpression variableExpression
                    && "this".equals(variableExpression.getName())) {
                return propertyExpression.getPropertyAsString();
            }
        }

        return null;
    }

    private boolean isAssignment(BinaryExpression binaryExpression) {
        return binaryExpression.getOperation() != null
                && "=".equals(binaryExpression.getOperation().getText());
    }

    private ModuleNode parseSourceFile(Path sourceFile) {
        try {
            String source = java.nio.file.Files.readString(sourceFile);
            SourceUnit sourceUnit = SourceUnit.create(sourceFile.toString(), source);
            sourceUnit.parse();
            sourceUnit.completePhase();
            sourceUnit.convert();
            return sourceUnit.getAST();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read Groovy source file: " + sourceFile, e);
        } catch (CompilationFailedException e) {
            throw new IllegalStateException("Failed to parse Groovy source file: " + sourceFile, e);
        }
    }

    private record ConstructorResolution(String sagaClassFqn,
                                         ConstructorCallExpression constructorExpression,
                                         Map<String, Expression> expressionScopes,
                                         List<String> resolutionNotes,
                                         ClassNode resolutionClassNode,
                                         GroovySourceClassMetadata resolutionMetadata) {
    }

    private record InheritanceLayer(String classFqn,
                                    ClassNode classNode,
                                    GroovySourceClassMetadata metadata) {
    }

    private record MethodResolutionContext(MethodNode methodNode,
                                           InheritanceLayer layer) {
    }

    private record HelperReturnResolution(Expression returnExpression,
                                          Map<String, Expression> helperExpressionScopes) {
    }

    private record MethodBodyScanResult(List<Expression> returnExpressions,
                                        Expression lastExpression,
                                        boolean ambiguousControlFlow) {
    }

    private static final class TraceBuilder {
        private final String sourceClassFqn;
        private final String sourceMethodName;
        private final String sagaClassFqn;
        private final List<String> traceLines = new ArrayList<>();
        private String lastLabel;

        private TraceBuilder(String sourceClassFqn, String sourceMethodName, String sagaClassFqn) {
            this.sourceClassFqn = sourceClassFqn;
            this.sourceMethodName = sourceMethodName;
            this.sagaClassFqn = sagaClassFqn;
        }

        private void appendConstructorLine(String constructorLine) {
            traceLines.add(constructorLine);
        }

        private void appendCallLine(String callLine) {
            traceLines.add(callLine);
        }

        private void appendInputLine(String inputLine) {
            traceLines.add(inputLine);
        }

        private void appendDetailLine(String detailLine) {
            traceLines.add(detailLine);
        }

        private void appendContextLabel(String label) {
            if (label == null || label.isBlank() || label.equals(lastLabel)) {
                return;
            }

            traceLines.add(label + ":");
            lastLabel = label;
        }

        private String buildTraceText() {
            return String.join(System.lineSeparator(), traceLines);
        }
    }
}
