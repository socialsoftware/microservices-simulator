package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.question.aggregate;

import java.util.Set;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId, String title, String content, QuestionCourse questionCourse, Set<Option> options, Set<QuestionTopic> topics);
    Question createQuestionCopy(Question existing);
    QuestionDto createQuestionDto(Question question);
}
