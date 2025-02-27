package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate;

import java.util.List;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId, QuestionCourse questionCourse, QuestionDto questionDto, List<QuestionTopic> questionTopics);
    Question createQuestionFromExisting(Question existingQuestion);
    QuestionDto createQuestionDto(Question question);
}
