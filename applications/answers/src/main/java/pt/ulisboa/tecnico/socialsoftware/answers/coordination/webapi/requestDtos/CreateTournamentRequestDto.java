package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizDto;
import java.util.Set;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentCreatorDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentParticipantDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentQuizDto;

public class CreateTournamentRequestDto {
    @NotNull
    private ExecutionDto execution;
    @NotNull
    private Set<TopicDto> topics;
    @NotNull
    private QuizDto quiz;
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private Integer numberOfQuestions;
    @NotNull
    private Boolean cancelled;

    public CreateTournamentRequestDto() {}

    public CreateTournamentRequestDto(ExecutionDto execution, Set<TopicDto> topics, QuizDto quiz, LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled) {
        this.execution = execution;
        this.topics = topics;
        this.quiz = quiz;
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = cancelled;
    }

    public ExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(ExecutionDto execution) {
        this.execution = execution;
    }
    public Set<TopicDto> getTopics() {
        return topics;
    }

    public void setTopics(Set<TopicDto> topics) {
        this.topics = topics;
    }
    public QuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(QuizDto quiz) {
        this.quiz = quiz;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }
    public Boolean getCancelled() {
        return cancelled;
    }

    public void setCancelled(Boolean cancelled) {
        this.cancelled = cancelled;
    }
}
