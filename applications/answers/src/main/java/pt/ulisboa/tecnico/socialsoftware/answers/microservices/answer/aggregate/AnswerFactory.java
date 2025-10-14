package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

public interface AnswerFactory {
    Answer createAnswer(Integer aggregateId,  Dto);
    Answer createAnswerFromExisting(Answer existingAnswer);
     createAnswerDto(Answer );
}
