package cyterdan.backtest.core.model;

import cyterdan.backtest.core.model.allocation.Allocation;
import java.time.LocalDate;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author cytermann
 */
public class Portfolio {

    private final DailySerie performance;

    private final Allocation allocation;

    public Portfolio(Allocation allocation) {
        this.allocation = allocation;
        this.performance = new DailySerie();
    }

    public DailySerie calculateAllocationPerformance(LocalDate from, LocalDate to, HistoricalData data) {

        performance.setValue(from, 1.0);

        for (LocalDate d = from.plusDays(1); d.isBefore(to); d = d.plusDays(1)) {

            double perf = 0.0;

            for (String isin : allocation.getIsinsForDate(d)) {
                Double currentAllocationForIsin = allocation.getPositionForDateAndIsin(d, isin);
                Double isinDayReturn = data.getFundData(isin).extractReturn(d.minusDays(1), d);
                perf += currentAllocationForIsin * isinDayReturn;
                allocation.updatePositionWithReturn(isin, isinDayReturn);
                switch (allocation.getRebalanceMode()) {

                    case REBALANCE_EVERY2WEEKS:
                        //are we at n*two weeks distance?
                        long nbDays = d.toEpochDay() - from.toEpochDay();
                        if (nbDays % 14 == 0) {
                            allocation.reset();
                        }
                        break;
                    case REBALANCE_FIVEPCTDIFF:
                        double diff = allocation.distanceFromInitial(isin);
                        if (diff > 0.05) {
                            allocation.reset();
                        }

                        break;

                    default:
                        break;

                }

            }

            performance.setValue(d, performance.getValue(d.minusDays(1)) * (1 + perf));
        }

        return performance;
    }

}
