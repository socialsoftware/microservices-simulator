package pt.ulisboa.tecnico.socialsoftware.quizzes

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.TraceService
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.eventProcessing.*
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities.*
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler.AnswerCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler.CourseCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler.CourseExecutionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler.QuestionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler.QuizCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler.TopicCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler.TournamentCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.factories.*
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories.CourseCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories.CourseExecutionCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories.QuizAnswerCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.repositories.TournamentCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService()
    }

    @Bean
    VersionService versionService() {
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
    CourseExecutionEventProcessing courseExecutionEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new CourseExecutionEventProcessing(unitOfWorkService)
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
    QuestionEventProcessing questionEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuestionEventProcessing(unitOfWorkService)
    }

    @Bean
    QuizFunctionalities quizFunctionalities() {
        return new QuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuizEventProcessing(unitOfWorkService)
    }

    @Bean
    QuizAnswerFunctionalities answerFunctionalities() {
        return new QuizAnswerFunctionalities()
    }

    @Bean
    QuizAnswerEventProcessing answerEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuizAnswerEventProcessing(unitOfWorkService)
    }

    @Bean
    TournamentFunctionalities tournamentFunctionalities() {
        return new TournamentFunctionalities()
    }

    @Bean
    TournamentEventProcessing tournamentEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new TournamentEventProcessing(unitOfWorkService)
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
    CommandGateway commandGateway(ApplicationContext applicationContext) {
        return new CommandGateway(applicationContext)
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