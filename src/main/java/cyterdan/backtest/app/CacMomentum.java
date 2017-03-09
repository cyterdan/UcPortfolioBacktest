package cyterdan.backtest.app;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import model.DateBasedSerie;
import model.HistoricalData;
import model.Portfolio;
import model.allocation.AllocationRebalanceMode;
import model.allocation.DateBasedAllocation;
import model.allocation.FixedAllocation;

/**
 *
 * @author cytermann
 */
public class CacMomentum {

    private static DataProvider dataProvider;
    private static final String CAC = "FR0007052782";
    private static final String CACLongLev = "FR0010592014";
    private static final String CacShortLev = "FR0010411884";

    public static void main(String[] args) throws SQLException, Exception {

        dataProvider = new H2DataProvider();

        Set<String> isins = Stream.of(CAC, CACLongLev, CacShortLev).collect(Collectors.toSet());

        HistoricalData data = dataProvider.getDataForIsins(isins);

        doMomentum(data, ChronoUnit.DAYS, 5, -0.02, 0.006);
        /*for (double lb = -2.0 / 100; lb < -0.1 / 100; lb += 0.1 / 100) {
            for (double ub = 0.1 / 100; ub < 2.0 / 100; ub += 0.1 / 100) {
                for (int d = 1; d < 30; d++) {
                    doMomentum(data, ChronoUnit.DAYS, d, lb, ub);
                }
            }
        }*/

    }

    private static double doMomentum(HistoricalData data, ChronoUnit chronoUnit, int qty, double lowerBound, double upperBound) {

        final LocalDate start = data.usefulStart().plus(qty, chronoUnit).plusDays(1);
        final LocalDate end = data.usefulEnd();

        DateBasedAllocation allocation = new DateBasedAllocation();
        for (LocalDate day = start; day.isBefore(end); day = day.plusDays(1)) {
            //chaque jour regarder l'évolution du cac sur la période, si >X% alors acheter du long si <X% alors acheter du short, sinon cash
            double perf = data.getFundData(CAC).extractReturn(day.minus(qty, chronoUnit), day);

            FixedAllocation dailyAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);

            String fund = HistoricalData.CASH;
            if (perf > upperBound) {
                fund = CacShortLev;
            } else if (perf < lowerBound) {
                fund = CACLongLev;

            }

            dailyAllocation.put(fund, fund.equals(HistoricalData.CASH) ? 1.0 : 1.0);
            allocation.set(day, dailyAllocation);
        }

        Portfolio portfolio = new Portfolio(allocation);
        DateBasedSerie K = portfolio.calculateCapital(start, end, data);

        double annualReturns = K.annualReturns();
        double sharp = (annualReturns - 2) / K.yearlyVolatility();
        if (annualReturns > 0 && sharp > 0.6) {
            System.out.println("every " + qty + " " + chronoUnit.name() + " (" + lowerBound + "," + upperBound + ") :  " + K.annualReturns() + " / "+ K.yearlyVolatility()+" / "+ sharp);
        }
        return annualReturns;
    }

}
