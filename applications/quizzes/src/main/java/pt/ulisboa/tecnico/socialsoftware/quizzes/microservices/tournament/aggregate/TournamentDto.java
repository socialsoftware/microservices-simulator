package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.io.Serializable;
import java.util.Set;
import java.util.stream.Collectors;

public class TournamentDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private String startTime;
    private String endTime;
    private Integer numberOfQuestions;
    private boolean isCancelled;
    private UserDto creator;
    private Set<UserDto> participants;
    private CourseExecutionDto courseExecution;
    private Set<TopicDto> topics;
    private QuizDto quiz;
    private String state;

    public TournamentDto() {
    }

    public TournamentDto(Tournament tournament) {
        setAggregateId(tournament.getAggregateId());
        setVersion(tournament.getVersion());
        setStartTime(DateHandler.toISOString(tournament.getStartTime()));
        setEndTime(DateHandler.toISOString(tournament.getEndTime()));
        setNumberOfQuestions(tournament.getNumberOfQuestions());
        setCancelled(tournament.isCancelled());
        setCreator(tournament.getTournamentCreator().buildDto());
        setParticipants(tournament.getTournamentParticipants().stream().map(TournamentParticipant::buildDto).collect(Collectors.toSet()));
        setCourseExecution(tournament.getTournamentCourseExecution().buildDto());
        setTopics(tournament.getTournamentTopics().stream().map(TournamentTopic::buildDto).collect(Collectors.toSet()));
        setQuiz(tournament.getTournamentQuiz().buildDto());
        setState(tournament.getState().toString());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public Integer getNumberOfQuestions() {
        return numberOfQuestions;
    }

    public void setNumberOfQuestions(Integer numberOfQuestions) {
        this.numberOfQuestions = numberOfQuestions;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public UserDto getCreator() {
        return creator;
    }

    public void setCreator(UserDto creator) {
        this.creator = creator;
    }

    public Set<UserDto> getParticipants() {
        return participants;
    }

    public void setParticipants(Set<UserDto> participants) {
        this.participants = participants;
    }

    public CourseExecutionDto getCourseExecution() {
        return courseExecution;
    }

    public void setCourseExecution(CourseExecutionDto courseExecution) {
        this.courseExecution = courseExecution;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public UserDto findParticipant(Integer userAggregateId) {
        return this.participants.stream().filter(p -> p.getAggregateId().equals(userAggregateId)).findFirst()
                .orElse(null);
    }
}
