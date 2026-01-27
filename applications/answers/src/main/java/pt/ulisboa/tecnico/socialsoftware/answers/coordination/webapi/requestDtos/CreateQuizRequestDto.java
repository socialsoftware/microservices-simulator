package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;

public class CreateQuizRequestDto {
    @NotNull
    private ExecutionDto execution;
    @NotNull
    private Set<QuestionDto> questions;
    @NotNull
    private String title;
    @NotNull
    private QuizType quizType;
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private LocalDateTime availableDate;
    @NotNull
    private LocalDateTime conclusionDate;
    @NotNull
    private LocalDateTime resultsDate;

    public CreateQuizRequestDto() {}

    public CreateQuizRequestDto(ExecutionDto execution, Set<QuestionDto> questions, String title, QuizType quizType, LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate) {
        this.execution = execution;
        this.questions = questions;
        this.title = title;
        this.quizType = quizType;
        this.creationDate = creationDate;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
    }

    public ExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(ExecutionDto execution) {
        this.execution = execution;
    }
    public Set<QuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<QuestionDto> questions) {
        this.questions = questions;
    }
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }
    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
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
}
