package cyterdan.backtest.core.data.providers;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.sql.DataSource;
import cyterdan.backtest.core.model.HistoricalData;

/**
 * Interface for funds historical data provider
 * @author cytermann
 */
public interface DataProvider {

    public Boolean isOk() ;
    
    public DataSource getDataSource() throws SQLException;

    public void logBacktest(String permalink, Double formattedPerf, Double formattedStd);

    public HistoricalData
            getDataForIsins(Set<String> isinSet) throws SQLException;

    public Set<String> getIsins() throws SQLException;
   
}
