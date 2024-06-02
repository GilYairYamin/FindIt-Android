package com.example.findit;

public class User {

    private String firstName, secondName, email, cellphone;

    public User(String email)
    {
        this.email = email;

        this.firstName = "";
        this.secondName = "";
        this.cellphone = "";
    }

    public User()
    {
    }


    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setSecondName(String secondName) {
        this.secondName = secondName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCellphone(String cellphone) {
        this.cellphone = cellphone;
    }


    public String getFirstName() {
        return firstName;
    }

    public String getSecondName() {
        return secondName;
    }

    public String getEmail() {
        return email;
    }

    public String getCellphone() {
        return cellphone;
    }

}
