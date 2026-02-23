package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.service.CategoryService;

@Service
public class CategoryEventProcessing {
    @Autowired
    private CategoryService categoryService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public CategoryEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}