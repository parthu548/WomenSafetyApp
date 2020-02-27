package com.example.womensafetyapp;

public class AllContacts {

    String number;
    String name;

    public AllContacts(String name, String number) {
        this.number = number;
        this.name = name;
    }
    public AllContacts(String name, Long number) {
        this.number = number.toString();
        this.name = name;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
