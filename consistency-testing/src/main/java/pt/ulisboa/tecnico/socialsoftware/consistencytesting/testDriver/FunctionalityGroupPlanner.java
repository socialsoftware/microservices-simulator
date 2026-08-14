package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityFootprint.Access;

/**
 * Given the solo-run footprints of a pool of functionalities,
 * decides which of them are worth testing concurrently.
 * <p>
 * Two functionalities CONFLICT when their footprints touch the same data and at
 * least one side writes it (write-write, or read-write in either direction).
 * Read-read overlaps are deliberately not conflicts, every inter-invariant
 * violation requires at least one write on shared data.
 * <p>
 * "Same data" is decided per {@link Access}: two handle-based accesses match on
 * equal handles, while a type-fallback access (an aggregate created mid-run,
 * unknown to the registry) matches as a wildcard (over-approximation) over
 * every access of the same aggregate type — see
 * {@link FunctionalityFootprint.Access}.
 * <p>
 * A functionality that writes anything conflicts with ITSELF, producing a
 * self-pair group: two same-argument instances racing each other (see
 * {@link FunctionalityGroup}).
 */
public final class FunctionalityGroupPlanner {

    private FunctionalityGroupPlanner() {
    }

    /**
     * Every conflicting pair (including self-pairs) of the given footprints, as
     * groups carrying their conflict evidence.
     */
    public static Set<FunctionalityGroup> planGroups(Collection<FunctionalityFootprint> footprints) {
        // Sort by id to ensure canonical order of the pairs (first <= second).
        List<FunctionalityFootprint> sorted = footprints.stream()
                .sorted(Comparator.comparing(footprint -> footprint.functionalityId().toString()))
                .toList();

        Set<FunctionalityGroup> groups = new LinkedHashSet<>();
        for (int i = 0; i < sorted.size(); i++) {
            for (int j = i; j < sorted.size(); j++) {
                FunctionalityFootprint first = sorted.get(i);
                FunctionalityFootprint second = sorted.get(j);

                Set<FunctionalityGroup.Conflict> conflicts = conflictsBetween(first, second);
                if (!conflicts.isEmpty()) {
                    groups.add(new FunctionalityGroup(
                            first.functionalityId(), second.functionalityId(), conflicts));
                }
            }
        }
        return groups;
    }

    /**
     * The per-identity conflict evidence between the two footprints (empty when
     * they do not conflict). Called with {@code first == second} for the
     * self-pair check, which then reduces to "does it write anything".
     */
    private static Set<FunctionalityGroup.Conflict> conflictsBetween(
            FunctionalityFootprint first, FunctionalityFootprint second) {

        Map<String, Set<StepEffect.EffectKind>> firstKindsByIdentity = new LinkedHashMap<>();
        Map<String, Set<StepEffect.EffectKind>> secondKindsByIdentity = new LinkedHashMap<>();

        for (Access firstAccess : first.accesses()) {
            for (Access secondAccess : second.accesses()) {
                if (!touchSameData(firstAccess, secondAccess)
                        || (!firstAccess.isWrite() && !secondAccess.isWrite())) {
                    continue;
                }

                // For wildcard matches, prefer the handle side as the evidence
                // identity: it names a concrete aggregate rather than a type.
                String identity = firstAccess.handleBased() ? firstAccess.identity() : secondAccess.identity();

                firstKindsByIdentity.computeIfAbsent(identity, key -> new LinkedHashSet<>())
                        .add(firstAccess.kind());
                secondKindsByIdentity.computeIfAbsent(identity, key -> new LinkedHashSet<>())
                        .add(secondAccess.kind());
            }
        }

        Set<FunctionalityGroup.Conflict> conflicts = new LinkedHashSet<>();
        for (String identity : firstKindsByIdentity.keySet()) {
            conflicts.add(new FunctionalityGroup.Conflict(
                    identity,
                    firstKindsByIdentity.get(identity),
                    secondKindsByIdentity.get(identity)));
        }
        return conflicts;
    }

    // TODO see if too permisive and should only let wildcards match with wildcards
    private static boolean touchSameData(Access a, Access b) {
        if (a.handleBased() && b.handleBased()) {
            return a.identity().equals(b.identity());
        }
        // At least one side is a type-fallback access: wildcard over its type.
        return a.aggregateType().equals(b.aggregateType());
    }
}
