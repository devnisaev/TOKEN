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
CREATE DATABASE reporting_service;
CREATE DATABASE settlement_service;
CREATE DATABASE valuation_service;
CREATE DATABASE audit_ledger_service;
CREATE DATABASE corporate_actions_service;
