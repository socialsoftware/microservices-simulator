package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate;

import java.util.Set;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId, QuizCourseExecution quizCourseExecution, Set<QuizQuestion> quizQuestions, QuizDto quizDto, QuizType quizType);
    Quiz createQuizFromExisting(Quiz existingQuiz);
}
