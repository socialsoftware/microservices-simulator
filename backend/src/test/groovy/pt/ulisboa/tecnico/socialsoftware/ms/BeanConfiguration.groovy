package pt.ulisboa.tecnico.socialsoftware.ms

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.PropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.event.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.event.QuizAnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService

import pt.ulisboa.tecnico.socialsoftware.ms.causal.causalUnityOfWork.CausalUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.causal.version.VersionService

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.CourseExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.event.CourseExecutionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.event.CourseExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.event.QuestionEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.event.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.event.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.event.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.TopicFunctionalities

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.event.TournamentEventHandling
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.event.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.UserFunctionalities
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
    CourseService courseService() {
        return new CourseService()
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