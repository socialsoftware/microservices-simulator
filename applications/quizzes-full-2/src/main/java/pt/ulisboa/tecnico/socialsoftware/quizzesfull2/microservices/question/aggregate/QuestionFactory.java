package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import java.time.LocalDateTime;

public interface QuestionFactory {
    Question createQuestion(Integer aggregateId, Integer courseAggregateId, String title, String content,
                            LocalDateTime creationDate);

    Question createQuestionCopy(Question existing);

    QuestionDto createQuestionDto(Question question);
}
