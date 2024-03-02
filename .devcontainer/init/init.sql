-- Create databases
CREATE DATABASE IF NOT EXISTS orchestrator;

CREATE DATABASE IF NOT EXISTS workflow;

-- Create users and grant rights
CREATE USER 'orchestrator'@'%' IDENTIFIED BY 'root';

CREATE USER 'workflow'@'%' IDENTIFIED BY 'root';

GRANT ALL PRIVILEGES ON orchestrator.* TO 'orchestrator'@'%';
GRANT XA_RECOVER_ADMIN ON *.* TO 'orchestrator'@'%';

GRANT ALL PRIVILEGES ON workflow.* TO 'workflow'@'%';
GRANT XA_RECOVER_ADMIN ON *.* TO 'workflow'@'%';
