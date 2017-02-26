package cyterdan.backtest.data.providers;

import cyterdan.backtest.utils.Data;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.postgresql.ds.PGPoolingDataSource;

/**
 *
 * @author cytermann
 */
public abstract class AbstractDataProvider implements DataProvider {

    @Override
    public void logBacktest(String permalink, BigDecimal formattedPerf, BigDecimal formattedStd) {


        Connection conn = null;
        try {
            conn = this.getDataSource().getConnection();
            String insertQuery = "INSERT INTO portfolio_log\n"
                    + "    (portfolio, perf,std)\n"
                    + "SELECT '" + permalink + "', " + formattedPerf.toString() + "," + formattedStd.toString() + "  \n"
                    + "WHERE\n"
                    + "    NOT EXISTS (\n"
                    + "        SELECT portfolio FROM portfolio_log WHERE portfolio='" + permalink + "'\n"
                    + "    );";
            conn.createStatement().executeUpdate(insertQuery);
        } catch (SQLException ex) {
            //dont really care if log doesnt work
            System.err.println(ex);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }

    }

    @Override
    public Map<String, SortedMap<LocalDate, Double>>
            getDataForIsins(Set<String> isinSet) throws SQLException {

        Connection conn = null;
        Map<String, SortedMap<LocalDate, Double>> data = new HashMap<>();
        try {
            conn = getDataSource().getConnection();
            String isins = String.join(",", isinSet);
            ResultSet rs = conn.createStatement().executeQuery(String.format("select date,%s  from funds", isins));

            while (rs.next()) {
                LocalDate date = rs.getDate("date").toLocalDate();
                for (String isin : isinSet) {

                    Double fundValue = rs.getDouble(isin);
                    if (fundValue != 0) {
                        if (!data.containsKey(isin)) {
                            data.put(isin, new TreeMap<>());
                        }
                        data.get(isin).put(date, fundValue);
                    }

                }

            }
            for (String isin : isinSet) {
                /* pour les fonds € , on extrapole l'historique */
                if (isin.startsWith("QU")) {

                    LocalDate first = data.get(isin).firstKey();
                    LocalDate last = data.get(isin).lastKey();
                    long nbDays = last.toEpochDay() - first.toEpochDay();
                    /*double pente = (data.get(isin).get(last) - data.get(isin).get(first)) / data.get(isin).get(first) / nbDays;
                     */
                    double totalReturn = (data.get(isin).get(last) - data.get(isin).get(first)) / data.get(isin).get(first);
                    double dailyReturn = Math.pow((1 + totalReturn), 1.0 / nbDays) - 1;
                    LocalDate beginning = LocalDate.of(2000, 1, 1);
                    while (first.isAfter(beginning)) {
                        LocalDate before = first.minusDays(1);
                        data.get(isin).put(before, data.get(isin).get(first) / (1 + dailyReturn));
                        first = before;
                    }
                    //normaliser la série
                    double startValue = data.get(isin).get(beginning);
                    for (LocalDate d = beginning; d.isBefore(last); d = d.plusDays(1)) {
                        data.get(isin).put(d, 100 * data.get(isin).get(d) / startValue);
                    }
                }
            }

        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                }
            }
        }
        return data;

    }

    @Override
    public Set<String> getIsins() throws SQLException {

        Set<String> isins = new HashSet<>();

        Connection conn = getDataSource().getConnection();
        ResultSet rs = conn.createStatement().executeQuery("select * from public.funds limit 1");
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 2; i <= metaData.getColumnCount(); i++) {
            String isin = metaData.getColumnName(i);
            if (!isin.startsWith("_")) {
                isins.add(isin);
            }
        }

        return isins;
    }

}
