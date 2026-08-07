package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "quiz_answers")
public abstract class QuizAnswer extends Aggregate {
    private final LocalDateTime creationDate;
    private final LocalDateTime answerDate;
    private Boolean completed;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "quizAnswer")
    private QuizAnswerQuiz quiz;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "quizAnswer")
    private QuizAnswerStudent student;
    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "quizAnswer")
    private QuizAnswerExecution execution;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionAnswer> questionAnswers = new ArrayList<>();

    public QuizAnswer() {
        this.creationDate = null;
        this.answerDate = null;
    }

    public QuizAnswer(Integer aggregateId, Integer quizAggregateId, Long quizVersion, Integer userAggregateId,
                      String userName, Long userVersion, Integer executionAggregateId, Long executionVersion,
                      LocalDateTime creationDate, LocalDateTime answerDate) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = false;
        setQuiz(new QuizAnswerQuiz(quizAggregateId, quizVersion));
        setStudent(new QuizAnswerStudent(userAggregateId, userName, userVersion));
        setExecution(new QuizAnswerExecution(executionAggregateId, executionVersion));
    }

    public QuizAnswer(QuizAnswer other) {
        super(other);
        this.creationDate = other.getCreationDate();
        this.answerDate = other.getAnswerDate();
        this.completed = other.getCompleted();
        setQuiz(new QuizAnswerQuiz(other.getQuiz()));
        setStudent(new QuizAnswerStudent(other.getStudent()));
        setExecution(new QuizAnswerExecution(other.getExecution()));
        this.questionAnswers = other.getQuestionAnswers().stream()
                .map(QuestionAnswer::new)
                .collect(Collectors.toList());
    }

    @Override
    public void verifyInvariants() {
        questionNotAlreadyAnswered();
        answerMatchesCorrectOption();
    }

    private void questionNotAlreadyAnswered() {
        Set<Integer> distinctQuestionIds = this.questionAnswers.stream()
                .map(QuestionAnswer::getQuestionAggregateId)
                .collect(Collectors.toSet());
        if (distinctQuestionIds.size() != this.questionAnswers.size()) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.QUESTION_ALREADY_ANSWERED);
        }
    }

    // An unanswered QuestionAnswer carries no optionKey, so correctness is not yet decided for it.
    private void answerMatchesCorrectOption() {
        boolean allConsistent = this.questionAnswers.stream()
                .filter(questionAnswer -> questionAnswer.getOptionKey() != null)
                .allMatch(questionAnswer -> Objects.equals(questionAnswer.getCorrect(),
                        Objects.equals(questionAnswer.getOptionKey(), questionAnswer.getCorrectOptionKey())));
        if (!allConsistent) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.ANSWER_MATCHES_CORRECT_OPTION);
        }
    }

    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public QuizAnswerQuiz getQuiz() {
        return quiz;
    }

    // No setter: the Quiz reference is immutable (QUIZANSWER_FINAL_QUIZ). Both constructors install
    // the snapshot through here so the back-reference mappedBy = "quizAnswer" resolves is always
    // wired before the entity is persisted. Same for the student and execution snapshots below.
    private void setQuiz(QuizAnswerQuiz quiz) {
        this.quiz = quiz;
        quiz.setQuizAnswer(this);
    }

    public QuizAnswerStudent getStudent() {
        return student;
    }

    private void setStudent(QuizAnswerStudent student) {
        this.student = student;
        student.setQuizAnswer(this);
    }

    public QuizAnswerExecution getExecution() {
        return execution;
    }

    private void setExecution(QuizAnswerExecution execution) {
        this.execution = execution;
        execution.setQuizAnswer(this);
    }

    public List<QuestionAnswer> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(List<QuestionAnswer> questionAnswers) {
        this.questionAnswers = questionAnswers;
    }

    public void addQuestionAnswer(QuestionAnswer questionAnswer) {
        this.questionAnswers.add(questionAnswer);
    }

    public void removeQuestionAnswer(Integer questionAggregateId) {
        this.questionAnswers.removeIf(
                questionAnswer -> questionAggregateId.equals(questionAnswer.getQuestionAggregateId()));
    }
}
