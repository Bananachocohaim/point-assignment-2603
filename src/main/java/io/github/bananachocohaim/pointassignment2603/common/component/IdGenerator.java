package io.github.bananachocohaim.pointassignment2603.common.component;

import org.springframework.stereotype.Component;

import com.github.f4b6a3.tsid.TsidCreator;

@Component
public class IdGenerator {
    //사용자 지갑 아이디
    public String getPointWalletId() {
        return "WLT" +"-"+ TsidCreator.getTsid().toString();
    }

    public String getPointEarnId() {
        return "ERN" +"-"+ TsidCreator.getTsid().toString();
    }

    public String getPointUsageId() {
        return "USG" +"-"+ TsidCreator.getTsid().toString();
    }
}
