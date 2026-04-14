package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;

@Service
public class AuthorEventProcessing {
    @Autowired
    private AuthorService authorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public AuthorEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}