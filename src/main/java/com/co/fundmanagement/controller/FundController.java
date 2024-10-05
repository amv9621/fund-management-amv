package com.co.fundmanagement.controller;

import com.co.fundmanagement.model.Fund;
import com.co.fundmanagement.service.impl.FundService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/fund")
public class FundController {

    @Autowired
    private FundService fundService;

    @GetMapping("/all")
    public Flux<Fund> findAll(){
        return fundService.findAll();
    }
}
