package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect.EffectKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.FunctionalityFootprint.Access;

class FunctionalityGroupPlannerTest {

    private static final String TOURNAMENT_TYPE = "SagaTournament";
    private static final String USER_TYPE = "SagaUser";
    private static final String TOPIC_TYPE = "SagaTopic";

    private static FunctionalityId id(String name) {
        return FunctionalityId.forSagaFunctionality(name);
    }

    private static FunctionalityFootprint footprint(String name, Access... accesses) {
        return new FunctionalityFootprint(id(name), Set.of(accesses));
    }

    private static Access read(String handle, String type) {
        return new Access(handle, type, true, EffectKind.READ);
    }

    private static Access write(String handle, String type) {
        return new Access(handle, type, true, EffectKind.WRITE);
    }

    /** A read of an aggregate the functionality created mid-run (no handle). */
    private static Access createdRead(String type) {
        return new Access(type, type, false, EffectKind.READ);
    }

    /** A write of an aggregate the functionality created mid-run (no handle). */
    private static Access createdWrite(String type) {
        return new Access(type, type, false, EffectKind.WRITE);
    }

    private static Optional<FunctionalityGroup> pairOf(Set<FunctionalityGroup> groups, String a, String b) {
        return groups.stream().filter(group -> group.isPairOf(id(a), id(b))).findFirst();
    }

    private static FunctionalityGroup requirePair(Set<FunctionalityGroup> groups, String a, String b) {
        return pairOf(groups, a, b).orElseThrow(
                () -> new AssertionError("expected a group for the pair (%s, %s), got: %s".formatted(a, b, groups)));
    }

    /**
     * A pool where every pairing outcome is represented at least once:
     * <ul>
     * <li>updateA writes tournamentA; summaryA reads it — RW pair;</li>
     * <li>joinB and updateB both write tournamentB — WW pair, and neither
     * touches tournamentA;</li>
     * <li>browser only ever reads — RR everywhere, so it never pairs;</li>
     * <li>topicEditor is fully disjoint from the tournament crowd.</li>
     * </ul>
     */
    private static List<FunctionalityFootprint> tournamentCrowd() {
        return List.of(
                footprint("updateA", write("tournamentA", TOURNAMENT_TYPE)),
                footprint("summaryA",
                        read("tournamentA", TOURNAMENT_TYPE), read("userA", USER_TYPE)),
                footprint("updateB", write("tournamentB", TOURNAMENT_TYPE)),
                footprint("joinB",
                        read("userA", USER_TYPE), write("tournamentB", TOURNAMENT_TYPE)),
                footprint("browser",
                        read("tournamentA", TOURNAMENT_TYPE), read("tournamentB", TOURNAMENT_TYPE)),
                footprint("topicEditor", write("topicA", TOPIC_TYPE)));
    }

    @Test
    void readWriteConflictPairsAndCarriesEvidence() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(tournamentCrowd());

