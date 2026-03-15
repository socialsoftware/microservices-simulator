package pt.ulisboa.tecnico.socialsoftware.ms.faultanalysis.testapp.service;

import org.springframework.stereotype.Service;

/**
 * A @Service class that does NOT inject UnitOfWorkService via a constructor.
 * Used to verify that ServiceVisitor ignores such classes.
 */
@Service
public class CourseFactory {

    public CourseFactory() {
    }
}
