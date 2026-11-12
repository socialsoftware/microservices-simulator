package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;

import java.time.LocalDateTime;
import java.util.List;

public class UpdateQuizCommand extends Command {
    private Integer quizAggregateId;
    private String title;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private List<QuizQuestionDto> questions;

    protected UpdateQuizCommand() {}

    public UpdateQuizCommand(UnitOfWork unitOfWork, String serviceName, Integer quizAggregateId, String title,
                             LocalDateTime availableDate, LocalDateTime conclusionDate,
                             LocalDateTime resultsDate, List<QuizQuestionDto> questions) {
        super(unitOfWork, serviceName, quizAggregateId);
        this.quizAggregateId = quizAggregateId;
        this.title = title;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.questions = questions;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
    }

    public List<QuizQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionDto> questions) {
        this.questions = questions;
    }
}
