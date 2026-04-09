package com.example.dummyapp.shared.service;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public interface AmbiguousServiceApi {
    Object doSomething(Integer id, UnitOfWork unitOfWork);
}
