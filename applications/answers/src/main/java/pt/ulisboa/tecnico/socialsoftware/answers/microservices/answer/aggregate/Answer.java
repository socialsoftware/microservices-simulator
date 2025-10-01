package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;

@Entity
public class Answer extends Aggregate {
    @Id
    private LocalDateTime answerDate;
    private LocalDateTime completedDate;
    private Boolean completed;
    private Object quizAnswerStudent;
    private Object quizAnswerCourseExecution;
    private Object questionAnswers;
    private Object answeredQuiz; 

    public Answer(LocalDateTime answerDate, LocalDateTime completedDate, Boolean completed, Object quizAnswerStudent, Object quizAnswerCourseExecution, Object questionAnswers, Object answeredQuiz) {
        this.answerDate = answerDate;
        this.completedDate = completedDate;
        this.completed = completed;
        this.quizAnswerStudent = quizAnswerStudent;
        this.quizAnswerCourseExecution = quizAnswerCourseExecution;
        this.questionAnswers = questionAnswers;
        this.answeredQuiz = answeredQuiz;
    }

    public Answer(Answer other) {
        // Copy constructor
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

    public Boolean isCompleted() {
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
	public Object createAnswer(Object student, Object courseExecution, Object quiz, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object getAnswerById(Integer answerId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public List<Answer> getAnswersByStudent(Integer studentId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public List<Answer> getAnswersByQuiz(Integer quizId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object submitAnswer(Integer answerId, Integer questionId, String answer, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Object completeAnswer(Integer answerId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}