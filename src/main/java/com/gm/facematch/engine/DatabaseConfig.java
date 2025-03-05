package com.gm.facematch.engine;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;

public class DatabaseConfig {

    private static HikariDataSource dataSource;

    public static DataSource getDataSource() {
        if (dataSource == null) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(System.getenv("SPRING_DATASOURCE_URL"));
            config.setUsername(System.getenv("SPRING_DATASOURCE_USERNAME"));
            config.setPassword(System.getenv("SPRING_DATASOURCE_PASSWORD"));
            config.setDriverClassName("org.postgresql.Driver");


//            // Connection Pool Settings
//            config.setMaximumPoolSize(5);
//            config.setMinimumIdle(1);
//            config.setIdleTimeout(30000);
//            config.setMaxLifetime(60000);
//            config.setConnectionTimeout(30000);

            dataSource = new HikariDataSource(config);
        }
        return dataSource;
    }
}

