package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuizAnswer;

@Service
@Profile("tcc")
public class CausalQuizAnswerFactory implements QuizAnswerFactory {
    @Override
    public QuizAnswer createQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz) {
        return new CausalQuizAnswer(aggregateId, answerCourseExecution, answerStudent, answeredQuiz);
    }
    
    @Override
    public QuizAnswer createQuizAnswerFromExisting(QuizAnswer existingAnswer) {
        return new CausalQuizAnswer((CausalQuizAnswer) existingAnswer);
    }
}