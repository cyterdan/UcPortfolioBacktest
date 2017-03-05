package cyterdan.backtest.app;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cyterdan.backtest.utils.SerializationUtils;
import java.sql.SQLException;
import java.util.Map;
import model.BacktestResponse;
import model.allocation.AllocationRebalanceMode;
import model.allocation.FixedAllocation;
import org.apache.commons.codec.binary.Base64;
import org.rapidoid.http.Req;
import org.rapidoid.setup.On;
import org.rapidoid.u.U;
import services.BacktestingService;

/**
 *
 * @author cytermann
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        BacktestingService backtestingService = new BacktestingService();

        //port setup in case it's imposed by the environnement (heroku)
        String port = System.getenv("PORT");
        if (port != null) {
            On.port(Integer.valueOf(port));
        }

        //redirect to /portfolio by default
        On.page("/").html((req, resp) -> {
            resp.redirect("/portfolio");
            return "";
        });

        //renders portfolio.html template
        On.page("/portfolio").mvc((req, resp) -> {

            //if we get preset data in the request params
            if (req.params().containsKey(BacktestResponse.PRESET)) {
                // unserialize the preset data
                Map<String, Object> deserialized = SerializationUtils.deserialize(req.param(BacktestResponse.PRESET), new TypeToken<Map<String, Object>>() {
                }.getType());

                // encode and set in the page data 
                return U.map(BacktestResponse.PRESET, Base64.encodeBase64String(new Gson().toJson(deserialized).getBytes()));
            } else {
                return null;
            }

        });

        //responds to /backtest post requests
        On.post("/backtest").json((Req req) -> {

            //read options
            AllocationRebalanceMode rebalanceMode = AllocationRebalanceMode.fromInteger(Integer.valueOf(req.posted("rebalanceMode")));
            String benchmark = req.posted("benchmark");

            //read allocation from post data
            FixedAllocation allocation = FixedAllocation.fromHtmlFormData(req.posted(), rebalanceMode);

            BacktestResponse response = backtestingService.backtest(allocation, rebalanceMode, benchmark);

            return response.build();

        });
    }

}
