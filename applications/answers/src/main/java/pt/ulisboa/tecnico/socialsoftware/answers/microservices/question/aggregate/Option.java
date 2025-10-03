package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Integer optionNumber;
    private String content;
    private Boolean isCorrect;
    @OneToOne
    private Question question; 

    public Option() {
    }

    public Option(QuestionDto questionDto) {
        setContent(questionDto.getContent());
        setIsCorrect(questionDto.getIsCorrect());
    }

    public Option(Option other) {
        setContent(other.getContent());
        setIsCorrect(other.getIsCorrect());
    }


    public Integer getOptionNumber() {
        return optionNumber;
    }

    public void setOptionNumber(Integer optionNumber) {
        this.optionNumber = optionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsCorrect() {
        return isCorrect;
    }

    public void setIsCorrect(Boolean isCorrect) {
        this.isCorrect = isCorrect;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }


}