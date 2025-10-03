package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class AnswerDto implements Serializable {
	private Integer aggregateId;
	private LocalDateTime answerDate;
	private LocalDateTime completedDate;
	private boolean completed;
	private QuizAnswerStudent quizAnswerStudent;
	private QuizAnswerExecution quizAnswerExecution;
	private Set<QuestionAnswer> questionAnswers;
	private AnsweredQuiz answeredQuiz;
	private Integer version;
	private AggregateState state;

	public AnswerDto() {
	}

	public AnswerDto(Answer answer) {
		this.aggregateId = answer.getAggregateId();
		this.answerDate = answer.getAnswerDate();
		this.completedDate = answer.getCompletedDate();
		this.completed = answer.isCompleted();
		this.quizAnswerStudent = answer.getQuizAnswerStudent();
		this.quizAnswerExecution = answer.getQuizAnswerExecution();
		this.questionAnswers = answer.getQuestionAnswers();
		this.answeredQuiz = answer.getAnsweredQuiz();
		this.version = answer.getVersion();
		this.state = answer.getState();
	}

	public Integer getAggregateId() {
		return aggregateId;
	}

	public void setAggregateId(Integer aggregateId) {
		this.aggregateId = aggregateId;
	}

	public LocalDateTime getAnswerDate() {
		return answerDate;
	}

	public void setAnswerDate(LocalDateTime answerDate) {
		this.answerDate = answerDate;
	}

	public LocalDateTime getCompletedDate() {
		return completedDate;
	}

	public void setCompletedDate(LocalDateTime completedDate) {
		this.completedDate = completedDate;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(Boolean completed) {
		this.completed = completed;
	}

	public QuizAnswerStudent getQuizAnswerStudent() {
		return quizAnswerStudent;
	}

	public void setQuizAnswerStudent(QuizAnswerStudent quizAnswerStudent) {
		this.quizAnswerStudent = quizAnswerStudent;
	}

	public QuizAnswerExecution getQuizAnswerExecution() {
		return quizAnswerExecution;
	}

	public void setQuizAnswerExecution(QuizAnswerExecution quizAnswerExecution) {
		this.quizAnswerExecution = quizAnswerExecution;
	}

	public Set<QuestionAnswer> getQuestionAnswers() {
		return questionAnswers;
	}

	public void setQuestionAnswers(Set<QuestionAnswer> questionAnswers) {
		this.questionAnswers = questionAnswers;
	}

	public AnsweredQuiz getAnsweredQuiz() {
		return answeredQuiz;
	}

	public void setAnsweredQuiz(AnsweredQuiz answeredQuiz) {
		this.answeredQuiz = answeredQuiz;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public AggregateState getState() {
		return state;
	}

	public void setState(AggregateState state) {
		this.state = state;
	}
}