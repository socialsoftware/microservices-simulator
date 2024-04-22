package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate;

public interface QuizAnswerFactory {
    QuizAnswer createQuizAnswer(Integer aggregateId, AnswerCourseExecution answerCourseExecution, AnswerStudent answerStudent, AnsweredQuiz answeredQuiz);
    QuizAnswer createQuizAnswerFromExisting(QuizAnswer existingAnswer);
}