        assertEquals(
                Set.of(new FunctionalityGroup.Conflict(
                        "tournamentA", Set.of(EffectKind.READ), Set.of(EffectKind.WRITE))),
                requirePair(groups, "updateA", "summaryA").conflicts(),
                "evidence should carry the shared identity and each member's kinds");
    }

    @Test
    void writeWriteConflictPairs() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(tournamentCrowd());

        assertTrue(pairOf(groups, "updateB", "joinB").isPresent(), "WW on tournamentB should pair");
    }

    @Test
    void writersOfDifferentAggregatesOfTheSameTypeDoNotPair() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(tournamentCrowd());

        assertTrue(pairOf(groups, "updateA", "updateB").isEmpty(),
                "writers of different tournaments should NOT pair (handle-level pruning)");
        assertTrue(pairOf(groups, "updateA", "joinB").isEmpty(),
                "a write of tournamentA and a write of tournamentB share no identity");
    }

    @Test
    void readOnlyOverlapDoesNotPair() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(tournamentCrowd());

        assertTrue(pairOf(groups, "browser", "summaryA").isEmpty(),
                "RR-only overlap should not pair");
        assertTrue(pairOf(groups, "summaryA", "joinB").isEmpty(),
                "shared read of userA with no write on it should not pair");
    }

    @Test
    void disjointFunctionalityPairsWithNothingButItself() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(tournamentCrowd());

        assertTrue(groups.stream()
                .filter(group -> group.members().contains(id("topicEditor")))
                .allMatch(FunctionalityGroup::isSelfPair),
                "topicEditor shares no identity with the tournament crowd");
    }

    @Test
    void oneConflictIsReportedPerSharedIdentity() {
        // both members touch tournamentA AND tournamentB, so the group must carry
        // one piece of evidence per shared identity rather than a single merged one
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("archiver",
                        write("tournamentA", TOURNAMENT_TYPE), write("tournamentB", TOURNAMENT_TYPE)),
                footprint("reviewer",
                        read("tournamentA", TOURNAMENT_TYPE), write("tournamentB", TOURNAMENT_TYPE))));

        assertEquals(
                Set.of(
                        new FunctionalityGroup.Conflict(
                                "tournamentA", Set.of(EffectKind.WRITE), Set.of(EffectKind.READ)),
                        new FunctionalityGroup.Conflict(
                                "tournamentB", Set.of(EffectKind.WRITE), Set.of(EffectKind.WRITE))),
                requirePair(groups, "archiver", "reviewer").conflicts());
    }

    @Test
    void bothKindsAreRecordedWhenAMemberReadsAndWritesTheSameIdentity() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("editor",
                        read("tournamentA", TOURNAMENT_TYPE), write("tournamentA", TOURNAMENT_TYPE)),
                footprint("writer", write("tournamentA", TOURNAMENT_TYPE))));

        assertEquals(
                Set.of(new FunctionalityGroup.Conflict(
                        "tournamentA", Set.of(EffectKind.READ, EffectKind.WRITE), Set.of(EffectKind.WRITE))),
                requirePair(groups, "editor", "writer").conflicts(),
                "a member's kinds accumulate across all of its accesses to the identity");
    }

    @Test
    void selfPairsOnlyForWriters() {
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("updateA", write("tournamentA", TOURNAMENT_TYPE)),
                footprint("browser", read("tournamentA", TOURNAMENT_TYPE))));

        assertTrue(requirePair(groups, "updateA", "updateA").isSelfPair(),
                "a writing functionality should race a same-arguments copy of itself");
        assertTrue(pairOf(groups, "browser", "browser").isEmpty(),
                "a read-only functionality cannot conflict with itself");
    }

    @Test
    void selfPairEvidenceCoversWrittenIdentitiesOnly() {
        // the functionality reads userA and writes tournamentA: raced against a copy
        // of itself, only tournamentA is contended — the shared read of userA is not
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("editor",
                        read("userA", USER_TYPE), write("tournamentA", TOURNAMENT_TYPE))));

        assertEquals(
                Set.of(new FunctionalityGroup.Conflict(
                        "tournamentA", Set.of(EffectKind.WRITE), Set.of(EffectKind.WRITE))),
                requirePair(groups, "editor", "editor").conflicts());
    }

    @Test
    void typeFallbackActsAsWildcardOverItsType() {
        // creator writes a tournament it created mid-run: unknown to the registry,
        // so it must be treated as possibly touching ANY tournament.
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("creator", createdWrite(TOURNAMENT_TYPE)),
                footprint("summaryA", read("tournamentA", TOURNAMENT_TYPE)),
                footprint("topicEditor", write("topicA", TOPIC_TYPE))));

        FunctionalityGroup wildcardPair = requirePair(groups, "creator", "summaryA");
        assertEquals("tournamentA",
                wildcardPair.conflicts().iterator().next().identity(),
                "evidence should prefer the handle side of a wildcard match");

        assertTrue(pairOf(groups, "creator", "topicEditor").isEmpty(),
                "wildcard only spans its own aggregate type");
    }

    @Test
    void wildcardOnTheReadSideStillPairsWithAHandleWrite() {
        // mirror of the case above, with the handle side sorting FIRST: the
        // wildcard is the reader, and the evidence must still name the handle
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("aUpdater", write("tournamentA", TOURNAMENT_TYPE)),
                footprint("zCreatedReader", createdRead(TOURNAMENT_TYPE))));

        assertEquals("tournamentA",
                requirePair(groups, "aUpdater", "zCreatedReader").conflicts().iterator().next().identity(),
                "evidence should name the handle regardless of which side it sorts to");
    }

    @Test
    void twoWildcardsOfTheSameTypePairOnTheTypeItself() {
        // documented over-approximation: two functionalities that each create their
        // own private aggregate of the same type cannot be told apart across
        // separate profiling runs, so the planner pairs them rather than risk
        // pruning a real conflict. Evidence falls back to the type name.
        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(List.of(
                footprint("creatorOne", createdWrite(TOURNAMENT_TYPE)),
                footprint("creatorTwo", createdWrite(TOURNAMENT_TYPE))));

        assertEquals(
                Set.of(new FunctionalityGroup.Conflict(
                        TOURNAMENT_TYPE, Set.of(EffectKind.WRITE), Set.of(EffectKind.WRITE))),
                requirePair(groups, "creatorOne", "creatorTwo").conflicts(),
                "with no handle on either side the evidence identity is the aggregate type");
    }

    @Test
    void emptyAndReadOnlyPoolsProduceNoGroups() {
        assertTrue(FunctionalityGroupPlanner.planGroups(List.of()).isEmpty());
        assertTrue(FunctionalityGroupPlanner.planGroups(List.of(
                footprint("browser", read("tournamentA", TOURNAMENT_TYPE)),
                footprint("otherBrowser", read("tournamentA", TOURNAMENT_TYPE)))).isEmpty());
    }

    @Test
    void outputDoesNotDependOnInputOrderAndMembersAreSorted() {
        List<FunctionalityFootprint> pool = List.of(
                footprint("zWriter", write("tournamentA", TOURNAMENT_TYPE)),
                footprint("aReader", read("tournamentA", TOURNAMENT_TYPE)));

        Set<FunctionalityGroup> groups = FunctionalityGroupPlanner.planGroups(pool);
        Set<FunctionalityGroup> reversed = FunctionalityGroupPlanner.planGroups(pool.reversed());

        assertEquals(groups, reversed, "planning should not depend on input order");
        FunctionalityGroup pair = requirePair(groups, "aReader", "zWriter");
        assertEquals(List.of(id("aReader"), id("zWriter")), pair.members(),
                "members should be sorted by id");
        assertFalse(pair.isSelfPair());
    }
}
