
package cyterdan.backtest.experimental;

import cyterdan.backtest.core.data.providers.DataProvider;
import cyterdan.backtest.core.data.providers.H2DataProvider;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import cyterdan.backtest.core.model.DailySerie;
import cyterdan.backtest.core.model.HistoricalData;
import cyterdan.backtest.core.model.Portfolio;
import cyterdan.backtest.core.model.allocation.AllocationRebalanceMode;
import cyterdan.backtest.core.model.allocation.DateBasedAllocation;
import cyterdan.backtest.core.model.allocation.FixedAllocation;

/**
 *
 * @author cytermann
 */
public class DCA {

    private static final DataProvider dataProvider = new H2DataProvider();

    private static final String MSCI_WORLD = "_MSCI_The_World_Index";

    public static void main(String[] args) throws SQLException {

        Set<String> isins = new HashSet<>();
        isins.add(MSCI_WORLD);
        HistoricalData data = dataProvider.getDataForIsins(isins);

        LocalDate start = data.usefulStart();
        LocalDate end = data.usefulEnd().minusYears(8);

        FixedAllocation buyAndHold = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
        buyAndHold.put(MSCI_WORLD, 1.0);

        
        Map<LocalDate,Double> dcaAdvantage = new HashMap<>();
        //pour chaque jour de début
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1)) {

            DateBasedAllocation dca = new DateBasedAllocation();

            double riskPart = 0;
            for (LocalDate monthDate = date; monthDate.isBefore(date.plusYears(8)); monthDate = monthDate.plusMonths(1)) {
                riskPart += 1.0 / 12;
                riskPart = Math.min(riskPart, 1.0);
                FixedAllocation monthAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_EVERY2WEEKS);
                monthAllocation.put(MSCI_WORLD, riskPart);
                monthAllocation.completeWith(HistoricalData.CASH);
                dca.set(monthDate, monthAllocation);
            }
            Portfolio port = new Portfolio(dca);
            Portfolio ref = new Portfolio(buyAndHold);
            DailySerie dcaPerformance = port.calculateAllocationPerformance(date, date.plusYears(8), data);
            DailySerie buyAndHoldPerformance = ref.calculateAllocationPerformance(date, date.plusYears(8), data);
            dcaAdvantage.put(date, dcaPerformance.totalReturn()-buyAndHoldPerformance.totalReturn());
            
        }

    }
}
