package vn.diabetes.config;

import dal.DBContext;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

/** Gives legacy JDBC DAOs the same Hikari pool managed by Spring Boot. */
@Component
public class LegacyDataSourceBridge {
    public LegacyDataSourceBridge(DataSource dataSource) {
        DBContext.setDataSource(dataSource);
    }
}
