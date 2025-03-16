package com.giozar04.configs;

public class DatabaseConfig {
    private final String host;
    private final String port;
    private final String name;
    private final String username;
    private final String password;

    public DatabaseConfig() {
        this.host = AppConfig.getProperty("database.host");
        this.port = AppConfig.getProperty("database.port");
        this.name = AppConfig.getProperty("database.name");
        this.username = AppConfig.getProperty("database.username");
        this.password = AppConfig.getProperty("database.password");
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
