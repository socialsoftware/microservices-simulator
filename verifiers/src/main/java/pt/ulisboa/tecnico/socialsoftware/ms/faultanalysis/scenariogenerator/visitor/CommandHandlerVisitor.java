package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.CommandDispatchInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.CommandHandlerBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.ServiceBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.util.TypeUtils;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Analyses CommandHandler subclasses to extract command dispatch mappings.
 * <p>
 * Phase A: Collect injected service fields (both @Autowired field and constructor injection).
 * <p>
 * Phase B: Extract the aggregate type name from getAggregateTypeName() method.
 * <p>
 * Phase C: For each private handler method with a Command parameter, find the first service call
 *          and resolve it to a CommandDispatchInfo using the service's access policy.
 */
public class CommandHandlerVisitor extends VoidVisitorAdapter<ApplicationAnalysisContext> {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandlerVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisContext context) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            // Only process CommandHandler subclasses
            if (!TypeUtils.isSubtypeOf(decl, CommandHandler.class)) {
                return;
            }

            String packageName = decl.resolve().getPackageName();
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);

            // Phase A: Build map of injected service fields
            Map<String, ServiceBuildingBlock> serviceFields = resolveServiceFields(decl, context);

            // Phase B: Extract aggregate type name
            Optional<String> aggregateTypeName = extractAggregateTypeName(decl);

            CommandHandlerBuildingBlock block = new CommandHandlerBuildingBlock(
                    filePath, packageName, decl.getNameAsString(), aggregateTypeName.orElse(null));

            // Phase C: Map command types to CommandDispatchInfo
            mapCommandsToDispatchInfo(decl, block, serviceFields, aggregateTypeName.orElse(null));

            context.commandHandlers.add(block);
            logger.info("CommandHandler {}: {}", decl.getNameAsString(), block.getCommandDispatch());
        });
    }

    /**
     * Phase A: Resolves service fields from both field injection (@Autowired) and constructor injection.
     */
    private Map<String, ServiceBuildingBlock> resolveServiceFields(ClassOrInterfaceDeclaration decl,
                                                                    ApplicationAnalysisContext context) {
        // Build a type-name-to-service map for lookup
        Map<String, ServiceBuildingBlock> serviceTypeMap = new LinkedHashMap<>();
        context.services.forEach(s -> serviceTypeMap.put(s.getName(), s));

        Map<String, ServiceBuildingBlock> serviceFields = new LinkedHashMap<>();

        // Scan field injection
        decl.getFields().forEach(field -> {
            String typeName = field.getCommonType().asString();
            if (serviceTypeMap.containsKey(typeName)) {
                ServiceBuildingBlock serviceBB = serviceTypeMap.get(typeName);
                field.getVariables().forEach(var -> serviceFields.put(var.getNameAsString(), serviceBB));
            }
        });

        // Scan constructor injection
        decl.getConstructors().forEach(ctor ->
                ctor.getParameters().forEach(param -> {
                    String typeName = param.getTypeAsString();
                    ServiceBuildingBlock serviceBB = serviceTypeMap.get(typeName);
                    if (serviceBB != null) {
                        String paramName = param.getNameAsString();
                        ctor.findAll(AssignExpr.class).forEach(assign -> {
                            assign.getValue().ifNameExpr(nameExpr -> {
                                if (nameExpr.getNameAsString().equals(paramName)) {
                                    extractAssignedFieldName(assign.getTarget())
                                            .ifPresent(fieldName -> serviceFields.put(fieldName, serviceBB));
                                }
                            });
                        });
                    }
                })
        );

        return serviceFields;
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
     */
    private Optional<String> extractAggregateTypeName(ClassOrInterfaceDeclaration decl) {
        return decl.getMethods().stream()
                .filter(m -> m.getNameAsString().equals("getAggregateTypeName"))
                .findFirst()
                .flatMap(m -> m.findFirst(ReturnStmt.class))
                .flatMap(ReturnStmt::getExpression)
                .filter(Expression::isStringLiteralExpr)
                .map(expr -> expr.asStringLiteralExpr().asString());
    }

    /**
     * Phase C: For each private handler method with a Command parameter, map the command type
     * to its dispatch info by finding the first service call in the method body.
     */
    private void mapCommandsToDispatchInfo(ClassOrInterfaceDeclaration decl, CommandHandlerBuildingBlock block,
                                           Map<String, ServiceBuildingBlock> serviceFields,
                                           String aggregateName) {
        decl.getMethods().stream()
                .filter(MethodDeclaration::isPrivate)
                .forEach(method -> {
                    method.getParameters().stream()
                            .filter(p -> p.getTypeAsString().endsWith("Command"))
                            .findFirst()
                            .ifPresent(commandParam -> {
                                String commandType = commandParam.getTypeAsString();
                                findFirstServiceCall(method, serviceFields)
                                        .ifPresent(call -> {
                                            String fieldName = call.getScope().get().asNameExpr().getNameAsString();
                                            ServiceBuildingBlock serviceBB = serviceFields.get(fieldName);
                                            String serviceMethodName = call.getNameAsString();

                                            block.addCommandDispatch(commandType,
                                                    new CommandDispatchInfo(
                                                            serviceBB.getName(),
                                                            serviceMethodName,
                                                            serviceBB.getAccessPolicy(serviceMethodName),
                                                            aggregateName));
                                        });
                            });
                });
    }

    /**
     * Finds the first method call in the method body whose scope is a known service field.
     */
    private Optional<MethodCallExpr> findFirstServiceCall(MethodDeclaration method,
                                                          Map<String, ServiceBuildingBlock> serviceFields) {
        return method.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope()
                        .filter(scope -> scope.isNameExpr() &&
                                serviceFields.containsKey(scope.asNameExpr().getNameAsString()))
                        .isPresent())
                .findFirst();
    }
}
