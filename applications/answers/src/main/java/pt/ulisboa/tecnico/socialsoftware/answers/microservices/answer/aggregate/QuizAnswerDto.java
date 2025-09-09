package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class QuizAnswerDto implements Serializable {
	private Integer aggregateId;
	private LocalDateTime answerDate;
	private LocalDateTime completedDate;
	private boolean completed;
	private Object quizAnswerStudent;
	private Object quizAnswerCourseExecution;
	private Object questionAnswers;
	private Object answeredQuiz;
	private Integer version;
	private AggregateState state;

	public QuizAnswerDto() {
	}

	public QuizAnswerDto(QuizAnswer quizanswer) {
		this.aggregateId = quizanswer.getAggregateId();
		this.answerDate = quizanswer.getAnswerDate();
		this.completedDate = quizanswer.getCompletedDate();
		this.completed = quizanswer.isCompleted();
		this.quizAnswerStudent = quizanswer.getQuizAnswerStudent();
		this.quizAnswerCourseExecution = quizanswer.getQuizAnswerCourseExecution();
		this.questionAnswers = quizanswer.getQuestionAnswers();
		this.answeredQuiz = quizanswer.getAnsweredQuiz();
		this.version = quizanswer.getVersion();
		this.state = quizanswer.getState();
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

	public Object getQuizAnswerStudent() {
		return quizAnswerStudent;
	}

	public void setQuizAnswerStudent(Object quizAnswerStudent) {
		this.quizAnswerStudent = quizAnswerStudent;
	}

	public Object getQuizAnswerCourseExecution() {
		return quizAnswerCourseExecution;
	}

	public void setQuizAnswerCourseExecution(Object quizAnswerCourseExecution) {
		this.quizAnswerCourseExecution = quizAnswerCourseExecution;
	}

	public Object getQuestionAnswers() {
		return questionAnswers;
	}

	public void setQuestionAnswers(Object questionAnswers) {
		this.questionAnswers = questionAnswers;
	}

	public Object getAnsweredQuiz() {
		return answeredQuiz;
	}

	public void setAnsweredQuiz(Object answeredQuiz) {
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