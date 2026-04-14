package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentCourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.EnrollmentTeacherDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.EnrollmentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.EnrollmentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.EnrollmentTeacherRemovedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.EnrollmentTeacherUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception.CrossrefsException;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.coordination.webapi.requestDtos.CreateEnrollmentRequestDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.Teacher;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;


@Service
@Transactional(noRollbackFor = CrossrefsException.class)
public class EnrollmentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private EnrollmentFactory enrollmentFactory;

    @Autowired
    private EnrollmentServiceExtension extension;

    public EnrollmentService() {}

    public EnrollmentDto createEnrollment(CreateEnrollmentRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            EnrollmentDto enrollmentDto = new EnrollmentDto();
            enrollmentDto.setEnrollmentDate(createRequest.getEnrollmentDate());
            enrollmentDto.setActive(createRequest.getActive());
            if (createRequest.getCourse() != null) {
                Course refSource = (Course) unitOfWorkService.aggregateLoadAndRegisterRead(createRequest.getCourse().getAggregateId(), unitOfWork);
                CourseDto refSourceDto = new CourseDto(refSource);
                EnrollmentCourseDto courseDto = new EnrollmentCourseDto();
                courseDto.setAggregateId(refSourceDto.getAggregateId());
                courseDto.setVersion(refSourceDto.getVersion());
                courseDto.setState(refSourceDto.getState() != null ? refSourceDto.getState().name() : null);
                courseDto.setTitle(refSourceDto.getTitle());
                courseDto.setDescription(refSourceDto.getDescription());
                courseDto.setMaxStudents(refSourceDto.getMaxStudents());
                enrollmentDto.setCourse(courseDto);
            }
            if (createRequest.getTeachers() != null) {
                enrollmentDto.setTeachers(createRequest.getTeachers().stream().map(reqDto -> {
                    Teacher refItem = (Teacher) unitOfWorkService.aggregateLoadAndRegisterRead(reqDto.getAggregateId(), unitOfWork);
                    TeacherDto refItemDto = new TeacherDto(refItem);
                    EnrollmentTeacherDto projDto = new EnrollmentTeacherDto();
                    projDto.setAggregateId(refItemDto.getAggregateId());
                    projDto.setVersion(refItemDto.getVersion());
                    projDto.setState(refItemDto.getState() != null ? refItemDto.getState().name() : null);

                    return projDto;
                }).collect(Collectors.toSet()));
            }

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Enrollment enrollment = enrollmentFactory.createEnrollment(aggregateId, enrollmentDto);
            unitOfWorkService.registerChanged(enrollment, unitOfWork);
            return enrollmentFactory.createEnrollmentDto(enrollment);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error creating enrollment: " + e.getMessage());
        }
    }

    public EnrollmentDto getEnrollmentById(Integer id, UnitOfWork unitOfWork) {
        try {
            Enrollment enrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return enrollmentFactory.createEnrollmentDto(enrollment);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving enrollment: " + e.getMessage());
        }
    }

    public List<EnrollmentDto> getAllEnrollments(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = enrollmentRepository.findAll().stream()
                .map(Enrollment::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(enrollmentFactory::createEnrollmentDto)
                .collect(Collectors.toList());
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving enrollment: " + e.getMessage());
        }
    }

    public EnrollmentDto updateEnrollment(EnrollmentDto enrollmentDto, UnitOfWork unitOfWork) {
        try {
            Integer id = enrollmentDto.getAggregateId();
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            if (enrollmentDto.getEnrollmentDate() != null) {
                newEnrollment.setEnrollmentDate(enrollmentDto.getEnrollmentDate());
            }
            newEnrollment.setActive(enrollmentDto.getActive());

            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);            EnrollmentUpdatedEvent event = new EnrollmentUpdatedEvent(newEnrollment.getAggregateId(), newEnrollment.getEnrollmentDate(), newEnrollment.getActive());
            event.setPublisherAggregateVersion(newEnrollment.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return enrollmentFactory.createEnrollmentDto(newEnrollment);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error updating enrollment: " + e.getMessage());
        }
    }

    public void deleteEnrollment(Integer id, UnitOfWork unitOfWork) {
        try {
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            newEnrollment.remove();
            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);            unitOfWorkService.registerEvent(new EnrollmentDeletedEvent(newEnrollment.getAggregateId()), unitOfWork);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error deleting enrollment: " + e.getMessage());
        }
    }

    public EnrollmentTeacherDto addEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto EnrollmentTeacherDto, UnitOfWork unitOfWork) {
        try {
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(enrollmentId, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            EnrollmentTeacher element = new EnrollmentTeacher(EnrollmentTeacherDto);
            newEnrollment.getTeachers().add(element);
            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);
            return EnrollmentTeacherDto;
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error adding EnrollmentTeacher: " + e.getMessage());
        }
    }

    public List<EnrollmentTeacherDto> addEnrollmentTeachers(Integer enrollmentId, List<EnrollmentTeacherDto> EnrollmentTeacherDtos, UnitOfWork unitOfWork) {
        try {
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(enrollmentId, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            EnrollmentTeacherDtos.forEach(dto -> {
                EnrollmentTeacher element = new EnrollmentTeacher(dto);
                newEnrollment.getTeachers().add(element);
            });
            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);
            return EnrollmentTeacherDtos;
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error adding EnrollmentTeachers: " + e.getMessage());
        }
    }

    public EnrollmentTeacherDto getEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, UnitOfWork unitOfWork) {
        try {
            Enrollment enrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(enrollmentId, unitOfWork);
            EnrollmentTeacher element = enrollment.getTeachers().stream()
                .filter(item -> item.getTeacherAggregateId() != null &&
                               item.getTeacherAggregateId().equals(teacherAggregateId))
                .findFirst()
                .orElseThrow(() -> new CrossrefsException("EnrollmentTeacher not found"));
            return element.buildDto();
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving EnrollmentTeacher: " + e.getMessage());
        }
    }

    public void removeEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, UnitOfWork unitOfWork) {
        try {
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(enrollmentId, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            newEnrollment.getTeachers().removeIf(item ->
                item.getTeacherAggregateId() != null &&
                item.getTeacherAggregateId().equals(teacherAggregateId)
            );
            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);
            EnrollmentTeacherRemovedEvent event = new EnrollmentTeacherRemovedEvent(enrollmentId, teacherAggregateId);
            event.setPublisherAggregateVersion(newEnrollment.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error removing EnrollmentTeacher: " + e.getMessage());
        }
    }

    public EnrollmentTeacherDto updateEnrollmentTeacher(Integer enrollmentId, Integer teacherAggregateId, EnrollmentTeacherDto EnrollmentTeacherDto, UnitOfWork unitOfWork) {
        try {
            Enrollment oldEnrollment = (Enrollment) unitOfWorkService.aggregateLoadAndRegisterRead(enrollmentId, unitOfWork);
            Enrollment newEnrollment = enrollmentFactory.createEnrollmentFromExisting(oldEnrollment);
            EnrollmentTeacher element = newEnrollment.getTeachers().stream()
                .filter(item -> item.getTeacherAggregateId() != null &&
                               item.getTeacherAggregateId().equals(teacherAggregateId))
                .findFirst()
                .orElseThrow(() -> new CrossrefsException("EnrollmentTeacher not found"));

            unitOfWorkService.registerChanged(newEnrollment, unitOfWork);
            EnrollmentTeacherUpdatedEvent event = new EnrollmentTeacherUpdatedEvent(enrollmentId, element.getTeacherAggregateId(), element.getTeacherVersion());
            event.setPublisherAggregateVersion(newEnrollment.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return element.buildDto();
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error updating EnrollmentTeacher: " + e.getMessage());
        }
    }






}