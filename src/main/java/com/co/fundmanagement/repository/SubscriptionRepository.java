package com.co.fundmanagement.repository;

import com.co.fundmanagement.model.Subscription;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SubscriptionRepository extends ReactiveMongoRepository<Subscription, String> {
}
