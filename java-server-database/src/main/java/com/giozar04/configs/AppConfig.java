package com.giozar04.configs;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = AppConfig.class.getClassLoader()
                .getResourceAsStream("com/giozar04/resources/config.properties")) {
            if (input == null) {
                throw new IOException("No se encontró config.properties en el classpath en com/giozar04/resources/");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("No se pudo cargar la configuración desde config.properties", e);
        }
    }

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }
}
