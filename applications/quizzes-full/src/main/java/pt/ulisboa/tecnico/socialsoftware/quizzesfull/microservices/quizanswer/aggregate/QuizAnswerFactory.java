package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate;

public interface QuizAnswerFactory {
    QuizAnswer createQuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion,
                                Integer userAggregateId, Long userVersion, String userName, String userUsername,
                                Integer executionAggregateId, Long executionVersion);
    QuizAnswer createQuizAnswerCopy(QuizAnswer existing);
    QuizAnswerDto createQuizAnswerDto(QuizAnswer quizAnswer);
}
