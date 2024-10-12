package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler;

@Entity
public class TournamentParticipant {
    @Id
    @GeneratedValue
    private Long id;
    private Integer participantAggregateId;
    private String participantName;
    private String participantUsername;
    private LocalDateTime enrollTime;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "tournamentParticipant")
    private TournamentParticipantQuizAnswer participantAnswer;
    private Integer participantVersion;
    @Enumerated(EnumType.STRING)
    private AggregateState state;
    @ManyToOne
    private Tournament tournament;

    public TournamentParticipant() {
        setEnrollTime(DateHandler.now());
    }

    public TournamentParticipant(UserDto userDto) {
        setParticipantAggregateId(userDto.getAggregateId());
        setParticipantName(userDto.getName());
        setParticipantUsername(userDto.getUsername());
        setParticipantVersion(userDto.getVersion());
        setParticipantAnswer(new TournamentParticipantQuizAnswer());
        setEnrollTime(DateHandler.now());
        setState(AggregateState.ACTIVE);
    }

    public TournamentParticipant(TournamentParticipant other) {
        setParticipantAggregateId(other.getParticipantAggregateId());
        setParticipantName(other.getParticipantName());
        setParticipantUsername(other.getParticipantUsername());
        setParticipantVersion(other.getParticipantVersion());
        setParticipantAnswer(new TournamentParticipantQuizAnswer(other.getParticipantAnswer()));
        setEnrollTime(other.getEnrollTime());
        setState(other.getState());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void answerQuiz() {
        this.participantAnswer.setAnswered(true);
    }

    public Integer getParticipantAggregateId() {
        return participantAggregateId;
    }

    public void setParticipantAggregateId(Integer id) {
        this.participantAggregateId = id;
    }

    public String getParticipantName() {
        return participantName;
    }

    public void setParticipantName(String participantName) {
        this.participantName = participantName;
    }

    public String getParticipantUsername() {
        return participantUsername;
    }

    public void setParticipantUsername(String participantUsername) {
        this.participantUsername = participantUsername;
    }

    public LocalDateTime getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDateTime enrollTime) {
        this.enrollTime = enrollTime;
    }

    public TournamentParticipantQuizAnswer getParticipantAnswer() {
        return participantAnswer;
    }

    public void setParticipantAnswer(TournamentParticipantQuizAnswer participantAnswer) {
        this.participantAnswer = participantAnswer;
        this.participantAnswer.setTournamentParticipant(this);
    }

    public Integer getParticipantVersion() {
        return participantVersion;
    }

    public void setParticipantVersion(Integer participantVersion) {
        this.participantVersion = participantVersion;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public void updateAnswerWithQuestion(Integer quizAnswerAggregateId, Integer questionAnswerAggregateId, boolean isCorrect, Integer version) {
        this.participantAnswer.updateAnswerWithQuestion(quizAnswerAggregateId, questionAnswerAggregateId, isCorrect, version);
    }

    public UserDto buildDto() {
        UserDto userDto = new UserDto();
        userDto.setAggregateId(getParticipantAggregateId());
        userDto.setVersion(getParticipantVersion());
        userDto.setName(getParticipantName());
        userDto.setUsername(getParticipantUsername());
        userDto.setAnswerAggregateId(getParticipantAnswer().getQuizAnswerAggregateId());
        userDto.setNumberAnswered(getParticipantAnswer().getNumberOfAnswered());
        userDto.setNumberCorrect(getParticipantAnswer().getNumberOfCorrect());
        return userDto;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + getParticipantAggregateId();
        hash = 31 * hash + (getParticipantVersion() == null ? 0 : getParticipantVersion().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TournamentParticipant otherParticipant)) {
            return false;
        }

        return getParticipantAggregateId() != null && getParticipantAggregateId().equals(otherParticipant.getParticipantAggregateId()) &&
                getParticipantVersion() != null && getParticipantVersion().equals(otherParticipant.getParticipantVersion());
    }
}
