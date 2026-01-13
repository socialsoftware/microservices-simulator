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
    private String userName;
    private AggregateState userState;
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
        setUserName(other.getUserName());
        setUserState(other.getUserState());
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

    public AggregateState getUserState() {
        return userState;
    }

    public void setUserState(AggregateState userState) {
        this.userState = userState;
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
        dto.setName(getUserName());
        dto.setState(getUserState());
        return dto;
    }
}