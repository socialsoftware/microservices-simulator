package com.example.dummyapp.shared.service;

import com.example.dummyapp.shared.fake.OnlyLooksLikeUnitOfWorkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SubstringTrapService {

    @Autowired
    private OnlyLooksLikeUnitOfWorkService onlyLooksLikeUnitOfWorkService;

    public String ping() {
        return onlyLooksLikeUnitOfWorkService.getClass().getSimpleName();
    }
}
