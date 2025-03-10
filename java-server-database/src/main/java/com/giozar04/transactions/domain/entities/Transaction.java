package com.giozar04.transactions.domain.entities;

import java.time.ZonedDateTime;
import java.util.List;

import com.giozar04.transactions.domain.enums.PaymentMethod;

public class Transaction {

    private long id;
    private String type; // "INCOME" o "EXPENSE"
    private PaymentMethod paymentMethod;
    private double amount;
    private String title;
    private Category category;
    private String description;
    private String comments;
    private ZonedDateTime date;
    private List<Tag> tags; // Varias etiquetas asignables

    public Transaction() {
    }

    public Transaction(long id, String type, PaymentMethod paymentMethod, double amount, String title,
                       Category category, String description, String comments, ZonedDateTime date, List<Tag> tags) {
        this.id = id;
        this.type = type;
        this.paymentMethod = paymentMethod;
        this.amount = amount;
        this.title = title;
        this.category = category;
        this.description = description;
        this.comments = comments;
        this.date = date;
        this.tags = tags;
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

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
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

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }

    

}
