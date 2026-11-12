package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.tournament

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.SagaTournament

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(TournamentIntraInvariantTest.LocalBeanConfiguration)
class TournamentIntraInvariantTest extends QuizzesFull2SpockTest {

    private static SagaTournament aTournament(LocalDateTime startTime = TOURNAMENT_START_TIME,
                                              LocalDateTime endTime = TOURNAMENT_END_TIME,
                                              String creatorName = TOURNAMENT_CREATOR_NAME,
                                              String creatorUsername = TOURNAMENT_CREATOR_USERNAME) {
        return new SagaTournament(TOURNAMENT_AGGREGATE_ID, EXECUTION_AGGREGATE_ID, TOURNAMENT_EXECUTION_VERSION,
                COURSE_AGGREGATE_ID, TOURNAMENT_CREATOR_AGGREGATE_ID, creatorName, creatorUsername,
                TOURNAMENT_CREATOR_VERSION, QUIZ_AGGREGATE_ID, TOURNAMENT_QUIZ_VERSION, startTime, endTime,
                TOURNAMENT_NUMBER_OF_QUESTIONS)
    }

    private static TournamentParticipant aParticipant(Integer userAggregateId = TOURNAMENT_PARTICIPANT_AGGREGATE_ID,
                                                      LocalDateTime enrollTime = TOURNAMENT_ENROLL_TIME) {
        return new TournamentParticipant(userAggregateId, TOURNAMENT_PARTICIPANT_NAME,
                TOURNAMENT_PARTICIPANT_USERNAME, TOURNAMENT_PARTICIPANT_VERSION, enrollTime)
    }

    // The creator enrolls like anyone else, so their participant entry mirrors the creator snapshot.
    private static TournamentParticipant theCreatorAsParticipant() {
        return new TournamentParticipant(TOURNAMENT_CREATOR_AGGREGATE_ID, TOURNAMENT_CREATOR_NAME,
                TOURNAMENT_CREATOR_USERNAME, TOURNAMENT_CREATOR_VERSION, TOURNAMENT_ENROLL_TIME)
    }

    private static TournamentTopic aTopic(Integer topicAggregateId = TOURNAMENT_TOPIC_AGGREGATE_ID,
                                          Integer courseAggregateId = COURSE_AGGREGATE_ID) {
        return new TournamentTopic(topicAggregateId, TOURNAMENT_TOPIC_NAME, TOURNAMENT_TOPIC_VERSION,
                courseAggregateId)
    }

    // A version whose prev is the given tournament: the copy constructor carries prev, and
    // lastModifiedTime is pinned by the caller because the mutators stamp DateHandler.now().
    private static SagaTournament aNextVersionOf(SagaTournament tournament) {
        return new SagaTournament(tournament)
    }

    def "create tournament"() {
        // Spec: plan.md §8 Tournament — CreateTournament(executionAggregateId, creatorAggregateId,
        // startTime, endTime, numberOfQuestions, topicAggregateIds); snapshots TournamentExecution /
        // TournamentCreator / TournamentQuiz (plan.md § snapshot table)
        when:
        def tournament = aTournament()
        tournament.verifyInvariants()

        then:
        tournament.aggregateId == TOURNAMENT_AGGREGATE_ID
        tournament.startTime == TOURNAMENT_START_TIME
        tournament.endTime == TOURNAMENT_END_TIME
        tournament.numberOfQuestions == TOURNAMENT_NUMBER_OF_QUESTIONS
        !tournament.cancelled
        tournament.execution.executionAggregateId == EXECUTION_AGGREGATE_ID
        tournament.execution.executionVersion == TOURNAMENT_EXECUTION_VERSION
        tournament.execution.courseAggregateId == COURSE_AGGREGATE_ID
        tournament.execution.tournament.is(tournament)
        tournament.creator.userAggregateId == TOURNAMENT_CREATOR_AGGREGATE_ID
        tournament.creator.userName == TOURNAMENT_CREATOR_NAME
        tournament.creator.userUsername == TOURNAMENT_CREATOR_USERNAME
        tournament.creator.userVersion == TOURNAMENT_CREATOR_VERSION
        tournament.creator.tournament.is(tournament)
        tournament.quiz.quizAggregateId == QUIZ_AGGREGATE_ID
        tournament.quiz.quizVersion == TOURNAMENT_QUIZ_VERSION
        tournament.quiz.tournament.is(tournament)
        tournament.participants.isEmpty()
        tournament.topics.isEmpty()
        tournament.sagaState == GenericSagaState.NOT_IN_SAGA
    }

