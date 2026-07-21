package dal;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sql.DataSource;

/** Temporary compatibility base for legacy DAOs during repository migration. */
public class DBContext {
    private static final Logger LOGGER = Logger.getLogger(DBContext.class.getName());
    protected Connection connection;
    private static final ThreadLocal<Connection> REQUEST_CONNECTION = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> REQUEST_ACTIVE = ThreadLocal.withInitial(() -> false);
    private static volatile DataSource dataSource;

    public DBContext() {
        try {
            Connection requestConnection = REQUEST_CONNECTION.get();
            if (REQUEST_ACTIVE.get() && requestConnection != null
                    && !requestConnection.isClosed()) {
                connection = requestConnection;
                return;
            }

            connection = openConnection();
            if (REQUEST_ACTIVE.get()) REQUEST_CONNECTION.set(connection);
        } catch (SQLException ex) {
            LOGGER.log(Level.SEVERE,
                    "Cannot connect to PostgreSQL. Check DB_URL, DB_USERNAME and DB_PASSWORD.", ex);
            throw new IllegalStateException("Cannot connect to PostgreSQL. Check database configuration.", ex);
        }
    }

    /** Called by the servlet filter so legacy DAOs cannot leak connections across requests. */
    public static void beginRequest() {
        closeRequestConnections();
        REQUEST_ACTIVE.set(true);
    }

    public static void closeRequestConnections() {
        Connection connection = REQUEST_CONNECTION.get();
        if (connection != null) {
            try {
                if (!connection.isClosed()) connection.close();
            } catch (SQLException ex) {
                LOGGER.log(Level.WARNING, "Cannot close database connection", ex);
            }
        }
        REQUEST_CONNECTION.remove();
        REQUEST_ACTIVE.remove();
    }

    public static void setDataSource(DataSource configuredDataSource) {
        dataSource = configuredDataSource;
    }

    /** Log the technical cause and fail the request instead of returning fake empty data. */
    protected IllegalStateException databaseError(String operation, SQLException cause) {
        LOGGER.log(Level.SEVERE, "Database operation failed: " + operation, cause);
        return new IllegalStateException("Không thể xử lý dữ liệu. Vui lòng thử lại sau.", cause);
    }

    private Connection openConnection() throws SQLException {
        DataSource configured = dataSource;
        long startedAt = System.nanoTime();
        try {
            if (configured != null) return configured.getConnection();

            return DriverManager.getConnection(
                    value("DB_URL", "spring.datasource.url",
                            "jdbc:postgresql://localhost:5432/diabetes_medical_record"),
                    value("DB_USERNAME", "spring.datasource.username", "postgres"),
                    value("DB_PASSWORD", "spring.datasource.password", ""));
        } finally {
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            if (elapsedMs >= 250) {
                String poolState = "";
                if (configured instanceof HikariDataSource hikari
                        && hikari.getHikariPoolMXBean() != null) {
                    var pool = hikari.getHikariPoolMXBean();
                    poolState = " (active=" + pool.getActiveConnections()
                            + ", idle=" + pool.getIdleConnections()
                            + ", total=" + pool.getTotalConnections()
                            + ", waiting=" + pool.getThreadsAwaitingConnection() + ")";
                }
                LOGGER.warning("Slow database connection acquisition: " + elapsedMs
                        + " ms via " + (configured == null
                                ? "DriverManager" : configured.getClass().getName()) + poolState);
            }
        }
    }

    private static String value(String environmentName, String propertyName, String defaultValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue;
        return System.getProperty(propertyName, defaultValue);
    }
}
