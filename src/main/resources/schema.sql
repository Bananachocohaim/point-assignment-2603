-- ==========================================================
-- [공통 코드 및 ID 규칙 가이드]
-- 1. ID PREFIX    : WLT(지갑), ERN(적립), USG(사용/취소)
-- 2. ID FORMAT    : {PREFIX}-{TsID} (예: WLT-01JK2...)
-- 3. EARN_TYPE    : MANUAL(수기) ORDER(주문적립), EVENT(이벤트), RE_EARN(취소재적립)
-- 4. EARN_STATUS  : ACTIVE(가용), EXPIRED(만료), CANCELLED(적립취소)
-- 5. USAGE_TYPE   : USE(사용), PARTIAL_CANCEL(부분취소), FULL_CANCEL(전체취소), EXPIRE(만료차감)
-- 6. USAGE_STATUS : COMPLETED(완료), PARTIAL(부분취소), FULL(전체취소)
-- 7. WALLET_TYPE  : FREE(무료),  CASH(충전)
-- ==========================================================

-- 1. 사용자 포인트 지갑 (사용자별 최종 잔액 관리)
CREATE TABLE IF NOT EXISTS user_point_wallet (
    wallet_id    VARCHAR(50) NOT NULL COMMENT '지갑 식별자 (WLT-...)',
    user_id                 VARCHAR(50) NOT NULL COMMENT '사용자 식별자 (고객ID)',
    wallet_type             VARCHAR(20) NOT NULL DEFAULT 'BASIC' COMMENT '지갑 유형 (FREE (무료), CASH (충전))',
    balance                 BIGINT      NOT NULL DEFAULT 0 COMMENT '현재 포인트 잔액',
    max_balance_limit       BIGINT      NOT NULL DEFAULT 100000 COMMENT '최대 보유 한도 금액',
    next_expiration_date    DATE                 COMMENT '가장 빠른 만료 예정일',
    expiring_amount         BIGINT      NOT NULL DEFAULT 0 COMMENT '해당 예정일에 만료될 금액',
    expiration_updated_at   TIMESTAMP            COMMENT '만료 정보 갱신 일시',
    
    -- 감사(Audit) 컬럼
    created_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성자 ID',
    updated_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정자 ID',
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    
    PRIMARY KEY (wallet_id),
	CONSTRAINT uk_user_wallet_type UNIQUE (user_id, wallet_type)
) COMMENT '사용자 포인트 지갑';


-- 2. 포인트 적립 기록 (적립 원장 - FIFO 차감의 기준)
CREATE TABLE IF NOT EXISTS point_earn_record (
    wallet_id   VARCHAR(50) NOT NULL COMMENT '지갑 식별자 (FK: WLT-...)',
    earn_id                VARCHAR(50) NOT NULL COMMENT '적립 식별자 (ERN-...)',
    created_date           DATE        NOT NULL COMMENT '적립 일자 (파티션 키)',
    earn_type              VARCHAR(20) NOT NULL COMMENT '적립 사유 (ORDER, EVENT, RE_EARN)',
    order_no               VARCHAR(50)          COMMENT '연계 주문번호',
    original_amount        BIGINT      NOT NULL COMMENT '최초 적립 금액',
    remaining_amount       BIGINT      NOT NULL COMMENT '현재 사용 가능 잔액',
    expiry_days            INT         NOT NULL DEFAULT 365 COMMENT '만료 기준 일수 (적립일 기준 만료까지 일수)',
    expiration_date        DATE        NOT NULL COMMENT '만료일 (created_date + expiry_days)',
    earn_status            VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '적립 상태 (ACTIVE, EXPIRED, CANCELLED)',
    original_earn_id       VARCHAR(50)          DEFAULT NULL COMMENT '만료 후 재적립 시 원본 적립 ID 참조',
    
    -- 감사(Audit) 컬럼
    created_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성자 ID',
    updated_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정자 ID',
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    
    PRIMARY KEY (wallet_id, earn_id, created_date)
) COMMENT '포인트 적립 이력';