    def "tournament: TOURNAMENT_START_BEFORE_END_TIME violation — end time equal to start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_START_BEFORE_END_TIME — startTime < endTime
        given:
        def tournament = aTournament(TOURNAMENT_START_TIME, TOURNAMENT_START_TIME)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_START_BEFORE_END_TIME
    }

    def "tournament: TOURNAMENT_START_BEFORE_END_TIME on-point — end time one tick after start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_START_BEFORE_END_TIME — boundary straddle
        given:
        def tournament = aTournament(TOURNAMENT_START_TIME, TOURNAMENT_START_TIME.plusNanos(1))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_UNIQUE_AS_PARTICIPANT violation — the same student enrolled twice"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_UNIQUE_AS_PARTICIPANT — distinct participant userAggregateIds
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant())
        tournament.addParticipant(aParticipant())

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_UNIQUE_AS_PARTICIPANT
    }

    def "tournament: TOURNAMENT_UNIQUE_AS_PARTICIPANT holds for two distinct students"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_UNIQUE_AS_PARTICIPANT — the satisfying equivalence class
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant())
        tournament.addParticipant(aParticipant(TOURNAMENT_PARTICIPANT_AGGREGATE_ID_2))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_ENROLL_UNTIL_START_TIME violation — enrolled exactly at the start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_ENROLL_UNTIL_START_TIME — p.enrollTime < startTime
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant(TOURNAMENT_PARTICIPANT_AGGREGATE_ID, TOURNAMENT_START_TIME))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_ENROLL_UNTIL_START_TIME
    }

    def "tournament: TOURNAMENT_ENROLL_UNTIL_START_TIME on-point — enrolled one tick before the start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_ENROLL_UNTIL_START_TIME — boundary straddle
        given:
        def tournament = aTournament()
        tournament.addParticipant(
                aParticipant(TOURNAMENT_PARTICIPANT_AGGREGATE_ID, TOURNAMENT_START_TIME.minusNanos(1)))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_ANSWER_BEFORE_START violation — first answer one tick before the start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_ANSWER_BEFORE_START — firstAnswerTime >= startTime
        given:
        def tournament = aTournament()
        def participant = aParticipant()
        participant.quizAnswer.setFirstAnswerTime(TOURNAMENT_START_TIME.minusNanos(1))
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_ANSWER_BEFORE_START
    }

    def "tournament: TOURNAMENT_ANSWER_BEFORE_START on-point — first answer exactly at the start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_ANSWER_BEFORE_START — boundary straddle
        given:
        def tournament = aTournament()
        def participant = aParticipant()
        participant.quizAnswer.setFirstAnswerTime(TOURNAMENT_START_TIME)
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_ANSWER_BEFORE_START does not constrain a participant who has not answered"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_ANSWER_BEFORE_START — the predicate is guarded on
        // firstAnswerTime != null, and QuizAnswerQuestionAnswerEvent sets it only once an answer arrives
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant())

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY violation — the creator's entry carries a stale name"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY — a creator-shaped
        // participant mirrors the creator snapshot
        given:
        def tournament = aTournament()
        def participant = theCreatorAsParticipant()
        participant.setUserName("Stale creator name")
        tournament.addParticipant(participant)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY
    }

    def "tournament: TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY holds when the creator's entry mirrors the snapshot"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY — the satisfying class
        given:
        def tournament = aTournament()
        tournament.addParticipant(theCreatorAsParticipant())

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: CREATOR_IS_NOT_ANONYMOUS violation — the creator's name was anonymized"() {
        // Spec: plan.md §3.2 rule CREATOR_IS_NOT_ANONYMOUS — neither cached creator field is ANONYMOUS
        given:
        def tournament = aTournament(TOURNAMENT_START_TIME, TOURNAMENT_END_TIME,
                QuizzesFull2DomainConstants.ANONYMOUS)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.CREATOR_IS_NOT_ANONYMOUS
    }

    def "tournament: CREATOR_IS_NOT_ANONYMOUS violation — the creator's username was anonymized"() {
        // Spec: plan.md §3.2 rule CREATOR_IS_NOT_ANONYMOUS — the second disjunct of the predicate
        given:
        def tournament = aTournament(TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, TOURNAMENT_CREATOR_NAME,
                QuizzesFull2DomainConstants.ANONYMOUS)

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.CREATOR_IS_NOT_ANONYMOUS
    }

    def "tournament: TOPIC_COURSE_EXECUTION violation — a topic from another course"() {
        // Spec: plan.md §3.2 rule TOPIC_COURSE_EXECUTION — every topic's cached courseAggregateId
        // equals the execution's cached courseAggregateId
        given:
        def tournament = aTournament()
        tournament.addTopic(aTopic(TOURNAMENT_TOPIC_AGGREGATE_ID, COURSE_AGGREGATE_ID_2))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOPIC_COURSE_EXECUTION
    }

    def "tournament: TOPIC_COURSE_EXECUTION holds for topics of the execution's course"() {
        // Spec: plan.md §3.2 rule TOPIC_COURSE_EXECUTION — the satisfying equivalence class
        given:
        def tournament = aTournament()
        tournament.addTopic(aTopic())
        tournament.addTopic(aTopic(TOURNAMENT_TOPIC_AGGREGATE_ID_2))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_DELETE violation — deleted with a participant still enrolled"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_DELETE — state == DELETED implies participants.isEmpty()
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant())
        tournament.remove()

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_DELETE
    }

    def "tournament: TOURNAMENT_DELETE holds when the participants are cleared before deletion"() {
        // Spec: plan.md §8 Tournament — DeleteTournament clears the participant list in the same operation
        given:
        def tournament = aTournament()
        tournament.addParticipant(aParticipant())
        tournament.removeParticipant(TOURNAMENT_PARTICIPANT_AGGREGATE_ID)
        tournament.remove()

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_FINAL_AFTER_START violation — end time changed after the tournament started"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_FINAL_AFTER_START — lastModifiedTime > prev.startTime implies
        // startTime, endTime, numberOfQuestions, topics and cancelled are unchanged from prev
        given:
        def tournament = aNextVersionOf(aTournament())
        tournament.setEndTime(TOURNAMENT_END_TIME.plusHours(1))
        tournament.setLastModifiedTime(TOURNAMENT_START_TIME.plusNanos(1))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_FINAL_AFTER_START
    }

    def "tournament: TOURNAMENT_FINAL_AFTER_START on-point — end time changed exactly at the start time"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_FINAL_AFTER_START — boundary straddle on
        // lastModifiedTime > prev.startTime; the freeze has not engaged at the start time itself
        given:
        def tournament = aNextVersionOf(aTournament())
        tournament.setEndTime(TOURNAMENT_END_TIME.plusHours(1))
        tournament.setLastModifiedTime(TOURNAMENT_START_TIME)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_FINAL_AFTER_START violation — topic added after the tournament started"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_FINAL_AFTER_START — the topic set is frozen too
        given:
        def tournament = aNextVersionOf(aTournament())
        tournament.addTopic(aTopic())
        tournament.setLastModifiedTime(TOURNAMENT_START_TIME.plusNanos(1))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_FINAL_AFTER_START
    }

    def "tournament: TOURNAMENT_FINAL_AFTER_START holds when a cached topic is refreshed after the start time"() {
        // Spec: plan.md §3.2 rule TOPIC_EXISTS (Tournament) — the UpdateTopicEvent handler refreshes a
        // TournamentTopic's cached name and version; only the topic set is frozen
        given:
        def previous = aTournament()
        previous.addTopic(aTopic())
        def tournament = aNextVersionOf(previous)
        tournament.getTopics().first().setTopicName("Refreshed topic name")
        tournament.setLastModifiedTime(TOURNAMENT_START_TIME.plusNanos(1))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_IS_CANCELED violation — end time changed after cancellation"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_IS_CANCELED — prev.cancelled implies the schedule, topics,
        // cancelled flag and participants are unchanged from prev
        given:
        def previous = aTournament()
        previous.setCancelled(true)
        def tournament = aNextVersionOf(previous)
        tournament.setEndTime(TOURNAMENT_END_TIME.plusHours(1))

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_IS_CANCELED
    }

    def "tournament: TOURNAMENT_IS_CANCELED violation — participant added after cancellation"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_IS_CANCELED — the participant list is frozen too
        given:
        def previous = aTournament()
        previous.setCancelled(true)
        def tournament = aNextVersionOf(previous)
        tournament.addParticipant(aParticipant())

        when:
        tournament.verifyInvariants()

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.TOURNAMENT_IS_CANCELED
    }

    def "tournament: TOURNAMENT_IS_CANCELED holds for a new version that changes nothing frozen"() {
        // Spec: plan.md §3.1 rule TOURNAMENT_IS_CANCELED — the satisfying equivalence class
        given:
        def previous = aTournament()
        previous.setCancelled(true)
        def tournament = aNextVersionOf(previous)

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    def "tournament: TOURNAMENT_IS_CANCELED does not freeze a tournament that was not cancelled"() {
        // Spec: plan.md §8 Tournament — UpdateTournament may change the schedule of an open tournament
        given:
        def tournament = aNextVersionOf(aTournament())
        tournament.setEndTime(TOURNAMENT_END_TIME.plusHours(1))

        when:
        tournament.verifyInvariants()

        then:
        notThrown(QuizzesFull2Exception)
    }

    // TOURNAMENT_UNIQUE_AS_PARTICIPANT, TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY, TOURNAMENT_DELETE,
    // TOURNAMENT_IS_CANCELED, CREATOR_IS_NOT_ANONYMOUS and TOPIC_COURSE_EXECUTION are categorical
    // (uniqueness, field mirroring, state freezes and set membership), so testing.md § Choosing Input
    // Values excludes them from BVA straddles. TOURNAMENT_CREATOR_IS_FINAL,
    // TOURNAMENT_COURSE_EXECUTION_IS_FINAL and TOURNAMENT_QUIZ_IS_FINAL are enforced by the absence of a
    // setter; testing.md § T1 excludes all three.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
