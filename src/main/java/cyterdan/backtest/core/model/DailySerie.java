package cyterdan.backtest.core.model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

/**
 *
 * represent a daily temporal series
 *
 * @author cytermann
 */
public class DailySerie {

    private final SortedMap<LocalDate, Double> serie;

    public Double extractReturn(LocalDate from, LocalDate to) {
        try {
            if(!serie.containsKey(from)){
                return 0.0;
            }
            if(!serie.containsKey(to)){
                return 0.0;
            }
            return (double) (serie.get(to) - serie.get(from)) / serie.get(from);
        } catch (NullPointerException npe) {
            System.out.println(npe);
            throw new NullPointerException();
        }
    }

    public Double getValue(LocalDate date) {
        return serie.get(date);
    }

    public void setValue(LocalDate date, Double value) {
        serie.put(date, value);
    }

    public Double totalReturn() {
        return extractReturn(firstDate(), latestDate());
    }

    public Double movingAverage(LocalDate day, long nbDays) {

        LocalDate averageCalculationStartDay = day.minusDays(nbDays);
        if (firstDate().isAfter(averageCalculationStartDay)) {
            averageCalculationStartDay = firstDate();
        }
        if (day.isAfter(latestDate())) {
            System.err.println("something is not right");
            throw new IllegalStateException("can't calculate moving average");
        }

        OptionalDouble optional = serie.subMap(averageCalculationStartDay, day).values().stream().mapToDouble(a -> a).average();

        return optional.orElse(Double.MAX_VALUE);
    }

    public DailySerie() {
        this.serie = new TreeMap<>();
    }

    public DailySerie(SortedMap<LocalDate, Double> serie) {
        this.serie = serie;
    }

    public SortedMap<LocalDate, Double> getSerie() {
        return this.serie;
    }

    public LocalDate firstDate() {
        return serie.firstKey();
    }

    public LocalDate latestDate() {
        return serie.lastKey();
    }

    public DailySerie extract(LocalDate fromKey, LocalDate toKey) {
        SortedMap<LocalDate, Double> ret = serie.subMap(fromKey, toKey);
        return new DailySerie(ret);
    }

    private static double getStd(Collection<Double> v) {
        List<Double> values = v.stream().collect(Collectors.toList());
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < values.size(); i++) {
            returns.add((double) (values.get(i) - values.get(i - 1)) / values.get(i - 1));
        }
        double[] doubleValues = new double[returns.size()];
        for (int i = 0; i < returns.size(); i++) {
            doubleValues[i] = returns.get(i);
        }
        return new StandardDeviation().evaluate(doubleValues);
    }

    public Double volatility() {
        return getStd(serie.values());
    }

    public Double sharpRatio() {
        return (totalReturn() - 0.02) / volatility();
    }

    public double annualReturns() {
        double perfGlobal = totalReturn();
        long nbDays = latestDate().toEpochDay() - firstDate().toEpochDay();
        double perfAnnual = 100 * (Math.pow(1 + perfGlobal, 1.0 / ((float) nbDays / 365.25)) - 1);

        return perfAnnual;

    }

    public double maxMonthlyDrawdown() {

        LocalDate firstDate = firstDate();
        LocalDate firstFirstOfTheMonth = (firstDate.getDayOfMonth() == 1) ? firstDate : firstDate.plusMonths(1).withDayOfMonth(1);

        LocalDate lastDate = latestDate();
        LocalDate lastFirstOfTheMonth = lastDate.minusMonths(1).withDayOfMonth(1);
        SortedMap<LocalDate, Double> monthlyReturns = new TreeMap<>();
        LocalDate day = firstFirstOfTheMonth;
        while (day.isBefore(lastFirstOfTheMonth)) {
            SortedMap<LocalDate, Double> Kmonth = serie.subMap(day, day.plusMonths(1));
            LocalDate first = Kmonth.firstKey();
            LocalDate last = Kmonth.lastKey();
            Double vlFirst = Kmonth.get(first);
            Double vlLast = Kmonth.get(last);

            double ret = (vlLast - vlFirst) / vlFirst;
            monthlyReturns.put(day, ret);
            day = day.plusMonths(1);
        }

        double maxDrop = monthlyReturns.values().stream().mapToDouble(d->d).min().getAsDouble();
        return maxDrop;

    }

    public double yearlyVolatility() {

        LocalDate firstMonday = firstDate().with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        LocalDate lastFriday = latestDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.FRIDAY));

        LocalDate day = firstMonday;
        SortedMap<LocalDate, Double> weeklyReturns = new TreeMap<>();
        while (day.isBefore(lastFriday)) {
            SortedMap<LocalDate, Double> Kweek = serie.subMap(day, day.with(TemporalAdjusters.next(DayOfWeek.SATURDAY)));
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
        return yearlyVolatility;
    }

    public List<List> toJsArray() {
        return formatAsJsArray(serie);
    }

    /**
     * awful hack to transform a nice {date:value} map into a list of y/m/d as
     * required by highcharts input format
     *
     * @param K
     * @return
     */
    private static List<List> formatAsJsArray(SortedMap<LocalDate, Double> K) {
        double normal = K.get(K.firstKey());
        //formater l'historique
        List<List> history = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : K.entrySet()) {
            List<Object> list = new ArrayList<>();
            list.add(entry.getKey().getYear());
            list.add(entry.getKey().getMonthValue());
            list.add(entry.getKey().getDayOfMonth());

            list.add(entry.getValue() / normal);
            history.add(list);
        }
        return history;
    }

    double averageWeeklyChanges() {
        long sum = 0;
        long count = 0;
        LocalDate start = firstDate();
        LocalDate end = latestDate();
        for (LocalDate date = start; date.isBefore(end); date = date.plusWeeks(1)) {
            sum += this.serie.subMap(date, date.plusWeeks(1)).values().stream().distinct().count();
            count += 1;
        }
        return ((float) sum) / count;
    }

    public int numberOfFullYears() {
        LocalDate earliest = firstDate();
        LocalDate latest = latestDate();
        int startYear = earliest.getYear();
        if (earliest.isAfter(earliest.withMonth(Month.JANUARY.getValue()).withDayOfMonth(1))) {
            startYear++;
        }
        int endYear = latest.getYear();
        if (latest.isBefore(latest.withMonth(12).withDayOfMonth(31))) {
            endYear--;
        }
        return endYear - startYear + 1;

    }
}
