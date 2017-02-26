package cyterdan.backtest.data.providers;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcConnectionPool;
import org.rapidoid.sql.JDBC;

/**
 *
 * @author cytermann
 */
public class H2DataProvider extends AbstractDataProvider implements DataProvider {

    private final JdbcConnectionPool connectionPool;

    private static final String dbPath = "/tmp/h2localdb;AUTO_SERVER=TRUE";
    private static final String user = "sa";
    private static final String password = "sa";

    public H2DataProvider() {
   
        this.connectionPool = JdbcConnectionPool.create("jdbc:h2:"+dbPath, user, password);
    }

    @Override
    public DataSource getDataSource() throws SQLException {
        return connectionPool;
    }
    
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
    }

}
