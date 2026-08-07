package pt.ulisboa.tecnico.socialsoftware.quizzesfull2

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.resilience4j.retry.RetryRegistry
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.CommandResponseAggregator
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.StreamCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.CentralizedVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionServiceClient

// Domain imports (factories, custom repositories, services, functionalities, command handlers,
// event processing/handling) are added here as aggregates are implemented in Phase 2.
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.factories.SagasCourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.repositories.CourseCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.messaging.CourseCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.factories.SagasUserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.repositories.UserCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.messaging.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.factories.SagasTopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.repositories.TopicCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.messaging.TopicCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.factories.SagasExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.repositories.ExecutionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.messaging.ExecutionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling.ExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.notification.handling.handlers.ExecutionEventHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.factories.SagasQuestionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.sagas.repositories.QuestionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.messaging.QuestionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.notification.handling.handlers.QuestionEventHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizCustomRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.factories.SagasQuizFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.repositories.QuizCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.messaging.QuizCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService()
    }

    @Bean
    IVersionService versionService(LocalCommandGateway commandGateway) {
        return new VersionServiceClient(commandGateway)
    }

    @Bean
    CentralizedVersionService centralizedVersionService() {
        return new CentralizedVersionService()
    }

    @Bean
    EventApplicationService eventApplicationService() {
        return new EventApplicationService()
    }

    @Bean
    EventService eventService() {
        return new EventService()
    }

    @Bean
    SagaUnitOfWorkService unitOfWorkService() {
        return new SagaUnitOfWorkService()
    }

    @Bean
    ImpairmentService ImpairmentService() {
        return new ImpairmentService()
    }

    @Bean
    RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults()
    }

    @Bean
    MessagingObjectMapperProvider messagingObjectMapperProvider() {
        return new MessagingObjectMapperProvider(new ObjectMapper().findAndRegisterModules())
    }

    @Bean
    LocalCommandService localCommandService(ApplicationContext applicationContext, MessagingObjectMapperProvider mapperProvider) {
        return new LocalCommandService(applicationContext, mapperProvider)
    }

    @Bean
    LocalCommandGateway commandGateway(ApplicationContext applicationContext, RetryRegistry registry, LocalCommandService localCommandService, MessagingObjectMapperProvider mapperProvider) {
        return new LocalCommandGateway(applicationContext, registry, localCommandService, mapperProvider)
    }

    @Bean
    StreamBridge streamBridge() {
        def mock = Mockito.mock(StreamBridge.class)
        Mockito.when(mock.send(Mockito.anyString(), Mockito.any())).thenReturn(true)
        return mock
    }

    @Bean
    CommandResponseAggregator commandResponseAggregator() {
        return new CommandResponseAggregator()
    }

    @Bean
    StreamCommandGateway streamCommandGateway(LocalCommandGateway commandGateway) {
        def mock = Mockito.mock(StreamCommandGateway.class)

        // Delegate basic send(command) to the in-memory CommandGateway
        Mockito.when(mock.send(Mockito.any())).thenAnswer(inv -> commandGateway.send(inv.getArgument(0)))

        return mock
    }

    @Bean
    TraceService TraceService() {
        return new TraceService()
    }

    // Command Handlers
    @Bean
    SagaCommandHandler sagaCommandHandler() {
        return new SagaCommandHandler()
    }

    @Bean
    VersionCommandHandler versionCommandHandler() {
        return new VersionCommandHandler()
    }

    // Domain beans are added below as aggregates are implemented in Phase 2.
    @Bean
    SagasCourseFactory sagasCourseFactory() {
        return new SagasCourseFactory()
    }

    @Bean
    CourseCustomRepositorySagas courseCustomRepositorySagas() {
        return new CourseCustomRepositorySagas()
    }

    @Bean
    CourseService courseService(CourseCustomRepository courseCustomRepository,
                                CourseFactory courseFactory,
                                UnitOfWorkService unitOfWorkService,
                                AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new CourseService(courseCustomRepository, courseFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    CourseCommandHandler courseCommandHandler() {
        return new CourseCommandHandler()
    }

    @Bean
    CourseFunctionalities courseFunctionalities() {
        return new CourseFunctionalities()
    }

    @Bean
    SagasUserFactory sagasUserFactory() {
        return new SagasUserFactory()
    }

    @Bean
    UserCustomRepositorySagas userCustomRepositorySagas() {
        return new UserCustomRepositorySagas()
    }

    @Bean
    UserService userService(UserCustomRepository userCustomRepository,
                            UserFactory userFactory,
                            UnitOfWorkService unitOfWorkService,
                            AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new UserService(userCustomRepository, userFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    UserCommandHandler userCommandHandler() {
        return new UserCommandHandler()
    }

    @Bean
    UserFunctionalities userFunctionalities() {
        return new UserFunctionalities()
    }

    @Bean
    SagasTopicFactory sagasTopicFactory() {
        return new SagasTopicFactory()
    }

    @Bean
    TopicCustomRepositorySagas topicCustomRepositorySagas() {
        return new TopicCustomRepositorySagas()
    }

    @Bean
    TopicService topicService(TopicCustomRepository topicCustomRepository,
                              TopicFactory topicFactory,
                              UnitOfWorkService unitOfWorkService,
                              AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new TopicService(topicCustomRepository, topicFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    TopicCommandHandler topicCommandHandler() {
        return new TopicCommandHandler()
    }

    @Bean
    TopicFunctionalities topicFunctionalities() {
        return new TopicFunctionalities()
    }

    @Bean
    SagasExecutionFactory sagasExecutionFactory() {
        return new SagasExecutionFactory()
    }

    @Bean
    ExecutionCustomRepositorySagas executionCustomRepositorySagas() {
        return new ExecutionCustomRepositorySagas()
    }

    @Bean
    ExecutionService executionService(ExecutionCustomRepository executionCustomRepository,
                                      ExecutionFactory executionFactory,
                                      UnitOfWorkService unitOfWorkService,
                                      AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new ExecutionService(executionCustomRepository, executionFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    ExecutionCommandHandler executionCommandHandler() {
        return new ExecutionCommandHandler()
    }

    @Bean
    ExecutionFunctionalities executionFunctionalities() {
        return new ExecutionFunctionalities()
    }

    @Bean
    ExecutionEventHandling executionEventHandling() {
        return new ExecutionEventHandling()
    }

    @Bean
    ExecutionEventHandler executionEventHandler(ExecutionRepository executionRepository,
                                                ExecutionEventProcessing executionEventProcessing) {
        return new ExecutionEventHandler(executionRepository, executionEventProcessing)
    }

    @Bean
    ExecutionEventProcessing executionEventProcessing() {
        return new ExecutionEventProcessing()
    }

    @Bean
    SagasQuestionFactory sagasQuestionFactory() {
        return new SagasQuestionFactory()
    }

    @Bean
    QuestionCustomRepositorySagas questionCustomRepositorySagas() {
        return new QuestionCustomRepositorySagas()
    }

    @Bean
    QuestionService questionService(QuestionCustomRepository questionCustomRepository,
                                    QuestionFactory questionFactory,
                                    UnitOfWorkService unitOfWorkService,
                                    AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new QuestionService(questionCustomRepository, questionFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    QuestionCommandHandler questionCommandHandler() {
        return new QuestionCommandHandler()
    }

    @Bean
    QuestionFunctionalities questionFunctionalities() {
        return new QuestionFunctionalities()
    }

    @Bean
    QuestionEventHandling questionEventHandling() {
        return new QuestionEventHandling()
    }

    @Bean
    QuestionEventHandler questionEventHandler(QuestionRepository questionRepository,
                                              QuestionEventProcessing questionEventProcessing) {
        return new QuestionEventHandler(questionRepository, questionEventProcessing)
    }

    @Bean
    QuestionEventProcessing questionEventProcessing() {
        return new QuestionEventProcessing()
    }

    @Bean
    SagasQuizFactory sagasQuizFactory() {
        return new SagasQuizFactory()
    }

    @Bean
    QuizCustomRepositorySagas quizCustomRepositorySagas() {
        return new QuizCustomRepositorySagas()
    }

    @Bean
    QuizService quizService(QuizCustomRepository quizCustomRepository,
                            QuizFactory quizFactory,
                            UnitOfWorkService unitOfWorkService,
                            AggregateIdGeneratorService aggregateIdGeneratorService) {
        return new QuizService(quizCustomRepository, quizFactory, unitOfWorkService,
                aggregateIdGeneratorService)
    }

    @Bean
    QuizCommandHandler quizCommandHandler() {
        return new QuizCommandHandler()
    }

    @Bean
    QuizFunctionalities quizFunctionalities() {
        return new QuizFunctionalities()
    }
}
