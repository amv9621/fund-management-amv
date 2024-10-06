package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.enums.SubscriptionStatusEnum;
import com.co.fundmanagement.enums.TransactionTypeEnum;
import com.co.fundmanagement.model.Fund;
import com.co.fundmanagement.model.Response;
import com.co.fundmanagement.model.Subscription;
import com.co.fundmanagement.model.Transaction;
import com.co.fundmanagement.repository.*;
import com.co.fundmanagement.service.ITransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
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

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    @Override
    public Mono<Response> createTransaction(RequestTransaction request) {
        return userRepository.findById(request.getUserId())
                .switchIfEmpty(Mono.error(new AccountNotFoundException("User not found")))
                .flatMap(user -> subscriptionRepository.findByUserIdAndFundId(request.getUserId(), request.getFundId())
                        .flatMap(subscription -> {
                            if (SubscriptionStatusEnum.ACTIVE.name().equals(subscription.getStatus()) &&
                                    !TransactionTypeEnum.CANCELLATION.name().equals(request.getTransactionType())) {
                                return Mono.error(new AccountNotFoundException("You have an ACTIVE Subscription"));
                            }
                            return Mono.just(subscription);
                        })
                        .switchIfEmpty(Mono.defer(() -> {
                            logger.info("No subscription found, creating a new one");
                            return Mono.just(new Subscription());
                        }))
                )
                .flatMap(subscription -> fundRepository.findById(request.getFundId())
                        .switchIfEmpty(Mono.error(new AccountNotFoundException("Fund not found")))
                        .flatMap(fund -> TransactionTypeEnum.OPENING.name().equals(request.getTransactionType()) ?
                                        Mono.just(fund)
                                                .filter(fundOpening -> request.getInitialValue() >= fundOpening.getMinInitialValue())
                                                .switchIfEmpty(Mono.error(new AccountNotFoundException("El valor ingresado no es mayor o igual al valor minimo de vinculacion")))
                                                .map(type -> Fund.builder()
                                                        .id(fund.getId())
                                                        .name(fund.getName())
                                                        .description(fund.getDescription())
                                                        .minInitialValue(fund.getMinInitialValue())
                                                        .categoryId(fund.getCategoryId())
                                                        .balance(fund.getBalance() + request.getInitialValue())
                                                        .build())
                                                .flatMap(fundSave -> fundRepository.save(fundSave))
                                                .map(type -> Subscription.builder()
                                                        .userId(request.getUserId())
                                                        .fundId(request.getFundId())
                                                        .status(SubscriptionStatusEnum.ACTIVE.name())
                                                        .openingDate(new Date())
                                                        .userId(request.getUserId())
                                                        .initialValue(request.getInitialValue())
                                                        .build())
                                                .flatMap(subscriptionSave -> subscriptionRepository.save(subscriptionSave))
                                                .map(subscriptionSaved -> Transaction.builder()
                                                        .date(subscriptionSaved.getOpeningDate())
                                                        .subscriptionId(subscriptionSaved.getId())
                                                        .userId(request.getUserId())
                                                        .transactionType(request.getTransactionType())
                                                        .build())
                                                .flatMap(transaction -> transactionRepository.save(transaction))
                                                .map(transaction -> Response.builder()
                                                        .code(200)
                                                        .message("Subscripcion iniciada con exito")
                                                        .body(transaction)
                                                        .build()) :
                                        TransactionTypeEnum.CANCELLATION.name().equals(request.getTransactionType()) ?
                                                Mono.just(Subscription.builder()
                                                                .id(subscription.getId())
                                                                .userId(subscription.getUserId())
                                                                .fundId(subscription.getFundId())
                                                                .initialValue(subscription.getInitialValue())
                                                                .openingDate(subscription.getOpeningDate())
                                                                .cancellationDate(new Date())
                                                                .status(SubscriptionStatusEnum.INACTIVE.name())
                                                                .build())
                                                        .flatMap(subscriptionSave -> subscriptionRepository.save(subscriptionSave)
                                                                .map(subscriptionSaved -> Fund.builder()
                                                                        .id(fund.getId())
                                                                        .name(fund.getName())
                                                                        .categoryId(fund.getCategoryId())
                                                                        .minInitialValue(fund.getMinInitialValue())
                                                                        .description(fund.getDescription())
                                                                        .balance(fund.getBalance() - subscriptionSaved.getInitialValue())
                                                                        .build())
                                                                .flatMap(updateFund -> fundRepository.save(updateFund))
                                                                .map(transaction -> Transaction.builder()
                                                                        .date(new Date())
                                                                        .userId(request.getUserId())
                                                                        .subscriptionId(subscription.getId())
                                                                        .transactionType(request.getTransactionType())
                                                                        .build())
                                                                .flatMap(transaction -> transactionRepository.save(transaction)))
                                                        .map(transaction -> Response.builder()
                                                                .code(200)
                                                                .message("Subscripcion cancelada con exito - El valor inicial de la vinculacion fue devuelto")
                                                                .body(transaction)
                                                                .build()) :

                                                Mono.error(new AccountNotFoundException("Validate subscriptionId - TransactionType not found"))
                        ));
    }

    @Override
    public Flux<Transaction> findAllByUserId(String userId) {
        return transactionRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("userId not found")));
    }
}
