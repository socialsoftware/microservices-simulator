package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalQuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService

import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.causal.version.VersionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CourseCustomRepositoryTCC   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.QuizAnswerCustomRepositoryTCC   
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.TournamentCustomRepositoryTCC   

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalCourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.handling.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalQuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.handling.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalQuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalTopicFunctionalities

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalTournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities.CausalUserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService

@TestConfiguration
@PropertySource("classpath:application-test.properties")
class BeanConfiguration {
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
    CausalCourseExecutionFunctionalities courseExecutionFunctionalities() {
        return new CausalCourseExecutionFunctionalities()
    }

    @Bean
    CourseExecutionEventProcessing courseExecutionEventProcessing() {
        return new CourseExecutionEventProcessing()
    }

    @Bean
    CausalUserFunctionalities userFunctionalities() {
        return new CausalUserFunctionalities()
    }

    @Bean
    CausalTopicFunctionalities topicFunctionalities() {
        return new CausalTopicFunctionalities()
    }

    @Bean
    CausalQuestionFunctionalities questionFunctionalities() {
        return new CausalQuestionFunctionalities()
    }

    @Bean
    QuestionEventProcessing questionEventProcessing() {
        return new QuestionEventProcessing();
    }

    @Bean
    CausalQuizFunctionalities quizFunctionalities() {
        return new CausalQuizFunctionalities()
    }

    @Bean
    QuizEventProcessing quizEventProcessing() {
        return new QuizEventProcessing();
    }

    @Bean
    CausalQuizAnswerFunctionalities answerFunctionalities() {
        return new CausalQuizAnswerFunctionalities()
    }

    @Bean
    QuizAnswerEventProcessing answerEventProcessing() {
        return new QuizAnswerEventProcessing()
    }

    @Bean
    CausalTournamentFunctionalities tournamentFunctionalities() {
        return new CausalTournamentFunctionalities()
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
    QuizAnswerCustomRepositoryTCC quizAnswerCustomRepositoryTCC(){
        return new QuizAnswerCustomRepositoryTCC()
    }

    @Bean
    CourseService courseService(CausalUnitOfWorkService unitOfWorkService, CourseCustomRepositoryTCC courseRepository) {
        return new CourseService(unitOfWorkService, courseRepository)
    }

    @Bean
    QuizAnswerService quizAnswerService(CausalUnitOfWorkService unitOfWorkService, QuizAnswerCustomRepositoryTCC quizAnswerRepository) {
        return new QuizAnswerService(unitOfWorkService, quizAnswerRepository)
    }

    @Bean
    TournamentService TournamentService(CausalUnitOfWorkService unitOfWorkService, TournamentCustomRepositoryTCC tournamentRepository) {
        return new TournamentService(unitOfWorkService, tournamentRepository)
    }

    @Bean
    CourseExecutionService courseExecutionService() {
        return new CourseExecutionService()
    }

    @Bean
    UserService userService() {
        return new UserService()
    }

    @Bean
    TopicService topicService() {
        return new TopicService()
    }

    @Bean
    QuestionService questionService() {
        return new QuestionService()
    }

    @Bean
    QuizService quizService() {
        return new QuizService()
    }

    @Bean
    QuizAnswerService answerService() {
        return new QuizAnswerService()
    }

    @Bean
    TournamentService tournamentService() {
        return new TournamentService()
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