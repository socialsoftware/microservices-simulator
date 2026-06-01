package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentParticipant
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.sagas.SagaTournament

import java.time.LocalDateTime

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_ANSWER_BEFORE_START
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_ENROLL_UNTIL_START_TIME

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class TournamentTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

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

    // Spec: plan.md §2.8 Tournament — TOURNAMENT_ENROLL_UNTIL_START_TIME (enrollTime < startTime).
    // Boundary straddle, pinned directly (the addParticipant saga stamps enrollTime = now()).

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
}
