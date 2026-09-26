-- One database per microservice (property_registry comes from POSTGRES_DB).

CREATE DATABASE auth_service;
CREATE DATABASE token_issuance;
CREATE DATABASE marketplace_service;
CREATE DATABASE payment_service;
CREATE DATABASE rental_service;
CREATE DATABASE compliance_service;
CREATE DATABASE document_service;
CREATE DATABASE notification_service;
CREATE DATABASE wallet_service;
CREATE DATABASE blockchain_indexer;
