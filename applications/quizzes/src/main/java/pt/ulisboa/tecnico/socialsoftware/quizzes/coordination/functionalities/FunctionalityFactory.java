package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities;


import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.lang.ClassNotFoundException;
import java.lang.RuntimeException;
import java.lang.Exception;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.AddStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.AnonymizeStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.CreateCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionByIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionsByUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionsFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.RemoveCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.RemoveStudentFromCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.UpdateStudentNameFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AddStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AnonymizeStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.CreateCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionsByUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.RemoveCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.RemoveStudentFromCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.UpdateStudentNameFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class FunctionalityFactory {

    void FunctionalityFactory() {
    }
    /* 
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private UserService userService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;
    
    @Autowired
    private Environment env;
    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        workflowType = SAGAS;
    }
    */

    public void executeFunctionality(String name, Object[] constructorArgs) throws QuizzesException {
        try {
            // Load the class dynamically
            Class<?> clazz = Class.forName(name);
            
            //Constructor<?>[] constructors = clazz.getConstructors();
            //Constructor<?> constructor = constructors[0];
            //Object obj = constructor.newInstance(constructorArgs);


            // Cast if needed and call a method
        //((WorkflowFunctionality) obj).executeWorkflow((SagaUnitOfWork) constructorArgs[constructorArgs.length - 1]);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: ", e);
        } catch (Exception e) {
            throw new RuntimeException("Error executing functionality: " + e.getMessage(), e);
        }
    }
}
