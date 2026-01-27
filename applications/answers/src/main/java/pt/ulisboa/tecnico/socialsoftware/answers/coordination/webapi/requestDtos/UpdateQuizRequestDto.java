package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.QuizType;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizQuestionDto;

public class UpdateQuizRequestDto {
    @NotNull
    private String title;
    @NotNull
    private String quizType;
    @NotNull
    private LocalDateTime creationDate;
    @NotNull
    private LocalDateTime availableDate;
    @NotNull
    private LocalDateTime conclusionDate;
    @NotNull
    private LocalDateTime resultsDate;
    @NotNull
    private QuizExecutionDto execution;
    @NotNull
    private Set<QuizQuestionDto> questions;

    public UpdateQuizRequestDto() {}

    public UpdateQuizRequestDto(String title, String quizType, LocalDateTime creationDate, LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate, QuizExecutionDto execution, Set<QuizQuestionDto> questions) {
        this.title = title;
        this.quizType = quizType;
        this.creationDate = creationDate;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.execution = execution;
        this.questions = questions;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public String getQuizType() {
        return quizType;
    }

    public void setQuizType(String quizType) {
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
    public QuizExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(QuizExecutionDto execution) {
        this.execution = execution;
    }
    public Set<QuizQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(Set<QuizQuestionDto> questions) {
        this.questions = questions;
    }
}
