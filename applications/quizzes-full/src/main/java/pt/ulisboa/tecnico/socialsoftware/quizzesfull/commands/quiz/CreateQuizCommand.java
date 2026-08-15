package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizType;

import java.time.LocalDateTime;
import java.util.Set;

public class CreateQuizCommand extends Command {
    private final String title;
    private final LocalDateTime availableDate;
    private final LocalDateTime conclusionDate;
    private final LocalDateTime resultsDate;
    private final QuizType quizType;
    private final QuizExecution quizExecution;
    private final Set<QuizQuestion> questions;

    public CreateQuizCommand(UnitOfWork unitOfWork, String serviceName,
                              String title, LocalDateTime availableDate, LocalDateTime conclusionDate,
                              LocalDateTime resultsDate, QuizType quizType,
                              QuizExecution quizExecution, Set<QuizQuestion> questions) {
        super(unitOfWork, serviceName, null);
        this.title = title;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.quizType = quizType;
        this.quizExecution = quizExecution;
        this.questions = questions;
    }

    public String getTitle() { return title; }
    public LocalDateTime getAvailableDate() { return availableDate; }
    public LocalDateTime getConclusionDate() { return conclusionDate; }
    public LocalDateTime getResultsDate() { return resultsDate; }
    public QuizType getQuizType() { return quizType; }
    public QuizExecution getQuizExecution() { return quizExecution; }
    public Set<QuizQuestion> getQuestions() { return questions; }
}
