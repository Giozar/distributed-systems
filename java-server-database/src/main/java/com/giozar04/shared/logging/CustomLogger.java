package com.giozar04.shared.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Clase para manejar el registro de eventos y mensajes en la aplicación.
 * Proporciona métodos para diferentes niveles de registro.
 */
public class CustomLogger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Registra un mensaje con el nivel especificado.
     *
     * @param level El nivel del mensaje.
     * @param message El mensaje a registrar.
     */
    private void log(String level, String message) {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + LocalDateTime.now().format(formatter) + "] [" + threadName + "] [" + level + "] " + message);
    }

    /**
     * Registra un mensaje informativo.
     *
     * @param message El mensaje a registrar.
     */
    public void info(String message) {
        log("INFO", message);
    }

    /**
     * Registra un mensaje de error.
     *
     * @param message El mensaje a registrar.
     */
    public void error(String message) {
        log("ERROR", message);
    }

    /**
     * Registra un mensaje de error junto con la excepción.
     *
     * @param message El mensaje a registrar.
     * @param error La excepción asociada.
     */
    public void error(String message, Throwable error) {
        String errorMessage = message + ": " + error.getMessage();
        log("ERROR", errorMessage);
        
        // Opcionalmente, podemos imprimir el stack trace completo
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : error.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        System.err.println(sb.toString());
    }

    /**
     * Registra un mensaje de advertencia.
     *
     * @param message El mensaje a registrar.
     */
    public void warn(String message) {
        log("WARN", message);
    }

    /**
     * Registra un mensaje de advertencia junto con la excepción.
     *
     * @param message El mensaje a registrar.
     * @param error La excepción asociada (puede ser null).
     */
    public void warn(String message, Throwable error) {
        if (error != null) {
            log("WARN", message + ": " + error.getMessage());
        } else {
            log("WARN", message);
        }
    }

    /**
     * Registra un mensaje de depuración.
     *
     * @param message El mensaje a registrar.
     */
    public void debug(String message) {
        log("DEBUG", message);
    }
}