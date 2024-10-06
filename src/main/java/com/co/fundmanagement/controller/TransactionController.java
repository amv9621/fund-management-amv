package com.co.fundmanagement.controller;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.model.Response;
import com.co.fundmanagement.model.Transaction;
import com.co.fundmanagement.service.impl.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/transaction")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/create")
    public Mono<Response> createTransaction(@RequestBody RequestTransaction requestTransaction){
        return transactionService.createTransaction(requestTransaction);
    }

    @GetMapping("/allByUserId")
    public Flux<Transaction> findAllByUserId(@RequestParam String userId){
        return transactionService.findAllByUserId(userId);
    }
}
