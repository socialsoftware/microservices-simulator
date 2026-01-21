package pt.ulisboa.tecnico.socialsoftware.quizzes

import io.github.resilience4j.retry.RetryRegistry
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.local.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.causal.factories.CausalQuizAnswerFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.causal.repositories.QuizAnswerCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler.AnswerCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.causal.factories.CausalCourseFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.causal.repositories.CourseCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler.CourseCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.factories.CausalCourseExecutionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.causal.repositories.CourseExecutionCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler.CourseExecutionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.eventProcessing.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.causal.factories.CausalQuestionFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.commandHandler.QuestionCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.causal.factories.CausalQuizFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler.QuizCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.causal.factories.CausalTopicFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler.TopicCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.causal.factories.CausalTournamentFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.causal.repositories.TournamentCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler.TournamentCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.factories.CausalUserFactory
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.commandHandler.UserCommandHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationCausal {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService();
    }

    @Bean
    IVersionService versionService() {
        return new VersionService();
    }

    @Bean
    EventApplicationService eventApplicationService() {
        return new EventApplicationService();
    }

    @Bean
    EventService eventService() {
        return new EventService();
    }

    @Bean
    CausalUnitOfWorkService unitOfWorkService() {
        return new CausalUnitOfWorkService();
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
        return new QuestionEventProcessing();
    }

    @Bean
    QuizFunctionalities quizFunctionalities() {
        return new QuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing() {
        return new QuizEventProcessing();
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
    CourseCustomRepositoryTCC courseCustomRepositoryTCC(){
        return new CourseCustomRepositoryTCC()
    }

    @Bean
    CourseExecutionCustomRepositoryTCC courseExecutionCustomRepositoryTCC(){
        return new CourseExecutionCustomRepositoryTCC()
    }

    @Bean
    TournamentCustomRepositoryTCC tournamentCustomRepositoryTCC(){
        return new TournamentCustomRepositoryTCC()
    }

    @Bean
    QuizAnswerCustomRepositoryTCC quizAnswerCustomRepositoryTCC(){
        return new QuizAnswerCustomRepositoryTCC()
    }

    @Bean
    CausalQuizAnswerFactory causalQuizAnswerFactory(){
        return new CausalQuizAnswerFactory()
    }

    @Bean
    CausalCourseFactory causalCourseFactory(){
        return new CausalCourseFactory()
    }

    @Bean
    CausalCourseExecutionFactory causalCourseExecutionFactory(){
        return new CausalCourseExecutionFactory()
    }

    @Bean
    CausalQuestionFactory causalQuestionFactory(){
        return new CausalQuestionFactory()
    }

    @Bean
    CausalQuizFactory causalQuizFactory(){
        return new CausalQuizFactory()
    }

    @Bean
    CausalTopicFactory causalTopicFactory(){
        return new CausalTopicFactory()
    }

    @Bean
    CausalTournamentFactory causalTournamentFactory(){
        return new CausalTournamentFactory()
    }

    @Bean
    CausalUserFactory causalUserFactory(){
        return new CausalUserFactory()
    }

    @Bean
    CourseService courseService(CausalUnitOfWorkService unitOfWorkService, CourseCustomRepositoryTCC courseRepository) {
        return new CourseService(unitOfWorkService, courseRepository)
    }

    @Bean
    QuizAnswerService answerService(CausalUnitOfWorkService unitOfWorkService, QuizAnswerCustomRepositoryTCC quizAnswerRepository) {
        return new QuizAnswerService(unitOfWorkService, quizAnswerRepository)
    }

    @Bean
    TournamentService tournamentService(CausalUnitOfWorkService unitOfWorkService, TournamentCustomRepositoryTCC tournamentRepository) {
        return new TournamentService(unitOfWorkService, tournamentRepository)
    }

    @Bean
    CourseExecutionService courseExecutionService(CausalUnitOfWorkService unitOfWorkService, CourseExecutionRepository courseExecutionRepository, CourseExecutionCustomRepositoryTCC courseExecutionCustomRepository) {
        return new CourseExecutionService(unitOfWorkService, courseExecutionRepository, courseExecutionCustomRepository)
    }

    @Bean
    UserService userService(CausalUnitOfWorkService unitOfWorkService, UserRepository userRepository) {
        return new UserService(unitOfWorkService, userRepository)
    }

    @Bean
    TopicService topicService(CausalUnitOfWorkService unitOfWorkService, TopicRepository topicRepository) {
        return new TopicService(unitOfWorkService, topicRepository)
    }

    @Bean
    QuestionService questionService(CausalUnitOfWorkService unitOfWorkService, QuestionRepository questionRepository) {
        return new QuestionService(unitOfWorkService, questionRepository)
    }

    @Bean
    QuizService quizService(CausalUnitOfWorkService unitOfWorkService, QuizRepository quizRepository) {
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
        return new BehaviourService();
    }

    @Bean
    RetryRegistry retryRegistry() {
        return RetryRegistry.ofDefaults()
    }

    @Bean
    LocalCommandGateway commandGateway(ApplicationContext applicationContext, RetryRegistry registry) {
        return new LocalCommandGateway(applicationContext, registry)
    }

    // Command Handlers
    @Bean
    UserCommandHandler userCommandHandler() {
        return new UserCommandHandler();
    }

    @Bean
    TournamentCommandHandler tournamentCommandHandler() {
        return new TournamentCommandHandler();
    }

    @Bean
    QuestionCommandHandler questionCommandHandler() {
        return new QuestionCommandHandler();
    }

    @Bean
    TopicCommandHandler topicCommandHandler() {
        return new TopicCommandHandler();
    }

    @Bean
    CourseExecutionCommandHandler courseExecutionCommandHandler() {
        return new CourseExecutionCommandHandler();
    }

    @Bean
    CourseCommandHandler courseCommandHandler() {
        return new CourseCommandHandler();
    }

    @Bean
    AnswerCommandHandler answerCommandHandler() {
        return new AnswerCommandHandler();
    }

    @Bean
    QuizCommandHandler quizCommandHandler() {
        return new QuizCommandHandler();
    }
}