package com.o3.server;

import java.util.Hashtable;
import java.util.Map;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticator extends com.sun.net.httpserver.BasicAuthenticator {
    
    
    private Map<String,User>users = null;
    

    
    UserAuthenticator(String realm) {
        super(realm);
        
        
        
        users = new Hashtable<String,User>();  
        
        addUser(new User("dummy", "passwd", "email")); 
    }
    
    @Override
    public boolean checkCredentials(String username, String password) {
        
        User checthisuser = users.get(username);
 
        if(checthisuser != null && checthisuser.getPassword().equals(password) ) {
            return true;
        }

        return false;
    }

    public boolean addUser(User newUser) {

        if (!users.containsKey(newUser.getName())) {
            users.put(newUser.getName(), newUser);
            return true;
        }else {
            return false;
        }
        
        

    }


}
        
    


