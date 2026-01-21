package pt.ulisboa.tecnico.socialsoftware.quizzes

import io.github.resilience4j.retry.RetryRegistry
import org.mockito.Mockito
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.cloud.stream.function.StreamBridge
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.CommandResponseAggregator
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.stream.StreamCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.factories.SagasQuizAnswerFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.sagas.repositories.QuizAnswerCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler.AnswerCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.factories.SagasCourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.repositories.CourseCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler.CourseCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.factories.SagasCourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.repositories.CourseExecutionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler.CourseExecutionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.sagas.factories.SagasQuestionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler.QuestionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.sagas.factories.SagasQuizFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler.QuizCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.factories.SagasTopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler.TopicCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.factories.SagasTournamentFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.sagas.repositories.TournamentCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler.TournamentCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.sagas.factories.SagasUserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService()
    }

    @Bean
    IVersionService versionService() {
        return new VersionService()
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
    CourseExecutionFunctionalities courseExecutionFunctionalities() {
        return new CourseExecutionFunctionalities()
    }

    @Bean
    CourseExecutionEventProcessing courseExecutionEventProcessing() {
        return new CourseExecutionEventProcessing()
    }

    @Bean
    UserFunctionalities userFunctionalities() {
        return new UserFunctionalities()
    }

    @Bean
    TopicFunctionalities topicFunctionalities() {
        return new TopicFunctionalities()
    }

    @Bean
    QuestionFunctionalities questionFunctionalities() {
        return new QuestionFunctionalities()
    }

    @Bean
    QuestionEventProcessing questionEventProcessing() {
        return new QuestionEventProcessing()
    }

    @Bean
    QuizFunctionalities quizFunctionalities() {
        return new QuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing() {
        return new QuizEventProcessing()
    }

    @Bean
    QuizAnswerFunctionalities answerFunctionalities() {
        return new QuizAnswerFunctionalities()
    }

    @Bean
    QuizAnswerEventProcessing answerEventProcessing() {
        return new QuizAnswerEventProcessing()
    }

    @Bean
    TournamentFunctionalities tournamentFunctionalities() {
        return new TournamentFunctionalities()
    }

    @Bean
    TournamentEventProcessing tournamentEventProcessing() {
        return new TournamentEventProcessing()
    }

    @Bean
    CourseCustomRepositorySagas courseCustomRepositorySagas(){
        return new CourseCustomRepositorySagas()
    }

    @Bean
    CourseExecutionCustomRepositorySagas courseExecutionCustomRepositorySagas(){
        return new CourseExecutionCustomRepositorySagas()
    }

    @Bean
    TournamentCustomRepositorySagas tournamentCustomRepositorySagas(){
        return new TournamentCustomRepositorySagas()
    }

    @Bean
    QuizAnswerCustomRepositorySagas quizAnswerCustomRepositorySagas(){
        return new QuizAnswerCustomRepositorySagas()
    }

    @Bean
    SagasQuizAnswerFactory sagasQuizAnswerFactory(){
        return new SagasQuizAnswerFactory()
    }

    @Bean
    SagasCourseFactory sagasCourseFactory(){
        return new SagasCourseFactory()
    }

    @Bean
    SagasCourseExecutionFactory sagasCourseExecutionFactory(){
        return new SagasCourseExecutionFactory()
    }

    @Bean
    SagasQuestionFactory sagasQuestionFactory(){
        return new SagasQuestionFactory()
    }

    @Bean
    SagasQuizFactory sagasQuizFactory(){
        return new SagasQuizFactory()
    }

    @Bean
    SagasTopicFactory sagasTopicFactory(){
        return new SagasTopicFactory()
    }

    @Bean
    SagasTournamentFactory sagasTournamentFactory(){
        return new SagasTournamentFactory()
    }

    @Bean
    SagasUserFactory sagasUserFactory(){
        return new SagasUserFactory()
    }

    @Bean
    CourseService courseService(SagaUnitOfWorkService unitOfWorkService, CourseCustomRepositorySagas courseRepository) {
        return new CourseService(unitOfWorkService, courseRepository)
    }

    @Bean
    QuizAnswerService answerService(SagaUnitOfWorkService unitOfWorkService, QuizAnswerCustomRepositorySagas quizAnswerRepository) {
        return new QuizAnswerService(unitOfWorkService, quizAnswerRepository)
    }

    @Bean
    TournamentService tournamentService(SagaUnitOfWorkService unitOfWorkService, TournamentCustomRepositorySagas tournamentRepository) {
        return new TournamentService(unitOfWorkService, tournamentRepository)
    }

    @Bean
    CourseExecutionService courseExecutionService(SagaUnitOfWorkService unitOfWorkService, CourseExecutionRepository courseExecutionRepository, CourseExecutionCustomRepositorySagas courseExecutionCustomRepository) {
        return new CourseExecutionService(unitOfWorkService, courseExecutionRepository, courseExecutionCustomRepository)
    }

    @Bean
    UserService userService(SagaUnitOfWorkService unitOfWorkService, UserRepository userRepository) {
        return new UserService(unitOfWorkService, userRepository)
    }

    @Bean
    TopicService topicService(SagaUnitOfWorkService unitOfWorkService, TopicRepository topicRepository) {
        return new TopicService(unitOfWorkService, topicRepository)
    }

    @Bean
    QuestionService questionService(SagaUnitOfWorkService unitOfWorkService, QuestionRepository questionRepository) {
        return new QuestionService(unitOfWorkService, questionRepository)
    }

    @Bean
    QuizService quizService(SagaUnitOfWorkService unitOfWorkService, QuizRepository quizRepository) {
        return new QuizService(unitOfWorkService, quizRepository)
    }

    @Bean
    CourseExecutionEventHandling courseExecutionEventDetection() {
        return new CourseExecutionEventHandling()
    }

    @Bean
    QuestionEventHandling questionEventDetection() {
        return new QuestionEventHandling()
    }

    @Bean
    QuizEventHandling quizEventDetection() {
        return new QuizEventHandling()
    }

    @Bean
    QuizAnswerEventHandling answerEventDetection() {
        return new QuizAnswerEventHandling()
    }

    @Bean
    TournamentEventHandling tournamentEventDetection() {
        return new TournamentEventHandling()
    }

    @Bean
    BehaviourService BehaviourService() {
        return new BehaviourService()
    }

    @Bean
    RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults()
    }

    @Bean
    LocalCommandGateway commandGateway(ApplicationContext applicationContext, RetryRegistry registry) {
        return new LocalCommandGateway(applicationContext, registry)
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
    UserCommandHandler userCommandHandler() {
        return new UserCommandHandler()
    }

    @Bean
    TournamentCommandHandler tournamentCommandHandler() {
        return new TournamentCommandHandler()
    }

    @Bean
    QuestionCommandHandler questionCommandHandler() {
        return new QuestionCommandHandler()
    }

    @Bean
    TopicCommandHandler topicCommandHandler() {
        return new TopicCommandHandler()
    }

    @Bean
    CourseExecutionCommandHandler courseExecutionCommandHandler() {
        return new CourseExecutionCommandHandler()
    }

    @Bean
    CourseCommandHandler courseCommandHandler() {
        return new CourseCommandHandler()
    }

    @Bean
    AnswerCommandHandler answerCommandHandler() {
        return new AnswerCommandHandler()
    }

    @Bean
    QuizCommandHandler quizCommandHandler() {
        return new QuizCommandHandler()
    }
}