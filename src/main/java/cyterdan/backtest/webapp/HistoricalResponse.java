package cyterdan.backtest.webapp;

import cyterdan.backtest.core.model.HistoricalData;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cytermann
 */
public class HistoricalResponse {

    private final Map<String, Object> response;

    private static final String STATUS = "status";

    private static final String MESSAGE = "message";

    public static final String PRESET = "preset";

    public static HistoricalResponse ok() {
        HistoricalResponse ok = new HistoricalResponse();
        ok.response.put(STATUS, "OK");
        return ok;
    }

    
    
    public HistoricalResponse series(HistoricalData data){
        this.response.put("results", data);
        return this;
    }
    
    public static HistoricalResponse error(String message){
        HistoricalResponse error = new HistoricalResponse();
        error.response.put(STATUS, "ERROR");
        error.response.put(MESSAGE, message);
        return error;
    }
    public HistoricalResponse() {
        response = new HashMap<>();
    }

}
