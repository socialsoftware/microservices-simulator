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

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.AddParticipantFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.RemoveTournamentFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalitiesWorkflows.UpdateStudentNameFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@DataJpaTest
class TournamentFunctionalityTestSagas extends SpockTest {
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

    // update name in course execution and add student in tournament

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

    // NEW
    def 'concurrent add student as tournament participant and update name in course execution - add student finishes first' () {
        given: 'student is added to tournament'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionality.getClass().getSimpleName()
        def functionalityName2 = AddParticipantFunctionality.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionality(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionality(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)

        updateStudentNameFunctionality.executeUntilStep("getOldCourseExecutionStep", unitOfWork1) 
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

    /*
    def 'concurrent add student as tournament participant and update name in course execution - add student finishes first' () {
        given: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()
        and: 'student name is updated and the commit does not require merge'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'update name event is processed such that the participant is updated in tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }
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

    // update creator name in course execution and add creator in tournament

    // check
    def 'sequential update creator name in course execution and add creator as tournament participant: fails because when creator is added tournament have not process the event yet' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'add creator as participant where the creator in tournament still has the old name'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because course execution emitted the update event but it was not processed by the tournament'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED
        and: 'when event is finally processed it updates the creator name'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        and: 'creator can be added as participant because tournament has processed all events it subscribes from course execution'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())
        and: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'the creator is participant with updated name'
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'sequential add creator as tournament participant and update creator name in course execution' () {
        given: 'add creator as participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())
        and: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'when event is processed it updates the creator name'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'the name is update and the creator is a participant'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'the creator is participant with updated name'
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    // TODO: decide how sagas become consistent (events)
    // NEW
    def 'concurrent add creator as tournament participant and update name in course execution: update name finishes first and event processing is after everything finished' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionality.getClass().getSimpleName()
        def functionalityName2 = AddParticipantFunctionality.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionality(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionality(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)

        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2) 
        updateStudentNameFunctionality.executeWorkflow(unitOfWork1) 
        
        when:
        addParticipantFunctionality.resumeWorkflow(unitOfWork2) 

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is added as participant with old name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == userCreatorDto.getName()
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == userCreatorDto.getName()

        when:
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the participant name is updated'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.creator.name == UPDATED_NAME
        tournamentDtoResult2.getParticipants().size() == 1
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    // NEW (fails)
    def 'concurrent add creator as tournament participant and update name in course execution: update name finishes first and event processing is during addParticipant' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionality.getClass().getSimpleName()
        def functionalityName2 = AddParticipantFunctionality.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionality(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionality(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)

        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2) 
        updateStudentNameFunctionality.executeWorkflow(unitOfWork1) 
        tournamentEventHandling.handleUpdateStudentNameEvent();
        
        when:
        addParticipantFunctionality.resumeWorkflow(unitOfWork2) 

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is added as participant with old name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }


    /*
    def 'concurrent add creator as tournament participant and update name in course execution: update name finishes first and event processing is concurrent with add creator' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)
        and: 'process update name event which updates the name of the creator in the tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'trying to add creator as participant using the version of the user before update'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because the event tournament subscribes an event that it has not processed, ' +
                'the course execution emitted the event and it is subscribed due to the participant which of a older version'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED
        and: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'there is no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }
    */

    // check
    def 'concurrent add creator as tournament participant and update name in course execution - update name finishes first and event processing starts before add creator finishes' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()
        and: 'add creator as participant which uses a previous version of the name, creator and participant have the same info'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'process update name in the tournament that does not have participant, so only the creator is updated, and' +
                'when merging with the tournament that has participant, creator and participant have different names'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'fails because invariant about same info for creator and participant, if the creator, breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK
        and: 'process update name event using tournament version that has the creator and the participant'
        tournamentEventHandling.handleUpdateStudentNameEvent();
        and: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'the creator is participant with updated name'
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'concurrent add creator as tournament participant and update name in course execution: add creator finishes first' () {
        given: 'add creator as participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()
        and: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'the event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'the creator is a participant with the correct name'
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    // anonymize creator in course execution and add student in tournament

    // check
    def 'sequential anonymize creator and add student: event is processed before add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails during commit because tournament is inactive due to anonymous creator'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE
        and: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    // check
    def 'sequential anonymize creator and add student: event is processed after add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because it is not possible to get a causal snapshot, ' +
                'course execution emitted an event that is not processed by tournament'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        and: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    // check
    def 'concurrent anonymize creator and add student: anonymize finishes first' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'another student is concurrently added to a tournament where the creator was anonymized in the course execution' +
                'but not in the tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails during merge because course execution emitted an event that was not processed by the tournament version'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_PERFORM_CAUSAL_READ_DUE_TO_EMITTED_EVENT_NOT_PROCESSED
        and: 'creator is anonymized'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == ANONYMOUS
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.username == ANONYMOUS
        and: 'the tournament is inactive and the creator anonymized'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.state == Aggregate.AggregateState.INACTIVE.toString()
        tournamentDtoResult.creator.name == ANONYMOUS
        tournamentDtoResult.creator.username == ANONYMOUS
        and: 'there are no participants'
        tournamentDtoResult.getParticipants().size() == 0
    }

    // delete tournament and add student in tournament

    def 'sequential remove tournament and add student' () {
        given: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'a student is added to a tournament that is removed'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because the the tournament is not found'
        def tournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournament == null
        // change to this
        //def error = thrown(TutorException)
        //error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    // NEW (fails) handleevent para este?
    def 'concurrent remove tournament and add student: add student finishes first' () {
        given: 'student added before tournament removal'
        def functionalityName1 = AddParticipantFunctionality.getClass().getSimpleName()
        def functionalityName2 = RemoveTournamentFunctionality.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality = new AddParticipantFunctionality(tournamentService, courseExecutionService, unitOfWorkService, 
                                                        tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork1)
        def removeTournamentFunctionality = new RemoveTournamentFunctionality(tournamentService,unitOfWorkService, tournamentFactory,
                                                        tournamentDto.getAggregateId(), unitOfWork2)

        removeTournamentFunctionality.executeUntilStep("getTournamentStep", unitOfWork2) 
        addParticipantFunctionality.executeWorkflow(unitOfWork1) 
        
        when: 'remove tournament concurrently with add'
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2) 


        then: 'fails during merge because breaks invariant that forbids to delete a tournament with participants'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult != null
        tournamentDtoResult.getParticipants().size() == 1
    }

    /*
    // check
    def 'concurrent remove tournament and add student: add student finishes first' () {
        given: 'add student'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'remove tournament concurrently with add'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        then: 'fails during merge because breaks invariant that forbids to delete a tournament with participants'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK
    }
    */

    // NEW (fails) handleevent para este?
    def 'concurrent remove tournament and add student: remove tournament finishes first' () {
        given: 'tournament removal before student added'
        def functionalityName1 = AddParticipantFunctionality.getClass().getSimpleName()
        def functionalityName2 = RemoveTournamentFunctionality.getClass().getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality = new AddParticipantFunctionality(tournamentService, courseExecutionService, unitOfWorkService, 
                                                        tournamentDto.getAggregateId(), userDto.getAggregateId(), unitOfWork1)
        def removeTournamentFunctionality = new RemoveTournamentFunctionality(tournamentService,unitOfWorkService, tournamentFactory,
                                                        tournamentDto.getAggregateId(), unitOfWork2)

        addParticipantFunctionality.executeUntilStep("getTournamentStep", unitOfWork1) 
        removeTournamentFunctionality.executeWorkflow(unitOfWork2) 
        
        when: 'remove tournament concurrently with add student'
        addParticipantFunctionality.resumeWorkflow(unitOfWork1) 

        then: 'the tournament is deleted'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult == null
    }

    /*
    // check
    def 'concurrent remove tournament and add student: remove finishes first' () {
        given: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'a student is concurrently added to a tournament that is not deleted'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails during merge because the most recent version of the tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_DELETED
    }
    */

    // delete tournament and update start time

    def 'concurrent remove tournament and update start time: update start time finishes first' () {
        given: 'update start time'
        def updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        def topics =  new HashSet<>(Arrays.asList(topicDto1.aggregateId,topicDto2.aggregateId))
        tournamentFunctionalities.updateTournament(updateTournamentDto, topics)
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()
        and: 'remove tournament which merges with the update'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'find the tournament'
        tournamentFunctionalities.findTournament(tournamentDto.aggregateId)

        then: 'after merge the tournament is removed, not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    // update topics in tournament and update topics in tournament

    def 'concurrent change of tournament topics' () {
        given: 'update topics to topic 2'
        def updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setNumberOfQuestions(1)
        def topics =  new HashSet<>(Arrays.asList(topicDto2.aggregateId))
        tournamentFunctionalities.updateTournament(updateTournamentDto, topics)
        and: 'the version number is decreased to simulate concurrency'
        versionService.decrementVersionNumber()

        when: 'update topics to topic 3 in the same concurrent version of the tournament'
        topics =  new HashSet<>(Arrays.asList(topicDto3.aggregateId));
        tournamentFunctionalities.updateTournament(updateTournamentDto, topics)

        then: 'as result of the merge a new quiz is created for the last committed topics'
        def quizDtoResult = quizFunctionalities.findQuiz(tournamentDto.quiz.aggregateId)
        quizDtoResult.questionDtos.size() == 1
        quizDtoResult.questionDtos.get(0).aggregateId == questionDto3.aggregateId
        and: 'the tournament topics are updated and it refers to the new quiz'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.topics.size() == 1
        tournamentDtoResult.topics.find{it.aggregateId == topicDto3.aggregateId} != null
        tournamentDtoResult.quiz.aggregateId == tournamentDto.quiz.aggregateId
    }

    /*------------------------------------------------------------------------------------------------------------------------*/

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
        def tournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())

        then:
        tournament == null
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
        removedTournament == null
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}