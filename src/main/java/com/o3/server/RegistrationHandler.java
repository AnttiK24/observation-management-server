package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegistrationHandler implements HttpHandler {
    
    private UserAuthenticator userAuth;

    RegistrationHandler(UserAuthenticator userAuth) {
        this.userAuth = userAuth;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {



        String requestParamValue=null; 

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {   
            
            InputStream stream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        
            stream.close();
            String[] credits = text.split(":"); 

            if (credits.length == 2 && !credits[0].isEmpty() && !credits[1].isEmpty()) {
                String urname = credits[0];
                String pword = credits[1];

                boolean NewUser = userAuth.addUser(urname, pword);

                if(NewUser){
                    String responseString = "Registeration was successfull";
                    byte [] bytes = responseString.getBytes("UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(responseString.getBytes()); 
                    outputStream.flush();
                    outputStream.close();
                    
                }else{
                    String responseString = "User already exists";
                    byte [] bytes = responseString.getBytes("UTF-8");
                    exchange.sendResponseHeaders(403, bytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(responseString.getBytes()); 
                    outputStream.flush();
                    outputStream.close();
                    
                }
                
            }else {
                String responseString = "Credentials were not ok , try format username:password";
                byte [] bytes = responseString.getBytes("UTF-8");
                exchange.sendResponseHeaders(400, bytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseString.getBytes()); 
                outputStream.flush();
                outputStream.close();
                   
            }    
            

            exchange.sendResponseHeaders(200, -1);
            exchange.close();
		// Handle POST requests here (users send this for sending messages) 
		} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) { 

            String responseString = "Not Supported";

            byte [] bytes = responseString.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length); 

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseString.getBytes()); 
            outputStream.flush();
            outputStream.close();
            exchange.close();
             
		} else { 

            String responseString = "Not Supported";

            
            byte [] bytes = responseString.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length); 

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseString.getBytes()); 
            outputStream.flush();
            outputStream.close();
            exchange.close(); 
	}



    }
}