package cyterdan.backtest.app;

import cyterdan.backtest.data.providers.DataProvider;
import cyterdan.backtest.data.providers.H2DataProvider;
import cyterdan.backtest.utils.Data;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.TemporalUnit;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import model.MomentumStrategy;
import model.Portfolio;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 *
 * @author cytermann
 */
public class Momentum {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws SQLException {

        DataProvider dataProvider = new H2DataProvider();

        Set<String> allIsins = dataProvider.getIsins();
        //retirer les scpis
        allIsins = allIsins.stream().filter(isin -> !isin.contains("SCPI")).collect(Collectors.toSet());
        Map<String, SortedMap<LocalDate, Double>> data = dataProvider.getDataForIsins(allIsins);

        for (int w = 1; w < 4; w++) {
            doMomentum(data, MomentumStrategy.BEST_PERFORMANCE, ChronoUnit.WEEKS, w, 3);
        }

        for (int m = 1; m < 12; m++) {
            doMomentum(data, MomentumStrategy.BEST_PERFORMANCE, ChronoUnit.MONTHS, m, 3);
        }

    }

    static Map<String, SortedMap<LocalDate, Double>> subData(Map<String, SortedMap<LocalDate, Double>> data, LocalDate from, LocalDate to) {
        return data.entrySet().stream().collect(Collectors.toMap((o) -> o.getKey(), (o) -> o.getValue().subMap(from, to)));
    }

    private static void doMomentum(Map<String, SortedMap<LocalDate, Double>> data, MomentumStrategy strategy, TemporalUnit unit, long qty, long maxNbFunds) {

        LocalDate firstKey = data.entrySet().stream().min((o1, o2) -> {
            return o1.getValue().firstKey().compareTo(o2.getValue().firstKey());
        }).get().getValue().firstKey();

        LocalDate lastKey = data.entrySet().stream().min((o1, o2) -> {
            return o1.getValue().lastKey().compareTo(o2.getValue().lastKey());
        }).get().getValue().lastKey();

        SortedMap<LocalDate, Map<String, Double>> orders = new TreeMap<>();

        for (LocalDate d = firstKey; d.isBefore(lastKey); d = d.plus(qty, unit)) {

            LocalDate finDePeriode = d.plus(qty, unit);
            Map<String, SortedMap<LocalDate, Double>> subData = subData(data, d, finDePeriode);
            //calculer les x meilleurs fonds sur la periode
            List<Map.Entry<String, SortedMap<LocalDate, Double>>> bests
                    = subData.entrySet().stream().sorted(strategy.getComparator()
                    ).limit(maxNbFunds).collect(Collectors.toList());

            Map<String, Double> portfolio = new HashMap<>();
            for (Map.Entry<String, SortedMap<LocalDate, Double>> best : bests) {
                String bestIsin = best.getKey();
                portfolio.put(bestIsin, 1.0 / bests.size());

            }

            //Double perf = (double)(best.getValue().get(best.getValue().lastKey())-best.getValue().get(best.getValue().firstKey()))/best.getValue().get(best.getValue().firstKey());
            //arbitrer vers ce(s) fonds à la fin de la periode
            orders.put(finDePeriode, portfolio);

        }

        SortedMap<LocalDate, Double> K = new TreeMap<>();
        K.put(firstKey.plus(qty, unit), 1.0);
        //now calculate the returns (starting one month + 1 day after firt choice)
        for (LocalDate d = firstKey.plus(qty, unit).plusDays(1); d.isBefore(lastKey); d = d.plusDays(1)) {

            double perf = 0.0;
            LocalDate lastFundDate = orders.headMap(d).lastKey();
            for (String isin : orders.get(lastFundDate).keySet()) {
                double endValue = data.get(isin).get(d);
                double startValue = data.get(isin).get(d.minusDays(1));
                perf += orders.get(lastFundDate).get(isin) * (endValue - startValue) / startValue;

            }
            K.put(d, K.get(d.minusDays(1)) * (1 + perf));

        }

        LocalDate firstMonday = K.firstKey().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate lastFriday = K.lastKey().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

        LocalDate day = firstMonday;
        SortedMap<LocalDate, Double> weeklyReturns = new TreeMap<>();
        while (day.isBefore(lastFriday)) {
            SortedMap<LocalDate, Double> Kweek = K.subMap(day, day.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)));
            LocalDate thisMonday = Kweek.firstKey();
            LocalDate thisSaturday = Kweek.lastKey();
            Double vlMonday = Kweek.get(thisMonday);
            Double vlSaturday = Kweek.get(thisSaturday);

            double ret = (vlSaturday - vlMonday) / vlMonday;
            weeklyReturns.put(day, ret);
            day = day.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        }
        List<Double> values = weeklyReturns.values().stream().collect(Collectors.toList());
        double[] doubleValues = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            doubleValues[i] = values.get(i);
        }
        StandardDeviation stdev = new StandardDeviation();

        stdev.clear();
        double weeklyVolatility = stdev.evaluate(doubleValues);

        double yearlyVolatility = Math.sqrt(52) * weeklyVolatility * 100;
        double perfGlobal = (K.get(K.lastKey()) - K.get(K.firstKey())) / K.get(K.firstKey());
        long nbDays = K.lastKey().toEpochDay() - K.firstKey().toEpochDay();

        double perfAnnual = 100 * (Math.pow(1 + perfGlobal, 1.0 / ((float) nbDays / 365.25)) - 1);
        System.out.println("momentum strategy " + strategy + " every " + qty + " " + unit + " : perf  " + perfAnnual + " volatility :" + yearlyVolatility);
    }

}
