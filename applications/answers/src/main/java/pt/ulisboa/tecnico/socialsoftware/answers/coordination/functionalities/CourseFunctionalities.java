package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.coursefactory.service.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseDto;

@Service
public class CourseFunctionalities {
    @Autowired
    private CourseService courseService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CourseFactory courseFactory;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CourseDto> getCoursesByType(String courseType, UnitOfWork unitOfWork) {
        return new ArrayList<>(); // TODO: Implement getCoursesByType
    }

    public List<CourseDto> searchCoursesByName(String name, UnitOfWork unitOfWork) {
        return new ArrayList<>(); // TODO: Implement searchCoursesByName
    }

    public List<CourseDto> searchCoursesByAcronym(String acronym, UnitOfWork unitOfWork) {
        return new ArrayList<>(); // TODO: Implement searchCoursesByAcronym
    }

    public Set<String> getUniqueCourseTypes(UnitOfWork unitOfWork) {
        return new HashSet<>(); // TODO: Implement getUniqueCourseTypes
    }

    public Set<CourseDto> getCoursesAsSet(UnitOfWork unitOfWork) {
        return new HashSet<>(); // TODO: Implement getCoursesAsSet
    }

}