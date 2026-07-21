package vn.diabetes.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/** Opens the first pooled connection after startup without delaying the health endpoint. */
@Component
@Lazy(false)
public class DatabaseWarmup implements ApplicationListener<ApplicationReadyEvent> {
    private final ObjectProvider<JdbcClient> jdbcProvider;

    public DatabaseWarmup(ObjectProvider<JdbcClient> jdbcProvider) {
        this.jdbcProvider = jdbcProvider;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        Thread warmup = new Thread(() -> {
            try {
                jdbcProvider.getObject().sql("SELECT 1").query(Integer.class).single();
            } catch (RuntimeException ignored) {
                // A later request or /ready check will retry when the database is available.
            }
        }, "database-warmup");
        warmup.setDaemon(true);
        warmup.start();
    }
}
