package pt.ulisboa.tecnico.socialsoftware.quizzesfull

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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.factories.SagasCourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.repositories.CourseCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.messaging.CourseCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.factories.SagasUserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.repositories.UserCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.messaging.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.factories.SagasTopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.repositories.TopicCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.messaging.TopicCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.factories.SagasExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.repositories.ExecutionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.messaging.ExecutionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling.ExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.notification.handling.handlers.ExecutionEventHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.factories.SagasQuestionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.sagas.repositories.QuestionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.messaging.QuestionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.notification.handling.handlers.QuestionEventHandler
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.service.QuestionService

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

    @Bean
    SagasCourseFactory sagasCourseFactory() {
        return new SagasCourseFactory()
    }

    @Bean
    CourseCustomRepositorySagas courseCustomRepositorySagas() {
        return new CourseCustomRepositorySagas()
    }

    // User — session 2.2.a
    @Bean
    SagasUserFactory sagasUserFactory() {
        return new SagasUserFactory()
    }

    @Bean
    UserCustomRepositorySagas userCustomRepositorySagas() {
        return new UserCustomRepositorySagas()
    }

    // Course — session 2.1.b
    @Bean
    CourseService courseService(SagaUnitOfWorkService unitOfWorkService, CourseCustomRepositorySagas courseRepository) {
        return new CourseService(unitOfWorkService, courseRepository)
    }

    @Bean
    CourseCommandHandler courseCommandHandler() {
        return new CourseCommandHandler()
    }

    @Bean
    CourseFunctionalities courseFunctionalities() {
        return new CourseFunctionalities()
    }

    // User — session 2.2.b
    @Bean
    UserService userService(SagaUnitOfWorkService unitOfWorkService) {
        return new UserService(unitOfWorkService)
    }

    @Bean
    UserCommandHandler userCommandHandler() {
        return new UserCommandHandler()
    }

    @Bean
    UserFunctionalities userFunctionalities() {
        return new UserFunctionalities()
    }

    // Topic — session 2.3.a
    @Bean
    SagasTopicFactory sagasTopicFactory() {
        return new SagasTopicFactory()
    }

    @Bean
    TopicCustomRepositorySagas topicCustomRepositorySagas() {
        return new TopicCustomRepositorySagas()
    }

    // Topic — session 2.3.b
    @Bean
    TopicService topicService(SagaUnitOfWorkService unitOfWorkService, TopicCustomRepositorySagas topicRepository) {
        return new TopicService(unitOfWorkService, topicRepository)
    }

    @Bean
    TopicCommandHandler topicCommandHandler() {
        return new TopicCommandHandler()
    }

    @Bean
    TopicFunctionalities topicFunctionalities() {
        return new TopicFunctionalities()
    }

    // Execution — session 2.4.a
    @Bean
    SagasExecutionFactory sagasExecutionFactory() {
        return new SagasExecutionFactory()
    }

    @Bean
    ExecutionCustomRepositorySagas executionCustomRepositorySagas() {
        return new ExecutionCustomRepositorySagas()
    }

    // Execution — session 2.4.b
    @Bean
    ExecutionService executionService(SagaUnitOfWorkService unitOfWorkService, ExecutionCustomRepositorySagas executionRepository) {
        return new ExecutionService(unitOfWorkService, executionRepository)
    }

    @Bean
    ExecutionCommandHandler executionCommandHandler() {
        return new ExecutionCommandHandler()
    }

    @Bean
    ExecutionFunctionalities executionFunctionalities() {
        return new ExecutionFunctionalities()
    }

    // Question — session 2.5.a
    @Bean
    SagasQuestionFactory sagasQuestionFactory() {
        return new SagasQuestionFactory()
    }

    @Bean
    QuestionCustomRepositorySagas questionCustomRepositorySagas() {
        return new QuestionCustomRepositorySagas()
    }

    // Question — session 2.5.b
    @Bean
    QuestionService questionService(SagaUnitOfWorkService unitOfWorkService, QuestionCustomRepositorySagas questionRepository) {
        return new QuestionService(unitOfWorkService, questionRepository)
    }

    @Bean
    QuestionCommandHandler questionCommandHandler() {
        return new QuestionCommandHandler()
    }

    @Bean
    QuestionFunctionalities questionFunctionalities() {
        return new QuestionFunctionalities()
    }

    // Execution — session 2.4.d
    @Bean
    ExecutionEventHandling executionEventHandling() {
        return new ExecutionEventHandling()
    }

    @Bean
    ExecutionEventHandler executionEventHandler(ExecutionRepository executionRepository) {
        return new ExecutionEventHandler(executionRepository)
    }

    @Bean
    ExecutionEventProcessing executionEventProcessing() {
        return new ExecutionEventProcessing()
    }

    // Question — session 2.5.d
    @Bean
    QuestionEventHandling questionEventHandling() {
        return new QuestionEventHandling()
    }

    @Bean
    QuestionEventHandler questionEventHandler(QuestionRepository questionRepository) {
        return new QuestionEventHandler(questionRepository)
    }

    @Bean
    QuestionEventProcessing questionEventProcessing() {
        return new QuestionEventProcessing()
    }
}
