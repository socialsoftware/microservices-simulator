package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import java.math.BigInteger;
import java.util.*;

/** Exact uncapped counts for one selected input tuple, over all its normal schedules.
 * No WorkloadPlan or FaultScenario is materialised. Counts preserve workload identity,
 * including normal steps subsequently skipped by a fault. Not an executability check.
 */
public final class FaultScenarioStructureCounter {
    public record Counts(BigInteger workloads, BigInteger faultScenarios) {
        public Counts add(Counts other) { return new Counts(workloads.add(other.workloads), faultScenarios.add(other.faultScenarios)); }
        public Counts multiply(BigInteger n) { return new Counts(workloads.multiply(n), faultScenarios.multiply(n)); }
        public static Counts zero() { return new Counts(BigInteger.ZERO, BigInteger.ZERO); }
    }
    // Only scalar answers are cached across sets, never their DP tables.
    private final Map<String, Counts> cache = new HashMap<>();
    private final long maxStates;
    public FaultScenarioStructureCounter(long maxStates) {
        if (maxStates < 1) throw new IllegalArgumentException("maxStates must be positive");
        this.maxStates = maxStates;
    }
    public int cachedStructures() { return cache.size(); }

    public Counts count(List<SagaDefinition> definitions,
                        List<ConflictGraphBuilder.ConflictCandidate> anchors,
                        List<EventConsequenceDefinition> events,
                        ScenarioGeneratorConfig.ScheduleStrategy strategy, int maxDeliveries) {
        if (maxDeliveries < 1) throw new IllegalArgumentException("maxDeliveries must be positive");
        List<SagaDefinition> sagas = definitions.stream().sorted(Comparator.comparing(SagaDefinition::sagaFqn))
                .map(s -> new SagaDefinition(s.sagaFqn(), s.steps().stream()
                        .sorted(Comparator.comparingInt(StepDefinition::orderIndex)
                                .thenComparing(StepDefinition::deterministicId, Comparator.nullsFirst(String::compareTo))
                                .thenComparing(StepDefinition::stepKey, Comparator.nullsFirst(String::compareTo))
                                .thenComparing(StepDefinition::name, Comparator.nullsFirst(String::compareTo)))
                        .toList(), s.warnings())).toList();
        if (sagas.isEmpty() || sagas.stream().map(SagaDefinition::sagaFqn).distinct().count() != sagas.size())
            throw new IllegalArgumentException("Requires a nonempty set of distinct Saga types");
        int n = sagas.size();
        int[][] ends = new int[n][];
        boolean[][] checkpoints = new boolean[n][];
        for (int i = 0; i < n; i++) {
            var saga = sagas.get(i);
            checkpoints[i] = new boolean[saga.steps().size()];
            Set<String> ids = new HashSet<>();
            for (var a : anchors) {
                if (a.leftSagaFqn().equals(saga.sagaFqn())) ids.add(a.leftStepId());
                if (a.rightSagaFqn().equals(saga.sagaFqn())) ids.add(a.rightStepId());
            }
            var boundaries = new ArrayList<Integer>();
            for (int j = 0; j < saga.steps().size(); j++) {
                var step = saga.steps().get(j);
                checkpoints[i][j] = step.compensationEvidence() != null;
                if (strategy == ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING
                        || strategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
                        && ids.contains(ScenarioIdGenerator.stepDefinitionId(saga.sagaFqn(), step))) boundaries.add(j + 1);
            }
            ends[i] = boundaries.stream().mapToInt(Integer::intValue).toArray();
        }
        Counts total = compute(checkpoints, ends, -1, -1, 0);
        // Exactly the same grouping as ScenarioGenerator: one producer occurrence/site,
        // excluding a downstream Saga already present as an explicit participant.
        var groups = new TreeMap<String, RouteGroup>();
        var names = new HashSet<>(sagas.stream().map(SagaDefinition::sagaFqn).toList());
        for (var event : events) {
            if (event == null || event.emissionSite() == null || names.contains(event.downstreamSagaFqn())) continue;
            for (int i = 0; i < n; i++) {
                if (!sagas.get(i).sagaFqn().equals(event.triggerSagaFqn())) continue;
                var steps = sagas.get(i).steps();
                for (int j = 0; j < steps.size(); j++) {
                    var step = steps.get(j);
                    if (!Objects.equals(event.triggerStepKey(), ScenarioIdGenerator.stepDefinitionId(sagas.get(i).sagaFqn(), step))
                            && !Objects.equals(event.triggerStepKey(), sagas.get(i).sagaFqn() + "::" + step.name())) continue;
                    String key = i + ":" + j + ":" + event.emissionSite().deterministicId();
                    RouteGroup group = groups.get(key);
                    if (group == null) { group = new RouteGroup(i, j, new HashSet<>()); groups.put(key, group); }
                    group.routes.add(ScenarioIdGenerator.eventConsequenceId("count-trigger", event.emissionSite(),
                            event.eventHandlingClassFqn(), event.eventHandlingMethodName(), event.eventHandlerClassFqn(),
                            event.eventProcessingClassFqn(), event.eventProcessingMethodName(), event.facadeClassFqn(),
                            event.facadeMethodName(), event.downstreamSagaFqn(), event.deliveryPolicy()));
                    break;
                }
            }
        }
        for (var group : groups.values()) {
            BigInteger permutations = BigInteger.ONE;
            for (int k = 1; k <= Math.min(group.routes.size(), maxDeliveries); k++) {
                permutations = permutations.multiply(BigInteger.valueOf(group.routes.size() - k + 1));
                total = total.add(compute(checkpoints, ends, group.saga, group.step, k).multiply(permutations));
            }
        }
        return total;
    }
    private record RouteGroup(int saga, int step, Set<String> routes) {}

