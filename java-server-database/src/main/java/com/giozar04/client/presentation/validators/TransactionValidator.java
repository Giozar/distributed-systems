package com.giozar04.client.presentation.validators;

import java.util.ArrayList;
import java.util.List;

/**
 * Clase para validar los campos del formulario de transacción.
 */
public class TransactionValidator {
    
    private final List<String> errors;
    
    /**
     * Constructor del validador.
     */
    public TransactionValidator() {
        this.errors = new ArrayList<>();
    }
    
    /**
     * Valida que un campo no esté vacío.
     * 
     * @param value valor del campo
     * @param fieldName nombre del campo para el mensaje de error
     * @return true si el campo es válido, false si está vacío
     */
    public boolean validateRequired(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            errors.add(fieldName + " es un campo requerido.");
            return false;
        }
        return true;
    }
    
    /**
     * Valida que un valor sea numérico.
     * 
     * @param value valor a validar
     * @param fieldName nombre del campo para el mensaje de error
     * @return true si el campo es numérico, false en caso contrario
     */
    public boolean validateNumeric(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Si no es requerido, no validamos
        }
        
        try {
            Double.valueOf(value.trim());
            return true;
        } catch (NumberFormatException e) {
            errors.add(fieldName + " debe ser un valor numérico válido.");
            return false;
        }
    }
    
    /**
     * Valida que un número sea positivo.
     * 
     * @param value valor a validar
     * @param fieldName nombre del campo para el mensaje de error
     * @return true si el número es positivo, false en caso contrario
     */
    public boolean validatePositiveNumber(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Si no es requerido, no validamos
        }
        
        try {
            double number = Double.parseDouble(value.trim());
            if (number <= 0) {
                errors.add(fieldName + " debe ser un valor positivo mayor que cero.");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            // Esta validación ya se hace en validateNumeric
            return false;
        }
    }
    
    /**
     * Verifica si hay errores de validación.
     * 
     * @return true si hay errores, false si todo es válido
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
    
    /**
     * Obtiene los errores de validación.
     * 
     * @return lista de mensajes de error
     */
    public List<String> getErrors() {
        return errors;
    }
    
    /**
     * Obtiene un mensaje de error formateado con todos los errores.
     * 
     * @return mensaje de error formateado
     */
    public String getErrorMessage() {
        StringBuilder sb = new StringBuilder("Por favor corrija los siguientes errores:\n\n");
        for (String error : errors) {
            sb.append("• ").append(error).append("\n");
        }
        return sb.toString();
    }
    
    /**
     * Limpia los errores almacenados.
     */
    public void clearErrors() {
        errors.clear();
    }
}