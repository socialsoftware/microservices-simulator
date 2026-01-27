package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId, QuestionCourse course, QuestionDto questionDto, Set<QuestionTopic> topics, List<Option> options);
    Question createQuestionFromExisting(Question existingQuestion);
    QuestionDto createQuestionDto(Question question);
}
