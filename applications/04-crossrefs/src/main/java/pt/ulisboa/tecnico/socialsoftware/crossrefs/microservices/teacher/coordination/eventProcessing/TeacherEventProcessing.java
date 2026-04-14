package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;

@Service
public class TeacherEventProcessing {
    @Autowired
    private TeacherService teacherService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TeacherEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}