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
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.MessagingObjectMapperProvider
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.local.LocalCommandService
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.CommandResponseAggregator
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.stream.StreamCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceService
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommandHandler
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.DistributedVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService

import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.factories.SagasCourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.repositories.CourseCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.service.CourseService

import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.factories.SagasUserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.repositories.UserCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.service.UserService

import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.factories.SagasTopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.sagas.repositories.TopicCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service.TopicService

import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas.factories.SagasCourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas.repositories.CourseExecutionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.service.CourseExecutionService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService()
    }

    @Bean
    IVersionService versionService() {
        return new DistributedVersionService("test")
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
        Mockito.when(mock.send(Mockito.any())).thenAnswer(inv -> commandGateway.send(inv.getArgument(0)))
        return mock
    }

    @Bean
    TraceService TraceService() {
        return new TraceService()
    }

    @Bean
    SagaCommandHandler sagaCommandHandler() {
        return new SagaCommandHandler()
    }

    // Course
    @Bean
    CourseCustomRepositorySagas courseCustomRepositorySagas() {
        return new CourseCustomRepositorySagas()
    }

    @Bean
    SagasCourseFactory sagasCourseFactory() {
        return new SagasCourseFactory()
    }

    @Bean
    CourseService courseService(SagaUnitOfWorkService uowService, CourseCustomRepositorySagas courseCustomRepository) {
        return new CourseService(uowService, courseCustomRepository)
    }

    // User
    @Bean
    UserCustomRepositorySagas userCustomRepositorySagas() {
        return new UserCustomRepositorySagas()
    }

    @Bean
    SagasUserFactory sagasUserFactory() {
        return new SagasUserFactory()
    }

    @Bean
    UserService userService(SagaUnitOfWorkService uowService, UserCustomRepositorySagas userCustomRepository) {
        return new UserService(uowService, userCustomRepository)
    }

    // Topic
    @Bean
    TopicCustomRepositorySagas topicCustomRepositorySagas() {
        return new TopicCustomRepositorySagas()
    }

    @Bean
    SagasTopicFactory sagasTopicFactory() {
        return new SagasTopicFactory()
    }

    @Bean
    TopicService topicService(SagaUnitOfWorkService uowService, TopicCustomRepositorySagas topicCustomRepository) {
        return new TopicService(uowService, topicCustomRepository)
    }

    // CourseExecution
    @Bean
    CourseExecutionCustomRepositorySagas courseExecutionCustomRepositorySagas() {
        return new CourseExecutionCustomRepositorySagas()
    }

    @Bean
    SagasCourseExecutionFactory sagasCourseExecutionFactory() {
        return new SagasCourseExecutionFactory()
    }

    @Bean
    CourseExecutionService courseExecutionService(
            SagaUnitOfWorkService uowService,
            CourseExecutionCustomRepositorySagas courseExecutionCustomRepository) {
        return new CourseExecutionService(uowService, courseExecutionCustomRepository)
    }
}
