package com.architect.interviewportal.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${DATABASE_URL:postgresql://javaprep_user:2ZytgjY0mCr1dNJzEmSildzD33mZ7Ij5@dpg-dbiaam67bikc73ajhe80-a.singapore-postgres.render.com/javaprep}")
    private String databaseUrlEnv;

    @Value("${SPRING_DATASOURCE_URL:jdbc:postgresql://dpg-dbiaam67bikc73ajhe80-a.singapore-postgres.render.com:5432/javaprep?sslmode=require}")
    private String springDatasourceUrl;

    @Value("${DB_HOST:dpg-dbiaam67bikc73ajhe80-a}")
    private String dbHost;

    @Value("${DB_PORT:5432}")
    private String dbPort;

    @Value("${DB_NAME:javaprep}")
    private String dbName;

    @Value("${DB_USER:javaprep_user}")
    private String dbUser;

    @Value("${DB_PASSWORD:2ZytgjY0mCr1dNJzEmSildzD33mZ7Ij5}")
    private String dbPassword;

    @Value("${spring.datasource.username:javaprep_user}")
    private String defaultUsername;

    @Value("${spring.datasource.password:2ZytgjY0mCr1dNJzEmSildzD33mZ7Ij5}")
    private String defaultPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = (springDatasourceUrl != null && !springDatasourceUrl.isBlank()) 
                ? springDatasourceUrl 
                : databaseUrlEnv;

        // Priority 1: Direct PostgreSQL JDBC URL
        if (dbUrl != null && !dbUrl.isBlank() && dbUrl.startsWith("jdbc:postgresql:")) {
            try {
                log.info("Connecting to Render PostgreSQL via JDBC: {}", dbUrl.split("\\?")[0]);
                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(dbUrl);
                config.setUsername(defaultUsername != null ? defaultUsername : dbUser);
                config.setPassword(defaultPassword != null ? defaultPassword : dbPassword);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(5);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(15000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                return new HikariDataSource(config);
            } catch (Exception e) {
                log.warn("PostgreSQL connection error: {}, falling back to local H2: {}", dbUrl, e.getMessage());
            }
        }

        // Priority 2: Parse postgres:// or postgresql:// from Render
        if (dbUrl != null && !dbUrl.isBlank() && (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://"))) {
            try {
                log.info("Configuring PostgreSQL from Render DATABASE_URL...");
                URI uri = new URI(dbUrl.replace("postgres://", "postgresql://"));
                String userInfo = uri.getUserInfo();
                String username = defaultUsername;
                String password = defaultPassword;
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    username = parts[0];
                    password = parts[1];
                }

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 5432;
                String path = uri.getPath();
                String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + "?sslmode=require";

                HikariConfig config = new HikariConfig();
                config.setJdbcUrl(jdbcUrl);
                config.setUsername(username);
                config.setPassword(password);
                config.setDriverClassName("org.postgresql.Driver");
                config.setMaximumPoolSize(5);
                config.setMinimumIdle(1);
                config.setConnectionTimeout(15000);
                config.setIdleTimeout(600000);
                config.setMaxLifetime(1800000);
                log.info("Configured Render PostgreSQL DataSource for host: {}, port: {}", host, port);
                return new HikariDataSource(config);
            } catch (Exception e) {
                log.warn("Failed to connect to Render PostgreSQL: {}, falling back to H2", e.getMessage());
            }
        }

        // Priority 3: Fallback for offline testing
        log.info("Initializing embedded H2 database (offline fallback)...");
        HikariConfig h2Config = new HikariConfig();
        h2Config.setJdbcUrl("jdbc:h2:file:./data/javaprep;DB_CLOSE_DELAY=-1;AUTO_SERVER=TRUE");
        h2Config.setUsername("sa");
        h2Config.setPassword("");
        h2Config.setDriverClassName("org.h2.Driver");
        return new HikariDataSource(h2Config);
    }
}
