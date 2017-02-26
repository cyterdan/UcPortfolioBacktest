package cyterdan.backtest.data.providers;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.sql.DataSource;

/**
 * Interface for funds historical data provider
 * @author cytermann
 */
public interface DataProvider {

    public DataSource getDataSource() throws SQLException;

    public void logBacktest(String permalink, BigDecimal formattedPerf, BigDecimal formattedStd);

    public Map<String, SortedMap<LocalDate, Double>>
            getDataForIsins(Set<String> isinSet) throws SQLException;

    public Set<String> getIsins() throws SQLException;
   
}
