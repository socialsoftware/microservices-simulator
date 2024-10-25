package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaQuizDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaTournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.execution.AnonymizeStudentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament.UpdateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState


@DataJpaTest
class AnonymizeStudentAndUpdateTournamentTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private TournamentService tournamentService
    @Autowired
    private TopicService topicService
    @Autowired
    private QuizService quizService
    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1, unitOfWork2

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
      
        def functionalityName1 = UpdateTournamentFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AnonymizeStudentFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)
    }

    def 'sequential: update tournament; anonymize creator; event' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when: 'the tournament is updated'
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)
        then: 'the tournament is updated'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.numberOfQuestions == 3
        tournamentDtoResult.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        def quizDto = quizFunctionalities.findQuiz(tournamentDtoResult.quiz.aggregateId)
        quizDto.questionDtos.size() == 3

        when: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament has not processed the anonymized event'
        tournamentDtoResult.state == Aggregate.AggregateState.ACTIVE.toString()
        tournamentDtoResult.creator.name == userCreatorDto.name
        tournamentDtoResult.creator.username == userCreatorDto.username

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult2.creator.name == ANONYMOUS
        tournamentDtoResult2.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult2.getParticipants().size() == 0
    }

    def 'sequential: anonymize creator; event; update tournament' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        then: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult2.creator.name == ANONYMOUS
        tournamentDtoResult2.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult2.getParticipants().size() == 0

        when: 'the tournament is updated'
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)
        then: 'cannot update inactive tournament'
        def error  = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE
    }

    def 'sequential: anonymize creator; update tournament; event' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'the tournament is updated'
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)
        then: 'the tournament is updated'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.numberOfQuestions == 3
        tournamentDtoResult.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        def quizDto = quizFunctionalities.findQuiz(tournamentDtoResult.quiz.aggregateId)
        quizDto.questionDtos.size() == 3
        and: 'the tournament has not processed the anonymized event'
        tournamentDtoResult.state == Aggregate.AggregateState.ACTIVE.toString()
        tournamentDtoResult.creator.name == userCreatorDto.name
        tournamentDtoResult.creator.username == userCreatorDto.username

        then: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult2.creator.name == ANONYMOUS
        tournamentDtoResult2.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult2.getParticipants().size() == 0
    }

    def 'concurrent: update - getOriginalTournamentStep; anonymize creator; update - resume; event' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes first step'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("getOriginalTournamentStep", unitOfWork1)
        and: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        
        when: 'the tournament finishes updating'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament has not processed the anonymized event'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.ACTIVE.toString()
        tournamentDtoResult.creator.name == userCreatorDto.name
        tournamentDtoResult.creator.username == userCreatorDto.username
        and: 'the tournament is updated'
        tournamentDtoResult.numberOfQuestions == 3
        tournamentDtoResult.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        def quizDto = quizFunctionalities.findQuiz(tournamentDtoResult.quiz.aggregateId)
        quizDto.questionDtos.size() == 3

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult2.creator.name == ANONYMOUS
        tournamentDtoResult2.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult2.getParticipants().size() == 0
    }

    def 'concurrent: update - getOriginalTournamentStep; anonymize creator; event; update - resume;' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes first step'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("getOriginalTournamentStep", unitOfWork1)

        when: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is being updated'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'the tournament finishes updating'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is changed'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.numberOfQuestions == 3
        tournamentDtoResult2.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        def quizDto = quizFunctionalities.findQuiz(tournamentDtoResult2.quiz.aggregateId)
        quizDto.questionDtos.size() == 3

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'tournament becomes inactive'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
    }

    def 'concurrent: update - updateTournamentStep; anonymize creator; update - resume; event' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes first step'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'the tournament finishes updating'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'tournament is updated'
        def updatedTournamentDto = (SagaTournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 3
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'quiz is changed'
        def quizDto = (SagaQuizDto) quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.questionDtos.size() == 3

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    def 'concurrent: update - updateTournamentStep; anonymize creator; event; update - resume;' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(3)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes more steps and writes a new tournament'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'tournament processes event to anonymize the creator but does not anonymize the updated'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is being updated'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'the tournament finishes updating'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'the tournament is updated'
        def updatedTournamentDto = (SagaTournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 3
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'quiz is changed'
        def quizDto = (SagaQuizDto) quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.questionDtos.size() == 3

        when: 'tournament retry to process the event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'tournament is inactive'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'contains the update'
        tournamentDtoResult.numberOfQuestions == 3
        tournamentDtoResult.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
    }

    def 'concurrent: update - updateTournamentStep; anonymize creator; update - resume fails: event: creator still anonymized despite compensations' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(4)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes first step'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'the tournament finishes updating by trying to create the quiz'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'there are not enough questions'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.NOT_ENOUGH_QUESTIONS
        and: 'compensation is executed'
        def updatedTournamentDto = (SagaTournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 2
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(),topicDto2.getAggregateId()].toSet()
        and: 'saga sate is undone'
        updatedTournamentDto.sagaState == GenericSagaState.NOT_IN_SAGA
        and: 'quiz is not changed'
        def quizDto = (SagaQuizDto) quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.questionDtos.size() == 2
        quizDto.sagaState == GenericSagaState.NOT_IN_SAGA

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    def 'concurrent: update - updateTournamentStep; anonymize creator; event; update - resume fails: creator still anonymized despite compensations' () {
        given: 'topics to update the tournament'
        tournamentDto.setNumberOfQuestions(4)
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()
        and: 'the tournament update executes first step'
        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, tournamentDto, topicsAggregateIds, unitOfWork1)
        updateTournamentFunctionality.executeUntilStep("updateTournamentStep", unitOfWork1)

        when: 'the creator is anonymized'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        then: 'the creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is being updated'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'the tournament finishes updating by trying to create the quiz'
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1)
        then: 'there are not enough questions'
        def error2 = thrown(TutorException)
        error2.errorMessage == ErrorMessage.NOT_ENOUGH_QUESTIONS
        and: 'compensation is executed'
        def updatedTournamentDto = (SagaTournamentDto) tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournamentDto.numberOfQuestions == 2
        updatedTournamentDto.topics*.aggregateId.toSet() == [topicDto1.getAggregateId(),topicDto2.getAggregateId()].toSet()
        and: 'saga sate is undone'
        updatedTournamentDto.sagaState == GenericSagaState.NOT_IN_SAGA
        and: 'quiz is not changed'
        def quizDto = (SagaQuizDto) quizFunctionalities.findQuiz(updatedTournamentDto.quiz.aggregateId)
        quizDto.questionDtos.size() == 2
        quizDto.sagaState == GenericSagaState.NOT_IN_SAGA

        when: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        then: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }


    def cleanup() {}


    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}