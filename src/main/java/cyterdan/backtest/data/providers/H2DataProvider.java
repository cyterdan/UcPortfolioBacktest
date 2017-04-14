package cyterdan.backtest.data.providers;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;

/**
 * Data provider used for dev and local analysis
 * @author cytermann
 */
public class H2DataProvider extends AbstractDataProvider implements DataProvider {

    private final JdbcConnectionPool connectionPool;

    private static final String dbPath = "/tmp/h2localdb;AUTO_SERVER=TRUE";
    private static final String user = "sa";
    private static final String password = "";

    public H2DataProvider() {
   
        this.connectionPool = JdbcConnectionPool.create("jdbc:h2:"+dbPath, user, password);
    }

    @Override
    public DataSource getDataSource() throws SQLException {
        return connectionPool;
    }
    
    @Override
    public void logBacktest(String permalink, Double formattedPerf, Double formattedStd) {
        //no logging for local database...
    }
    
    
    

}
