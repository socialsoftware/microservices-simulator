package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;

@Service
public class BookEventProcessing {
    @Autowired
    private BookService bookService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public BookEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}