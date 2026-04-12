package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

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
                extractStepFootprints(lambdaArg, stepBlock, state, stepKey, DispatchPhase.FORWARD);

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
    private void extractStepFootprints(Expression lambdaArg, SagaStepBuildingBlock stepBlock,
                                       ApplicationAnalysisState state, String stepKey, DispatchPhase phase) {
        lambdaArg.ifLambdaExpr(lambda ->
                lambda.findAll(ObjectCreationExpr.class).forEach(creation -> {
                    if (!TypeUtils.isSubtypeOf(creation.getType(), Command.class)) {
                        return;
                    }

                    // Resolve command type to dispatch info
                    state.getCommandDispatchInfo(creation.getType()).ifPresentOrElse(
                            info -> {
                                String commandTypeFqn = creation.getType().resolve().describe();
                                StepDispatchFootprint dispatch = new StepDispatchFootprint(
                                        stepKey,
                                        commandTypeFqn,
                                        info.aggregateName(),
                                        info.accessPolicy(),
                                        phase,
                                        inferDispatchMultiplicity(creation));
                                stepBlock.addDispatch(dispatch);
                            },
                            () -> logger.warn("Command not found in registry: {} (step: {})",
                                    creation.getType().asString(), stepKey)
                    );
                })
        );
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
                            .reduce((first, second) -> second)
                            .ifPresent(call -> {
                                if (call.getArguments().isEmpty()) {
                                    return;
                                }

                                extractStepFootprints(call.getArgument(0), stepBlock, state, stepKey,
                                        DispatchPhase.COMPENSATION);
                            });
                }));
    }
}
