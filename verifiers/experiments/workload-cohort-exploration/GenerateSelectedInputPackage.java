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
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceIndex;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** Bounded exploratory generator for an exact set of source-derived inputs. */
public final class GenerateSelectedInputPackage {
    private GenerateSelectedInputPackage() {}

    public static void main(String[] args) throws Exception {
        if (args.length < 10 || args.length > 12) {
            throw new IllegalArgumentException("Expected applicationsRoot application output inputIdCsv "
                    + "maxWorkloads maxInputs maxSchedules recoveryCap seed fallback "
                    + "[retainedWorkloadCap [completeBeforePairs]]");
        }
        Path applicationsRoot = Path.of(args[0]);
        String application = args[1];
        Path output = Path.of(args[2]);
        String selection = args[3];
        boolean storySelection = selection.startsWith("story:");
        Set<String> requestedInputIds = storySelection ? Set.of()
                : Arrays.stream(selection.split(",", -1)).collect(Collectors.toUnmodifiableSet());
        int maxWorkloads = positive(args[4], "maxWorkloads");
        int maxInputs = positive(args[5], "maxInputs");
        int maxSchedules = positive(args[6], "maxSchedules");
        int recoveryCap = positive(args[7], "recoveryCap");
        long seed = Long.parseLong(args[8]);
        boolean fallback = strictBoolean(args[9]);
        int retainedWorkloadCap = args.length >= 11 ? positive(args[10], "retainedWorkloadCap") : maxWorkloads;
        List<String[]> completeBeforePairs = args.length == 12 && !args[11].equals("-")
                ? Arrays.stream(args[11].split("\\+", -1)).map(pair -> pair.split("<", -1)).toList()
                : List.of();
        if (completeBeforePairs.stream().anyMatch(pair -> pair.length != 2
                || pair[0].isBlank() || pair[1].isBlank())) {
            throw new IllegalArgumentException("completeBeforePairs must be SagaA<SagaB joined with +");
        }
        if (!storySelection && (requestedInputIds.size() < 2 || requestedInputIds.contains(""))) {
            throw new IllegalArgumentException("At least two distinct input IDs are required");
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
        List<InputVariant> inputs;
        if (storySelection) {
            String[] parts = selection.substring("story:".length()).split("\\|", -1);
            if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                throw new IllegalArgumentException("Story selection must be story:<class-suffix>|<method>|<saga-simple-names>");
            }
            Set<String> sagaSimpleNames = Arrays.stream(parts[2].split("\\+", -1))
                    .collect(Collectors.toUnmodifiableSet());
            inputs = model.inputVariants().stream()
                    .filter(input -> input.sourceClassFqn() != null && input.sourceClassFqn().endsWith(parts[0]))
                    .filter(input -> parts[1].equals(input.sourceMethodName()))
                    .filter(input -> sagaSimpleNames.contains(simpleName(input.sagaFqn())))
                    .toList();
            if (inputs.size() != sagaSimpleNames.size()) {
                throw new IllegalStateException("Expected one story input for each requested saga; got "
                        + inputs.stream().map(input -> input.deterministicId() + ":" + input.sagaFqn()).toList());
            }
        } else {
            inputs = model.inputVariants().stream()
                    .filter(input -> requestedInputIds.contains(input.deterministicId()))
                    .toList();
        }
        Set<String> selectedInputIds = inputs.stream().map(InputVariant::deterministicId)
                .collect(Collectors.toUnmodifiableSet());
        Set<String> sagas = inputs.stream().map(InputVariant::sagaFqn).collect(Collectors.toUnmodifiableSet());
        if (inputs.size() != selectedInputIds.size() || sagas.size() != selectedInputIds.size()) {
            throw new IllegalStateException("Expected one selected source-derived input for each saga; got "
                    + inputs.stream().map(input -> input.deterministicId() + ":" + input.sagaFqn()).toList());
        }
        var definitions = model.sagaDefinitions().stream()
                .filter(definition -> sagas.contains(definition.sagaFqn())).toList();
        if (definitions.size() != sagas.size()) {
            throw new IllegalStateException("Expected exactly the selected saga definitions, got " + definitions.size());
        }

        inputs.forEach(input -> System.err.println("SELECTED_INPUT " + input.deterministicId() + " "
                + input.sagaFqn() + " keys=" + input.logicalKeyBindings() + " recipe=" + input.inputRecipe()));
        model.aggregateKeyInputEvidence().stream()
                .filter(evidence -> selectedInputIds.contains(evidence.inputVariantId()))
                .forEach(evidence -> System.err.println("SELECTED_EVIDENCE " + evidence));

        var config = new ScenarioGeneratorConfig(true,
                ScenarioGeneratorConfig.GenerationStrategy.INTERACTION_PRUNED,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false, sagas.size(), maxWorkloads, maxInputs, maxSchedules, fallback,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING, seed);
        var allGenerated = ScenarioGenerator.generate(definitions, inputs,
                model.eventConsequenceDefinitions(), model.sourceSetupPlanBindings(),
                model.aggregateKeyInputEvidence(), config);
        var selectedWorkloads = allGenerated.workloadPlans().stream()
                .filter(workload -> workload.participants().size() == selectedInputIds.size())
                .filter(workload -> workload.participants().stream().map(p -> p.inputVariantId())
                        .collect(Collectors.toSet()).equals(selectedInputIds))
                .filter(workload -> workload.prerequisiteBaseline() == null && workload.setupPlan() != null)
                .filter(workload -> EagerFaultScenarioGenerator.evaluateMaterializability(workload).materializable())
                .filter(workload -> respectsCompleteBeforePairs(workload, completeBeforePairs))
                .limit(retainedWorkloadCap)
                .toList();
        var counts = new LinkedHashMap<>(allGenerated.counts());
        counts.put("preFilterWorkloadsEmitted", allGenerated.workloadPlans().size());
        counts.put("workloadsEmitted", selectedWorkloads.size());
        counts.put("postFilterSelectedInputWorkloads", selectedWorkloads.size());
        var generated = new WorkloadGenerationResult(allGenerated.schemaVersion(), allGenerated.effectiveConfig(),
                selectedWorkloads, allGenerated.rejectedInputVariants(), counts, allGenerated.warnings());
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
                        "inputs", inputs.stream().map(InputVariant::deterministicId).toList(),
                        "configuration", config,
                        "recoveryCap", recoveryCap,
                        "retainedWorkloadCap", retainedWorkloadCap,
                        "completeBeforePairs", completeBeforePairs.stream()
                                .map(pair -> pair[0] + "<" + pair[1]).toList(),
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

    private static String simpleName(String fqn) {
        int separator = fqn.lastIndexOf('.');
        return separator < 0 ? fqn : fqn.substring(separator + 1);
    }

    private static boolean respectsCompleteBeforePairs(WorkloadPlan workload, List<String[]> pairs) {
        for (String[] pair : pairs) {
            String beforeId = workload.participants().stream()
                    .filter(participant -> pair[0].equals(simpleName(participant.sagaFqn())))
                    .map(participant -> participant.deterministicId()).findFirst().orElse(null);
            String afterId = workload.participants().stream()
                    .filter(participant -> pair[1].equals(simpleName(participant.sagaFqn())))
                    .map(participant -> participant.deterministicId()).findFirst().orElse(null);
            if (beforeId == null || afterId == null) return false;
            int lastBefore = -1;
            int firstAfter = Integer.MAX_VALUE;
            for (int index = 0; index < workload.forwardSchedule().size(); index++) {
                String participantId = workload.forwardSchedule().get(index).sagaInstanceId();
                if (beforeId.equals(participantId)) lastBefore = index;
                if (afterId.equals(participantId)) firstAfter = Math.min(firstAfter, index);
            }
            if (lastBefore < 0 || firstAfter == Integer.MAX_VALUE || lastBefore >= firstAfter) return false;
        }
        return true;
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
