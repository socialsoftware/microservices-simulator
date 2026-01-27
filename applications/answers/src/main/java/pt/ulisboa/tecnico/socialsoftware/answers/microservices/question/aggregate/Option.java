package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Long id;
    private Integer key;
    private Integer sequence;
    private boolean correct;
    private String content;
    @OneToOne
    private Question question;

    public Option() {

    }

    public Option(OptionDto optionDto) {
        setKey(optionDto.getKey());
        setSequence(optionDto.getSequence());
        setCorrect(optionDto.getCorrect());
        setContent(optionDto.getContent());
    }


    public Option(Option other) {
        setSequence(other.getSequence());
        setCorrect(other.getCorrect());
        setContent(other.getContent());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }

    public boolean getCorrect() {
        return correct;
    }

    public void setCorrect(boolean correct) {
        this.correct = correct;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }



    public OptionDto buildDto() {
        OptionDto dto = new OptionDto();
        dto.setKey(getKey());
        dto.setSequence(getSequence());
        dto.setCorrect(getCorrect());
        dto.setContent(getContent());
        return dto;
    }
}