package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import java.time.LocalDateTime;

// The participant's quiz-answer statistics are flattened into this Dto: TournamentParticipantQuizAnswer
// is owned one-per-participant and never travels on its own.
public class TournamentParticipantDto {
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Long userVersion;
    private LocalDateTime enrollTime;
    private Integer quizAnswerAggregateId;
    private Long quizAnswerVersion;
    private Boolean answered;
    private Integer numberOfAnswered;
    private Integer numberOfCorrect;
    private LocalDateTime firstAnswerTime;

    public TournamentParticipantDto() {
    }

    public TournamentParticipantDto(Integer userAggregateId, String userName, String userUsername, Long userVersion,
                                    LocalDateTime enrollTime, Integer quizAnswerAggregateId, Long quizAnswerVersion,
                                    Boolean answered, Integer numberOfAnswered, Integer numberOfCorrect,
                                    LocalDateTime firstAnswerTime) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
        this.enrollTime = enrollTime;
        this.quizAnswerAggregateId = quizAnswerAggregateId;
        this.quizAnswerVersion = quizAnswerVersion;
        this.answered = answered;
        this.numberOfAnswered = numberOfAnswered;
        this.numberOfCorrect = numberOfCorrect;
        this.firstAnswerTime = firstAnswerTime;
    }

    public TournamentParticipantDto(TournamentParticipant participant) {
        this.userAggregateId = participant.getUserAggregateId();
        this.userName = participant.getUserName();
        this.userUsername = participant.getUserUsername();
        this.userVersion = participant.getUserVersion();
        this.enrollTime = participant.getEnrollTime();
        TournamentParticipantQuizAnswer quizAnswer = participant.getQuizAnswer();
        this.quizAnswerAggregateId = quizAnswer.getQuizAnswerAggregateId();
        this.quizAnswerVersion = quizAnswer.getQuizAnswerVersion();
        this.answered = quizAnswer.getAnswered();
        this.numberOfAnswered = quizAnswer.getNumberOfAnswered();
        this.numberOfCorrect = quizAnswer.getNumberOfCorrect();
        this.firstAnswerTime = quizAnswer.getFirstAnswerTime();
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public LocalDateTime getEnrollTime() {
        return enrollTime;
    }

    public void setEnrollTime(LocalDateTime enrollTime) {
        this.enrollTime = enrollTime;
    }

    public Integer getQuizAnswerAggregateId() {
        return quizAnswerAggregateId;
    }

    public void setQuizAnswerAggregateId(Integer quizAnswerAggregateId) {
        this.quizAnswerAggregateId = quizAnswerAggregateId;
    }

    public Long getQuizAnswerVersion() {
        return quizAnswerVersion;
    }

    public void setQuizAnswerVersion(Long quizAnswerVersion) {
        this.quizAnswerVersion = quizAnswerVersion;
    }

    public Boolean getAnswered() {
        return answered;
    }

    public void setAnswered(Boolean answered) {
        this.answered = answered;
    }

    public Integer getNumberOfAnswered() {
        return numberOfAnswered;
    }

    public void setNumberOfAnswered(Integer numberOfAnswered) {
        this.numberOfAnswered = numberOfAnswered;
    }

    public Integer getNumberOfCorrect() {
        return numberOfCorrect;
    }

    public void setNumberOfCorrect(Integer numberOfCorrect) {
        this.numberOfCorrect = numberOfCorrect;
    }

    public LocalDateTime getFirstAnswerTime() {
        return firstAnswerTime;
    }

    public void setFirstAnswerTime(LocalDateTime firstAnswerTime) {
        this.firstAnswerTime = firstAnswerTime;
    }
}
