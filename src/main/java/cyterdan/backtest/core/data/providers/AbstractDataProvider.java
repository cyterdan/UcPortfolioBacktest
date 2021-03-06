package cyterdan.backtest.core.data.providers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import cyterdan.backtest.core.model.HistoricalData;

/**
 *
 * @author cytermann
 */
public abstract class AbstractDataProvider implements DataProvider {

    @Override
    public void logBacktest(String permalink, Double formattedPerf, Double formattedStd) {

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
    public HistoricalData
            getDataForIsins(Set<String> isinSet) throws SQLException {
        HistoricalData data = null;
        Connection conn = null;
        try {
            conn = getDataSource().getConnection();
            String isins = String.join(",", isinSet);
            ResultSet rs = conn.createStatement().executeQuery(String.format("select date,%s  from funds", isins));

            data = new HistoricalData(isinSet, rs);

            data.extendsEuroFunds();

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

    @Override
    public Boolean isOk() {
        try {
            //check connection
            Boolean isValid = this.getDataSource().getConnection().isValid(5);
            if (!isValid) {
                return isValid;
            }
            //check data
            getDataSource().getConnection().createStatement().executeQuery("select * from public.funds limit 1");
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
            return false;
        }
        return true;

    }

}
