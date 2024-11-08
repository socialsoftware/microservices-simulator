package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.execution.UpdateStudentNameFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.tournament.RemoveTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate

@DataJpaTest
class FaultTest extends QuizzesSpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private QuizService quizService

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private EventService eventService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def unitOfWork1, unitOfWork2
    def removeTournamentFunctionality

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

        and: 'two units of work'
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)


        and: 'information required to remove tournament'
        functionalityName2 = RemoveTournamentFunctionalitySagas.class.getSimpleName()
        unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)
        removeTournamentFunctionality = new RemoveTournamentFunctionalitySagas(tournamentService, quizService, unitOfWorkService, tournamentDto.aggregateId, unitOfWork2)
    
    }

    def cleanup() {}

    // add student other than creator


    def'Crash-Fault' () {
        given: 'add participant executes the first step'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)

        when: 'Simulate a failure at "getUserStep" step'
        addParticipantFunctionality.executeUntilError("getUserStep", unitOfWork2)

        then: 'Receive exception'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CRASH
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().size() == 0
        
        when: 'Try to resume the workflow'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)
        then: 'Its not possible because of crash'
        thrown(NullPointerException)

        when: 'Retry the workflow execution to the same step'
        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2)

        then: 'No exception'
        notThrown(TutorException)
        
        when: 'creator is added as participant'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)
        then: 'the creator is added as participant with new name'
        def tournamentDtoResult3 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult3.getParticipants().size() == 1
        
    }


    def'CrashRecovery-fault' () {
        given: 'add participant executes the first step'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)

        when: 'Simulate a failure at "getUserStep" step'
        addParticipantFunctionality.executeUntilErrorWithRecovery("getUserStep", unitOfWork2)

        then: 'Receive exception'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CRASH
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().size() == 0
        
        when: 'Try to resume the workflow'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)
        then: 'the creator is added as participant with new name'
        def tournamentDtoResult3 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult3.getParticipants().size() == 1
        
    }


    def 'Omission-fault' () {

        when: 'remove tournament until removeQuizStep'
        removeTournamentFunctionality.executeWithOmission(unitOfWork2)
        then: 'it is being updated'

        when: 'get the deleted quiz'
        SagaQuiz sagaQuiz= unitOfWorkService.aggregateDeletedLoad(tournamentDto.quiz.aggregateId)
        then: 'is deleted'
        sagaQuiz.state == Aggregate.AggregateState.DELETED

        when: 'remove is retried'
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2)
        then: 'already deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND

       
    }


    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}