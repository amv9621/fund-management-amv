package com.co.fundmanagement.service;

import com.co.fundmanagement.model.Fund;
import reactor.core.publisher.Flux;

public interface IFundService {

    Flux<Fund> findAll();
}
