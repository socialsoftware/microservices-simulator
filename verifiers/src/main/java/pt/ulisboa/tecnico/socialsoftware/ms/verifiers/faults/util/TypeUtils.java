package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import com.github.javaparser.resolution.types.ResolvedType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public final class TypeUtils {
    private static final Logger logger = LoggerFactory.getLogger(TypeUtils.class);

    public static boolean isSubclassOf(ClassOrInterfaceDeclaration decl, Class<?> target) {
        String targetFqn = target.getName();
        try {
            ResolvedReferenceTypeDeclaration resolvedDecl = decl.resolve();
            if (resolvedDecl.getQualifiedName().equals(targetFqn)) return true;
            return resolvedDecl.getAllAncestors().stream()
                    .flatMap(a -> a.getTypeDeclaration().stream())
                    .anyMatch(aDecl -> aDecl.getQualifiedName().equals(targetFqn));
        } catch (Exception e) {
            logger.warn("Could not resolve type for {} (checking against {}): {}",
                    decl.getNameAsString(), targetFqn, e.toString());
            return false;
        }
    }

    public static boolean isSubtypeOf(Type type, Class<?> target) {
        String targetFqn = target.getName();
        try {
            var resolved = type.resolve();
            return isResolvedSubtypeOf(resolved, targetFqn);
        } catch (Exception e) {
            logger.debug("Could not resolve type '{}' (checking against {}): {}",
                    type.asString(), targetFqn, e.toString());
            return false;
        }
    }

    public static boolean isResolvedSubtypeOf(ResolvedType resolved, Class<?> target) {
        return isResolvedSubtypeOf(resolved, target.getName());
    }

    /**
     * Builds a FQN-signature key for a method declaration.
     * Format: methodName(FQN1,FQN2,...) — fully-qualified parameter types.
     * Falls back to the declared type string if the symbol solver cannot resolve a parameter.
     */
    public static String buildSignature(MethodDeclaration m) {
        String params = m.getParameters().stream()
                .map(p -> {
                    try {
                        return p.getType().resolve().describe();
                    } catch (Exception e) {
                        logger.debug("Could not resolve param type '{}': {}", p.getTypeAsString(), e.getMessage());
                        return p.getTypeAsString();
                    }
                })
                .collect(Collectors.joining(","));
        return m.getNameAsString() + "(" + params + ")";
    }

    /**
     * Builds a FQN-signature key from a method call expression.
     * Format: methodName(FQN1,FQN2,...) — fully-qualified argument types resolved at the call site.
     * Falls back to the argument's source text if resolution fails.
     * Must produce the same key as {@link #buildSignature} for matching lookups to succeed.
     */
    public static String buildCallSignature(MethodCallExpr call) {
        String params = call.getArguments().stream()
                .map(arg -> {
                    try {
                        return arg.calculateResolvedType().describe();
                    } catch (Exception e) {
                        logger.debug("Could not resolve arg type for '{}': {}", arg, e.getMessage());
                        return arg.toString();
                    }
                })
                .collect(Collectors.joining(","));
        return call.getNameAsString() + "(" + params + ")";
    }

    private static boolean isResolvedSubtypeOf(ResolvedType resolved, String targetFqn) {
        if (resolved == null || !resolved.isReferenceType()) {
            return false;
        }

        ResolvedReferenceTypeDeclaration resolvedType =
                resolved.asReferenceType().getTypeDeclaration().orElse(null);
        if (resolvedType == null) {
            return false;
        }

        if (resolvedType.getQualifiedName().equals(targetFqn)) {
            return true;
        }

        return resolvedType.getAllAncestors().stream()
                .flatMap(a -> a.getTypeDeclaration().stream())
                .anyMatch(aDecl -> aDecl.getQualifiedName().equals(targetFqn));
    }
}
