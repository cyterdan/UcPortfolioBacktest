package cyterdan.backtest.utils;

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
import org.rapidoid.commons.Env;
import org.rapidoid.config.Conf;

/**
 *
 * @author cytermann
 */
public final class Data {

    private static final String DATASOURCE_NAME = "detna67j3hmevq";

    public static void initDataSource() throws SQLException {

        
        PGPoolingDataSource source = PGPoolingDataSource.getDataSource(DATASOURCE_NAME);
        if (source == null) {
            source = new PGPoolingDataSource();
            source.setDataSourceName(DATASOURCE_NAME);

            source.setServerName("ec2-54-235-120-39.compute-1.amazonaws.com");
            source.setDatabaseName("detna67j3hmevq");
            source.setUser("pmsyfqcauhvlco");
            source.setPassword("9f24a8f2548a78eb3b62e5e0591929f5b2e6e48a119be7b7d0ad20a3f48f5f83");
            source.setMaxConnections(10);
            source.setSsl(true);
            source.setSslfactory("org.postgresql.ssl.NonValidatingFactory");
            source.setInitialConnections(3);
            source.initialize();

        }
    }

    public static void logBacktest(String permalink, BigDecimal formattedPerf, BigDecimal formattedStd)  {
        try {
            Data.initDataSource();
        } catch (SQLException ex) {
            Logger.getLogger(Data.class.getName()).log(Level.SEVERE, null, ex);
        }

        Connection conn = null;
        try {
            conn = PGPoolingDataSource.getDataSource(DATASOURCE_NAME).getConnection();
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

    public static Map<String, SortedMap<LocalDate, Double>>
            getDataForIsins(Set<String> isinSet) throws SQLException {
                 Data.initDataSource();

        Connection conn = null;
        Map<String, SortedMap<LocalDate, Double>> data = new HashMap<>();
        try {
            conn = PGPoolingDataSource.getDataSource(DATASOURCE_NAME).getConnection();
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
            
            
    public static Set<String> getIsins() throws SQLException {
        Data.initDataSource();

        Connection conn = null;
        Set<String> isins = new HashSet<>();

        conn = PGPoolingDataSource.getDataSource(DATASOURCE_NAME).getConnection();
        ResultSet rs = conn.createStatement().executeQuery("select * from funds limit 1");
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 2; i <= metaData.getColumnCount(); i++) {
         String isin = metaData.getColumnName(i);
         if(!isin.startsWith("_")){
             isins.add(isin);
         }
        }

        return isins;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }


}
