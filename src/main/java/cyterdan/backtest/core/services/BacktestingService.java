package cyterdan.backtest.core.services;

import cyterdan.backtest.core.data.providers.DataProvider;
import cyterdan.backtest.core.data.providers.PostgresDataProvider;
import cyterdan.backtest.webapp.SerializationUtils;
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
import cyterdan.backtest.core.model.BacktestResponse;
import cyterdan.backtest.webapp.HistoricalResponse;
import cyterdan.backtest.core.model.DailySerie;
import cyterdan.backtest.core.model.HistoricalData;
import cyterdan.backtest.core.model.Portfolio;
import cyterdan.backtest.core.model.allocation.AllocationRebalanceMode;
import cyterdan.backtest.core.model.allocation.FixedAllocation;
import org.rapidoid.job.Jobs;
import org.rapidoid.util.Msc;

/**
 * Portfolio backtesting service
 *
 * @author cytermann
 */
public class BacktestingService {

    //postgres data provider for web pages
    private static final DataProvider dataProvider;

    static {
        dataProvider = new PostgresDataProvider();
        if (!dataProvider.isOk()) {
            try {
                throw new Exception("Database is KO");
            } catch (Exception ex) {
                Logger.getLogger(BacktestingService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public BacktestResponse backtest(FixedAllocation allocation, AllocationRebalanceMode rebalanceMode, String benchmark) throws SQLException, IOException {

        if (!allocation.isValid()) {
            return BacktestResponse.error("sum of allocations must be 100%");
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
            String permalink = "/portfolio?preset=" + Msc.urlEncode(SerializationUtils.serialize(porteData));

            //async log
            Jobs.schedule(() -> dataProvider.logBacktest(permalink, perfAnnual, yearlyVolatility), 1, TimeUnit.NANOSECONDS);

            return BacktestResponse.ok()
                    .history(capital)
                    .reference(referenceData)
                    .perf(perfAnnual)
                    .std(yearlyVolatility)
                    .message(String.format("Les fonds de ce portefeuil ont des données entre %s et %s ", minDate.toString(), maxDate.toString()))
                    .permalink(permalink);

        }

    }

    public HistoricalResponse historicalData(String isins) {
        //validate parameters
        if (isins.isEmpty()) {
            return HistoricalResponse.error("parameter isins is empty");
        }
        String[] split = isins.split(",");
        Set<String> isinSet = new HashSet<>();
        for (String isin : split) {
            String clean = isin.trim().toUpperCase();
            if (!clean.matches("[A-Z0-9]+")) {
                return HistoricalResponse.error("error processing isin : " + isin);
            } else {
                isinSet.add(clean);
            }
        }

        try {
            HistoricalData dataForIsins = dataProvider.getDataForIsins(isinSet);
            return HistoricalResponse.ok().series(dataForIsins);

        } catch (SQLException sqlException) {
            return HistoricalResponse.error("Service is unavailable");
        }

    }

}
