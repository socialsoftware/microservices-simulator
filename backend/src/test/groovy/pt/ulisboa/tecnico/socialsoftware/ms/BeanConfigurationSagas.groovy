package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaQuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService

import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.causal.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.CourseCustomRepositorySagas   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.QuizAnswerCustomRepositorySagas   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.TournamentCustomRepositorySagas
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.CourseExecutionCustomRepositorySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.TopicRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.CourseRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionRepository;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.SagasQuizAnswerFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.aggregate.SagasCourseFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.SagasCourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.SagasQuestionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.SagasQuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.aggregate.SagasTopicFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.SagasTournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.SagasUserFactory;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaCourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaQuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaQuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaTopicFunctionalities

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaTournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities.SagaUserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfigurationSagas {
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
    SagaUnitOfWorkService unitOfWorkService() {
        return new SagaUnitOfWorkService();
    }

    @Bean
    SagaCourseExecutionFunctionalities courseExecutionFunctionalities() {
        return new SagaCourseExecutionFunctionalities()
    }

    @Bean
    CourseExecutionEventProcessing courseExecutionEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new CourseExecutionEventProcessing(unitOfWorkService)
    }

    @Bean
    SagaUserFunctionalities userFunctionalities() {
        return new SagaUserFunctionalities()
    }

    @Bean
    SagaTopicFunctionalities topicFunctionalities() {
        return new SagaTopicFunctionalities()
    }

    @Bean
    SagaQuestionFunctionalities questionFunctionalities() {
        return new SagaQuestionFunctionalities()
    }

    @Bean
    QuestionEventProcessing questionEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuestionEventProcessing(unitOfWorkService);
    }

    @Bean
    SagaQuizFunctionalities quizFunctionalities() {
        return new SagaQuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuizEventProcessing(unitOfWorkService);
    }

    @Bean
    SagaQuizAnswerFunctionalities answerFunctionalities() {
        return new SagaQuizAnswerFunctionalities()
    }

    @Bean
    QuizAnswerEventProcessing answerEventProcessing(SagaUnitOfWorkService unitOfWorkService) {
        return new QuizAnswerEventProcessing(unitOfWorkService)
    }

    @Bean
    SagaTournamentFunctionalities tournamentFunctionalities() {
        return new SagaTournamentFunctionalities()
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
}