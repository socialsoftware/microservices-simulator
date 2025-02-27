package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Entity
public class AnswerStudent {
    @Id
    @GeneratedValue
    private Long id;
    private Integer studentAggregateId;
    private String name;
    private Aggregate.AggregateState studentState;
    @OneToOne
    private QuizAnswer quizAnswer;

    public AnswerStudent() {
        this.studentAggregateId = 0;
    }

    public AnswerStudent(UserDto userDto) {
        this.studentAggregateId = userDto.getAggregateId();
        this.name = userDto.getName();
        setStudentState(userDto.getState());
    }

    public AnswerStudent(AnswerStudent other) {
        this.studentAggregateId = other.getStudentAggregateId();
        this.name = other.getName();
        setStudentState(other.getStudentState());
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Aggregate.AggregateState getStudentState() {
        return studentState;
    }

    public void setStudentState(Aggregate.AggregateState studentState) {
        this.studentState = studentState;
    }

    public QuizAnswer getQuizAnswer() {
        return quizAnswer;
    }

    public void setQuizAnswer(QuizAnswer quizAnswer) {
        this.quizAnswer = quizAnswer;
    }
}
