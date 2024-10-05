package com.co.fundmanagement.repository;

import com.co.fundmanagement.model.TransactionType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface TransactionTypeRepository extends ReactiveMongoRepository<TransactionType, String> {
}
