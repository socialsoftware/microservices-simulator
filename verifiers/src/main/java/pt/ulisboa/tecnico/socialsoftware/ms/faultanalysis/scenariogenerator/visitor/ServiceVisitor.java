package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.ServiceBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Analyses @Service classes to extract public method access policies (READ/WRITE).
 * <p>
 * A method is classified as WRITE if its body contains a call to
 * unitOfWorkService.registerChanged(...), otherwise READ.
 */
public class ServiceVisitor extends VoidVisitorAdapter<ApplicationAnalysisContext> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisContext context) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            // Only process classes explicitly marked with @Service annotation
            if (!decl.isAnnotationPresent("Service")) {
                return;
            }

            String packageName = decl.resolve().getPackageName();
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);
            String className = decl.getNameAsString();

            // Collect all UnitOfWorkService field/parameter names for registerChanged detection
            Set<String> uowFieldNames = collectUnitOfWorkServiceNames(decl);

            ServiceBuildingBlock serviceBB = new ServiceBuildingBlock(filePath, packageName, className);

            // Analyze each public non-static method and classify by access policy
            classifyPublicMethods(decl, serviceBB, uowFieldNames);

            context.services.add(serviceBB);
            logger.info("Service {}: {}", className, serviceBB.getMethodAccessPolicies());
        });
    }

    /**
     * Collects field and constructor parameter names that refer to UnitOfWorkService types.
     * These names are used to detect registerChanged(...) calls.
     */
    private Set<String> collectUnitOfWorkServiceNames(ClassOrInterfaceDeclaration decl) {
        Set<String> uowFieldNames = new HashSet<>();

        // Scan fields for UnitOfWorkService types
        decl.getFields().forEach(field -> {
            if (field.getCommonType().asString().contains("UnitOfWorkService")) {
                field.getVariables().forEach(var -> uowFieldNames.add(var.getNameAsString()));
            }
        });

        // Scan constructor parameters for UnitOfWorkService types
        decl.getConstructors().forEach(ctor ->
                ctor.getParameters().forEach(param -> {
                    if (param.getTypeAsString().contains("UnitOfWorkService")) {
                        uowFieldNames.add(param.getNameAsString());
                    }
                })
        );

        return uowFieldNames;
    }

    /**
     * Classifies each public non-static method as WRITE (if it calls registerChanged) or READ.
     */
    private void classifyPublicMethods(ClassOrInterfaceDeclaration decl, ServiceBuildingBlock serviceBB,
                                       Set<String> uowFieldNames) {
        decl.getMethods().stream()
                .filter(m -> m.isPublic() && !m.isStatic())
                .forEach(m -> {
                    boolean isWrite = detectRegisterChangedCall(m, uowFieldNames);
                    serviceBB.addMethod(m.getNameAsString(),
                            isWrite ? AccessPolicy.WRITE : AccessPolicy.READ);
                });
    }

    /**
     * Detects if the method body contains a call to unitOfWorkService.registerChanged(...).
     */
    private boolean detectRegisterChangedCall(MethodDeclaration method,
                                              Set<String> uowFieldNames) {
        return method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> isRegisterChangedCall(call, uowFieldNames));
    }

    /**
     * Checks if a method call is a registerChanged call on a UnitOfWorkService field.
     */
    private boolean isRegisterChangedCall(MethodCallExpr call, Set<String> uowFieldNames) {
        if (!call.getNameAsString().equals("registerChanged")) {
            return false;
        }

        return call.getScope()
                .map(scope -> {
                    if (scope.isNameExpr()) {
                        return uowFieldNames.contains(scope.asNameExpr().getNameAsString());
                    }
                    if (scope.isFieldAccessExpr()) {
                        return uowFieldNames.contains(scope.asFieldAccessExpr().getNameAsString());
                    }
                    return false;
                })
                .orElse(false);
    }
}
