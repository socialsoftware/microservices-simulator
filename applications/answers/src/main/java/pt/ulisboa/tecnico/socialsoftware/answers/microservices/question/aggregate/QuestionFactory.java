package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId,  Dto);
    Question createQuestionFromExisting(Question existingQuestion);
     createQuestionDto(Question );
}
