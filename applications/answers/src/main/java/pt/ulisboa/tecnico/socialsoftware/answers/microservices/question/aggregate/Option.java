package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.aggregate;

import java.util.stream.Collectors;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Option {
    @Id
    @GeneratedValue
    private Long id;
    private Integer key;
    private Integer sequence;
    private Boolean correct;
    private String content;
    @OneToOne
    private Question question;

    public Option() {

    }

    public Option(QuestionDto questionDto) {
        setTitle(questionDto.getTitle());
        setContent(questionDto.getContent());
        setCreationDate(questionDto.getCreationDate());
        setTopics(questionDto.getTopics());
        setOptions(questionDto.getOptions() != null ? questionDto.getOptions().stream().map(dto -> new Option(dto)).collect(Collectors.toList()) : null);
    }

    public Option(Option other) {
        setKey(other.getKey());
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

    public Boolean getCorrect() {
        return correct;
    }

    public void setCorrect(Boolean correct) {
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


}