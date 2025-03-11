package com.giozar04.servers.domain.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Representa un mensaje que se intercambia entre el cliente y el servidor.
 * Es serializable para permitir su transmisión a través de la red.
 */
public class Message implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    // Tipo de mensaje (comando)
    private String type;
    
    // Contenido principal del mensaje
    private String content;
    
    // Datos adicionales que pueden ser necesarios para el procesamiento
    private Map<String, Object> data;
    
    // Estado del mensaje (éxito, error, etc.)
    private Status status;
    
    /**
     * Enumeración para representar el estado de un mensaje.
     */
    public enum Status {
        SUCCESS, 
        ERROR,
        PENDING
    }
    
    /**
     * Constructor por defecto.
     */
    public Message() {
        this.data = new HashMap<>();
        this.status = Status.PENDING;
    }
    
    /**
     * Constructor con tipo y contenido.
     *
     * @param type El tipo de mensaje.
     * @param content El contenido del mensaje.
     */
    public Message(String type, String content) {
        this();
        this.type = type;
        this.content = content;
    }
    
    /**
     * Constructor completo.
     *
     * @param type El tipo de mensaje.
     * @param content El contenido del mensaje.
     * @param data Datos adicionales.
     * @param status El estado del mensaje.
     */
    public Message(String type, String content, Map<String, Object> data, Status status) {
        this.type = type;
        this.content = content;
        this.data = data != null ? data : new HashMap<>();
        this.status = status;
    }
    
    /**
     * @return El tipo de mensaje.
     */
    public String getType() {
        return type;
    }
    
    /**
     * @param type El tipo de mensaje a establecer.
     */
    public void setType(String type) {
        this.type = type;
    }
    
    /**
     * @return El contenido del mensaje.
     */
    public String getContent() {
        return content;
    }
    
    /**
     * @param content El contenido a establecer.
     */
    public void setContent(String content) {
        this.content = content;
    }
    
    /**
     * @return Los datos adicionales del mensaje.
     */
    public Map<String, Object> getData() {
        return data;
    }
    
    /**
     * @param data Los datos a establecer.
     */
    public void setData(Map<String, Object> data) {
        this.data = data != null ? data : new HashMap<>();
    }
    
    /**
     * Agrega un dato al mapa de datos.
     *
     * @param key La clave del dato.
     * @param value El valor del dato.
     */
    public void addData(String key, Object value) {
        this.data.put(key, value);
    }
    
    /**
     * Obtiene un dato específico del mapa de datos.
     *
     * @param key La clave del dato.
     * @return El valor asociado, o null si no existe.
     */
    public Object getData(String key) {
        return this.data.get(key);
    }
    
    /**
     * @return El estado del mensaje.
     */
    public Status getStatus() {
        return status;
    }
    
    /**
     * @param status El estado a establecer.
     */
    public void setStatus(Status status) {
        this.status = status;
    }
    
    /**
     * Crea un mensaje de éxito.
     *
     * @param type El tipo de mensaje.
     * @param content El contenido del mensaje.
     * @return Un nuevo mensaje con estado SUCCESS.
     */
    public static Message createSuccessMessage(String type, String content) {
        Message message = new Message(type, content);
        message.setStatus(Status.SUCCESS);
        return message;
    }
    
    /**
     * Crea un mensaje de error.
     *
     * @param type El tipo de mensaje.
     * @param errorMessage El mensaje de error.
     * @return Un nuevo mensaje con estado ERROR.
     */
    public static Message createErrorMessage(String type, String errorMessage) {
        Message message = new Message(type, errorMessage);
        message.setStatus(Status.ERROR);
        return message;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", content='" + content + '\'' +
                ", data=" + data +
                ", status=" + status +
                '}';
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(type, message.type) &&
                Objects.equals(content, message.content) &&
                Objects.equals(data, message.data) &&
                status == message.status;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(type, content, data, status);
    }
}