package cyterdan.backtest.core.data.providers;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.postgresql.ds.PGPoolingDataSource;

/**
 * Postgres data source for web application
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
            URI postgresUri;
            try {
                //load db credentials from env vars

                postgresUri = new URI(System.getenv("DATABASE_URL"));
                source = new PGPoolingDataSource();
                source.setServerName(postgresUri.getHost());
                source.setDatabaseName(postgresUri.getPath().substring(1));
                
                source.setPortNumber(postgresUri.getPort());
                source.setDataSourceName(DATASOURCE_NAME);
                String[] userInfoParts = postgresUri.getUserInfo().split(":");
                source.setUser(userInfoParts[0]);
                source.setPassword(userInfoParts[1]);

            } catch (URISyntaxException ex) {
                Logger.getLogger(PostgresDataProvider.class.getName()).log(Level.SEVERE, null, ex);
            }

            source.setMaxConnections(10);
            source.setSsl(true);
            source.setSslfactory("org.postgresql.ssl.NonValidatingFactory");
            source.setInitialConnections(3);
            source.initialize();

        }
        return source;

    }

}
