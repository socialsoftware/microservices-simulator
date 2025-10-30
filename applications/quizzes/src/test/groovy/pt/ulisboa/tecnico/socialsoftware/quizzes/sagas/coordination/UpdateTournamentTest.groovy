package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas

@DataJpaTest
class UpdateTournamentTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private TournamentService tournamentService
    @Autowired
    private TopicService topicService
    @Autowired
    private QuizService quizService
    @Autowired
    private LocalCommandGateway commandGateway;

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1

    def setup() {
        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'a user to enroll in the course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        and: 'another user to enroll in the course execution'
        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        and: 'three topics'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        and: 'three questions'
        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto1)), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto2)), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto3)), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)

        and: 'a tournament where the first user is the creator'
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(),  courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(),topicDto2.getAggregateId()])

        and: 'a unit of work'
        def functionalityName1 = UpdateTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
    }

    def cleanup() {}

    def "only run setup"() {
        expect:
        true // or just leave it blank if using Spock 2+
    }

    def 'update tournament successfully'() {
        given:
        tournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        tournamentDto.setEndTime(DateHandler.toISOString(TIME_4))
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when:
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)

        then:
        def updatedTournamentDto = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto != null
        updatedTournamentDto.startTime == DateHandler.toISOString(TIME_2)
        updatedTournamentDto.endTime == DateHandler.toISOString(TIME_4)
        updatedTournamentDto.numberOfQuestions == 3
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        def quizDto = quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)
        quizDto.conclusionDate == DateHandler.toISOString(TIME_4)
        quizDto.questionDtos.size() == 3
    }

    def 'update tournament aborts when trying to create the tournament and violates an invariant'() {
        given:
        tournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        tournamentDto.setEndTime(DateHandler.toISOString(TIME_1))
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when:
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)

        then:
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.INVARIANT_BREAK
        and: 'tournament is not changed'
        def updatedTournamentDto = (TournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 2
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(),topicDto2.getAggregateId()].toSet()
        and: 'saga sate is undone'
        sagaStateOf(updatedTournamentDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def 'update tournament aborts when trying to create a quiz and there are not enough questions'() {
        given:
        tournamentDto.setNumberOfQuestions(4)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when:
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)

        then:
        def error = thrown(QuizzesException)
        error.errorMessage == QuizzesErrorMessage.NOT_ENOUGH_QUESTIONS
        and: 'compensation is executed'
        def updatedTournamentDto = (TournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 2
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(),topicDto2.getAggregateId()].toSet()
        and: 'saga sate is undone'
        sagaStateOf(updatedTournamentDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
        and: 'quiz is not changed'
        def quizDto = (QuizDto) quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.questionDtos.size() == 2
        sagaStateOf(quizDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def 'concurrent update of tournament' () {
        given: 'one update'
        tournamentDto.setNumberOfQuestions(1)
        def topicsAggregateIds = [topicDto3.getAggregateId()].toSet()
        and: 'another update'
        def tournamentDto2 = new TournamentDto()
        tournamentDto2.aggregateId = tournamentDto.aggregateId
        tournamentDto2.setStartTime(DateHandler.toISOString(TIME_2))
        tournamentDto2.setEndTime(DateHandler.toISOString(TIME_4))
        and: 'the first execution occurs until getTopicsStep'
        def updateTournamentFunctionalityOne = new UpdateTournamentFunctionalitySagas(unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1, commandGateway)
        updateTournamentFunctionalityOne.executeUntilStep('getTopicsStep', unitOfWork1)

        when: 'the second execution occurs'
        tournamentFunctionalities.updateTournament(tournamentDto2, topicsAggregateIds)
        then:
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'first functionality finishes'
        updateTournamentFunctionalityOne.resumeWorkflow(unitOfWork1)
        then: 'the tournament topics are updated and it refers to the new quiz'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.topics.size() == 1
        tournamentDtoResult.topics.find{it.aggregateId == topicDto3.aggregateId} != null
        tournamentDtoResult.quiz.aggregateId == tournamentDto.quiz.aggregateId
        def quizDtoResult = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        and: 'quiz is ok as well'
        quizDtoResult.questionDtos.size() == 1
        quizDtoResult.questionDtos.get(0).aggregateId == questionDto3.aggregateId

        when: 'the second execution is retried'
        tournamentFunctionalities.updateTournament(tournamentDto2, topicsAggregateIds)
        then: 'there are no errors'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.startTime == DateHandler.toISOString(TIME_2)
        tournamentDtoResult2.endTime == DateHandler.toISOString(TIME_4)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}