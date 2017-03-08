package cyterdan.backtest.app;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import cyterdan.backtest.utils.AV;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Iterator;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import model.allocation.AllocationRebalanceMode;
import model.allocation.DateBasedAllocation;
import model.DateBasedSerie;
import model.allocation.FixedAllocation;
import model.HistoricalData;
import model.MomentumStrategy;
import model.Portfolio;

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
    public static void main(String[] args) throws SQLException, Exception {

        dataProvider = new H2DataProvider();

        Set<String> allIsins = dataProvider.getIsins();
        List<String> inclusions = AV.MES_PLACEMENTS_LIBERTE;

        //garder que certains fonds
        //retirer les scpis et les fonds euro 
        allIsins = allIsins.stream().filter(isin -> inclusions.contains(isin))
                .filter(isin -> !isin.contains("SCPI") && !isin.contains("QU")).collect(Collectors.toSet());
        HistoricalData data = dataProvider.getDataForIsins(allIsins);

        doMomentum(data, MomentumStrategy.BEST_PERFORMANCE, ChronoUnit.WEEKS, 2, 3, true);

    }

    private static void doMomentum(HistoricalData data, MomentumStrategy strategy, ChronoUnit unit, long qty, long maxNbFunds, boolean verbose) throws SQLException, Exception {

        DateBasedAllocation allocation = new DateBasedAllocation();

        //tous les mardi, on devrait avoir la maj des données du vendredi précédent.
        //on commence le mardi suivant la première phase d'observation (de durée paramètrable)
        LocalDate firstKey = data.series().stream().min((o1, o2) -> {
            return o1.getValue().firstDate().compareTo(o2.getValue().firstDate());
        }).get().getValue().firstDate().plus(qty, unit).plusWeeks(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY));
        LocalDate lastKey = data.series().stream().max((o1, o2) -> {
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
                    .filter((o) -> !o.getKey().equals(HistoricalData.CASH))
                    .sorted(strategy.getComparator(data, qty, unit).reversed()).collect(Collectors.toList());

            bests = bests.stream().limit(maxNbFunds).collect(Collectors.toList());

            FixedAllocation tuesdayAllocation = new FixedAllocation(AllocationRebalanceMode.REBALANCE_NEVER);

            //retirer ceux dont la performance ne dépasse pas le moving average
            final LocalDate definitelyATuesday = tuesday;
            bests = bests.stream()
                    .filter((o) -> data.getFundData(o.getKey()).movingAverage(definitelyATuesday, MOVING_AVERAGE_WINDOW_SIZE) < data.getForAt(o.getKey(), definitelyATuesday))
                    .collect(Collectors.toList());

            for (Map.Entry<String, DateBasedSerie> best : bests) {
                String bestIsin = best.getKey();
                tuesdayAllocation.put(bestIsin, 1.0 / maxNbFunds);
            }

            tuesdayAllocation.completeWith(HistoricalData.CASH);

            if (!tuesdayAllocation.isValid()) {
                throw new Exception("allocation is not valid");
            }

            //arbitrer vers ce(s) fonds le mardi
            allocation.set(tuesday, tuesdayAllocation);
            //orders.put(tuesday, portfolio);

        }

        if (verbose) {
             allocation.print();
        }

        LocalDate portfolioStart = allocation.firstOrder().plusDays(1);
        LocalDate portfolioEnd = allocation.lastOrder();

        Portfolio portfolio = new Portfolio(allocation);

        DateBasedSerie capital = portfolio.calculateCapital(portfolioStart, portfolioEnd, data);

        double annualReturn = capital.annualReturns();
        double yearlyVolatility = capital.yearlyVolatility();

        double sharp = BigDecimal.valueOf((annualReturn - 2.0) / yearlyVolatility).setScale(2, RoundingMode.FLOOR).doubleValue();
        Iterator<LocalDate> iterator = allocation.dates().iterator();
        iterator.next();
        LocalDate start = iterator.next();
        double worst = 100.0;
        LocalDate worstDate = start;
        LocalDate previous = start;
        while(iterator.hasNext()){
            LocalDate date = iterator.next();
            if(iterator.hasNext()){
            double perf = capital.extractReturn(previous, date);
            if(worst>perf){
                worst = perf;
                worstDate = date;
            }
            //System.out.println(date+"\t"+perf );
            previous = date;
            }
        }
        System.out.println("worst drawdown="+worst+" @"+worstDate+" with "+allocation.getIsinsForDate(worstDate.minusDays(3)));
    

        String result = ("momentum strategy " + strategy + " every " + qty + " " + unit + " with " + maxNbFunds + " funds :  sharp " + sharp + " perf  " + annualReturn + " volatility :" + yearlyVolatility);
        System.out.println(result);
        Set<String> allusedIsin = allocation.dates().stream().map(date-> allocation.getIsinsForDate(date)).flatMap(Set::stream).collect(Collectors.toSet());
        for(LocalDate date = capital.latestDate();date.isAfter(capital.firstDate().minusDays(1));date=date.minusDays(1)){
            //System.out.println("_MOMENTUM_STRATEGIE_"+"|"+date+"|"+capital.getValue(date));
        }
    }
    
 

}
