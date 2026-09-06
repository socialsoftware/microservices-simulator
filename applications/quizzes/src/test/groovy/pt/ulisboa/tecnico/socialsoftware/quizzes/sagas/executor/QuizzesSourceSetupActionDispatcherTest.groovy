package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.executor

import pt.ulisboa.tecnico.socialsoftware.quizzes.executor.QuizzesSourceSetupActionDispatcher
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities
import spock.lang.Specification

class QuizzesSourceSetupActionDispatcherTest extends Specification {
    private ExecutionFunctionalities executions = Mock()
    private UserFunctionalities users = Mock()
    private TopicFunctionalities topics = Mock()
    private QuestionFunctionalities questions = Mock()
    private QuizFunctionalities quizzes = Mock()
    private QuizAnswerFunctionalities quizAnswers = Mock()
    private TournamentFunctionalities tournaments = Mock()

    private QuizzesSourceSetupActionDispatcher dispatcher = new QuizzesSourceSetupActionDispatcher(
            executions, users, topics, questions, quizzes, quizAnswers, tournaments)

    def 'the closed setup map authorizes only the exact reviewed methods'() {
        expect:
        dispatcher.setupMethods().keySet() == [
                QuizzesSourceSetupActionDispatcher.CREATE_COURSE_EXECUTION,
                QuizzesSourceSetupActionDispatcher.CREATE_USER,
                QuizzesSourceSetupActionDispatcher.ACTIVATE_USER,
                QuizzesSourceSetupActionDispatcher.ADD_STUDENT,
                QuizzesSourceSetupActionDispatcher.REMOVE_STUDENT,
                QuizzesSourceSetupActionDispatcher.CREATE_TOPIC,
                QuizzesSourceSetupActionDispatcher.CREATE_QUESTION,
                QuizzesSourceSetupActionDispatcher.CREATE_QUIZ,
                QuizzesSourceSetupActionDispatcher.START_QUIZ,
                QuizzesSourceSetupActionDispatcher.CREATE_TOURNAMENT,
                QuizzesSourceSetupActionDispatcher.ADD_PARTICIPANT
        ] as Set
        dispatcher.setupMethods().size() == 11
        !dispatcher.setupMethods().containsKey(
                QuizFunctionalities.name + '#findQuiz(java.lang.Integer):' + QuizDto.name)

        and:
        def createQuiz = dispatcher.setupMethods().get(QuizzesSourceSetupActionDispatcher.CREATE_QUIZ)
        createQuiz.declaredResultTypeFqn() == QuizDto.name
        !createQuiz.voidResult()
        def startQuiz = dispatcher.setupMethods().get(QuizzesSourceSetupActionDispatcher.START_QUIZ)
        startQuiz.declaredResultTypeFqn() == 'void'
        startQuiz.voidResult()
        def addParticipant = dispatcher.setupMethods().get(QuizzesSourceSetupActionDispatcher.ADD_PARTICIPANT)
        addParticipant.declaredResultTypeFqn() == 'void'
        addParticipant.voidResult()
    }

    def 'the reviewed methods dispatch their typed arguments in source order'() {
        given:
        def quizInput = new QuizDto()
        def createdQuiz = new QuizDto()

        when:
        def createResult = dispatcher.setupMethods()
                .get(QuizzesSourceSetupActionDispatcher.CREATE_QUIZ)
                .invocation().invoke([41, quizInput])

        then:
        1 * quizzes.createQuiz(41, quizInput) >> createdQuiz
        createResult.is(createdQuiz)

        when:
        def startResult = dispatcher.setupMethods()
                .get(QuizzesSourceSetupActionDispatcher.START_QUIZ)
                .invocation().invoke([42, 43, 44])

        then:
        1 * quizAnswers.startQuiz(42, 43, 44)
        startResult == null

        when:
        def removeStudentResult = dispatcher.setupMethods()
                .get(QuizzesSourceSetupActionDispatcher.REMOVE_STUDENT)
                .invocation().invoke([45, 46])

        then:
        1 * executions.removeStudentFromCourseExecution(45, 46)
        removeStudentResult == null

        when:
        def participantResult = dispatcher.setupMethods()
                .get(QuizzesSourceSetupActionDispatcher.ADD_PARTICIPANT)
                .invocation().invoke([47, 48, 49])

        then:
        1 * tournaments.addParticipant(47, 48, 49)
        participantResult == null
        0 * _
    }
}
