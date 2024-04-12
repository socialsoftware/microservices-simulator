package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.aggregate.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.service.QuestionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizFunctionalitiesInterface;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("tcc")
@Service
public class CausalQuizFunctionalities implements QuizFunctionalitiesInterface {
    @Autowired
    private CausalUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private QuestionService questionService;

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork));

        Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
                .collect(Collectors.toSet());

        QuizDto quizDto1 = quizService.createQuiz(quizCourseExecution, questions, quizDto, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
        return quizDto1;

    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return quizService.getQuizById(quizAggregateId, unitOfWork);
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
    }

    public QuizDto updateQuiz(QuizDto quizDto) {
        CausalUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
        QuizDto quizDto1 = quizService.updateQuiz(quizDto, quizQuestions, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
        return quizDto1;
    }

}
