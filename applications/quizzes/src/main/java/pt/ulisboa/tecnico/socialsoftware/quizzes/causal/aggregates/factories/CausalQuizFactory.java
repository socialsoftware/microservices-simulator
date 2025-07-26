package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.CausalQuiz;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.*;

import java.util.Set;

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

    @Override
    public QuizDto createQuizDto(Quiz quiz) {
        return new QuizDto(quiz);
    }
}
