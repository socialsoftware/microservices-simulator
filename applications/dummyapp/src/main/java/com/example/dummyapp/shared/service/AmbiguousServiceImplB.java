package com.example.dummyapp.shared.service;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWorkService;

@Service
public class AmbiguousServiceImplB implements AmbiguousServiceApi {

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public AmbiguousServiceImplB(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    @Override
    public Object doSomething(Integer id, UnitOfWork unitOfWork) {
        return unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
    }
}
