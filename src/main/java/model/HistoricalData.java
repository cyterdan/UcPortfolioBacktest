package model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Collectors;

/**
 *
 * @author cytermann
 */
public final class HistoricalData {

    HashMap<String, DateBasedSerie> history;
    public static final String CASH = "_CASH_";

    public HistoricalData(HashMap<String, DateBasedSerie> history) {
        this.history = history;
    }

    /**
     * retourne la performance moyenn d'un fond pour une periodicité donnée
     * entre deux dates
     *
     */
    public Double getFundAveragePeriodicalReturnBetween(String isin, ChronoUnit unit, long qty, LocalDate from, LocalDate to) {

        DateBasedSerie data = history.get(isin).extract(from, to);
        if (data.getSerie().isEmpty()) {
            return Double.MAX_VALUE;
        }
        List<Double> returns = new ArrayList<>();
        for (LocalDate d = data.firstDate(); d.isBefore(data.latestDate().minus(qty, unit)); d = d.plus(qty, unit)) {
            returns.add(data.extractReturn(d, d.plus(qty, unit)));
        }
        OptionalDouble average = returns.stream().mapToDouble(a -> a).average();
        if (!average.isPresent()) {
            return null;
        } else {
            return average.getAsDouble();
        }
    }

    public LocalDate usefulStart() {
        return this.history.values().stream()
                .map(serie -> serie.firstDate())
                .max((o1, o2) -> o1.compareTo(o2)).get();
    }

    public LocalDate usefulEnd() {
        return this.history.values().stream()
                .map(serie -> serie.latestDate())
                .min((o1, o2) -> o1.compareTo(o2)).get();
    }

    public Set<Map.Entry<String, DateBasedSerie>> series() {
        return history.entrySet();
    }

    public HistoricalData() {
        history = new HashMap<>();
    }

    public HistoricalData(Set<String> isins, ResultSet rs) throws SQLException {

        history = new HashMap<>();
        while (rs.next()) {
            LocalDate date = rs.getDate("date").toLocalDate();
            for (String isin : isins) {

                Double fundValue = rs.getDouble(isin);
                if (fundValue != 0) {
                    if (!containsFund(isin)) {
                        history.put(isin, new DateBasedSerie());
                    }
                    putForAt(isin, date, fundValue);
                }

            }

        }

        addCash();

    }

    public Double getForAt(String isin, LocalDate date) {
        return this.getFundData(isin).getSerie().get(date);
    }

    public Double putForAt(String isin, LocalDate date, Double value) {
        return this.getFundData(isin).getSerie().put(date, value);
    }

    public boolean containsFund(String isin) {
        return history.containsKey(isin);
    }

    public DateBasedSerie getFundData(String isin) {
        if (!history.containsKey(isin)) {
            history.put(isin, new DateBasedSerie());
        }
        return history.get(isin);
    }

    public DateBasedSerie put(String isin, DateBasedSerie serie) {
        return history.put(isin, serie);
    }

    public HistoricalData subData(LocalDate from, LocalDate to) {

        long nbDaysExpected = to.toEpochDay() - from.toEpochDay();

        HashMap<String, DateBasedSerie> filtered = (HashMap<String, DateBasedSerie>) history.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, o -> o.getValue().getSerie().subMap(from, to)))
                .entrySet().stream()
                .filter((o) -> !o.getValue().isEmpty())
                .filter(o -> o.getValue().size() == nbDaysExpected)
                .collect(Collectors.toMap(Map.Entry::getKey, o -> new DateBasedSerie(o.getValue())));
        ;
        return new HistoricalData(filtered);

    }

    public void extendsEuroFunds() {

        LocalDate allDataEnd = this.history.entrySet().stream().map((o) -> o.getValue().latestDate()).max((a, b) -> a.compareTo(b)).get();

        for (String isin : this.history.keySet()) {
            /* pour les fonds € , on extrapole l'historique */
            if (isin.startsWith("QU")) {

                LocalDate first = getFundData(isin).firstDate();
                LocalDate last = getFundData(isin).latestDate();
                long nbDays = last.toEpochDay() - first.toEpochDay();
                /*double pente = (data.get(isin).get(last) - data.get(isin).get(first)) / data.get(isin).get(first) / nbDays;
                 */
                double totalReturn = (getForAt(isin, last) - getForAt(isin, first)) / getForAt(isin, first);
                double dailyReturn = Math.pow((1 + totalReturn), 1.0 / nbDays) - 1;
                LocalDate beginning = LocalDate.of(2000, 1, 1);
                while (first.isAfter(beginning)) {
                    LocalDate before = first.minusDays(1);
                    putForAt(isin, before, getForAt(isin, first) / (1 + dailyReturn));
                    first = before;
                }
       
                //normaliser la série
                double startValue = getForAt(isin, beginning);
                for (LocalDate d = beginning; d.isBefore(last); d = d.plusDays(1)) {
                    putForAt(isin, d, 100 * getForAt(isin, d) / startValue);
                }
            }
        }
    }

    public void merge(HistoricalData cashData) {
        for (Entry<String, DateBasedSerie> entry : cashData.series()) {
            this.history.put(entry.getKey(), entry.getValue());
        }
    }

    private void addCash() {
        LocalDate allDataEnd = this.history.entrySet().stream().map((o) -> o.getValue().latestDate()).max((a, b) -> a.compareTo(b)).get();
        LocalDate allDataStart = this.history.entrySet().stream().map((o) -> o.getValue().firstDate()).min((a, b) -> a.compareTo(b)).get();

        //taux du "cash" 1%/an
        this.put(CASH, new DateBasedSerie());
        this.putForAt(CASH, allDataStart, 1.0);
        for (LocalDate day = allDataStart; day.isBefore(allDataEnd); day = day.plusDays(1)) {
            this.putForAt(CASH, day.plusDays(1), this.getForAt(CASH, day) * 1.00003);
        }

    }

}
