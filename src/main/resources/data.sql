-- 고객 지갑 초기 데이터
INSERT INTO user_point_wallet (
    wallet_id,
    user_id,
    wallet_type,
    balance,
    max_balance_limit,
    next_expiration_date,
    expiring_amount,
    expiration_updated_at,
    created_by,
    updated_by,
    created_at,
    updated_at
) VALUES (
    'WLT-001',
    'testId01',
    'FREE',
    0,
    100000,
    NULL,
    0,
    NULL,
    'SYSTEM',
    'SYSTEM',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 포인트 정책 초기 데이터
INSERT INTO point_policy (policy_key, policy_value, description, updated_by, updated_at) VALUES
('MAX_EARN_AMOUNT_PER_TX', '100000', '1회 최대 적립 가능 금액 (원)', 'SYSTEM', CURRENT_TIMESTAMP),
('RE_EARN_EXPIRY_DAYS', '3', '취소 재적립 만료일수 (일)', 'SYSTEM', CURRENT_TIMESTAMP);