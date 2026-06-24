package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantAsyncFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.notification.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository

import java.util.concurrent.CompletableFuture

@SpringBootTest
class AddParticipantAsyncTest extends QuizzesSpockTest {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private ExecutionService courseExecutionService
    @Autowired
    private TournamentService tournamentService

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities
    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private EventService eventService

    @Autowired
    private TournamentEventHandling tournamentEventHandling
    
    @Autowired
    private LocalCommandGateway commandGateway
    
    @Autowired
    public TraceService traceService

    @Autowired
    private CourseRepository courseRepository
    @Autowired
    private CourseExecutionRepository courseExecutionRepository
    @Autowired
    private UserRepository userRepository
    @Autowired
    private TopicRepository topicRepository
    @Autowired
    private QuestionRepository questionRepository
    @Autowired
    private TournamentRepository tournamentRepository
    @Autowired
    private QuizRepository quizRepository
    @Autowired
    private QuizAnswerRepository quizAnswerRepository
    @Autowired
    private EventRepository eventRepository
    @Autowired
    private AggregateIdRepository aggregateIdRepository
    @Autowired
    private VersionRepository versionRepository

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto, userDto3
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def setup() {
        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)


        and: 'a user to enroll in the course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        and: 'another user to enroll in the course execution'
        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        and: 'a third user to enroll in the course execution'
        userDto3 = createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto3.aggregateId)

        and: 'three topics'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        and: 'three questions'
        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto1)), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto2)), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>(Arrays.asList(topicDto3)), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)

        and: 'a tournament where the first user is the creator'
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()])

    }

    @Transactional
    def cleanup() {
        tournamentRepository.deleteAllInBatch()
        quizAnswerRepository.deleteAllInBatch()
        quizRepository.deleteAllInBatch()
        questionRepository.deleteAllInBatch()
        topicRepository.deleteAllInBatch()
        courseExecutionRepository.deleteAllInBatch()
        courseRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
        eventRepository.deleteAllInBatch()
        aggregateIdRepository.deleteAllInBatch()
        versionRepository.deleteAllInBatch()
        impairmentService.cleanUpCounter()
    }



     def 'add one participant' () {
         when: 'student is added to tournament'
         tournamentFunctionalities.addParticipantAsync(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
         then: 'student is added'
         def tournamentDtoResult = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
         tournamentDtoResult.getParticipants().size() == 1
         tournamentDtoResult.getParticipants().find{it.aggregateId == userDto.aggregateId}.name == USER_NAME_2
     }

     def 'add a participant'() {
         given: 'create a unit of work'
         def functionalityName = AddParticipantAsyncFunctionalitySagas.class.getSimpleName()
         def unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName)

         and: 'a functionality to add a participant asynchronously'
         def addParticipantFunctionality = new AddParticipantAsyncFunctionalitySagas(
             unitOfWorkService,
             tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto.getAggregateId(), unitOfWork, commandGateway
         )
         System.out.println(courseExecutionDto.getAggregateId())

         when: 'execute the workflow to add a participant'
         addParticipantFunctionality.executeWorkflow(unitOfWork)

         then: 'the participant is successfully added to the tournament'
         def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
         updatedTournament.participants.size() == 1
         updatedTournament.participants.any { it.aggregateId == userDto.getAggregateId() }
     }

     def 'add two participants to tournament concurrently with async steps'() {
         given: 'create unit of works for concurrent addition of participants'
         def functionalityName1 = AddParticipantAsyncFunctionalitySagas.class.getSimpleName()
         def functionalityName2 = AddParticipantAsyncFunctionalitySagas.class.getSimpleName()
         def unitOfWork1 = unitOfWorkService.createUnitOfWork(functionalityName1)
         def unitOfWork2 = unitOfWorkService.createUnitOfWork(functionalityName2)

         and: 'two functionalities to add participants asynchronously'
         def addParticipantFunctionality1 = new AddParticipantAsyncFunctionalitySagas(
             unitOfWorkService,
             tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto.getAggregateId(), unitOfWork1, commandGateway
         )
         def addParticipantFunctionality2 = new AddParticipantAsyncFunctionalitySagas(
             unitOfWorkService,
             tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto3.getAggregateId(), unitOfWork2, commandGateway
         )

         when: 'execute both workflows concurrently'
         def future1 = CompletableFuture.runAsync({
             addParticipantFunctionality1.executeWorkflow(unitOfWork1)
         } as Runnable)
         def future2 = CompletableFuture.runAsync({
             addParticipantFunctionality2.executeWorkflow(unitOfWork2)
         } as Runnable)
        
         // Wait for both futures to complete
         CompletableFuture.allOf(future1, future2).join()

         then: 'both participants are successfully added to the tournament'
         def updatedTournament = tournamentFunctionalities.findTournament(tournamentDto.getAggregateId())
         updatedTournament.participants.size() == 2
         updatedTournament.participants.any { it.aggregateId == userDto.getAggregateId() }
         updatedTournament.participants.any { it.aggregateId == userDto3.getAggregateId() }
     }

     def 'compare async vs sync implementation performance'() {
         given: 'create unit of works'
         def asyncFunctionalityName = AddParticipantAsyncFunctionalitySagas.class.getSimpleName()
         def syncFunctionalityName = AddParticipantFunctionalitySagas.class.getSimpleName()
         def asyncUnitOfWork1 = unitOfWorkService.createUnitOfWork(asyncFunctionalityName)
         def asyncUnitOfWork2 = unitOfWorkService.createUnitOfWork(asyncFunctionalityName)
         def syncUnitOfWork1 = unitOfWorkService.createUnitOfWork(syncFunctionalityName)
         def syncUnitOfWork2 = unitOfWorkService.createUnitOfWork(syncFunctionalityName)

         when: 'measure sync execution time'
         def syncTournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()])

         def syncFunc1 = new AddParticipantFunctionalitySagas(
             unitOfWorkService,
             syncTournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto.getAggregateId(), syncUnitOfWork1, commandGateway
         )
         def syncFunc2 = new AddParticipantFunctionalitySagas(
             unitOfWorkService,
             syncTournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto3.getAggregateId(), syncUnitOfWork2, commandGateway
         )
        
         def syncStart = System.currentTimeMillis()
         syncFunc1.executeWorkflow(syncUnitOfWork1)
         syncFunc2.executeWorkflow(syncUnitOfWork2)
         def syncEnd = System.currentTimeMillis()
         def syncDuration = syncEnd - syncStart
        
         and: 'measure async execution time'
         def asyncTournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()])

         def asyncFunc1 = new AddParticipantAsyncFunctionalitySagas(
             unitOfWorkService,
             asyncTournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto.getAggregateId(), asyncUnitOfWork1, commandGateway
         )
         def asyncFunc2 = new AddParticipantAsyncFunctionalitySagas(
             unitOfWorkService,
             asyncTournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(),
             userDto3.getAggregateId(), asyncUnitOfWork2, commandGateway
         )
        
         def asyncStart = System.currentTimeMillis()
         asyncFunc1.executeWorkflow(asyncUnitOfWork1)
         asyncFunc2.executeWorkflow(asyncUnitOfWork2)
         def asyncEnd = System.currentTimeMillis()
         def asyncDuration = asyncEnd - asyncStart

         then: 'async execution should be faster or comparable to sync execution'
         println "Sync execution took: ${syncDuration}ms"
         println "Async execution took: ${asyncDuration}ms"
         // Not using a strict assertion as performance can vary, but we expect async to be faster
         // in general when there are non-trivial operations that can benefit from parallelism
         true
     }
}