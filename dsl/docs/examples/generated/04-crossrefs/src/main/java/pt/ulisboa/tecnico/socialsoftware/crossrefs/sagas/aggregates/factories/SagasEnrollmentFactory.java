package pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentFactory;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.SagaEnrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.sagas.aggregates.dtos.SagaEnrollmentDto;

@Service
@Profile("sagas")
public class SagasEnrollmentFactory implements EnrollmentFactory {
    @Override
    public Enrollment createEnrollment(Integer aggregateId, EnrollmentDto enrollmentDto) {
        return new SagaEnrollment(aggregateId, enrollmentDto);
    }

    @Override
    public Enrollment createEnrollmentFromExisting(Enrollment existingEnrollment) {
        return new SagaEnrollment((SagaEnrollment) existingEnrollment);
    }

    @Override
    public EnrollmentDto createEnrollmentDto(Enrollment enrollment) {
        return new SagaEnrollmentDto(enrollment);
    }
}