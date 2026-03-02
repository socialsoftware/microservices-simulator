package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.util;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for type-checking operations using JavaParser's symbol resolver.
 */
public class TypeUtils {
    private static final Logger logger = LoggerFactory.getLogger(TypeUtils.class);

    /**
     * Checks if a class declaration is a subtype of a target class.
     * Uses the symbol resolver to check the type hierarchy, handling resolution failures gracefully.
     *
     * @param decl the class declaration to check
     * @param target the target class to check against
     * @return true if decl is the same as or a subtype of target; false otherwise
     */
    public static boolean isSubtypeOf(ClassOrInterfaceDeclaration decl, Class<?> target) {
        String targetFqn = target.getName();

        try {
            ResolvedReferenceTypeDeclaration resolvedDecl = decl.resolve();

            if (resolvedDecl.getQualifiedName().equals(targetFqn)) {
                return true;
            }

            return resolvedDecl.getAllAncestors().stream()
                    .flatMap(a -> a.getTypeDeclaration().stream())
                    .anyMatch(aDecl -> aDecl.getQualifiedName().equals(targetFqn));

        } catch (Exception e) {
            logger.warn("Could not resolve type for {} (checking against {}): {}",
                    decl.getNameAsString(), targetFqn, e.toString());
            return false;
        }
    }
}
