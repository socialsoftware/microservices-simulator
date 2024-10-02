package pt.ulisboa.tecnico.socialsoftware.ms.functionality

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.ms.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.OptionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.factories.SagasCourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.*

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.RemoveTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.UpdateStudentNameFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@DataJpaTest
class NewTest extends SpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;
    @Autowired
    private TournamentFactory tournamentFactory;

    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private UserFunctionalities userFunctionalities
    @Autowired
    private TopicFunctionalities topicFunctionalities
    @Autowired
    private QuestionFunctionalities questionFunctionalities
    @Autowired
    private QuizFunctionalities quizFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private VersionService versionService;
    @Autowired
    private EventService eventService;

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
        questionDto3.setTitle('Title' + 3)
        questionDto3.setContent('Content' + 3)
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

    // update name in course execution and add student in tournament

    /* Case C: involves writing to the main aggregate (the course execution aggregate in this case). 
    The anomaly described in Case C, where a lost update can occur, could happen if an intermediate state is overwritten. 
    The functionality writes to the student’s information in the course execution first, 
    and then checks if it was propagated properly to the tournament
    */
    def 'sequential update name in course execution and then add student as tournament participant' () {
        given: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find { it.aggregateId == userDto.aggregateId }.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find { it.aggregateId == userDto.aggregateId }.name == UPDATED_NAME
    }

    /* Case D: it deals with writing to both the main and secondary upstream aggregates. 
    Here, the student is first added to the tournament (which can be seen as a secondary upstream aggregate in this context), 
    and then their name is updated in the course execution (main aggregate). 
    The event processing ensures consistency across both aggregates, avoiding lost updates.
    */
    def 'sequential add student as tournament participant and then update name in course execution' () {
        given: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
        and: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'update name event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }

    def 'sequential remove tournament and add student' () {
        given: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'a student is added to a tournament that is removed'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
        then: 'the tournament is removed, not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    /* Case B deals with reading and writing from both main and secondary aggregates 
    (in this case, course execution and tournament). 
    A dirty read occurs when the student is added to the tournament with the old name, 
    and non-repeatable read can happen as the name is updated later and must be synchronized across aggregates.
    */
    def 'concurrent add student as tournament participant and update name in course execution - add student finishes first' () {
        given: 'student is added to tournament'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.getClass().getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionalitySagas(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)

        updateStudentNameFunctionality.executeUntilStep("getCourseExecutionStep", unitOfWork1) 
        addParticipantFunctionality.executeWorkflow(unitOfWork2) 
        
        when:
        updateStudentNameFunctionality.resumeWorkflow(unitOfWork1) 

        then: 'student is added with old name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == userDto.name

        when: 'update name event is processed such that the participant is updated in tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }

    /* Case B (Dirty Read, Non-repeatable Read):
    Anomalies: This case involves the concurrency issue where the tournament aggregate reads an outdated version of the student's name. 
    The system tries to update the name in the course execution, 
    but the tournament does not immediately pick up this change. 
    This can be viewed as a dirty read and non-repeatable read anomaly since the tournament 
    first reads old data and then later needs to be updated with the correct name.

    Case C (Lost Update):
    Anomalies: The outdated version issue is a classic example of a lost update anomaly. 
    The tournament functionality adds the student using an outdated version of their data, 
    simulating a lost update because the name change was not initially reflected.
    */
    def 'concurrent add student as tournament participant and update name in course execution - update name finishes first' () {
        given: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)
        and: 'try to process update name event but there are no subscribers'
        tournamentEventHandling.handleUpdateStudentNameEvent();
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()
        and: 'student is added to tournament but uses the version without update'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        when: 'update name event is processed such that the participant is updated in tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}