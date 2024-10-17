package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination

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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.*

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;


import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@DataJpaTest
class TournamentFunctionalitySagasTest extends SpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private CourseExecutionService courseExecutionService
    @Autowired
    private QuizAnswerService quizAnswerService
    @Autowired
    private QuizService quizService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private TopicService topicService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory
    @Autowired
    private TournamentFactory tournamentFactory
    @Autowired
    private QuizFactory quizFactory


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
    private QuizAnswerFunctionalities quizAnswerFunctionalities
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

    def 'sequential update name in course execution and then add student as tournament participant' () {
        given: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find { it.aggregateId == userDto.aggregateId }.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find { it.aggregateId == userDto.aggregateId }.name == UPDATED_NAME
    }

    def 'sequential add student as tournament participant and then update name in course execution' () {
        given: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
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

    def 'concurrent add student as tournament participant and update name in course execution - update name finishes first' () {
        given: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)
        and: 'try to process update name event but there are no subscribers'
        tournamentEventHandling.handleUpdateStudentNameEvent();
        
        and: 'student is added to tournament but uses the version without update'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

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

    def 'sequential update creator name in course execution and add creator as tournament participant: fails because when creator is added tournament have not process the event yet' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'add creator as participant where the creator in tournament still has the old name'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because invariant breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK

        then: 'when event is finally processed it updates the creator name'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        and: 'creator can be added as participant because tournament has processed all events it subscribes from course execution'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
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
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
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

    def 'concurrent add creator as tournament participant and update name in course execution: update name finishes first and event processing is during addParticipant' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionalitySagas(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)

        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2) 
        updateStudentNameFunctionality.executeWorkflow(unitOfWork1) 
        
        when:
        tournamentEventHandling.handleUpdateStudentNameEvent() 
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult1 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult1.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is not added as participant with new name'
        def tournamentDtoResult1 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult1.creator.name == UPDATED_NAME //name updated
        tournamentDtoResult1.getParticipants().size() == 0 //creator not added
        
        when:
        addParticipantFunctionality.resumeWorkflow(unitOfWork2) 
        then: 'fails because invariant breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK

        then:
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork3)
        addParticipantFunctionality2.executeWorkflow(unitOfWork1) 

        tournamentEventHandling.handleUpdateStudentNameEvent() 


        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is added as participant with new name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'concurrent add creator as tournament participant and update name in course execution: add creator finishes first' () {
        given: 'add creator as participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
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

    def 'sequential anonymize creator and add student: event is processed before add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because tournament is inactive'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE

        then: 'creator is anonymized'
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

    def 'sequential anonymize creator and add student: event is processed after add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'tournament processes event to anonymize the creator'
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

        and: 'participant was added before event processing'
        tournamentDtoResult.getParticipants().size() == 1
    }

    def 'sequential anonymize creator and add creator: cant add anonymous user' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        when: 'the creator is added as participant to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.USER_IS_ANONYMOUS
    }

    def 'concurrent anonymize creator and add student: anonymize finishes first' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'another student is concurrently added to a tournament where the creator was anonymized in the course execution' +
                'but not in the tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        then: 'fails because tournament is inactive'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE

        then: 'creator is anonymized'
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

    def 'sequential remove tournament and add student' () {
        given: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'a student is added to a tournament that is removed'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        then: 'the tournament is removed, not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    def 'concurrent remove tournament and update start time: remoce finishes first' () {
        given: 'update start time'
        def functionalityName1 = UpdateTournamentFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        def topics =  new HashSet<>(Arrays.asList(topicDto1.aggregateId,topicDto2.aggregateId))

        def updateTournamentFunctionality = new UpdateTournamentFunctionalitySagas(tournamentService, topicService, quizService, unitOfWorkService, 
                                tournamentFactory, quizFactory,
                                updateTournamentDto, topics, unitOfWork1)

        updateTournamentFunctionality.executeUntilStep("getOriginalTournamentStep", unitOfWork1) 
        
        when: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        updateTournamentFunctionality.resumeWorkflow(unitOfWork1) 

        then: 'fails because tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    def 'concurrent change of tournament topics' () {
        given: 'update topics to topic 2'
        def updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setNumberOfQuestions(1)
        def topics =  new HashSet<>(Arrays.asList(topicDto2.aggregateId))
        tournamentFunctionalities.updateTournament(updateTournamentDto, topics)

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

    // TODO reviw this
    def 'concurrent add two participants to tournament'() {
        given: 'two new users'
        def userDto1 = new UserDto()
        userDto1.setName('User1')
        userDto1.setUsername('Username1')
        userDto1.setRole('STUDENT')
        userDto1 = userFunctionalities.createUser(userDto1)
        userFunctionalities.activateUser(userDto1.getAggregateId())
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto1.getAggregateId())

        def userDto2 = new UserDto()
        userDto2.setName('User2')
        userDto2.setUsername('Username2')
        userDto2.setRole('STUDENT')
        userDto2 = userFunctionalities.createUser(userDto2)
        userFunctionalities.activateUser(userDto2.getAggregateId())
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userDto2.getAggregateId())

        and: 'create unit of works for concurrent addition of participants'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality1 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto1.getAggregateId(), unitOfWork1)
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork2)

        when: 
        addParticipantFunctionality1.executeUntilStep("getUserStep", unitOfWork1)
        addParticipantFunctionality2.executeWorkflow(unitOfWork2)

        /*
        then: 'tournament is locked'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'add finishes and add participant tries again'
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1) 
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName2)
        def addParticipantFunctionality3 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork3)
        addParticipantFunctionality3.executeWorkflow(unitOfWork3) 
        */
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1)


        then: 'both participants should be successfully added to the tournament'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 2
        updatedTournament.participants.any { it.aggregateId == userDto1.getAggregateId() }
        updatedTournament.participants.any { it.aggregateId == userDto2.getAggregateId() }
    }


    def 'sequential solve quiz and anonymize user'() {
        
        given: 'a quiz is solved for a user'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        tournamentFunctionalities.solveQuiz(tournamentDto.aggregateId, userDto.getAggregateId())

        when: 'the user is anonymized after starting the quiz'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.getAggregateId(), userDto.getAggregateId())
        tournamentEventHandling.handleAnonymizeStudentEvents()

        then: 'the user is anonymized in the course execution'
        def courseExecutionResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionResult.getStudents().find{ it.aggregateId == userDto.getAggregateId() }.name == ANONYMOUS

        and: 'the quiz is still started for the anonymized user'
        def unitOfWork = unitOfWorkService.createUnitOfWork("getQuizAnswerDtoByQuizIdAndUserId")
        def quizAnswerResult = quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(tournamentDto.quiz.aggregateId, userDto.getAggregateId(), unitOfWork)
        quizAnswerResult.getStudentName() == userDto.getName()
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
        def unitOfWork = unitOfWorkService.createUnitOfWork("TEST");
        def courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionDto.getAggregateId(), unitOfWork)
        courseExecution.sagaState == GenericSagaState.NOT_IN_SAGA;
    }
    def "create tournament with invalid input"() {
        when:
        tournamentFunctionalities.createTournament(null, courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()], new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2))

        then:
        thrown(TutorException)
    }

    def "saga compensations"() {
        given:
        def tournamentDto = new TournamentDto(startTime: DateHandler.toISOString(TIME_1), endTime: DateHandler.toISOString(TIME_3), numberOfQuestions: 2)
        
        when:
        tournamentFunctionalities.createTournament(userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId(), 999], tournamentDto)

        and: 'find the tournament'
        tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        then: 'the tournament is not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
        then:
        def unitOfWork = unitOfWorkService.createUnitOfWork("TEST");
        def courseExecution = (SagaCourseExecution) unitOfWorkService.aggregateLoadAndRegisterRead(courseExecutionDto.getAggregateId(), unitOfWork)
        courseExecution.sagaState == GenericSagaState.NOT_IN_SAGA;
        def topic2 = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto2.getAggregateId(), unitOfWork)
        def topic1 = (SagaTopic) unitOfWorkService.aggregateLoadAndRegisterRead(topicDto1.getAggregateId(), unitOfWork)
        topic1.sagaState == GenericSagaState.NOT_IN_SAGA;
        topic2.sagaState == GenericSagaState.NOT_IN_SAGA;
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
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), newUserDto.getAggregateId())

        then: 'the participant should be added successfully'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.any { it.aggregateId == newUserDto.getAggregateId() }
    }

    def "update tournament successfully"() {
        given:
        def topicsAggregateIds = [topicDto1.getAggregateId(), topicDto2.getAggregateId(), topicDto3.getAggregateId()].toSet()

        when:
        tournamentFunctionalities.updateTournament(tournamentDto, topicsAggregateIds)

        then:
        def updatedTournamentDto = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
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
        tournamentFunctionalities.addParticipant(tournamentDto.aggregateId, courseExecutionDto.getAggregateId(), userToLeaveDto.aggregateId)

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
        given: 'tournament is deleted'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'find the tournament'
        tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        then: 'the tournament is removed, not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}