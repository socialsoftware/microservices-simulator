package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipantDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling.QuizEventHandling

@DataJpaTest
@Transactional
@Import(TournamentInterInvariantTest.LocalBeanConfiguration)
class TournamentInterInvariantTest extends QuizzesFull2SpockTest {

    public static final String OTHER_USER_NAME = "Erin Green"
    public static final String OTHER_USER_USERNAME = "erin"
    public static final String UPDATED_USER_NAME = "Dave A. Black"
    public static final String OTHER_TOPIC_NAME = "Data Structures"
    public static final String UPDATED_TOPIC_NAME = "Advanced Algorithms"
    public static final String OTHER_EXECUTION_ACRONYM = "SE-02"
    public static final Integer SINGLE_QUESTION = 1

    @Autowired
    TournamentEventHandling tournamentEventHandling

    @Autowired
    QuizEventHandling quizEventHandling

    Integer courseAggregateId
    Integer executionAggregateId
    Integer creatorAggregateId
    Integer participantAggregateId
    Integer topicAggregateId
    Integer tournamentAggregateId

    def setup() {
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        creatorAggregateId = createActiveUser(TOURNAMENT_CREATOR_NAME, TOURNAMENT_CREATOR_USERNAME)
        enrollStudentInExecution(executionAggregateId, creatorAggregateId)
        participantAggregateId = createActiveUser(TOURNAMENT_PARTICIPANT_NAME, TOURNAMENT_PARTICIPANT_USERNAME)
        enrollStudentInExecution(executionAggregateId, participantAggregateId)
        topicAggregateId = createTopic(courseAggregateId)
        tournamentAggregateId = createTournament(executionAggregateId, creatorAggregateId,
                TOURNAMENT_START_TIME, TOURNAMENT_END_TIME, SINGLE_QUESTION, [topicAggregateId])
        tournamentFunctionalities.addParticipant(tournamentAggregateId, participantAggregateId)
    }

    def "tournament updates the cached participant name on UpdateStudentNameEvent"() {
        // Spec: plan.md §8 Tournament — subscribed UpdateStudentNameEvent; grouping §2
        // Tournament/User-participant row caches (userName, userVersion). Payload field: updatedName.
        given:
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'the participant is renamed'
        userFunctionalities.updateUserName(participantAggregateId, UPDATED_USER_NAME)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleUpdateStudentNameEvents()

        then: 'the cached participant carries the payload and advances past the published version'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userName == UPDATED_USER_NAME
        participant.userVersion > versionBefore
    }

    def "tournament ignores an UpdateStudentNameEvent for another user"() {
        // Spec: plan.md §8 Tournament — each cached user reference anchors its own subscription
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def nameBefore = participantOf(tournamentAggregateId, participantAggregateId).userName
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'an unrelated user is renamed'
        userFunctionalities.updateUserName(otherUserAggregateId, UPDATED_USER_NAME)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleUpdateStudentNameEvents()

        then: 'the cached participant snapshot is untouched'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userName == nameBefore
        participant.userVersion == versionBefore
    }

