package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId, QuestionDto questionDto);
    Question createQuestionFromExisting(Question existingQuestion);
    QuestionDto createQuestionDto(Question question);
}
