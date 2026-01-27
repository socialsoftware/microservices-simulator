package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos;

import jakarta.validation.constraints.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
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

public class UpdateTournamentRequestDto {
    @NotNull
    private LocalDateTime startTime;
    @NotNull
    private LocalDateTime endTime;
    @NotNull
    private Integer numberOfQuestions;
    @NotNull
    private Boolean cancelled;
    @NotNull
    private TournamentCreatorDto creator;
    @NotNull
    private Set<TournamentParticipantDto> participants;
    @NotNull
    private TournamentExecutionDto execution;
    @NotNull
    private Set<TournamentTopicDto> topics;
    @NotNull
    private TournamentQuizDto quiz;

    public UpdateTournamentRequestDto() {}

    public UpdateTournamentRequestDto(LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions, Boolean cancelled, TournamentCreatorDto creator, Set<TournamentParticipantDto> participants, TournamentExecutionDto execution, Set<TournamentTopicDto> topics, TournamentQuizDto quiz) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.numberOfQuestions = numberOfQuestions;
        this.cancelled = cancelled;
        this.creator = creator;
        this.participants = participants;
        this.execution = execution;
        this.topics = topics;
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
    public TournamentCreatorDto getCreator() {
        return creator;
    }

    public void setCreator(TournamentCreatorDto creator) {
        this.creator = creator;
    }
    public Set<TournamentParticipantDto> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<TournamentParticipantDto> participants) {
        this.participants = participants;
    }
    public TournamentExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(TournamentExecutionDto execution) {
        this.execution = execution;
    }
    public Set<TournamentTopicDto> getTopics() {
        return topics;
    }

    public void setTopics(Set<TournamentTopicDto> topics) {
        this.topics = topics;
    }
    public TournamentQuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(TournamentQuizDto quiz) {
        this.quiz = quiz;
    }
}
