package pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizQuestion;

import java.time.LocalDateTime;
import java.util.Set;

public class UpdateQuizCommand extends Command {
    private final Integer quizAggregateId;
    private final LocalDateTime availableDate;
    private final LocalDateTime conclusionDate;
    private final LocalDateTime resultsDate;
    private final Set<QuizQuestion> questions;

    public UpdateQuizCommand(UnitOfWork unitOfWork, String serviceName,
                              Integer quizAggregateId,
                              LocalDateTime availableDate, LocalDateTime conclusionDate,
                              LocalDateTime resultsDate, Set<QuizQuestion> questions) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.questions = questions;
    }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public LocalDateTime getAvailableDate() { return availableDate; }
    public LocalDateTime getConclusionDate() { return conclusionDate; }
    public LocalDateTime getResultsDate() { return resultsDate; }
    public Set<QuizQuestion> getQuestions() { return questions; }
}
