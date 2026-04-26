package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.ServiceBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Analyses @Service classes to extract public method access policies (READ/WRITE).
 * <p>
 * A method is classified as WRITE if its body contains a call to
 * unitOfWorkService.registerChanged(...), a local alias of the unit-of-work
 * service, a trivial getter that returns it, or one local helper method that
 * wraps registerChanged(...). Otherwise it is READ.
 */
public class ServiceVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(ServiceVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            // Only process classes explicitly marked with @Service annotation
            if (!decl.isAnnotationPresent("Service")) {
                return;
            }

            // Only process classes that inject UnitOfWorkService via a constructor parameter
            // Or classes that have an @Autowired UnitOfWorkService field
            if (!hasUnitOfWorkServiceConstructorParam(decl) && !hasUnitOfWorkServiceAutowiredField(decl)) {
                return;
            }

            // Only admit classes that are command-handler dispatch targets.
            // Skip this guard when the index has not been run (dispatchTargetFqns empty) so that
            // ServiceVisitor can still be used in isolation for write-detection tests or scripts.
            if (!state.dispatchTargetFqns.isEmpty() && !isDispatchTarget(decl, state)) {
                logger.debug("Skipping {} — not a command-handler dispatch target", decl.getNameAsString());
                return;
            }

            String packageName = decl.resolve().getPackageName();
            Path filePath = cu.getStorage().map(CompilationUnit.Storage::getPath).orElse(null);
            String className = decl.getFullyQualifiedName().orElseGet(() -> {
                String name = decl.getNameAsString();
                logger.warn("Could not get fully qualified name of class {}", name);
                return name;
            });

            // Collect all UnitOfWorkService field/parameter names for registerChanged detection
            Set<String> uowFieldNames = collectUnitOfWorkServiceNames(decl);
            Set<String> uowGetterNames = collectUnitOfWorkServiceGetterNames(decl, uowFieldNames);
            Set<String> directWriteMethodNames = collectDirectWriteMethodNames(decl, uowFieldNames, uowGetterNames);

            ServiceBuildingBlock serviceBB = new ServiceBuildingBlock(filePath, packageName, className);

            // Analyze each public non-static method and classify by access policy
            classifyPublicMethods(decl, serviceBB, uowFieldNames, uowGetterNames, directWriteMethodNames);

            state.services.add(serviceBB);

            // Populate interface-to-service index for each implemented interface
            decl.getImplementedTypes().forEach(impl -> {
                try {
                    String ifaceFqn = impl.resolve().asReferenceType().getQualifiedName();
                    state.interfaceToServices
                            .computeIfAbsent(ifaceFqn, k -> new ArrayList<>())
                            .add(serviceBB);
                } catch (Exception e) {
                    logger.debug("Could not resolve implemented interface '{}': {}", impl.asString(), e.getMessage());
                }
            });

            logger.info("Service {}: {}", className, serviceBB.getMethodAccessPolicies());
        });
    }

    private boolean hasUnitOfWorkServiceAutowiredField(ClassOrInterfaceDeclaration decl) {
        return decl.getFields().stream()
                .anyMatch(fieldDecl ->
                        fieldDecl.getAnnotationByClass(Autowired.class).isPresent() &&
                        isUnitOfWorkServiceType(fieldDecl.getCommonType()));
    }

    /**
     * Returns true if the class's own FQN appears in state.dispatchTargetFqns, or if it is
     * the only @Service implementation of an interface injected by a command handler.
     */
    private boolean isDispatchTarget(ClassOrInterfaceDeclaration decl, ApplicationAnalysisState state) {
        try {
            if (state.dispatchTargetFqns.contains(decl.resolve().getQualifiedName())) {
                return true;
            }

            return decl.getImplementedTypes().stream().anyMatch(impl -> isSingleImplementationDispatchInterface(impl, state));
        } catch (Exception e) {
            logger.debug("Could not resolve FQN for dispatch-target check of {}: {}",
                    decl.getNameAsString(), e.getMessage());
            return false;
        }
    }

    private boolean isSingleImplementationDispatchInterface(Type implementedType, ApplicationAnalysisState state) {
        try {
            String ifaceFqn = implementedType.resolve().asReferenceType().getQualifiedName();
            return state.dispatchTargetInterfaceFqns.contains(ifaceFqn)
                    && state.serviceImplementationCountsByInterface.getOrDefault(ifaceFqn, 0) == 1;
        } catch (Exception e) {
            logger.debug("Could not resolve implemented interface '{}': {}", implementedType.asString(), e.getMessage());
            return false;
        }
    }

    /**
     * Returns true if any constructor declares a parameter whose type contains "UnitOfWorkService".
     */
    private boolean hasUnitOfWorkServiceConstructorParam(ClassOrInterfaceDeclaration decl) {
        return decl.getConstructors().stream()
                .anyMatch(ctor -> ctor.getParameters().stream()
                        .anyMatch(param -> isUnitOfWorkServiceType(param.getType())));
    }

    /**
     * Collects field and constructor parameter names that refer to UnitOfWorkService types.
     * These names are used to detect registerChanged(...) calls.
     */
    private Set<String> collectUnitOfWorkServiceNames(ClassOrInterfaceDeclaration decl) {
        Set<String> uowFieldNames = new HashSet<>();

        // Scan fields for UnitOfWorkService types
        decl.getFields().forEach(field -> {
            if (isUnitOfWorkServiceType(field.getCommonType())) {
                field.getVariables().forEach(var -> uowFieldNames.add(var.getNameAsString()));
            }
        });

        // Scan constructor parameters for UnitOfWorkService types
        decl.getConstructors().forEach(ctor ->
                ctor.getParameters().forEach(param -> {
                    if (isUnitOfWorkServiceType(param.getType())) {
                        uowFieldNames.add(param.getNameAsString());
                    }
                })
        );

        return uowFieldNames;
    }

    /**
     * Collects local getter methods that trivially return a tracked UnitOfWorkService reference.
     */
    private Set<String> collectUnitOfWorkServiceGetterNames(ClassOrInterfaceDeclaration decl,
                                                            Set<String> uowFieldNames) {
        Set<String> getterNames = new HashSet<>();

        decl.getMethods().forEach(method -> {
            if (!method.getParameters().isEmpty()) {
                return;
            }

            method.findFirst(ReturnStmt.class)
                    .map(ReturnStmt::getExpression)
                    .flatMap(expression -> expression)
                    .filter(expr -> isTrackedUnitOfWorkServiceReference(expr, uowFieldNames,
                            Set.<String>of(), Set.<String>of()))
                    .ifPresent(expr -> getterNames.add(method.getNameAsString()));
        });

        return getterNames;
    }

    /**
     * Collects methods that directly contain a registerChanged(...) call on a tracked
     * UnitOfWorkService reference or a local alias of it.
     */
    private Set<String> collectDirectWriteMethodNames(ClassOrInterfaceDeclaration decl,
                                                      Set<String> uowFieldNames,
                                                      Set<String> uowGetterNames) {
        Set<String> directWriteMethodNames = new HashSet<>();

        decl.getMethods().forEach(method -> {
            Set<String> localAliases = collectLocalUnitOfWorkServiceAliases(method, uowFieldNames, uowGetterNames);
            if (containsRegisterChangedCall(method, uowFieldNames, uowGetterNames, localAliases)) {
                directWriteMethodNames.add(TypeUtils.buildSignature(method));
            }
        });

        return directWriteMethodNames;
    }

    /**
     * Collects method-local aliases that point to a tracked UnitOfWorkService reference.
     * This supports simple patterns such as {@code var tracker = unitOfWorkService; }.
     */
    private Set<String> collectLocalUnitOfWorkServiceAliases(MethodDeclaration method,
                                                             Set<String> uowFieldNames,
                                                             Set<String> uowGetterNames) {
        Set<String> aliases = new HashSet<>();

        boolean changed;
        do {
            changed = false;
            for (VariableDeclarator variable : method.findAll(VariableDeclarator.class)) {
                if (variable.getInitializer()
                        .filter(initializer -> isTrackedUnitOfWorkServiceReference(initializer, uowFieldNames, uowGetterNames, aliases))
                        .isPresent()) {
                    changed |= aliases.add(variable.getNameAsString());
                }
            }
        } while (changed);

        return aliases;
    }

    private boolean isUnitOfWorkServiceType(Type type) {
        return TypeUtils.isSubtypeOf(type, UnitOfWorkService.class);
    }

    /**
     * Classifies each public non-static method as WRITE (if it calls registerChanged) or READ.
     */
    private void classifyPublicMethods(ClassOrInterfaceDeclaration decl, ServiceBuildingBlock serviceBB,
                                       Set<String> uowFieldNames,
                                       Set<String> uowGetterNames,
                                       Set<String> directWriteMethodNames) {
        decl.getMethods().stream()
                .filter(m -> m.isPublic() && !m.isStatic())
                .forEach(m -> {
                    boolean isWrite = directWriteMethodNames.contains(TypeUtils.buildSignature(m))
                            || callsKnownWriteHelper(m, directWriteMethodNames);
                    serviceBB.addMethod(TypeUtils.buildSignature(m),
                            isWrite ? AccessPolicy.WRITE : AccessPolicy.READ);
                });
    }

    /**
     * Detects if the method body contains a call to unitOfWorkService.registerChanged(...)
     * through a direct reference, local alias, or getter call.
     */
    private boolean containsRegisterChangedCall(MethodDeclaration method,
                                                Set<String> uowFieldNames,
                                                Set<String> uowGetterNames,
                                                Set<String> localAliases) {
        return method.findAll(MethodCallExpr.class).stream()
                .anyMatch(call -> isRegisterChangedCall(call, uowFieldNames, uowGetterNames, localAliases));
    }

    /**
     * Checks if a method call is a registerChanged call on a UnitOfWorkService field.
     */
    private boolean isRegisterChangedCall(MethodCallExpr call,
                                          Set<String> uowFieldNames,
                                          Set<String> uowGetterNames,
                                          Set<String> localAliases) {
        if (!call.getNameAsString().equals("registerChanged")) {
            return false;
        }

        return call.getScope()
                .map(scope -> {
                    return isTrackedUnitOfWorkServiceReference(scope, uowFieldNames, uowGetterNames, localAliases);
                })
                .orElse(false);
    }

    /**
     * Returns true if the expression resolves to the tracked UnitOfWorkService reference,
     * a local alias of it, or a trivial getter call that returns it.
     */
    private boolean isTrackedUnitOfWorkServiceReference(Expression expr,
                                                        Set<String> uowFieldNames,
                                                        Set<String> uowGetterNames,
                                                        Set<String> localAliases) {
        if (expr.isNameExpr()) {
            String name = expr.asNameExpr().getNameAsString();
            return uowFieldNames.contains(name) || localAliases.contains(name);
        }

        if (expr.isFieldAccessExpr()) {
            return uowFieldNames.contains(expr.asFieldAccessExpr().getNameAsString());
        }

        if (expr.isMethodCallExpr()) {
            MethodCallExpr call = expr.asMethodCallExpr();
            if (!uowGetterNames.contains(call.getNameAsString()) || !call.getArguments().isEmpty()) {
                return false;
            }
            return call.getScope()
                    .map(scope -> scope.isThisExpr() || scope.isNameExpr() || scope.isFieldAccessExpr())
                    .orElse(true);
        }

        return false;
    }

    /**
     * Returns true if the method calls a helper method known to be write-bearing.
     * This intentionally stays one level deep.
     */
    private boolean callsKnownWriteHelper(MethodDeclaration method, Set<String> directWriteMethodNames) {
        return method.findAll(MethodCallExpr.class).stream()
                .filter(call -> call.getScope()
                        .map(scope -> scope.isThisExpr() || scope.isNameExpr())
                        .orElse(true))
                .map(call -> TypeUtils.buildCallSignature(call))
                .anyMatch(directWriteMethodNames::contains);
    }
}
