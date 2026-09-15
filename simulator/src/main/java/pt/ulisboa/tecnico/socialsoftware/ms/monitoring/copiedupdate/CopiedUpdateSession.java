package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.*;

import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Attempt-scoped provenance for direct constructor copies. No application-specific mappings. */
public final class CopiedUpdateSession {
    static final ObjectMapper JSON = new ObjectMapper().findAndRegisterModules();
    private final List<Map<String, Object>> contracts;
    private final List<Map<String, Object>> rows = new ArrayList<>();
    private final List<String> gaps = new ArrayList<>();
    private final IdentityHashMap<Object, Origin> origins = new IdentityHashMap<>();
    private final IdentityHashMap<Object, List<Map<String, Object>>> constructed =
            new IdentityHashMap<>();
    private final Deque<Call> calls = new ArrayDeque<>();
    private final long ownerThread = Thread.currentThread().threadId();
    private final String attemptId;
    private boolean enabled = true;

    record Origin(long order, Object writer, String path, Map<String, Object> values) {}

    record Item(Object object, String path, Map<String, Object> values, Origin origin) {}

    record Call(
            long order,
            Object command,
            Object writer,
            Map<String, Item> inputs,
            IdentityHashMap<Object, List<Long>> admitted) {}

    public CopiedUpdateSession(String attemptId, List<Map<String, Object>> contracts) {
        this.attemptId = Objects.requireNonNull(attemptId);
        this.contracts = List.copyOf(contracts);
    }

    public synchronized void disable() {
        enabled = false;
    }

    public synchronized boolean isRecording() {
        return enabled;
    }

    public synchronized void gap(String reason) {
        if (!gaps.contains(reason)) gaps.add(reason);
    }

    private boolean active() {
        if (!enabled) return false;
        if (Thread.currentThread().threadId() != ownerThread) {
            gap("UNSUPPORTED_CONCURRENT_THREAD");
            return false;
        }
        var w = ImpactWriterContext.current().orElse(null);
        if (w != null && !attemptId.equals(w.executionAttemptId())) {
            gap("WRITER_ATTEMPT_MISMATCH");
            return false;
        }
        return true;
    }

    private long add(String kind, Map<String, Object> data) {
        long n = rows.size();
        rows.add(map("order", n, "kind", kind, "data", JSON.convertValue(data, Map.class)));
        return n;
    }

    static Object writer() {
        return ImpactWriterContext.current().orElse(null);
    }

