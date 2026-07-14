package vn.diabetes.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/** Makes Spring's resolved settings available to legacy DAO/util classes during migration. */
@Component
public class LegacyRuntimeProperties implements ApplicationRunner {
    private final Environment environment;

    public LegacyRuntimeProperties(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        copy("spring.datasource.url");
        copy("spring.datasource.username");
        copy("spring.datasource.password");
        copy("app.upload-dir");
        copy("app.openai.api-key");
    }

    private void copy(String key) {
        String value = environment.getProperty(key);
        if (value != null) System.setProperty(key, value);
    }
}
