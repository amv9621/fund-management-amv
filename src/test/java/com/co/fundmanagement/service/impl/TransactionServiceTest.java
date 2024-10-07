package com.co.fundmanagement.service.impl;

import com.co.fundmanagement.dto.RequestTransaction;
import com.co.fundmanagement.enums.SubscriptionStatusEnum;
import com.co.fundmanagement.exception.TransactionException;
import com.co.fundmanagement.model.*;
import com.co.fundmanagement.repository.FundRepository;
import com.co.fundmanagement.repository.SubscriptionRepository;
import com.co.fundmanagement.repository.TransactionRepository;
import com.co.fundmanagement.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private FundRepository fundRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testCreateTransaction_UserNotFound() {
        RequestTransaction request = RequestTransaction.builder()
                .userId(UUID.randomUUID().toString())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.empty());

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(404))
                .verifyComplete();
    }

    @Test
    public void testCreateTransaction_FundNotFound() {
        RequestTransaction request = RequestTransaction.builder()
                .userId(UUID.randomUUID().toString())
                .build();

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));
        when(fundRepository.findById(request.getFundId())).thenReturn(Mono.empty());

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(404))
                .verifyComplete();
    }

    @Test
    public void testFindAllByUserId_NoTransactionsFound() {
        String userId = "user-id-no-transactions";

        when(transactionRepository.findByUserId(userId)).thenReturn(Flux.empty());

        Flux<Transaction> result = transactionService.findAllByUserId(userId);

        StepVerifier.create(result)
                .expectError(TransactionException.class)
                .verify();
    }

    @Test
    public void testCreateTransaction_SuccessfulOpening() {
        RequestTransaction request = RequestTransaction.builder()
                .userId("user1")
                .fundId("fund1")
                .transactionType("OPENING")
                .initialValue(1000.0)
                .build();

        Fund fund = Fund.builder()
                .id(request.getFundId())
                .minInitialValue(500.0)
                .balance(50000.0)
                .name("name fund")
                .build();

        User user = User.builder()
                .id("fundId")
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));
        when(subscriptionRepository.findByUserIdAndFundIdAndStatus(request.getUserId(), request.getFundId(), "ACTIVE"))
                .thenReturn(Mono.empty());
        when(fundRepository.findById(request.getFundId())).thenReturn(Mono.just(fund));
        when(fundRepository.save(any(Fund.class))).thenReturn(Mono.just(fund));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(Mono.just(new Subscription()));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(new Transaction()));

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(200))
                .verifyComplete();
    }

    @Test
    public void testCreateTransaction_SuccessfulCancellation() {
        RequestTransaction request = RequestTransaction.builder()
                .userId("user1")
                .fundId("fund1")
                .transactionType("CANCELLATION")
                .subscriptionId("subscriptionId")
                .build();

        Fund fund = Fund.builder()
                .id(request.getFundId())
                .minInitialValue(500.0)
                .balance(50000.0)
                .name("name fund")
                .build();

        User user = User.builder()
                .id("user1")
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        Subscription subscription = Subscription.builder()
                .id(request.getSubscriptionId())
                .initialValue(500000.0)
                .status(SubscriptionStatusEnum.ACTIVE.name())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));
        when(subscriptionRepository.findByIdAndUserId(request.getSubscriptionId(), user.getId()))
                .thenReturn(Mono.just(subscription));
        when(fundRepository.findById(request.getFundId())).thenReturn(Mono.just(fund));
        when(subscriptionRepository.save(any(Subscription.class))).thenReturn(Mono.just(subscription));
        when(fundRepository.save(any(Fund.class))).thenReturn(Mono.just(fund));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(Mono.just(new Transaction()));

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(200))
                .verifyComplete();
    }

    @Test
    public void subscriptionException_Active_Subscription() {
        RequestTransaction request = RequestTransaction.builder()
                .userId("user1")
                .fundId("fund1")
                .transactionType("OPENING")
                .initialValue(1000.0)
                .build();

        User user = User.builder()
                .id("fundId")
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        Subscription subscription = Subscription.builder()
                .id(request.getSubscriptionId())
                .initialValue(500000.0)
                .status(SubscriptionStatusEnum.ACTIVE.name())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));
        when(subscriptionRepository.findByUserIdAndFundIdAndStatus(request.getUserId(), request.getFundId(), "ACTIVE"))
                .thenReturn(Mono.just(subscription));

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(404))
                .verifyComplete();
    }

    @Test
    public void validateArgumentsException() {
        RequestTransaction request = RequestTransaction.builder()
                .userId("user1")
                .fundId("fund1")
                .transactionType("NN")
                .build();

        User user = User.builder()
                .id("fundId")
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(404))
                .verifyComplete();
    }

    @Test
    public void validateSubscriptionException() {
        RequestTransaction request = RequestTransaction.builder()
                .userId("user1")
                .fundId("fund1")
                .transactionType("CANCELLATION")
                .build();

        User user = User.builder()
                .id("fundId")
                .fullName("AMV")
                .customerId(UUID.randomUUID().toString())
                .build();

        when(userRepository.findById(request.getUserId())).thenReturn(Mono.just(user));

        Mono<Response> result = transactionService.createTransaction(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getCode().equals(404))
                .verifyComplete();
    }
}
