package com.o3.server;

public class User {
    
    private String name;
    private String password;
    private String email;

    User(String name,String password,String email) {
        this.name = name;
        this.password = password;
        this.email = email;
    }

    public String getPassword() {
        return password;
    }
    public String getEmail() {
        return email;
    }
    public String getName() {
        return name;
    }

}
