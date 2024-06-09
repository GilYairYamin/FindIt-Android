package com.example.findit;

public class User {

    private String firstName, lastName, email, cellphone;

    public User(String email)
    {
        this.email = email;

        this.firstName = "";
        this.lastName = "";
        this.cellphone = "";
    }

    public User()
    {
    }


    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
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

    public String getLastName() {
        return lastName;
    }

    public String getEmail() {
        return email;
    }

    public String getCellphone() {
        return cellphone;
    }

}
