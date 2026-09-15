import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ClassLoaderTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.ApplicationsFileTreeParser;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleCap;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ApplicationAnalysisScenarioModelAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ExecutableArtifactWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerIndexVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.CommandHandlerVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ConstructorCopyVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventConsequenceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.EventHandlingBridgeVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.GroovyConstructorInputTraceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.ServiceVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityCreationSiteVisitor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.visitor.WorkflowFunctionalityVisitor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Bounded, source-derived package generator retained from the M3 qualification. */
public final class GenerateLostCopiedUpdatePackage {
    private GenerateLostCopiedUpdatePackage() {}

    public static void main(String[] args) throws Exception {
        if (args.length != 12) {
            throw new IllegalArgumentException("Expected applicationsRoot application output sourceClass "
                    + "sourceMethod sagaCsv maxWorkloads maxInputs maxSchedules recoveryCap seed fallback");
        }
        Path applicationsRoot = Path.of(args[0]);
        String application = args[1];
        Path output = Path.of(args[2]);
        String sourceClass = args[3];
        String sourceMethod = args[4];
        Set<String> sagas = Arrays.stream(args[5].split(",", -1)).collect(Collectors.toUnmodifiableSet());
        int maxWorkloads = positive(args[6], "maxWorkloads");
        int maxInputs = positive(args[7], "maxInputs");
        int maxSchedules = positive(args[8], "maxSchedules");
        int recoveryCap = positive(args[9], "recoveryCap");
        long seed = Long.parseLong(args[10]);
        boolean fallback = strictBoolean(args[11]);
        if (sagas.size() != 2 || sagas.contains("")) {
            throw new IllegalArgumentException("Exactly two distinct saga FQNs are required");
        }

        Path applicationPath = applicationsRoot.resolve(application);
        var solver = new CombinedTypeSolver(
                new ReflectionTypeSolver(),
                new ClassLoaderTypeSolver(Thread.currentThread().getContextClassLoader()),
                new JavaParserTypeSolver(applicationPath.resolve("src/main/java")));
        StaticJavaParser.setConfiguration(new ParserConfiguration()
                .setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21)
                .setSymbolResolver(new JavaSymbolSolver(solver)));

        ApplicationsFileTreeParser parser = new ApplicationsFileTreeParser();
        parser.parse(applicationPath);
        ApplicationAnalysisState state = new ApplicationAnalysisState();

        CommandHandlerIndexVisitor commandHandlers = new CommandHandlerIndexVisitor();
        visitJava(parser, applicationsRoot, application, state, commandHandlers::visit);
        ServiceVisitor services = new ServiceVisitor();
        visitJava(parser, applicationsRoot, application, state, services::visit);
        CommandHandlerVisitor dispatch = new CommandHandlerVisitor();
        visitJava(parser, applicationsRoot, application, state, dispatch::visit);
        WorkflowFunctionalityVisitor workflows = new WorkflowFunctionalityVisitor();
        visitJava(parser, applicationsRoot, application, state, workflows::visit);
        WorkflowFunctionalityCreationSiteVisitor creationSites = new WorkflowFunctionalityCreationSiteVisitor();
        visitJava(parser, applicationsRoot, application, state, creationSites::visit);
        EventHandlingBridgeVisitor bridges = new EventHandlingBridgeVisitor();
        visitJava(parser, applicationsRoot, application, state, bridges::visit);
        bridges.finish(state);
        EventConsequenceVisitor consequences = new EventConsequenceVisitor();
        visitJava(parser, applicationsRoot, application, state, consequences::visit);
        consequences.finish(state);
        ConstructorCopyVisitor copies = new ConstructorCopyVisitor();
        visitJava(parser, applicationsRoot, application, state, copies::visit);
        copies.finish(state);

        Path testRoot = applicationPath.resolve("src/test/groovy");
        if (!Files.isDirectory(testRoot)) {
            throw new IllegalArgumentException("Missing Groovy test root: " + testRoot);
        }
        GroovySourceIndex sourceIndex = new GroovySourceIndex();
        sourceIndex.parse(testRoot);
        new GroovyConstructorInputTraceVisitor().visit(sourceIndex, state);

