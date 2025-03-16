package com.giozar04.configs;

public class ServerConfig {
    private final String host;
    private final int port;

    public ServerConfig() {
        this.host = AppConfig.getProperty("server.host");
        this.port = Integer.parseInt(AppConfig.getProperty("server.port"));
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
