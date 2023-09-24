CREATE TABLE IF NOT EXISTS currencies (
    id              BIGINT                      NOT NULL    AUTO_INCREMENT,
    code            VARCHAR(64)                 NOT NULL    UNIQUE,
    name 	        VARCHAR(64)                 NOT NULL,
    created_date    TIMESTAMP WITH TIME ZONE    NOT NULL,
    last_modified   TIMESTAMP WITH TIME ZONE    NOT NULL,
    version         INT                         NOT NULL,
    PRIMARY KEY (id)
);