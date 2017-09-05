package cyterdan.backtest.experimental;

import cyterdan.backtest.core.data.providers.DataProvider;
import cyterdan.backtest.core.data.providers.H2DataProvider;
import cyterdan.backtest.core.utils.FUND_CONSTANTS;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
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
public class SellInMayBuyInOctober {

    private static final DataProvider dataProvider = new H2DataProvider();
    private static HistoricalData data;
    private static final String MSCI_WORLD = "_MSCI_The_World_Index";
    private static final String CAC = "FR0000989386";

    public static double sellInBuyIn(int sellInMonth, int buyInMonth, String isin, int dayOfMonth) throws SQLException {

        LocalDate usefulStart = data.getFundData(isin).firstDate();
        int startYear = usefulStart.getYear();
        LocalDate startMayDate = usefulStart.withMonth(sellInMonth).withDayOfMonth(dayOfMonth);
        if (startMayDate.isBefore(usefulStart)) {
            startMayDate = startMayDate.withYear(startYear + 1);
        }

        LocalDate endDate = data.getFundData(isin).latestDate();

        DateBasedAllocation allocation = new DateBasedAllocation();

        FixedAllocation fullmsci = new FixedAllocation(AllocationRebalanceMode.REBALANCE_EVERY2WEEKS);
        fullmsci.put(isin, 1.0);

        FixedAllocation fullcash = new FixedAllocation(AllocationRebalanceMode.REBALANCE_EVERY2WEEKS);
        fullcash.put(HistoricalData.CASH, 1.0);

        DateBasedAllocation refAllocation = new DateBasedAllocation();
        refAllocation.set(startMayDate, fullmsci);
        Portfolio reference = new Portfolio(refAllocation);

        LocalDate date = startMayDate;
        while (date.isBefore(endDate)) {
            LocalDate may = date;
            LocalDate october = date.withMonth(buyInMonth);
            allocation.set(may, fullcash);
            allocation.set(october, fullmsci);
            //System.out.println(may + " to " + october);
            date = date.plusYears(1l);
        }
        Portfolio portfolio = new Portfolio(allocation);

        DailySerie results = portfolio.calculateAllocationPerformance(startMayDate, endDate, data);
        DailySerie refResults = reference.calculateAllocationPerformance(startMayDate, endDate, data);
        /*System.out.println("sell @" + dayOfMonth + " in " + sellInMonth + " buy in " + buyInMonth + "=>" + results.totalReturn() * 100 + "% vs " + refResults.totalReturn() * 100 + "%");
         int endYear = endDate.getYear();

        
         for (int year = startYear + 2; year < endYear; year++) {

         Double perf = results.extractReturn(LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));
         Double ref = refResults.extractReturn(LocalDate.of(year, Month.JANUARY, 1), LocalDate.of(year, Month.DECEMBER, 31));

         System.out.println(year +"\t"+ perf+"\t"+ref);
         }*/

        return results.totalReturn() - refResults.totalReturn();

// System.out.println("total : " + results.totalReturn() * 100 + "% vs " + refResults.totalReturn() * 100 + "%");
        // System.out.println("annual : " + results.annualReturns() + " vs " + refResults.annualReturns());
        // System.out.println("volatility : " + results.yearlyVolatility() + " vs " + refResults.yearlyVolatility());
    }

    public static void main(String[] args) throws SQLException {

        Set<String> isins = dataProvider.getIsins();
        
                List<String> inclusions = FUND_CONSTANTS.MES_PLACEMENTS_LIBERTE;

        //garder que certains fonds
        //retirer les scpis et les fonds euro  
          isins = isins.stream().filter(isin -> inclusions.contains(isin))
                  .filter(isin -> !isin.contains("SCPI") && !isin.contains("QU")).collect(Collectors.toSet());
                    


         data = dataProvider.getDataForIsins(isins);
         
         //retirer ceux qui ont moins de 10 ans de données
         isins = isins.stream().filter(isin -> data.getFundData(isin).numberOfFullYears()>=10 ).collect(Collectors.toSet());
         
         
        double max = 0;
        double min = 0;
        Map<String,Double> surperformances = new HashMap<>();
        
        
        for(String isin : isins){
            double surperf= sellInBuyIn(5, 10,isin, 15);
            
            surperformances.put(isin, surperf);
      
        }
     Map<String,Double> sorted  = surperformances.entrySet().stream()
    .sorted(Entry.comparingByValue())
    .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                              (e1, e2) -> e1, LinkedHashMap::new));
        
     sorted.forEach((String t, Double u) -> {
         System.out.println(t+":"+u);
        });

    }

}
