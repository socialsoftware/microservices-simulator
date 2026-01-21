package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.RemoveTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

@DataJpaTest
class RemoveTournamentAndUpdateTournamentTest extends QuizzesSpockTest {
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

    @Autowired
    private LocalCommandGateway commandGateway;

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

        and: 'information required to update tournament'
        functionalityName1 = UpdateTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        topics =  new HashSet<>(Arrays.asList(topicDto1.aggregateId,topicDto2.aggregateId))
        updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(unitOfWorkService,
                updateTournamentDto, topics, unitOfWork1, commandGateway)

        and: 'information required to remove tournament'
        functionalityName2 = RemoveTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)
        removeTournamentFunctionality = new RemoveTournamentFunctionalitySagas(unitOfWorkService, tournamentDto.aggregateId, unitOfWork2, commandGateway)
    }

    def cleanup() {}

    def 'sequential: update; remove' () {
        given: 'a tournament update'
        tournamentFunctionalities.updateTournament(tournamentDto, topics)
        and: 'the tournament is deleted'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'try to get tournament'
        tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        then: 'tournament does not exist'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND

        when: 'find quiz'
        quizFunctionalities.findQuiz(tournamentDto.getQuiz().aggregateId)
        then: 'the quiz is removed, not found'
        def error1 = thrown(SimulatorException)
        error1.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND

        when: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        then: 'compensation did not execute'
        sagaTournament.state == Aggregate.AggregateState.DELETED
        sagaTournament.startTime == TIME_1

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'it is not consistent with remove tournament'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
        sagaQuiz.availableDate == TIME_1
    }

    def 'sequential: remove; update' () {
        given: 'the tournament is deleted'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'a tournament update'
        tournamentFunctionalities.updateTournament(tournamentDto, topics)
        then: 'tournament does not exist'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND

        when: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        then: 'compensation did not execute'
        sagaTournament.state == Aggregate.AggregateState.DELETED
        sagaTournament.startTime == TIME_1

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'it is not consistent with remove tournament'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
        sagaQuiz.availableDate == TIME_1
    }

    def 'concurrent: update - getOriginalTournamentStep; remove; update - resume' () {
        given: 'update start time until getOriginalTournamentStep'
        updateTournamentFunctionality.executeUntilStep("getOriginalTournamentStep", unitOfWork1)

        when: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'tournament does not exist'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: update - getOriginalTournamentStep; remove - removeQuizStep; update - resume; remove - resume' () {
        given: 'update start time until getOriginalTournamentStep'
        updateTournamentFunctionality.executeUntilStep("getOriginalTournamentStep", unitOfWork1)

        when: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)
        then: 'it is being updated'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: update - getTopicsStep; remove; update - resume' () {
        given: 'update start time until getTopicsStep'
        updateTournamentFunctionality.executeUntilStep("getTopicsStep", unitOfWork1)

        when: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'it is being updated'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: update - getTopicsStep; remove - removeQuizStep; update - resume; remove - resume' () {
        given: 'update start time until getTopicsStep'
        updateTournamentFunctionality.executeUntilStep("getTopicsStep", unitOfWork1)

        when: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)
        then: 'it is being updated'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: update - updateTournamentStep; remove; update - resume' () {
        given: 'update start time until updateTournamentStep'
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'it is being updated'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: update - updateTournamentStep; remove - updateQuizStep; update - resume; remove - resume' () {
        given: 'update start time until updateTournamentStep'
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)
        then: 'it is being updated'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'try to finish update'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is update'
        def tournamentDto = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        tournamentDto.startTime == DateHandler.toISOString(TIME_2)
        and: 'quiz is updated'
        def quizDto = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDto.availableDate == DateHandler.toISOString(TIME_2)

        when: 'remove is retried'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: remove - removeQuizStep; update; remove - resume'() {
        given: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)

        when: 'tournament is updated'
        tournamentFunctionalities.updateTournament(tournamentDto, topics)
        then: 'it is being deleted'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'remove finishes'
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.startTime == TIME_1
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.availableDate == TIME_1
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    def 'concurrent: remove - removeQuizStep; update - updateTournamentStep; remove - resume; update - resume'() {
        given: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeUntilStep("removeQuizStep", unitOfWork2)

        when: 'update start time until updateTournamentStep'
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)
        then: 'it is being deleted'
        def error = thrown(SimulatorException)
        error.errorMessage == SimulatorErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        and: 'remove finishes'
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2)
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'get the deleted tournament'
        SagaTournament sagaTournament = unitOfWorkService.aggregateDeletedLoad(tournamentDto.aggregateId)
        and: 'is deleted'
        sagaTournament.startTime == TIME_1
        sagaTournament.state == Aggregate.AggregateState.DELETED

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.availableDate == TIME_1
        sagaQuiz.state == Aggregate.AggregateState.DELETED
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}