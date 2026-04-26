package com.example.dummyapp.shared.service;

import pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork.UnitOfWork;

public interface InterfaceOnlyServiceApi {
    Object loadInterfaceOnly(Integer id, UnitOfWork unitOfWork);
}
