package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto

import java.nio.file.Files
import java.nio.file.Path

@DataJpaTest
@ImportAutoConfiguration(DynamicEvidenceConfiguration)
class CreateTournamentDynamicEvidenceSmokeTest extends QuizzesSpockTest {
    private static final Path dynamicEvidenceDir = Files.createTempDirectory('quizzes-dynamic-evidence-')

    @Autowired
    private SagaUnitOfWorkService unitOfWorkService

    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    @Autowired
    private LocalCommandGateway commandGateway

    @Autowired
    private DynamicEvidenceRecorder dynamicEvidenceRecorder

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto
    private TopicDto topicDto1, topicDto2
    private QuestionDto questionDto1, questionDto2

    @DynamicPropertySource
    static void dynamicEvidenceProperties(DynamicPropertyRegistry registry) {
        registry.add('simulator.dynamic-evidence.enabled') { true }
        registry.add('simulator.dynamic-evidence.output-dir') { dynamicEvidenceDir.toString() }
        registry.add('simulator.dynamic-evidence.application-name') { 'quizzes-smoke-test' }
    }

    def setup() {
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userCreatorDto.aggregateId)
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        questionDto1 = createQuestion(courseExecutionDto, [topicDto1] as Set, TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, [topicDto2] as Set, TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
    }

    def cleanupSpec() {
        deleteRecursively(dynamicEvidenceDir)
    }

    def 'records exact SagaExecution read for CreateTournament getCourseExecutionStep'() {
        given: 'a CreateTournament saga using the production Quizzes local/sagas path'
        def tournamentDto = new TournamentDto(
                startTime: DateHandler.toISOString(TIME_1),
                endTime: DateHandler.toISOString(TIME_3),
                numberOfQuestions: 2)
        def unitOfWork = unitOfWorkService.createUnitOfWork(CreateTournamentFunctionalitySagas.simpleName)
        def createTournamentFunctionality = new CreateTournamentFunctionalitySagas(
                unitOfWorkService,
                userCreatorDto.aggregateId,
                courseExecutionDto.aggregateId,
                [topicDto1.aggregateId, topicDto2.aggregateId],
                tournamentDto,
                unitOfWork,
                commandGateway)

        when: 'the CreateTournament saga executes and the dynamic evidence recorder is flushed'
        createTournamentFunctionality.executeWorkflow(unitOfWork)
        dynamicEvidenceRecorder.close() // flush JSONL + manifest before reading artifacts in-process

        then: 'the JSONL contains semantic evidence for getCourseExecutionStep in CreateTournament'
        def evidencePath = dynamicEvidenceDir.resolve('dynamic-evidence.jsonl')
        Files.exists(evidencePath)
        def events = evidencePath.readLines().collect { new JsonSlurper().parseText(it) }
        def getCourseExecutionStepEvents = events.findAll { event ->
            event.functionalityName == 'CreateTournamentFunctionalitySagas' &&
                    event.stepName == 'getCourseExecutionStep'
        }

        getCourseExecutionStepEvents.any { it.eventKind == 'STEP_STARTED' }
        getCourseExecutionStepEvents.any { event ->
            event.eventKind == 'STEP_FINISHED' &&
                    event.payload?.outcome?.toString() == 'SUCCESS'
        }

        def commandEventsInStep = getCourseExecutionStepEvents.findAll { it.eventKind == 'COMMAND_SENT' }
        !commandEventsInStep.isEmpty()
        commandEventsInStep.any { event ->
            def payload = event.payload ?: [:]
            if (payload.containsKey('serviceName')) {
                return payload.serviceName == 'execution'
            }
            payload.commandType == 'GetCourseExecutionByIdCommand' &&
                    payload.rootAggregateId?.toString() == courseExecutionDto.aggregateId?.toString()
        }

        getCourseExecutionStepEvents.any { event ->
            event.eventKind == 'AGGREGATE_ACCESSED' &&
                    event.payload.accessMode == 'READ' &&
                    event.payload.aggregateType == 'SagaExecution' &&
                    event.payload.aggregateId?.toString() == '2'
        }

        and: 'the manifest counts are non-zero for required kinds and match the JSONL contents'
        def manifestPath = dynamicEvidenceDir.resolve('dynamic-evidence-manifest.json')
        Files.exists(manifestPath)
        def manifest = new JsonSlurper().parseText(manifestPath.toFile().text)
        manifest.enabled == true
        manifest.counts.eventsWritten == events.size()

        def jsonlKindCounts = [
                STEP_STARTED      : events.count { it.eventKind == 'STEP_STARTED' },
                STEP_FINISHED     : events.count { it.eventKind == 'STEP_FINISHED' },
                COMMAND_SENT      : events.count { it.eventKind == 'COMMAND_SENT' },
                AGGREGATE_ACCESSED: events.count { it.eventKind == 'AGGREGATE_ACCESSED' }
        ]
        jsonlKindCounts.each { eventKind, jsonlCount ->
            jsonlCount > 0
            manifest.counts[eventKind] == jsonlCount
        }
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return
        }
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
        }
    }
}
