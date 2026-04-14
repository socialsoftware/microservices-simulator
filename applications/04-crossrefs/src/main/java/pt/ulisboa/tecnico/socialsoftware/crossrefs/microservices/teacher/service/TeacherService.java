package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.TeacherDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.TeacherUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.events.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.exception.CrossrefsException;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.webapi.requestDtos.CreateTeacherRequestDto;


@Service
@Transactional(noRollbackFor = CrossrefsException.class)
public class TeacherService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private TeacherRepository teacherRepository;

    @Autowired
    private TeacherFactory teacherFactory;

    @Autowired
    private TeacherServiceExtension extension;

    public TeacherService() {}

    public TeacherDto createTeacher(CreateTeacherRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TeacherDto teacherDto = new TeacherDto();
            teacherDto.setName(createRequest.getName());
            teacherDto.setEmail(createRequest.getEmail());
            teacherDto.setDepartment(createRequest.getDepartment());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Teacher teacher = teacherFactory.createTeacher(aggregateId, teacherDto);
            unitOfWorkService.registerChanged(teacher, unitOfWork);
            return teacherFactory.createTeacherDto(teacher);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error creating teacher: " + e.getMessage());
        }
    }

    public TeacherDto getTeacherById(Integer id, UnitOfWork unitOfWork) {
        try {
            Teacher teacher = (Teacher) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return teacherFactory.createTeacherDto(teacher);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving teacher: " + e.getMessage());
        }
    }

    public List<TeacherDto> getAllTeachers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = teacherRepository.findAll().stream()
                .map(Teacher::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Teacher) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(teacherFactory::createTeacherDto)
                .collect(Collectors.toList());
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error retrieving teacher: " + e.getMessage());
        }
    }

    public TeacherDto updateTeacher(TeacherDto teacherDto, UnitOfWork unitOfWork) {
        try {
            Integer id = teacherDto.getAggregateId();
            Teacher oldTeacher = (Teacher) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Teacher newTeacher = teacherFactory.createTeacherFromExisting(oldTeacher);
            if (teacherDto.getName() != null) {
                newTeacher.setName(teacherDto.getName());
            }
            if (teacherDto.getEmail() != null) {
                newTeacher.setEmail(teacherDto.getEmail());
            }
            if (teacherDto.getDepartment() != null) {
                newTeacher.setDepartment(teacherDto.getDepartment());
            }

            unitOfWorkService.registerChanged(newTeacher, unitOfWork);            TeacherUpdatedEvent event = new TeacherUpdatedEvent(newTeacher.getAggregateId(), newTeacher.getName(), newTeacher.getEmail(), newTeacher.getDepartment());
            event.setPublisherAggregateVersion(newTeacher.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return teacherFactory.createTeacherDto(newTeacher);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error updating teacher: " + e.getMessage());
        }
    }

    public void deleteTeacher(Integer id, UnitOfWork unitOfWork) {
        try {
            Teacher oldTeacher = (Teacher) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Teacher newTeacher = teacherFactory.createTeacherFromExisting(oldTeacher);
            newTeacher.remove();
            unitOfWorkService.registerChanged(newTeacher, unitOfWork);            unitOfWorkService.registerEvent(new TeacherDeletedEvent(newTeacher.getAggregateId()), unitOfWork);
        } catch (CrossrefsException e) {
            throw e;
        } catch (Exception e) {
            throw new CrossrefsException("Error deleting teacher: " + e.getMessage());
        }
    }








}