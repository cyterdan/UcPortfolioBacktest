package cyterdan.backtest.app;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import cyterdan.backtest.utils.AV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import model.allocation.AllocationRebalanceMode;
import model.allocation.DateBasedAllocation;
import model.DateBasedSerie;
import model.allocation.FixedAllocation;
import model.HistoricalData;
import model.MomentumStrategy;
import model.Portfolio;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 *
 * @author cytermann
 */
public class Momentum {

    private static DataProvider dataProvider;
    
    private static final int MOVING_AVERAGE_WINDOW_SIZE = 160;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        dataProvider = new H2DataProvider();

        Set<String> allIsins = dataProvider.getIsins();
        List<String> inclusions = AV.MES_PLACEMENTS_LIBERTE;

        //retirer les scpis et les fonds euro
        allIsins = allIsins.stream().filter(isin -> inclusions.contains(isin))
                .filter(isin -> !isin.contains("SCPI") && !isin.contains("QU")).collect(Collectors.toSet());
        HistoricalData data = dataProvider.getDataForIsins(allIsins);

        //for (int w = 2; w < 13; w++) {
        //   for (int f = 2; f < 7; f++) {
        doMomentum(data, MomentumStrategy.BEST_PERFORMANCE, ChronoUnit.WEEKS, 2, 3, false);
          //  }
        //}
        /*for(int r=1;r<50;r++){
         for (int w = 1; w < 10; w++) {
         doMomentum(data, MomentumStrategy.RANDOM, ChronoUnit.WEEKS, 3, 3, false);
         }
         }*/

        /*for (MomentumStrategy strategy : MomentumStrategy.values()) {
         for (int w = 1; w < 9+1; w++) {

         for (int f = 1; f < 10+1; f++) {
         doMomentum(data, strategy, ChronoUnit.WEEKS, w, f);
         }
         }
         }*/
        /*for (int w = 1; w < 6; w++) {
         doMomentum(data, MomentumStrategy.BEST_SHARP, ChronoUnit.MONTHS, w, 3);

         }*/
        //}
        //doMomentum(data,MomentumStrategy.BEST_SHARP,ChronoUnit.WEEKS,1,2);
        /*for (int f = 1; f < 20; f++) {
         for (MomentumStrategy strategy : MomentumStrategy.values()) {
         for (int w = 1; w < 4; w++) {
         doMomentum(data, strategy, ChronoUnit.WEEKS, w, f);
         }

         for (int m = 1; m < 6; m++) {
         doMomentum(data, strategy, ChronoUnit.MONTHS, m, f);
         }
         }
         }*/
    }

    private static void doMomentum(HistoricalData data, MomentumStrategy strategy, ChronoUnit unit, long qty, long maxNbFunds, boolean verbose) throws SQLException {

        DateBasedAllocation allocation = new DateBasedAllocation();


        //tous les mardi, on devrait avoir la maj des données du vendredi précédent.
        //on commence le mardi suivant la première phase d'observation (de durée paramètrable)
        LocalDate firstKey = data.series().stream().min((o1, o2) -> {
            return o1.getValue().firstDate().compareTo(o2.getValue().firstDate());
        }).get().getValue().firstDate().plus(qty, unit).plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        LocalDate lastKey = data.series().stream().min((o1, o2) -> {
            return o1.getValue().latestDate().compareTo(o2.getValue().latestDate());
        }).get().getValue().latestDate();

        /*
         tous les mardis  prendre les "meilleurs" fonds sur la période de vendredi de 2 semaines précédentes jusqu'au vendredi précédent
         */
        for (LocalDate tuesday = firstKey; tuesday.isBefore(lastKey); tuesday = tuesday.plus(qty, unit)) {

            LocalDate debutDePeriode = tuesday.minus(qty, unit).with(TemporalAdjusters.previous(DayOfWeek.FRIDAY));
            LocalDate finDePeriode = debutDePeriode.plus(qty, unit).plusDays(1);

            HistoricalData subData = data.subData(debutDePeriode, finDePeriode);

            //calculer les x meilleurs fonds sur la periode
            List<Map.Entry<String, DateBasedSerie>> bests = subData.series().stream()
                    .sorted(strategy.getComparator(data, qty, unit).reversed()).collect(Collectors.toList());

            //enlever ceux qui sont vides
            bests = bests.stream().filter((o) -> !o.getValue().getSerie().isEmpty()).collect(Collectors.toList());
            bests = bests.stream().limit(maxNbFunds).collect(Collectors.toList());

            FixedAllocation tuesdayAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);
 
            //retirer ceux dont la performance ne dépasse pas le moving average
            final LocalDate definitelyATuesday = tuesday;
            bests = bests.stream()
                    .filter((o) -> data.getFundData(o.getKey()).movingAverage(definitelyATuesday, MOVING_AVERAGE_WINDOW_SIZE) < data.getForAt(o.getKey(), definitelyATuesday))
                    .collect(Collectors.toList());

            for (Map.Entry<String, DateBasedSerie> best : bests) {
                String bestIsin = best.getKey();
                tuesdayAllocation.put(bestIsin, 1.0/maxNbFunds);
               
            }

            tuesdayAllocation.completeWith(HistoricalData.CASH);
 
            assert tuesdayAllocation.isValid();
            
            //arbitrer vers ce(s) fonds le mardi
            allocation.set(tuesday,tuesdayAllocation);
            //orders.put(tuesday, portfolio);

        }

        /*if (verbose) {
            for (Entry<LocalDate, Map<String, Double>> order : orders.entrySet()) {
                System.out.println(order.getKey() + " : " + order.getValue().keySet());
            }
        }*/
        
        LocalDate portfolioStart = allocation.firstOrder().plusDays(1);
        LocalDate portfolioEnd = allocation.lastOrder();
              
        Portfolio portfolio = new Portfolio(allocation);
        
        DateBasedSerie capital = portfolio.calculateCapital(portfolioStart, portfolioEnd, data);
        

        double annualReturn = capital.annualReturns();
        double yearlyVolatility = capital.yearlyVolatility();
        
        double sharp = BigDecimal.valueOf((annualReturn-2.0)/yearlyVolatility).setScale(2,RoundingMode.FLOOR).doubleValue();
        

        String result = ("momentum strategy " + strategy + " every " + qty + " " + unit + " with " + maxNbFunds + " funds :  sharp " + sharp + " perf  " + annualReturn + " volatility :" + yearlyVolatility);
        System.out.println(result);
    }

}
