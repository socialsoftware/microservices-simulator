package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.util.TypeUtils;

import java.util.Optional;

/**
 * <p>
 * Walks every CommandHandler subclass and records the FQNs of their injected
 * service fields (both @Autowired field and constructor injection) into
 * {@code state.dispatchTargetFqns}.
 * <p>
 * Only concrete class FQNs are recorded. Interface types are skipped so that
 * interface-typed injection points (e.g. OrderServiceApi) do not cause
 * unrelated implementations (e.g. AmbiguousServiceImplA/B) to be admitted by
 * ServiceVisitor when those implementations have no direct dispatch path.
 * <p>
 * Must run before ServiceVisitor.
 */
public class CommandHandlerIndexVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(CommandHandlerIndexVisitor.class);

    @Override
    public void visit(CompilationUnit cu, ApplicationAnalysisState state) {
        cu.findFirst(ClassOrInterfaceDeclaration.class).ifPresent(decl -> {
            if (!TypeUtils.isSubclassOf(decl, CommandHandler.class)) return;

            String handlerName = decl.getNameAsString();

            // Field injection
            decl.getFields().forEach(field ->
                    indexFieldType(field.getCommonType(), state, handlerName));

            // Constructor injection
            decl.getConstructors().forEach(ctor ->
                    ctor.getParameters().forEach(param ->
                            indexFieldType(param.getType(), state, handlerName)));
        });
    }

    private void indexFieldType(Type type, ApplicationAnalysisState state, String handlerName) {
        if (isInterfaceType(type)) {
            logger.debug("CommandHandler {}: skipping interface type '{}'", handlerName, type.asString());
            return;
        }
        resolveTypeFqn(type).ifPresent(fqn -> {
            state.dispatchTargetFqns.add(fqn);
            logger.debug("CommandHandler {}: indexed service type {}", handlerName, fqn);
        });
    }

    /**
     * Returns true when the type resolves to an interface declaration.
     * Returns false on resolution failure (conservative: treat unknown types as non-interface).
     */
    private boolean isInterfaceType(Type type) {
        try {
            var resolved = type.resolve();
            if (!resolved.isReferenceType()) return false;
            return resolved.asReferenceType().getTypeDeclaration()
                    .map(decl -> decl.isInterface())
                    .orElse(false);
        } catch (Exception e) {
            return false;
        }
    }

    private Optional<String> resolveTypeFqn(Type type) {
        try {
            return Optional.of(type.resolve().describe());
        } catch (Exception e) {
            logger.debug("Could not resolve type '{}': {}", type.asString(), e.getMessage());
            return Optional.empty();
        }
    }
}
