package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import java.time.LocalDateTime;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId, Integer executionAggregateId, Long executionVersion, String title,
                    LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate,
                    LocalDateTime resultsDate, QuizType quizType);

    Quiz createQuizCopy(Quiz existing);

    QuizDto createQuizDto(Quiz quiz);
}
