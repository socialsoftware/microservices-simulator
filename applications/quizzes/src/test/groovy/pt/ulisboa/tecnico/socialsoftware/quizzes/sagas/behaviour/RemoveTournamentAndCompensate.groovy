package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.RemoveTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament.UpdateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService


@DataJpaTest
class RemoveTournamentAndCompensate extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private QuizService quizService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private TopicService topicService

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private Set<Integer> topics
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1
    def updateTournamentFunctionality
    def updateTournamentDto
    def functionalityName1
    def unitOfWork2
    def removeTournamentFunctionality
    def functionalityName2


     def setup() {
        given: 'load a behavior specification'
        loadBehaviorScripts()

        and: 'a course execution'
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

        and: 'information required to update tournament'
        functionalityName1 = UpdateTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        topics =  new HashSet<>(Arrays.asList(topicDto1.aggregateId,topicDto2.aggregateId))
        updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService,
                updateTournamentDto, topics, unitOfWork1)

        and: 'information required to remove tournament'
        functionalityName2 = RemoveTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)
        removeTournamentFunctionality = new RemoveTournamentFunctionalitySagas(tournamentService, quizService, unitOfWorkService, tournamentDto.aggregateId, unitOfWork2)
    }

    def cleanup() {
        behaviourService.cleanUpCounter()
    }

    def 'resume after partial quiz removal completes tournament deletion or fails gracefully'() {
        given: 'a clear report'
        behaviourService.cleanReportFile()

        and: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.availableDate == TIME_1
        sagaQuiz.state == Aggregate.AggregateState.DELETED
        

        when: 'remove finishes'
        boolean exceptionThrown = false
        try {
            removeTournamentFunctionality.resumeWorkflow(unitOfWork2)
        } catch (Exception e) {
            exceptionThrown = true
        }


        then: 'verify tournament state depending on the outcome of the resume operation'
        if (exceptionThrown) {
            println "\u001B[31mEntered the exceptionThrown branch\u001B[0m"
            sagaQuiz.state == Aggregate.AggregateState.ACTIVE
        } else {
            SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
            sagaTournament.startTime == TIME_1
            sagaTournament.state == Aggregate.AggregateState.DELETED
        }
        

        cleanup: 'remove all generated artifacts after test execution'
        behaviourService.cleanDirectory()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
