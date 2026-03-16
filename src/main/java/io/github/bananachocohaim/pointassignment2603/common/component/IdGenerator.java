package io.github.bananachocohaim.pointassignment2603.common.component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * ID 생성기 - DB 시퀀스 기반.
 * 형식: {PREFIX}-{yyMMddHHmmss}{4자리 DB시퀀스}
 * 시퀀스는 종류별로 독립된 H2 SEQUENCE(0000~9999 CYCLE) 사용.
 * 예: WLT-2603151432010001
 */
@Component
@RequiredArgsConstructor
public class IdGenerator {

    private final JdbcTemplate jdbcTemplate;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyMMddHHmmss");

    private String generate(String prefix, String seqName) {
        String ts = LocalDateTime.now().format(TS_FMT);
        Integer seq = jdbcTemplate.queryForObject(
            "SELECT NEXT VALUE FOR " + seqName, Integer.class);
        return String.format("%s-%s%04d", prefix, ts, seq != null ? seq : 0);
    }

    /** 사용자 지갑 ID (WLT-yyMMddHHmmssSSSSS) */
    public String getPointWalletId() {
        return generate("WLT", "seq_wlt");
    }

    /** 적립 ID (ERN-yyMMddHHmmssSSSSS) */
    public String getPointEarnId() {
        return generate("ERN", "seq_ern");
    }

    /** 사용 ID (USG-yyMMddHHmmssSSSSS) */
    public String getPointUsageId() {
        return generate("USG", "seq_usg");
    }

    /** 전체취소 ID (CNC-yyMMddHHmmssSSSSS) */
    public String getPointCancelId() {
        return generate("CNC", "seq_cnc");
    }

    /** 부분취소 ID (PCN-yyMMddHHmmssSSSSS) */
    public String getPointPartialCancelId() {
        return generate("PCN", "seq_pcn");
    }
}
