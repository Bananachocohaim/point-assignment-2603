package io.github.bananachocohaim.pointassignment2603;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class PointConcurrencyIntegrityTest {

    private static final Logger log = LoggerFactory.getLogger(PointConcurrencyIntegrityTest.class);
    private static final String WALLET_ID = "WLT-CONC-TEST-001";
    private static final String USER_ID = "concurrency-user";
    private static final int THREAD_COUNT = 10;
    private static final long AMOUNT = 230L;
    private static final long PARTIAL_CANCEL_AMOUNT = 115L;
    private static final int LOG_BODY_MAX = 500;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 테스트 간 데이터 간섭 제거
        jdbcTemplate.update("DELETE FROM point_usage_earn_mapping");
        jdbcTemplate.update("DELETE FROM point_usage_record");
        jdbcTemplate.update("DELETE FROM point_earn_record");
        jdbcTemplate.update("DELETE FROM user_point_wallet");

        // 동시성 테스트용 지갑 생성 (충분한 한도로 셋업)
        jdbcTemplate.update("""
            INSERT INTO user_point_wallet (
                wallet_id, user_id, wallet_type, balance, max_balance_limit,
                next_expiration_date, expiring_amount, expiration_updated_at,
                created_by, updated_by, created_at, updated_at
            ) VALUES (?, ?, 'FREE', 0, 1000000, NULL, 0, NULL, 'TEST', 'TEST', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
            """, WALLET_ID, USER_ID);
    }

    /**
     * [TC-CONC-EARN-001]
     * 적립 API를 230원 * 10건 동시에 호출했을 때:
     * - 모든 호출이 성공해야 한다.
     * - 최종 잔액은 2,300원이 되어야 한다.
     */
    @Test
    void concurrentEarn_10Requests_shouldKeepConsistency() throws Exception {
        long before = currentBalance();
        log.info("[CASE][EARN] start walletId={} userId={} beforeBalance={} amount={} threads={}",
            WALLET_ID, USER_ID, before, AMOUNT, THREAD_COUNT);

        List<ApiResult> results = runConcurrently(THREAD_COUNT, idx -> {
            String body = """
                {
                  "walletId":"%s",
                  "orderNo":"%s",
                  "earnType":"ORDER",
                  "amount":%d
                }
                """.formatted(WALLET_ID, randomOrderNo(), AMOUNT);
            ApiResult r = postJson("/api/point/earn", body);
            logEarnResult(idx, r);
            return r;
        });

        long successCount = results.stream().filter(r -> r.status == 200).count();
        assertThat(successCount).isEqualTo(THREAD_COUNT);
        long after = currentBalance();
        log.info("[CASE][EARN] end successCount={} afterBalance={} expectedAfter={}",
            successCount, after, AMOUNT * THREAD_COUNT);
        assertThat(after).isEqualTo(AMOUNT * THREAD_COUNT);
    }

    /**
     * [TC-CONC-USE-001]
     * 사용 API를 230원 * 10건 동시에 호출했을 때:
     * - 사전 적립(2,300원)을 해두면 모든 사용이 성공해야 한다.
     * - 최종 잔액은 0원이 되어야 한다.
     */
    @Test
    void concurrentUse_10Requests_shouldKeepConsistency() throws Exception {
        for (int i = 0; i < THREAD_COUNT; i++) {
            createEarn(AMOUNT);
        }
        long before = currentBalance();
        log.info("[CASE][USE] start walletId={} beforeBalance={} amount={} threads={}",
            WALLET_ID, before, AMOUNT, THREAD_COUNT);
        assertThat(before).isEqualTo(AMOUNT * THREAD_COUNT);

        List<ApiResult> results = runConcurrently(THREAD_COUNT, idx -> {
            String body = """
                {
                  "walletId":"%s",
                  "orderNo":"%s",
                  "amount":%d
                }
                """.formatted(WALLET_ID, randomOrderNo(), AMOUNT);
            ApiResult r = postJson("/api/point/use", body);
            logUseResult(idx, r);
            return r;
        });

        long successCount = results.stream().filter(r -> r.status == 200).count();
        assertThat(successCount).isEqualTo(THREAD_COUNT);
        long after = currentBalance();
        log.info("[CASE][USE] end successCount={} afterBalance={} expectedAfter=0",
            successCount, after);
        assertThat(after).isZero();
    }

    /**
     * [TC-CONC-USE-CANCEL-001]
     * 동일 원거래(USG-...)에 대해 전체취소를 10건 동시에 호출했을 때:
     * - 전체취소는 한 번만 성공해야 한다.
     * - 최종 지갑 잔액은 원거래 사용금액(2,300원)과 동일해야 한다.
     */
    @Test
    void concurrentUseFullCancel_10Requests_sameOriginalUsage_shouldKeepConsistency() throws Exception {
        for (int i = 0; i < THREAD_COUNT; i++) {
            createEarn(AMOUNT);
        }
        String usageId = createUse(AMOUNT * THREAD_COUNT);
        long before = currentBalance();
        log.info("[CASE][USE_FULL_CANCEL] start walletId={} originalUsageId={} beforeBalance={} threads={}",
            WALLET_ID, usageId, before, THREAD_COUNT);
        assertThat(before).isZero();

        List<ApiResult> results = runConcurrently(THREAD_COUNT, idx -> {
            String body = """
                {
                  "walletId":"%s",
                  "originalUsageId":"%s",
                  "cancelType":"FULL_CANCEL"
                }
                """.formatted(WALLET_ID, usageId);
            ApiResult r = postJson("/api/point/use/cancel", body);
            logUseCancelResult(idx, r);
            return r;
        });

        long successCount = results.stream().filter(r -> r.status == 200).count();
        assertThat(successCount).isEqualTo(1);
        long after = currentBalance();
        log.info("[CASE][USE_FULL_CANCEL] end successCount={} afterBalance={} expectedAfter={}",
            successCount, after, AMOUNT * THREAD_COUNT);
        assertThat(after).isEqualTo(AMOUNT * THREAD_COUNT);

        Long totalCancelled = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(used_amount), 0)
            FROM point_usage_record
            WHERE wallet_id = ?
              AND original_usage_id = ?
              AND usage_type IN ('PARTIAL_CANCEL', 'FULL_CANCEL')
            """, Long.class, WALLET_ID, usageId);
        assertThat(totalCancelled).isEqualTo(AMOUNT * THREAD_COUNT);
    }

    /**
     * [TC-CONC-USE-CANCEL-002]
     * 동일 원거래(USG-...)에 대해 부분취소 115원 * 10건을 동시에 호출했을 때:
     * - 총 환급 금액은 1,150원이어야 한다.
     * - 최종 지갑 잔액은 정확히 1,150원이어야 한다.
     */
    @Test
    void concurrentUsePartialCancel_10Requests_sameOriginalUsage_shouldKeepConsistency() throws Exception {
        for (int i = 0; i < THREAD_COUNT; i++) {
            createEarn(AMOUNT);
        }
        String usageId = createUse(AMOUNT * THREAD_COUNT);
        long before = currentBalance();
        log.info("[CASE][USE_CANCEL] start walletId={} originalUsageId={} beforeBalance={} cancelAmount={} threads={}",
            WALLET_ID, usageId, before, PARTIAL_CANCEL_AMOUNT, THREAD_COUNT);
        assertThat(before).isZero();

        List<ApiResult> results = runConcurrently(THREAD_COUNT, idx -> {
            String body = """
                {
                  "walletId":"%s",
                  "originalUsageId":"%s",
                  "cancelAmount":%d,
                  "cancelType":"PARTIAL_CANCEL"
                }
                """.formatted(WALLET_ID, usageId, PARTIAL_CANCEL_AMOUNT);
            ApiResult r = postJson("/api/point/use/cancel", body);
            logUseCancelResult(idx, r);
            return r;
        });

        long successCount = results.stream().filter(r -> r.status == 200).count();
        assertThat(successCount).isEqualTo(THREAD_COUNT);
        long after = currentBalance();
        log.info("[CASE][USE_CANCEL] end successCount={} afterBalance={} expectedAfter={}",
            successCount, after, PARTIAL_CANCEL_AMOUNT * THREAD_COUNT);
        assertThat(after).isEqualTo(PARTIAL_CANCEL_AMOUNT * THREAD_COUNT);

        Long totalCancelled = jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(used_amount), 0)
            FROM point_usage_record
            WHERE wallet_id = ?
              AND original_usage_id = ?
              AND usage_type IN ('PARTIAL_CANCEL', 'FULL_CANCEL')
            """, Long.class, WALLET_ID, usageId);
        assertThat(totalCancelled).isEqualTo(PARTIAL_CANCEL_AMOUNT * THREAD_COUNT);
    }

    /**
     * [TC-CONC-EARN-CANCEL-001]
     * 서로 다른 적립건 10개를 대상으로 적립취소를 동시에 호출했을 때:
     * - 모든 호출이 성공해야 한다.
     * - 최종 잔액은 0원이 되어야 한다.
     */
    @Test
    void concurrentEarnCancel_10Requests_distinctEarnIds_shouldKeepConsistency() throws Exception {
        List<String> earnIds = new ArrayList<>();
        for (int i = 0; i < THREAD_COUNT; i++) {
            earnIds.add(createEarn(AMOUNT));
        }
        long before = currentBalance();
        log.info("[CASE][EARN_CANCEL] start walletId={} beforeBalance={} threads={}",
            WALLET_ID, before, THREAD_COUNT);
        assertThat(before).isEqualTo(AMOUNT * THREAD_COUNT);

        List<ApiResult> results = runConcurrently(THREAD_COUNT, idx -> {
            String body = """
                {
                  "walletId":"%s",
                  "earnId":"%s"
                }
                """.formatted(WALLET_ID, earnIds.get(idx));
            ApiResult r = postJson("/api/point/earn/cancel", body);
            logEarnCancelResult(idx, earnIds.get(idx), r);
            return r;
        });

        long successCount = results.stream().filter(r -> r.status == 200).count();
        assertThat(successCount).isEqualTo(THREAD_COUNT);
        long after = currentBalance();
        log.info("[CASE][EARN_CANCEL] end successCount={} afterBalance={} expectedAfter=0",
            successCount, after);
        assertThat(after).isZero();
    }

    private String createEarn(long amount) throws Exception {
        String body = """
            {
              "walletId":"%s",
              "orderNo":"%s",
              "earnType":"ORDER",
              "amount":%d
            }
            """.formatted(WALLET_ID, randomOrderNo(), amount);
        ApiResult result = postJson("/api/point/earn", body);
        assertThat(result.status).isEqualTo(200);
        JsonNode json = objectMapper.readTree(result.body);
        return json.get("earnId").asText();
    }

    private String createUse(long amount) throws Exception {
        String body = """
            {
              "walletId":"%s",
              "orderNo":"%s",
              "amount":%d
            }
            """.formatted(WALLET_ID, randomOrderNo(), amount);
        ApiResult result = postJson("/api/point/use", body);
        assertThat(result.status).isEqualTo(200);
        JsonNode json = objectMapper.readTree(result.body);
        return json.get("usageId").asText();
    }

    private long currentBalance() throws Exception {
        MvcResult mvcResult = mockMvc.perform(
                get("/api/point")
                    .param("userId", USER_ID)
                    .param("walletType", "FREE"))
            .andReturn();
        JsonNode json = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return json.get("totalBalance").asLong();
    }

    private ApiResult postJson(String path, String body) throws Exception {
        log.info("[REQ] POST {} body={}", path, trimForLog(body));
        MvcResult mvcResult = mockMvc.perform(
                post(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body))
            .andReturn();
        int status = mvcResult.getResponse().getStatus();
        String resBody = mvcResult.getResponse().getContentAsString();
        log.info("[RES] POST {} status={} body={}", path, status, trimForLog(resBody));
        return new ApiResult(status, resBody);
    }

    private String randomOrderNo() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    private List<ApiResult> runConcurrently(int threadCount, IndexedTask task) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<ApiResult>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            futures.add(executor.submit((Callable<ApiResult>) () -> {
                ready.countDown();
                if (!start.await(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IllegalStateException("동시 시작 대기 타임아웃");
                }
                try {
                    ApiResult r = task.call(idx);
                    log.info("[THREAD-{}] status={}", idx, r.status);
                    return r;
                } catch (Exception e) {
                    log.error("[THREAD-{}] failed: {}", idx, e.toString(), e);
                    throw e;
                }
            }));
        }

        if (!ready.await(10, TimeUnit.SECONDS)) {
            executor.shutdownNow();
            throw new IllegalStateException("스레드 준비 타임아웃");
        }
        start.countDown();

        List<ApiResult> results = new ArrayList<>();
        for (Future<ApiResult> future : futures) {
            results.add(future.get(20, TimeUnit.SECONDS));
        }

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);
        return results;
    }

    @FunctionalInterface
    interface IndexedTask {
        ApiResult call(int index) throws Exception;
    }

    private record ApiResult(int status, String body) {}

    private void logEarnResult(int idx, ApiResult r) throws Exception {
        if (r.status != 200) {
            log.warn("[EARN][{}] failed status={} body={}", idx, r.status, trimForLog(r.body));
            return;
        }
        JsonNode json = objectMapper.readTree(r.body);
        log.info("[EARN][{}] success earnId={} earnedAmount={} balance={} expirationDate={} ",
            idx,
            json.path("earnId").asText(),
            json.path("earnedAmount").asLong(),
            json.path("balance").asLong(),
            json.path("expirationDate").asText());
    }

    private void logUseResult(int idx, ApiResult r) throws Exception {
        if (r.status != 200) {
            log.warn("[USE][{}] failed status={} body={}", idx, r.status, trimForLog(r.body));
            return;
        }
        JsonNode json = objectMapper.readTree(r.body);
        log.info("[USE][{}] success usageId={} usedAmount={} balance={} orderNo={}",
            idx,
            json.path("usageId").asText(),
            json.path("usedAmount").asLong(),
            json.path("balance").asLong(),
            json.path("orderNo").asText());
    }

    private void logUseCancelResult(int idx, ApiResult r) throws Exception {
        if (r.status != 200) {
            log.warn("[USE_CANCEL][{}] failed status={} body={}", idx, r.status, trimForLog(r.body));
            return;
        }
        JsonNode json = objectMapper.readTree(r.body);
        log.info("[USE_CANCEL][{}] success cancelId={} originalUsageId={} cancelledAmount={} remainingUsageAmount={} walletBalance={} type={}",
            idx,
            json.path("cancelId").asText(),
            json.path("originalUsageId").asText(),
            json.path("cancelledAmount").asLong(),
            json.path("remainingUsageAmount").asLong(),
            json.path("userPointWalletBalance").asLong(),
            json.path("usageType").asText());
    }

    private void logEarnCancelResult(int idx, String earnId, ApiResult r) throws Exception {
        if (r.status != 200) {
            log.warn("[EARN_CANCEL][{}] failed earnId={} status={} body={}", idx, earnId, r.status, trimForLog(r.body));
            return;
        }
        JsonNode json = objectMapper.readTree(r.body);
        log.info("[EARN_CANCEL][{}] success earnId={} cancelledAmount={}",
            idx,
            json.path("earnId").asText(),
            json.path("cancelledAmount").asLong());
    }

    private String trimForLog(String s) {
        if (s == null) return "null";
        String oneLine = s.replaceAll("\\s+", " ").trim();
        if (oneLine.length() <= LOG_BODY_MAX) return oneLine;
        return oneLine.substring(0, LOG_BODY_MAX) + "...(truncated)";
    }
}

