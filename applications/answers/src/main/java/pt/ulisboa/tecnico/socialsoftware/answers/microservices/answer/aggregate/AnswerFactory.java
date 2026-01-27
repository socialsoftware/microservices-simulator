package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.util.List;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

public interface AnswerFactory {
    Answer createAnswer(Integer aggregateId, AnswerExecution execution, AnswerUser user, AnswerQuiz quiz, AnswerDto answerDto, List<QuestionAnswered> questions);
    Answer createAnswerFromExisting(Answer existingAnswer);
    AnswerDto createAnswerDto(Answer answer);
}
