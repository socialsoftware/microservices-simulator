package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.FaultScenarioValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.DateExpressionSupport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.WorkloadPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventEmissionSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeMapEntry;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionRef;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.PrerequisiteBaseline;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupParticipantBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadExecutionShape;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadMaterializability;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ScenarioCatalogPackageReader {

    /**
     * Reads the current count-only/static package through the same package
     * boundary used by the catalog readers. Static facts and future executable
     * roles therefore share one manifest/reader boundary.
     */
    public StaticPackageContents readCurrentStatic(Path manifestPath) {
        return CurrentStaticPackageReader.read(manifestPath);
    }

    /**
     * Read a catalog-writing package through the current role-keyed boundary.
     * All static and executable roles are integrity checked before the raw
     * records are returned.  The executor-facing model projection is exposed
     * separately so this reader remains the single validation boundary.
     */
    public ExecutablePackageContents readCurrent(Path manifestPath) {
        StaticPackageContents staticContents = CurrentStaticPackageReader.read(manifestPath);
        ScenarioCatalogManifest.Current current = staticContents.manifest();
        if (current.files().size() < 8 || current.files().size() > 11
                || !current.files().keySet().containsAll(CurrentStaticPackageReader.EXECUTABLE_ROLES)) {
            throw new IllegalArgumentException("current executable package must contain setups, workloads, faultScenarios, and requests");
        }
        LinkedHashMap<String, List<JsonNode>> records = new LinkedHashMap<>();
        LinkedHashMap<String, Path> paths = new LinkedHashMap<>();
        Path root = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize().getParent();
        for (String role : CurrentStaticPackageReader.EXECUTABLE_ROLES) {
            ScenarioCatalogManifest.Current.ArtifactFile file = current.files().get(role);
            Path path = CurrentStaticPackageReader.verifyAndResolve(root, file, role);
            paths.put(role, path);
            records.put(role, CurrentStaticPackageReader.readJsonLines(path, role));
        }
        CurrentStaticPackageReader.validateExecutable(records, staticContents.sagaFacts(),
                staticContents.inputFacts(), staticContents.interactionFacts());
        CurrentStaticPackageReader.validateDynamicAccounting(staticContents.accounting(),
                staticContents.dynamicObservations(), staticContents.dynamicAttributionLinks(),
                records.get("workloads"));
        return new ExecutablePackageContents(current, staticContents.accounting(),
                staticContents.sagaFacts(), staticContents.inputFacts(), staticContents.interactionFacts(),
                staticContents.copyContracts(), staticContents.copyContractPath(),
                records.get("setups"), records.get("workloads"), records.get("faultScenarios"),
                records.get("requests"), paths.get("setups"), paths.get("workloads"),
                paths.get("faultScenarios"), paths.get("requests"), staticContents.accountingPath(),
                staticContents.dynamicObservations(), staticContents.dynamicAttributionLinks(),
                staticContents.dynamicObservationPath(), staticContents.dynamicAttributionPath());
    }

    /** Model projection used by the existing preflight/executor runtime. */
    public PackageContents readCurrentForExecution(Path manifestPath) {
        ExecutablePackageContents packageContents = readCurrent(manifestPath);
        Map<String, InputVariant> inputs = new LinkedHashMap<>();
        for (JsonNode input : packageContents.inputFacts()) {
            InputVariant value = currentInput(input);
            inputs.put(value.deterministicId(), value);
        }
        Map<String, SetupPlan> sourceSetups = new LinkedHashMap<>();
        Map<String, PrerequisiteBaseline> providerSetups = new LinkedHashMap<>();
        for (JsonNode setup : packageContents.setupRecords()) {
            String id = setup.path("id").asText();
            if ("sourceDerived".equals(setup.path("kind").asText())) sourceSetups.put(id, currentSourceSetup(setup, inputs));
            else providerSetups.put(id, currentProviderSetup(setup));
        }
        Map<String, WorkloadPlan> workloads = new LinkedHashMap<>();
        for (JsonNode workload : packageContents.workloadRecords()) {
            WorkloadPlan value = currentWorkload(workload, inputs, sourceSetups, providerSetups,
                    packageContents.sagaFacts());
            workloads.put(value.deterministicId(), value);
        }
        List<FaultScenario> scenarios = packageContents.faultScenarioRecords().stream()
                .map(record -> currentFaultScenario(record, workloads.get(record.path("workload").asText())))
                .toList();
        List<WorkloadMaterializability> materializability = workloads.values().stream()
                .map(pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.EagerFaultScenarioGenerator::evaluateMaterializability)
                .toList();
        ScenarioCatalogManifest projectedManifest = executionManifest(packageContents, materializability);
        return new PackageContents(projectedManifest, List.copyOf(workloads.values()), scenarios,
                packageContents.accounting(), packageContents.workloadPath(), packageContents.faultScenarioPath(),
                packageContents.accountingPath());
    }

    /** Resolve one current fault record after the complete package has passed the reader boundary. */
    public SelectedPackageContents readCurrentSelectedForExecution(Path manifestPath,
                                                                    String workloadPlanId,
                                                                    String faultScenarioId) {
        PackageContents contents = readCurrentForExecution(manifestPath);
        FaultScenario selected = contents.faultScenarios().stream()
                .filter(scenario -> Objects.equals(scenario.deterministicId(), faultScenarioId))
                .findFirst().orElse(null);
        WorkloadPlan workload = selected == null ? contents.workloadPlans().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), workloadPlanId))
                .findFirst().orElse(null) : contents.workloadPlans().stream()
                .filter(candidate -> Objects.equals(candidate.deterministicId(), selected.workloadPlanId()))
                .findFirst().orElse(null);
        return selected == null ? null : new SelectedPackageContents(contents.manifest(), workload, selected,
                contents.workloadCatalogPath(), contents.faultScenarioCatalogPath(),
                contents.accountingPath());
    }

    private InputVariant currentInput(JsonNode input) {
        JsonNode source = input.path("source");
        SourceMode mode = switch (input.path("transactionModel").asText("unknown")) {
            case "saga" -> SourceMode.SAGAS; case "tcc" -> SourceMode.TCC; default -> SourceMode.UNKNOWN;
        };
        InputResolutionStatus resolution = switch (input.path("resolution").asText("unresolved")) {
            case "fullyResolved" -> InputResolutionStatus.RESOLVED;
            case "runtimeDependent" -> InputResolutionStatus.REPLAYABLE;
            case "partial" -> InputResolutionStatus.PARTIAL;
            default -> InputResolutionStatus.UNRESOLVED;
        };
        List<InputRecipeArgument> args = new ArrayList<>();
        for (JsonNode arg : input.path("arguments")) {
            InputRecipeNode node = currentInputNode(arg.path("value"));
            args.add(new InputRecipeArgument(arg.path("index").asInt(), textOrNull(arg, "expectedType"), resolution,
                    node.executorReady(), node.blockers(), textOrNull(arg, "sourceExpression"), node));
        }
        InputRecipe recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, input.path("materializable").asBoolean(),
                List.of(), args);
        return new InputVariant(input.path("id").asText(), input.path("saga").asText(),
                textOrNull(source, "testClass"), textOrNull(source, "method"), null, null,
                pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRole.UNKNOWN,
                pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FixtureOrigin.UNKNOWN,
                resolution, mode, SourceModeConfidence.UNKNOWN, List.of(), textOrNull(source, "call"),
                textOrNull(source, "call"), List.of(), List.of(), Map.of(), List.of(), recipe);
    }

    private InputRecipeNode currentInputNode(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return InputRecipeNode.builder("unresolved").executorReady(false).build();
        String kind = value.path("kind").asText("unresolved");
        if ("relativeDateTime".equals(kind)) {
            CurrentStaticPackageReader.requireObject(value, "relativeDateTime input recipe");
            CurrentStaticPackageReader.allowed(value, Set.of("kind", "anchor", "offset"), "relativeDateTime input recipe");
            String anchor = CurrentStaticPackageReader.text(value, "anchor", "relativeDateTime input recipe");
            String offset = CurrentStaticPackageReader.text(value, "offset", "relativeDateTime input recipe");
            if (!DateExpressionSupport.isAllowedRelativeDateTime(anchor, offset)) {
                throw CurrentStaticPackageReader.invalid("relativeDateTime input recipe has unsupported anchor/offset");
            }
        }
        InputRecipeNode.Builder builder = InputRecipeNode.builder(switch (kind) {
            case "property" -> "property_access"; case "transform" -> "local_transform";
            case "relativeDateTime" -> "relative_date_time";
            case "runtime" -> "runtime"; default -> kind;
        }).executorReady(currentInputNodeExecutorReady(value));
        if (value.has("value")) builder.value(currentScalar(value.get("value"))).literalKind(value.get("value").isNumber() ? "number" : "string");
        if (value.has("targetType")) builder.targetTypeFqn(value.path("targetType").asText());
        if (value.has("type")) builder.expectedTypeFqn(value.path("type").asText());
        if (value.has("property")) builder.propertyName(value.path("property").asText());
        if (value.has("name")) builder.transformName(value.path("name").asText());
        if ("relativeDateTime".equals(kind)) {
            builder.anchor(value.path("anchor").asText());
            builder.offset(value.path("offset").asText());
        }
        if (value.has("id")) builder.placeholderId(value.path("id").asText());
        if ("baseline_binding".equals(kind)) {
            builder.bindingKey(textOrNull(value, "key"));
            builder.bindingTypeFqn(textOrNull(value, "type"));
        }
        if (value.has("reason")) builder.blockers(List.of(value.path("reason").asText()));
        if (value.has("receiver")) builder.receiver(currentInputNode(value.path("receiver")));
        if (value.has("elements")) {
            List<InputRecipeNode> elements = new ArrayList<>(); value.path("elements").forEach(e -> elements.add(currentInputNode(e))); builder.elements(elements);
        }
        if (value.has("arguments")) {
            List<InputRecipeArgument> args = new ArrayList<>(); int index = 0;
            for (JsonNode child : value.path("arguments")) {
                InputRecipeNode childNode = currentInputNode(child);
                args.add(new InputRecipeArgument(index++, null,
                        childNode.executorReady() ? InputResolutionStatus.RESOLVED : InputResolutionStatus.UNRESOLVED,
                        childNode.executorReady(), childNode.blockers(), null, childNode));
            }
            builder.arguments(args);
        }
        if (value.has("fields")) {
            List<InputRecipeAssignment> assignments = new ArrayList<>(); int index = 0;
            var fields = value.path("fields").fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                InputRecipeNode fieldNode = currentInputNode(entry.getValue());
                assignments.add(new InputRecipeAssignment("property", entry.getKey(), entry.getKey(), index++, null,
                        fieldNode.executorReady(), fieldNode.blockers(), fieldNode));
            }
            builder.assignments(assignments);
        }
        if (value.has("entries")) {
            List<InputRecipeMapEntry> entries = new ArrayList<>(); int index = 0;
            for (JsonNode entry : value.path("entries")) {
                entries.add(new InputRecipeMapEntry(index++, currentInputNode(entry.path("key")), currentInputNode(entry.path("value"))));
            }
            builder.entries(entries);
        }
        return builder.build();
    }

    private boolean currentInputNodeExecutorReady(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return false;
        String kind = value.path("kind").asText("unresolved");
        if ("unresolved".equals(kind) || "call".equals(kind) || "call_result".equals(kind)) return false;
        if ("constructor".equals(kind)) {
            if (!value.hasNonNull("targetType") || value.path("targetType").asText().isBlank()) return false;
            for (JsonNode argument : value.path("arguments")) {
                if (!currentInputNodeExecutorReady(argument)) return false;
            }
            var fields = value.path("fields").fields();
            while (fields.hasNext()) {
                if (!currentInputNodeExecutorReady(fields.next().getValue())) return false;
            }
            return true;
        }
        if ("collection".equals(kind)) {
            for (JsonNode element : value.path("elements")) {
                if (!currentInputNodeExecutorReady(element)) return false;
            }
            for (JsonNode entry : value.path("entries")) {
                if (!currentInputNodeExecutorReady(entry.path("key"))
                        || !currentInputNodeExecutorReady(entry.path("value"))) return false;
            }
            return true;
        }
        if ("property".equals(kind) || "transform".equals(kind)) {
            return currentInputNodeExecutorReady(value.path("receiver"));
        }
        if ("helper_result".equals(kind)) {
            return currentInputNodeExecutorReady(value.path("result"));
        }
        return true;
    }

    private Object currentScalar(JsonNode value) {
        if (value == null || value.isNull()) return null;
        if (value.isTextual()) return value.asText();
        if (value.isBoolean()) return value.asBoolean();
        // Preserve Jackson's Integer/Long/BigInteger representation without numeric promotion.
        if (value.isIntegralNumber()) return value.numberValue();
        if (value.isFloatingPointNumber()) return value.decimalValue();
        return value;
    }

    private SetupPlan currentSourceSetup(JsonNode setup, Map<String, InputVariant> inputs) {
        List<SetupAction> actions = new ArrayList<>();
        for (JsonNode action : setup.path("actions")) {
            String methodKey = action.path("call").asText();
            List<String> parameterTypes = setupMethodParameterTypes(methodKey);
            List<SetupArgument> args = new ArrayList<>(); int index = 0;
            for (JsonNode value : action.path("arguments")) {
                String expectedType = index < parameterTypes.size() ? parameterTypes.get(index) : null;
                args.add(new SetupArgument(index++, expectedType, currentSetupValue(value), List.of()));
            }
            boolean result = action.has("result");
            actions.add(new SetupAction(action.path("id").asText(), actions.size(), action.path("id").asText(),
                    methodKey, args, setupMethodResultType(methodKey), !result, List.of()));
        }
        List<SetupParticipantBinding> bindings = new ArrayList<>();
        for (JsonNode binding : setup.path("bindings")) {
            String inputId = textOrNull(binding, "input");
            int argumentIndex = binding.path("argument").asInt();
            InputVariant input = inputs.get(inputId);
            InputRecipeArgument inputArgument = input == null || input.inputRecipe() == null ? null
                    : input.inputRecipe().arguments().stream()
                    .filter(argument -> argument.index() == argumentIndex).findFirst().orElse(null);
            bindings.add(new SetupParticipantBinding(inputId, argumentIndex,
                    inputArgument == null ? null : inputArgument.expectedTypeFqn(),
                    currentSetupValue(binding.path("value")), List.of()));
        }
        return new SetupPlan(SetupPlan.SCHEMA_VERSION, actions, bindings,
                textList(setup.path("blockers")));
    }

    private List<String> setupMethodParameterTypes(String methodKey) {
        int open = methodKey.indexOf('(');
        int close = methodKey.lastIndexOf("):");
        if (open < 0 || close < open) return List.of();
        String parameters = methodKey.substring(open + 1, close);
        if (parameters.isBlank()) return List.of();
        List<String> result = new ArrayList<>();
        int genericDepth = 0;
        int start = 0;
        for (int index = 0; index < parameters.length(); index++) {
            char current = parameters.charAt(index);
            if (current == '<') genericDepth++;
            else if (current == '>') genericDepth--;
            else if (current == ',' && genericDepth == 0) {
                result.add(parameters.substring(start, index));
                start = index + 1;
            }
        }
        result.add(parameters.substring(start));
        return List.copyOf(result);
    }

    private String setupMethodResultType(String methodKey) {
        int marker = methodKey.lastIndexOf("):");
        return marker < 0 ? null : methodKey.substring(marker + 2);
    }

    private PrerequisiteBaseline currentProviderSetup(JsonNode setup) {
        String provider = setup.path("provider").asText(); int split = provider.lastIndexOf('@');
        String id = split < 0 ? provider : provider.substring(0, split);
        String version = split < 0 ? "1" : provider.substring(split + 1);
        List<BaselineBindingRequirement> bindings = new ArrayList<>();
        for (JsonNode binding : setup.path("bindings")) bindings.add(new BaselineBindingRequirement(binding.path("key").asText(), binding.path("type").asText()));
        return new PrerequisiteBaseline(id, version, bindings);
    }

    private SetupValueRecipe currentSetupValue(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) return new SetupValueRecipe(SetupValueKind.LITERAL, null, "blocked", null, null, List.of(), List.of(), List.of(), null, null, null, List.of("MISSING_SETUP_VALUE"));
        return switch (value.path("kind").asText("unresolved")) {
            case "literal" -> {
                Object scalar = currentScalar(value.get("value"));
                if (scalar instanceof String text && text.matches(
                        "DateHandler\\.now\\(\\)(\\.plusHours\\(1\\))?\\.plusMinutes\\((5|25)\\)")) {
                    yield new SetupValueRecipe(SetupValueKind.LOCAL_DATE_TIME, "java.time.LocalDateTime",
                            "local_date_time_expression", text, null, List.of(), List.of(), List.of(),
                            null, null, null, List.of());
                }
                yield new SetupValueRecipe(SetupValueKind.LITERAL, null, currentLiteralKind(value.get("value")),
                        scalar, null, List.of(), List.of(), List.of(), null, null, null, List.of());
            }
            case "result" -> SetupValueRecipe.actionResult(value.path("action").asText(), null,
                    currentSetupAssignments(value.path("fields")));
            case "property", "resultProperty" -> SetupValueRecipe.actionProperty(value.path("action").asText(), value.path("property").asText(), null);
            case "collection" -> {
                List<SetupValueRecipe> elements = new ArrayList<>();
                value.path("elements").forEach(element -> elements.add(currentSetupValue(element)));
                yield new SetupValueRecipe("set".equals(value.path("collectionKind").asText()) ? SetupValueKind.SET : SetupValueKind.LIST,
                        null, null, null, null, List.of(), List.of(), elements, null, null, null, List.of());
            }
            case "constructor" -> {
                List<SetupValueRecipe> args = new ArrayList<>();
                value.path("arguments").forEach(argument -> args.add(currentSetupValue(argument)));
                List<pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment> assignments = new ArrayList<>();
                value.path("fields").fields().forEachRemaining(entry -> assignments.add(
                        new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment(
                                assignments.size(), entry.getKey(), currentSetupValue(entry.getValue()), List.of())));
                yield new SetupValueRecipe(SetupValueKind.CONSTRUCTOR, null, null, null, textOrNull(value, "type"),
                        args, assignments, List.of(), null, null, null, List.of());
            }
            case "transform" -> new SetupValueRecipe(SetupValueKind.LOCAL_DATE_TO_STRING,
                    String.class.getName(), null, null, null, List.of(), List.of(), List.of(),
                    currentSetupValue(value.path("receiver")), null, null, List.of());
            default -> new SetupValueRecipe(SetupValueKind.LITERAL, null, "blocked", null, null, List.of(), List.of(), List.of(), null, null, null, List.of(value.path("reason").asText("UNRESOLVED_SETUP_VALUE")));
        };
    }

    private List<pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment>
    currentSetupAssignments(JsonNode fields) {
        List<pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment> assignments = new ArrayList<>();
        if (fields == null || fields.isMissingNode()) return List.of();
        if (!fields.isObject()) throw new IllegalArgumentException("setup result fields must be an object");
        fields.fields().forEachRemaining(entry -> assignments.add(
                new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment(
                        assignments.size(), entry.getKey(), currentSetupValue(entry.getValue()), List.of())));
        return List.copyOf(assignments);
    }

    private WorkloadPlan currentWorkload(JsonNode workload, Map<String, InputVariant> inputs,
                                         Map<String, SetupPlan> sourceSetups, Map<String, PrerequisiteBaseline> providerSetups,
                                         List<JsonNode> sagaFacts) {
        String id = workload.path("id").asText();
        List<SagaInstance> participants = new ArrayList<>();
        for (JsonNode participant : workload.path("participants")) participants.add(new SagaInstance(participant.path("id").asText(), participant.path("saga").asText(), participant.path("input").asText(), List.of()));
        List<InputVariant> accepted = participants.stream().map(p -> inputs.get(p.inputVariantId()))
                .filter(Objects::nonNull).distinct().toList();
        Map<String, ScheduledStep> steps = new LinkedHashMap<>(); Map<String, Integer> slots = new LinkedHashMap<>();
        List<ScheduledStep> forward = new ArrayList<>(); int index = 0;
        for (JsonNode occurrence : workload.path("schedule")) if ("step".equals(occurrence.path("kind").asText())) {
            String participant = occurrence.path("participant").asText(); String saga = participants.stream().filter(p -> p.deterministicId().equals(participant)).map(SagaInstance::sagaFqn).findFirst().orElse("unknown");
            String localStep = occurrence.path("sagaStep").asText();
            String step = saga + "::" + localStep;
            ScheduledStep value = new ScheduledStep(occurrence.path("id").asText(), participant, step, index, localStep.replaceFirst("#\\d+$", ""), List.of());
            steps.put(value.deterministicId(), value); forward.add(value);
            if (occurrence.has("faultSlot")) slots.put(value.deterministicId(), occurrence.path("faultSlot").asInt());
            index++;
        }
        List<EventConsequence> events = new ArrayList<>(); List<NormalActionRef> normal = new ArrayList<>(); Map<String, EventConsequence> eventById = new LinkedHashMap<>();
        for (JsonNode occurrence : workload.path("schedule")) {
            if ("step".equals(occurrence.path("kind").asText())) normal.add(NormalActionRef.forward(normal.size(), occurrence.path("id").asText()));
            else {
                String eventId = occurrence.path("id").asText(); String trigger = occurrence.path("triggeringStep").asText();
                JsonNode route = currentEventRoute(occurrence, trigger, steps, participants, sagaFacts);
                String eventType = textOrNull(route, "event");
                String eventHandlingClass = textOrNull(route, "eventHandlingClass");
                String handler = textOrNull(route, "handler");
                String processingMethod = textOrNull(route, "processingMethod");
                String functionalityMethod = textOrNull(route, "functionalityMethod");
                EventConsequence event = new EventConsequence(eventId, trigger,
                        new EventEmissionSite(eventId, "current", "current", 0, eventType, List.of()),
                        eventType, eventHandlingClass, processingMethod, handler, null, processingMethod,
                        null, functionalityMethod, textOrNull(route, "downstreamSaga"),
                        EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, List.of());
                events.add(event); eventById.put(eventId, event); normal.add(NormalActionRef.eventConsequence(normal.size(), eventId));
            }
        }
        List<ForwardFaultSlot> faultSlots = slots.entrySet().stream().sorted(Map.Entry.comparingByValue()).map(entry -> new ForwardFaultSlot("slot-" + entry.getValue(), entry.getValue(), entry.getKey(), steps.get(entry.getKey()).sagaInstanceId(), steps.get(entry.getKey()).stepId(), steps.get(entry.getKey()).runtimeStepName(), entry.getKey())).toList();
        List<CompensationCheckpoint> checkpoints = new ArrayList<>(); int checkpointIndex = 0;
        for (ScheduledStep step : forward) {
            CompensationEvidenceClass kind = currentCompensationKind(sagaFacts, participants, step);
            if (kind == null) continue;
            checkpoints.add(new CompensationCheckpoint("checkpoint-" + step.deterministicId(), checkpointIndex++, step.sagaInstanceId(), step.deterministicId(), step.stepId(), step.runtimeStepName(), step.deterministicId(), kind, List.of(), List.of(), List.of()));
        }
        SetupPlan setup = workload.has("setup") ? sourceSetups.get(workload.path("setup").asText()) : null;
        if (setup != null) setup = restoreSetupBindingTypes(setup, inputs);
        PrerequisiteBaseline baseline = workload.has("setup") ? providerSetups.get(workload.path("setup").asText()) : null;
        // A reusable provider-backed setup may be selected for a workload that
        // happens to use only statically constructed inputs.  In that case no
        // baseline binding is referenced by the resolved inputs, so retaining
        // the provider declaration as an executable prerequisite would make
        // the model fail the coverage invariant for an otherwise materializable
        // workload.  Keep the provider record in the raw package while only
        // projecting an executable baseline when it is actually consumed.
        if (baseline != null && participants.stream().map(p -> inputs.get(p.inputVariantId()))
                .filter(Objects::nonNull).noneMatch(this::referencesBaselineBinding)) {
            baseline = null;
        }
        return new WorkloadPlan(WorkloadPlan.CURRENT_SCHEMA_VERSION, id,
                participants.size() > 1 ? ScenarioKind.MULTI_SAGA : ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, accepted, forward, events, normal,
                baseline, setup, List.of(), faultSlots, checkpoints, List.of());
    }

    private String currentLiteralKind(JsonNode value) {
        if (value == null || value.isNull()) return "null";
        if (value.isIntegralNumber()) return "integer";
        if (value.isFloatingPointNumber()) return "decimal";
        if (value.isBoolean()) return "boolean";
        return "string";
    }

    private JsonNode currentEventRoute(JsonNode occurrence, String trigger,
                                       Map<String, ScheduledStep> steps,
                                       List<SagaInstance> participants,
                                       List<JsonNode> sagaFacts) {
        ScheduledStep triggeringStep = steps.get(trigger);
        if (triggeringStep == null) {
            throw new IllegalArgumentException("event occurrence " + occurrence.path("id").asText()
                    + " references missing triggering step " + trigger);
        }
        String saga = participants.stream()
                .filter(participant -> participant.deterministicId().equals(triggeringStep.sagaInstanceId()))
                .map(SagaInstance::sagaFqn).findFirst().orElseThrow();
        String routeId = occurrence.path("route").asText();
        for (JsonNode fact : sagaFacts) {
            if (!fact.path("fqn").asText().equals(saga)) continue;
            for (JsonNode step : fact.path("steps")) {
                if (!step.path("id").asText().equals(localStepId(triggeringStep.stepId()))) continue;
                for (JsonNode route : step.path("eventRoutes")) {
                    if (route.path("id").asText().equals(routeId)) return route;
                }
            }
        }
        throw new IllegalArgumentException("event occurrence " + occurrence.path("id").asText()
                + " references missing Saga route " + saga + "::" + routeId);
    }

    private String localStepId(String stepId) {
        int marker = stepId == null ? -1 : stepId.lastIndexOf("::");
        return marker < 0 ? stepId : stepId.substring(marker + 2);
    }

    private boolean referencesBaselineBinding(InputVariant input) {
        if (input.inputRecipe() == null) return false;
        return input.inputRecipe().arguments().stream()
                .anyMatch(argument -> referencesBaselineBinding(argument.recipe()));
    }

    private SetupPlan restoreSetupBindingTypes(SetupPlan setup, Map<String, InputVariant> inputs) {
        List<SetupParticipantBinding> bindings = setup.participantBindings().stream().map(binding -> {
            InputVariant input = inputs.get(binding.inputVariantId());
            String type = binding.expectedTypeFqn();
            if (type == null && input != null && input.inputRecipe() != null) {
                type = input.inputRecipe().arguments().stream()
                        .filter(argument -> argument.index() == binding.argumentIndex())
                        .map(InputRecipeArgument::expectedTypeFqn).filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
            return new SetupParticipantBinding(binding.inputVariantId(), binding.argumentIndex(), type,
                    binding.value(), binding.blockers());
        }).toList();
        return new SetupPlan(setup.schemaVersion(), setup.actions(), bindings, setup.blockers());
    }

    private boolean referencesBaselineBinding(InputRecipeNode node) {
        if (node == null) return false;
        if ("baseline_binding".equals(node.kind())) return true;
        if (node.arguments().stream().anyMatch(argument -> referencesBaselineBinding(argument.recipe()))) return true;
        if (node.assignments().stream().anyMatch(assignment -> referencesBaselineBinding(assignment.valueRecipe()))) return true;
        if (node.elements().stream().anyMatch(this::referencesBaselineBinding)) return true;
        if (node.entries().stream().anyMatch(entry -> referencesBaselineBinding(entry.keyRecipe())
                || referencesBaselineBinding(entry.valueRecipe()))) return true;
        return referencesBaselineBinding(node.receiver());
    }

    private CompensationEvidenceClass currentCompensationKind(List<JsonNode> sagas, List<SagaInstance> participants, ScheduledStep step) {
        String saga = participants.stream().filter(p -> p.deterministicId().equals(step.sagaInstanceId())).map(SagaInstance::sagaFqn).findFirst().orElse(null);
        String local = step.stepId().substring(step.stepId().lastIndexOf("::") + 2);
        for (JsonNode fact : sagas) if (fact.path("fqn").asText().equals(saga)) for (JsonNode current : fact.path("steps")) if (current.path("id").asText().equals(local)) {
            return switch (current.path("compensation").path("kind").asText("none")) {
                case "explicit" -> CompensationEvidenceClass.EXPLICIT_COMPENSATION;
                case "implicitSagaRollback" -> CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK;
                case "conservativeUnknown" -> CompensationEvidenceClass.CONSERVATIVE_UNKNOWN;
                default -> null;
            };
        }
        return null;
    }

    private FaultScenario currentFaultScenario(JsonNode scenario, WorkloadPlan workload) {
        if (workload == null) throw new IllegalArgumentException("fault scenario references missing workload " + scenario.path("workload").asText());
        Map<String, ScheduledStep> steps = workload.forwardSchedule().stream().collect(java.util.stream.Collectors.toMap(ScheduledStep::deterministicId, value -> value));
        Map<String, ForwardFaultSlot> slots = workload.faultSlots().stream().collect(java.util.stream.Collectors.toMap(ForwardFaultSlot::scheduledStepId, value -> value));
        Map<String, EventConsequence> events = workload.eventConsequences().stream().collect(java.util.stream.Collectors.toMap(EventConsequence::deterministicId, value -> value));
        Map<String, CompensationCheckpoint> checkpoints = workload.compensationCheckpoints().stream().collect(java.util.stream.Collectors.toMap(CompensationCheckpoint::occurrenceId, value -> value));
        List<FaultScenarioAction> actions = new ArrayList<>();
        for (JsonNode action : scenario.path("actions")) {
            if (action.has("step")) {
                ScheduledStep step = steps.get(action.path("step").asText());
                ForwardFaultSlot slot = slots.get(step == null ? null : step.deterministicId());
                if (slot == null) throw new IllegalArgumentException("fault scenario " + scenario.path("id").asText() + " references non-faultable step " + action.path("step").asText());
                String aid = pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator.faultScenarioActionId(FaultScenarioActionKind.FORWARD, slot.sagaInstanceId(), slot.deterministicId(), null, slot.occurrenceId());
                actions.add(new FaultScenarioAction(aid, FaultScenarioActionKind.FORWARD, slot.sagaInstanceId(), slot.deterministicId(), null, slot.occurrenceId()));
            } else if (action.has("event")) {
                EventConsequence event = events.get(action.path("event").asText());
                ScheduledStep trigger = steps.get(event == null ? null : event.triggerScheduledStepId());
                if (event == null || trigger == null) throw new IllegalArgumentException("fault scenario references missing event occurrence " + action.path("event").asText());
                String aid = pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator.faultScenarioActionId(FaultScenarioActionKind.EVENT_CONSEQUENCE, trigger.sagaInstanceId(), null, null, event.deterministicId(), event.deterministicId());
                actions.add(new FaultScenarioAction(aid, FaultScenarioActionKind.EVENT_CONSEQUENCE, trigger.sagaInstanceId(), null, null, event.deterministicId(), event.deterministicId()));
            } else if (action.has("compensate")) {
                ScheduledStep compensated = steps.get(action.path("compensate").asText());
                CompensationCheckpoint checkpoint = compensated == null ? null : checkpoints.get(compensated.deterministicId());
                if (checkpoint == null) throw new IllegalArgumentException("fault scenario references missing compensation target " + action.path("compensate").asText());
                String aid = pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator.faultScenarioActionId(FaultScenarioActionKind.COMPENSATION, checkpoint.sagaInstanceId(), null, checkpoint.deterministicId(), checkpoint.occurrenceId());
                actions.add(new FaultScenarioAction(aid, FaultScenarioActionKind.COMPENSATION, checkpoint.sagaInstanceId(), null, checkpoint.deterministicId(), checkpoint.occurrenceId()));
            } else throw new IllegalArgumentException("fault scenario action must reference step, event, or compensate");
        }
        return new FaultScenario(FaultScenario.CURRENT_SCHEMA_VERSION, scenario.path("id").asText(), workload.deterministicId(), scenario.path("faultVector").asText(), actions);
    }

    private ScenarioCatalogManifest executionManifest(ExecutablePackageContents contents, List<WorkloadMaterializability> materializability) {
        int cap = contents.accounting().path("configuration").path("maxRecoverySchedulesPerVector").asInt(20);
        Map<String, String> counts = new LinkedHashMap<>(); counts.put("materializableWorkloadPlans", Long.toString(materializability.stream().filter(WorkloadMaterializability::materializable).count()));
        counts.put("nonMaterializableWorkloadPlans", Long.toString(materializability.stream().filter(value -> !value.materializable()).count()));
        counts.put("workloadsExported", Integer.toString(contents.workloadRecords().size()));
        return new ScenarioCatalogManifest(ScenarioCatalogManifest.SCHEMA_VERSION, null, new pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig(), "CURRENT", "CURRENT", cap, "CURRENT", materializability, counts, List.of(),
                new ScenarioCatalogManifest.ArtifactMetadata("WORKLOAD_CATALOG", WorkloadPlan.CURRENT_SCHEMA_VERSION, contents.workloadPath().toString(), Integer.toString(contents.workloadRecords().size()), ""),
                new ScenarioCatalogManifest.ArtifactMetadata("FAULT_SCENARIO_CATALOG", FaultScenario.CURRENT_SCHEMA_VERSION, contents.faultScenarioPath().toString(), Integer.toString(contents.faultScenarioRecords().size()), ""),
                null, null, Map.of(), Map.of(), Map.of());
    }

    private static String textOrNull(JsonNode node, String field) { return node.has(field) && !node.path(field).isNull() ? node.path(field).asText() : null; }
    private static List<String> textList(JsonNode node) { List<String> result = new ArrayList<>(); if (node != null && node.isArray()) node.forEach(value -> result.add(value.asText())); return result; }

    public ScenarioCatalogPackageReader() { }

    /** Ordinary package consumption is current-only; historical embedded catalogs are not accepted. */
    public PackageContents read(Path manifestPath) {
        return readCurrentForExecution(manifestPath);
    }

    /** Current-only selection boundary used by preflight and execution. */
    public SelectedPackageContents readSelected(Path manifestPath,
                                                String workloadPlanId,
                                                String faultScenarioId) {
        return readCurrentSelectedForExecution(manifestPath, workloadPlanId, faultScenarioId);
    }

    /** Current static-role validation kept inside the single package reader. */
    private static final class CurrentStaticPackageReader {
        private static final ObjectMapper MAPPER = new ObjectMapper();
        private static final Set<String> STATIC_ROLES = Set.of("accounting", "sagas", "inputs", "interactions");
        private static final Set<String> EXECUTABLE_ROLES = Set.of("setups", "workloads", "faultScenarios", "requests");
        private static final Set<String> DYNAMIC_ROLES = Set.of("dynamicObservations", "dynamicAttributionLinks");
        private static final Set<String> OPTIONAL_STATIC_ROLES = Set.of("copy-contracts");
        private static final Set<String> ROLES = new java.util.LinkedHashSet<>() {{ addAll(STATIC_ROLES); addAll(OPTIONAL_STATIC_ROLES); addAll(EXECUTABLE_ROLES); addAll(DYNAMIC_ROLES); }};

        private static StaticPackageContents read(Path manifestPath) {
            Path manifest = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize();
            Path root = manifest.getParent();
            if (root == null) throw invalid("manifest path has no package directory");
            JsonNode manifestNode = readSingleJson(manifest, "manifest");
            requireObject(manifestNode, "manifest");
            if (manifestNode.size() != 2 || !manifestNode.has("formatVersion") || !manifestNode.has("files")) {
                throw invalid("manifest must contain only formatVersion and files");
            }
            JsonNode version = required(manifestNode, "formatVersion", "manifest");
            if (!version.isInt() || version.asInt() != ScenarioCatalogManifest.Current.FORMAT_VERSION) {
                throw invalid("manifest formatVersion must be integer " + ScenarioCatalogManifest.Current.FORMAT_VERSION);
            }
            JsonNode filesNode = required(manifestNode, "files", "manifest");
            if (!filesNode.isObject() || filesNode.size() < STATIC_ROLES.size() || filesNode.size() > ROLES.size()) {
                throw invalid("manifest files must contain the current static roles, optional executable roles, and optional dynamic roles");
            }
            LinkedHashMap<String, ScenarioCatalogManifest.Current.ArtifactFile> files = new LinkedHashMap<>();
            Set<String> resolvedPaths = new HashSet<>();
            filesNode.fields().forEachRemaining(entry -> {
                if (!ROLES.contains(entry.getKey())) throw invalid("manifest contains unsupported artifact role " + entry.getKey());
                JsonNode metadata = entry.getValue();
                requireObject(metadata, "manifest files." + entry.getKey());
                if (metadata.size() != 2 || !metadata.has("path") || !metadata.has("sha256")) {
                    throw invalid("manifest files." + entry.getKey() + " must contain only path and sha256");
                }
                String pathText = text(metadata, "path", "manifest files." + entry.getKey());
                String sha = text(metadata, "sha256", "manifest files." + entry.getKey());
                if (!sha.matches("[0-9a-f]{64}")) throw invalid("manifest files." + entry.getKey() + " has invalid sha256");
                if ("copy-contracts".equals(entry.getKey()) && !"copy-contracts.json".equals(pathText)) {
                    throw invalid("manifest files.copy-contracts path must be copy-contracts.json");
                }
                Path resolved = resolveInside(root, pathText, entry.getKey());
                if (!resolvedPaths.add(resolved.toString())) throw invalid("manifest artifact paths must be unique");
                files.put(entry.getKey(), new ScenarioCatalogManifest.Current.ArtifactFile(pathText, sha));
                if (!Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)) throw invalid("missing artifact " + resolved);
            });
            if (!files.keySet().containsAll(STATIC_ROLES)) throw invalid("manifest is missing one or more static artifact roles");
            boolean anyExecutable = files.keySet().stream().anyMatch(EXECUTABLE_ROLES::contains);
            boolean allExecutable = files.keySet().containsAll(EXECUTABLE_ROLES);
            boolean anyDynamic = files.keySet().stream().anyMatch(DYNAMIC_ROLES::contains);
            if (anyExecutable != allExecutable) {
                throw invalid("manifest is missing one or more executable artifact roles");
            }
            if (anyDynamic && !allExecutable) throw invalid("dynamic roles require a current executable package");
            if (files.containsKey("dynamicAttributionLinks") && !files.containsKey("dynamicObservations")) {
                throw invalid("dynamic attribution links require dynamic observations");
            }

            Path accountingPath = verifyAndResolve(root, files.get("accounting"), "accounting");
            Path sagaPath = verifyAndResolve(root, files.get("sagas"), "sagas");
            Path inputPath = verifyAndResolve(root, files.get("inputs"), "inputs");
            Path interactionPath = verifyAndResolve(root, files.get("interactions"), "interactions");
            Path copyContractPath = files.containsKey("copy-contracts")
                    ? verifyAndResolve(root, files.get("copy-contracts"), "copy-contracts") : null;
            JsonNode accounting = readSingleJson(accountingPath, "accounting");
            validateAccounting(accounting);
            List<JsonNode> sagas = readJsonLines(sagaPath, "sagas");
            List<JsonNode> inputs = readJsonLines(inputPath, "inputs");
            List<JsonNode> interactions = readJsonLines(interactionPath, "interactions");
            validateSagas(sagas);
            validateInputs(inputs, sagas);
            validateInteractions(interactions, sagas);
            Path dynamicObservationPath = files.containsKey("dynamicObservations")
                    ? verifyAndResolve(root, files.get("dynamicObservations"), "dynamicObservations") : null;
            Path dynamicAttributionPath = files.containsKey("dynamicAttributionLinks")
                    ? verifyAndResolve(root, files.get("dynamicAttributionLinks"), "dynamicAttributionLinks") : null;
            List<JsonNode> dynamicObservations = dynamicObservationPath == null ? List.of()
                    : readJsonLines(dynamicObservationPath, "dynamicObservations");
            List<JsonNode> dynamicAttributions = dynamicAttributionPath == null ? List.of()
                    : readJsonLines(dynamicAttributionPath, "dynamicAttributionLinks");
            JsonNode copyContracts = copyContractPath == null ? null
                    : readSingleJson(copyContractPath, "copy-contracts");
            if (copyContracts != null) validateCopyContracts(copyContracts);
            if (dynamicObservationPath != null && dynamicObservations.isEmpty()) {
                throw invalid("manifest-linked dynamicObservations artifact must not be empty");
            }
            if (dynamicAttributionPath != null && dynamicAttributions.isEmpty()) {
                throw invalid("manifest-linked dynamicAttributionLinks artifact must not be empty");
            }
            validateDynamic(accounting, dynamicObservations, dynamicAttributions, sagas, inputs);
            return new StaticPackageContents(new ScenarioCatalogManifest.Current(version.asInt(), files), accounting,
                    sagas, inputs, interactions, accountingPath, sagaPath, inputPath, interactionPath,
                    copyContracts, copyContractPath,
                    dynamicObservations, dynamicAttributions, dynamicObservationPath, dynamicAttributionPath);
        }

        private static void validateCopyContracts(JsonNode artifact) {
            requireObject(artifact, "copy-contracts");
            allowed(artifact, Set.of("schema", "support", "limitations", "contracts"), "copy-contracts");
            if (!"copy-contracts.v1".equals(text(artifact, "schema", "copy-contracts"))) {
                throw invalid("copy-contracts schema must be copy-contracts.v1");
            }
            JsonNode support = required(artifact, "support", "copy-contracts");
            requireObject(support, "copy-contracts support");
            allowed(support, Set.of("status", "description"), "copy-contracts support");
            if (!"bounded".equals(text(support, "status", "copy-contracts support"))) {
                throw invalid("copy-contracts support status must be bounded");
            }
            text(support, "description", "copy-contracts support");
            JsonNode limitations = required(artifact, "limitations", "copy-contracts");
            if (!limitations.isArray() || limitations.isEmpty()) {
                throw invalid("copy-contracts limitations must be a non-empty array");
            }
            limitations.forEach(value -> {
                if (!value.isTextual() || value.asText().isBlank()) {
                    throw invalid("copy-contracts limitations must contain non-blank text");
                }
            });
            JsonNode contracts = required(artifact, "contracts", "copy-contracts");
            if (!contracts.isArray()) throw invalid("copy-contracts contracts must be an array");
            String previous = null;
            for (JsonNode contract : contracts) {
                requireObject(contract, "copy contract");
                allowed(contract, Set.of("sourceType", "targetType", "sourceKey", "targetKey", "fields", "proof", "sourceFiles"), "copy contract");
                String sourceType = text(contract, "sourceType", "copy contract");
                String targetType = text(contract, "targetType", "copy contract");
                String sourceKey = text(contract, "sourceKey", "copy contract");
                String targetKey = text(contract, "targetKey", "copy contract");
                if (!"aggregateId".equals(sourceKey)) throw invalid("copy contract sourceKey must be aggregateId");
                JsonNode fields = required(contract, "fields", "copy contract");
                requireObject(fields, "copy contract fields");
                if (fields.size() < 2 || !fields.has(sourceKey)
                        || !targetKey.equals(fields.path(sourceKey).asText())) {
                    throw invalid("copy contract fields must include identity plus one business field");
                }
                fields.fields().forEachRemaining(entry -> {
                    if (entry.getKey().isBlank() || !entry.getValue().isTextual()
                            || entry.getValue().asText().isBlank()) {
                        throw invalid("copy contract fields must map non-blank source fields to target fields");
                    }
                });
                JsonNode proof = required(contract, "proof", "copy contract");
                if (!proof.isArray() || proof.size() != fields.size()) {
                    throw invalid("copy contract proof must cover every inferred field");
                }
                JsonNode sourceFiles = required(contract, "sourceFiles", "copy contract");
                if (!sourceFiles.isArray() || sourceFiles.isEmpty()) {
                    throw invalid("copy contract sourceFiles must be a non-empty array");
                }
                sourceFiles.forEach(file -> {
                    requireObject(file, "copy contract source file");
                    allowed(file, Set.of("path", "sha256"), "copy contract source file");
                    text(file, "path", "copy contract source file");
                    String sha256 = text(file, "sha256", "copy contract source file");
                    if (!sha256.matches("[0-9a-f]{64}")) throw invalid("copy contract source file has invalid sha256");
                });
                String order = sourceType + "\u0000" + targetType + "\u0000" + targetKey;
                if (previous != null && previous.compareTo(order) >= 0) {
                    throw invalid("copy contracts must have unique deterministic ordering");
                }
                previous = order;
            }
        }

        private static Path verifyAndResolve(Path root, ScenarioCatalogManifest.Current.ArtifactFile metadata, String role) {
            Path path = resolveInside(root, metadata.path(), role);
            try {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw invalid("missing " + role + " artifact " + path);
                if (!metadata.sha256().equals(sha256(path))) throw invalid("hash mismatch for " + role + " artifact");
                return path;
            } catch (IOException exception) {
                throw invalid("cannot read " + role + " artifact " + path, exception);
            }
        }

        private static Path resolveInside(Path root, String configured, String role) {
            if (configured == null || configured.isBlank()) throw invalid("missing path for " + role);
            Path path;
            try { path = Path.of(configured); } catch (RuntimeException exception) { throw invalid("invalid path for " + role, exception); }
            if (path.isAbsolute()) throw invalid("absolute path for " + role + " is outside package");
            for (Path segment : path) if ("..".equals(segment.toString())) throw invalid("escaping path for " + role);
            Path resolved = root.resolve(path).normalize();
            if (!resolved.startsWith(root)) throw invalid("artifact path for " + role + " escapes package directory");
            Path current = root;
            for (Path segment : root.relativize(resolved)) {
                current = current.resolve(segment);
                if (Files.exists(current, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(current)) throw invalid("symlink path for " + role);
            }
            return resolved;
        }

        private static JsonNode readSingleJson(Path path, String label) {
            try (InputStream input = Files.newInputStream(path);
                 InputStreamReader chars = new InputStreamReader(input, StandardCharsets.UTF_8.newDecoder()
                         .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT));
                 JsonParser parser = MAPPER.createParser(chars)) {
                JsonNode value = MAPPER.readTree(parser);
                if (value == null || parser.nextToken() != null) throw invalid("malformed " + label + " JSON");
                return value;
            } catch (CharacterCodingException exception) {
                throw invalid("malformed UTF-8 in " + label, exception);
            } catch (IOException exception) {
                throw invalid("failed to read " + label, exception);
            }
        }

        private static List<JsonNode> readJsonLines(Path path, String label) {
            List<JsonNode> records = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)))) {
                String line; int lineNumber = 0;
                while ((line = reader.readLine()) != null) {
                    lineNumber++;
                    if (line.isBlank()) throw invalid("blank line in " + label + " at line " + lineNumber);
                    try (JsonParser parser = MAPPER.createParser(line)) {
                        JsonNode node = MAPPER.readTree(parser);
                        if (node != null && parser.nextToken() != null) throw invalid("multiple JSON values in " + label + " at line " + lineNumber);
                        if (node == null) throw invalid("malformed " + label + " JSON at line " + lineNumber);
                        records.add(node);
                    } catch (IOException exception) {
                        throw invalid("malformed " + label + " JSON at line " + lineNumber, exception);
                    }
                }
            } catch (CharacterCodingException exception) {
                throw invalid("malformed UTF-8 in " + label, exception);
            } catch (IOException exception) { throw invalid("failed to read " + label, exception); }
            return List.copyOf(records);
        }

        private static void validateExecutable(Map<String, List<JsonNode>> records,
                                               List<JsonNode> sagaFacts,
                                               List<JsonNode> inputFacts,
                                               List<JsonNode> interactionFacts) {
            Set<String> sagaIds = sagaFacts.stream().map(saga -> saga.path("fqn").asText()).collect(java.util.stream.Collectors.toSet());
            Map<String, JsonNode> sagaById = sagaFacts.stream().collect(java.util.stream.Collectors.toMap(saga -> saga.path("fqn").asText(), value -> value, (left, right) -> left, LinkedHashMap::new));
            Set<String> inputIds = inputFacts.stream().map(input -> input.path("id").asText()).collect(java.util.stream.Collectors.toSet());
            Set<String> interactionIds = interactionFacts.stream().map(input -> input.path("id").asText()).collect(java.util.stream.Collectors.toSet());
            Map<String, Set<String>> routesBySaga = new LinkedHashMap<>();
            for (JsonNode saga : sagaFacts) {
                Set<String> routes = new HashSet<>();
                for (JsonNode step : saga.path("steps")) for (JsonNode route : step.path("eventRoutes")) {
                    if (route.has("id")) routes.add(route.path("id").asText());
                }
                routesBySaga.put(saga.path("fqn").asText(), routes);
            }
            for (JsonNode setup : records.getOrDefault("setups", List.of())) {
                requireObject(setup, "setup record");
                allowed(setup, Set.of("id", "kind", "materializable", "actions", "bindings", "provider", "blockers"), "setup record");
                text(setup, "id", "setup record");
                String kind = text(setup, "kind", "setup record");
                if (!Set.of("sourceDerived", "providerBacked").contains(kind)) throw invalid("invalid setup kind " + kind);
                if ("sourceDerived".equals(kind)) {
                    if (!required(setup, "materializable", "source-derived setup").isBoolean()) throw invalid("source-derived setup materializable must be a boolean");
                    if (!required(setup, "actions", "setup record").isArray()) throw invalid("source-derived setup actions must be an array");
                    if (!required(setup, "bindings", "setup record").isArray()) throw invalid("source-derived setup bindings must be an array");
                    Set<String> actionIds = new HashSet<>();
                    for (JsonNode action : setup.path("actions")) {
                        requireObject(action, "source-derived setup action");
                        allowed(action, Set.of("id", "call", "arguments", "result"), "source-derived setup action");
                        if (!actionIds.add(text(action, "id", "source-derived setup action"))) throw invalid("duplicate setup action id");
                        if (!required(action, "arguments", "source-derived setup action").isArray()) throw invalid("setup action arguments must be an array");
                    }
                    for (JsonNode binding : setup.path("bindings")) {
                        requireObject(binding, "source-derived setup binding");
                        allowed(binding, Set.of("input", "argument", "value"), "source-derived setup binding");
                        text(binding, "input", "source-derived setup binding");
                        if (!required(binding, "argument", "source-derived setup binding").isIntegralNumber() || binding.path("argument").asInt() < 0) throw invalid("setup binding argument must be non-negative");
                        required(binding, "value", "source-derived setup binding");
                    }
                } else {
                    text(setup, "provider", "provider-backed setup");
                    if (!required(setup, "bindings", "provider-backed setup").isArray()) throw invalid("provider-backed setup bindings must be an array");
                    for (JsonNode binding : setup.path("bindings")) {
                        requireObject(binding, "provider-backed setup binding");
                        allowed(binding, Set.of("key", "type"), "provider-backed setup binding");
                        text(binding, "key", "provider-backed setup binding"); text(binding, "type", "provider-backed setup binding");
                    }
                }
            }
            Set<String> setupIds = uniqueIds(records.getOrDefault("setups", List.of()), "setup");
            Set<String> workloadIds = uniqueIds(records.getOrDefault("workloads", List.of()), "workload");
            for (JsonNode workload : records.getOrDefault("workloads", List.of())) {
                requireObject(workload, "workload record");
                allowed(workload, Set.of("id", "participants", "setup", "interactions", "schedule"), "workload record");
                String id = text(workload, "id", "workload record");
                if (!required(workload, "participants", "workload " + id).isArray()) throw invalid("workload participants must be an array");
                if (!required(workload, "interactions", "workload " + id).isArray()) throw invalid("workload interactions must be an array");
                if (!required(workload, "schedule", "workload " + id).isArray()) throw invalid("workload schedule must be an array");
                if (workload.has("setup")) {
                    String setup = text(workload, "setup", "workload " + id);
                    if (!setupIds.contains(setup)) throw invalid("workload " + id + " references missing setup " + setup);
                }
                Set<String> participantIds = new HashSet<>();
                for (JsonNode participant : workload.path("participants")) {
                    requireObject(participant, "workload " + id + " participant");
                    String participantId = text(participant, "id", "workload " + id + " participant");
                    if (!participantIds.add(participantId)) throw invalid("duplicate participant id " + participantId + " in workload " + id);
                    String saga = text(participant, "saga", "workload " + id + " participant");
                    String input = text(participant, "input", "workload " + id + " participant");
                    if (!sagaIds.contains(saga)) throw invalid("workload " + id + " references missing Saga " + saga);
                    if (!inputIds.contains(input)) throw invalid("workload " + id + " references missing input " + input);
                }
                if (workload.has("setup")) {
                    JsonNode setupRecord = records.getOrDefault("setups", List.of()).stream()
                            .filter(candidate -> workload.path("setup").asText().equals(candidate.path("id").asText()))
                            .findFirst().orElse(null);
                    if (setupRecord != null && "sourceDerived".equals(setupRecord.path("kind").asText())) {
                        Set<String> workloadInputs = new HashSet<>();
                        workload.path("participants").forEach(participant -> workloadInputs.add(participant.path("input").asText()));
                        for (JsonNode binding : setupRecord.path("bindings")) {
                            String input = text(binding, "input", "source-derived setup binding");
                            if (!workloadInputs.contains(input)) throw invalid("workload " + id + " cannot resolve setup binding input " + input);
                        }
                    }
                }
                for (JsonNode interaction : workload.path("interactions")) {
                    if (!interaction.isTextual() || !interactionIds.contains(interaction.asText())) throw invalid("workload " + id + " references missing interaction " + interaction);
                }
                Set<String> occurrenceIds = new HashSet<>();
                Set<String> participantSagaSteps = new HashSet<>();
                Map<String, Integer> faultSlotsByOccurrence = new LinkedHashMap<>();
                Set<Integer> faultSlotIndexes = new HashSet<>();
                for (JsonNode occurrence : workload.path("schedule")) {
                    requireObject(occurrence, "workload " + id + " schedule occurrence");
                    String occurrenceId = text(occurrence, "id", "workload " + id + " schedule occurrence");
                    if (!occurrenceIds.add(occurrenceId)) throw invalid("duplicate occurrence id " + occurrenceId + " in workload " + id);
                    String kind = text(occurrence, "kind", "workload " + id + " schedule occurrence");
                    if ("step".equals(kind)) {
                        allowed(occurrence, Set.of("id", "kind", "participant", "sagaStep", "faultSlot"), "workload " + id + " step");
                        String participantId = text(occurrence, "participant", "workload " + id + " step");
                        if (!participantIds.contains(participantId)) throw invalid("step references missing participant in workload " + id);
                        String participantSaga = null;
                        for (JsonNode participant : workload.path("participants")) if (participantId.equals(participant.path("id").asText())) participantSaga = participant.path("saga").asText();
                        String sagaStep = text(occurrence, "sagaStep", "workload " + id + " step");
                        if (!participantSagaSteps.add(participantId + "\u0000" + sagaStep)) {
                            throw invalid("duplicate exact Saga step occurrence " + participantId + "/" + sagaStep
                                    + " in workload " + id);
                        }
                        JsonNode sagaFact = sagaById.get(participantSaga);
                        boolean exactStep = false;
                        if (sagaFact != null) for (JsonNode candidate : sagaFact.path("steps")) if (sagaStep.equals(candidate.path("id").asText())) exactStep = true;
                        if (!exactStep) throw invalid("workload " + id + " references missing exact Saga step " + participantSaga + "/" + sagaStep);
                        if (occurrence.has("faultSlot") && (!occurrence.get("faultSlot").isIntegralNumber() || occurrence.get("faultSlot").asInt() < 0)) throw invalid("invalid faultSlot in workload " + id);
                        if (occurrence.has("faultSlot")) {
                            int slot = occurrence.path("faultSlot").asInt();
                            if (!faultSlotIndexes.add(slot)) throw invalid("duplicate faultSlot in workload " + id);
                            faultSlotsByOccurrence.put(occurrenceId, slot);
                        }
                    } else if ("event".equals(kind)) {
                        allowed(occurrence, Set.of("id", "kind", "route", "triggeringStep"), "workload " + id + " event");
                        String route = text(occurrence, "route", "workload " + id + " event");
                        if (!occurrenceIds.contains(text(occurrence, "triggeringStep", "workload " + id + " event"))) throw invalid("event references missing triggering step in workload " + id);
                        String trigger = occurrence.path("triggeringStep").asText();
                        JsonNode triggerOccurrence = null;
                        for (JsonNode candidate : workload.path("schedule")) {
                            if (trigger.equals(candidate.path("id").asText())) {
                                triggerOccurrence = candidate;
                                break;
                            }
                        }
                        if (triggerOccurrence == null || !"step".equals(triggerOccurrence.path("kind").asText())) throw invalid("event triggeringStep must reference a step in workload " + id);
                        String ownerSaga = null;
                        for (JsonNode step : workload.path("schedule")) if (trigger.equals(step.path("id").asText())) {
                            String participant = step.path("participant").asText();
                            for (JsonNode candidate : workload.path("participants")) if (participant.equals(candidate.path("id").asText())) ownerSaga = candidate.path("saga").asText();
                        }
                        if (ownerSaga != null && !routesBySaga.getOrDefault(ownerSaga, Set.of()).contains(route)) throw invalid("event references missing Saga route " + route);
                    } else throw invalid("invalid schedule occurrence kind " + kind);
                }
                for (int expected = 0; expected < faultSlotIndexes.size(); expected++) {
                    if (!faultSlotIndexes.contains(expected)) throw invalid("faultSlot indexes must be contiguous from zero in workload " + id);
                }
            }
            Set<String> faultIds = uniqueIds(records.getOrDefault("faultScenarios", List.of()), "fault scenario");
            Map<String, JsonNode> workloadById = new LinkedHashMap<>();
            for (JsonNode workload : records.getOrDefault("workloads", List.of())) workloadById.put(workload.path("id").asText(), workload);
            for (JsonNode scenario : records.getOrDefault("faultScenarios", List.of())) {
                requireObject(scenario, "fault scenario record");
                allowed(scenario, Set.of("id", "workload", "faultVector", "actions"), "fault scenario record");
                String id = text(scenario, "id", "fault scenario record");
                String workload = text(scenario, "workload", "fault scenario " + id);
                if (!workloadIds.contains(workload)) throw invalid("fault scenario " + id + " references missing workload " + workload);
                String vector = text(scenario, "faultVector", "fault scenario " + id);
                if (!vector.matches("[01]*")) throw invalid("fault scenario " + id + " has non-binary faultVector");
                if (!required(scenario, "actions", "fault scenario " + id).isArray()) throw invalid("fault scenario actions must be an array");
                JsonNode workloadRecord = records.getOrDefault("workloads", List.of()).stream()
                        .filter(value -> workload.equals(value.path("id").asText())).findFirst().orElse(null);
                Map<String, JsonNode> occurrences = new LinkedHashMap<>();
                Map<String, JsonNode> participantsById = new LinkedHashMap<>();
                int faultSlotCount = 0;
                if (workloadRecord != null) {
                    Set<Integer> slotIndexes = new HashSet<>();
                    for (JsonNode occurrence : workloadRecord.path("schedule")) {
                        occurrences.put(occurrence.path("id").asText(), occurrence);
                        if (occurrence.has("faultSlot")) {
                            faultSlotCount++;
                            if (!slotIndexes.add(occurrence.path("faultSlot").asInt())) throw invalid("duplicate faultSlot in workload " + workload);
                        }
                    }
                }
                if (workloadRecord != null) workloadRecord.path("participants").forEach(participant -> participantsById.put(participant.path("id").asText(), participant));
                if (vector.length() != faultSlotCount) throw invalid("fault scenario " + id + " vector length must equal fault-slot count");
                Set<String> completedForwardOccurrences = new HashSet<>();
                for (JsonNode action : scenario.path("actions")) {
                    requireObject(action, "fault scenario " + id + " action");
                    if (action.size() != 1 || (!action.has("step") && !action.has("event") && !action.has("compensate"))) throw invalid("fault scenario action must contain exactly one action reference");
                    String reference = action.elements().next().asText();
                    JsonNode occurrence = occurrences.get(reference);
                    if (occurrence == null) throw invalid("fault scenario " + id + " references missing occurrence " + reference);
                    if (action.has("step") && (!"step".equals(occurrence.path("kind").asText()) || !occurrence.has("faultSlot"))) throw invalid("forward action must reference a faultable step");
                    if (action.has("step")) completedForwardOccurrences.add(reference);
                    if (action.has("event") && !"event".equals(occurrence.path("kind").asText())) throw invalid("event action must reference an event occurrence");
                    if (action.has("compensate") && !"step".equals(occurrence.path("kind").asText())) throw invalid("compensation must reference a step occurrence");
                    if (action.has("compensate")) {
                        if (!completedForwardOccurrences.remove(reference)) throw invalid("compensation must reference an earlier un-compensated forward occurrence");
                        JsonNode participant = participantsById.get(occurrence.path("participant").asText());
                        JsonNode saga = participant == null ? null : sagaById.get(participant.path("saga").asText());
                        boolean compensatable = false;
                        if (saga != null) for (JsonNode step : saga.path("steps")) {
                            if (step.path("id").asText().equals(occurrence.path("sagaStep").asText())) {
                                compensatable = !"none".equals(step.path("compensation").path("kind").asText("none"));
                            }
                        }
                        if (!compensatable) throw invalid("compensation references a step without compensation evidence");
                    }
                }
            }
            Set<String> requestKeys = new HashSet<>();
            for (JsonNode request : records.getOrDefault("requests", List.of())) {
                requireObject(request, "request record");
                allowed(request, Set.of("workload", "faultVector", "effectiveRecoveryScheduleCap", "uncappedPossibleRecoverySchedules", "faultScenarioIds"), "request record");
                String workload = text(request, "workload", "request record");
                if (!workloadIds.contains(workload)) throw invalid("request references missing workload " + workload);
                String vector = text(request, "faultVector", "request record");
                if (!vector.matches("[01]*")) throw invalid("request has non-binary faultVector");
                JsonNode cap = required(request, "effectiveRecoveryScheduleCap", "request record");
                if (!cap.isIntegralNumber() || cap.asInt() <= 0) throw invalid("request cap must be a positive integer");
                JsonNode uncapped = required(request, "uncappedPossibleRecoverySchedules", "request record");
                if (!uncapped.isIntegralNumber() || uncapped.asLong() < 0) throw invalid("request uncapped schedule count must be non-negative");
                String key = workload + "\u0000" + vector + "\u0000" + cap.asText();
                if (!requestKeys.add(key)) throw invalid("duplicate request " + key);
                if (!required(request, "faultScenarioIds", "request record").isArray()) throw invalid("request faultScenarioIds must be an array");
                if (vector.length() != workloadById.get(workload).path("schedule").findValues("faultSlot").size()) throw invalid("request vector length must equal workload fault-slot count");
                Set<String> requestFaults = new HashSet<>();
                for (JsonNode fault : request.path("faultScenarioIds")) {
                    if (!fault.isTextual() || !faultIds.contains(fault.asText()) || !requestFaults.add(fault.asText())) throw invalid("request references missing or duplicate FaultScenario " + fault);
                    JsonNode scenario = records.getOrDefault("faultScenarios", List.of()).stream()
                            .filter(value -> fault.asText().equals(value.path("id").asText())).findFirst().orElse(null);
                    if (scenario == null || !workload.equals(scenario.path("workload").asText()) || !vector.equals(scenario.path("faultVector").asText())) throw invalid("request FaultScenario reference does not match identity");
                }
            }
        }

        private static Set<String> uniqueIds(List<JsonNode> records, String label) {
            Set<String> ids = new HashSet<>();
            for (JsonNode record : records) {
                requireObject(record, label + " record");
                String id = text(record, "id", label + " record");
                if (!ids.add(id)) throw invalid("duplicate " + label + " id " + id);
            }
            return ids;
        }

        private static void validateAccounting(JsonNode account) {
            requireObject(account, "accounting");
            for (String field : List.of("configuration", "sagas", "inputs", "interactions", "events", "workloads")) required(account, field, "accounting");
            for (String field : List.of("configuration", "sagas", "inputs", "interactions", "events", "workloads")) requireObject(account.path(field), "accounting." + field);
            if (account.has("schemaVersion")) throw invalid("accounting must not carry a per-file schema version");

            JsonNode configuration = account.path("configuration");
            text(configuration, "targetApplication", "accounting.configuration");
            text(configuration, "catalogWriteMode", "accounting.configuration");
            if (!required(configuration, "sagaSetSizes", "accounting.configuration").isArray()) throw invalid("accounting.configuration.sagaSetSizes must be an array");
            text(configuration, "sagaSetSelection", "accounting.configuration");
            if (!required(configuration, "acceptedInputStatuses", "accounting.configuration").isArray()) throw invalid("accounting.configuration.acceptedInputStatuses must be an array");

            JsonNode sagaMetrics = account.path("sagas");
            for (String field : List.of("found", "withAcceptedInputs", "withoutAcceptedInputs")) integral(sagaMetrics, field, "accounting.sagas");
            JsonNode sagaRows = required(sagaMetrics, "bySaga", "accounting.sagas");
            if (!sagaRows.isArray()) throw invalid("accounting.sagas.bySaga must be an array");
            for (JsonNode row : sagaRows) {
                requireObject(row, "accounting.sagas.bySaga row");
                text(row, "fqn", "accounting.sagas.bySaga row");
                for (String field : List.of("stepCount", "acceptedInputs", "notAcceptedInputs", "materializableInputs", "blockedInputs", "strictDirectInteractions", "fallbackDirectInteractions")) integral(row, field, "accounting.sagas.bySaga row");
            }

            JsonNode inputMetrics = account.path("inputs");
            for (String field : List.of("found", "accepted", "notAccepted")) integral(inputMetrics, field, "accounting.inputs");
            integerMap(required(inputMetrics, "acceptedByStatus", "accounting.inputs"), "accounting.inputs.acceptedByStatus");
            integerMap(required(inputMetrics, "notAcceptedByReason", "accounting.inputs"), "accounting.inputs.notAcceptedByReason");
            JsonNode materializability = required(inputMetrics, "materializability", "accounting.inputs");
            requireObject(materializability, "accounting.inputs.materializability");
            for (String field : List.of("materializable", "blocked")) integral(materializability, field, "accounting.inputs.materializability");
            integerMap(required(materializability, "affectedInputsByReason", "accounting.inputs.materializability"), "accounting.inputs.materializability.affectedInputsByReason");

            JsonNode interactionMetrics = account.path("interactions");
            requireObject(required(interactionMetrics, "direct", "accounting.interactions"), "accounting.interactions.direct");
            JsonNode direct = interactionMetrics.path("direct");
            integral(direct, "total", "accounting.interactions.direct");
            integerMap(required(direct, "byEvidence", "accounting.interactions.direct"), "accounting.interactions.direct.byEvidence");
            JsonNode sagaSets = required(interactionMetrics, "sagaSets", "accounting.interactions");
            requireObject(sagaSets, "accounting.interactions.sagaSets");
            for (String selection : List.of("strict", "withTypeOnlyFallback")) {
                JsonNode setMetrics = required(sagaSets, selection, "accounting.interactions.sagaSets");
                requireObject(setMetrics, "accounting.interactions.sagaSets." + selection);
                integerMap(required(setMetrics, "connectedBySize", "accounting.interactions.sagaSets." + selection), "connectedBySize");
                integerMap(required(setMetrics, "withAcceptedInputsBySize", "accounting.interactions.sagaSets." + selection), "withAcceptedInputsBySize");
            }

            JsonNode eventMetrics = account.path("events");
            integral(eventMetrics, "emissionSites", "accounting.events");
            integral(eventMetrics, "resolvedEventRoutes", "accounting.events");
            JsonNode workloads = account.path("workloads");
            for (String selection : List.of("all", "selected")) {
                JsonNode metrics = required(workloads, selection, "accounting.workloads");
                requireObject(metrics, "accounting.workloads." + selection);
                integral(metrics, "total", "accounting.workloads." + selection);
                integerMap(required(metrics, "bySagaSetSize", "accounting.workloads." + selection), "bySagaSetSize");
                integral(metrics, "inputBoundTotal", "accounting.workloads." + selection);
            }
            JsonNode written = required(workloads, "written", "accounting.workloads");
            requireObject(written, "accounting.workloads.written");
            integral(written, "total", "accounting.workloads.written");
        }

        private static void validateSagas(List<JsonNode> sagas) {
            Set<String> ids = new HashSet<>();
            for (JsonNode saga : sagas) {
                requireObject(saga, "Saga fact");
                String fqn = text(saga, "fqn", "Saga fact"); if (!ids.add(fqn)) throw invalid("duplicate Saga id " + fqn);
                if (!required(saga, "dependencies", "Saga " + fqn).isArray()) throw invalid("Saga dependencies must be an array");
                JsonNode steps = required(saga, "steps", "Saga " + fqn); if (!steps.isArray()) throw invalid("Saga steps must be an array");
                Set<String> stepIds = new HashSet<>();
                for (JsonNode step : steps) {
                    requireObject(step, "Saga " + fqn + " step");
                    String id = text(step, "id", "Saga " + fqn + " step");
                    if (!stepIds.add(id)) throw invalid("duplicate step id " + id + " in Saga " + fqn);
                }
                Set<String> routeIds = new HashSet<>();
                for (JsonNode step : steps) {
                    requireObject(step, "Saga " + fqn + " step");
                    String id = text(step, "id", "Saga " + fqn + " step");
                    JsonNode accesses = required(step, "commandAccesses", "step " + id); if (!accesses.isArray()) throw invalid("commandAccesses must be an array");
                    for (JsonNode access : accesses) {
                        requireObject(access, "step " + id + " command access");
                        JsonNode aggregate = required(access, "aggregate", "step " + id + " command access");
                        requireObject(aggregate, "step " + id + " command access aggregate");
                        text(aggregate, "name", "step " + id + " command access aggregate");
                        String mode = text(aggregate, "mode", "step " + id + " command access aggregate");
                        if (!Set.of("read", "write").contains(mode)) throw invalid("invalid command access mode " + mode);
                    }
                    JsonNode compensation = required(step, "compensation", "step " + id); requireObject(compensation, "step compensation " + id);
                    String compensationKind = text(compensation, "kind", "step compensation " + id);
                    if (!Set.of("none", "explicit", "implicitSagaRollback", "conservativeUnknown").contains(compensationKind)) throw invalid("invalid compensation kind " + compensationKind);
                    if (compensation.has("step")) {
                        String target = text(compensation, "step", "step compensation " + id);
                        if (!stepIds.contains(target)) throw invalid("step compensation " + id + " references missing target step " + target);
                    }
                    JsonNode routes = required(step, "eventRoutes", "step " + id); if (!routes.isArray()) throw invalid("eventRoutes must be an array");
                    for (JsonNode route : routes) {
                        requireObject(route, "step " + id + " event route");
                        String routeId = text(route, "id", "step " + id + " event route");
                        if (!routeIds.add(routeId)) throw invalid("duplicate event route id " + routeId + " in Saga " + fqn);
                        text(route, "event", "step " + id + " event route");
                        text(route, "eventHandlingClass", "step " + id + " event route");
                        text(route, "handler", "step " + id + " event route");
                    }
                }
            }
            for (JsonNode saga : sagas) {
                String fqn = saga.path("fqn").asText();
                for (JsonNode dependency : saga.path("dependencies")) {
                    if (!dependency.isTextual() || dependency.asText().isBlank() || !ids.contains(dependency.asText())) {
                        throw invalid("Saga " + fqn + " references missing dependency " + dependency);
                    }
                }
                for (JsonNode step : saga.path("steps")) {
                    for (JsonNode route : step.path("eventRoutes")) {
                        if (route.has("downstreamSaga")) {
                            String downstream = text(route, "downstreamSaga", "Saga " + fqn + " event route");
                            if (!ids.contains(downstream)) throw invalid("Saga " + fqn + " event route references missing Saga " + downstream);
                        }
                    }
                }
            }
        }

        private static void validateInputs(List<JsonNode> inputs, List<JsonNode> sagas) {
            Set<String> sagaIds = sagas.stream().map(saga -> saga.path("fqn").asText()).collect(java.util.stream.Collectors.toSet());
            Set<String> ids = new HashSet<>();
            for (JsonNode input : inputs) {
                requireObject(input, "input fact"); String id = text(input, "id", "input fact");
                if (!ids.add(id)) throw invalid("duplicate input id " + id);
                String saga = text(input, "saga", "input " + id); if (!sagaIds.contains(saga)) throw invalid("input " + id + " references missing Saga " + saga);
                JsonNode source = required(input, "source", "input " + id); requireObject(source, "input " + id + " source");
                for (String field : List.of("testClass", "method", "testRole", "call")) text(source, field, "input " + id + " source");
                String model = text(input, "transactionModel", "input " + id); if (!Set.of("saga", "tcc", "unknown").contains(model)) throw invalid("invalid transactionModel for input " + id);
                String resolution = text(input, "resolution", "input " + id); if (!Set.of("fullyResolved", "runtimeDependent", "partial", "unresolved").contains(resolution)) throw invalid("invalid resolution for input " + id);
                if (!required(input, "arguments", "input " + id).isArray()) throw invalid("input arguments must be an array");
                for (JsonNode argument : input.path("arguments")) {
                    if (argument.isObject() && argument.has("value")) {
                        validateInputRecipeNode(argument.path("value"), "input " + id + " argument recipe");
                    }
                }
                if (input.has("aggregateKeyEvidence")) validateAggregateKeyEvidence(input.get("aggregateKeyEvidence"), id);
                JsonNode accepted = required(input, "accepted", "input " + id); if (!accepted.isBoolean()) throw invalid("input accepted must be boolean");
                JsonNode materializable = required(input, "materializable", "input " + id); if (!materializable.isBoolean()) throw invalid("input materializable must be boolean");
                if (!accepted.asBoolean()) {
                    text(input, "notAcceptedReason", "rejected input " + id);
                } else if (input.has("notAcceptedReason")) {
                    throw invalid("accepted input " + id + " must not have notAcceptedReason");
                }
                if (input.has("blockers")) {
                    JsonNode blockers = input.get("blockers");
                    if (!blockers.isArray()) throw invalid("input " + id + " blockers must be an array");
                    if (materializable.asBoolean()) throw invalid("materializable input " + id + " must not have blockers");
                    if (!materializable.asBoolean() && blockers.isEmpty()) throw invalid("blocked input " + id + " must have non-empty blockers");
                    for (JsonNode blocker : blockers) {
                        requireObject(blocker, "input " + id + " blocker");
                        text(blocker, "reason", "input " + id + " blocker");
                        if (blocker.has("argument")) {
                            JsonNode argument = blocker.get("argument");
                            if (!argument.isIntegralNumber() || argument.asInt() < 0) throw invalid("input " + id + " blocker argument must be a non-negative integer");
                        }
                        if (blocker.has("sourceExpression")) text(blocker, "sourceExpression", "input " + id + " blocker");
                    }
                } else if (!materializable.asBoolean()) {
                    throw invalid("blocked input " + id + " has no blockers");
                }
            }
        }

        private static void validateInputRecipeNode(JsonNode node, String label) {
            if (node == null || node.isMissingNode() || node.isNull() || !node.isObject()) return;
            String kind = node.path("kind").asText();
            if ("relativeDateTime".equals(kind)) {
                requireObject(node, label + " relativeDateTime");
                allowed(node, Set.of("kind", "anchor", "offset"), label + " relativeDateTime");
                String anchor = text(node, "anchor", label + " relativeDateTime");
                String offset = text(node, "offset", label + " relativeDateTime");
                if (!DateExpressionSupport.isAllowedRelativeDateTime(anchor, offset)) {
                    throw invalid(label + " relativeDateTime has unsupported anchor/offset");
                }
            }
            if (node.has("receiver")) validateInputRecipeNode(node.path("receiver"), label + " receiver");
            if (node.has("elements")) for (JsonNode element : node.path("elements")) {
                validateInputRecipeNode(element, label + " element");
            }
            if (node.has("arguments")) for (JsonNode child : node.path("arguments")) {
                validateInputRecipeNode(child, label + " child");
            }
            if (node.has("fields") && node.path("fields").isObject()) {
                node.path("fields").elements().forEachRemaining(child -> validateInputRecipeNode(child, label + " field"));
            }
            if (node.has("entries")) for (JsonNode entry : node.path("entries")) {
                if (entry.isObject()) {
                    validateInputRecipeNode(entry.path("key"), label + " map key");
                    validateInputRecipeNode(entry.path("value"), label + " map value");
                }
            }
        }

        private static void validateAggregateKeyEvidence(JsonNode evidence, String inputId) {
            requireObject(evidence, "input " + inputId + " aggregateKeyEvidence");
            String kind = text(evidence, "kind", "input " + inputId + " aggregateKeyEvidence");
            if (!Set.of("exact", "sameSource").contains(kind)) {
                throw invalid("invalid aggregate-key evidence kind " + kind + " for input " + inputId);
            }
            text(evidence, "aggregate", "input " + inputId + " aggregateKeyEvidence");
            if ("exact".equals(kind)) {
                text(evidence, "value", "input " + inputId + " aggregateKeyEvidence");
            } else {
                text(evidence, "origin", "input " + inputId + " aggregateKeyEvidence");
                text(evidence, "expression", "input " + inputId + " aggregateKeyEvidence");
            }
        }

        private static void validateInteractions(List<JsonNode> interactions, List<JsonNode> sagas) {
            Map<String, Set<String>> stepsBySaga = new LinkedHashMap<>();
            for (JsonNode saga : sagas) { Set<String> steps = new HashSet<>(); saga.path("steps").forEach(step -> steps.add(step.path("id").asText())); stepsBySaga.put(saga.path("fqn").asText(), steps); }
            Set<String> ids = new HashSet<>();
            for (JsonNode interaction : interactions) {
                requireObject(interaction, "interaction fact"); String id = text(interaction, "id", "interaction fact"); if (!ids.add(id)) throw invalid("duplicate interaction id " + id);
                JsonNode accesses = required(interaction, "accesses", "interaction " + id); if (!accesses.isArray() || accesses.size() != 2) throw invalid("interaction " + id + " must have exactly two accesses");
                for (JsonNode access : accesses) { requireObject(access, "interaction " + id + " access"); String saga = text(access, "saga", "interaction access"); String step = text(access, "step", "interaction access"); if (!stepsBySaga.getOrDefault(saga, Set.of()).contains(step)) throw invalid("interaction " + id + " references missing Saga/step " + saga + "/" + step); text(access, "aggregate", "interaction access"); String mode = text(access, "mode", "interaction access"); if (!Set.of("read", "write").contains(mode)) throw invalid("invalid interaction access mode " + mode); }
                String evidence = text(interaction, "evidence", "interaction " + id); if (!Set.of("exact", "symbolic", "typeOnly", "unknown").contains(evidence)) throw invalid("invalid interaction evidence " + evidence);
            }
        }

        private static void validateDynamic(JsonNode accounting,
                                            List<JsonNode> observations,
                                            List<JsonNode> attributions,
                                            List<JsonNode> sagas,
                                            List<JsonNode> inputs) {
            if (observations.isEmpty() && attributions.isEmpty()) return;
            JsonNode metrics = required(accounting, "dynamicEvidence", "accounting");
            requireObject(metrics, "accounting.dynamicEvidence");
            for (String section : List.of("testOutcomes", "observations", "sagaInvocations",
                    "uniqueInputEvidence", "workloadParticipantEvidence")) {
                requireObject(required(metrics, section, "accounting.dynamicEvidence"),
                        "accounting.dynamicEvidence." + section);
            }
            for (String field : List.of("passed", "failed")) integral(metrics.path("testOutcomes"), field,
                    "accounting.dynamicEvidence.testOutcomes");
            integral(metrics.path("observations"), "total", "accounting.dynamicEvidence.observations");
            integral(metrics.path("observations"), "withoutTestContext", "accounting.dynamicEvidence.observations");
            integerMap(required(metrics.path("observations"), "byKind", "accounting.dynamicEvidence.observations"),
                    "accounting.dynamicEvidence.observations.byKind");
            integral(metrics.path("sagaInvocations"), "total", "accounting.dynamicEvidence.sagaInvocations");
            integerMap(required(metrics.path("sagaInvocations"), "byStatus", "accounting.dynamicEvidence.sagaInvocations"),
                    "accounting.dynamicEvidence.sagaInvocations.byStatus");
            integerMap(metrics.path("uniqueInputEvidence"), "accounting.dynamicEvidence.uniqueInputEvidence");
            integerMap(metrics.path("workloadParticipantEvidence"), "accounting.dynamicEvidence.workloadParticipantEvidence");

            Map<String, Set<String>> stepsBySaga = new LinkedHashMap<>();
            for (JsonNode saga : sagas) {
                Set<String> steps = new HashSet<>();
                saga.path("steps").forEach(step -> steps.add(step.path("id").asText()));
                stepsBySaga.put(saga.path("fqn").asText(), steps);
            }
            Set<String> inputIds = new HashSet<>();
            inputs.forEach(input -> inputIds.add(input.path("id").asText()));
            Map<String, JsonNode> observationsById = new LinkedHashMap<>();
            Set<String> kinds = Set.of("stepStarted", "stepFinished", "commandSent", "aggregateAccessed", "invariantViolation");
            for (JsonNode observation : observations) {
                requireObject(observation, "dynamic observation");
                allowed(observation, Set.of("id", "kind", "sequence", "timestamp", "thread", "test", "saga",
                        "invocation", "step", "input", "phase", "outcome", "error", "command", "access", "violation"),
                        "dynamic observation");
                String id = text(observation, "id", "dynamic observation");
                if (observationsById.putIfAbsent(id, observation) != null) throw invalid("duplicate dynamic observation id " + id);
                String kind = text(observation, "kind", "dynamic observation " + id);
                if (!kinds.contains(kind)) throw invalid("invalid dynamic observation kind " + kind);
                integral(observation, "sequence", "dynamic observation " + id);
                text(observation, "timestamp", "dynamic observation " + id);
                text(observation, "thread", "dynamic observation " + id);
                if (observation.has("test")) {
                    requireObject(observation.path("test"), "dynamic observation test");
                    allowed(observation.path("test"), Set.of("execution", "class", "method"), "dynamic observation test");
                    text(observation.path("test"), "execution", "dynamic observation test");
                    optionalText(observation.path("test"), "class", "dynamic observation test");
                    optionalText(observation.path("test"), "method", "dynamic observation test");
                }
                String saga = optionalText(observation, "saga", "dynamic observation " + id);
                if (saga != null && !stepsBySaga.containsKey(saga)) throw invalid("dynamic observation references missing Saga " + saga);
                String step = optionalText(observation, "step", "dynamic observation " + id);
                if (step != null && (saga == null || !stepsBySaga.getOrDefault(saga, Set.of()).contains(step))) {
                    throw invalid("dynamic observation references missing Saga-local step " + step);
                }
                String input = optionalText(observation, "input", "dynamic observation " + id);
                if (input != null && !inputIds.contains(input)) throw invalid("dynamic observation references missing input " + input);
                switch (kind) {
                    case "stepStarted" -> {
                        rejectKindFields(observation, id, Set.of("outcome", "error", "command", "access", "violation"));
                        text(observation, "phase", "dynamic observation " + id);
                    }
                    case "stepFinished" -> {
                        rejectKindFields(observation, id, Set.of("phase", "command", "access", "violation"));
                        text(observation, "outcome", "dynamic observation " + id);
                        if (observation.has("error")) validateError(observation, id);
                    }
                    case "commandSent" -> validateCommand(observation, id);
                    case "aggregateAccessed" -> validateAccess(observation, id);
                    case "invariantViolation" -> validateViolation(observation, id);
                    default -> throw invalid("invalid dynamic observation kind " + kind);
                }
            }

            Set<String> statuses = Set.of("exactInput", "testAndShape", "shapeOnly", "ambiguous", "unmatched");
            for (JsonNode link : attributions) {
                requireObject(link, "dynamic attribution link");
                allowed(link, Set.of("testExecution", "saga", "sagaInvocation", "status", "observationIds",
                        "input", "candidateInputs", "reason"), "dynamic attribution link");
                String testExecution = text(link, "testExecution", "dynamic attribution link");
                String saga = text(link, "saga", "dynamic attribution link");
                if (!stepsBySaga.containsKey(saga)) throw invalid("dynamic attribution references missing Saga " + saga);
                String invocation = text(link, "sagaInvocation", "dynamic attribution link");
                String status = text(link, "status", "dynamic attribution link");
                if (!statuses.contains(status)) throw invalid("invalid dynamic attribution status " + status);
                JsonNode ids = required(link, "observationIds", "dynamic attribution link");
                if (!ids.isArray() || ids.isEmpty()) throw invalid("dynamic attribution observationIds must be non-empty");
                Set<String> unique = new HashSet<>();
                for (JsonNode value : ids) {
                    if (!value.isTextual() || !unique.add(value.asText())) throw invalid("invalid or duplicate dynamic attribution observation id");
                    JsonNode observation = observationsById.get(value.asText());
                    if (observation == null) throw invalid("dynamic attribution references missing observation " + value.asText());
                    if (!testExecution.equals(observation.path("test").path("execution").asText())
                            || !saga.equals(observation.path("saga").asText())
                            || !invocation.equals(observation.path("invocation").asText())) {
                        throw invalid("dynamic attribution observation does not belong to its test/Saga invocation group");
                    }
                }
                if (Set.of("exactInput", "testAndShape", "shapeOnly").contains(status)) {
                    String input = text(link, "input", "dynamic attribution link");
                    if (!inputIds.contains(input)) throw invalid("dynamic attribution references missing input " + input);
                    if (link.has("candidateInputs") || link.has("reason")) throw invalid("successful dynamic attribution has incompatible evidence fields");
                } else if ("ambiguous".equals(status)) {
                    JsonNode candidates = required(link, "candidateInputs", "ambiguous dynamic attribution");
                    if (!candidates.isArray() || candidates.isEmpty()) throw invalid("ambiguous attribution requires candidate inputs");
                    for (JsonNode candidate : candidates) if (!candidate.isTextual() || !inputIds.contains(candidate.asText())) {
                        throw invalid("ambiguous attribution references missing input");
                    }
                    if (link.has("input") || link.has("reason")) throw invalid("ambiguous attribution has incompatible evidence fields");
                } else {
                    if (link.has("input") || link.has("candidateInputs")) throw invalid("unmatched attribution cannot identify inputs");
                    optionalText(link, "reason", "unmatched dynamic attribution");
                }
            }
        }

        private static void validateDynamicAccounting(JsonNode accounting,
                                                      List<JsonNode> observations,
                                                      List<JsonNode> attributions,
                                                      List<JsonNode> workloads) {
            if (observations.isEmpty() && attributions.isEmpty()) return;
            JsonNode dynamic = required(accounting, "dynamicEvidence", "accounting");

            LinkedHashMap<String, Integer> byKind = zeroDynamicCounts(Set.of(
                    "stepStarted", "stepFinished", "commandSent", "aggregateAccessed", "invariantViolation"));
            int withoutTest = 0;
            for (JsonNode observation : observations) {
                byKind.merge(observation.path("kind").asText(), 1, Integer::sum);
                if (!observation.has("test")) withoutTest++;
            }
            requireCount(dynamic.path("observations"), "total", observations.size(), "dynamic observations");
            requireCount(dynamic.path("observations"), "withoutTestContext", withoutTest, "dynamic observations");
            requireExactCounts(dynamic.path("observations").path("byKind"), byKind, "dynamic observations byKind");

            LinkedHashMap<String, Integer> byStatus = zeroDynamicCounts(Set.of(
                    "exactInput", "testAndShape", "shapeOnly", "ambiguous", "unmatched"));
            for (JsonNode link : attributions) byStatus.merge(link.path("status").asText(), 1, Integer::sum);
            requireCount(dynamic.path("sagaInvocations"), "total", attributions.size(), "dynamic sagaInvocations");
            requireExactCounts(dynamic.path("sagaInvocations").path("byStatus"), byStatus,
                    "dynamic sagaInvocations byStatus");

            LinkedHashMap<String, String> strongestByInput = new LinkedHashMap<>();
            for (JsonNode link : attributions) {
                String status = link.path("status").asText();
                if (!Set.of("exactInput", "testAndShape", "shapeOnly").contains(status)) continue;
                String input = link.path("input").asText();
                strongestByInput.compute(input, (ignored, prior) -> dynamicRank(status) > dynamicRank(prior) ? status : prior);
            }
            LinkedHashMap<String, Integer> strongest = zeroDynamicCounts(Set.of("exactInput", "testAndShape", "shapeOnly"));
            strongestByInput.values().forEach(status -> strongest.merge(status, 1, Integer::sum));
            requireExactCounts(dynamic.path("uniqueInputEvidence"), strongest, "dynamic uniqueInputEvidence");

            Map<String, Set<String>> executionsByInput = new LinkedHashMap<>();
            for (JsonNode link : attributions) {
                if (!Set.of("exactInput", "testAndShape").contains(link.path("status").asText())) continue;
                executionsByInput.computeIfAbsent(link.path("input").asText(), ignored -> new HashSet<>())
                        .add(link.path("testExecution").asText());
            }
            LinkedHashMap<String, Integer> participants = zeroDynamicCounts(Set.of(
                    "allInputsObservedInOneCommonTest", "allInputsObservedAcrossSeparateTests",
                    "someInputsObserved", "noInputsObserved"));
            for (JsonNode workload : workloads) {
                List<String> inputs = new ArrayList<>();
                workload.path("participants").forEach(participant -> {
                    String input = participant.path("input").asText(null);
                    if (input != null && !inputs.contains(input)) inputs.add(input);
                });
                long observed = inputs.stream().filter(executionsByInput::containsKey).count();
                if (observed == 0) participants.merge("noInputsObserved", 1, Integer::sum);
                else if (observed < inputs.size()) participants.merge("someInputsObserved", 1, Integer::sum);
                else {
                    Set<String> common = new HashSet<>(executionsByInput.get(inputs.get(0)));
                    for (String input : inputs.subList(1, inputs.size())) common.retainAll(executionsByInput.get(input));
                    participants.merge(common.isEmpty() ? "allInputsObservedAcrossSeparateTests"
                            : "allInputsObservedInOneCommonTest", 1, Integer::sum);
                }
            }
            requireExactCounts(dynamic.path("workloadParticipantEvidence"), participants,
                    "dynamic workloadParticipantEvidence");
        }

        private static LinkedHashMap<String, Integer> zeroDynamicCounts(Set<String> names) {
            LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
            names.stream().sorted().forEach(name -> result.put(name, 0));
            return result;
        }

        private static int dynamicRank(String status) {
            if (status == null) return 0;
            return switch (status) { case "exactInput" -> 3; case "testAndShape" -> 2; case "shapeOnly" -> 1; default -> 0; };
        }

        private static void requireCount(JsonNode object, String field, int expected, String label) {
            integral(object, field, label);
            if (object.path(field).asInt() != expected) throw invalid(label + " field " + field
                    + " does not reconcile: expected " + expected + " but was " + object.path(field).asInt());
        }

        private static void requireExactCounts(JsonNode actual, Map<String, Integer> expected, String label) {
            requireObject(actual, label);
            Set<String> keys = new HashSet<>();
            actual.fieldNames().forEachRemaining(keys::add);
            if (!keys.equals(expected.keySet())) throw invalid(label + " keys do not reconcile");
            expected.forEach((key, value) -> requireCount(actual, key, value, label));
        }

        private static void validateCommand(JsonNode observation, String id) {
            rejectKindFields(observation, id, Set.of("phase", "outcome", "error", "access", "violation"));
            JsonNode command = required(observation, "command", "dynamic observation " + id);
            requireObject(command, "dynamic command");
            allowed(command, Set.of("type", "fields"), "dynamic command");
            text(command, "type", "dynamic command");
            requireObject(required(command, "fields", "dynamic command"), "dynamic command fields");
        }

        private static void validateAccess(JsonNode observation, String id) {
            rejectKindFields(observation, id, Set.of("phase", "outcome", "error", "command", "violation"));
            JsonNode access = required(observation, "access", "dynamic observation " + id);
            requireObject(access, "dynamic access");
            allowed(access, Set.of("aggregate", "id", "mode"), "dynamic access");
            text(access, "aggregate", "dynamic access");
            optionalText(access, "id", "dynamic access");
            String mode = text(access, "mode", "dynamic access");
            if (!Set.of("read", "write").contains(mode)) throw invalid("invalid dynamic aggregate access mode " + mode);
        }

        private static void validateViolation(JsonNode observation, String id) {
            rejectKindFields(observation, id, Set.of("phase", "outcome", "error", "command", "access"));
            JsonNode violation = required(observation, "violation", "dynamic observation " + id);
            requireObject(violation, "dynamic violation");
            allowed(violation, Set.of("type", "message"), "dynamic violation");
            text(violation, "type", "dynamic violation");
            optionalText(violation, "message", "dynamic violation");
        }

        private static void validateError(JsonNode observation, String id) {
            JsonNode error = observation.path("error");
            requireObject(error, "dynamic observation " + id + " error");
            allowed(error, Set.of("type", "message"), "dynamic observation " + id + " error");
            String type = optionalText(error, "type", "dynamic observation " + id + " error");
            String message = optionalText(error, "message", "dynamic observation " + id + " error");
            if (type == null && message == null) throw invalid("dynamic observation " + id + " error must not be empty");
        }

        private static void rejectKindFields(JsonNode observation, String id, Set<String> incompatible) {
            for (String field : incompatible) if (observation.has(field)) {
                throw invalid("dynamic observation " + id + " kind " + observation.path("kind").asText()
                        + " has incompatible field " + field);
            }
        }

        private static String optionalText(JsonNode node, String field, String label) {
            if (!node.has(field) || node.path(field).isNull()) return null;
            JsonNode value = node.path(field);
            if (!value.isTextual() || value.asText().isBlank()) throw invalid(label + " field " + field + " must be non-blank text when present");
            return value.asText();
        }

        private static JsonNode required(JsonNode node, String field, String label) { JsonNode value = node.get(field); if (value == null || value.isNull()) throw invalid(label + " is missing required field " + field); return value; }
        private static String text(JsonNode node, String field, String label) { JsonNode value = required(node, field, label); if (!value.isTextual() || value.asText().isBlank()) throw invalid(label + " field " + field + " must be non-blank text"); return value.asText(); }
        private static void integral(JsonNode node, String field, String label) { if (!required(node, field, label).isIntegralNumber()) throw invalid(label + " field " + field + " must be an integer number"); }
        private static void integerMap(JsonNode node, String label) {
            requireObject(node, label);
            node.fields().forEachRemaining(entry -> {
                if (!entry.getValue().isIntegralNumber()) throw invalid(label + " value for " + entry.getKey() + " must be an integer number");
            });
        }
        private static void requireObject(JsonNode node, String label) { if (node == null || !node.isObject()) throw invalid(label + " must be an object"); }
        private static void allowed(JsonNode node, Set<String> fields, String label) {
            node.fieldNames().forEachRemaining(field -> { if (!fields.contains(field)) throw invalid(label + " contains unsupported field " + field); });
        }
        private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
        private static IllegalArgumentException invalid(String message, Throwable cause) { return new IllegalArgumentException(message, cause); }

        private static String sha256(Path path) throws IOException {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                try (InputStream input = Files.newInputStream(path)) { byte[] buffer = new byte[65536]; int read; while ((read = input.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read); }
                return java.util.HexFormat.of().formatHex(digest.digest());
            } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
        }
    }

    public record StaticPackageContents(
            ScenarioCatalogManifest.Current manifest,
            JsonNode accounting,
            List<JsonNode> sagaFacts,
            List<JsonNode> inputFacts,
            List<JsonNode> interactionFacts,
            Path accountingPath,
            Path sagaFactsPath,
            Path inputFactsPath,
            Path interactionFactsPath,
            JsonNode copyContracts,
            Path copyContractPath,
            List<JsonNode> dynamicObservations,
            List<JsonNode> dynamicAttributionLinks,
            Path dynamicObservationPath,
            Path dynamicAttributionPath) {
        public StaticPackageContents {
            sagaFacts = sagaFacts == null ? List.of() : List.copyOf(sagaFacts);
            inputFacts = inputFacts == null ? List.of() : List.copyOf(inputFacts);
            interactionFacts = interactionFacts == null ? List.of() : List.copyOf(interactionFacts);
            dynamicObservations = dynamicObservations == null ? List.of() : List.copyOf(dynamicObservations);
            dynamicAttributionLinks = dynamicAttributionLinks == null ? List.of() : List.copyOf(dynamicAttributionLinks);
        }
    }

    public record ExecutablePackageContents(
            ScenarioCatalogManifest.Current manifest,
            JsonNode accounting,
            List<JsonNode> sagaFacts,
            List<JsonNode> inputFacts,
            List<JsonNode> interactionFacts,
            JsonNode copyContracts,
            Path copyContractPath,
            List<JsonNode> setupRecords,
            List<JsonNode> workloadRecords,
            List<JsonNode> faultScenarioRecords,
            List<JsonNode> requestRecords,
            Path setupPath,
            Path workloadPath,
            Path faultScenarioPath,
            Path requestPath,
            Path accountingPath,
            List<JsonNode> dynamicObservations,
            List<JsonNode> dynamicAttributionLinks,
            Path dynamicObservationPath,
            Path dynamicAttributionPath) {
        public ExecutablePackageContents {
            sagaFacts = sagaFacts == null ? List.of() : List.copyOf(sagaFacts);
            inputFacts = inputFacts == null ? List.of() : List.copyOf(inputFacts);
            interactionFacts = interactionFacts == null ? List.of() : List.copyOf(interactionFacts);
            setupRecords = setupRecords == null ? List.of() : List.copyOf(setupRecords);
            workloadRecords = workloadRecords == null ? List.of() : List.copyOf(workloadRecords);
            faultScenarioRecords = faultScenarioRecords == null ? List.of() : List.copyOf(faultScenarioRecords);
            requestRecords = requestRecords == null ? List.of() : List.copyOf(requestRecords);
            dynamicObservations = dynamicObservations == null ? List.of() : List.copyOf(dynamicObservations);
            dynamicAttributionLinks = dynamicAttributionLinks == null ? List.of() : List.copyOf(dynamicAttributionLinks);
        }
    }

    public record SelectedPackageContents(
            ScenarioCatalogManifest manifest,
            WorkloadPlan workloadPlan,
            FaultScenario faultScenario,
            Path workloadCatalogPath,
            Path faultScenarioCatalogPath,
            Path accountingPath) {
    }

    public record PackageContents(
            ScenarioCatalogManifest manifest,
            List<WorkloadPlan> workloadPlans,
            List<FaultScenario> faultScenarios,
            JsonNode accounting,
            Path workloadCatalogPath,
            Path faultScenarioCatalogPath,
            Path accountingPath) {
        public PackageContents {
            workloadPlans = workloadPlans == null ? List.of() : List.copyOf(workloadPlans);
            faultScenarios = faultScenarios == null ? List.of() : List.copyOf(faultScenarios);
        }
    }
}
