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
import org.codehaus.groovy.ast.expr.ListExpression;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.ast.expr.TupleExpression;
import org.codehaus.groovy.ast.expr.VariableExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.CaseStatement;
import org.codehaus.groovy.ast.stmt.CatchStatement;
import org.codehaus.groovy.ast.stmt.DoWhileStatement;
import org.codehaus.groovy.ast.stmt.EmptyStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.ast.stmt.IfStatement;
import org.codehaus.groovy.ast.stmt.ReturnStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.ast.stmt.SwitchStatement;
import org.codehaus.groovy.ast.stmt.TryCatchStatement;
import org.codehaus.groovy.ast.stmt.WhileStatement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.SourceUnit;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyConstructorInputTrace;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyImportMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceOriginKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceClassMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyWorkflowCall;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSourceKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
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
                builder.sourceBindingName,
                builder.originKind,
                builder.sourceExpressionText,
                builder.sagaClassFqn,
                List.copyOf(builder.constructorArguments),
                List.copyOf(builder.workflowCalls),
                List.copyOf(builder.resolutionNotes),
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
            traceStatement(statement, traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
        }
    }

    private void traceStatement(Statement statement,
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
        if (statement == null) {
            return;
        }

        if (statement instanceof BlockStatement nestedBlock) {
            traceBlock(nestedBlock, traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof TryCatchStatement tryCatchStatement) {
            traceStatement(tryCatchStatement.getTryStatement(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);

            for (CatchStatement catchStatement : tryCatchStatement.getCatchStatements()) {
                traceStatement(catchStatement.getCode(),
                        traceSourceClassFqn, classNode, metadata, state,
                        methodScopes, classFieldScopes,
                        methodExpressionScopes, classFieldExpressionScopes,
                        visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            }

            traceStatement(tryCatchStatement.getFinallyStatement(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof WhileStatement whileStatement) {
            traceStatement(whileStatement.getLoopBlock(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof DoWhileStatement doWhileStatement) {
            traceStatement(doWhileStatement.getLoopBlock(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof ForStatement forStatement) {
            traceStatement(forStatement.getLoopBlock(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof IfStatement ifStatement) {
            traceStatement(ifStatement.getIfBlock(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            traceStatement(ifStatement.getElseBlock(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
        }

        if (statement instanceof SwitchStatement switchStatement) {
            for (CaseStatement caseStatement : switchStatement.getCaseStatements()) {
                traceStatement(caseStatement.getCode(),
                        traceSourceClassFqn, classNode, metadata, state,
                        methodScopes, classFieldScopes,
                        methodExpressionScopes, classFieldExpressionScopes,
                        visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            }

            traceStatement(switchStatement.getDefaultStatement(),
                    traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName);
            return;
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
            if (traceFacadeCall(methodCallExpression, traceSourceClassFqn, classNode, metadata, state,
                    methodScopes, classFieldScopes,
                    methodExpressionScopes, classFieldExpressionScopes,
                    visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName, label)) {
                return;
            }

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

        if (rightExpression instanceof MethodCallExpression methodCallExpression
                && traceFacadeCall(methodCallExpression, traceSourceClassFqn, classNode, metadata, state,
                methodScopes, classFieldScopes,
                methodExpressionScopes, classFieldExpressionScopes,
                visibleFieldKeysByClassFqn, tracedBuilders, methodsByName, methodName, label)) {
            methodExpressionScopes.put(variableName, rightExpression);
            if (visibleClassFieldKey != null) {
                classFieldExpressionScopes.put(visibleClassFieldKey, rightExpression);
            }
            return;
        }

        ConstructorResolution constructorResolution = resolveSagaConstructor(rightExpression, classNode, metadata, state,
                methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                visibleFieldKeysByClassFqn, new ArrayDeque<>(), 0,
                GroovyTraceOriginKind.DIRECT_CONSTRUCTOR, rightExpression.getText());

        if (constructorResolution != null) {
            String sagaClassFqn = constructorResolution.sagaClassFqn();

            TraceBuilder builder = new TraceBuilder(traceSourceClassFqn,
                    contextName(methodName, variableName), variableName, sagaClassFqn);
            builder.originKind = constructorResolution.originKind();
            builder.sourceExpressionText = constructorResolution.sourceExpressionText();
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
        builder.appendCallLine(scopeName + "." + calledMethod + "(...)", label);
    }

    private boolean traceFacadeCall(MethodCallExpression methodCallExpression,
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
        FacadeResolution facadeResolution = resolveFacadeResolution(methodCallExpression, classNode, metadata, state,
                methodExpressionScopes, classFieldExpressionScopes, visibleFieldKeysByClassFqn, methodsByName,
                traceSourceClassFqn,
                methodName,
                traceScopeKey(classNode == null ? "(unknown)" : classNode.getName(), methodName),
                new ArrayDeque<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                Map.of(),
                0,
                false).orElse(null);
        if (facadeResolution == null) {
            return false;
        }

        TraceBuilder builder = new TraceBuilder(traceSourceClassFqn,
                contextName(methodName, null), null, facadeResolution.creationSite().sagaClassFqn());
        builder.originKind = GroovyTraceOriginKind.FACADE_CALL;
        builder.sourceExpressionText = methodCallExpression.getText();
        builder.appendContextLabel(label);
        builder.appendConstructorLine(methodCallExpression.getText());
        facadeResolution.constructorArguments().forEach(argument ->
                builder.appendInputLine(argument.index(), argument.provenance(), argument.recipe()));
        facadeResolution.resolutionNotes().forEach(builder::appendDetailLine);
        registerConstructorTrace(builder, state);
        tracedBuilders.add(builder);
        return true;
    }

    private Optional<FacadeResolution> resolveFacadeResolution(MethodCallExpression methodCallExpression,
                                                               ClassNode classNode,
                                                               GroovySourceClassMetadata metadata,
                                                               ApplicationAnalysisState state,
                                                               Map<String, Expression> methodExpressionScopes,
                                                               Map<String, Expression> classFieldExpressionScopes,
                                                               Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                               Map<String, List<MethodResolutionContext>> methodsByName,
                                                               String traceSourceClassFqn,
                                                               String traceMethodName,
                                                               String traceScopeKey,
                                                               Deque<String> helperCallStack,
                                                               Set<String> visitedVariables,
                                                               Set<String> emittedNestedFacadeTraceKeys,
                                                               Map<String, Expression> rebindingFallbackScopes,
                                                               int depth,
                                                               boolean helperScope) {
        String calledMethod = methodCallExpression.getMethodAsString();
        if (calledMethod == null || state.sagaCreationSites.isEmpty()) {
            return Optional.empty();
        }

        Optional<String> receiverTypeFqn = resolveFacadeReceiverType(methodCallExpression.getObjectExpression(),
                classNode, metadata, state, methodExpressionScopes, classFieldExpressionScopes, traceScopeKey);
        if (receiverTypeFqn.isEmpty()) {
            return Optional.empty();
        }

        List<Expression> callArguments = extractArguments(methodCallExpression.getArguments());

        List<WorkflowFunctionalityCreationSite> matchingSites = state.sagaCreationSites.stream()
                .filter(site -> Objects.equals(site.classFqn(), receiverTypeFqn.get())
                        && Objects.equals(site.methodName(), calledMethod))
                .toList();
        SelectedCreationSite selectedCreationSite = selectFacadeCreationSite(matchingSites, callArguments).orElse(null);
        if (selectedCreationSite == null) {
            return Optional.empty();
        }

        WorkflowFunctionalityCreationSite creationSite = selectedCreationSite.creationSite();
        List<GroovyTraceArgument> constructorArguments = new ArrayList<>();
        for (WorkflowCreationArgumentSource argumentSource : creationSite.argumentSources()) {
            ValueTrace argumentTrace = describeFacadeArgument(argumentSource, callArguments, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, visibleFieldKeysByClassFqn, methodsByName,
                    traceSourceClassFqn, traceMethodName, traceScopeKey,
                    helperCallStack, visitedVariables, emittedNestedFacadeTraceKeys,
                    rebindingFallbackScopes,
                    depth + 1, helperScope);
            if (argumentTrace == null) {
                return Optional.empty();
            }

            constructorArguments.add(new GroovyTraceArgument(argumentSource.argumentIndex(),
                    argumentTrace.provenance(), argumentTrace.recipe()));
        }

        List<String> resolutionNotes = new ArrayList<>();
        resolutionNotes.add("resolved via facade " + simpleName(receiverTypeFqn.get()) + "." + calledMethod + "(...)");
        if (selectedCreationSite.note() != null && !selectedCreationSite.note().isBlank()) {
            resolutionNotes.add(selectedCreationSite.note());
        }

        return Optional.of(new FacadeResolution(creationSite, receiverTypeFqn.get(), constructorArguments, resolutionNotes));
    }

    private Optional<FacadeResolution> resolveFacadeResolution(MethodCallExpression methodCallExpression,
                                                               ClassNode classNode,
                                                               GroovySourceClassMetadata metadata,
                                                               ApplicationAnalysisState state,
                                                               Map<String, Expression> methodExpressionScopes,
                                                               Map<String, Expression> classFieldExpressionScopes,
                                                               Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                                               Map<String, List<MethodResolutionContext>> methodsByName) {
        return resolveFacadeResolution(methodCallExpression,
                classNode,
                metadata,
                state,
                methodExpressionScopes,
                classFieldExpressionScopes,
                visibleFieldKeysByClassFqn,
                methodsByName,
                classNode == null ? "(unknown)" : classNode.getName(),
                "(unknown)",
                traceScopeKey(classNode == null ? "(unknown)" : classNode.getName(), "(unknown)"),
                new ArrayDeque<>(),
                new LinkedHashSet<>(),
                new LinkedHashSet<>(),
                Map.of(),
                0,
                false);
    }

    private Optional<SelectedCreationSite> selectFacadeCreationSite(List<WorkflowFunctionalityCreationSite> matchingSites,
                                                                    List<Expression> callArguments) {
        if (matchingSites == null || matchingSites.isEmpty()) {
            return Optional.empty();
        }

        if (matchingSites.size() == 1) {
            return Optional.of(new SelectedCreationSite(matchingSites.get(0), null));
        }

        List<WorkflowFunctionalityCreationSite> parameterCompatibleSites = matchingSites.stream()
                .filter(site -> site.argumentSources().stream()
                        .filter(source -> source.kind() == WorkflowCreationArgumentSourceKind.METHOD_PARAMETER)
                        .allMatch(source -> source.parameterIndex() != null
                                && source.parameterIndex() >= 0
                                && source.parameterIndex() < callArguments.size()))
                .toList();
        if (parameterCompatibleSites.isEmpty()) {
            return Optional.empty();
        }

        if (parameterCompatibleSites.size() == 1) {
            return Optional.of(new SelectedCreationSite(parameterCompatibleSites.get(0), null));
        }

        Set<String> sagaClasses = parameterCompatibleSites.stream()
                .map(WorkflowFunctionalityCreationSite::sagaClassFqn)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (sagaClasses.size() != 1) {
            return Optional.empty();
        }

        WorkflowFunctionalityCreationSite selectedSite = parameterCompatibleSites.stream()
                .sorted(Comparator
                        .comparingLong((WorkflowFunctionalityCreationSite site) -> site.argumentSources().stream()
                                .filter(source -> source.kind() == WorkflowCreationArgumentSourceKind.METHOD_PARAMETER)
                                .count())
                        .reversed()
                        .thenComparingInt(site -> site.argumentSources().size()))
                .findFirst()
                .orElse(null);
        if (selectedSite == null) {
            return Optional.empty();
        }

        return Optional.of(new SelectedCreationSite(selectedSite,
                "multiple creation sites matched; selected conservative representative"));
    }

    private Optional<String> resolveFacadeReceiverType(Expression receiverExpression,
                                                       ClassNode classNode,
                                                       GroovySourceClassMetadata metadata,
                                                       ApplicationAnalysisState state,
                                                       Map<String, Expression> methodExpressionScopes,
                                                       Map<String, Expression> classFieldExpressionScopes) {
        return resolveFacadeReceiverType(receiverExpression, classNode, metadata, state,
                methodExpressionScopes, classFieldExpressionScopes,
                traceScopeKey(classNode == null ? "(unknown)" : classNode.getName(), "(unknown)"));
    }

    private Optional<String> resolveFacadeReceiverType(Expression receiverExpression,
                                                       ClassNode classNode,
                                                       GroovySourceClassMetadata metadata,
                                                       ApplicationAnalysisState state,
                                                       Map<String, Expression> methodExpressionScopes,
                                                       Map<String, Expression> classFieldExpressionScopes,
                                                       String traceScopeKey) {
        return resolveFacadeReceiverType(receiverExpression, classNode, metadata, state,
                methodExpressionScopes, classFieldExpressionScopes, 0, new LinkedHashSet<>(),
                traceScopeKey);
    }

    private Optional<String> resolveFacadeReceiverType(Expression receiverExpression,
                                                       ClassNode classNode,
                                                       GroovySourceClassMetadata metadata,
                                                       ApplicationAnalysisState state,
                                                       Map<String, Expression> methodExpressionScopes,
                                                       Map<String, Expression> classFieldExpressionScopes,
                                                       int depth,
                                                       Set<String> visitedVariableNames,
                                                       String traceScopeKey) {
        if (receiverExpression == null || depth > MAX_TRACE_DEPTH) {
            return Optional.empty();
        }

        Set<String> knownFacadeTypes = state.sagaCreationSites.stream()
                .map(WorkflowFunctionalityCreationSite::classFqn)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (receiverExpression instanceof VariableExpression variableExpression) {
            String variableName = variableExpression.getName();
            String scopedVariableKey = scopedVariableKey(traceScopeKey, variableName);
            if (scopedVariableKey != null && !visitedVariableNames.add(scopedVariableKey)) {
                return Optional.empty();
            }

            Expression resolvedExpression = resolveExpressionFromScopes(variableName,
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression != null && !isSelfReference(variableExpression, resolvedExpression)) {
                Optional<String> resolvedType = resolveFacadeReceiverType(resolvedExpression, classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, depth + 1, visitedVariableNames,
                        traceScopeKey);
                if (scopedVariableKey != null) {
                    visitedVariableNames.remove(scopedVariableKey);
                }
                if (resolvedType.isPresent()) {
                    return resolvedType;
                }
            }

            if (variableName != null && methodExpressionScopes.containsKey(variableName)) {
                if (scopedVariableKey != null) {
                    visitedVariableNames.remove(scopedVariableKey);
                }
                return Optional.empty();
            }

            Optional<String> explicitFieldType = resolveFieldTypeFqn(classNode, variableName, metadata, knownFacadeTypes);
            if (scopedVariableKey != null) {
                visitedVariableNames.remove(scopedVariableKey);
            }
            return explicitFieldType;
        }

        if (receiverExpression instanceof PropertyExpression propertyExpression) {
            Optional<String> scopeName = resolveScopeName(propertyExpression);
            if (scopeName.isPresent()) {
                Expression resolvedExpression = resolveExpressionFromScopes(scopeName.get(),
                        methodExpressionScopes, classFieldExpressionScopes);
                if (resolvedExpression != null) {
                    Optional<String> resolvedType = resolveFacadeReceiverType(resolvedExpression, classNode, metadata, state,
                            methodExpressionScopes, classFieldExpressionScopes, depth + 1, visitedVariableNames,
                            traceScopeKey);
                    if (resolvedType.isPresent()) {
                        return resolvedType;
                    }
                }

                if (methodExpressionScopes.containsKey(scopeName.get())) {
                    return Optional.empty();
                }

                return resolveFieldTypeFqn(classNode, scopeName.get(), metadata, knownFacadeTypes);
            }

            return Optional.empty();
        }

        if (receiverExpression instanceof ConstructorCallExpression constructorCallExpression) {
            return resolveGroovyTypeFqn(constructorCallExpression.getType().getName(), metadata, knownFacadeTypes);
        }

        if (receiverExpression instanceof MethodCallExpression methodCallExpression && isLocalHelperCall(methodCallExpression)) {
            // Conservative: do not infer receiver types through helper call chains here.
            return Optional.empty();
        }

        return Optional.empty();
    }

    private Optional<String> resolveFieldTypeFqn(ClassNode classNode,
                                                  String fieldName,
                                                  GroovySourceClassMetadata metadata,
                                                  Set<String> knownTypes) {
        if (classNode == null || fieldName == null || fieldName.isBlank()) {
            return Optional.empty();
        }

        FieldNode fieldNode = classNode.getField(fieldName);
        if (fieldNode == null || fieldNode.getType() == null) {
            return Optional.empty();
        }

        return resolveGroovyTypeFqn(fieldNode.getType().getName(), metadata, knownTypes);
    }

    private Optional<String> resolveGroovyTypeFqn(String typeName,
                                                  GroovySourceClassMetadata metadata,
                                                  Set<String> knownTypes) {
        if (typeName == null || typeName.isBlank()) {
            return Optional.empty();
        }

        if (typeName.contains(".")) {
            return knownTypes.contains(typeName) ? Optional.of(typeName) : Optional.empty();
        }

        List<String> candidates = new ArrayList<>();
        if (metadata.packageName() != null && !metadata.packageName().isBlank()) {
            candidates.add(metadata.packageName() + "." + typeName);
        }

        for (GroovyImportMetadata importMetadata : metadata.imports()) {
            if (importMetadata.staticImport() || importMetadata.importedType() == null || importMetadata.importedType().isBlank()) {
                continue;
            }

            if (importMetadata.star()) {
                candidates.add(importMetadata.importedType() + "." + typeName);
                continue;
            }

            String importedSimpleName = simpleName(importMetadata.importedType());
            if (typeName.equals(importMetadata.alias())
                    || typeName.equals(importedSimpleName)
                    || typeName.equals(importMetadata.importedType())) {
                candidates.add(importMetadata.importedType());
            }
        }

        List<String> simpleNameMatches = knownTypes.stream()
                .filter(fqn -> Objects.equals(simpleName(fqn), typeName))
                .toList();
        if (simpleNameMatches.size() == 1) {
            candidates.add(simpleNameMatches.get(0));
        }

        return candidates.stream()
                .filter(knownTypes::contains)
                .findFirst();
    }

    private ValueTrace describeFacadeArgument(WorkflowCreationArgumentSource source,
                                              List<Expression> callArguments,
                                              ClassNode classNode,
                                              GroovySourceClassMetadata metadata,
                                              ApplicationAnalysisState state,
                                              Map<String, Expression> methodExpressionScopes,
                                              Map<String, Expression> classFieldExpressionScopes,
                                              Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                              Map<String, List<MethodResolutionContext>> methodsByName,
                                              String traceSourceClassFqn,
                                              String traceMethodName,
                                               String traceScopeKey,
                                               Deque<String> helperCallStack,
                                               Set<String> visitedVariables,
                                               Set<String> emittedNestedFacadeTraceKeys,
                                               Map<String, Expression> rebindingFallbackScopes,
                                               int depth,
                                               boolean helperScope) {
        if (source == null || source.kind() == null) {
            return null;
        }

        return switch (source.kind()) {
            case METHOD_PARAMETER -> {
                Integer parameterIndex = source.parameterIndex();
                if (parameterIndex == null || parameterIndex < 0 || parameterIndex >= callArguments.size()) {
                    yield null;
                }

                yield describeExpressionTrace(callArguments.get(parameterIndex), classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn,
                        helperCallStack,
                        visitedVariables,
                        emittedNestedFacadeTraceKeys,
                        depth,
                        traceSourceClassFqn,
                        traceMethodName,
                        traceScopeKey,
                        helperScope,
                        rebindingFallbackScopes);
            }
            case LOCAL_VARIABLE -> describeConservativeSourceBackedValue(source,
                    "local " + defaultText(source.name()),
                    "source-backed local variable");
            case FIELD_REFERENCE -> describeConservativeSourceBackedValue(source,
                    "field " + defaultText(source.name()),
                    "source-backed field reference");
            case INLINE_EXPRESSION -> {
                String text = defaultText(source.text());
                yield new ValueTrace(text + " [unresolved external/runtime edge]",
                        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, text, List.of()));
            }
        };
    }

    private ValueTrace describeConservativeSourceBackedValue(WorkflowCreationArgumentSource source,
                                                             String fallbackProvenance,
                                                             String unresolvedLabel) {
        String provenanceText = source.text() == null || source.text().isBlank()
                ? fallbackProvenance
                : defaultText(source.name()) + " <- " + source.text();

        if (source.text() != null && !source.text().isBlank() && !JAVA_IDENTIFIER.matcher(source.text()).matches()) {
            return new ValueTrace(provenanceText + " [unresolved external/runtime edge]",
                    new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, source.text(), List.of()));
        }

        return new ValueTrace(provenanceText + " [unresolved source-backed variable]",
                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE,
                        defaultText(source.name()), List.of()));
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value;
    }

    private void registerConstructorTrace(TraceBuilder builder, ApplicationAnalysisState state) {
        state.groovyConstructorInputTraces.add(new GroovyConstructorInputTrace(
                builder.sourceClassFqn,
                builder.sourceMethodName,
                builder.sourceBindingName,
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

    private static String traceScopeKey(String owner, String methodName) {
        return (owner == null || owner.isBlank() ? "(unknown-owner)" : owner)
                + "::"
                + (methodName == null || methodName.isBlank() ? "(unknown-method)" : methodName);
    }

    private static String scopedVariableKey(String traceScopeKey, String variableName) {
        if (variableName == null || variableName.isBlank()) {
            return null;
        }

        return (traceScopeKey == null || traceScopeKey.isBlank() ? "(unknown-scope)" : traceScopeKey)
                + "::"
                + variableName;
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
        String builderScopeKey = traceScopeKey(classNode == null ? "(unknown)" : classNode.getName(),
                builder.sourceMethodName);
        Set<String> emittedNestedFacadeTraceKeys = new LinkedHashSet<>();
        for (int index = 0; index < argumentExpressions.size(); index++) {
            Expression argumentExpression = argumentExpressions.get(index);
            ValueTrace trace = describeExpressionTrace(argumentExpression, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn,
                    new ArrayDeque<>(),
                    new LinkedHashSet<>(),
                    emittedNestedFacadeTraceKeys,
                    0,
                    builder.sourceClassFqn,
                    builder.sourceMethodName,
                    builderScopeKey,
                    false);
            builder.appendInputLine(index, trace.provenance(), trace.recipe());
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
                                                         int depth,
                                                         GroovyTraceOriginKind originKind,
                                                         String sourceExpressionText) {
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
                    new LinkedHashMap<>(methodExpressionScopes), List.of(), classNode, metadata,
                    originKind, sourceExpressionText);
        }

        if (expression instanceof VariableExpression variableExpression) {
            Expression resolvedExpression = resolveExpressionFromScopes(variableExpression.getName(),
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression == null || isSelfReference(variableExpression, resolvedExpression)) {
                return null;
            }

            return resolveSagaConstructor(resolvedExpression, classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn, helperCallStack, depth + 1,
                    originKind, sourceExpressionText);
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
                    visibleFieldKeysByClassFqn, helperCallStack, depth + 1,
                    originKind, sourceExpressionText);
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
                    methodsByName, visibleFieldKeysByClassFqn, helperCallStack, depth + 1,
                    originKind, sourceExpressionText);
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
                    helperResolution.resolutionClassNode(), helperResolution.resolutionMetadata(),
                    originKind, sourceExpressionText);
        }

        return null;
    }

    private ValueTrace describeExpressionTrace(Expression expression,
                                               ClassNode classNode,
                                               GroovySourceClassMetadata metadata,
                                               ApplicationAnalysisState state,
                                               Map<String, Expression> methodExpressionScopes,
                                               Map<String, Expression> classFieldExpressionScopes,
                                               Map<String, List<MethodResolutionContext>> methodsByName,
                                               Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                               Deque<String> helperCallStack,
                                               Set<String> visitedVariables,
                                               Set<String> emittedNestedFacadeTraceKeys,
                                               int depth,
                                               String traceSourceClassFqn,
                                               String traceMethodName,
                                               String traceScopeKey,
                                               boolean helperScope) {
        return describeExpressionTrace(expression,
                classNode,
                metadata,
                state,
                methodExpressionScopes,
                classFieldExpressionScopes,
                methodsByName,
                visibleFieldKeysByClassFqn,
                helperCallStack,
                visitedVariables,
                emittedNestedFacadeTraceKeys,
                depth,
                traceSourceClassFqn,
                traceMethodName,
                traceScopeKey,
                helperScope,
                Map.of());
    }

    private ValueTrace describeExpressionTrace(Expression expression,
                                               ClassNode classNode,
                                               GroovySourceClassMetadata metadata,
                                               ApplicationAnalysisState state,
                                               Map<String, Expression> methodExpressionScopes,
                                               Map<String, Expression> classFieldExpressionScopes,
                                               Map<String, List<MethodResolutionContext>> methodsByName,
                                               Map<String, Map<String, String>> visibleFieldKeysByClassFqn,
                                               Deque<String> helperCallStack,
                                               Set<String> visitedVariables,
                                               Set<String> emittedNestedFacadeTraceKeys,
                                               int depth,
                                               String traceSourceClassFqn,
                                               String traceMethodName,
                                               String traceScopeKey,
                                               boolean helperScope,
                                               Map<String, Expression> rebindingFallbackScopes) {
        if (expression == null) {
            return new ValueTrace("(unknown)", new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, "(unknown)", List.of()));
        }

        if (depth > MAX_TRACE_DEPTH) {
            return new ValueTrace(expression.getText() + " [unresolved depth-limit]",
                    new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, expression.getText(), List.of()));
        }

        if (expression instanceof ConstantExpression constantExpression) {
            return new ValueTrace(constantExpression.getText(),
                    new GroovyValueRecipe(GroovyValueKind.LITERAL, constantExpression.getText(), List.of()));
        }

        if (expression instanceof ConstructorCallExpression constructorCallExpression) {
            List<ValueTrace> nestedArgumentTraces = extractArguments(constructorCallExpression.getArguments()).stream()
                    .map(argument -> describeExpressionTrace(argument, classNode, metadata, state,
                            methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                            visibleFieldKeysByClassFqn,
                            helperCallStack,
                            visitedVariables,
                            emittedNestedFacadeTraceKeys,
                            depth + 1,
                            traceSourceClassFqn,
                            traceMethodName,
                            traceScopeKey,
                            helperScope,
                            rebindingFallbackScopes))
                    .toList();
            return new ValueTrace("new " + constructorCallExpression.getType().getNameWithoutPackage() + "(" +
                    nestedArgumentTraces.stream().map(ValueTrace::provenance).collect(Collectors.joining(", ")) + ")",
                    new GroovyValueRecipe(GroovyValueKind.CONSTRUCTOR,
                            constructorCallExpression.getType().getNameWithoutPackage(),
                            nestedArgumentTraces.stream().map(ValueTrace::recipe).toList()));
        }

        if (expression instanceof ListExpression listExpression) {
            List<ValueTrace> nestedTraces = listExpression.getExpressions().stream()
                    .map(item -> describeExpressionTrace(item, classNode, metadata, state,
                            methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                            visibleFieldKeysByClassFqn,
                            helperCallStack,
                            visitedVariables,
                            emittedNestedFacadeTraceKeys,
                            depth + 1,
                            traceSourceClassFqn,
                            traceMethodName,
                            traceScopeKey,
                            helperScope,
                            rebindingFallbackScopes))
                    .toList();
            return new ValueTrace("[" + nestedTraces.stream().map(ValueTrace::provenance).collect(Collectors.joining(", ")) + "]",
                    new GroovyValueRecipe(GroovyValueKind.COLLECTION_LITERAL, "list",
                            nestedTraces.stream().map(ValueTrace::recipe).toList()));
        }

        if (expression instanceof VariableExpression variableExpression) {
            String variableName = variableExpression.getName();
            if ("this".equals(variableName) || "super".equals(variableName)) {
                return new ValueTrace(variableName,
                        new GroovyValueRecipe(GroovyValueKind.LITERAL, variableName, List.of()));
            }

            Expression resolvedExpression = resolveExpressionFromScopes(variableName,
                    methodExpressionScopes, classFieldExpressionScopes);
            if (resolvedExpression == null) {
                return new ValueTrace(variableName + " [unresolved source-backed variable]",
                        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, variableName, List.of()));
            }
            if (isSelfReference(variableExpression, resolvedExpression)) {
                return new ValueTrace(variableName + " [unresolved self-reference]",
                        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, variableName, List.of()));
            }
            String scopedVariableKey = scopedVariableKey(traceScopeKey, variableName);
            if (scopedVariableKey != null && !visitedVariables.add(scopedVariableKey)) {
                if (helperScope
                        && isSelfRebindingExpression(resolvedExpression, variableName)
                        && rebindingFallbackScopes != null) {
                    Expression fallbackExpression = rebindingFallbackScopes.get(variableName);
                    if (fallbackExpression != null
                            && !isSelfReference(variableExpression, fallbackExpression)
                            && !Objects.equals(textOf(fallbackExpression), textOf(resolvedExpression))) {
                        ValueTrace fallbackTrace = describeExpressionTrace(fallbackExpression,
                                classNode, metadata, state,
                                methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                                visibleFieldKeysByClassFqn,
                                helperCallStack,
                                visitedVariables,
                                emittedNestedFacadeTraceKeys,
                                depth + 1,
                                traceSourceClassFqn,
                                traceMethodName,
                                traceScopeKey,
                                helperScope,
                                rebindingFallbackScopes);
                        return new ValueTrace(variableName + " <- " + fallbackTrace.provenance(), fallbackTrace.recipe());
                    }
                }
                return new ValueTrace(variableName + " [unresolved cyclic reference]",
                        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_VARIABLE, variableName, List.of()));
            }

            ValueTrace resolvedTrace = describeExpressionTrace(resolvedExpression,
                    classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn,
                    helperCallStack,
                    visitedVariables,
                    emittedNestedFacadeTraceKeys,
                    depth + 1,
                    traceSourceClassFqn,
                    traceMethodName,
                    traceScopeKey,
                    helperScope,
                    rebindingFallbackScopes);
            if (scopedVariableKey != null) {
                visitedVariables.remove(scopedVariableKey);
            }

            return new ValueTrace(variableName + " <- " + resolvedTrace.provenance(), resolvedTrace.recipe());
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            ValueTrace receiverTrace = describeExpressionTrace(propertyExpression.getObjectExpression(),
                    classNode, metadata, state,
                    methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                    visibleFieldKeysByClassFqn,
                    helperCallStack,
                    visitedVariables,
                    emittedNestedFacadeTraceKeys,
                    depth + 1,
                    traceSourceClassFqn,
                    traceMethodName,
                    traceScopeKey,
                    helperScope,
                    rebindingFallbackScopes);
            String propertyName = propertyExpression.getPropertyAsString();
            if (propertyName == null || propertyName.isBlank()) {
                propertyName = propertyExpression.getProperty().getText();
            }
            return new ValueTrace(receiverTrace.provenance() + "." + propertyName,
                    new GroovyValueRecipe(GroovyValueKind.PROPERTY_ACCESS, propertyName,
                            List.of(receiverTrace.recipe())));
        }

        if (expression instanceof MethodCallExpression methodCallExpression) {
            String calledMethod = methodCallExpression.getMethodAsString();
            if (calledMethod == null) {
                return new ValueTrace(methodCallExpression.getText() + " [unresolved dynamic-method-call]",
                        new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, methodCallExpression.getText(), List.of()));
            }

            if (isAccessorMethod(methodCallExpression)) {
                ValueTrace receiverTrace = describeExpressionTrace(methodCallExpression.getObjectExpression(),
                        classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn,
                        helperCallStack,
                        visitedVariables,
                    emittedNestedFacadeTraceKeys,
                    depth + 1,
                    traceSourceClassFqn,
                    traceMethodName,
                    traceScopeKey,
                    helperScope,
                    rebindingFallbackScopes);
                String accessorName = accessorPropertyName(calledMethod).orElse(calledMethod + "()");
                return new ValueTrace(receiverTrace.provenance() + "." + accessorName,
                        new GroovyValueRecipe(GroovyValueKind.PROPERTY_ACCESS, accessorName,
                                List.of(receiverTrace.recipe())));
            }

            if (isLocalToSetTransform(methodCallExpression)) {
                ValueTrace receiverTrace = describeExpressionTrace(methodCallExpression.getObjectExpression(),
                        classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn,
                        helperCallStack,
                        visitedVariables,
                    emittedNestedFacadeTraceKeys,
                    depth + 1,
                    traceSourceClassFqn,
                    traceMethodName,
                    traceScopeKey,
                    helperScope,
                    rebindingFallbackScopes);
                if (hasUnresolvedRecipe(receiverTrace.recipe()) || !isCollectionLikeRecipe(receiverTrace.recipe())) {
                    return new ValueTrace(methodCallExpression.getText() + " [unresolved external/runtime edge]",
                            new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, methodCallExpression.getText(),
                                    List.of(receiverTrace.recipe())));
                }
                return new ValueTrace(receiverTrace.provenance() + ".toSet()",
                        new GroovyValueRecipe(GroovyValueKind.LOCAL_TRANSFORM, "toSet",
                                List.of(receiverTrace.recipe())));
            }

            if (helperScope) {
                Optional<FacadeResolution> nestedFacadeResolution = resolveFacadeResolution(
                        methodCallExpression,
                        classNode,
                        metadata,
                        state,
                        methodExpressionScopes,
                        classFieldExpressionScopes,
                        visibleFieldKeysByClassFqn,
                        methodsByName,
                        traceSourceClassFqn,
                        traceMethodName,
                        traceScopeKey,
                        helperCallStack,
                        new LinkedHashSet<>(visitedVariables),
                        emittedNestedFacadeTraceKeys,
                        rebindingFallbackScopes,
                        0,
                        true);
                if (nestedFacadeResolution.isPresent()) {
                    registerNestedHelperFacadeTrace(
                            state,
                            nestedFacadeResolution.get(),
                            traceSourceClassFqn,
                            traceMethodName,
                            methodCallExpression,
                            emittedNestedFacadeTraceKeys,
                            traceScopeKey);
                    return new ValueTrace(methodCallExpression.getText(),
                            new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE,
                                    methodCallExpression.getText(),
                                    List.of()));
                }
            }

            if (isLocalHelperCall(methodCallExpression)) {
                MethodResolutionContext helperMethodContext = resolveLocalHelperMethod(methodCallExpression, methodsByName)
                        .orElse(null);
                if (helperMethodContext == null) {
                    return new ValueTrace(calledMethod + "(...) [unresolved local-helper-method]",
                            new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, calledMethod + "(...)", List.of()));
                }
                MethodNode helperMethod = helperMethodContext.methodNode();

                String helperKey = helperMethodContext.layer().classFqn() + "#" + helperMethod.getTypeDescriptor();
                if (helperCallStack.contains(helperKey)) {
                    return new ValueTrace(calledMethod + "(...) [unresolved helper-cycle]",
                            new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, calledMethod + "(...)", List.of()));
                }

                HelperReturnResolution helperReturn = resolveHelperReturn(methodCallExpression, helperMethodContext,
                        methodExpressionScopes, classFieldExpressionScopes, visibleFieldKeysByClassFqn);
                if (helperReturn == null) {
                    return new ValueTrace(calledMethod + "(...) [unresolved local-helper-return]",
                            new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, calledMethod + "(...)", List.of()));
                }

                helperCallStack.push(helperKey);
                ValueTrace helperReturnTrace = describeExpressionTrace(helperReturn.returnExpression(),
                        helperMethodContext.layer().classNode(), helperMethodContext.layer().metadata(), state,
                        helperReturn.helperExpressionScopes(), classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn,
                        helperCallStack,
                        visitedVariables,
                        emittedNestedFacadeTraceKeys,
                        depth + 1,
                        traceSourceClassFqn,
                        helperMethod.getName(),
                        traceScopeKey(helperMethodContext.layer().classFqn(), helperMethod.getTypeDescriptor()),
                        true,
                        helperReturn.helperRebindingFallbackScopes());
                helperCallStack.pop();

                return new ValueTrace(calledMethod + "(...) <- " + helperReturnTrace.provenance(),
                        new GroovyValueRecipe(GroovyValueKind.HELPER_CALL_RESULT, calledMethod,
                                List.of(helperReturnTrace.recipe())));
            }

            ValueTrace receiverTrace = null;
            Expression objectExpression = methodCallExpression.getObjectExpression();
            if (objectExpression != null && !(objectExpression instanceof VariableExpression variableExpression && "this".equals(variableExpression.getName()))) {
                receiverTrace = describeExpressionTrace(objectExpression,
                        classNode, metadata, state,
                        methodExpressionScopes, classFieldExpressionScopes, methodsByName,
                        visibleFieldKeysByClassFqn,
                        helperCallStack,
                        visitedVariables,
                        emittedNestedFacadeTraceKeys,
                        depth + 1,
                        traceSourceClassFqn,
                        traceMethodName,
                        traceScopeKey,
                        helperScope,
                        rebindingFallbackScopes);
            }

            List<GroovyValueRecipe> children = receiverTrace == null ? List.of() : List.of(receiverTrace.recipe());
            return new ValueTrace(methodCallExpression.getText() + " [unresolved external/runtime edge]",
                    new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, methodCallExpression.getText(), children));
        }

        return new ValueTrace(expression.getText() + " [unresolved external/runtime edge]",
                new GroovyValueRecipe(GroovyValueKind.UNRESOLVED_RUNTIME_EDGE, expression.getText(), List.of()));
    }

    private void registerNestedHelperFacadeTrace(ApplicationAnalysisState state,
                                                 FacadeResolution facadeResolution,
                                                 String traceSourceClassFqn,
                                                 String traceMethodName,
                                                 MethodCallExpression methodCallExpression,
                                                 Set<String> emittedNestedFacadeTraceKeys,
                                                 String traceScopeKey) {
        String nestedTraceKey = traceScopeKey + "::" + methodCallExpression.getText()
                + "::" + facadeResolution.creationSite().sagaClassFqn();
        if (!emittedNestedFacadeTraceKeys.add(nestedTraceKey)) {
            return;
        }

        List<String> traceLines = new ArrayList<>();
        traceLines.add(methodCallExpression.getText());
        facadeResolution.constructorArguments().forEach(argument ->
                traceLines.add("arg[" + argument.index() + "]: " + argument.provenance()));
        traceLines.addAll(facadeResolution.resolutionNotes());

        state.groovyFullTraceResults.add(new GroovyFullTraceResult(
                traceSourceClassFqn,
                traceMethodName,
                null,
                GroovyTraceOriginKind.FACADE_CALL,
                methodCallExpression.getText(),
                facadeResolution.creationSite().sagaClassFqn(),
                List.copyOf(facadeResolution.constructorArguments()),
                List.of(),
                List.copyOf(facadeResolution.resolutionNotes()),
                String.join(System.lineSeparator(), traceLines)
        ));
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
            String parameterName = parameters[i].getName();
            Expression normalizedArgument = normalizeHelperParameterBinding(parameterName,
                    callArguments.get(i),
                    callerExpressionScopes,
                    classFieldExpressionScopes);
            if (normalizedArgument != null) {
                helperExpressionScopes.put(parameterName, normalizedArgument);
            }
        }

        Map<String, Expression> helperRebindingFallbackScopes = new LinkedHashMap<>();

        MethodBodyScanResult bodyScanResult = scanMethodBody(helperBlock,
                helperExpressionScopes,
                helperRebindingFallbackScopes);
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

        return new HelperReturnResolution(returnExpressions.get(0), helperExpressionScopes, helperRebindingFallbackScopes);
    }

    private Expression normalizeHelperParameterBinding(String parameterName,
                                                      Expression callArgument,
                                                      Map<String, Expression> callerExpressionScopes,
                                                      Map<String, Expression> classFieldExpressionScopes) {
        if (!(callArgument instanceof VariableExpression variableArgument)
                || parameterName == null
                || parameterName.isBlank()
                || !Objects.equals(variableArgument.getName(), parameterName)) {
            return callArgument;
        }

        Expression callerResolvedExpression = resolveExpressionFromScopes(variableArgument.getName(),
                callerExpressionScopes,
                classFieldExpressionScopes);
        if (callerResolvedExpression == null || isSelfReference(variableArgument, callerResolvedExpression)) {
            return null;
        }

        return callerResolvedExpression;
    }

    private MethodBodyScanResult scanMethodBody(BlockStatement blockStatement,
                                                Map<String, Expression> helperExpressionScopes,
                                                Map<String, Expression> helperRebindingFallbackScopes) {
        List<Expression> returnExpressions = new ArrayList<>();
        Expression lastExpression = null;
        boolean ambiguousControlFlow = false;

        for (Statement statement : blockStatement.getStatements()) {
            if (statement instanceof BlockStatement nestedBlock) {
                Map<String, Expression> nestedScopes = new LinkedHashMap<>(helperExpressionScopes);
                Map<String, Expression> nestedFallbackScopes = new LinkedHashMap<>(helperRebindingFallbackScopes);
                MethodBodyScanResult nestedResult = scanMethodBody(nestedBlock, nestedScopes, nestedFallbackScopes);
                returnExpressions.addAll(nestedResult.returnExpressions());
                if (nestedResult.lastExpression() != null) {
                    lastExpression = nestedResult.lastExpression();
                }
                ambiguousControlFlow = ambiguousControlFlow || nestedResult.ambiguousControlFlow();
                if (!nestedResult.ambiguousControlFlow()) {
                    helperExpressionScopes.clear();
                    helperExpressionScopes.putAll(nestedScopes);
                    helperRebindingFallbackScopes.clear();
                    helperRebindingFallbackScopes.putAll(nestedFallbackScopes);
                }
                continue;
            }

            if (statement instanceof ExpressionStatement expressionStatement) {
                Expression expression = expressionStatement.getExpression();
                captureScopedAssignment(expression, helperExpressionScopes, helperRebindingFallbackScopes);
                lastExpression = expression;
                continue;
            }

            if (statement instanceof IfStatement ifStatement) {
                Map<String, Expression> ifScopes = new LinkedHashMap<>(helperExpressionScopes);
                Map<String, Expression> ifFallbackScopes = new LinkedHashMap<>(helperRebindingFallbackScopes);
                MethodBodyScanResult ifResult = scanMethodStatement(ifStatement.getIfBlock(), ifScopes, ifFallbackScopes);

                Statement elseStatement = ifStatement.getElseBlock();
                boolean hasElseBranch = hasMeaningfulElse(elseStatement);
                Map<String, Expression> elseScopes = new LinkedHashMap<>(helperExpressionScopes);
                Map<String, Expression> elseFallbackScopes = new LinkedHashMap<>(helperRebindingFallbackScopes);
                MethodBodyScanResult elseResult = scanMethodStatement(elseStatement, elseScopes, elseFallbackScopes);

                boolean hasBranchReturns = !ifResult.returnExpressions().isEmpty() || !elseResult.returnExpressions().isEmpty();
                boolean branchAmbiguous = ifResult.ambiguousControlFlow() || elseResult.ambiguousControlFlow();
                if (hasBranchReturns || branchAmbiguous) {
                    ambiguousControlFlow = true;
                    continue;
                }

                if (hasElseBranch && !equivalentScopedExpressions(ifScopes, elseScopes)) {
                    ambiguousControlFlow = true;
                    continue;
                }

                helperExpressionScopes.clear();
                helperExpressionScopes.putAll(ifScopes);
                if (hasElseBranch) {
                    helperExpressionScopes.putAll(elseScopes);
                }

                helperRebindingFallbackScopes.clear();
                helperRebindingFallbackScopes.putAll(ifFallbackScopes);
                if (hasElseBranch) {
                    elseFallbackScopes.forEach((key, value) -> {
                        if (!helperRebindingFallbackScopes.containsKey(key)
                                || Objects.equals(textOf(helperRebindingFallbackScopes.get(key)), textOf(value))) {
                            helperRebindingFallbackScopes.put(key, value);
                        }
                    });
                }

                if (ifResult.lastExpression() != null) {
                    lastExpression = ifResult.lastExpression();
                }
                if (elseResult.lastExpression() != null) {
                    lastExpression = elseResult.lastExpression();
                }
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

    private MethodBodyScanResult scanMethodStatement(Statement statement,
                                                     Map<String, Expression> helperExpressionScopes,
                                                     Map<String, Expression> helperRebindingFallbackScopes) {
        if (statement == null || statement instanceof EmptyStatement) {
            return new MethodBodyScanResult(List.of(), null, false);
        }

        if (statement instanceof BlockStatement nestedBlock) {
            return scanMethodBody(nestedBlock, helperExpressionScopes, helperRebindingFallbackScopes);
        }

        if (statement instanceof ExpressionStatement expressionStatement) {
            captureScopedAssignment(expressionStatement.getExpression(), helperExpressionScopes,
                    helperRebindingFallbackScopes);
            return new MethodBodyScanResult(List.of(), expressionStatement.getExpression(), false);
        }

        if (statement instanceof ReturnStatement returnStatement) {
            if (returnStatement.getExpression() == null) {
                return new MethodBodyScanResult(List.of(), null, false);
            }

            return new MethodBodyScanResult(List.of(returnStatement.getExpression()), returnStatement.getExpression(), false);
        }

        return new MethodBodyScanResult(List.of(), null, true);
    }

    private boolean hasMeaningfulElse(Statement elseStatement) {
        return elseStatement != null && !(elseStatement instanceof EmptyStatement);
    }

    private boolean equivalentScopedExpressions(Map<String, Expression> left,
                                                Map<String, Expression> right) {
        if (!Objects.equals(left.keySet(), right.keySet())) {
            return false;
        }

        for (Map.Entry<String, Expression> leftEntry : left.entrySet()) {
            Expression leftExpression = leftEntry.getValue();
            Expression rightExpression = right.get(leftEntry.getKey());
            if (!Objects.equals(textOf(leftExpression), textOf(rightExpression))) {
                return false;
            }
        }

        return true;
    }

    private String textOf(Expression expression) {
        return expression == null ? null : expression.getText();
    }

    private void captureScopedAssignment(Expression expression,
                                         Map<String, Expression> helperExpressionScopes,
                                         Map<String, Expression> helperRebindingFallbackScopes) {
        if (expression instanceof DeclarationExpression declarationExpression) {
            String variableName = resolveDeclaredVariableName(declarationExpression.getLeftExpression());
            if (variableName != null) {
                Expression previousExpression = helperExpressionScopes.get(variableName);
                Expression assignedExpression = declarationExpression.getRightExpression();
                if (previousExpression != null
                        && isSelfRebindingExpression(assignedExpression, variableName)
                        && !Objects.equals(textOf(previousExpression), textOf(assignedExpression))) {
                    helperRebindingFallbackScopes.put(variableName, previousExpression);
                }

                helperExpressionScopes.put(variableName, assignedExpression);
            }
            return;
        }

        if (expression instanceof BinaryExpression binaryExpression && isAssignment(binaryExpression)) {
            String variableName = resolveAssignedVariableName(binaryExpression.getLeftExpression());
            if (variableName != null) {
                Expression previousExpression = helperExpressionScopes.get(variableName);
                Expression assignedExpression = binaryExpression.getRightExpression();
                if (previousExpression != null
                        && isSelfRebindingExpression(assignedExpression, variableName)
                        && !Objects.equals(textOf(previousExpression), textOf(assignedExpression))) {
                    helperRebindingFallbackScopes.put(variableName, previousExpression);
                }

                helperExpressionScopes.put(variableName, assignedExpression);
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

    private boolean isSelfRebindingExpression(Expression expression,
                                              String variableName) {
        if (!(expression instanceof MethodCallExpression methodCallExpression)
                || variableName == null
                || variableName.isBlank()) {
            return false;
        }

        if (expressionReferencesVariable(methodCallExpression.getObjectExpression(), variableName)) {
            return true;
        }

        return extractArguments(methodCallExpression.getArguments()).stream()
                .anyMatch(argument -> expressionReferencesVariable(argument, variableName));
    }

    private boolean expressionReferencesVariable(Expression expression,
                                                 String variableName) {
        if (expression == null || variableName == null || variableName.isBlank()) {
            return false;
        }

        if (expression instanceof VariableExpression variableExpression) {
            return Objects.equals(variableExpression.getName(), variableName);
        }

        if (expression instanceof PropertyExpression propertyExpression) {
            return expressionReferencesVariable(propertyExpression.getObjectExpression(), variableName)
                    || expressionReferencesVariable(propertyExpression.getProperty(), variableName);
        }

        if (expression instanceof MethodCallExpression methodCallExpression) {
            if (expressionReferencesVariable(methodCallExpression.getObjectExpression(), variableName)) {
                return true;
            }

            return extractArguments(methodCallExpression.getArguments()).stream()
                    .anyMatch(argument -> expressionReferencesVariable(argument, variableName));
        }

        if (expression instanceof ConstructorCallExpression constructorCallExpression) {
            return extractArguments(constructorCallExpression.getArguments()).stream()
                    .anyMatch(argument -> expressionReferencesVariable(argument, variableName));
        }

        if (expression instanceof ListExpression listExpression) {
            return listExpression.getExpressions().stream()
                    .anyMatch(item -> expressionReferencesVariable(item, variableName));
        }

        if (expression instanceof TupleExpression tupleExpression) {
            return tupleExpression.getExpressions().stream()
                    .anyMatch(item -> expressionReferencesVariable(item, variableName));
        }

        if (expression instanceof BinaryExpression binaryExpression) {
            return expressionReferencesVariable(binaryExpression.getLeftExpression(), variableName)
                    || expressionReferencesVariable(binaryExpression.getRightExpression(), variableName);
        }

        return false;
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

    private boolean isLocalToSetTransform(MethodCallExpression methodCallExpression) {
        if (methodCallExpression == null) {
            return false;
        }

        String methodName = methodCallExpression.getMethodAsString();
        if (!"toSet".equals(methodName) || !extractArguments(methodCallExpression.getArguments()).isEmpty()) {
            return false;
        }

        return methodCallExpression.getObjectExpression() != null;
    }

    private boolean hasUnresolvedRecipe(GroovyValueRecipe recipe) {
        if (recipe == null) {
            return true;
        }

        if (recipe.kind() == GroovyValueKind.UNRESOLVED_VARIABLE || recipe.kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE) {
            return true;
        }

        return recipe.children() != null && recipe.children().stream().anyMatch(this::hasUnresolvedRecipe);
    }

    private boolean isCollectionLikeRecipe(GroovyValueRecipe recipe) {
        if (recipe == null) {
            return false;
        }

        return switch (recipe.kind()) {
            case COLLECTION_LITERAL -> true;
            case HELPER_CALL_RESULT, LOCAL_TRANSFORM -> recipe.children() != null
                    && recipe.children().stream().anyMatch(this::isCollectionLikeRecipe);
            default -> false;
        };
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
                                          GroovySourceClassMetadata resolutionMetadata,
                                          GroovyTraceOriginKind originKind,
                                          String sourceExpressionText) {
    }

    private record FacadeResolution(WorkflowFunctionalityCreationSite creationSite,
                                     String receiverTypeFqn,
                                     List<GroovyTraceArgument> constructorArguments,
                                     List<String> resolutionNotes) {
    }

    private record SelectedCreationSite(WorkflowFunctionalityCreationSite creationSite,
                                        String note) {
    }

    private record InheritanceLayer(String classFqn,
                                    ClassNode classNode,
                                    GroovySourceClassMetadata metadata) {
    }

    private record MethodResolutionContext(MethodNode methodNode,
                                           InheritanceLayer layer) {
    }

    private record HelperReturnResolution(Expression returnExpression,
                                          Map<String, Expression> helperExpressionScopes,
                                          Map<String, Expression> helperRebindingFallbackScopes) {
    }

    private record ValueTrace(String provenance, GroovyValueRecipe recipe) {
    }

    private record MethodBodyScanResult(List<Expression> returnExpressions,
                                        Expression lastExpression,
                                        boolean ambiguousControlFlow) {
    }

    private static final class TraceBuilder {
        private final String sourceClassFqn;
        private final String sourceMethodName;
        private final String sourceBindingName;
        private final String sagaClassFqn;
        private GroovyTraceOriginKind originKind;
        private String sourceExpressionText;
        private final List<String> traceLines = new ArrayList<>();
        private final List<GroovyTraceArgument> constructorArguments = new ArrayList<>();
        private final List<GroovyWorkflowCall> workflowCalls = new ArrayList<>();
        private final List<String> resolutionNotes = new ArrayList<>();
        private String lastLabel;

        private TraceBuilder(String sourceClassFqn,
                             String sourceMethodName,
                             String sourceBindingName,
                             String sagaClassFqn) {
            this.sourceClassFqn = sourceClassFqn;
            this.sourceMethodName = sourceMethodName;
            this.sourceBindingName = sourceBindingName;
            this.sagaClassFqn = sagaClassFqn;
        }

        private void appendConstructorLine(String constructorLine) {
            traceLines.add(constructorLine);
        }

        private void appendCallLine(String callLine, String contextLabel) {
            traceLines.add(callLine);
            workflowCalls.add(new GroovyWorkflowCall(callLine, contextLabel));
        }

        private void appendInputLine(int index, String provenance) {
            appendInputLine(index, provenance, null);
        }

        private void appendInputLine(int index, String provenance, GroovyValueRecipe recipe) {
            String inputLine = "arg[" + index + "]: " + provenance;
            traceLines.add(inputLine);
            constructorArguments.add(new GroovyTraceArgument(index, provenance, recipe));
        }

        private void appendDetailLine(String detailLine) {
            traceLines.add(detailLine);
            resolutionNotes.add(detailLine);
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
