package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

public interface QuizFactory {
    Quiz createQuiz(Integer aggregateId,  Dto);
    Quiz createQuizFromExisting(Quiz existingQuiz);
     createQuizDto(Quiz );
}
