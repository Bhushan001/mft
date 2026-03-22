-- Chrono Platform — PostgreSQL schema initialisation
-- All services share a single database instance with isolated schemas

CREATE SCHEMA IF NOT EXISTS chrono_auth;
CREATE SCHEMA IF NOT EXISTS chrono_users;
CREATE SCHEMA IF NOT EXISTS chrono_tenants;
CREATE SCHEMA IF NOT EXISTS chrono_mapper;
CREATE SCHEMA IF NOT EXISTS chrono_engine;
CREATE SCHEMA IF NOT EXISTS chrono_orchestrator;
CREATE SCHEMA IF NOT EXISTS chrono_etl;
CREATE SCHEMA IF NOT EXISTS chrono_audit;

-- Grant full access on each schema to the application user
-- (postgres user owns all schemas by default in dev)
GRANT ALL PRIVILEGES ON SCHEMA chrono_auth        TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_users       TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_tenants     TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_mapper      TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_engine      TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_orchestrator TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_etl         TO postgres;
GRANT ALL PRIVILEGES ON SCHEMA chrono_audit       TO postgres;
