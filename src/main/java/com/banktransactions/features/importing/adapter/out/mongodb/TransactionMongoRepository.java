package com.banktransactions.features.importing.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface TransactionMongoRepository extends MongoRepository<TransactionDocument, String> {
}
