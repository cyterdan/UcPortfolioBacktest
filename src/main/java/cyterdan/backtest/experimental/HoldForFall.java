package cyterdan.backtest.experimental;

import cyterdan.backtest.core.data.providers.DataProvider;
import cyterdan.backtest.core.data.providers.H2DataProvider;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import cyterdan.backtest.core.model.DailySerie;
import cyterdan.backtest.core.model.HistoricalData;
import cyterdan.backtest.core.model.Portfolio;
import cyterdan.backtest.core.model.allocation.AllocationRebalanceMode;
import cyterdan.backtest.core.model.allocation.DateBasedAllocation;
import cyterdan.backtest.core.model.allocation.FixedAllocation;

/**
 * Test what happens when you wait before a drop
 *
 * (if the past y years have been consistently rising, you sell everything and
 * wait for the market to drop d% before buying again)
 *
 * @author cytermann
 */
public class HoldForFall {

    private static final DataProvider dataProvider = new H2DataProvider();

    private static final String MSCI_WORLD = "_MSCI_The_World_Index";

    public static void main(String[] args) throws SQLException {

        Set<String> isins = new HashSet<>();
        isins.add(MSCI_WORLD);
        HistoricalData data = dataProvider.getDataForIsins(isins);

        double d = 0.2;
  //      for (int y = 1; y < 10; y++) {
            doHoldForFall(data, 8, d);
   //     }

    }

    private static void doHoldForFall(final HistoricalData data, final int y, final double d) {
        LocalDate start = data.usefulStart();
        LocalDate end = data.usefulEnd();

        FixedAllocation referenceAlocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
        referenceAlocation.put(MSCI_WORLD, 1.0);
        Portfolio reference = new Portfolio(referenceAlocation);
        DateBasedAllocation dateBasedAllocation = new DateBasedAllocation();

        dateBasedAllocation.set(start, new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER));
        final DailySerie fundData = data.getFundData(MSCI_WORLD);

        //with start after y year observation period
        for (LocalDate date = start.plusYears(y); date.isBefore(end); date = date.plusDays(1)) {
            final LocalDate finalDate = date;

            if (!dateBasedAllocation.getIsinsForDate(date).isEmpty()) {

                boolean allPositive = IntStream.rangeClosed(0, y).mapToDouble(
                        i -> {
                            int low = y - i - 1;
                            int high = y - i;
                            //System.out.println("from  "+finalDate.minusYears(high)+ " to "+ finalDate.minusYears(low));
                            double ret = fundData.extractReturn(finalDate.minusYears(high), finalDate.minusYears(low));
                            return ret;
                        }).allMatch(dbl -> dbl > 0.0);

                //a fall is coming ?
                if (allPositive) {
                    //uninvest
                    dateBasedAllocation.set(date, new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER));
                }
            } else {
                //if we're not invested, check if the market has dropped d% and invest
                LocalDate lastInvestementDate = dateBasedAllocation.dates().stream().max((a, b) -> a.compareTo(b)).get();
                Double drop = fundData.extractReturn(lastInvestementDate, date);
                if (drop < -d) {
                    FixedAllocation investment = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
                    investment.put(MSCI_WORLD, 1.0);
                    dateBasedAllocation.set(date, investment);
                }

            }

        }
        Portfolio port = new Portfolio(dateBasedAllocation);
        DailySerie performance = port.calculateAllocationPerformance(start, end, data);
        DailySerie referencePerf = reference.calculateAllocationPerformance(start, end, data);
        System.out.println(y + " years /" + d * 100 + "% : " + performance.annualReturns() + " vs " + referencePerf.annualReturns());

    }

}
