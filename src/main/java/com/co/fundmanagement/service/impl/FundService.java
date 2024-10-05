package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.model.Fund;
import com.co.fundmanagement.repository.FundRepository;
import com.co.fundmanagement.service.IFundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class FundService implements IFundService {
    @Autowired
    private FundRepository fundRepository;

    @Override
    public Flux<Fund> findAll() {
        return fundRepository.findAll();
    }
}
