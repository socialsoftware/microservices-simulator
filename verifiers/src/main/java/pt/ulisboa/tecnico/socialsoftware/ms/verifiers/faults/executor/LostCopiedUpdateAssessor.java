package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Applies the generic lost-copied-update rule to one attempt-scoped runtime trace.
 * The input is converted to a JSON tree so runtime map implementations and Java number
 * classes cannot affect attribution or value comparison.
 */
public final class LostCopiedUpdateAssessor {
    private static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private static final Comparator<ImpactEvidence.AggregateIdentity> IDENTITY_ORDER =
            Comparator.comparing(ImpactEvidence.AggregateIdentity::aggregateType,
                            Comparator.nullsFirst(String::compareTo))
                    .thenComparing(ImpactEvidence.AggregateIdentity::aggregateId,
                            Comparator.nullsFirst(Integer::compareTo));

    public LostCopiedUpdateReport assess(Map<String, Object> trace,
                                         List<ImpactEvidence.AggregateSnapshot> baseline) {
        JsonNode root = JSON.valueToTree(trace == null ? Map.of() : trace);
        List<LostCopiedUpdateReport.CoverageGap> gaps = traceGaps(root.path("gaps"));
        List<Event> events = events(root.path("events"), gaps);
        Map<Long, Event> byOrder = new HashMap<>();
        for (Event event : events) {
            if (byOrder.putIfAbsent(event.order(), event) != null) {
                gap(gaps, event.order(), "TRACE", String.valueOf(event.order()),
                        "DUPLICATE_EVENT_ORDER", "Trace event order is not unique");
            }
        }

        List<JsonNode> contracts = array(root.path("contracts"));
        if (contracts.isEmpty()) {
            gap(gaps, 0, "CONTRACT", null, "COPY_CONTRACTS_UNAVAILABLE",
                    "No inferred copy contracts were supplied");
        }
        List<Event> writes = events.stream().filter(event -> "COMMITTED_WRITE".equals(event.kind()))
                .sorted(Comparator.comparingLong(Event::order)).toList();
        Map<GroupKey, MutableFinding> grouped = new TreeMap<>();

        for (Event registeredEvent : events) {
            if (!"REGISTERED_COPY".equals(registeredEvent.kind())) continue;
            assessRegistered(registeredEvent, byOrder, writes, contracts,
                    baseline == null ? List.of() : baseline, grouped, gaps);
        }

        List<LostCopiedUpdateReport.Finding> findings = grouped.values().stream()
                .map(MutableFinding::report).toList();
        List<LostCopiedUpdateReport.CoverageGap> uniqueGaps = gaps.stream()
                .distinct().sorted(Comparator.comparingLong(LostCopiedUpdateReport.CoverageGap::order)
                        .thenComparing(LostCopiedUpdateReport.CoverageGap::stage,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(LostCopiedUpdateReport.CoverageGap::reason,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(LostCopiedUpdateReport.CoverageGap::subject,
                                Comparator.nullsFirst(String::compareTo))).toList();
        return new LostCopiedUpdateReport(null, findings, uniqueGaps, null);
    }

    private void assessRegistered(Event registration, Map<Long, Event> indexed, List<Event> writes,
                                  List<JsonNode> contracts,
                                  List<ImpactEvidence.AggregateSnapshot> baseline,
                                  Map<GroupKey, MutableFinding> grouped,
                                  List<LostCopiedUpdateReport.CoverageGap> gaps) {
        JsonNode registered = registration.data();
        ImpactEvidence.AggregateIdentity identity = identity(registered.path("aggregateIdentity"));
        if (identity == null) {
            gap(gaps, registration.order(), "REGISTRATION", null,
                    "AGGREGATE_IDENTITY_UNAVAILABLE", "Registered copy lacks a complete aggregate identity");
            return;
        }
        Long version = longValue(registered.get("version"));
        JsonNode registeredWriter = registered.path("writer");
        if (version == null || !registeredWriter.isObject()) {
            gap(gaps, registration.order(), "REGISTRATION", identity.toString(),
                    "COMMIT_MATCH_METADATA_UNAVAILABLE", "Registered copy lacks version or writer metadata");
            return;
        }

        Long copyOrder = longValue(registered.get("copyOrder"));
        Event construction = copyOrder == null ? null : indexed.get(copyOrder);
        if (construction == null || !"CONSTRUCTOR_COPY".equals(construction.kind())) {
            gap(gaps, registration.order(), "ORIGIN", identity.toString(),
                    "CONSTRUCTOR_ORIGIN_UNAVAILABLE", "Registered copy has no constructor-origin observation");
            return;
        }
        JsonNode copy = construction.data();
        Long readOrder = longValue(copy.get("readOrder"));
        Long inputOrder = longValue(copy.get("call"));
        Event read = readOrder == null ? null : indexed.get(readOrder);
        Event input = inputOrder == null ? null : indexed.get(inputOrder);
        if (read == null || !"RESPONSE".equals(read.kind()) || input == null
                || !"COMMAND_INPUT".equals(input.kind())
                || !(read.order() < input.order() && input.order() < construction.order())) {
            gap(gaps, registration.order(), "ORIGIN", identity.toString(),
                    "RESPONSE_INPUT_COPY_CHAIN_UNAVAILABLE", "Missing or unordered response/input/copy chain");
            return;
        }

        List<Event> matchingWrites = writes.stream().filter(write -> {
            JsonNode aggregate = write.data().path("aggregate");
            return identity.equals(identity(aggregate.path("identity")))
                    && Objects.equals(version, longValue(aggregate.get("version")))
                    && equivalent(registeredWriter, write.data().path("writer"));
        }).toList();
        // Registration can be followed by transaction rollback. With no committed write,
        // there is no candidate committed overwrite and therefore no coverage claim to make.
        if (matchingWrites.isEmpty()) return;
        if (matchingWrites.size() != 1) {
            gap(gaps, registration.order(), "COMMIT", identity.toString(),
                    "AMBIGUOUS_COMMITTED_WRITE", "Registered copy has multiple matching committed writes");
            return;
        }
        Event after = matchingWrites.get(0);
        if (registration.order() <= construction.order() || registration.order() >= after.order()) {
            gap(gaps, registration.order(), "COMMIT", identity.toString(),
                    "COPY_REGISTRATION_COMMIT_ORDER_INVALID",
                    "Constructor copy, aggregate registration and committed write are not ordered");
            return;
        }

        Actor owner = actor(after.data().path("writer"));
        Actor reader = actor(copy.path("readWriter"));
        if (owner == null || reader == null) {
            gap(gaps, registration.order(), "WRITER", identity.toString(),
                    "WRITER_ATTRIBUTION_UNAVAILABLE", "Copy reader or overwriting writer is not attributable");
            return;
        }
        if (!owner.equals(reader)) return;

        List<Event> history = writes.stream().filter(write -> write.order() < after.order()
                && identity.equals(identity(write.data().path("aggregate").path("identity")))).toList();
        if (history.isEmpty()) return;
        Event foreign = history.get(history.size() - 1);
        Actor foreignActor = actor(foreign.data().path("writer"));
        if (foreignActor == null) {
            gap(gaps, foreign.order(), "WRITER", identity.toString(),
                    "FOREIGN_WRITER_ATTRIBUTION_UNAVAILABLE", "Immediate predecessor writer is not attributable");
            return;
        }
        if (foreignActor.equals(owner) || foreign.order() <= read.order()) return;

        String sourceType = text(copy.get("sourceType"));
        String targetType = text(copy.get("targetType"));
        if (sourceType == null || targetType == null) {
            gap(gaps, construction.order(), "CONTRACT", identity.toString(),
                    "COPY_TYPE_METADATA_UNAVAILABLE", "Constructor copy lacks its source or target type");
            return;
        }
        List<JsonNode> matchingContracts = contracts.stream().filter(contract ->
                Objects.equals(sourceType, text(contract.get("sourceType")))
                        && Objects.equals(targetType, text(contract.get("targetType")))).toList();
        if (matchingContracts.size() != 1) {
            gap(gaps, construction.order(), "CONTRACT", identity.toString(),
                    matchingContracts.isEmpty() ? "COPY_CONTRACT_UNAVAILABLE" : "AMBIGUOUS_COPY_CONTRACT",
                    "Constructor copy does not have exactly one inferred contract");
            return;
        }

        JsonNode contract = matchingContracts.get(0);
        if (!validSourceChain(copy, read, input, construction.order(), indexed.values(), contract)) {
            gap(gaps, construction.order(), "ORIGIN", identity.toString(),
                    "DISCONNECTED_RESPONSE_INPUT_COPY_CHAIN",
                    "Response, transported command input and constructor copy do not form one observed value chain");
            return;
        }
        String path = text(registered.get("path"));
        if (path == null) {
            gap(gaps, registration.order(), "PERSISTED_PATH", identity.toString(),
                    "PERSISTED_PATH_UNAVAILABLE", "Registered copy lacks its persisted path");
            return;
        }
        JsonNode priorAggregate = priorAggregate(history, foreign, identity, baseline);
        if (priorAggregate == null) {
            gap(gaps, foreign.order(), "PERSISTED_PATH", identity.toString(),
                    "PREDECESSOR_SNAPSHOT_UNAVAILABLE", "No baseline or preceding committed snapshot is available");
            return;
        }

        PathResult prior = locate(priorAggregate.path("applicationData"), path);
        PathResult before = locate(foreign.data().path("aggregate").path("applicationData"), path);
        PathResult afterValues = locate(after.data().path("aggregate").path("applicationData"), path);
        if (prior.absentEntry() || before.absentEntry() || afterValues.absentEntry()) return;
        if (prior.error() != null || before.error() != null || afterValues.error() != null) {
            gap(gaps, registration.order(), "PERSISTED_PATH", identity.toString(),
                    "PERSISTED_PATH_UNAVAILABLE", firstError(prior, before, afterValues));
            return;
        }

        JsonNode registeredValues = registered.path("values");
        JsonNode fields = contract.path("fields");
        String aggregateIdField = mappedField(fields, "aggregateId");
        String versionField = mappedField(fields, "version");
        if (!registeredValues.isObject()) {
            gap(gaps, registration.order(), "COPY_VALUE", identity.toString(),
                    "REGISTERED_VALUES_UNAVAILABLE", "Registered copy has no copied-value object");
            return;
        }
        registeredValues.fieldNames().forEachRemaining(field -> {
            if (field.equals(aggregateIdField) || field.equals(versionField)) return;
            JsonNode old = registeredValues.get(field);
            JsonNode constructedValue = copy.path("values").get(field);
            JsonNode priorValue = prior.value().get(field);
            JsonNode beforeValue = before.value().get(field);
            JsonNode afterValue = afterValues.value().get(field);
            if (constructedValue == null || priorValue == null || beforeValue == null || afterValue == null) return;
            if (!equivalent(constructedValue, old)) return;
            if (!equivalent(priorValue, old) || equivalent(beforeValue, old) || !equivalent(afterValue, old)) return;

            ImpactEvidence.Writer overwriteWriter = writer(after.data().path("writer"));
            ImpactEvidence.Writer predecessorWriter = writer(foreign.data().path("writer"));
            String attemptId = overwriteWriter == null ? null : overwriteWriter.executionAttemptId();
            GroupKey key = new GroupKey(identity, version, attemptId);
            MutableFinding finding = grouped.computeIfAbsent(key,
                    ignored -> new MutableFinding(key, longValue(foreign.data().path("aggregate").get("version")),
                            text(after.data().path("writer").get("phase")), overwriteWriter));
            finding.add(new LostCopiedUpdateReport.FieldEvidence(path + "." + field,
                    old.deepCopy(), beforeValue.deepCopy(), read.order(), input.order(), construction.order(),
                    foreign.order(), after.order(), predecessorWriter));
        });
    }

    private boolean validSourceChain(JsonNode copy, Event read, Event input, long constructionOrder,
                                     java.util.Collection<Event> events, JsonNode contract) {
        String sourceType = text(copy.get("sourceType"));
        String readPath = text(copy.get("readPath"));
        Long readOrder = longValue(copy.get("readOrder"));
        Long callOrder = longValue(copy.get("call"));
        JsonNode copiedValues = copy.path("values");
        if (sourceType == null || readPath == null || readOrder == null || callOrder == null
                || !copiedValues.isObject() || !contract.path("fields").isObject()) return false;
        if (!equivalent(copy.path("readWriter"), read.data().path("writer"))) return false;
        Actor reader = actor(copy.path("readWriter"));
        if (reader == null || !reader.equals(actor(copy.path("writer")))
                || !reader.equals(actor(input.data().path("writer")))) return false;

        JsonNode responseItem = read.data().path("values").get(readPath);
        if (!sourceItemMatches(responseItem, sourceType, copiedValues, contract)) return false;

        List<Map.Entry<String, JsonNode>> inputItems = new ArrayList<>();
        JsonNode values = input.data().path("values");
        if (!values.isObject()) return false;
        values.fields().forEachRemaining(entry -> {
            JsonNode item = entry.getValue();
            if (Objects.equals(readOrder, longValue(item.get("readOrder")))
                    && sourceItemMatches(item, sourceType, copiedValues, contract)) {
                inputItems.add(Map.entry(entry.getKey(), item));
            }
        });
        if (inputItems.isEmpty()) return false;
        var inputPaths = inputItems.stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.toSet());
        var retainedLinkOrders = new java.util.HashSet<Long>();
        JsonNode recordedLinks = copy.path("transportLinkOrders");
        if (!recordedLinks.isArray()) return false;
        for (JsonNode recordedLink : recordedLinks) {
            Long order = longValue(recordedLink);
            if (order == null) return false;
            retainedLinkOrders.add(order);
        }
        if (retainedLinkOrders.isEmpty()) return false;

        long links = events.stream().filter(event -> "TRANSPORT_LINK".equals(event.kind()))
                .filter(event -> retainedLinkOrders.contains(event.order()))
                .filter(event -> Objects.equals(callOrder, longValue(event.data().get("call"))))
                .filter(event -> Objects.equals(readOrder, longValue(event.data().get("readOrder"))))
                .filter(event -> inputPaths.contains(text(event.data().get("path"))))
                .filter(event -> input.order() < event.order() && event.order() < constructionOrder)
                .count();
        return links >= 1;
    }

    private boolean sourceItemMatches(JsonNode item, String sourceType,
                                      JsonNode copiedValues, JsonNode contract) {
        if (item == null || !item.isObject() || !Objects.equals(sourceType, text(item.get("type")))) return false;
        JsonNode sourceValues = item.path("values");
        JsonNode fields = contract.path("fields");
        if (!sourceValues.isObject()) return false;
        var copiedFields = copiedValues.fields();
        while (copiedFields.hasNext()) {
            Map.Entry<String, JsonNode> copied = copiedFields.next();
            String sourceField = null;
            var mappings = fields.fields();
            while (mappings.hasNext()) {
                Map.Entry<String, JsonNode> mapping = mappings.next();
                if (Objects.equals(copied.getKey(), text(mapping.getValue()))) {
                    if (sourceField != null) return false;
                    sourceField = mapping.getKey();
                }
            }
            if (sourceField == null || sourceValues.get(sourceField) == null
                    || !equivalent(sourceValues.get(sourceField), copied.getValue())) return false;
        }
        return true;
    }

    private JsonNode priorAggregate(List<Event> history, Event foreign,
                                    ImpactEvidence.AggregateIdentity identity,
                                    List<ImpactEvidence.AggregateSnapshot> baseline) {
        Event preceding = null;
        for (Event write : history) if (write.order() < foreign.order()) preceding = write;
        if (preceding != null) return preceding.data().path("aggregate");
        List<ImpactEvidence.AggregateSnapshot> matches = baseline.stream()
                .filter(snapshot -> identity.equals(snapshot.identity())).toList();
        return matches.size() == 1 ? JSON.valueToTree(matches.get(0)) : null;
    }

    private List<Event> events(JsonNode node, List<LostCopiedUpdateReport.CoverageGap> gaps) {
        List<Event> result = new ArrayList<>();
        if (!node.isArray()) {
            gap(gaps, 0, "TRACE", null, "EVENTS_UNAVAILABLE", "Trace events are unavailable");
            return result;
        }
        int index = 0;
        for (JsonNode event : node) {
            Long order = longValue(event.get("order"));
            String kind = text(event.get("kind"));
            JsonNode data = event.path("data");
            if (order == null || kind == null || !data.isObject()) {
                gap(gaps, index, "TRACE", String.valueOf(index), "MALFORMED_EVENT",
                        "Trace event lacks order, kind or object data");
            } else {
                result.add(new Event(order, kind, data));
            }
            index++;
        }
        result.sort(Comparator.comparingLong(Event::order));
        return result;
    }

    private List<LostCopiedUpdateReport.CoverageGap> traceGaps(JsonNode node) {
        List<LostCopiedUpdateReport.CoverageGap> result = new ArrayList<>();
        if (!node.isArray() && !node.isMissingNode()) {
            gap(result, 0, "TRACE", null, "MALFORMED_COVERAGE_GAPS", "Trace gaps must be an array");
            return result;
        }
        for (JsonNode value : node) {
            if (value.isTextual()) {
                gap(result, 0, "TRACE", null, "RUNTIME_COVERAGE_GAP", value.textValue());
            } else if (value.isObject()) {
                Long order = longValue(value.get("order"));
                gap(result, order == null ? 0 : order, defaultText(value.get("stage"), "TRACE"),
                        text(value.get("subject")), defaultText(value.get("reason"), "RUNTIME_COVERAGE_GAP"),
                        text(value.get("message")));
            } else {
                gap(result, 0, "TRACE", null, "RUNTIME_COVERAGE_GAP", value.toString());
            }
        }
        return result;
    }

    private PathResult locate(JsonNode root, String path) {
        if (path == null || !path.startsWith("$")) return PathResult.error("Unsupported root path");
        JsonNode value = root;
        if ("$".equals(path)) return PathResult.value(value);
        int offset = 1;
        while (offset < path.length()) {
            if (path.charAt(offset) != '.') return PathResult.error("Unsupported path separator at " + offset);
            int next = nextDot(path, offset + 1);
            String segment = path.substring(offset + 1, next < 0 ? path.length() : next);
            int bracket = segment.indexOf('[');
            String name = bracket < 0 ? segment : segment.substring(0, bracket);
            value = value == null ? MissingNode.getInstance() : value.get(name);
            if (value == null || value.isMissingNode()) return PathResult.error("Missing field " + name);
            if (bracket >= 0) {
                if (!segment.endsWith("]") || !value.isArray()) return PathResult.error("Invalid keyed collection " + segment);
                String selector = segment.substring(bracket + 1, segment.length() - 1);
                int equals = selector.indexOf('=');
                if (equals <= 0) return PathResult.error("Invalid collection key " + selector);
                String key = selector.substring(0, equals);
                String expected = selector.substring(equals + 1);
                List<JsonNode> matches = new ArrayList<>();
                for (JsonNode entry : value) {
                    JsonNode actual = entry.get(key);
                    if (actual != null && expected.equals(actual.isTextual() ? actual.textValue() : actual.asText())) {
                        matches.add(entry);
                    }
                }
                if (matches.isEmpty()) return PathResult.absent();
                if (matches.size() != 1) return PathResult.error("Missing or ambiguous collection key " + selector);
                value = matches.get(0);
            }
            if (next < 0) break;
            offset = next;
        }
        return PathResult.value(value);
    }

    private int nextDot(String path, int from) {
        int depth = 0;
        for (int index = from; index < path.length(); index++) {
            char character = path.charAt(index);
            if (character == '[') depth++;
            else if (character == ']') depth--;
            else if (character == '.' && depth == 0) return index;
        }
        return -1;
    }

    private boolean equivalent(JsonNode left, JsonNode right) {
        if (left == null || right == null) return left == right;
        if (left.isNumber() && right.isNumber()) {
            BigDecimal a = left.decimalValue();
            BigDecimal b = right.decimalValue();
            return a.compareTo(b) == 0;
        }
        if (left.isArray() && right.isArray()) {
            if (left.size() != right.size()) return false;
            for (int index = 0; index < left.size(); index++) {
                if (!equivalent(left.get(index), right.get(index))) return false;
            }
            return true;
        }
        if (left.isObject() && right.isObject()) {
            if (left.size() != right.size()) return false;
            var fields = left.fieldNames();
            while (fields.hasNext()) {
                String field = fields.next();
                if (!right.has(field) || !equivalent(left.get(field), right.get(field))) return false;
            }
            return true;
        }
        return left.equals(right);
    }

    private ImpactEvidence.AggregateIdentity identity(JsonNode node) {
        if (!node.isObject()) return null;
        String type = text(node.get("aggregateType"));
        Integer id = integerValue(node.get("aggregateId"));
        return type == null || id == null ? null : new ImpactEvidence.AggregateIdentity(type, id);
    }

    private ImpactEvidence.Writer writer(JsonNode node) {
        if (!node.isObject()) return null;
        try {
            return JSON.treeToValue(node, ImpactEvidence.Writer.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Actor actor(JsonNode writer) {
        String attempt = text(writer.get("executionAttemptId"));
        String saga = text(writer.get("sagaInstanceId"));
        return attempt == null || saga == null ? null : new Actor(attempt, saga);
    }

    private String mappedField(JsonNode fields, String sourceField) {
        return fields.isObject() ? text(fields.get(sourceField)) : null;
    }

    private List<JsonNode> array(JsonNode node) {
        if (!node.isArray()) return List.of();
        List<JsonNode> result = new ArrayList<>();
        node.forEach(result::add);
        return result;
    }

    private String firstError(PathResult... values) {
        for (PathResult value : values) if (value.error() != null) return value.error();
        return "Persisted path is unavailable";
    }

    private String text(JsonNode node) {
        return node == null || node.isNull() || !node.isValueNode() ? null : node.asText();
    }

    private String defaultText(JsonNode node, String fallback) {
        String value = text(node);
        return value == null ? fallback : value;
    }

    private Long longValue(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToLong() ? node.longValue() : null;
    }

    private Integer integerValue(JsonNode node) {
        return node != null && node.isIntegralNumber() && node.canConvertToInt() ? node.intValue() : null;
    }

    private void gap(List<LostCopiedUpdateReport.CoverageGap> gaps, long order, String stage,
                     String subject, String reason, String message) {
        gaps.add(new LostCopiedUpdateReport.CoverageGap(order, stage, subject, reason, message));
    }

    private record Event(long order, String kind, JsonNode data) {
    }

    private record Actor(String attemptId, String sagaInstanceId) {
    }

    private record PathResult(JsonNode value, boolean absentEntry, String error) {
        static PathResult value(JsonNode value) { return new PathResult(value, false, null); }
        static PathResult absent() { return new PathResult(null, true, null); }
        static PathResult error(String error) { return new PathResult(null, false, error); }
    }

    private record GroupKey(ImpactEvidence.AggregateIdentity identity, Long afterVersion,
                            String attemptId) implements Comparable<GroupKey> {
        @Override
        public int compareTo(GroupKey other) {
            int identityResult = IDENTITY_ORDER.compare(identity, other.identity);
            if (identityResult != 0) return identityResult;
            int versionResult = Comparator.nullsFirst(Long::compareTo).compare(afterVersion, other.afterVersion);
            return versionResult != 0 ? versionResult
                    : Comparator.nullsFirst(String::compareTo).compare(attemptId, other.attemptId);
        }
    }

    private static final class MutableFinding {
        private static final Comparator<LostCopiedUpdateReport.FieldEvidence> FIELD_ORDER =
                Comparator.comparing(LostCopiedUpdateReport.FieldEvidence::fieldPath)
                        .thenComparingLong(LostCopiedUpdateReport.FieldEvidence::readOrder)
                        .thenComparingLong(LostCopiedUpdateReport.FieldEvidence::constructorOrder)
                        .thenComparingLong(LostCopiedUpdateReport.FieldEvidence::foreignWriteOrder);
        private final GroupKey key;
        private final Long beforeVersion;
        private final String phase;
        private final ImpactEvidence.Writer writer;
        private final Map<String, LostCopiedUpdateReport.FieldEvidence> fields = new LinkedHashMap<>();

        private MutableFinding(GroupKey key, Long beforeVersion, String phase, ImpactEvidence.Writer writer) {
            this.key = key;
            this.beforeVersion = beforeVersion;
            this.phase = phase;
            this.writer = writer;
        }

        private void add(LostCopiedUpdateReport.FieldEvidence field) {
            fields.putIfAbsent(field.fieldPath(), field);
        }

        private LostCopiedUpdateReport.Finding report() {
            String source = key.identity().aggregateType() + "\n" + key.identity().aggregateId()
                    + "\n" + key.afterVersion() + "\n" + key.attemptId();
            String id = "lost-copied-update:"
                    + UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
            List<LostCopiedUpdateReport.FieldEvidence> ordered = fields.values().stream()
                    .sorted(FIELD_ORDER).toList();
            return new LostCopiedUpdateReport.Finding(id, key.identity(), beforeVersion,
                    key.afterVersion(), phase, writer, ordered);
        }
    }
}
