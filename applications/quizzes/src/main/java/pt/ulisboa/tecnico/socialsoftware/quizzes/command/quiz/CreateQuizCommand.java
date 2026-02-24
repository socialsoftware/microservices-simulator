package pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;

import java.util.Set;

public class CreateQuizCommand extends Command {
    private QuizCourseExecution quizCourseExecution;
    private Set<QuestionDto> questions;
    private QuizDto quizDto;

    public CreateQuizCommand(UnitOfWork unitOfWork, String serviceName, QuizCourseExecution quizCourseExecution, Set<QuestionDto> questions, QuizDto quizDto) {
        super(unitOfWork, serviceName, null);
        this.quizCourseExecution = quizCourseExecution;
        this.questions = questions;
        this.quizDto = quizDto;
    }

    public QuizCourseExecution getQuizCourseExecution() { return quizCourseExecution; }
    public Set<QuestionDto> getQuestions() { return questions; }
    public QuizDto getQuizDto() { return quizDto; }
}
