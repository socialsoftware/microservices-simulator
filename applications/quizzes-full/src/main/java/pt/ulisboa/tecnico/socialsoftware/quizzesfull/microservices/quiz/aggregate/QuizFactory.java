package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate;

import java.time.LocalDateTime;
import java.util.Set;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId, String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                    LocalDateTime resultsDate, QuizType quizType, QuizExecution quizExecution, Set<QuizQuestion> questions);
    Quiz createQuizCopy(Quiz existing);
    QuizDto createQuizDto(Quiz quiz);
}
