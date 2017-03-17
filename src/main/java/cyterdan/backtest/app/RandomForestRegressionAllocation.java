package cyterdan.backtest.app;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import model.DateBasedSerie;
import model.HistoricalData;
import model.Portfolio;
import model.allocation.AllocationRebalanceMode;
import model.allocation.DateBasedAllocation;
import model.allocation.FixedAllocation;

/**
 * model is trained via another program.
 *
 * @author cytermann
 */
public class RandomForestRegressionAllocation {

    private static final DataProvider dataProvider = new H2DataProvider();

    public static void main(String[] args) throws SQLException {

        List<String> BuyDates = Arrays.asList(
                "2006-08-16", "2006-08-30", "2006-09-13", "2006-09-27", "2006-10-11", "2006-10-25",
                "2006-11-08", "2006-11-22", "2006-12-06", "2006-12-20", "2007-01-03", "2007-01-17",
                "2007-01-31", "2007-02-14", "2007-02-28", "2007-03-14", "2007-03-28", "2007-04-11",
                "2007-04-25", "2007-05-09", "2007-05-23", "2007-06-20", "2007-08-01", "2007-08-15",
                "2007-08-29", "2007-09-12", "2007-09-26", "2007-10-10", "2007-10-24", "2007-12-05",
                "2007-12-19", "2008-01-30", "2008-02-13", "2008-02-27", "2008-03-12", "2008-04-09",
                "2008-04-23", "2008-05-07", "2008-05-21", "2008-07-16", "2008-08-13", "2008-08-27",
                "2008-12-17", "2008-12-31", "2009-01-14", "2009-01-28", "2009-03-11", "2009-03-25",
                "2009-04-08", "2009-04-22", "2009-05-06", "2009-05-20", "2009-06-03", "2009-06-17",
                "2009-07-01", "2009-07-15", "2009-07-29", "2009-08-12", "2009-08-26", "2009-09-09",
                "2009-09-23", "2009-10-07", "2009-11-04", "2009-11-18", "2009-12-02", "2009-12-16",
                "2009-12-30", "2010-01-13", "2010-01-27", "2010-02-10", "2010-02-24", "2010-03-10",
                "2010-03-24", "2010-04-07", "2010-05-05", "2010-06-02", "2010-06-16", "2010-06-30",
                "2010-07-14", "2010-07-28", "2010-08-11", "2010-08-25", "2010-09-08", "2010-09-22",
                "2010-10-06", "2010-10-20", "2010-11-03", "2010-12-01", "2010-12-15", "2010-12-29",
                "2011-01-12", "2011-01-26", "2011-02-09", "2011-02-23", "2011-03-09", "2011-03-23",
                "2011-04-06", "2011-04-20", "2011-05-04", "2011-06-01", "2011-06-15", "2011-06-29",
                "2011-07-27", "2011-08-24", "2011-10-05", "2011-10-19", "2011-11-30", "2011-12-14",
                "2011-12-28", "2012-01-11", "2012-01-25", "2012-02-08", "2012-02-22", "2012-03-07",
                "2012-03-21", "2012-04-18", "2012-05-02", "2012-06-13", "2012-06-27", "2012-07-11",
                "2012-07-25", "2012-08-08", "2012-08-22", "2012-09-05", "2012-09-19", "2012-10-03",
                "2012-10-17", "2012-10-31", "2012-11-14", "2012-11-28", "2012-12-12", "2012-12-26",
                "2013-01-09", "2013-01-23", "2013-02-06", "2013-02-20", "2013-03-06", "2013-04-17",
                "2013-05-01", "2013-05-15", "2013-06-12", "2013-06-26", "2013-07-10", "2013-07-24",
                "2013-09-18", "2013-10-02", "2013-10-16", "2013-10-30", "2013-11-27", "2013-12-11",
                "2014-01-08", "2014-01-22", "2014-02-05", "2014-03-05", "2014-03-19", "2014-04-16",
                "2014-04-30", "2014-05-14", "2014-05-28", "2014-06-11", "2014-07-09", "2014-08-06",
                "2014-08-20", "2014-09-03", "2014-10-29", "2014-11-12", "2014-11-26", "2014-12-24",
                "2015-01-21", "2015-02-04", "2015-02-18", "2015-03-04", "2015-03-18", "2015-04-01",
                "2015-04-15", "2015-04-29", "2015-05-13", "2015-06-10", "2015-07-08", "2015-07-22",
                "2015-09-02", "2015-09-16", "2015-09-30", "2015-10-14", "2015-10-28", "2015-11-25",
                "2015-12-23", "2016-01-06", "2016-01-20", "2016-02-17", "2016-03-02", "2016-03-16",
                "2016-03-30", "2016-04-13", "2016-04-27", "2016-05-11", "2016-05-25", "2016-06-08",
                "2016-06-22", "2016-07-06", "2016-07-20", "2016-08-03", "2016-08-17", "2016-08-31",
                "2016-09-14", "2016-09-28", "2016-10-12", "2016-11-23", "2016-12-07", "2016-12-21",
                "2017-01-18", "2017-02-01", "2017-02-15"
        );

        String isin = "LU0110060430";

        Set<String> isins = new HashSet();
        isins.add(isin);
        List<LocalDate> buyLocalDates = BuyDates.stream()
                .map(sdate -> Date.valueOf(sdate).toLocalDate())
                .collect(Collectors.toList());

        DateBasedAllocation allocation = new DateBasedAllocation();
        HistoricalData data = dataProvider.getDataForIsins(isins);
        LocalDate start = data.usefulStart();
        LocalDate end = data.usefulEnd();
        for (LocalDate date = start; date.isBefore(end); date = date.plus(2, ChronoUnit.WEEKS)) {
            FixedAllocation dayAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
            if (buyLocalDates.contains(date)) {
                dayAllocation.put(isin, 1.0);
            } else {
                dayAllocation.put(HistoricalData.CASH, 1.0);
            }
            allocation.set(date, dayAllocation);
        }

        Portfolio portfolio = new Portfolio(allocation);
        DateBasedSerie results = portfolio.calculateCapital(start, end, data);
        System.out.println(results.annualReturns());
        System.out.println(results.yearlyVolatility());
        System.out.println(results.totalReturn());

    }

}
