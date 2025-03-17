package com.giozar04.client.presentation.components;

import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/**
 * Componente para seleccionar fechas.
 */
public class DatePickerComponent extends JPanel {
    private static final long serialVersionUID = 1L;
    
    private final JSpinner yearSpinner;
    private final JComboBox<String> monthCombo;
    private final JSpinner daySpinner;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    
    // Meses para el combobox
    private static final String[] MONTHS = {
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    };
    
    // Días por mes (para validación)
    private static final int[] DAYS_IN_MONTH = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };

    /**
     * Constructor del componente de selección de fechas.
     */
    public DatePickerComponent() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        // Obtenemos la fecha actual
        LocalDate today = LocalDate.now();
        
        // Componentes de fecha
        add(new JLabel("Día:"));
        daySpinner = new JSpinner(new SpinnerNumberModel(
                today.getDayOfMonth(), 1, 31, 1));
        daySpinner.setEditor(new JSpinner.NumberEditor(daySpinner, "00"));
        add(daySpinner);
        
        add(new JLabel("Mes:"));
        monthCombo = new JComboBox<>(MONTHS);
        monthCombo.setSelectedIndex(today.getMonthValue() - 1);
        add(monthCombo);
        
        add(new JLabel("Año:"));
        yearSpinner = new JSpinner(new SpinnerNumberModel(
                today.getYear(), 1900, 2100, 1));
        add(yearSpinner);
        
        // Ajustar el ancho del combo
        monthCombo.setPrototypeDisplayValue("Septiembre");
        
        // Añadir listener para validar días según el mes seleccionado
        monthCombo.addActionListener(e -> adjustDaysInMonth());
    }
    
    /**
     * Ajusta el número máximo de días en el spinner de días según el mes seleccionado.
     */
    private void adjustDaysInMonth() {
        int month = monthCombo.getSelectedIndex();
        int year = (Integer) yearSpinner.getValue();
        int maxDays = DAYS_IN_MONTH[month];
        
        // Febrero en año bisiesto
        if (month == 1 && isLeapYear(year)) {
            maxDays = 29;
        }
        
        int currentDay = (Integer) daySpinner.getValue();
        SpinnerNumberModel model = (SpinnerNumberModel) daySpinner.getModel();
        model.setMaximum(maxDays);
        
        // Ajustar el día si es mayor que el máximo para el mes
        if (currentDay > maxDays) {
            daySpinner.setValue(maxDays);
        }
    }
    
    /**
     * Determina si un año es bisiesto.
     */
    private boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * Establece una fecha en el componente.
     * 
     * @param date la fecha a establecer en formato ZonedDateTime
     */
    public void setDate(ZonedDateTime date) {
        if (date != null) {
            yearSpinner.setValue(date.getYear());
            monthCombo.setSelectedIndex(date.getMonthValue() - 1);
            daySpinner.setValue(date.getDayOfMonth());
            adjustDaysInMonth();
        } else {
            // Usar la fecha actual
            LocalDate today = LocalDate.now();
            yearSpinner.setValue(today.getYear());
            monthCombo.setSelectedIndex(today.getMonthValue() - 1);
            daySpinner.setValue(today.getDayOfMonth());
        }
    }

    /**
     * Obtiene la fecha seleccionada en formato ZonedDateTime.
     * 
     * @return la fecha seleccionada con la hora actual
     */
    public ZonedDateTime getDate() {
        int year = (Integer) yearSpinner.getValue();
        int month = monthCombo.getSelectedIndex() + 1;
        int day = (Integer) daySpinner.getValue();
        
        // Validar la fecha (especialmente para febrero)
        adjustDaysInMonth();
        
        return LocalDate.of(year, month, day)
                .atStartOfDay(ZoneId.systemDefault());
    }

    /**
     * Obtiene la fecha en formato ISO para ser almacenada.
     * 
     * @return la fecha en formato ISO
     */
    public String getISODate() {
        return getDate().format(FORMATTER);
    }

    /**
     * Restablece la fecha al día actual.
     */
    public void clear() {
        LocalDate today = LocalDate.now();
        yearSpinner.setValue(today.getYear());
        monthCombo.setSelectedIndex(today.getMonthValue() - 1);
        daySpinner.setValue(today.getDayOfMonth());
    }

    /**
     * Establece si el componente está habilitado o no.
     * 
     * @param enabled true para habilitar, false para deshabilitar
     */
    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        yearSpinner.setEnabled(enabled);
        monthCombo.setEnabled(enabled);
        daySpinner.setEnabled(enabled);
    }
}