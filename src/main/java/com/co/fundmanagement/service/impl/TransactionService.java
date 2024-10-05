package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.model.Response;
import com.co.fundmanagement.service.ITransactionService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class TransactionService implements ITransactionService {
    @Override
    public Mono<Response> createTransaction(RequestTransaction requestTransaction) {
        return null;
    }
}
