package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;

public interface EnrollmentFactory {
    Enrollment createEnrollment(Integer aggregateId, EnrollmentDto enrollmentDto);
    Enrollment createEnrollmentFromExisting(Enrollment existingEnrollment);
    EnrollmentDto createEnrollmentDto(Enrollment enrollment);
}
