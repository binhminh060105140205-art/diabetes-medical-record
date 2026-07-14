package dal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Temporary compatibility base for legacy DAOs during repository migration. */
public class DBContext {
    protected Connection connection;

    public DBContext() {
        try {
            String url = value("DB_URL", "spring.datasource.url",
                    "jdbc:postgresql://localhost:5432/diabetes_medical_record");
            String user = value("DB_USERNAME", "spring.datasource.username", "postgres");
            String pass = value("DB_PASSWORD", "spring.datasource.password", "");
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, user, pass);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(DBContext.class.getName()).log(Level.SEVERE,
                    "Cannot connect to PostgreSQL. Check DB_URL, DB_USERNAME and DB_PASSWORD.", ex);
            throw new IllegalStateException("Cannot connect to PostgreSQL. Check database configuration.", ex);
        }
    }

    private static String value(String environmentName, String propertyName, String defaultValue) {
        String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) return environmentValue;
        return System.getProperty(propertyName, defaultValue);
    }
}
