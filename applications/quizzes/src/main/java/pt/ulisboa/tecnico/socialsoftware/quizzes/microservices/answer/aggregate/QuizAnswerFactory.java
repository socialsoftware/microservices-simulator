package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate;

public interface QuizAnswerFactory {
    QuizAnswer createQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz);
    QuizAnswer createQuizAnswerFromExisting(QuizAnswer existingAnswer);
    QuizAnswerDto createQuizAnswerDto(QuizAnswer quizAnswer);
}
