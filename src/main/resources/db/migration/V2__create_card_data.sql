-- Flyway migration V2__create_card_data.sql
-- Creates the card_data table corresponding to CVACT02Y copybook / CARDFILE VSAM KSDS
-- Used by CBACT02C batch job (card file sequential read/update)

CREATE TABLE IF NOT EXISTS card_data (
    card_num              CHAR(16)     NOT NULL,          -- CARD-NUM          PIC X(16)  (KSDS primary key)
    card_acct_id          BIGINT       NOT NULL,          -- CARD-ACCT-ID      PIC 9(11)
    card_cvv_cd           SMALLINT,                       -- CARD-CVV-CD       PIC 9(03)
    card_embossed_name    VARCHAR(50),                    -- CARD-EMBOSSED-NAME PIC X(50)
    card_expiration_date  VARCHAR(10),                    -- CARD-EXPIRAION-DATE PIC X(10)
    card_active_status    CHAR(1)      DEFAULT 'N',       -- CARD-ACTIVE-STATUS PIC X(01)
    CONSTRAINT pk_card_data PRIMARY KEY (card_num),
    CONSTRAINT chk_card_active_status CHECK (card_active_status IN ('Y', 'N'))
);

CREATE INDEX IF NOT EXISTS idx_card_data_acct_id
    ON card_data (card_acct_id);
