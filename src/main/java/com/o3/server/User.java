package com.o3.server;

public class User {
    
    private String name;
    private String password;
    private String email;
    private String nickname;

    User(String name,String password,String email,String nickname) {
        this.name = name;
        this.password = password;
        this.email = email;
        this.nickname = nickname;
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

    public String getNicname(){
        return nickname;
    }
}
