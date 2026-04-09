package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;

public class WorkflowFunctionalityCreationSiteVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityCreationSiteVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findAll(ClassOrInterfaceDeclaration.class).forEach(decl -> {
            String className = decl.resolve().getQualifiedName();

            // Skip classes that are themselves sagas
            if (state.sagas.stream().anyMatch(s -> s.getFqn().equals(className))) {
                return;
            }

            decl.getMethods().stream()
                    .filter(m -> !m.isPrivate())
                    .forEach(method -> method.findAll(ObjectCreationExpr.class).forEach(expr -> {
                        String typeName;
                        try {
                            typeName = expr.getType().resolve().describe();
                        } catch (Exception e) {
                            logger.debug("Could not resolve saga creation type '{}': {}",
                                    expr.getType().asString(), e.getMessage());
                            return;
                        }

                        if (!state.sagas.stream().anyMatch(s -> s.getFqn().equals(typeName))) {
                            return;
                        }

                        WorkflowFunctionalityCreationSite site = new WorkflowFunctionalityCreationSite(
                                className, method.getNameAsString(), typeName);
                        state.sagaCreationSites.add(site);
                        logger.info("Saga creation site: {}.{}() -> {}",
                                className, method.getNameAsString(), typeName);
                    }));
        });
    }
}