    def "tournament anonymizes the cached participant on AnonymizeStudentEvent"() {
        // Spec: plan.md §3.2 — rule PARTICIPANT_EXISTS (Tournament), P2; grouping §4 payload fields
        // name and username.
        given:
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'the participant is anonymized'
        userFunctionalities.anonymizeUser(participantAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'both cached name fields are anonymized and the version advances'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userName == QuizzesFull2DomainConstants.ANONYMOUS
        participant.userUsername == QuizzesFull2DomainConstants.ANONYMOUS
        participant.userVersion > versionBefore
    }

    def "tournament ignores an AnonymizeStudentEvent for another user"() {
        // Spec: plan.md §8 Tournament — each cached user reference anchors its own subscription
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def nameBefore = participantOf(tournamentAggregateId, participantAggregateId).userName
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'an unrelated user is anonymized'
        userFunctionalities.anonymizeUser(otherUserAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'the cached participant snapshot is untouched'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userName == nameBefore
        participant.userVersion == versionBefore
    }

    def "anonymizing the creator leaves the AnonymizeStudentEvent unprocessed"() {
        // Spec: plan.md §3.1 — CREATOR_IS_NOT_ANONYMOUS. Folding the payload into the creator snapshot
        // violates the invariant, so the commit is refused and the cached creator stays as it was.
        when: 'the creator is anonymized'
        userFunctionalities.anonymizeUser(creatorAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'the invariant rejects the update'
        def exception = thrown(QuizzesFull2Exception)
        exception.message == QuizzesFull2ErrorMessage.CREATOR_IS_NOT_ANONYMOUS

        and: 'the cached creator snapshot is unchanged'
        tournamentOf(tournamentAggregateId).creatorName == TOURNAMENT_CREATOR_NAME
    }

    def "tournament drops the participant on DeleteUserEvent"() {
        // Spec: plan.md §3.2 — rule PARTICIPANT_EXISTS (Tournament), P2: a participant is one member of
        // a collection the tournament remains valid without.
        when: 'the participant is deleted'
        userFunctionalities.deleteUser(participantAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteUserEvents()

        then: 'the participant is gone and the tournament survives'
        def tournament = tournamentOf(tournamentAggregateId)
        tournament.participants.every { it.userAggregateId != participantAggregateId }
        tournament.creatorAggregateId == creatorAggregateId
    }

    def "tournament is removed when its creator is deleted"() {
        // Spec: plan.md §3.2 — rule CREATOR_EXISTS (Tournament), P2: the creator is structural, so the
        // tournament cannot outlive it.
        when: 'the creator is deleted'
        userFunctionalities.deleteUser(creatorAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteUserEvents()

        and: 'attempt to load the now-removed tournament'
        loadForCheck(tournamentAggregateId, SagaTournament)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "tournament ignores a DeleteUserEvent for another user"() {
        // Spec: plan.md §8 Tournament — each cached user reference anchors its own subscription
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'an unrelated user is deleted'
        userFunctionalities.deleteUser(otherUserAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteUserEvents()

        then: 'the participant list and its cached snapshot are intact'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userAggregateId == participantAggregateId
        participant.userVersion == versionBefore
    }

    def "tournament updates the cached topic name on UpdateTopicEvent"() {
        // Spec: plan.md §8 Tournament — subscribed UpdateTopicEvent; grouping §2 Tournament/Topic row
        // caches (topicName, topicVersion). Payload field: topicName.
        given:
        def versionBefore = topicOf(tournamentAggregateId, topicAggregateId).topicVersion

        when: 'the topic is renamed'
        topicFunctionalities.updateTopic(topicAggregateId, UPDATED_TOPIC_NAME)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleUpdateTopicEvents()

        then: 'the cached topic carries the payload and advances past the published version'
        def topic = topicOf(tournamentAggregateId, topicAggregateId)
        topic.topicName == UPDATED_TOPIC_NAME
        topic.topicVersion > versionBefore
    }

    def "tournament ignores an UpdateTopicEvent for a topic it does not cover"() {
        // Spec: plan.md §8 Tournament — each cached topic anchors its own subscription
        given:
        def otherTopicAggregateId = createTopic(courseAggregateId, OTHER_TOPIC_NAME)
        def nameBefore = topicOf(tournamentAggregateId, topicAggregateId).topicName
        def versionBefore = topicOf(tournamentAggregateId, topicAggregateId).topicVersion

        when: 'a topic the tournament does not cover is renamed'
        topicFunctionalities.updateTopic(otherTopicAggregateId, UPDATED_TOPIC_NAME)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleUpdateTopicEvents()

        then: 'the cached topic snapshot is untouched'
        def topic = topicOf(tournamentAggregateId, topicAggregateId)
        topic.topicName == nameBefore
        topic.topicVersion == versionBefore
    }

    def "tournament drops the topic on DeleteTopicEvent"() {
        // Spec: plan.md §3.2 — rule TOPIC_EXISTS (Tournament), P2: the handler removes the stale topic;
        // the tournament still draws from whatever topics remain.
        when: 'the topic is deleted'
        topicFunctionalities.deleteTopic(topicAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteTopicEvents()

        then: 'the topic is gone and the tournament survives'
        def tournament = tournamentOf(tournamentAggregateId)
        tournament.topics.every { it.topicAggregateId != topicAggregateId }
        tournament.aggregateId == tournamentAggregateId
    }

    def "tournament ignores a DeleteTopicEvent for a topic it does not cover"() {
        // Spec: plan.md §8 Tournament — each cached topic anchors its own subscription
        given:
        def otherTopicAggregateId = createTopic(courseAggregateId, OTHER_TOPIC_NAME)
        def versionBefore = topicOf(tournamentAggregateId, topicAggregateId).topicVersion

        when: 'a topic the tournament does not cover is deleted'
        topicFunctionalities.deleteTopic(otherTopicAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteTopicEvents()

        then: 'the cached topic is still there and untouched'
        def topic = topicOf(tournamentAggregateId, topicAggregateId)
        topic.topicAggregateId == topicAggregateId
        topic.topicVersion == versionBefore
    }

    def "tournament is removed on DeleteCourseExecutionEvent"() {
        // Spec: plan.md §3.2 — rule COURSE_EXECUTION_EXISTS (Tournament), P2: a tournament cannot
        // outlive the course execution it belongs to.
        when: 'the course execution is deleted'
        executionFunctionalities.deleteExecution(executionAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteCourseExecutionEvents()

        and: 'attempt to load the now-removed tournament'
        loadForCheck(tournamentAggregateId, SagaTournament)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "tournament ignores a DeleteCourseExecutionEvent for another execution"() {
        // Spec: plan.md §8 Tournament — the subscription is anchored on the cached execution snapshot
        given:
        def otherExecutionAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        def versionBefore = tournamentOf(tournamentAggregateId).executionVersion

        when: 'another course execution is deleted'
        executionFunctionalities.deleteExecution(otherExecutionAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDeleteCourseExecutionEvents()

        then: 'the tournament survives with its cached execution snapshot intact'
        def tournament = tournamentOf(tournamentAggregateId)
        tournament.executionAggregateId == executionAggregateId
        tournament.executionVersion == versionBefore
    }

    def "tournament drops the participant on DisenrollStudentFromCourseExecutionEvent"() {
        // Spec: plan.md §3.2 — rule PARTICIPANT_COURSE_EXECUTION, P2 half: the add-time guard cannot
        // keep the predicate true once the student leaves the execution.
        when: 'the participant is disenrolled from the execution'
        executionFunctionalities.disenrollStudent(executionAggregateId, participantAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        then: 'the participant is gone and the tournament survives'
        def tournament = tournamentOf(tournamentAggregateId)
        tournament.participants.every { it.userAggregateId != participantAggregateId }
        tournament.aggregateId == tournamentAggregateId
    }

    def "tournament ignores a DisenrollStudentFromCourseExecutionEvent for another student"() {
        // Spec: session-d.md § Shared-anchor events — the event is anchored on the execution, so every
        // tournament of that execution receives it; the student check discriminates in the service.
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        enrollStudentInExecution(executionAggregateId, otherUserAggregateId)
        def versionBefore = participantOf(tournamentAggregateId, participantAggregateId).userVersion

        when: 'another student of the same execution is disenrolled'
        executionFunctionalities.disenrollStudent(executionAggregateId, otherUserAggregateId)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        then: 'the participant is still enrolled with its cached snapshot intact'
        def participant = participantOf(tournamentAggregateId, participantAggregateId)
        participant.userAggregateId == participantAggregateId
        participant.userVersion == versionBefore
    }

    def "tournament is removed on InvalidateQuizEvent"() {
        // Spec: plan.md §3.2 — rule QUIZ_EXISTS (Tournament), P2: an invalidated quiz is treated as
        // deleted downstream, and Tournament.quiz is constructor-final.
        given:
        def quizAggregateId = tournamentOf(tournamentAggregateId).quizAggregateId
        def questionAggregateId = quizFunctionalities.getQuizById(quizAggregateId)
                .questions.first().questionAggregateId

        when: 'a question of the generated quiz is deleted and the quiz invalidates itself'
        questionFunctionalities.deleteQuestion(questionAggregateId)
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleInvalidateQuizEvents()

        and: 'attempt to load the now-removed tournament'
        loadForCheck(tournamentAggregateId, SagaTournament)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "tournament ignores an InvalidateQuizEvent for another quiz"() {
        // Spec: plan.md §8 Tournament — the subscription is anchored on the cached quiz snapshot
        given:
        def otherQuestionAggregateId = createQuestionWithOptions(courseAggregateId)
        createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE,
                QUIZ_RESULTS_DATE, QUIZ_TYPE, [otherQuestionAggregateId])
        def versionBefore = tournamentOf(tournamentAggregateId).quizVersion

        when: 'the other quiz invalidates itself'
        questionFunctionalities.deleteQuestion(otherQuestionAggregateId)
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleInvalidateQuizEvents()

        then: 'the tournament survives with its cached quiz snapshot intact'
        def tournament = tournamentOf(tournamentAggregateId)
        tournament.aggregateId == tournamentAggregateId
        tournament.quizVersion == versionBefore
    }

    def "tournament folds an answer into the participant statistics on QuizAnswerQuestionAnswerEvent"() {
        // Spec: plan.md §3.2 — rule QUIZ_ANSWER_EXISTS (Tournament), P2: the handler updates answered,
        // numberOfAnswered and numberOfCorrect and takes firstAnswerTime from the payload.
        given:
        def fixture = startedTournamentWithAnswerableQuiz()

        when: 'the participant answers the question correctly'
        answerCorrectly(fixture)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleQuizAnswerQuestionAnswerEvents()

        then: 'the participant statistics reflect the answer'
        def participant = participantOf(fixture.tournamentAggregateId, participantAggregateId)
        participant.answered
        participant.numberOfAnswered == 1
        participant.numberOfCorrect == 1
        participant.quizAnswerAggregateId == fixture.quizAnswerAggregateId
        participant.firstAnswerTime != null
        !participant.firstAnswerTime.isBefore(fixture.startTime)
    }

    def "tournament ignores a QuizAnswerQuestionAnswerEvent for a student who is not a participant"() {
        // Spec: session-d.md § Shared-anchor events — the event is anchored on the quiz, so the
        // studentAggregateId check in the service is what keeps another student's answer out.
        given:
        def fixture = startedTournamentWithAnswerableQuiz()
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        enrollStudentInExecution(executionAggregateId, otherUserAggregateId)
        def answeredBefore = participantOf(fixture.tournamentAggregateId, participantAggregateId)
                .numberOfAnswered

        when: 'a student who never joined the tournament answers the same quiz'
        def quizAnswerAggregateId = quizAnswerFunctionalities.createQuizAnswer(fixture.quizAggregateId,
                otherUserAggregateId, executionAggregateId).aggregateId
        quizAnswerFunctionalities.answerQuestion(quizAnswerAggregateId, fixture.questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN)

        and: 'the tournament polls for the event'
        tournamentEventHandling.handleQuizAnswerQuestionAnswerEvents()

        then: 'the participant statistics are untouched'
        def participant = participantOf(fixture.tournamentAggregateId, participantAggregateId)
        participant.numberOfAnswered == answeredBefore
        !participant.answered
        participant.firstAnswerTime == null
    }

    // TOURNAMENT_ANSWER_BEFORE_START requires the answer to land at or after startTime, so this fixture
    // opens the shortest window it can and waits it out - the same constraint createClosedTournament
    // works around, except the participant must also be added while the tournament is still upcoming.
    // The quiz is generated from a topicless question carrying a correct option key, which the topicked
    // question the base fixture stocked does not have.
    private Map startedTournamentWithAnswerableQuiz() {
        def questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def startTime = DateHandler.now().plusSeconds(5)
        def endTime = DateHandler.now().plusMinutes(10)
        def tournament = tournamentFunctionalities.createTournament(executionAggregateId, creatorAggregateId,
                startTime, endTime, SINGLE_QUESTION, [])
        tournamentFunctionalities.addParticipant(tournament.aggregateId, participantAggregateId)
        while (DateHandler.now().isBefore(startTime)) {
            sleep(50)
        }
        return [tournamentAggregateId: tournament.aggregateId,
                quizAggregateId      : tournament.quizAggregateId,
                questionAggregateId  : questionAggregateId,
                startTime            : startTime]
    }

    private Map answerCorrectly(Map fixture) {
        def quizAnswerAggregateId = quizAnswerFunctionalities.createQuizAnswer(fixture.quizAggregateId,
                participantAggregateId, executionAggregateId).aggregateId
        quizAnswerFunctionalities.answerQuestion(quizAnswerAggregateId, fixture.questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN)
        fixture.quizAnswerAggregateId = quizAnswerAggregateId
        return fixture
    }

    private TournamentParticipantDto participantOf(Integer tournamentAggregateId, Integer userAggregateId) {
        return tournamentOf(tournamentAggregateId).participants
                .find { it.userAggregateId == userAggregateId }
    }

    private TournamentTopicDto topicOf(Integer tournamentAggregateId, Integer topicAggregateId) {
        return tournamentOf(tournamentAggregateId).topics
                .find { it.topicAggregateId == topicAggregateId }
    }

    private tournamentOf(Integer tournamentAggregateId) {
        return tournamentService.getTournamentById(tournamentAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
