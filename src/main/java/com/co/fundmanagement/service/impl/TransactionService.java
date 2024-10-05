package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.enums.SubscriptionStatusEnum;
import com.co.fundmanagement.model.Fund;
import com.co.fundmanagement.model.Response;
import com.co.fundmanagement.model.Subscription;
import com.co.fundmanagement.model.Transaction;
import com.co.fundmanagement.repository.*;
import com.co.fundmanagement.service.ITransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import javax.security.auth.login.AccountNotFoundException;
import java.util.Date;

@Service
public class TransactionService implements ITransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private TransactionTypeRepository transactionTypeRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Mono<Response> createTransaction(RequestTransaction request) {
        return userRepository.findById(request.getUserId())
                .switchIfEmpty(Mono.error(new AccountNotFoundException("User not found")))
                .flatMap(subscription -> fundRepository.findById(request.getFundId())
                        .switchIfEmpty(Mono.error(new AccountNotFoundException("Fund not found")))
                        .filter(fund -> request.getInitialValue() >= fund.getMinInitialValue())
                        .switchIfEmpty(Mono.error(new AccountNotFoundException("El valor ingresado no es mayor o igual al valor minimo de vinculacion"))))
                .flatMap(fund -> transactionTypeRepository.findById(request.getTransactionType())
                        .switchIfEmpty(Mono.error(new AccountNotFoundException("TransactionType not found")))
                        .flatMap(transactionType -> "OPENING".equals(transactionType.getName()) ?
                                Mono.just(transactionType)
                                        .map(type -> Subscription.builder()
                                                .userId(request.getUserId())
                                                .fundId(request.getFundId())
                                                .status(SubscriptionStatusEnum.ACTIVE.name())
                                                .openingDate(new Date())
                                                .initialValue(request.getInitialValue())
                                                .build())
                                        .flatMap(subscription -> subscriptionRepository.save(subscription))
                                        .map(subscription -> Transaction.builder()
                                                .date(subscription.getOpeningDate())
                                                .subscriptionId(subscription.getId())
                                                .transactionTypeId(request.getTransactionType())
                                                .build())
                                        .flatMap(transaction -> transactionRepository.save(transaction))
                                        .map(transaction -> Response.builder()
                                                .code(200)
                                                .message("Subscripcion iniciada con exito")
                                                .body(transaction)
                                                .build()):

                                subscriptionRepository.findById(request.getSubscriptionId())
                                        .switchIfEmpty(Mono.error(new AccountNotFoundException("Subscription not found")))
                                        .map(subscription -> Subscription.builder()
                                                .id(subscription.getId())
                                                .cancellationDate(new Date())
                                                .status(SubscriptionStatusEnum.INACTIVE.name())
                                                .build())
                                        .flatMap(subscription -> subscriptionRepository.save(subscription)
                                                .map(subscriptionSaved -> Fund.builder()
                                                        .id(fund.getId())
                                                        .balance(fund.getBalance() - subscriptionSaved.getInitialValue())
                                                        .build())
                                                .flatMap(updateFund -> fundRepository.save(updateFund))
                                                .map(transaction -> Transaction.builder()
                                                        .date(new Date())
                                                        .subscriptionId(subscription.getId())
                                                        .transactionTypeId(request.getTransactionType())
                                                        .build()))
                                        .map(transaction -> Response.builder()
                                                .code(200)
                                                .message("Subscripcion cancelada con exito")
                                                .body(transaction)
                                                .build())
                        ))
                ;
    }
}
