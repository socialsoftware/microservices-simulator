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
}
