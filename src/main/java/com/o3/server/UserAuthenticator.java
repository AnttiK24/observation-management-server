package com.o3.server;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    
    private Map<String,User>users = null;
    

    
    UserAuthenticator(String realm) {
        super(realm);
        
        
        
         
    }
    
    @Override
    public boolean checkCredentials(String username, String password) {
        
        MessageDatabase sql = MessageDatabase.getInstance();
        String userinfo = sql.getRegisterationInfo(username);
    
        if(userinfo == null) {
            return false;
        }

        String[] parts = userinfo.split(":");
        String sqlPassword = parts[1]; 

    
        String mathing_pass = sql.HashPassword(password, sqlPassword);

        return sqlPassword.equals(mathing_pass);
    }

    public boolean addUser(User newUser, String path) {

        MessageDatabase sql = MessageDatabase.getInstance();

        try{
            if(sql.getRegisterationInfo(newUser.getName()) == null){
                
                sql.saveUser(newUser, path);
                return true;
            }else {
                return false;
            }
        } catch(Exception e){
            System.err.println(e);
        }
    
        return false;
    }  

}

        
    


