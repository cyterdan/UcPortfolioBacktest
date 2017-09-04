
package cyterdan.backtest.data.providers;

import java.sql.SQLException;
import javax.sql.DataSource;
import org.postgresql.ds.PGPoolingDataSource;

/**
 *
 * @author cytermann
 */
public class PostgresDataProvider extends AbstractDataProvider implements DataProvider {

    private static final String DATASOURCE_NAME = "detna67j3hmevq";

    @Override
    public DataSource getDataSource() throws SQLException {
           PGPoolingDataSource source = PGPoolingDataSource.getDataSource(DATASOURCE_NAME);
        if (source == null) {
            source = new PGPoolingDataSource();
            source.setDataSourceName(DATASOURCE_NAME);

            
            //load db credentials from env vars
            source.setServerName(System.getenv("POSTGRESQL_HOST"));
            source.setDatabaseName(System.getenv("POSTGRESQL_DB"));
            source.setUser( System.getenv("POSTGRESL_USER"));
            source.setPassword( System.getenv("POSTGRESQL_PASS"));
            source.setMaxConnections(10);
            source.setSsl(true);
            source.setSslfactory("org.postgresql.ssl.NonValidatingFactory");
            source.setInitialConnections(3);
            source.initialize();

        }
        return source;
    
    }
    
    
    
}
