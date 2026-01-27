package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class AnswerUser {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private AggregateState userState;
    private String userName;
    @OneToOne
    private Answer answer;

    public AnswerUser() {

    }

    public AnswerUser(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserState(userDto.getState());
        setUserName(userDto.getName());
    }

    public AnswerUser(AnswerUser other) {
        setUserState(other.getUserState());
        setUserName(other.getUserName());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Answer getAnswer() {
        return answer;
    }

    public void setAnswer(Answer answer) {
        this.answer = answer;
    }


    public AnswerUserDto buildDto() {
        AnswerUserDto dto = new AnswerUserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setState(getUserState());
        dto.setName(getUserName());
        return dto;
    }
}