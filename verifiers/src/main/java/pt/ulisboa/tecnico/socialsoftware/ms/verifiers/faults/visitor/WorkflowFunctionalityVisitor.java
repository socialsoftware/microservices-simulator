package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaConstructorSignature;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandRootKeyPath;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public class WorkflowFunctionalityVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            // Only process WorkflowFunctionality subclasses with SagaUnitOfWorkService
            if (!TypeUtils.isSubclassOf(decl, WorkflowFunctionality.class) ||
                    !declaresUnitOfWorkService(decl)) {
                return;
            }

            String packageName = decl.resolve().getPackageName();
            String sagaFqn = decl.getFullyQualifiedName().orElseGet(decl::getNameAsString);
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);
            String sagaClassName = decl.getNameAsString();

            SagaFunctionalityBuildingBlock sagaBlock =
                    new SagaFunctionalityBuildingBlock(filePath, packageName, sagaFqn);

            extractConstructorSignatures(decl, sagaBlock);

            // Extract all saga steps from new SagaStep(...) expressions
            extractSagaSteps(decl, state, filePath, packageName, sagaClassName, sagaBlock);

            state.sagas.add(sagaBlock);
            logger.info("Saga {}: {} steps", sagaClassName, sagaBlock.getSteps().size());
        });
    }

    /**
     * Checks if the class injects SagaUnitOfWorkService through either a field or a constructor parameter.
     */
    private boolean declaresUnitOfWorkService(ClassOrInterfaceDeclaration decl) {
        boolean hasFieldInjection = decl.getFields().stream()
                .anyMatch(field -> declaresSagaUnitOfWorkService(field));

        boolean hasConstructorInjection = decl.getConstructors().stream()
                .anyMatch(ctor -> ctor.getParameters().stream()
                        .anyMatch(p -> TypeUtils.isSubtypeOf(p.getType(), SagaUnitOfWorkService.class)));

        return hasFieldInjection || hasConstructorInjection;
    }

    private boolean declaresSagaUnitOfWorkService(FieldDeclaration field) {
        return TypeUtils.isSubtypeOf(field.getCommonType(), SagaUnitOfWorkService.class);
    }

    private void extractConstructorSignatures(ClassOrInterfaceDeclaration decl,
                                              SagaFunctionalityBuildingBlock sagaBlock) {
        decl.getConstructors().forEach(constructor -> {
            List<String> parameterTypeFqns = constructor.getParameters().stream()
                    .map(parameter -> resolveParameterTypeFqn(constructor, parameter))
                    .toList();
            sagaBlock.addConstructorSignature(new SagaConstructorSignature(parameterTypeFqns));
        });
    }

    private String resolveParameterTypeFqn(ConstructorDeclaration constructor, com.github.javaparser.ast.body.Parameter parameter) {
        try {
            return parameter.getType().resolve().describe();
        } catch (Exception e) {
            logger.debug("Could not resolve constructor param type '{}' in {}: {}",
                    parameter.getTypeAsString(), constructor.getNameAsString(), e.getMessage());
            return parameter.getTypeAsString();
        }
    }

    /**
     * Extracts all saga steps from new SagaStep(...) expressions in the class body.
     */
    private void extractSagaSteps(ClassOrInterfaceDeclaration decl, ApplicationAnalysisState state,
                                  Path filePath, String packageName, String sagaClassName,
                                  SagaFunctionalityBuildingBlock sagaBlock) {
        decl.findAll(ObjectCreationExpr.class).forEach(expr -> {
            if (!TypeUtils.isSubtypeOf(expr.getType(), SagaStep.class)
                    || expr.getArguments().size() < 2) {
                return;
            }

            expr.getArgument(0).ifStringLiteralExpr(literal -> {
                String stepName = literal.getValue();
                String stepKey = sagaClassName + "::" + stepName;

                SagaStepBuildingBlock stepBlock =
                        new SagaStepBuildingBlock(filePath, packageName, stepKey, stepName);

                Expression lambdaArg = expr.getArgument(1);
                extractStepFootprints(lambdaArg, stepBlock, state, decl, stepKey, DispatchPhase.FORWARD);

                Map<String, String> stepVariableToStepKey =
                        buildStepVariableToStepKeyMap(expr, sagaClassName);

                if (expr.getArguments().size() >= 3) {
                    extractPredecessorStepKeys(expr.getArgument(2), stepBlock, stepVariableToStepKey);
                }

                extractStepCompensations(expr, stepBlock, state, stepKey, stepVariableToStepKey);

                sagaBlock.addStep(stepBlock);
                logger.info("Step {}: {} dispatches", stepKey, stepBlock.getDispatches().size());
            });
        });
    }

    private Map<String, String> buildStepVariableToStepKeyMap(ObjectCreationExpr currentStepExpr,
                                                              String sagaClassName) {
        Map<String, String> stepVariableToStepKey = new LinkedHashMap<>();
        currentStepExpr.findAncestor(BlockStmt.class).ifPresent(block -> {
            for (var statement : block.getStatements()) {
                if (!statement.isExpressionStmt()) {
                    continue;
                }

                ExpressionStmt exprStmt = statement.asExpressionStmt();
                exprStmt.getExpression().ifVariableDeclarationExpr(varDecl -> {
                    varDecl.getVariables().forEach(variable -> {
                        Optional<ObjectCreationExpr> init = variable.getInitializer()
                                .filter(Expression::isObjectCreationExpr)
                                .map(Expression::asObjectCreationExpr);
                        if (init.isEmpty()
                                || !TypeUtils.isSubtypeOf(init.get().getType(), SagaStep.class)
                                || init.get().getArguments().isEmpty()) {
                            return;
                        }

                        init.get().getArgument(0).ifStringLiteralExpr(literal -> {
                            String stepKey = sagaClassName + "::" + literal.getValue();
                            stepVariableToStepKey.put(variable.getNameAsString(), stepKey);
                        });
                    });
                });
            }
        });

        return stepVariableToStepKey;
    }

    private void extractPredecessorStepKeys(Expression dependencyArg, SagaStepBuildingBlock stepBlock,
                                            Map<String, String> stepVariableToStepKey) {
        dependencyArg.findAll(NameExpr.class).forEach(nameExpr -> {
            if (isMethodCallScope(nameExpr)) {
                return;
            }
            if (!isResolvedSagaStepReference(nameExpr)) {
                logger.warn("Unknown dependency reference '{}' in step {}", nameExpr.getNameAsString(),
                        stepBlock.getName());
                return;
            }

            String predecessorStepKey = stepVariableToStepKey.get(nameExpr.getNameAsString());
            if (predecessorStepKey != null) {
                stepBlock.addPredecessorStepKey(predecessorStepKey);
            } else {
                logger.warn("Unknown dependency reference '{}' in step {}", nameExpr.getNameAsString(),
                        stepBlock.getName());
            }
        });
    }

    private boolean isResolvedSagaStepReference(NameExpr nameExpr) {
        try {
            return TypeUtils.isResolvedSubtypeOf(nameExpr.calculateResolvedType(), SagaStep.class);
        } catch (Exception e) {
            logger.debug("Could not resolve dependency reference '{}': {}", nameExpr, e.getMessage());
            return false;
        }
    }

    private boolean isMethodCallScope(NameExpr nameExpr) {
        return nameExpr.findAncestor(MethodCallExpr.class)
                .map(call -> call.getScope().map(scope -> scope == nameExpr).orElse(false))
                .orElse(false);
    }

    /**
     * Extracts dispatch footprints from all new *Command(...) expressions in the lambda/method reference body.
     */
    private void extractStepFootprints(Expression operation, SagaStepBuildingBlock stepBlock,
                                       ApplicationAnalysisState state, ClassOrInterfaceDeclaration sagaDeclaration,
                                       String stepKey, DispatchPhase phase) {
        if (!operation.isLambdaExpr()) {
            String code = operation.isMethodReferenceExpr()
                    ? "UNSUPPORTED_METHOD_REFERENCE"
                    : "UNSUPPORTED_OPERATION_SHAPE";
            String message = "cannot resolve " + phase.name().toLowerCase()
                    + " dispatches from " + operation.getClass().getSimpleName();
            stepBlock.markDispatchAnalysisIncomplete(phase, code, message);
            logger.warn("{} (step: {})", message, stepKey);
            return;
        }

        LambdaExpr lambda = operation.asLambdaExpr();
        Set<ObjectCreationExpr> recognizedCommandCreations = new LinkedHashSet<>();
        Set<VariableDeclarator> recognizedCommandVariables = new LinkedHashSet<>();
        Map<ObjectCreationExpr, StepDispatchFootprint> dispatchByCommandCreation = new LinkedHashMap<>();
        Set<MethodCallExpr> unresolvedAggregateKeyCalls = new LinkedHashSet<>();
        lambda.findAll(ObjectCreationExpr.class).forEach(creation -> {
            String commandTypeFqn;
            try {
                var resolvedType = creation.getType().resolve();
                if (!TypeUtils.isResolvedSubtypeOf(resolvedType, Command.class)) {
                    return;
                }
                commandTypeFqn = resolvedType.describe();
                if (TypeUtils.isResolvedSubtypeOf(resolvedType, SagaCommand.class)) {
                    return;
                }
            } catch (Exception exception) {
                if (creation.getType().asString().endsWith("Command")) {
                    String message = "command type could not be resolved: " + creation.getType().asString();
                    stepBlock.markDispatchAnalysisIncomplete(
                            phase, "UNRESOLVED_COMMAND_TYPE", message);
                    logger.warn("{} (step: {})", message, stepKey);
                }
                return;
            }

            if (Command.class.getName().equals(commandTypeFqn) && phase == DispatchPhase.COMPENSATION) {
                resolveGenericCompensation(creation, state, sagaDeclaration).ifPresentOrElse(
                        target -> {
                            StepDispatchFootprint dispatch = new StepDispatchFootprint(
                                    stepKey,
                                    commandTypeFqn,
                                    target.aggregateName(),
                                    AccessPolicy.WRITE,
                                    phase,
                                    inferDispatchMultiplicity(creation),
                                    target.aggregateKey().text(),
                                    target.aggregateKey().confidence(),
                                    target.aggregateKey().sagaConstructorArgumentIndex(),
                                    target.aggregateKey().propertyPath());
                            stepBlock.addDispatch(dispatch);
                            dispatchByCommandCreation.put(creation, dispatch);
                            recognizeCommandCreation(creation, recognizedCommandCreations,
                                    recognizedCommandVariables);
                        },
                        () -> {
                            recognizeCommandCreation(creation, recognizedCommandCreations,
                                    recognizedCommandVariables);
                            markUnresolvedCompensationPayload(stepBlock, stepKey);
                        });
                return;
            }

            state.getCommandDispatchInfo(creation.getType()).ifPresentOrElse(
                    info -> {
                        AggregateKeyResolution aggregateKey = resolveAggregateKey(
                                creation, commandTypeFqn, state, sagaDeclaration);
                        StepDispatchFootprint dispatch = new StepDispatchFootprint(
                                stepKey,
                                commandTypeFqn,
                                info.aggregateName(),
                                info.accessPolicy(),
                                phase,
                                inferDispatchMultiplicity(creation),
                                aggregateKey.text(),
                                aggregateKey.confidence(),
                                aggregateKey.sagaConstructorArgumentIndex(),
                                aggregateKey.propertyPath());
                        stepBlock.addDispatch(dispatch);
                        dispatchByCommandCreation.put(creation, dispatch);
                        recognizeCommandCreation(creation, recognizedCommandCreations,
                                recognizedCommandVariables);
                        if (aggregateKey.text() == null) {
                            unresolvedAggregateKeyCalls.addAll(rootArgumentMethodCalls(
                                    creation, commandTypeFqn, state));
                        }
                    },
                    () -> {
                        String message = "command dispatch not found in registry: " + creation.getType().asString();
                        stepBlock.markDispatchAnalysisIncomplete(
                                phase, "UNRESOLVED_COMMAND_DISPATCH", message);
                        logger.warn("{} (step: {})", message, stepKey);
                        recognizeCommandCreation(creation, recognizedCommandCreations,
                                recognizedCommandVariables);
                    }
            );
        });

        Set<ObjectCreationExpr> recognizedWrapperCreations = new LinkedHashSet<>();
        lambda.findAll(ObjectCreationExpr.class).stream()
                .filter(this::isSagaCommandCreation)
                .forEach(wrapper -> {
                    recognizedWrapperCreations.add(wrapper);
                    recognizeCommandCreation(wrapper, recognizedWrapperCreations,
                            recognizedCommandVariables);
                    boolean payloadResolved = wrapper.getArguments().size() == 1
                            && resolvesToRecognizedCommand(
                                    wrapper.getArgument(0), recognizedCommandCreations,
                                    recognizedCommandVariables);
                    if (!payloadResolved) {
                        String code = phase == DispatchPhase.COMPENSATION
                                ? "UNRESOLVED_COMPENSATION_PAYLOAD"
                                : "UNRESOLVED_COMMAND_PAYLOAD";
                        String message = phase == DispatchPhase.COMPENSATION
                                ? "cannot resolve compensation command payload"
                                : "cannot resolve SagaCommand payload";
                        stepBlock.markDispatchAnalysisIncomplete(phase, code, message);
                        logger.warn("{} (step: {})", message, stepKey);
                        return;
                    }

                    resolveRecognizedCommandDispatch(wrapper.getArgument(0), dispatchByCommandCreation)
                            .ifPresent(payloadDispatch -> addSemanticLockWriteForDispatchedWrapper(
                                    lambda, wrapper, payloadDispatch, stepBlock));
                });

        lambda.findAll(MethodCallExpr.class).stream()
                .filter(call -> !isSupportedCommandGatewayDispatch(
                        call, recognizedCommandCreations, recognizedCommandVariables))
                .filter(call -> !isSupportedCommandGatewayDispatch(
                        call, recognizedWrapperCreations, recognizedCommandVariables))
                .filter(call -> unresolvedAggregateKeyCalls.contains(call)
                        || callMayHideCommandDispatch(call))
                .forEach(call -> {
                    String code = unresolvedAggregateKeyCalls.contains(call)
                            ? "UNRESOLVED_AGGREGATE_KEY"
                            : "UNRESOLVED_COMMAND_DISPATCH";
                    String message = unresolvedAggregateKeyCalls.contains(call)
                            ? "cannot resolve aggregate key from call " + call.getNameAsString()
                            : "cannot resolve command dispatch through call " + call.getNameAsString();
                    stepBlock.markDispatchAnalysisIncomplete(
                            phase, code, message);
                    logger.warn("{} (step: {})", message, stepKey);
                });
    }

    private void recognizeCommandCreation(ObjectCreationExpr creation,
                                          Set<ObjectCreationExpr> recognizedCreations,
                                          Set<VariableDeclarator> recognizedVariables) {
        recognizedCreations.add(creation);
        creation.findAncestor(VariableDeclarator.class)
                .filter(variable -> variable.getInitializer()
                        .map(initializer -> initializer == creation)
                        .orElse(false))
                .ifPresent(recognizedVariables::add);
    }

    private boolean isSagaCommandCreation(ObjectCreationExpr creation) {
        try {
            return TypeUtils.isResolvedSubtypeOf(creation.getType().resolve(), SagaCommand.class);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean resolvesToRecognizedCommand(Expression expression,
                                                Set<ObjectCreationExpr> recognizedCreations,
                                                Set<VariableDeclarator> recognizedVariables) {
        return expression.isObjectCreationExpr()
                ? recognizedCreations.contains(expression.asObjectCreationExpr())
                : resolvesToRecognizedCommandVariable(expression, recognizedVariables);
    }

    private Optional<StepDispatchFootprint> resolveRecognizedCommandDispatch(
            Expression expression,
            Map<ObjectCreationExpr, StepDispatchFootprint> dispatchByCommandCreation) {
        Expression source = unwrap(expression);
        if (source.isObjectCreationExpr()) {
            return Optional.ofNullable(dispatchByCommandCreation.get(source.asObjectCreationExpr()));
        }
        return dispatchByCommandCreation.entrySet().stream()
                .filter(entry -> resolvesToCreation(source, entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private void addSemanticLockWriteForDispatchedWrapper(
            LambdaExpr operation,
            ObjectCreationExpr wrapper,
            StepDispatchFootprint payloadDispatch,
            SagaStepBuildingBlock stepBlock) {
        Set<ObjectCreationExpr> wrapperCreations = Set.of(wrapper);
        Set<VariableDeclarator> wrapperVariables = wrapper.findAncestor(VariableDeclarator.class)
                .filter(variable -> variable.getInitializer().map(initializer -> initializer == wrapper).orElse(false))
                .map(Set::of)
                .orElseGet(Set::of);
        List<MethodCallExpr> sends = operation.findAll(MethodCallExpr.class).stream()
                .filter(call -> isSupportedCommandGatewayDispatch(call, wrapperCreations, wrapperVariables))
                .toList();
        if (sends.isEmpty()) {
            return;
        }
        if (!isExactFrameworkSagaCommandConstruction(wrapper)) {
            markUnresolvedSagaCommandConfiguration(payloadDispatch, stepBlock);
            return;
        }

        List<MethodCallExpr> semanticLockSetters = operation.findAll(MethodCallExpr.class).stream()
                .filter(call -> isExactSagaCommandCall(call, wrapper, "setSemanticLock", 1))
                .toList();
        boolean writesSemanticState = false;
        boolean uncertainConfiguration = false;
        for (MethodCallExpr send : sends) {
            List<MethodCallExpr> precedingSetters = semanticLockSetters.stream()
                    .filter(setter -> occursBefore(setter, send))
                    .toList();
            if (!sameBoundedExecutionContext(operation, wrapper, send)
                    || hasWrapperAssignmentBefore(operation, wrapper, send)
                    || hasWrapperAliasBefore(operation, wrapper, send)
                    || precedingSetters.stream().anyMatch(setter ->
                    !sameBoundedExecutionContext(operation, setter, send))) {
                uncertainConfiguration = true;
                continue;
            }
            boolean occurrenceWrites = precedingSetters.stream()
                    .reduce((left, right) -> occursBefore(left, right) ? right : left)
                    .map(setter -> !unwrap(setter.getArgument(0)).isNullLiteralExpr())
                    .orElse(false);
            writesSemanticState = writesSemanticState || occurrenceWrites;
        }
        if (uncertainConfiguration) {
            markUnresolvedSagaCommandConfiguration(payloadDispatch, stepBlock);
        }
        if (!writesSemanticState) {
            return;
        }

        stepBlock.addDispatch(new StepDispatchFootprint(
                payloadDispatch.stepKey(),
                SagaCommand.class.getName(),
                payloadDispatch.aggregateName(),
                AccessPolicy.WRITE,
                payloadDispatch.phase(),
                payloadDispatch.multiplicity(),
                payloadDispatch.aggregateKeyText(),
                payloadDispatch.aggregateKeyConfidence(),
                payloadDispatch.aggregateKeyConstructorArgumentIndex(),
                payloadDispatch.aggregateKeyPropertyPath()));
    }

    private boolean isExactFrameworkSagaCommandConstruction(ObjectCreationExpr wrapper) {
        if (wrapper.getAnonymousClassBody().isPresent()) {
            return false;
        }
        try {
            return wrapper.getType().resolve().describe().equals(SagaCommand.class.getName());
        } catch (Exception exception) {
            return false;
        }
    }

    private void markUnresolvedSagaCommandConfiguration(StepDispatchFootprint payloadDispatch,
                                                        SagaStepBuildingBlock stepBlock) {
        String message = "cannot resolve SagaCommand configuration for exact dispatched wrapper";
        stepBlock.markDispatchAnalysisIncomplete(payloadDispatch.phase(),
                "UNRESOLVED_SAGA_COMMAND_CONFIGURATION", message);
        logger.warn("{} (step: {})", message, payloadDispatch.stepKey());
    }

    private boolean hasWrapperAssignmentBefore(LambdaExpr operation,
                                               ObjectCreationExpr wrapper,
                                               MethodCallExpr send) {
        return operation.findAll(AssignExpr.class).stream()
                .filter(assignment -> occursBefore(assignment, send))
                .anyMatch(assignment -> resolvesToCreation(assignment.getTarget(), wrapper)
                        || resolvesToCreation(assignment.getValue(), wrapper));
    }

    private boolean hasWrapperAliasBefore(LambdaExpr operation,
                                          ObjectCreationExpr wrapper,
                                          MethodCallExpr send) {
        return operation.findAll(VariableDeclarator.class).stream()
                .filter(variable -> occursBefore(variable, send))
                .filter(variable -> variable.getInitializer().isPresent())
                .filter(variable -> unwrap(variable.getInitializer().orElseThrow()) != wrapper)
                .anyMatch(variable -> resolvesToCreation(variable.getInitializer().orElseThrow(), wrapper));
    }

    private boolean sameBoundedExecutionContext(LambdaExpr operation, Node left, Node right) {
        List<ExecutionContext> leftContexts = boundedExecutionContexts(operation, left);
        List<ExecutionContext> rightContexts = boundedExecutionContexts(operation, right);
        if (leftContexts.size() != rightContexts.size()) {
            return false;
        }
        for (int index = 0; index < leftContexts.size(); index++) {
            if (leftContexts.get(index).owner() != rightContexts.get(index).owner()
                    || leftContexts.get(index).branch() != rightContexts.get(index).branch()) {
                return false;
            }
        }
        return true;
    }

    private List<ExecutionContext> boundedExecutionContexts(LambdaExpr operation, Node source) {
        List<ExecutionContext> contexts = new ArrayList<>();
        Node child = source;
        Optional<Node> parent = source.getParentNode();
        while (parent.isPresent() && parent.orElseThrow() != operation) {
            Node owner = parent.orElseThrow();
            if (isBoundedControlContext(owner)
                    || owner instanceof LambdaExpr) {
                Node branch = owner instanceof SwitchEntry ? owner : child;
                contexts.add(new ExecutionContext(owner, branch));
            }
            child = owner;
            parent = owner.getParentNode();
        }
        return contexts;
    }

    private boolean isBoundedControlContext(Node node) {
        return node instanceof IfStmt
                || node instanceof ConditionalExpr
                || node instanceof ForStmt
                || node instanceof ForEachStmt
                || node instanceof WhileStmt
                || node instanceof DoStmt
                || node instanceof SwitchEntry
                || node instanceof TryStmt;
    }

    private record ExecutionContext(Node owner, Node branch) { }

    private boolean isExactSagaCommandCall(MethodCallExpr call,
                                           ObjectCreationExpr wrapper,
                                           String methodName,
                                           int argumentCount) {
        if (!call.getNameAsString().equals(methodName)
                || call.getArguments().size() != argumentCount
                || call.getScope().isEmpty()
                || !resolvesToCreation(call.getScope().orElseThrow(), wrapper)) {
            return false;
        }
        try {
            return call.resolve().declaringType().getQualifiedName().equals(SagaCommand.class.getName());
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean resolvesToCreation(Expression expression, ObjectCreationExpr creation) {
        Expression source = unwrap(expression);
        if (source.isObjectCreationExpr()) {
            return source == creation;
        }
        if (!source.isNameExpr()) {
            return false;
        }
        try {
            Optional<Node> declaration = source.asNameExpr().resolve().toAst();
            Optional<VariableDeclarator> owner = creation.findAncestor(VariableDeclarator.class);
            if (owner.isEmpty()) {
                return false;
            }
            if (declaration.filter(VariableDeclarator.class::isInstance).isPresent()) {
                return sameDeclaration(owner.orElseThrow(),
                        (VariableDeclarator) declaration.orElseThrow());
            }
            return declaration.filter(VariableDeclarationExpr.class::isInstance)
                    .map(VariableDeclarationExpr.class::cast)
                    .map(variableDeclaration -> variableDeclaration.getVariables().stream()
                            .filter(variable -> variable.getNameAsString()
                                    .equals(source.asNameExpr().getNameAsString()))
                            .anyMatch(variable -> sameDeclaration(owner.orElseThrow(), variable)))
                    .orElse(false);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean occursBefore(Node left, Node right) {
        return left.getBegin().flatMap(leftBegin -> right.getBegin()
                        .map(rightBegin -> leftBegin.compareTo(rightBegin) < 0))
                .orElse(false);
    }

    private boolean isSupportedCommandGatewayDispatch(MethodCallExpr call,
                                                        Set<ObjectCreationExpr> recognizedCommandCreations,
                                                        Set<VariableDeclarator> recognizedCommandVariables) {
        if (!(call.getNameAsString().equals("send") || call.getNameAsString().equals("sendAsync"))
                || call.getArguments().size() != 1 || call.getScope().isEmpty()) {
            return false;
        }

        Expression commandArgument = call.getArgument(0);
        boolean recognizedCommand = commandArgument.isObjectCreationExpr()
                ? recognizedCommandCreations.contains(commandArgument.asObjectCreationExpr())
                : resolvesToRecognizedCommandVariable(commandArgument, recognizedCommandVariables);
        if (!recognizedCommand) {
            return false;
        }

        try {
            var resolvedMethod = call.resolve();
            return resolvedMethod.getNumberOfParams() == 1
                    && resolvedMethod.getParam(0).getType().describe().equals(Command.class.getName())
                    && TypeUtils.isResolvedSubtypeOf(
                            call.getScope().orElseThrow().calculateResolvedType(), CommandGateway.class);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean resolvesToRecognizedCommandVariable(Expression commandArgument,
                                                          Set<VariableDeclarator> recognizedCommandVariables) {
        if (!commandArgument.isNameExpr()) {
            return false;
        }
        try {
            NameExpr commandName = commandArgument.asNameExpr();
            Optional<Node> declarationNode = commandName.resolve().toAst();
            if (declarationNode.filter(VariableDeclarator.class::isInstance).isPresent()) {
                VariableDeclarator resolved = (VariableDeclarator) declarationNode.orElseThrow();
                return recognizedCommandVariables.stream()
                        .anyMatch(recognized -> sameDeclaration(recognized, resolved));
            }
            return declarationNode.filter(VariableDeclarationExpr.class::isInstance)
                    .map(VariableDeclarationExpr.class::cast)
                    .map(declaration -> declaration.getVariables().stream()
                            .filter(variable -> variable.getNameAsString().equals(commandName.getNameAsString()))
                            .anyMatch(resolvedVariable -> recognizedCommandVariables.stream()
                                    .anyMatch(recognizedVariable -> sameDeclaration(recognizedVariable, resolvedVariable))))
                    .orElse(false);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean sameDeclaration(VariableDeclarator left, VariableDeclarator right) {
        return sameDeclarationNode(left, right);
    }

    private boolean sameDeclaration(MethodDeclaration left, MethodDeclaration right) {
        return sameDeclarationNode(left, right);
    }

    private boolean sameDeclarationNode(Node left, Node right) {
        return left.getRange().equals(right.getRange())
                && left.findCompilationUnit().flatMap(CompilationUnit::getStorage).map(CompilationUnit.Storage::getPath)
                .equals(right.findCompilationUnit().flatMap(CompilationUnit::getStorage).map(CompilationUnit.Storage::getPath));
    }

    private Optional<GenericCompensationTarget> resolveGenericCompensation(
            ObjectCreationExpr creation,
            ApplicationAnalysisState state,
            ClassOrInterfaceDeclaration sagaDeclaration) {
        if (creation.getArguments().size() != 3) {
            return Optional.empty();
        }
        String serviceToken = resolveServiceToken(creation.getArgument(1)).orElse(null);
        if (serviceToken == null) {
            return Optional.empty();
        }
        List<String> matchingAggregates = state.commandHandlers.stream()
                .filter(handler -> simpleName(handler.getFqn())
                        .equalsIgnoreCase(serviceToken + "CommandHandler"))
                .flatMap(handler -> handler.getCommandDispatch().values().stream())
                .map(CommandDispatchInfo::aggregateName)
                .filter(name -> name != null)
                .distinct()
                .toList();
        if (matchingAggregates.size() != 1) {
            return Optional.empty();
        }
        AggregateKeyResolution key = resolveAggregateKey(
                creation, Command.class.getName(), state, sagaDeclaration);
        if (key.text() == null) {
            return Optional.empty();
        }
        return Optional.of(new GenericCompensationTarget(matchingAggregates.get(0), key));
    }

    private Optional<String> resolveServiceToken(Expression expression) {
        Expression source = unwrap(expression);
        if (source.isStringLiteralExpr()) {
            return Optional.of(source.asStringLiteralExpr().asString());
        }
        if (!source.isMethodCallExpr()) {
            return Optional.empty();
        }
        MethodCallExpr call = source.asMethodCallExpr();
        if (!call.getNameAsString().equals("getServiceName")
                || !call.getArguments().isEmpty() || call.getScope().isEmpty()) {
            return Optional.empty();
        }
        Expression scope = unwrap(call.getScope().orElseThrow());
        if (scope.isFieldAccessExpr()) {
            return Optional.of(scope.asFieldAccessExpr().getNameAsString());
        }
        if (scope.isNameExpr()) {
            return Optional.of(scope.asNameExpr().getNameAsString());
        }
        return Optional.empty();
    }

    private void markUnresolvedCompensationPayload(SagaStepBuildingBlock stepBlock, String stepKey) {
        String message = "cannot resolve compensation command target and aggregate key";
        stepBlock.markDispatchAnalysisIncomplete(
                DispatchPhase.COMPENSATION, "UNRESOLVED_COMPENSATION_PAYLOAD", message);
        logger.warn("{} (step: {})", message, stepKey);
    }

    private Set<MethodCallExpr> rootArgumentMethodCalls(ObjectCreationExpr creation,
                                                         String commandTypeFqn,
                                                         ApplicationAnalysisState state) {
        CommandRootKeyPath rootPath;
        if (Command.class.getName().equals(commandTypeFqn)) {
            rootPath = creation.getArguments().size() == 3
                    ? CommandRootKeyPath.baseCommand("Command") : null;
        } else {
            String signature = resolveCommandConstructorSignature(creation);
            rootPath = signature == null ? null
                    : state.getCommandRootKeyPath(commandTypeFqn, signature).orElse(null);
        }
        if (rootPath == null || rootPath.literalText() != null
                || rootPath.constructorParameterIndex() < 0
                || rootPath.constructorParameterIndex() >= creation.getArguments().size()) {
            return Set.of();
        }
        Expression root = unwrap(creation.getArgument(rootPath.constructorParameterIndex()));
        if (root.isNullLiteralExpr()) {
            return Set.of();
        }
        return root.findAll(MethodCallExpr.class).stream()
                .filter(call -> callMayHideCommandDispatch(call) || !isOrdinaryGetter(call))
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean isOrdinaryGetter(MethodCallExpr call) {
        if (!call.getArguments().isEmpty()
                || !(call.getNameAsString().matches("get[A-Z].*")
                || call.getNameAsString().matches("is[A-Z].*"))) {
            return false;
        }
        try {
            var resolved = call.resolve();
            return !resolved.isStatic() && resolved.getNumberOfParams() == 0;
        } catch (Exception exception) {
            return false;
        }
    }

    private String simpleName(String fqn) {
        if (fqn == null) {
            return "";
        }
        int separator = fqn.lastIndexOf('.');
        return separator < 0 ? fqn : fqn.substring(separator + 1);
    }

    private boolean callMayHideCommandDispatch(MethodCallExpr call) {
        if (isCommandTypedInvocation(call)) {
            return true;
        }
        try {
            return call.resolve().toAst()
                    .filter(MethodDeclaration.class::isInstance)
                    .map(MethodDeclaration.class::cast)
                    .map(method -> methodMayDispatchCommand(method, new LinkedHashSet<>()))
                    .orElse(false);
        } catch (Exception exception) {
            return false;
        }
    }

    private boolean isCommandTypedInvocation(MethodCallExpr call) {
        try {
            if (call.getScope().isPresent()
                    && TypeUtils.isResolvedSubtypeOf(
                            call.getScope().orElseThrow().calculateResolvedType(), CommandGateway.class)
                    && (call.getNameAsString().equals("send")
                    || call.getNameAsString().equals("sendAsync"))) {
                return true;
            }
            var resolved = call.resolve();
            for (int index = 0; index < Math.min(resolved.getNumberOfParams(), call.getArguments().size()); index++) {
                if (TypeUtils.isResolvedSubtypeOf(resolved.getParam(index).getType(), Command.class)
                        || TypeUtils.isResolvedSubtypeOf(
                        call.getArgument(index).calculateResolvedType(), Command.class)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            // An unresolved ordinary call is not enough to claim a hidden dispatch.
        }
        return false;
    }

    private boolean methodMayDispatchCommand(MethodDeclaration method,
                                             Set<MethodDeclaration> visited) {
        if (!visited.add(method)) {
            return false;
        }
        boolean directCreation = method.findAll(ObjectCreationExpr.class).stream().anyMatch(creation -> {
            try {
                return TypeUtils.isResolvedSubtypeOf(creation.getType().resolve(), Command.class);
            } catch (Exception exception) {
                return creation.getType().asString().endsWith("Command");
            }
        });
        if (directCreation) {
            return true;
        }
        return method.findAll(MethodCallExpr.class).stream().anyMatch(nested -> {
            if (isCommandTypedInvocation(nested)) {
                return true;
            }
            try {
                return nested.resolve().toAst()
                        .filter(MethodDeclaration.class::isInstance)
                        .map(MethodDeclaration.class::cast)
                        .map(candidate -> methodMayDispatchCommand(candidate, visited))
                        .orElse(false);
            } catch (Exception exception) {
                return false;
            }
        });
    }

    private AggregateKeyResolution resolveAggregateKey(
            ObjectCreationExpr commandCreation,
            String commandTypeFqn,
            ApplicationAnalysisState state,
            ClassOrInterfaceDeclaration sagaDeclaration) {
        CommandCreationRoot root = resolveCommandCreationRoot(commandCreation, commandTypeFqn, state);
        if (root == null) {
            return AggregateKeyResolution.unresolved();
        }
        Integer constructorIndex = root.sourceParameter() == null || sagaDeclaration == null
                ? null
                : inferAggregateKeyConstructorArgumentIndex(
                        commandCreation, root.sourceParameter(), sagaDeclaration, state);
        return new AggregateKeyResolution(
                root.text(), root.confidence(), constructorIndex, root.propertyPath());
    }

    private CommandCreationRoot resolveCommandCreationRoot(
            ObjectCreationExpr commandCreation,
            String commandTypeFqn,
            ApplicationAnalysisState state) {
        CommandRootKeyPath rootPath;
        if (Command.class.getName().equals(commandTypeFqn)) {
            if (commandCreation.getArguments().size() != 3) {
                return null;
            }
            rootPath = CommandRootKeyPath.baseCommand("Command");
        } else {
            String constructorSignature = resolveCommandConstructorSignature(commandCreation);
            if (constructorSignature == null) {
                return null;
            }
            rootPath = state.getCommandRootKeyPath(commandTypeFqn, constructorSignature).orElse(null);
            if (rootPath == null) {
                return null;
            }
        }

        if (rootPath.literalText() != null) {
            return new CommandCreationRoot(
                    rootPath.literalText(),
                    StepDispatchFootprint.AggregateKeyConfidence.EXACT,
                    null,
                    List.of());
        }

        int parameterIndex = rootPath.constructorParameterIndex();
        if (parameterIndex < 0 || parameterIndex >= commandCreation.getArguments().size()) {
            return null;
        }
        Expression source = unwrap(commandCreation.getArgument(parameterIndex));
        if (source.isNullLiteralExpr()) {
            return null;
        }

        if (isSupportedLiteralExpression(source)) {
            if (!rootPath.propertyPath().isEmpty()) {
                return null;
            }
            return new CommandCreationRoot(
                    source.toString(),
                    StepDispatchFootprint.AggregateKeyConfidence.EXACT,
                    null,
                    List.of());
        }

        if (!isSupportedSymbolicRootExpression(source)) {
            return null;
        }

        Optional<SourceParameterPath> sourcePath = directSourceParameterPath(source);
        List<String> propertyPath = new java.util.ArrayList<>();
        sourcePath.ifPresent(path -> propertyPath.addAll(path.propertyPath()));
        propertyPath.addAll(rootPath.propertyPath());

        String text = source.toString();
        for (String property : rootPath.propertyPath()) {
            text += "." + property;
        }
        if (text.isBlank()) {
            return null;
        }
        return new CommandCreationRoot(
                text,
                StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC,
                sourcePath.map(SourceParameterPath::parameter).orElse(null),
                propertyPath);
    }

    private boolean isSupportedLiteralExpression(Expression expression) {
        Expression unwrapped = unwrap(expression);
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
        Expression operand = unwrap(unary.getExpression());
        return operand.isIntegerLiteralExpr()
                || operand.isLongLiteralExpr()
                || operand.isDoubleLiteralExpr();
    }

    private boolean isSupportedSymbolicRootExpression(Expression expression) {
        Expression unwrapped = unwrap(expression);
        if (unwrapped.isNameExpr()) {
            try {
                unwrapped.asNameExpr().resolve();
                return true;
            } catch (Exception exception) {
                return false;
            }
        }
        if (unwrapped.isThisExpr()) {
            return true;
        }
        if (unwrapped.isFieldAccessExpr()) {
            try {
                unwrapped.asFieldAccessExpr().resolve();
                return isSupportedSymbolicRootExpression(unwrapped.asFieldAccessExpr().getScope());
            } catch (Exception exception) {
                return false;
            }
        }
        if (unwrapped.isMethodCallExpr()) {
            MethodCallExpr call = unwrapped.asMethodCallExpr();
            if (getterProperty(call.getNameAsString()) == null
                    || !call.getArguments().isEmpty()
                    || call.getScope().isEmpty()
                    || !isSupportedSymbolicRootExpression(call.getScope().orElseThrow())) {
                return false;
            }
            try {
                var resolved = call.resolve();
                return !resolved.isStatic() && resolved.getNumberOfParams() == 0;
            } catch (Exception exception) {
                return false;
            }
        }
        return false;
    }

    private String resolveCommandConstructorSignature(ObjectCreationExpr commandCreation) {
        try {
            return commandCreation.resolve().getQualifiedSignature();
        } catch (Exception exception) {
            return null;
        }
    }

    private Expression unwrap(Expression expression) {
        Expression result = expression;
        while (result.isEnclosedExpr()) {
            result = result.asEnclosedExpr().getInner();
        }
        return result;
    }

    private Optional<SourceParameterPath> directSourceParameterPath(Expression expression) {
        Expression unwrapped = unwrap(expression);
        if (unwrapped.isNameExpr()) {
            try {
                return unwrapped.asNameExpr().resolve().toAst()
                        .filter(Parameter.class::isInstance)
                        .map(Parameter.class::cast)
                        .map(parameter -> new SourceParameterPath(parameter, List.of()));
            } catch (Exception exception) {
                return Optional.empty();
            }
        }
        if (unwrapped.isMethodCallExpr()) {
            MethodCallExpr call = unwrapped.asMethodCallExpr();
            String property = getterProperty(call.getNameAsString());
            if (property == null || !call.getArguments().isEmpty() || call.getScope().isEmpty()) {
                return Optional.empty();
            }
            return directSourceParameterPath(call.getScope().orElseThrow())
                    .map(path -> path.append(property));
        }
        if (unwrapped.isFieldAccessExpr()) {
            var access = unwrapped.asFieldAccessExpr();
            return directSourceParameterPath(access.getScope())
                    .map(path -> path.append(access.getNameAsString()));
        }
        return Optional.empty();
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

    private Integer inferAggregateKeyConstructorArgumentIndex(
            ObjectCreationExpr commandCreation,
            Parameter aggregateParameter,
            ClassOrInterfaceDeclaration sagaDeclaration,
            ApplicationAnalysisState state) {

        Optional<ConstructorDeclaration> declaringConstructor = aggregateParameter.findAncestor(ConstructorDeclaration.class);
        MethodDeclaration declaringMethod = aggregateParameter.findAncestor(MethodDeclaration.class).orElse(null);
        int methodParameterIndex = declaringMethod == null
                ? -1
                : declaringMethod.getParameters().indexOf(aggregateParameter);
        if (declaringConstructor.isEmpty() && (declaringMethod == null || methodParameterIndex < 0)) {
            return null;
        }

        Set<Integer> entryIndexes = new LinkedHashSet<>();
        boolean foundApplicablePath = false;
        for (ConstructorDeclaration constructor : sagaDeclaration.getConstructors()) {
            ConstructorIndexResolution resolution = resolveConstructorEntryIndex(
                    constructor,
                    commandCreation,
                    declaringMethod,
                    methodParameterIndex,
                    state,
                    new LinkedHashSet<>());
            if (!resolution.applicable()) {
                continue;
            }
            foundApplicablePath = true;
            if (resolution.index() == null) {
                return null;
            }
            entryIndexes.add(resolution.index());
        }
        return foundApplicablePath && entryIndexes.size() == 1
                ? entryIndexes.iterator().next()
                : null;
    }

    private ConstructorIndexResolution resolveConstructorEntryIndex(
            ConstructorDeclaration constructor,
            ObjectCreationExpr commandCreation,
            MethodDeclaration declaringMethod,
            int methodParameterIndex,
            ApplicationAnalysisState state,
            Set<ConstructorDeclaration> activePath) {
        if (!activePath.add(constructor)) {
            return new ConstructorIndexResolution(true, null);
        }

        Set<Integer> indexes = new LinkedHashSet<>();
        boolean applicable = false;

        if (declaringMethod != null) {
            List<MethodCallExpr> exactCalls = constructor.findAll(MethodCallExpr.class).stream()
                    .filter(call -> resolvesTo(call, declaringMethod))
                    .toList();
            applicable = !exactCalls.isEmpty();
            for (MethodCallExpr call : exactCalls) {
                if (call.getArguments().size() <= methodParameterIndex) {
                    return new ConstructorIndexResolution(true, null);
                }
                Integer index = constructorParameterIndex(call.getArgument(methodParameterIndex), constructor);
                if (index == null) {
                    return new ConstructorIndexResolution(true, null);
                }
                indexes.add(index);
            }
        } else {
            List<ObjectCreationExpr> directCommands = constructor.findAll(ObjectCreationExpr.class).stream()
                    .filter(creation -> sameCommandPath(commandCreation, creation))
                    .toList();
            applicable = !directCommands.isEmpty();
            for (ObjectCreationExpr directCommand : directCommands) {
                Integer index = directConstructorAggregateKeyIndex(directCommand, state);
                if (index == null) {
                    return new ConstructorIndexResolution(true, null);
                }
                indexes.add(index);
            }
        }

        Optional<ExplicitConstructorInvocationStmt> delegation = constructor.getBody().getStatements().stream()
                .filter(statement -> statement.isExplicitConstructorInvocationStmt())
                .map(statement -> statement.asExplicitConstructorInvocationStmt())
                .filter(ExplicitConstructorInvocationStmt::isThis)
                .findFirst();
        if (delegation.isPresent()) {
            ConstructorDeclaration target = resolveDelegatedConstructor(delegation.get());
            if (target == null) {
                return new ConstructorIndexResolution(true, null);
            }
            ConstructorIndexResolution downstream = resolveConstructorEntryIndex(
                    target, commandCreation, declaringMethod, methodParameterIndex, state, activePath);
            if (downstream.applicable()) {
                applicable = true;
                if (downstream.index() == null
                        || delegation.get().getArguments().size() <= downstream.index()) {
                    return new ConstructorIndexResolution(true, null);
                }
                Integer delegatedIndex = constructorParameterIndex(
                        delegation.get().getArgument(downstream.index()), constructor);
                if (delegatedIndex == null) {
                    return new ConstructorIndexResolution(true, null);
                }
                indexes.add(delegatedIndex);
            }
        }

        activePath.remove(constructor);
        return !applicable
                ? new ConstructorIndexResolution(false, null)
                : new ConstructorIndexResolution(true, indexes.size() == 1 ? indexes.iterator().next() : null);
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

    private record ConstructorIndexResolution(boolean applicable, Integer index) { }

    private boolean resolvesTo(MethodCallExpr call, MethodDeclaration declaration) {
        try {
            return call.resolve().toAst()
                    .filter(MethodDeclaration.class::isInstance)
                    .map(MethodDeclaration.class::cast)
                    .map(resolved -> sameDeclaration(resolved, declaration))
                    .orElse(false);
        } catch (Exception exception) {
            return false;
        }
    }

    private Integer constructorParameterIndex(Expression argument,
                                              ConstructorDeclaration constructor) {
        if (!argument.isNameExpr()) {
            return null;
        }
        try {
            return argument.asNameExpr().resolve().toAst()
                    .filter(com.github.javaparser.ast.body.Parameter.class::isInstance)
                    .map(com.github.javaparser.ast.body.Parameter.class::cast)
                    .filter(constructor.getParameters()::contains)
                    .map(constructor.getParameters()::indexOf)
                    .filter(index -> index >= 0)
                    .orElse(null);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean sameCommandPath(ObjectCreationExpr expected, ObjectCreationExpr candidate) {
        if (!expected.getTypeAsString().equals(candidate.getTypeAsString())) {
            return false;
        }
        String expectedStep = enclosingStepName(expected);
        return expectedStep != null && expectedStep.equals(enclosingStepName(candidate));
    }

    private String enclosingStepName(ObjectCreationExpr commandCreation) {
        return commandCreation.findAncestor(ObjectCreationExpr.class)
                .filter(creation -> TypeUtils.isSubtypeOf(creation.getType(), SagaStep.class))
                .filter(creation -> !creation.getArguments().isEmpty())
                .flatMap(creation -> creation.getArgument(0).toStringLiteralExpr())
                .map(literal -> literal.getValue())
                .orElse(null);
    }

    private Integer directConstructorAggregateKeyIndex(ObjectCreationExpr commandCreation,
                                                       ApplicationAnalysisState state) {
        String commandTypeFqn;
        try {
            commandTypeFqn = commandCreation.getType().resolve().describe();
        } catch (Exception exception) {
            return null;
        }
        CommandCreationRoot root = resolveCommandCreationRoot(commandCreation, commandTypeFqn, state);
        if (root == null || root.sourceParameter() == null) {
            return null;
        }
        ConstructorDeclaration constructor = commandCreation.findAncestor(ConstructorDeclaration.class)
                .orElse(null);
        if (constructor == null || !constructor.getParameters().contains(root.sourceParameter())) {
            return null;
        }
        int index = constructor.getParameters().indexOf(root.sourceParameter());
        return index < 0 ? null : index;
    }

    private record CommandCreationRoot(
            String text,
            StepDispatchFootprint.AggregateKeyConfidence confidence,
            Parameter sourceParameter,
            List<String> propertyPath) {
        private CommandCreationRoot {
            propertyPath = propertyPath == null ? List.of() : List.copyOf(propertyPath);
        }
    }

    private record AggregateKeyResolution(
            String text,
            StepDispatchFootprint.AggregateKeyConfidence confidence,
            Integer sagaConstructorArgumentIndex,
            List<String> propertyPath) {
        private AggregateKeyResolution {
            propertyPath = propertyPath == null ? List.of() : List.copyOf(propertyPath);
        }

        private static AggregateKeyResolution unresolved() {
            return new AggregateKeyResolution(null, null, null, List.of());
        }
    }

    private record SourceParameterPath(Parameter parameter, List<String> propertyPath) {
        private SourceParameterPath {
            propertyPath = propertyPath == null ? List.of() : List.copyOf(propertyPath);
        }

        private SourceParameterPath append(String property) {
            List<String> path = new java.util.ArrayList<>(propertyPath);
            path.add(property);
            return new SourceParameterPath(parameter, path);
        }
    }

    private DispatchMultiplicity inferDispatchMultiplicity(ObjectCreationExpr creation) {
        int repeatCount = 1;
        boolean hasRepeatLoop = false;

        Node current = creation;
        while (true) {
            Optional<Node> parentOpt = current.getParentNode();
            if (parentOpt.isEmpty()) {
                return new DispatchMultiplicity(DispatchMultiplicityKind.PARAMETRIC_REPEAT, null);
            }

            Node parent = parentOpt.get();
            if (parent instanceof LambdaExpr) {
                if (isCollectionForEachLambda(parent)) {
                    return new DispatchMultiplicity(DispatchMultiplicityKind.PARAMETRIC_REPEAT, null);
                }

                return hasRepeatLoop
                        ? new DispatchMultiplicity(DispatchMultiplicityKind.STATIC_REPEAT, repeatCount)
                        : new DispatchMultiplicity(DispatchMultiplicityKind.SINGLE, 1);
            }

            if (parent instanceof ForEachStmt || parent instanceof WhileStmt || parent instanceof DoStmt) {
                return new DispatchMultiplicity(DispatchMultiplicityKind.PARAMETRIC_REPEAT, null);
            }

            if (parent instanceof ForStmt forStmt) {
                OptionalInt loopCount = inferStaticForLoopCount(forStmt);
                if (loopCount.isEmpty()) {
                    return new DispatchMultiplicity(DispatchMultiplicityKind.PARAMETRIC_REPEAT, null);
                }

                hasRepeatLoop = true;
                repeatCount *= loopCount.getAsInt();
            }

            current = parent;
        }
    }

    private boolean isCollectionForEachLambda(Node lambdaNode) {
        return lambdaNode.getParentNode()
                .filter(MethodCallExpr.class::isInstance)
                .map(MethodCallExpr.class::cast)
                .map(call -> call.getNameAsString().equals("forEach"))
                .orElse(false);
    }

    private OptionalInt inferStaticForLoopCount(ForStmt forStmt) {
        if (forStmt.getInitialization().size() != 1 || forStmt.getUpdate().size() != 1) {
            return OptionalInt.empty();
        }

        if (!(forStmt.getInitialization().get(0) instanceof com.github.javaparser.ast.expr.VariableDeclarationExpr initExpr)) {
            return OptionalInt.empty();
        }

        if (initExpr.getVariables().size() != 1) {
            return OptionalInt.empty();
        }

        VariableDeclarator loopVariable = initExpr.getVariable(0);
        OptionalInt startValue = parseIntegerValue(loopVariable.getInitializer().orElse(null));
        OptionalInt boundValue = parseStaticForBound(forStmt.getCompare().orElse(null), loopVariable.getNameAsString());
        OptionalInt stepValue = parseStaticForStep(forStmt.getUpdate().get(0), loopVariable.getNameAsString());

        if (startValue.isEmpty() || boundValue.isEmpty() || stepValue.isEmpty() || stepValue.getAsInt() != 1) {
            return OptionalInt.empty();
        }

        int start = startValue.getAsInt();
        int bound = boundValue.getAsInt();

        if (forStmt.getCompare().isEmpty()) {
            return OptionalInt.empty();
        }

        Expression compareExpr = forStmt.getCompare().get();
        if (!compareExpr.isBinaryExpr()) {
            return OptionalInt.empty();
        }

        var binaryExpr = compareExpr.asBinaryExpr();
        int repeatCount;
        switch (binaryExpr.getOperator()) {
            case LESS -> repeatCount = bound - start;
            case LESS_EQUALS -> repeatCount = bound - start + 1;
            default -> {
                return OptionalInt.empty();
            }
        }

        if (repeatCount < 0) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(repeatCount);
    }

    private OptionalInt parseStaticForBound(Expression compareExpr, String loopVariableName) {
        if (compareExpr == null || !compareExpr.isBinaryExpr()) {
            return OptionalInt.empty();
        }

        var binaryExpr = compareExpr.asBinaryExpr();
        if (!binaryExpr.getLeft().isNameExpr() ||
                !binaryExpr.getLeft().asNameExpr().getNameAsString().equals(loopVariableName)) {
            return OptionalInt.empty();
        }

        return parseIntegerValue(binaryExpr.getRight());
    }

    private OptionalInt parseStaticForStep(Expression updateExpr, String loopVariableName) {
        if (updateExpr.isUnaryExpr()) {
            var unaryExpr = updateExpr.asUnaryExpr();
            if (unaryExpr.getExpression().isNameExpr() &&
                    unaryExpr.getExpression().asNameExpr().getNameAsString().equals(loopVariableName) &&
                    (unaryExpr.getOperator() == UnaryExpr.Operator.POSTFIX_INCREMENT
                            || unaryExpr.getOperator() == UnaryExpr.Operator.PREFIX_INCREMENT)) {
                return OptionalInt.of(1);
            }
        }

        if (updateExpr.isAssignExpr()) {
            var assignExpr = updateExpr.asAssignExpr();
            if (assignExpr.getTarget().isNameExpr() &&
                    assignExpr.getTarget().asNameExpr().getNameAsString().equals(loopVariableName)) {
                if (assignExpr.getOperator() == com.github.javaparser.ast.expr.AssignExpr.Operator.PLUS) {
                    return parseIntegerValue(assignExpr.getValue());
                }
            }
        }

        return OptionalInt.empty();
    }

    private OptionalInt parseIntegerValue(Expression expression) {
        if (expression == null) {
            return OptionalInt.empty();
        }

        if (expression.isIntegerLiteralExpr()) {
            return OptionalInt.of(Integer.parseInt(expression.asIntegerLiteralExpr().getValue()));
        }

        if (expression.isUnaryExpr()) {
            UnaryExpr unaryExpr = expression.asUnaryExpr();
            OptionalInt nested = parseIntegerValue(unaryExpr.getExpression());
            if (nested.isEmpty()) {
                return OptionalInt.empty();
            }

            return switch (unaryExpr.getOperator()) {
                case PLUS -> nested;
                case MINUS -> OptionalInt.of(-nested.getAsInt());
                default -> OptionalInt.empty();
            };
        }

        if (expression.isEnclosedExpr()) {
            return parseIntegerValue(expression.asEnclosedExpr().getInner());
        }

        return OptionalInt.empty();
    }

    private void extractStepCompensations(ObjectCreationExpr currentStepExpr,
                                          SagaStepBuildingBlock stepBlock,
                                          ApplicationAnalysisState state,
                                          String stepKey,
                                          Map<String, String> stepVariableToStepKey) {
        currentStepExpr.findAncestor(BlockStmt.class).ifPresent(block ->
                currentStepExpr.findAncestor(VariableDeclarator.class).ifPresent(variableDeclarator -> {
                    String stepVariable = variableDeclarator.getNameAsString();
                    String resolvedStepKey = stepVariableToStepKey.get(stepVariable);
                    if (!stepKey.equals(resolvedStepKey)) {
                        return;
                    }

                    block.findAll(MethodCallExpr.class).stream()
                            .filter(call -> call.getNameAsString().equals("registerCompensation"))
                            .filter(call -> call.getScope()
                                    .filter(Expression::isNameExpr)
                                    .map(Expression::asNameExpr)
                                    .map(NameExpr::getNameAsString)
                                    .filter(stepVariable::equals)
                                    .isPresent())
                            .forEach(call -> {
                                stepBlock.markCompensationRegistered();
                                if (call.getArguments().isEmpty()) {
                                    stepBlock.markDispatchAnalysisIncomplete(
                                            DispatchPhase.COMPENSATION,
                                            "MISSING_COMPENSATION_OPERATION",
                                            "registerCompensation has no operation argument");
                                    return;
                                }

                                extractStepFootprints(call.getArgument(0), stepBlock, state,
                                        currentStepExpr.findAncestor(ClassOrInterfaceDeclaration.class).orElse(null),
                                        stepKey, DispatchPhase.COMPENSATION);
                            });
                }));
    }

    private record GenericCompensationTarget(
            String aggregateName,
            AggregateKeyResolution aggregateKey) {
    }
}
