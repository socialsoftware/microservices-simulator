package pt.ulisboa.tecnico.socialsoftware.ms.functionality

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.causal.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaCourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaQuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.OptionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaQuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaTopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaTournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaUserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.SagasCourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.*

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@DataJpaTest
class TournamentFunctionalityTestSagas extends SpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private SagaCourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private SagaUserFunctionalities userFunctionalities
    @Autowired
    private SagaTopicFunctionalities topicFunctionalities
    @Autowired
    private SagaQuestionFunctionalities questionFunctionalities
    @Autowired
    private SagaQuizFunctionalities quizFunctionalities
    @Autowired
    private SagaTournamentFunctionalities tournamentFunctionalities

    @Autowired
    private VersionService versionService;

    @Autowired
    private TournamentEventHandling tournamentEventHandling

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def setup() {
        given: 'a course execution'
        courseExecutionDto = new CourseExecutionDto()
        courseExecutionDto.setName('BLCM')
        courseExecutionDto.setType('TECNICO')
        courseExecutionDto.setAcronym('TESTBLCM')
        courseExecutionDto.setAcademicTerm('2022/2023')
        courseExecutionDto.setEndDate(DateHandler.toISOString(TIME_4))
        courseExecutionDto = courseExecutionFunctionalities.createCourseExecution(courseExecutionDto)
        courseExecutionDto = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())

        userCreatorDto = new UserDto()
        userCreatorDto.setName('Name' + 1)
        userCreatorDto.setUsername('Username' + 1)
        userCreatorDto.setRole('STUDENT')
        userCreatorDto = userFunctionalities.createUser(userCreatorDto)

        userFunctionalities.activateUser(userCreatorDto.getAggregateId())

        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        userDto = new UserDto()
        userDto.setName('Name' + 2)
        userDto.setUsername('Username' + 2)
        userDto.setRole('STUDENT')
        userDto = userFunctionalities.createUser(userDto)
        userFunctionalities.activateUser(userDto.aggregateId)

        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        topicDto1 = new TopicDto()
        topicDto1.setName('Topic' + 1)
        topicDto1 = topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto1)
        topicDto2 = new TopicDto()
        topicDto2.setName('Topic' + 2)
        topicDto2 = topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto2)
        topicDto3 = new TopicDto()
        topicDto3.setName('Topic' + 3)
        topicDto3 = topicFunctionalities.createTopic(courseExecutionDto.getCourseAggregateId(), topicDto3)

        questionDto1 = new QuestionDto()
        questionDto1.setTitle('Title' + 1)
        questionDto1.setContent('Content' + 1)
        def set =  new HashSet<>(Arrays.asList(topicDto1));
        questionDto1.setTopicDto(set)
        def optionDto1 = new OptionDto()
        optionDto1.setSequence(1)
        optionDto1.setCorrect(true)
        optionDto1.setContent("Option" + 1)
        def optionDto2 = new OptionDto()
        optionDto2.setSequence(2)
        optionDto2.setCorrect(false)
        optionDto2.setContent("Option" + 2)
        questionDto1.setOptionDtos([optionDto1,optionDto2])
        questionDto1 = questionFunctionalities.createQuestion(courseExecutionDto.getCourseAggregateId(), questionDto1)

        questionDto2 = new QuestionDto()
        questionDto2.setTitle('Title' + 2)
        questionDto2.setContent('Content' + 2)
        set =  new HashSet<>(Arrays.asList(topicDto2));
        questionDto2.setTopicDto(set)
        questionDto2.setOptionDtos([optionDto1,optionDto2])
        questionDto2 = questionFunctionalities.createQuestion(courseExecutionDto.getCourseAggregateId(), questionDto2)

        questionDto3 = new QuestionDto()
        questionDto3.setTitle('Title' + 2)
        questionDto3.setContent('Content' + 2)
        set =  new HashSet<>(Arrays.asList(topicDto3));
        questionDto3.setTopicDto(set)
        questionDto3.setOptionDtos([optionDto1,optionDto2])
        questionDto3 = questionFunctionalities.createQuestion(courseExecutionDto.getCourseAggregateId(), questionDto3)

        tournamentDto = new TournamentDto()
        tournamentDto.setStartTime(DateHandler.toISOString(TIME_1))
        tournamentDto.setEndTime(DateHandler.toISOString(TIME_3))
        tournamentDto.setNumberOfQuestions(2)
        tournamentDto = tournamentFunctionalities.createTournament(userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(),
                [topicDto1.getAggregateId(),topicDto2.getAggregateId()], tournamentDto)
    }

    def cleanup() {

    }

    def "create tournament successfully"() {
        when:
        def result = tournamentFunctionalities.createTournament(userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()], new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2))

        then:
        result != null
        LocalDateTime.parse(result.startTime, DateTimeFormatter.ISO_DATE_TIME) == TIME_1
        LocalDateTime.parse(result.endTime, DateTimeFormatter.ISO_DATE_TIME) == TIME_3
    
        result.numberOfQuestions == 2
        result.topics*.aggregateId.containsAll([topicDto1.getAggregateId(), topicDto2.getAggregateId()])
    }

    def "create tournament with invalid input"() {
        when:
        tournamentFunctionalities.createTournament(null, courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()], new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2))

        then:
        thrown(TutorException)
    }

    def "saga compensations"() {
        given:
        def unitOfWork = unitOfWorkService.createUnitOfWork("TEST");
        def tournamentDto = new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2)
        
        when:
        tournamentFunctionalities.createTournament(userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId(), 999], tournamentDto)

        then:
        def courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionDto.getAggregateId(), unitOfWork)
        courseExecution.sagaState == SagaState.NOT_IN_SAGA;
        def topic1 = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto1.getAggregateId(), unitOfWork)
        def topic2 = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto2.getAggregateId(), unitOfWork)
        topic1.sagaState == SagaState.NOT_IN_SAGA;
        topic2.sagaState == SagaState.NOT_IN_SAGA;
        
        when:
        tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())

        then:
        thrown(TutorException)
    }

    def "find tournament successfully"() {
        when:
        def foundTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())

        then:
        foundTournament.getStartTime() == DateHandler.toISOString(TIME_1)
        foundTournament.getEndTime() == DateHandler.toISOString(TIME_3)
        foundTournament.getNumberOfQuestions() == 2
    }

    def "create add participant successfully"() {
        given: 'a new user'
        def newUserDto = new UserDto()
        newUserDto.setName('NewUser')
        newUserDto.setUsername('NewUsername')
        newUserDto.setRole('STUDENT')
        newUserDto = userFunctionalities.createUser(newUserDto)
        userFunctionalities.activateUser(newUserDto.getAggregateId())
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), newUserDto.getAggregateId())

        when: 'adding the new user as a participant to the tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), newUserDto.getAggregateId())

        then: 'the participant should be added successfully'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.any { it.aggregateId == newUserDto.getAggregateId() }
    }

    def "update tournament successfully"() {
        given:
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when:
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)
        def updatedTournamentDto = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())

        then:
        updatedTournamentDto != null
        updatedTournamentDto.topics*.aggregateId.containsAll([topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()])
    }

    def "leave tournament successfully"() {
        given:
        def userToLeaveDto = new UserDto()
        userToLeaveDto.setName('TestUser')
        userToLeaveDto.setUsername('TestUsername')
        userToLeaveDto.setRole('STUDENT')
        userToLeaveDto = userFunctionalities.createUser(userToLeaveDto)
        userFunctionalities.activateUser(userToLeaveDto.aggregateId)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userToLeaveDto.aggregateId)
        tournamentFunctionalities.addParticipant(tournamentDto.aggregateId, userToLeaveDto.aggregateId)

        when:
        tournamentFunctionalities.leaveTournament(tournamentDto.aggregateId, userToLeaveDto.aggregateId)

        then:
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        !updatedTournament.participants.any { it.aggregateId == userToLeaveDto.aggregateId }
    }

    /*
    def "solve quiz successfully"() {
        given:
        def userToSolveDto = new UserDto()
        userToSolveDto.setName('TestUser')
        userToSolveDto.setUsername('TestUsername')
        userToSolveDto.setRole('STUDENT')
        userToSolveDto = userFunctionalities.createUser(userToSolveDto)
        userFunctionalities.activateUser(userToSolveDto.aggregateId)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userToSolveDto.aggregateId)
        tournamentFunctionalities.addParticipant(tournamentDto.aggregateId, userToSolveDto.aggregateId)

        when:
        def quizDto = tournamentFunctionalities.solveQuiz(tournamentDto.aggregateId, userToSolveDto.aggregateId)
        
        then:
        //TODO
    }
    */
    
    def "cancel tournament successfully"() {
        when:
        tournamentFunctionalities.cancelTournament(tournamentDto.aggregateId)

        then:
        def canceledTournament = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)
        canceledTournament.isCancelled() == true
    }

    def "remove tournament successfully"() {
        when:
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        def removedTournament = tournamentFunctionalities.findTournament(tournamentDto.aggregateId)

        then:
        thrown(TutorException)
        removedTournament == null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}