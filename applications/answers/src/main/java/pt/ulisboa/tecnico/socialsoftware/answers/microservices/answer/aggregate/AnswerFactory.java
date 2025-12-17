package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

public interface AnswerFactory {
    Answer createAnswer(Integer aggregateId, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, AnswerDto answerDto);
    Answer createAnswerFromExisting(Answer existingAnswer);
    AnswerDto createAnswerDto(Answer answer);
}
