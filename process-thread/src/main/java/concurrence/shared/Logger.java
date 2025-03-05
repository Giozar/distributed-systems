package concurrence.shared;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private void log(String level, String  message) {
        System.out.println("["+ LocalDateTime.now().format(formatter) + "] [" + level + "] " + message);
    }

    public void info(String message) {
       log("INFO", message);
    }

    public void error( String message, Throwable error){
        log("ERROR", message +  ": " + error.getMessage());
        error.printStackTrace();
    }
}