    static Map<String, Object> map(Object... values) {
        var m = new LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2) m.put((String) values[i], values[i + 1]);
        return m;
    }

    public synchronized void begin(Object command) {
        if (!active()) return;
        try {
            var inputs = items(command);
            long n =
                    add(
                            "COMMAND_INPUT",
                            map(
                                    "command",
                                    command.getClass().getName(),
                                    "writer",
                                    writer(),
                                    "values",
                                    serialItems(inputs)));
            calls.push(new Call(n, command, writer(), inputs, new IdentityHashMap<>()));
        } catch (RuntimeException | LinkageError e) {
            gap(e);
            calls.push(new Call(-1, command, writer(), Map.of(), new IdentityHashMap<>()));
        }
    }

    public synchronized void inbound(Object command) {
        if (!active() || calls.isEmpty()) return;
        try {
            Call call = calls.peek();
            if (!command.getClass().equals(call.command().getClass()))
                throw new IllegalStateException("Transport command type mismatch");
            var received = items(command);
            for (var entry : received.entrySet()) {
                Item from = call.inputs().get(entry.getKey()), to = entry.getValue();
                if (from == null
                        || !from.object().getClass().equals(to.object().getClass())
                        || !from.values().equals(to.values()))
                    throw new IllegalStateException(
                            "Transport projection mismatch at " + entry.getKey());
            }
            if (!received.keySet().equals(call.inputs().keySet()))
                throw new IllegalStateException("Transport paths changed");
            for (var entry : received.entrySet()) {
                Item from = call.inputs().get(entry.getKey()), to = entry.getValue();
                call.admitted().computeIfAbsent(to.object(), ignored -> new ArrayList<>());
                if (from.origin() != null) {
                    origins.put(to.object(), from.origin());
                    long link =
                            add(
                                    "TRANSPORT_LINK",
                                    map(
                                            "call",
                                            call.order(),
                                            "path",
                                            entry.getKey(),
                                            "cloned",
                                            from.object() != to.object(),
                                            "readOrder",
                                            from.origin().order()));
                    call.admitted().get(to.object()).add(link);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            gap(e);
        }
    }

    public synchronized void end(Object result, Throwable failure) {
        if (!active() || calls.isEmpty()) return;
        try {
            Call call = calls.pop();
            if (failure != null) return;
            var returned = items(result);
            long n =
                    add(
                            "RESPONSE",
                            map(
                                    "call",
                                    call.order(),
                                    "writer",
                                    call.writer(),
                                    "command",
                                    call.command().getClass().getName(),
                                    "values",
                                    serialItems(returned)));
            for (var i : returned.values())
                origins.put(i.object(), new Origin(n, call.writer(), i.path(), i.values()));
        } catch (RuntimeException | LinkageError e) {
            gap(e);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void copied(Object target, Object[] args) {
        if (!active() || args.length != 1 || args[0] == null) return;
        try {
            Object source = args[0];
            if (contracts.stream()
                    .noneMatch(
                            c ->
                                    c.get("sourceType").equals(source.getClass().getName())
                                            && c.get("targetType")
                                                    .equals(target.getClass().getName()))) return;
            if (calls.isEmpty()) {
                gap("MISSING_COMMAND_SCOPE:" + source.getClass().getName());
                return;
            }
            Origin o = origins.get(source);
            if (!calls.peek().admitted().containsKey(source)) {
                gap("UNADMITTED_CONSTRUCTOR_INPUT");
                return;
            }
            if (o == null) {
                gap("MISSING_INPUT_ORIGIN:" + source.getClass().getName());
                return;
            }
            for (var c : contracts) {
                if (!c.get("sourceType").equals(source.getClass().getName())
                        || !c.get("targetType").equals(target.getClass().getName())) continue;
                Map<String, String> fields = (Map<String, String>) c.get("fields");
                var values = new LinkedHashMap<String, Object>();
                for (var f : fields.entrySet()) {
                    Object input = field(source, f.getKey()), output = field(target, f.getValue());
                    if (Objects.equals(input, o.values().get(f.getKey()))
                            && Objects.equals(input, output)) values.put(f.getValue(), input);
                    else
                        gap(
                                "CHANGED_OR_UNSUPPORTED_COPY:"
                                        + source.getClass().getName()
                                        + "."
                                        + f.getKey());
                }
                long n =
                        add(
                                "CONSTRUCTOR_COPY",
                                map(
                                        "call",
                                        calls.peek().order(),
                                        "readOrder",
                                        o.order(),
                                        "readWriter",
                                        o.writer(),
                                        "readPath",
                                        o.path(),
                                        "transportLinkOrders",
                                        List.copyOf(calls.peek().admitted().get(source)),
                                        "targetType",
                                        c.get("targetType"),
                                        "sourceType",
                                        c.get("sourceType"),
                                        "keyField",
                                        c.get("targetKey"),
                                        "key",
                                        field(target, (String) c.get("targetKey")),
                                        "values",
                                        values,
                                        "writer",
                                        writer()));
                constructed
                        .computeIfAbsent(target, k -> new ArrayList<>())
                        .add(map("copyOrder", n, "contract", c, "values", values));
            }
        } catch (RuntimeException | LinkageError e) {
            gap(e);
        }
    }

    @SuppressWarnings("unchecked")
    public synchronized void registered(Object value) {
        if (!active() || !(value instanceof Aggregate a)) return;
        try {
            walk(
                    value,
                    "$",
                    new IdentityHashMap<>(),
                    0,
                    (object, path) -> {
                        for (var copy : constructed.getOrDefault(object, List.of())) {
                            var c = (Map<String, Object>) copy.get("contract");
                            var values = (Map<String, Object>) copy.get("values");
                            var retained = new LinkedHashMap<String, Object>();
                            values.forEach(
                                    (f, v) -> {
                                        if (Objects.equals(field(object, f), v)) retained.put(f, v);
                                    });
                            add(
                                    "REGISTERED_COPY",
                                    map(
                                            "copyOrder",
                                            copy.get("copyOrder"),
                                            "aggregateId",
                                            a.getAggregateId(),
                                            "aggregateIdentity",
                                            new ImpactEvidence.AggregateIdentity(
                                                    a.getAggregateType() == null
                                                            ? a.getClass().getSimpleName()
                                                            : a.getAggregateType(),
                                                    a.getAggregateId()),
                                            "version",
                                            a.getVersion(),
                                            "path",
                                            path,
                                            "values",
                                            retained,
                                            "writer",
                                            writer()));
                        }
                    });
        } catch (RuntimeException | LinkageError e) {
            gap(e);
        }
    }

    public synchronized void committed(
            ImpactEvidence.AggregateSnapshot a, ImpactEvidence.Writer w) {
        if (active()) add("COMMITTED_WRITE", map("aggregate", a, "writer", w));
    }

    /** Freeze serializable evidence, then release every retained application object. */
    public synchronized Map<String, Object> finish() {
        if (!calls.isEmpty()) gap("UNCLOSED_COMMAND_SCOPE");
        enabled = false;
        Map<String, Object> trace =
                map("contracts", contracts, "events", List.copyOf(rows), "gaps", List.copyOf(gaps));
        origins.clear();
        constructed.clear();
        calls.clear();
        rows.clear();
        return trace;
    }

    private void gap(Throwable e) {
        gap(e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    private Map<String, Object> serialItems(Map<String, Item> items) {
        var m = new TreeMap<String, Object>();
        items.forEach(
                (p, i) ->
                        m.put(
                                p,
                                map(
                                        "type",
                                        i.object().getClass().getName(),
                                        "values",
                                        i.values(),
                                        "readOrder",
                                        i.origin() == null ? null : i.origin().order())));
        return m;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Item> items(Object root) {
        var result = new TreeMap<String, Item>();
        walk(
                root,
                "$",
                new IdentityHashMap<>(),
                0,
                (o, path) -> {
                    var fields = new TreeSet<String>();
                    for (var c : contracts)
                        if (c.get("sourceType").equals(o.getClass().getName()))
                            fields.addAll(((Map<String, String>) c.get("fields")).keySet());
                    if (fields.isEmpty()) return;
                    var values = new TreeMap<String, Object>();
                    for (String f : fields) values.put(f, field(o, f));
                    if (result.put(path, new Item(o, path, values, origins.get(o))) != null)
                        throw new IllegalStateException("Duplicate graph path");
                });
        return result;
    }

    interface Visit {
        void accept(Object value, String path);
    }

    private void walk(
            Object o, String path, IdentityHashMap<Object, Boolean> seen, int depth, Visit visit) {
        if (o == null || scalar(o.getClass()) || seen.put(o, true) != null) return;
        try {
            if (depth > 10)
                throw new IllegalStateException("Object graph depth exceeds supported limit");
            if (o instanceof Collection<?> collection) {
                var keys = new HashSet<String>();
                int index = 0;
                for (Object child : collection) {
                    String key = key(child);
                    if (key == null) {
                        if (o instanceof List<?>) key = "index=" + index;
                        else throw new IllegalStateException("Unkeyed collection");
                    }
                    if (!keys.add(key))
                        throw new IllegalStateException("Duplicate collection identity");
                    walk(child, path + "[" + key + "]", seen, depth + 1, visit);
                    index++;
                }
                return;
            }
            String pkg = o.getClass().getPackageName();
            if (pkg.startsWith("java.")
                    || pkg.startsWith("org.")
                    || pkg.startsWith("com.fasterxml.")) return;
            if (pkg.startsWith("pt.ulisboa.tecnico.socialsoftware.ms.")
                    && !(o instanceof Command)
                    && !(o instanceof Aggregate)) return;
            visit.accept(o, path);
            for (Field f : fields(o.getClass())) {
                if (Modifier.isStatic(f.getModifiers()) || f.isSynthetic()) continue;
                // Framework state is not a user input graph; inspect application-owned aggregate
                // fields only.
                if (o instanceof Aggregate
                        && f.getDeclaringClass()
                                .getPackageName()
                                .startsWith("pt.ulisboa.tecnico.socialsoftware.ms.")) continue;
                try {
                    f.setAccessible(true);
                    walk(f.get(o), path + "." + f.getName(), seen, depth + 1, visit);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        } finally {
            seen.remove(o);
        }
    }

    static boolean scalar(Class<?> c) {
        return c.isPrimitive()
                || Enum.class.isAssignableFrom(c)
                || Number.class.isAssignableFrom(c)
                || c == String.class
                || c == Boolean.class
                || c == Character.class
                || c.getPackageName().startsWith("java.time");
    }

    private String key(Object o) {
        if (o == null) return "null";
        if (scalar(o.getClass())) return "value=" + o;
        for (var c : contracts) {
            String f =
                    c.get("sourceType").equals(o.getClass().getName())
                            ? (String) c.get("sourceKey")
                            : c.get("targetType").equals(o.getClass().getName())
                                    ? (String) c.get("targetKey")
                                    : null;
            if (f != null) return f + "=" + field(o, f);
        }
        // Other DTO identities use the simulator aggregate identity convention, never array
        // position for sets.
        try {
            Object id = field(o, "aggregateId");
            return id == null ? null : "aggregateId=" + id;
        } catch (RuntimeException e) {
            return null;
        }
    }

    static List<Field> fields(Class<?> c) {
        var out = new ArrayList<Field>();
        for (; c != null && c != Object.class; c = c.getSuperclass())
            out.addAll(Arrays.asList(c.getDeclaredFields()));
        out.sort(Comparator.comparing(Field::getName));
        return out;
    }

    static Object field(Object o, String name) {
        for (Field f : fields(o.getClass()))
            if (f.getName().equals(name))
                try {
                    f.setAccessible(true);
                    return f.get(o);
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
        throw new IllegalStateException("No field " + name + " on " + o.getClass());
    }
}
