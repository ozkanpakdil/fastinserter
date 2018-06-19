DROP TABLE event IF EXISTS;

CREATE TABLE IF NOT EXISTS event (
    id VARCHAR(50) ,
    state VARCHAR(50),
    timestamp BIGINT,
    type VARCHAR(50),
    host VARCHAR(50),
    alert BOOLEAN
);