    private Counts compute(boolean[][] checkpoints, int[][] ends, int producer, int trigger, int deliveries) {
        String key = Arrays.deepToString(checkpoints) + Arrays.deepToString(ends) + ":" + producer + ":" + trigger + ":" + deliveries;
        Counts hit = cache.get(key);
        if (hit != null) return hit;
        BigInteger workloads = new Engine(checkpoints, ends, producer, trigger, deliveries, false, maxStates).run();
        boolean anyRecovery = false;
        for (boolean[] chain : checkpoints) for (boolean checkpoint : chain) anyRecovery |= checkpoint;
        BigInteger scenarios;
        if (anyRecovery) scenarios = new Engine(checkpoints, ends, producer, trigger, deliveries, true, maxStates).run();
        else {
            BigInteger vectors = BigInteger.ONE;
            for (boolean[] chain : checkpoints) vectors = vectors.multiply(BigInteger.valueOf(chain.length + 1L));
            scenarios = workloads.multiply(vectors);
        }
        Counts result = new Counts(workloads, scenarios);
        cache.put(key, result);
        return result;
    }

    /** Chooses the next visible normal action before interleaving recovery. This avoids
     * multiplying identical scenarios by positions around skipped forward slots. A
     * skipped slot still advances the normal schedule and can distinguish workloads.
     */
    private static final class Engine {
        final int n, producer, trigger, deliveries, eventIndex, pendingIndex;
        final int[] lengths, prefixLimit, radix;
        final int[][] ends, prefixCheckpoints;
        final long[] unit;
        final boolean faults;
        final long maxStates;
        final Map<Long, BigInteger> memo = new HashMap<>();
        Engine(boolean[][] checkpoints, int[][] ends, int producer, int trigger, int deliveries, boolean faults, long maxStates) {
            this.n = checkpoints.length; this.ends = ends; this.producer = producer; this.trigger = trigger;
            this.deliveries = deliveries; this.faults = faults; this.maxStates = maxStates;
            lengths = new int[n]; prefixLimit = new int[n]; prefixCheckpoints = new int[n][];
            eventIndex = 3 * n; pendingIndex = eventIndex + 1;
            radix = new int[pendingIndex + 1]; unit = new long[radix.length];
            for (int i = 0; i < n; i++) {
                lengths[i] = checkpoints[i].length;
                prefixLimit[i] = ends[i].length == 0 ? 0 : ends[i][ends[i].length - 1];
                prefixCheckpoints[i] = new int[lengths[i] + 1];
                for (int j = 0; j < lengths[i]; j++) prefixCheckpoints[i][j + 1] = prefixCheckpoints[i][j] + (checkpoints[i][j] ? 1 : 0);
                radix[i] = lengths[i] + 1;
                // Failure location only affects future event availability: before/at the
                // selected emission, or after it. Other chains need one failed bit.
                radix[n + i] = faults ? (i == producer ? 3 : 2) : 1;
                radix[2 * n + i] = faults ? prefixCheckpoints[i][lengths[i]] + 1 : 1;
            }
            radix[eventIndex] = deliveries + 1;
            // 0 chooses next; 1..n forward; n+1 event; n+2 terminal drain.
            radix[pendingIndex] = n + 3;
            long product = 1;
            for (int i = 0; i < radix.length; i++) {
                unit[i] = product;
                try { product = Math.multiplyExact(product, radix[i]); }
                catch (ArithmeticException e) { throw new IllegalArgumentException("Counting state exceeds 64-bit index capacity", e); }
            }
        }
        BigInteger run() { return count(0); }
        int get(long key, int i) { return (int) (key / unit[i] % radix[i]); }
        BigInteger count(long key) {
            BigInteger cached = memo.get(key);
            if (cached != null) return cached;
            if (Thread.currentThread().isInterrupted()) throw new IllegalStateException("Counting interrupted; no complete total available");
            if (memo.size() >= maxStates) throw new IllegalStateException("Counting state limit exceeded (" + maxStates + "); no complete total available");
            int pending = get(key, pendingIndex);
            BigInteger result = BigInteger.ZERO;
            if (pending == 0) {
                int[] positions = new int[n];
                for (int i = 0; i < n; i++) positions[i] = get(key, i);
                int locked = -1;
                for (int i = 0; i < n; i++) {
                    if (positions[i] > 0 && positions[i] < prefixLimit[i] && Arrays.binarySearch(ends[i], positions[i]) < 0) locked = i;
                }
                boolean prefixesDone = true;
                for (int i = 0; i < n; i++) if (positions[i] < prefixLimit[i]) prefixesDone = false;
                int tail = -1;
                if (prefixesDone) for (int i = 0; i < n; i++) if (positions[i] < lengths[i]) { tail = i; break; }
                boolean normalAvailable = false;
                for (int i = 0; i < n; i++) {
                    boolean enabled = prefixesDone ? i == tail : positions[i] < prefixLimit[i] && (locked < 0 || i == locked);
                    if (!enabled) continue;
                    normalAvailable = true;
                    long next = key + unit[i];
                    // Skipped steps have no action in the scenario, but remain in workload identity.
                    if (get(key, n + i) == 0) next += (i + 1L) * unit[pendingIndex];
                    result = result.add(count(next));
                }
                boolean eventAvailable = deliveries > get(key, eventIndex) && positions[producer] > trigger;
                if (eventAvailable) {
                    normalAvailable = true;
                    result = result.add(count(key + unit[eventIndex] + (n + 1L) * unit[pendingIndex]));
                }
                if (!normalAvailable) result = count(key + (n + 2L) * unit[pendingIndex]);
            } else {
                long next = key - pending * unit[pendingIndex];
                boolean forcedNoOp = pending == n + 1 && get(key, n + producer) == 1;
                boolean recovery = false;
                if (!forcedNoOp) for (int i = 0; i < n; i++) if (get(key, 2 * n + i) > 0) {
                    recovery = true;
                    result = result.add(count(key - unit[2 * n + i]));
                }
                if (pending <= n) {
                    int i = pending - 1;
                    result = result.add(count(next)); // success
                    if (faults) {
                        int step = get(key, i);
                        result = result.add(count(next + (i == producer && step > trigger + 1 ? 2 : 1) * unit[n + i] + prefixCheckpoints[i][step - 1] * unit[2 * n + i]));
                    }
                } else if (pending == n + 1) result = result.add(count(next));
                else if (!recovery) result = BigInteger.ONE;
            }
            memo.put(key, result);
            return result;
        }
    }
}