        var model = new ApplicationAnalysisScenarioModelAdapter().adapt(state);
        var inputs = model.inputVariants().stream()
                .filter(input -> sourceClass.equals(input.sourceClassFqn()))
                .filter(input -> sourceMethod.equals(input.sourceMethodName()))
                .filter(input -> sagas.contains(input.sagaFqn()))
                .filter(input -> input.sourceMode() == SourceMode.SAGAS)
                .toList();
        if (inputs.size() != 2 || inputs.stream().map(InputVariant::sagaFqn).collect(Collectors.toSet()).size() != 2) {
            throw new IllegalStateException("Expected one source-derived input for each saga; got "
                    + inputs.stream().map(input -> input.deterministicId() + ":" + input.sagaFqn()).toList());
        }
        var definitions = model.sagaDefinitions().stream()
                .filter(definition -> sagas.contains(definition.sagaFqn())).toList();
        if (definitions.size() != 2) {
            throw new IllegalStateException("Expected exactly two saga definitions, got " + definitions.size());
        }

        inputs.forEach(input -> System.err.println("SELECTED_INPUT " + input.deterministicId() + " "
                + input.sagaFqn() + " keys=" + input.logicalKeyBindings() + " recipe=" + input.inputRecipe()));
        var selectedInputIds = inputs.stream().map(InputVariant::deterministicId).collect(Collectors.toSet());
        model.aggregateKeyInputEvidence().stream()
                .filter(evidence -> selectedInputIds.contains(evidence.inputVariantId()))
                .forEach(evidence -> System.err.println("SELECTED_EVIDENCE " + evidence));

        var config = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, 2, maxWorkloads, maxInputs, maxSchedules, fallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, seed);
        var generated = ScenarioGenerator.generate(definitions, inputs,
                model.eventConsequenceDefinitions(), model.sourceSetupPlanBindings(),
                model.aggregateKeyInputEvidence(), config);
        if (generated.workloadPlans().isEmpty()) {
            throw new IllegalStateException("Expected generated workloads; " + generated.counts()
                    + " " + generated.warnings());
        }
        for (var workload : generated.workloadPlans()) {
            if (workload.prerequisiteBaseline() != null || workload.setupPlan() == null) {
                throw new IllegalStateException("Every workload must use source setup without a prerequisite provider");
            }
            var materializability = EagerFaultScenarioGenerator.evaluateMaterializability(workload);
            if (!materializability.materializable()) {
                throw new IllegalStateException("Not materializable: " + materializability);
            }
        }
        var eager = EagerFaultScenarioGenerator.generate(generated, new RecoveryScheduleCap(recoveryCap));
        new ExecutableArtifactWriter().write(model, application, eager, output);
        new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(output.resolve("generation-proof.json").toFile(),
                Map.of("workloads", generated.workloadPlans().size(),
                        "scenarios", eager.faultScenarios().size(),
                        "sourceClass", sourceClass,
                        "sourceMethod", sourceMethod,
                        "inputs", inputs.stream().map(InputVariant::deterministicId).toList(),
                        "configuration", config,
                        "recoveryCap", recoveryCap,
                        "counts", generated.counts(),
                        "warnings", generated.warnings()));
    }

    private static int positive(String value, String name) {
        int parsed = Integer.parseInt(value);
        if (parsed < 1) throw new IllegalArgumentException(name + " must be positive");
        return parsed;
    }

    private static boolean strictBoolean(String value) {
        if (!value.equals("true") && !value.equals("false")) {
            throw new IllegalArgumentException("fallback must be true or false");
        }
        return Boolean.parseBoolean(value);
    }

    private static void visitJava(ApplicationsFileTreeParser parser, Path applicationsRoot, String application,
                                  ApplicationAnalysisState state,
                                  SourceVisitor visitor) {
        parser.getJavaFilePathsForApplication(applicationsRoot, application).forEach((fqn, path) -> {
            try {
                visitor.visit(StaticJavaParser.parse(path), state);
            } catch (IOException failure) {
                throw new RuntimeException(failure);
            }
        });
    }

    @FunctionalInterface
    private interface SourceVisitor {
        void visit(CompilationUnit unit, ApplicationAnalysisState state);
    }
}
