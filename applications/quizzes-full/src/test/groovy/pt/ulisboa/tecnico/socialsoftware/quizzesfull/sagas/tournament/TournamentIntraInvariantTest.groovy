package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentParticipant
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.SagaTournament

import java.time.LocalDateTime

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.CREATOR_IS_NOT_ANONYMOUS
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_ANSWER_BEFORE_START
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_DELETE
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_ENROLL_UNTIL_START_TIME
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_FINAL_AFTER_START
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_START_BEFORE_END_TIME
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_UNIQUE_AS_PARTICIPANT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TournamentIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private static TournamentTopic makeTopic(int id) {
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(id)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        return topic
    }

    private static SagaTournament makeTournament(LocalDateTime startTime, LocalDateTime endTime) {
        new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([makeTopic(10)]),
                startTime, endTime, 5)
    }

    // ─── Creation happy-path ──────────────────────────────────────────────────

    def "create tournament"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")

        when:
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)

        then:
        tournament.executionAggregateId == 100
        tournament.executionVersion == 1L
        tournament.creatorAggregateId == 200
        tournament.creatorName == "Alice"
        tournament.creatorUsername == "alice"
        tournament.creatorVersion == 1L
        tournament.quizAggregateId == 300
        tournament.quizVersion == 1L
        tournament.startTime == startTime
        tournament.endTime == endTime
        tournament.numberOfQuestions == 5
        tournament.cancelled == false
        tournament.topics.size() == 1
        tournament.participants.isEmpty()
        tournament.lastModifiedTime != null
    }

    // ─── P1: TOURNAMENT_START_BEFORE_END_TIME ─────────────────────────────────
    // Spec: startTime < endTime (strict less-than).
    // Moved from CreateTournamentTest and UpdateTournamentTest; single EP violation kept,
    // BVA straddle added here.

    def "TOURNAMENT_START_BEFORE_END_TIME: valid times satisfy invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "TOURNAMENT_START_BEFORE_END_TIME: start after end violates invariant (EP violation)"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(3),
                LocalDateTime.now().plusDays(1))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_START_BEFORE_END_TIME
    }

    // BVA: startTime < endTime (strict).
    // On-point : startTime one nanosecond before endTime — satisfies strict <.
    // Off-point: startTime == endTime — equal is NOT strictly less-than, violates.

    def "TOURNAMENT_START_BEFORE_END_TIME boundary — startTime one nanosecond before endTime satisfies (on-point)"() {
        given: 'start is exactly one nanosecond before end'
        def endTime = LocalDateTime.now().plusDays(1)
        def tournament = makeTournament(endTime.minusNanos(1), endTime)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "TOURNAMENT_START_BEFORE_END_TIME boundary — startTime == endTime violates (off-point)"() {
        given: 'start equals end'
        def instant = LocalDateTime.now().plusDays(1)
        def tournament = makeTournament(instant, instant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_START_BEFORE_END_TIME
    }

    // ─── P1: TOURNAMENT_ENROLL_UNTIL_START_TIME ───────────────────────────────
    // Spec: ∀p: p.enrollTime < startTime (strict less-than).
    // Original EP and BVA cases preserved from TournamentTest.
    // Boundary straddle pinned directly (the addParticipant saga stamps enrollTime = now()).

    def "enroll one nanosecond before start satisfies invariant (boundary on-point)"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, startTime.minusNanos(1))
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "enroll exactly at start violates invariant (boundary off-point)"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, startTime)
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_ENROLL_UNTIL_START_TIME
    }

    // ─── P1: TOURNAMENT_FINAL_AFTER_START ─────────────────────────────────────
    // Spec: once lastModifiedTime > prev.startTime the mutable fields
    //       (startTime, endTime, numberOfQuestions, topics, cancelled) must equal prev.
    // Predicate: lastModifiedTime.isAfter(prev.startTime) ⟹ fields unchanged.
    // setPrev(Aggregate) is public on Aggregate; setLastModifiedTime(LocalDateTime) is public
    // on Tournament — both are pinned directly without the versioning/merge machinery.
    // Moved from UpdateTournamentTest.
    //
    // BVA boundary: lastModifiedTime vs prev.startTime
    // On-point : lastModifiedTime == prev.startTime  (isAfter is false → guard not entered → passes)
    // Off-point: lastModifiedTime == prev.startTime + 1 ns AND a field changed → guard entered → violates

    def "TOURNAMENT_FINAL_AFTER_START boundary — lastModifiedTime == prev.startTime satisfies (on-point)"() {
        given: 'a prev tournament with a pinned startTime'
        def startTime = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def endTime = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def prev = makeTournament(startTime, endTime)

        and: 'a copy with lastModifiedTime == prev.startTime (on-point: isAfter is false)'
        def current = new SagaTournament(prev)
        current.setPrev(prev)
        current.setLastModifiedTime(startTime)  // exactly equal — not after

        when:
        current.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "TOURNAMENT_FINAL_AFTER_START boundary — lastModifiedTime one nanosecond after prev.startTime with changed endTime violates (off-point)"() {
        given: 'a prev tournament with a pinned startTime'
        def startTime = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def endTime = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def prev = makeTournament(startTime, endTime)

        and: 'a copy with lastModifiedTime one nanosecond after prev.startTime (off-point: isAfter is true) and a changed endTime'
        def current = new SagaTournament(prev)
        current.setPrev(prev)
        // setEndTime re-stamps lastModifiedTime, so reset it to the off-point value after
        current.setEndTime(endTime.plusDays(1))
        current.setLastModifiedTime(startTime.plusNanos(1))

        when:
        current.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_FINAL_AFTER_START
    }

    // ─── P1: TOURNAMENT_ANSWER_BEFORE_START ───────────────────────────────────
    // Spec: ∀p: p.quizAnswer.firstAnswerTime != null → firstAnswerTime >= startTime.
    // Original EP and BVA cases preserved from TournamentTest.

    def "answer before start violates invariant"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusDays(1))
        participant.getQuizAnswer().setFirstAnswerTime(LocalDateTime.now().minusHours(1))
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_ANSWER_BEFORE_START
    }

    def "answer after start satisfies invariant"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusDays(1))
        participant.getQuizAnswer().setFirstAnswerTime(LocalDateTime.now().plusDays(2))
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    // ─── Boundary Value Analysis ───────────────────────────────────────────────
    // Spec: plan.md §2.8 Tournament — TOURNAMENT_ANSWER_BEFORE_START (firstAnswerTime >= startTime).
    // The two cases above are equivalence-partitioning representatives (far before / far after).
    // The pair below straddles the exact boundary; see docs/concepts/testing.md § Choosing Input Values.
    // Direct-aggregate cases pin the on-point to the exact start instant (the saga path stamps now()).

    def "answer exactly at start satisfies invariant (boundary on-point)"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusDays(1))
        participant.getQuizAnswer().setFirstAnswerTime(startTime)
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "answer one nanosecond before start violates invariant (boundary off-point)"() {
        given:
        def startTime = LocalDateTime.now().plusDays(1)
        def endTime = LocalDateTime.now().plusDays(2)
        def topic = new TournamentTopic()
        topic.setTopicAggregateId(10)
        topic.setTopicVersion(1L)
        topic.setTopicName("Software Design")
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "Alice", "alice", 1L,
                300, 1L,
                new HashSet<>([topic]),
                startTime, endTime, 5)
        def participant = new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusDays(1))
        participant.getQuizAnswer().setFirstAnswerTime(startTime.minusNanos(1))
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_ANSWER_BEFORE_START
    }

    // ─── P1: TOURNAMENT_UNIQUE_AS_PARTICIPANT ─────────────────────────────────
    // Spec: all participants have distinct aggregateIds.
    // Categorical — one representative violation (no BVA).
    // Moved from AddParticipantTest.

    def "TOURNAMENT_UNIQUE_AS_PARTICIPANT: distinct participants satisfy invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        tournament.addParticipant(new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusHours(1)))
        tournament.addParticipant(new TournamentParticipant(202, "Carol", "carol", 1L, LocalDateTime.now().minusHours(1)))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    def "TOURNAMENT_UNIQUE_AS_PARTICIPANT: two participants with same userId violates invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        tournament.addParticipant(new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusHours(1)))
        // Add a second participant object with the same aggregateId
        tournament.addParticipant(new TournamentParticipant(201, "Bob Duplicate", "bobdup", 1L, LocalDateTime.now().minusHours(2)))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_UNIQUE_AS_PARTICIPANT
    }

    // ─── P1: TOURNAMENT_IS_CANCELED ───────────────────────────────────────────
    // Spec: once prev.cancelled == true, all mutable fields and participants are frozen.
    // Categorical — one representative: change endTime on a previously-cancelled tournament.
    // Moved from CancelTournamentTest, DeleteTournamentTest, AddParticipantTest, SolveQuizTest, UpdateTournamentTest.

    def "TOURNAMENT_IS_CANCELED: mutation after cancellation violates invariant"() {
        given: 'a prev tournament that is cancelled'
        def startTime = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def endTime = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def prev = makeTournament(startTime, endTime)
        prev.setCancelled(true)

        and: 'a copy with a changed endTime (field mutation after cancel)'
        def current = new SagaTournament(prev)
        current.setPrev(prev)
        current.setEndTime(endTime.plusDays(1))

        when:
        current.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_IS_CANCELED
    }

    def "TOURNAMENT_IS_CANCELED: unchanged fields after cancellation satisfy invariant"() {
        given: 'a prev tournament that is cancelled'
        def startTime = LocalDateTime.of(2030, 6, 1, 10, 0, 0, 0)
        def endTime = LocalDateTime.of(2030, 6, 2, 10, 0, 0, 0)
        def prev = makeTournament(startTime, endTime)
        prev.setCancelled(true)

        and: 'a copy with no field changes'
        def current = new SagaTournament(prev)
        current.setPrev(prev)

        when:
        current.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    // ─── P1: TOURNAMENT_DELETE ────────────────────────────────────────────────
    // Spec: state == DELETED → participants.isEmpty().
    // Categorical — one representative: DELETED state with a participant.

    def "TOURNAMENT_DELETE: deleted tournament with participants violates invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        tournament.addParticipant(new TournamentParticipant(201, "Bob", "bob", 1L, LocalDateTime.now().minusHours(1)))
        tournament.setState(Aggregate.AggregateState.DELETED)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_DELETE
    }

    def "TOURNAMENT_DELETE: deleted tournament with no participants satisfies invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        tournament.setState(Aggregate.AggregateState.DELETED)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    // ─── P1: TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY ───────────────────────
    // Spec: when creator is also a participant, name/username/version must match the creator fields.
    // Categorical — one representative: creator enrolled with a mismatched name.

    def "TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY: creator-as-participant with mismatched name violates invariant"() {
        given: 'creator is Alice (aggregateId=200, name=Alice, username=alice, version=1)'
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        // Enroll creator (aggregateId 200) as participant but with wrong name
        tournament.addParticipant(new TournamentParticipant(200, "WRONG_NAME", "alice", 1L, LocalDateTime.now().minusHours(1)))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY
    }

    def "TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY: creator-as-participant with matching fields satisfies invariant"() {
        given: 'creator is Alice (aggregateId=200, name=Alice, username=alice, version=1)'
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))
        // Enroll creator (aggregateId 200) as participant with correct name/username/version
        tournament.addParticipant(new TournamentParticipant(200, "Alice", "alice", 1L, LocalDateTime.now().minusHours(1)))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }

    // ─── P1: CREATOR_IS_NOT_ANONYMOUS ─────────────────────────────────────────
    // Spec: state == ACTIVE → creatorName != "ANONYMOUS" && creatorUsername != "ANONYMOUS".
    // Categorical — one representative: ACTIVE tournament with creatorName == "ANONYMOUS".

    def "CREATOR_IS_NOT_ANONYMOUS: ACTIVE tournament with anonymous creator violates invariant"() {
        given: 'tournament with creatorName set to ANONYMOUS'
        def tournament = new SagaTournament(
                1,
                100, 1L,
                200, "ANONYMOUS", "ANONYMOUS", 1L,
                300, 1L,
                new HashSet<>([makeTopic(10)]),
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2), 5)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFullException)
        ex.getErrorMessage() == CREATOR_IS_NOT_ANONYMOUS
    }

    def "CREATOR_IS_NOT_ANONYMOUS: ACTIVE tournament with named creator satisfies invariant"() {
        given:
        def tournament = makeTournament(
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(2))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFullException)
    }
}
