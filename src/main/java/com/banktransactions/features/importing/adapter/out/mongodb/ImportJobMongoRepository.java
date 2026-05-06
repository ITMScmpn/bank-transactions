package com.banktransactions.features.importing.adapter.out.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface ImportJobMongoRepository extends MongoRepository<ImportJobDocument, String> {
}
