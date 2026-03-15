package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.model.SagaCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.scenariogenerator.ApplicationAnalysisContext;

/**
 * Pass 3.5: Scans all Java methods for {@code new *Saga()} constructor calls
 * to build a mapping from (className, methodName) → sagaClassName.
 * <p>
 * This is intentionally general — not limited to *Functionalities classes —
 * because any Java class could wrap a saga creation.
 * Classes that are themselves sagas are skipped.
 */
public class SagaCreationSiteVisitor extends VoidVisitorAdapter<ApplicationAnalysisContext> {
    private static final Logger logger = LoggerFactory.getLogger(SagaCreationSiteVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisContext context) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
            String className = decl.getNameAsString();

            // Skip classes that are themselves sagas
            if (context.getSagaClassNames().contains(className)) {
                return;
            }

            decl.getMethods().stream()
                    .filter(m -> !m.isPrivate())
                    .forEach(method -> method.findAll(ObjectCreationExpr.class).forEach(expr -> {
                        String typeName = expr.getType().getNameAsString();
                        if (context.getSagaClassNames().contains(typeName)) {
                            SagaCreationSite site = new SagaCreationSite(
                                    className, method.getNameAsString(), typeName);
                            context.sagaCreationSites.add(site);
                            logger.info("Saga creation site: {}.{}() -> {}",
                                    className, method.getNameAsString(), typeName);
                        }
                    }));
        });
    }
}
