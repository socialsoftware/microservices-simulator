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

@DataJpaTest
class AddParticipantAndUpdateStudentNameTest extends QuizzesSpockTest {
    public static final String UPDATED_NAME = "UpdatedName"

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
    }

    def cleanup() {}

    // add student other than creator

    def 'sequential: update; add' () {
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

    def 'sequential: add; update' () {
        given: 'student is added to tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        when: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        then: 'name in course execution is updated'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'name in tournament is not updated'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == USER_NAME_2

        when: 'update name event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult2 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult2.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }

    def 'concurrent: add(1); update; add(2); event' () {
        given: 'add participant executes the first step'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId(), unitOfWork2)
        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2);
        and: 'student name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userDto.getAggregateId(), updateNameDto)

        when: 'student is added to tournament update'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is not updated in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == USER_NAME_2

        when: 'update name event is processed such that the participant is updated in tournament'
        tournamentEventHandling.handleUpdateStudentNameEvent();

        then: 'the name is updated in course execution'
        def courseExecutionDtoResult2 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult2.getStudents().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
        and: 'the name is updated in tournament'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == UPDATED_NAME
    }

    // update creator name in course execution and add creator as participant in tournament

    def 'sequential - add creator: update; add: fails because when creator is added tournament have not process the event yet' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'add creator as participant where the creator in tournament still has the old name'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
        then: 'fails because invariant breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK

        when: 'event is finally processed it updates the creator name'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        and: 'creator is added as participant because tournament has processed all events it subscribes from course execution'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME
        and: 'the creator is participant with updated name'
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'sequential - add creator: update; event; add' () {
        given: 'a update name dto'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)

        when: 'name is updated'
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)
        then: 'the student name is updated'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME

        when: 'event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        then: 'it updates the creator name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getCreator().getName() == UPDATED_NAME

        when: 'add creator as participant'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
        then: 'the creator is participant with updated name'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().size() == 1
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'sequential - add creator: add; update; event' () {
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

    def 'concurrent - add creator: update; add-s1; event; add-s2' () {
        given: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)
        and: 'creator is read for to be added as participant'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)
        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2)

        when: 'the event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is update in tournament'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.creator.name == UPDATED_NAME

        when: 'creator is added as participant'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)
        then: 'the creator is added as participant with new name'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.getParticipants().size() == 1
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'concurrent - add creator: add-s1; update; event; add-s2' () {
        given: 'creator is read from course to be added'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)
        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2)
        and: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent() 
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult1 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult1.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator is not added as participant with new name'
        def tournamentDtoResult1 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult1.creator.name == UPDATED_NAME
        tournamentDtoResult1.getParticipants().size() == 0
        
        when: 'the read creator is added'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2) 
        then: 'fails because invariant breaks'
        def error = thrown(TutorException)
        error.errorMessage == ErrorMessage.INVARIANT_BREAK

        when: 'the functionality is retried'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())
        then: 'the creator is added as participant with new name'
        def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult.getParticipants().size() == 1
        tournamentDtoResult.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    def 'concurrent - add creator: add-s1; update; add-s2; event' () {
        given: 'creator is read from course to be added'
        def addParticipantFunctionality = new AddParticipantFunctionalitySagas(tournamentService, courseExecutionService, unitOfWorkService, tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), unitOfWork2)
        addParticipantFunctionality.executeUntilStep("getUserStep", unitOfWork2)
        and: 'creator name is updated'
        def updateNameDto = new UserDto()
        updateNameDto.setName(UPDATED_NAME)
        courseExecutionFunctionalities.updateStudentName(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId(), updateNameDto)

        when: 'the read creator is added'
        addParticipantFunctionality.resumeWorkflow(unitOfWork2)
        then: 'the name is updated in course execution'
        def courseExecutionDtoResult1 = courseExecutionFunctionalities.getCourseExecutionByAggregateId(courseExecutionDto.getAggregateId())
        courseExecutionDtoResult1.getStudents().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
        and: 'the creator still has the old name and it is add with old name'
        def tournamentDtoResult1 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult1.creator.name == USER_NAME_1
        tournamentDtoResult1.getParticipants().size() == 1
        tournamentDtoResult1.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == USER_NAME_1

        when: 'event is processed'
        tournamentEventHandling.handleUpdateStudentNameEvent()
        then: 'the names are updated for both, as creator and as participant'
        def tournamentDtoResult2 = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
        tournamentDtoResult2.creator.name == UPDATED_NAME
        tournamentDtoResult2.getParticipants().find{it.aggregateId == userCreatorDto.aggregateId}.name == UPDATED_NAME
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}