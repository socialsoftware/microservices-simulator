package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;

@Entity
public abstract class Answer extends Aggregate {
    @Id
    private LocalDateTime answerDate;
    private LocalDateTime completedDate;
    private Boolean completed;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private QuizAnswerStudent quizAnswerStudent;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private QuizAnswerExecution quizAnswerExecution;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "answer")
    private Set<QuestionAnswer> questionAnswers = new HashSet<>();
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "answer")
    private AnsweredQuiz answeredQuiz; 

    public Answer() {
    }

    public Answer(Integer aggregateId, AnswerDto answerDto, QuizAnswerStudent quizAnswerStudent, QuizAnswerExecution quizAnswerExecution, AnsweredQuiz answeredQuiz) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAnswerDate(answerDto.getAnswerDate());
        setCompletedDate(answerDto.getCompletedDate());
        setCompleted(answerDto.getCompleted());
        setQuizAnswerStudent(quizAnswerStudent);
        setQuizAnswerExecution(quizAnswerExecution);
        setAnsweredQuiz(answeredQuiz);
    }

    public Answer(Answer other) {
        super(other);
        setAnswerDate(other.getAnswerDate());
        setCompletedDate(other.getCompletedDate());
        setCompleted(other.getCompleted());
        setQuizAnswerStudent(new QuizAnswerStudent(other.getQuizAnswerStudent()));
        setQuizAnswerExecution(new QuizAnswerExecution(other.getQuizAnswerExecution()));
        setQuestionAnswers(other.getQuestionAnswers().stream().map(QuestionAnswer::new).collect(Collectors.toSet()));
        setAnsweredQuiz(new AnsweredQuiz(other.getAnsweredQuiz()));
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

    public QuizAnswerStudent getQuizAnswerStudent() {
        return quizAnswerStudent;
    }

    public void setQuizAnswerStudent(QuizAnswerStudent quizAnswerStudent) {
        this.quizAnswerStudent = quizAnswerStudent;
        if (this.quizAnswerStudent != null) {
            this.quizAnswerStudent.setAnswer(this);
        }
    }

    public QuizAnswerExecution getQuizAnswerExecution() {
        return quizAnswerExecution;
    }

    public void setQuizAnswerExecution(QuizAnswerExecution quizAnswerExecution) {
        this.quizAnswerExecution = quizAnswerExecution;
        if (this.quizAnswerExecution != null) {
            this.quizAnswerExecution.setAnswer(this);
        }
    }

    public Set<QuestionAnswer> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(Set<QuestionAnswer> questionAnswers) {
        this.questionAnswers = questionAnswers;
        if (this.questionAnswers != null) {
            this.questionAnswers.forEach(questionanswer -> questionanswer.setAnswer(this));
        }
    }

    public AnsweredQuiz getAnsweredQuiz() {
        return answeredQuiz;
    }

    public void setAnsweredQuiz(AnsweredQuiz answeredQuiz) {
        this.answeredQuiz = answeredQuiz;
        if (this.answeredQuiz != null) {
            this.answeredQuiz.setAnswer(this);
        }
    }

	public Answer createAnswer(QuizAnswerStudent student, QuizAnswerExecution execution, AnsweredQuiz quiz, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Answer getAnswerById(Integer answerId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public List<Answer> getAnswersByStudent(Integer studentId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public List<Answer> getAnswersByQuiz(Integer quizId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Answer submitAnswer(Integer answerId, Integer questionId, String answer, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

	public Answer completeAnswer(Integer answerId, UnitOfWork unitOfWork) {

		return null; // TODO: Implement method
	}

}