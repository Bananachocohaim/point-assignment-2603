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
    'user_001',
    'FREE',
    11500,
    100000,
    NULL,
    0,
    NULL,
    'SYSTEM',
    'SYSTEM',
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
);

-- 포인트 적립 기록 테스트 데이터 (만료일 갱신 테스트용, WLT-001 지갑)
-- 가장 빠른 만료일: 2026-03-20 (3건 합계 4000), 그 다음 2026-03-25 (2건 1500), 2026-04-01 (2건 2000) 등
INSERT INTO point_earn_record (wallet_id, earn_id, order_no, created_date, earn_type, original_amount, remaining_amount, expiration_date, earn_status, original_earn_id, created_by, updated_by, created_at, updated_at) VALUES
('WLT-001', 'ERN-001', 'ORD-001', '2026-03-01', 'ORDER', 1000, 1000, '2026-03-20', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-002', 'ORD-002', '2026-03-02', 'ORDER', 1500, 1500, '2026-03-20', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-003', 'ORD-003', '2026-03-03', 'EVENT', 1500, 1500, '2026-03-20', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-004', 'ORD-004', '2026-03-04', 'ORDER', 500, 500, '2026-03-25', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-005', 'ORD-005', '2026-03-05', 'EVENT', 1000, 1000, '2026-03-25', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-006', 'ORD-006', '2026-03-06', 'ORDER', 1000, 1000, '2026-04-01', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-007', 'ORD-007', '2026-03-07', 'ORDER', 1000, 1000, '2026-04-01', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-008', 'ORD-008', '2026-03-08', 'EVENT', 2000, 2000, '2026-04-10', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-009', 'ORD-009', '2026-03-09', 'ORDER', 500, 500, '2026-04-15', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-010', 'ORD-010', '2026-03-10', 'RE_EARN', 300, 300, '2026-03-11', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


INSERT INTO point_earn_record (wallet_id, earn_id, order_no, created_date, earn_type, original_amount, remaining_amount, expiration_date, earn_status, original_earn_id, created_by, updated_by, created_at, updated_at) VALUES
('WLT-001', 'ERN-011', 'ORD-011', '2026-03-10', 'RE_EARN', 300, 300, '2026-03-11', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-012', 'ORD-012', '2026-03-10', 'RE_EARN', 300, 300, '2026-03-11', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-013', 'ORD-013', '2026-03-10', 'RE_EARN', 300, 300, '2026-03-11', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('WLT-001', 'ERN-014', 'ORD-014', '2026-03-10', 'RE_EARN', 300, 300, '2026-03-11', 'ACTIVE', NULL, 'SYSTEM', 'SYSTEM', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);