-- 3. 포인트 사용/취소 기록 (사용 이력 기록)
CREATE TABLE IF NOT EXISTS point_usage_record (
    wallet_id   VARCHAR(50) NOT NULL COMMENT '지갑 식별자 (FK: WLT-...)',
    usage_id               VARCHAR(50) NOT NULL COMMENT '사용/취소 식별자 (USG-...)',
    created_date           DATE        NOT NULL COMMENT '처리 일자 (파티션 키)',
    usage_type             VARCHAR(20) NOT NULL COMMENT '행위 타입 (USE, CANCEL, EXPIRE)',
    order_no               VARCHAR(50) NOT NULL COMMENT '주문번호 (Search Key)',
    used_amount            BIGINT      NOT NULL COMMENT '사용 금액 (취소 시 취소 금액)',
    original_usage_id      VARCHAR(50)          DEFAULT NULL COMMENT '취소 시 원거래 usage_id 참조',
    usage_status           VARCHAR(20)          DEFAULT 'COMPLETED' COMMENT '거래 상태 (COMPLETED: 정상완료, PARTIAL_CANCEL: 부분취소됨, FULL_CANCEL: 전체취소됨)',
    
    -- 감사(Audit) 컬럼
    created_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성자 ID',
    updated_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정자 ID',
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    
    PRIMARY KEY (wallet_id, usage_id, created_date)
) COMMENT '포인트 사용 및 취소 이력';


-- 4. 포인트 사용-적립 상세 매핑
CREATE TABLE IF NOT EXISTS point_usage_earn_mapping (
    wallet_id   VARCHAR(50) NOT NULL COMMENT '지갑 식별자 (FK: WLT-...)',
    usage_id               VARCHAR(50) NOT NULL COMMENT '사용/취소 ID (USG-...)',
    usage_created_date     DATE        NOT NULL COMMENT '매핑 처리일 (파티션 키)',
    earn_id                VARCHAR(50) NOT NULL COMMENT '연결된 적립 원장 ID (ERN-...)',
    earn_created_date      DATE        NOT NULL COMMENT '적립 원장의 생성일 (파티션 프루닝 보조용)',
    amount                 BIGINT      NOT NULL COMMENT '처리 금액 (사용 시 +, 취소 시 -)',
    usage_type             VARCHAR(20) NOT NULL DEFAULT 'USE' COMMENT '상위 usage_record의 타입을 따름',
    
    -- 감사(Audit) 컬럼
    created_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '생성자 ID',
    updated_by             VARCHAR(50) NOT NULL DEFAULT 'SYSTEM' COMMENT '수정자 ID',
    created_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '생성 일시',
    updated_at             TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',
    
    PRIMARY KEY (wallet_id, usage_id, usage_created_date, earn_id)
) COMMENT '사용 건과 적립 건의 매핑 상세 이력';


-- 5. 포인트 정책 (KEY-VALUE 구조)
CREATE TABLE IF NOT EXISTS point_policy (
    policy_key   VARCHAR(100) NOT NULL COMMENT '정책 키 (고유 식별자)',
    policy_value VARCHAR(500) NOT NULL COMMENT '정책 값',
    description  VARCHAR(500)         COMMENT '정책 설명',
    updated_by   VARCHAR(50)  NOT NULL DEFAULT 'SYSTEM' COMMENT '수정자',
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '수정 일시',

    PRIMARY KEY (policy_key)
) COMMENT '포인트 정책 테이블';


-- 6. ID 생성용 DB 시퀀스 (0000~9999 사이클, 종류별 독립)
CREATE SEQUENCE IF NOT EXISTS seq_wlt START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE;
CREATE SEQUENCE IF NOT EXISTS seq_ern START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE;
CREATE SEQUENCE IF NOT EXISTS seq_usg START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE;
CREATE SEQUENCE IF NOT EXISTS seq_cnc START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE;
CREATE SEQUENCE IF NOT EXISTS seq_pcn START WITH 0 INCREMENT BY 1 MINVALUE 0 MAXVALUE 9999 CYCLE;