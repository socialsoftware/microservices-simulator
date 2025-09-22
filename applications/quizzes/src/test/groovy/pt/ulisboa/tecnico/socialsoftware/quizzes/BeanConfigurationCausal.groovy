package pt.ulisboa.tecnico.socialsoftware.quizzes

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.LocalCommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.BehaviourService
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.factories.*
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories.CourseCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories.CourseExecutionCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories.QuizAnswerCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories.TournamentCustomRepositoryTCC
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

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationCausal {
    @Bean
    AggregateIdGeneratorService aggregateIdGeneratorService() {
        return new AggregateIdGeneratorService();
    }

    @Bean
    VersionService versionService() {
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
    CourseExecutionEventProcessing courseExecutionEventProcessing(CausalUnitOfWorkService unitOfWorkService) {
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
    QuestionEventProcessing questionEventProcessing(CausalUnitOfWorkService unitOfWorkService) {
        return new QuestionEventProcessing(unitOfWorkService);
    }

    @Bean
    QuizFunctionalities quizFunctionalities() {
        return new QuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing(CausalUnitOfWorkService unitOfWorkService) {
        return new QuizEventProcessing(unitOfWorkService);
    }

    @Bean
    QuizAnswerFunctionalities answerFunctionalities() {
        return new QuizAnswerFunctionalities()
    }

    @Bean
    QuizAnswerEventProcessing answerEventProcessing(CausalUnitOfWorkService unitOfWorkService) {
        return new QuizAnswerEventProcessing(unitOfWorkService)
    }

    @Bean
    TournamentFunctionalities tournamentFunctionalities() {
        return new TournamentFunctionalities()
    }

    @Bean
    TournamentEventProcessing tournamentEventProcessing(CausalUnitOfWorkService unitOfWorkService) {
        return new TournamentEventProcessing(unitOfWorkService)
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
    LocalCommandGateway commandGateway(ApplicationContext applicationContext) {
        return new LocalCommandGateway(applicationContext);
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