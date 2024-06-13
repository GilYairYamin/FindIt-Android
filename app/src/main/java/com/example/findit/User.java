package com.example.findit;

public class User
{
    // Private member variables for user details
    private String firstName, lastName, email, cellphone;

    /**
     * Constructor to initialize a User with an email.
     * Initializes other fields to empty strings.
     *
     * @param email The email of the user.
     */
    public User(String email)
    {
        this.email = email;

        // Initialize other fields to empty strings
        this.firstName = "";
        this.lastName = "";
        this.cellphone = "";
    }

    /**
     * Default constructor for User.
     * Initializes fields to null.
     */
    public User()
    {
    }

    /**
     * Sets the first name of the user.
     *
     * @param firstName The first name to set.
     */
    public void setFirstName(String firstName)
    {
        this.firstName = firstName;
    }

    /**
     * Sets the last name of the user.
     *
     * @param lastName The last name to set.
     */
    public void setLastName(String lastName)
    {
        this.lastName = lastName;
    }

    /**
     * Sets the email of the user.
     *
     * @param email The email to set.
     */
    public void setEmail(String email)
    {
        this.email = email;
    }

    /**
     * Sets the cellphone number of the user.
     *
     * @param cellphone The cellphone number to set.
     */
    public void setCellphone(String cellphone)
    {
        this.cellphone = cellphone;
    }

    /**
     * Gets the first name of the user.
     *
     * @return The first name of the user.
     */
    public String getFirstName()
    {
        return firstName;
    }

    /**
     * Gets the last name of the user.
     *
     * @return The last name of the user.
     */
    public String getLastName()
    {
        return lastName;
    }

    /**
     * Gets the email of the user.
     *
     * @return The email of the user.
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * Gets the cellphone number of the user.
     *
     * @return The cellphone number of the user.
     */
    public String getCellphone()
    {
        return cellphone;
    }
}
