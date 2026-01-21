package com.o3.server;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    private Map<String,String>users = null;
    
    
    UserAuthenticator(String realm) {
        super(realm);
        
        users = new Hashtable<String,String>();  
        
        users.put("dummy", "passwd"); 
    }
    
    @Override
    public boolean checkCredentials(String username, String password) {
        
        String PasswordCheck = users.get(username);

        if(!PasswordCheck.equals(null) && PasswordCheck.equals(password) ) {
            return true;
        }

        return false;
    }

    public boolean addUser(String userName, String password) {

        if (!users.containsKey(userName)) {
            users.put(userName, password);
            return true;
        }else {
            return false;
        }
        
        

    }


}
        
    


