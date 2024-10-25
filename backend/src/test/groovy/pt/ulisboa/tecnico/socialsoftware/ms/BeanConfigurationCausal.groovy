package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories.CourseCustomRepositoryTCC   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories.QuizAnswerCustomRepositoryTCC   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories.TournamentCustomRepositoryTCC
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.repositories.CourseExecutionCustomRepositoryTCC;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalQuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalCourseFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalCourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalQuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalQuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalTopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalTournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.factories.CausalUserFactory;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TopicFunctionalities

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService

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
}