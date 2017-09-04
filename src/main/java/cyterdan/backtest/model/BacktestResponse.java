package cyterdan.backtest.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * flexible backtest response object
 * @author cytermann
 */
public class BacktestResponse {

    private static final String STATUS = "status";

    private static final String MESSAGE = "message";

    public static final String PRESET = "preset";

    public static BacktestResponse ok() {
        BacktestResponse ok = new BacktestResponse();
        ok.response.put(STATUS, "OK");
        return ok;
    }

    public BacktestResponse() {
        response = new  HashMap<>();
    }

    
    private Map<String, Object> response;

    public Map<String, Object> build() {
        return response;
    }

    public static BacktestResponse error(String errorMessage) {
        BacktestResponse err = new BacktestResponse();
        err.response.put(STATUS, "ERROR");
        err.response.put(MESSAGE, errorMessage);
        return err;
    }

    public BacktestResponse history(DailySerie history) {
        this.response.put("history", history.toJsArray());
        return this;
    }

    public BacktestResponse reference(DailySerie history) {
        this.response.put("reference", history.toJsArray());
        return this;
    }

    public BacktestResponse perf(double perfAnnual) {
        BigDecimal formattedPerf = BigDecimal.valueOf(perfAnnual);
        formattedPerf.setScale(2, RoundingMode.FLOOR);
        response.put("perf", formattedPerf);
        return this;

    }

    public BacktestResponse std(double yearlyVolatility) {
        BigDecimal formattedStd = BigDecimal.valueOf(yearlyVolatility);
        formattedStd.setScale(2, RoundingMode.FLOOR);
        response.put("std", formattedStd);
        return this;
    }

    public BacktestResponse message(String message) {
        response.put(MESSAGE,message);
        return this;
    }

    public BacktestResponse permalink(String permalink) {
        response.put("permalink", permalink);
        return this;
    }

}
