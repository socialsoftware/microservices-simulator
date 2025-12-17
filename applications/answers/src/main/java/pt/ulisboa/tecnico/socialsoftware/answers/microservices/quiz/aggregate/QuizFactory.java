package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId, QuizExecution execution, QuizDto quizDto, Set<QuizQuestion> questions);
    Quiz createQuizFromExisting(Quiz existingQuiz);
    QuizDto createQuizDto(Quiz quiz);
}
