package com.example.demo.vendor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String name; // e.g., "A1 Plumbing"
    private String trade; // e.g., "Plumbing", "Electrical"
    private String phoneNumber;

    // Constructors, Getters, and Setters
    public Vendor() {
    }

    public Vendor(String name, String trade, String phoneNumber) {
        this.name = name;
        this.trade = trade;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTrade() {
        return trade;
    }

    public void setTrade(String trade) {
        this.trade = trade;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
