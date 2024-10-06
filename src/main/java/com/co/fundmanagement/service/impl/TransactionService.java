package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.enums.SubscriptionStatusEnum;
import com.co.fundmanagement.enums.TransactionTypeEnum;
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

import static com.co.fundmanagement.factory.FactoryObject.*;

@Service
public class TransactionService implements ITransactionService {
    @Autowired
    private TransactionRepository transactionRepository;

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
                .flatMap(user -> subscriptionRepository.findByUserIdAndFundIdAndStatus(request.getUserId(), request.getFundId(), SubscriptionStatusEnum.ACTIVE.name())
                        .flatMap(subscription -> {
                            if (!TransactionTypeEnum.CANCELLATION.name().equals(request.getTransactionType())) {
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
                                                .map(fundSave -> createFund(fundSave, fund.getBalance() + request.getInitialValue()))
                                                .flatMap(fundSave -> fundRepository.save(fundSave))
                                                .map(fundSaved -> createSubscription(request))
                                                .flatMap(subscriptionSave -> subscriptionRepository.save(subscriptionSave))
                                                .map(subscriptionSaved -> createTransactionObject(subscriptionSaved, request))
                                                .flatMap(transaction -> transactionRepository.save(transaction))
                                                .map(transaction -> createResponse(transaction, "Subscripcion iniciada con exito")) :
                                        TransactionTypeEnum.CANCELLATION.name().equals(request.getTransactionType()) ?
                                                Mono.just(updateSubscription(subscription))
                                                        .flatMap(subscriptionSave -> subscriptionRepository.save(subscriptionSave)
                                                                .map(subscriptionSaved -> createFund(fund, fund.getBalance() - subscriptionSaved.getInitialValue()))
                                                                .flatMap(updateFund -> fundRepository.save(updateFund))
                                                                .map(transaction -> createTransactionObject(subscription, request))
                                                                .flatMap(transaction -> transactionRepository.save(transaction)))
                                                        .map(transaction -> createResponse(transaction, "Subscripcion cancelada con exito - El valor inicial de la vinculacion fue devuelto")) :

                                                Mono.error(new AccountNotFoundException("Validate subscriptionId - TransactionType not found"))
                        ));
    }

    @Override
    public Flux<Transaction> findAllByUserId(String userId) {
        return transactionRepository.findByUserId(userId)
                .switchIfEmpty(Mono.error(new AccountNotFoundException("userId not found")));
    }
}
