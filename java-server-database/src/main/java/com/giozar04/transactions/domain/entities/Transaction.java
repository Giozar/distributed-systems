package com.giozar04.transactions.domain.entities;

import java.io.Serializable; // Se importa Serializable
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.giozar04.transactions.domain.enums.PaymentMethod;

public class Transaction implements Serializable { // Se implementa Serializable
    private static final long serialVersionUID = 1L; // Versión de serialización

    private long id;
    private String type; // "INCOME" o "EXPENSE"
    private PaymentMethod paymentMethod;
    private double amount;
    private String title;
    private String category;
    private String description;
    private String comments;
    private ZonedDateTime date;
    private List<String> tags; // Varias etiquetas asignables

    public Transaction() {
        this.tags = new ArrayList<>();
    }

    public Transaction(long id, String type, PaymentMethod paymentMethod, double amount, String title,
                      String category, String description, String comments, ZonedDateTime date, List<String> tags) {
        this.id = id;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.title = title;
        this.category = category;
        this.description = description;
        this.comments = comments;
        this.date = date;
        this.tags = tags != null ? tags : new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public ZonedDateTime getDate() {
        return date;
    }

    public void setDate(ZonedDateTime date) {
        this.date = date;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags != null ? tags : new ArrayList<>();
    }
    
    // Métodos helper para convertir entre List<String> y String para la BD
    public String getTagsAsString() {
        if (tags == null || tags.isEmpty()) {
            return "";
        }
        return String.join(",", tags);
    }
    
    public void setTagsFromString(String tagsString) {
        if (tagsString == null || tagsString.isEmpty()) {
            this.tags = new ArrayList<>();
        } else {
            List<String> tagsList = new ArrayList<>();
            for (String tag : tagsString.split(",")) {
                tagsList.add(tag.trim());
            }
            this.tags = tagsList;
        }
    }
}
