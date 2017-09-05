package services;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import cyterdan.backtest.data.providers.PostgresDataProvider;
import cyterdan.backtest.utils.SerializationUtils;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.BacktestResponse;
import model.DailySerie;
import model.HistoricalData;
import model.Portfolio;
import model.allocation.AllocationRebalanceMode;
import model.allocation.FixedAllocation;
import org.rapidoid.job.Jobs;
import org.rapidoid.util.Msc;

/**
 * Portfolio backtesting service
 * @author cytermann
 */
public class BacktestingService {

    //postgres data provider for web pages
    private static final DataProvider dataProvider;

    static{
        dataProvider = new PostgresDataProvider();
        if(!dataProvider.isOk()){
            try {
                throw new Exception("Database is KO");
            } catch (Exception ex) {
                Logger.getLogger(BacktestingService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    public BacktestResponse backtest(FixedAllocation allocation, AllocationRebalanceMode rebalanceMode, String benchmark) throws SQLException, IOException {

        if (!allocation.isValid()) {
            return BacktestResponse.error("La somme des parts doit faire 100%");
        } else {
            Set<String> isinSet = allocation.getFunds();

            HistoricalData data = dataProvider.getDataForIsins(isinSet);

            Set<String> referenceIsin = new HashSet<>();

            String refIsin = benchmark;
            referenceIsin.add(refIsin);

            DailySerie referenceData = dataProvider.getDataForIsins(referenceIsin).getFundData(refIsin);

            Portfolio portfolio = new Portfolio(allocation);

            LocalDate minDate = data.usefulStart();
            LocalDate maxDate = data.usefulEnd();


            DailySerie capital = portfolio.calculateAllocationPerformance(minDate, maxDate, data);

            double perfAnnual = capital.annualReturns();

            double yearlyVolatility = capital.yearlyVolatility();

            System.out.println("perf globale : " + perfAnnual + " std : " + yearlyVolatility);

            //retirer l'historique inutile de la réference
            referenceData = referenceData.extract(minDate, maxDate);


            Map<String, Object> porteData = new HashMap<>();
            porteData.put("porte", allocation.getAllocationMap());
            porteData.put("rebalanceMode", rebalanceMode.getMode());
            porteData.put("benchmark", benchmark);
            String permalink =  "/portfolio?preset=" + Msc.urlEncode(SerializationUtils.serialize(porteData));

            //async log
            Jobs.schedule(() -> dataProvider.logBacktest(permalink,perfAnnual , yearlyVolatility), 1, TimeUnit.NANOSECONDS);
            
            return BacktestResponse.ok()
                    .history(capital)
                    .reference(referenceData)
                    .perf(perfAnnual)
                    .std(yearlyVolatility)
                    .message(String.format("Les fonds de ce portefeuil ont des données entre %s et %s ", minDate.toString(), maxDate.toString()))
                    .permalink(permalink);
            
        }

    }

}


