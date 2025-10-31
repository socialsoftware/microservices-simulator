package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId, QuizDto quizDto);
    Quiz createQuizFromExisting(Quiz existingQuiz);
    QuizDto createQuizDto(Quiz quiz);
}
