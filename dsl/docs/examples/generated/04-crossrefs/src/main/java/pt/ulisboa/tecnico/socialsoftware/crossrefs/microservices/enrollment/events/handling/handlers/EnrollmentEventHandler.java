package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.eventProcessing.EnrollmentEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.Enrollment;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.EnrollmentRepository;

public abstract class EnrollmentEventHandler extends EventHandler {
    private EnrollmentRepository enrollmentRepository;
    protected EnrollmentEventProcessing enrollmentEventProcessing;

    public EnrollmentEventHandler(EnrollmentRepository enrollmentRepository, EnrollmentEventProcessing enrollmentEventProcessing) {
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentEventProcessing = enrollmentEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return enrollmentRepository.findAll().stream().map(Enrollment::getAggregateId).collect(Collectors.toSet());
    }

}
