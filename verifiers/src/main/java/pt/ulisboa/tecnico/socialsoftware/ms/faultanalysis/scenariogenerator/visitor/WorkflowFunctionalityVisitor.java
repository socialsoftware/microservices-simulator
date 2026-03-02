package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaWorkflowFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.util.TypeUtils;

import java.nio.file.Path;

/**
 * Pass 3: Analyzes WorkflowFunctionality subclasses to extract saga steps and link them to StepFootprints.
 * <p>
 * Strategy:
 * 1. Filter for WorkflowFunctionality subclasses that declare a SagaUnitOfWorkService in a constructor.
 * 2. For each new SagaStep(...) expression, extract the step name and lambda body.
 * 3. In the lambda body, find all new *Command(...) expressions.
 * 4. Resolve each command type to a CommandDispatchInfo via context.resolveCommand().
 * 5. Create StepFootprints linking each step to the aggregates it touches (READ/WRITE).
 */
public class WorkflowFunctionalityVisitor extends VoidVisitorAdapter<ApplicationAnalysisContext> {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisContext context) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            // Only process WorkflowFunctionality subclasses with SagaUnitOfWorkService
            if (!TypeUtils.isSubtypeOf(decl, WorkflowFunctionality.class) ||
                    !declaresUnitOfWorkService(decl)) {
                return;
            }

            String packageName = decl.resolve().getPackageName();
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);
            String sagaClassName = decl.getNameAsString();

            SagaWorkflowFunctionalityBuildingBlock sagaBlock =
                    new SagaWorkflowFunctionalityBuildingBlock(filePath, packageName, sagaClassName);

            // Extract all saga steps from new SagaStep(...) expressions
            extractSagaSteps(decl, context, filePath, packageName, sagaClassName, sagaBlock);

            context.sagas.add(sagaBlock);
            logger.info("Saga {}: {} steps", sagaClassName, sagaBlock.getSteps().size());
        });
    }

    /**
     * Checks if the class declares a SagaUnitOfWorkService in any constructor parameter.
     */
    private boolean declaresUnitOfWorkService(ClassOrInterfaceDeclaration decl) {
        return decl.getConstructors().stream()
                .anyMatch(ctor -> ctor.getParameters().stream()
                        .anyMatch(p -> p.getTypeAsString().contains("SagaUnitOfWorkService")));
    }

    /**
     * Extracts all saga steps from new SagaStep(...) expressions in the class body.
     */
    private void extractSagaSteps(ClassOrInterfaceDeclaration decl, ApplicationAnalysisContext context,
                                  Path filePath, String packageName, String sagaClassName,
                                  SagaWorkflowFunctionalityBuildingBlock sagaBlock) {
        decl.findAll(ObjectCreationExpr.class).forEach(expr -> {
            // Filter for new SagaStep(...) with at least 2 arguments (name and lambda)
            if (!expr.getType().asString().equals("SagaStep") || expr.getArguments().size() < 2) {
                return;
            }

            // First argument: step name (StringLiteralExpr)
            expr.getArgument(0).ifStringLiteralExpr(literal -> {
                String stepName = literal.getValue();
                String stepKey = sagaClassName + "::" + stepName;

                SagaStepBuildingBlock stepBlock =
                        new SagaStepBuildingBlock(filePath, packageName, stepKey, expr);

                // Second argument: lambda body containing command dispatches
                Expression lambdaArg = expr.getArgument(1);

                // Extract command footprints from lambda body
                extractStepFootprints(lambdaArg, stepBlock, context, stepKey);

                sagaBlock.addStep(stepBlock);
                context.steps.add(stepBlock);
                logger.info("  Step {}: {} footprints", stepKey, stepBlock.getStepFootprints().size());
            });
        });
    }

    /**
     * Extracts StepFootprints from all new *Command(...) expressions in the lambda/method reference body.
     */
    private void extractStepFootprints(Expression lambdaArg, SagaStepBuildingBlock stepBlock,
                                       ApplicationAnalysisContext context, String stepKey) {
        lambdaArg.ifLambdaExpr(lambda ->
                lambda.findAll(ObjectCreationExpr.class).forEach(creation -> {
                    String typeName = creation.getType().getNameAsString();
                    if (!typeName.endsWith("Command")) {
                        return;
                    }

                    // Resolve command type to dispatch info
                    context.resolveCommand(typeName).ifPresentOrElse(
                            info -> {
                                StepFootprint footprint =
                                        new StepFootprint(stepBlock, info.aggregateName(), info.accessPolicy());
                                stepBlock.addStepFootprint(footprint);
                            },
                            () -> logger.warn("Command not found in registry: {} (step: {})", typeName, stepKey)
                    );
                })
        );
    }
}
