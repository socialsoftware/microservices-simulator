package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Profile("sagas")
@Service
public class SagaQuizFunctionalities implements QuizFunctionalitiesInterface{
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private QuestionService questionService;

    public QuizDto createQuiz(Integer courseExecutionId, QuizDto quizDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        try {
            QuizCourseExecution quizCourseExecution = new QuizCourseExecution(courseExecutionService.getCourseExecutionById(courseExecutionId, unitOfWork));
            Set<QuestionDto> questions = quizDto.getQuestionDtos().stream()
                    .map(qq -> questionService.getQuestionById(qq.getAggregateId(), unitOfWork))
                    .collect(Collectors.toSet());
            QuizDto createdQuizDto = quizService.createQuiz(quizCourseExecution, questions, quizDto, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> quizService.removeQuiz(createdQuizDto.getAggregateId(), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
            return createdQuizDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error creating quiz", ex);
        }
    }

    public QuizDto findQuiz(Integer quizAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return quizService.getQuizById(quizAggregateId, unitOfWork);
    }

    public List<QuizDto> getAvailableQuizzes(Integer userAggregateId, Integer courseExecutionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        return quizService.getAvailableQuizzes(courseExecutionAggregateId, unitOfWork);
    }

    public QuizDto updateQuiz(QuizDto quizDto) throws Exception {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        QuizDto originalQuizDto = null;
        try {
            originalQuizDto = quizService.getQuizById(quizDto.getAggregateId(), unitOfWork);
            Set<QuizQuestion> quizQuestions = quizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet());
            QuizDto updatedQuizDto = quizService.updateQuiz(quizDto, quizQuestions, unitOfWork);

            // TODO
            // unitOfWork.registerCompensation(() -> quizService.updateQuiz(originalQuizDto, originalQuizDto.getQuestionDtos().stream().map(QuizQuestion::new).collect(Collectors.toSet()), unitOfWork));

            unitOfWorkService.commit(unitOfWork);
            return updatedQuizDto;
        } catch (Exception ex) {
            unitOfWorkService.compensate(unitOfWork);
            throw new Exception("Error updating quiz", ex);
        }
    }

}
