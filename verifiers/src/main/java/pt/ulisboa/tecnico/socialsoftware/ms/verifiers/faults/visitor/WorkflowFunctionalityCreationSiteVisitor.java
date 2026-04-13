package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowCreationArgumentSourceKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.WorkflowFunctionalityCreationSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;

public class WorkflowFunctionalityCreationSiteVisitor extends VoidVisitorAdapter<ApplicationAnalysisState> {
    private static final Logger logger = LoggerFactory.getLogger(WorkflowFunctionalityCreationSiteVisitor.class);
    private static final Set<String> WORKFLOW_EXECUTION_METHODS = Set.of("executeWorkflow", "resumeWorkflow", "executeUntilStep");

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
                    .filter(this::containsWorkflowExecutionCall)
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

                        List<WorkflowCreationArgumentSource> argumentSources = extractArgumentSources(decl, method, expr);
                        WorkflowFunctionalityCreationSite site = new WorkflowFunctionalityCreationSite(
                                className, method.getNameAsString(), typeName, argumentSources);
                        state.sagaCreationSites.add(site);
                        logger.info("Saga creation site: {}.{}() -> {} [args={}]",
                                className, method.getNameAsString(), typeName, argumentSources.size());
                    }));
        });
    }

    private boolean containsWorkflowExecutionCall(MethodDeclaration method) {
        return method.findAll(MethodCallExpr.class).stream()
                .map(MethodCallExpr::getNameAsString)
                .anyMatch(WORKFLOW_EXECUTION_METHODS::contains);
    }

    private List<WorkflowCreationArgumentSource> extractArgumentSources(ClassOrInterfaceDeclaration decl,
                                                                        MethodDeclaration method,
                                                                        ObjectCreationExpr expr) {
        Map<String, Integer> parameterIndexes = new LinkedHashMap<>();
        for (int i = 0; i < method.getParameters().size(); i++) {
            Parameter parameter = method.getParameters().get(i);
            parameterIndexes.put(parameter.getNameAsString(), i);
        }

        Set<String> fieldNames = decl.getFields().stream()
                .flatMap(field -> field.getVariables().stream())
                .map(VariableDeclarator::getNameAsString)
                .collect(java.util.stream.Collectors.toSet());

        List<WorkflowCreationArgumentSource> sources = new java.util.ArrayList<>();
        for (int i = 0; i < expr.getArguments().size(); i++) {
            Expression argument = expr.getArgument(i);
            sources.add(extractArgumentSource(i, argument, method, expr, parameterIndexes, fieldNames));
        }

        return sources;
    }

    private WorkflowCreationArgumentSource extractArgumentSource(int argumentIndex,
                                                                  Expression argument,
                                                                  MethodDeclaration method,
                                                                  ObjectCreationExpr creationExpr,
                                                                  Map<String, Integer> parameterIndexes,
                                                                  Set<String> fieldNames) {
        if (argument.isNameExpr()) {
            String name = argument.asNameExpr().getNameAsString();

            Optional<VariableDeclarator> local = resolveVisibleLocalVariable(method, creationExpr, name);
            if (local.isPresent()) {
                VariableDeclarator variable = local.get();
                String initializerText = variable.getInitializer().map(Expression::toString).orElse(null);
                return new WorkflowCreationArgumentSource(argumentIndex,
                        WorkflowCreationArgumentSourceKind.LOCAL_VARIABLE,
                        null,
                        name,
                        initializerText);
            }

            Integer parameterIndex = parameterIndexes.get(name);
            if (parameterIndex != null) {
                return new WorkflowCreationArgumentSource(argumentIndex,
                        WorkflowCreationArgumentSourceKind.METHOD_PARAMETER,
                        parameterIndex,
                        name,
                        null);
            }

            if (fieldNames.contains(name)) {
                return new WorkflowCreationArgumentSource(argumentIndex,
                        WorkflowCreationArgumentSourceKind.FIELD_REFERENCE,
                        null,
                        name,
                        null);
            }
        }

        if (argument.isFieldAccessExpr()) {
            FieldAccessExpr fieldAccess = argument.asFieldAccessExpr();
            if (fieldAccess.getScope().isThisExpr() && fieldNames.contains(fieldAccess.getNameAsString())) {
                return new WorkflowCreationArgumentSource(argumentIndex,
                        WorkflowCreationArgumentSourceKind.FIELD_REFERENCE,
                        null,
                        fieldAccess.getNameAsString(),
                        null);
            }
        }

        return new WorkflowCreationArgumentSource(argumentIndex,
                WorkflowCreationArgumentSourceKind.INLINE_EXPRESSION,
                null,
                null,
                argument.toString());
    }

    private Optional<VariableDeclarator> resolveVisibleLocalVariable(MethodDeclaration method,
                                                                     ObjectCreationExpr creationExpr,
                                                                     String name) {
        if (creationExpr.getBegin().isEmpty()) {
            return Optional.empty();
        }

        return method.findAll(VariableDeclarator.class).stream()
                .filter(variable -> variable.getNameAsString().equals(name))
                .filter(variable -> variable.getBegin().isPresent())
                .filter(variable -> variable.getBegin().get().isBefore(creationExpr.getBegin().get()))
                .filter(variable -> isVisibleToExpression(variable, creationExpr))
                .max(Comparator.comparing(variable -> variable.getBegin().get()));
    }

    private boolean isVisibleToExpression(VariableDeclarator variable, Node expression) {
        Optional<Node> scope = findLexicalScopeContainer(variable);
        if (scope.isEmpty()) {
            return false;
        }

        Node current = expression;
        while (current != null) {
            if (current == scope.get()) {
                return true;
            }
            current = current.getParentNode().orElse(null);
        }

        return false;
    }

    private Optional<Node> findLexicalScopeContainer(Node node) {
        Node current = node.getParentNode().orElse(null);
        while (current != null) {
            if (current instanceof BlockStmt
                    || current instanceof MethodDeclaration
                    || current instanceof ConstructorDeclaration
                    || current instanceof ForStmt
                    || current instanceof ForEachStmt
                    || current instanceof CatchClause
                    || current instanceof SwitchEntry
                    || current instanceof TryStmt
                    || current instanceof WhileStmt
                    || current instanceof DoStmt) {
                return Optional.of(current);
            }
            current = current.getParentNode().orElse(null);
        }

        return Optional.empty();
    }
}
