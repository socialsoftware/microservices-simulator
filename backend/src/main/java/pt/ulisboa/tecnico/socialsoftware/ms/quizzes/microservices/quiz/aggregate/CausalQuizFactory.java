package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalQuiz;

@Service
@Profile("tcc")
public class CausalQuizFactory implements QuizFactory {
    @Override
    public Quiz createQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType) {
        return new CausalQuiz(aggregateId, quizCourseExecution, quizQuestions, quizDto, quizType);
    }

    @Override
    public Quiz createQuizFromExisting(Quiz existingQuiz) {
        return new CausalQuiz((CausalQuiz) existingQuiz);
    }
}
