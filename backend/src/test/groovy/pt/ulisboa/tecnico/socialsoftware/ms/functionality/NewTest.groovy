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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
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
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;


import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.*;
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
    private QuizAnswerService quizAnswerService
    @Autowired
    private QuizService quizService
    @Autowired
    private TournamentService tournamentService
    @Autowired
    private CourseExecutionFactory courseExecutionFactory
    @Autowired
    private TournamentFactory tournamentFactory

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


    /* Case B deals with reading and writing from both main and secondary aggregates 
    (in this case, course execution and tournament). 
    A dirty read occurs when the student is added to the tournament with the old name, 
    and non-repeatable read can happen as the name is updated later and must be synchronized across aggregates.
    */
    def 'concurrent add student as tournament participant and update name in course execution - add student finishes first' () {
        given: 'student is added to tournament'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
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

    // update creator name in course execution and add creator in tournament

    /* Case A (Dirty Read):
    Anomalies: In this case, a dirty read occurs when the tournament reads the outdated name of the creator 
    while trying to add them as a participant. This happens because the event that updates the name has not been processed yet.

    Case B (Non-repeatable Read, Phantom Read):
    Anomalies: The test highlights the issue of non-repeatable read since the tournament 
    first reads the old name, and then later, after event processing, it needs to read the updated name. 
    There could also be a phantom read if the data (the creator's name) changes between different local 
    transactions before the functionality is complete.
    */
    def 'sequential update creator name in course execution and add creator as tournament participant: fails because when creator is added tournament have not process the event yet' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'add creator as participant where the creator in tournament still has the old name'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because invariant breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK

        then: 'when event is finally processed it updates the creator name'
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

    /* Case B (Non-repeatable Read, Phantom Read):
    Anomalies: Since the creator is added as a participant with the old name and 
    the update event hasn't been processed yet, a non-repeatable read or phantom read situation arises. 
    Initially, the tournament uses the outdated name, and after the event is processed, the system needs to re-read the data to update the name.
    The test ensures that, after event handling, the correct name is propagated to both the course execution and the tournament, 
    ensuring consistency across the aggregates. This is crucial for maintaining eventual consistency in distributed systems.
    Case C (Lost Update):
    Anomalies: The update of the creator's name in the course execution without immediately propagating it to the tournament 
    could lead to a lost update if the data were not eventually synchronized. However, by processing the event and updating the tournament participant, the system avoids this anomaly.
    */
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

    /* Case C (Lost Update):
    Anomalies: Since the addParticipant process begins while the name update is ongoing, 
    there is a risk of a lost update if the tournament were to add the creator with the old name before processing the name change.

    Case B (Non-repeatable Read, Phantom Read):
    Anomalies: During the concurrent operation, the addParticipant functionality 
    initially uses the old state of the user (with the outdated name) until the update is processed. 
    This creates a non-repeatable read scenario, where different parts of the system might see different versions of the data (old vs. new name).
    */
    def 'concurrent add creator as tournament participant and update name in course execution: update name finishes first and event processing is during addParticipant' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        def functionalityName1 = UpdateStudentNameFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def updateStudentNameFunctionality = new UpdateStudentNameFunctionalitySagas(courseExecutionService, courseExecutionFactory, unitOfWorkService, courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto, unitOfWork1)
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)

        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2) 
        updateStudentNameFunctionality.executeWorkflow(unitOfWork1) 
        
        when:
        tournamentEventHandling.handleUpdateStudentNameEvent() 
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult1 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult1.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is added as participant with new name'
        def tournamentDtoResult1 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult1.creator.name == UPDATED_NAME //name updated
        tournamentDtoResult1.getParticipants().size() == 0 //creator not added
        
        when:
        addParticipantFunctionality.resumeWorkflow(unitOfWork2) 
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

    /* Case B (Non-repeatable Read, Phantom Read):
    Anomalies: The primary issue here is that the addParticipant operation uses 
    the old name while the name update is pending. This can cause non-repeatable 
    reads if the system reads the outdated name before processing the event.

    Case D (Lost Update):
    Anomalies: The lost update could occur if the tournament initially adds the 
    creator with an old name and doesn't update it after the event is processed.
    */
    def 'concurrent add creator as tournament participant and update name in course execution: add creator finishes first' () {
        given: 'add creator as participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())
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

    /* Case A (Dirty Read):
    Anomalies: Although not directly related to dirty reads, ensuring that the creator’s anonymization 
    is processed before adding a new participant helps prevent scenarios where outdated information might affect the addition of new participants.

    Case C (Lost Update):
    Anomalies: There is no direct lost update issue here as the anonymization is 
    processed before the new student is added. The focus is on ensuring that the 
    anonymization event is handled correctly before any new updates.

    Case E (Complex Functionality):
    Anomalies: Anonymization involves updating multiple states (creator and participants), 
    and ensuring that the tournament becomes inactive after anonymization reflects proper handling of complex functionality.
    */
    def 'sequential anonymize creator and add student: event is processed before add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

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

    /* 
    Case A (Dirty Read):
    Anomalies: By adding a student before processing the anonymization, this test ensures that 
    there are no inconsistencies or dirty reads where a partially anonymized or 
    inconsistent state might affect the addition of participants.
    
    Case C (Lost Update):
    Anomalies: If the anonymization event is processed after the student is added, 
    the system must ensure that no updates are lost and that the anonymization 
    fully overrides any pre-existing data (e.g., participants list).
    
    Case E (Complex Functionality):
    Anomalies: Anonymization and handling of participants involve multiple state 
    updates and event processing, which makes it a complex functionality. The test 
    ensures that these operations are handled in the correct sequence.
    */
    def 'sequential anonymize creator and add student: event is processed after add student' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)

        when: 'a student is added to a tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

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
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userCreatorDto.getAggregateId())

        then: 'fails because tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.USER_IS_ANONYMOUS
    }

    /* 
    
    */
    def 'concurrent anonymize creator and add student: anonymize finishes first' () {
        given: 'anonymize creator'
        courseExecutionFunctionalities.anonymizeStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        and: 'tournament processes event to anonymize the creator'
        tournamentEventHandling.handleAnonymizeStudentEvents()

        when: 'another student is concurrently added to a tournament where the creator was anonymized in the course execution' +
                'but not in the tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())

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

    /*
    Case A (Dirty Read):
    Anomalies: If the system allows adding a participant to a removed tournament, 
    it would result in a dirty read scenario, where data no longer exists in the system but is still being referenced.

    Case B (Lost Update):
    Anomalies: If the system doesn't throw an error when trying to add a participant 
    to a removed tournament, it may lead to a lost update where operations are performed on nonexistent data.
    */
    def 'sequential remove tournament and add student' () {
        given: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)

        when: 'a student is added to a tournament that is removed'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
        then: 'the tournament is removed, not found'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    /*
    
    */
    def 'concurrent remove tournament and add student: remove finishes first' () {
        given: 'student added after tournament removal'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = RemoveTournamentFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, 
                                                        tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork1)
        def removeTournamentFunctionality = new RemoveTournamentFunctionalitySagas(eventService, tournamentService,unitOfWorkService, tournamentFactory,
                                                        tournamentDto.getAggregateId(), unitOfWork2)

        when: 'remove tournament concurrently with add'
        removeTournamentFunctionality.executeUntilStep("getTournamentStep", unitOfWork2) 
        addParticipantFunctionality.executeWorkflow(unitOfWork1) 

        then: 'tournament is locked'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'remove finishes and add participant tries again'
        removeTournamentFunctionality.resumeWorkflow(unitOfWork2) 
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName2)
        def addParticipantFunctionality3 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, 
                                                        tournamentDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork3)
        addParticipantFunctionality3.executeWorkflow(unitOfWork3) 

        then: 'fails because tournament is deleted'
        def error2 = thrown(TutorException)
        error2.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    /*

    */
    def 'concurrent remove tournament and add student: add finishes first' () {
        given: 'tournament removal after student added'
        def functionalityName1 = AddParticipantFunctionalitySagas.class.getSimpleName()
        def functionalityName2 = RemoveTournamentFunctionalitySagas.class.getSimpleName()
        def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
        def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, courseExecutionService, unitOfWorkService, 
                                                        tournamentDto.getAggregateId(), userDto.getAggregateId(), unitOfWork1)
        def removeTournamentFunctionality = new RemoveTournamentFunctionalitySagas(eventService, tournamentService,unitOfWorkService, tournamentFactory,
                                                        tournamentDto.getAggregateId(), unitOfWork2)

        when: 'remove tournament concurrently with add'
        addParticipantFunctionality.executeUntilStep("getTournamentStep", unitOfWork1) 
        removeTournamentFunctionality.executeWorkflow(unitOfWork2) 

        then: 'tournament is locked'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'add finishes and remove tries again'
        addParticipantFunctionality.resumeWorkflow(unitOfWork1) 
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName2)
        def removeTournamentFunctionality2 = new RemoveTournamentFunctionalitySagas(eventService, tournamentService,unitOfWorkService, tournamentFactory,
                                                        tournamentDto.getAggregateId(), unitOfWork3)
        removeTournamentFunctionality2.executeWorkflow(unitOfWork3)
        
        then: 'fails because tournament has participant'
        def error2 = thrown(TutorException)
        error2.errorMessage == ErrorMessage.CANNOT_DELETE_TOURNAMENT // TODO test not catching
    }

    /*
    Case D (Check-Then-Act):
    Anomalies: The system updates the tournament’s start time, assuming the tournament 
    still exists. However, by the time the tournament is removed, the state has changed, 
    causing the final act (the find operation) to fail.
    
    Case B (Lost Update):
    Anomalies: If the removal operation did not correctly update the state, the system 
    might still show the updated start time for a removed tournament, resulting in an inconsistency.
    */
    def 'concurrent remove tournament and update start time: update start time finishes first' () {
        given: 'update start time'
        def updateTournamentDto = new TournamentDto()
        updateTournamentDto.setAggregateId(tournamentDto.aggregateId)
        updateTournamentDto.setStartTime(DateHandler.toISOString(TIME_2))
        def topics =  new HashSet<>(Arrays.asList(topicDto1.aggregateId,topicDto2.aggregateId))
        tournamentFunctionalities.updateTournament(updateTournamentDto, topics)

        //TODO use executeUntil
        when: 'remove tournament'
        tournamentFunctionalities.removeTournament(tournamentDto.aggregateId)
        tournamentFunctionalities.findTournament(tournamentDto.aggregateId)

        then: 'fails because tournament is deleted'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_NOT_FOUND
    }

    /*
    Case D (Check-Then-Act):
    Anomalies: The system assumes the first update is the latest, but the second update occurs concurrently and alters the state. The merge must ensure that the final state reflects the most recent update.

    Case B (Lost Update):
    Anomalies: Without proper version handling, the system could incorrectly apply topicDto2 and lose the more recent update to topicDto3.
    */
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
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userDto1.getAggregateId(), unitOfWork1)
        def addParticipantFunctionality2 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork2)

        when: 
        addParticipantFunctionality1.executeUntilStep("getTournamentStep", unitOfWork1)
        addParticipantFunctionality2.executeWorkflow(unitOfWork2)

        then: 'tournament is locked'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA

        when: 'add finishes and add participant tries again'
        addParticipantFunctionality1.resumeWorkflow(unitOfWork1) 
        def unitOfWork3 = unitOfWorkService.createUnitOfWork(functionalityName2)
        def addParticipantFunctionality3 = new AddParticipantFunctionalitySagas(eventService, tournamentEventHandling, tournamentService, 
                courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), userDto2.getAggregateId(), unitOfWork3)
        addParticipantFunctionality3.executeWorkflow(unitOfWork3) 

        then: 'both participants should be successfully added to the tournament'
        def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        updatedTournament.participants.size() == 2
        updatedTournament.participants.any { it.aggregateId == userDto1.getAggregateId() }
        updatedTournament.participants.any { it.aggregateId == userDto2.getAggregateId() }
    }


    def 'sequential solve quiz and anonymize user'() {
        
        given: 'a quiz is solved for a user'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), userDto.getAggregateId())
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