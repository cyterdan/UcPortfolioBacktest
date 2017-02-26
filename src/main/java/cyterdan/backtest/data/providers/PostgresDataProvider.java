
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
        return source;
    
    }
    
    
    
}
