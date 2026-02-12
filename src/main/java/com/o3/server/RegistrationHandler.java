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

import org.json.JSONObject;
import org.json.JSONException;

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
            
            try {
                JSONObject object = new JSONObject(text);
                
                if(!object.getString("username").isEmpty() && !object.getString("password").isEmpty() && !object.getString("email").isEmpty() && !object.getString("nickname").isEmpty()) {
                    String urname = object.getString("username");
                    String password = object.getString("password");
                    String email = object.getString("email");
                    String nickname = object.getString("nickname"); 
                    

                    User newuser = new User(urname, password, email, nickname);

                    boolean NewUser = userAuth.addUser(newuser, getUrl());

                    if(NewUser){
                        //1
                        //MessageDatabase obje = MessageDatabase.getInstance();
                        //String path = getUrl();
                        //obje.saveUser(newuser, path);

                        String responseString = "Registeration was successfull";
                        byte [] bytes = responseString.getBytes("UTF-8");
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(200, bytes.length);
                        OutputStream outputStream = exchange.getResponseBody();
                        outputStream.write(responseString.getBytes()); 
                        outputStream.flush();
                        outputStream.close();
                        
                    }else{
                        String responseString = "User already exists";
                        byte [] bytes = responseString.getBytes("UTF-8");
                        exchange.getResponseHeaders().set("Content-Type", "application/json");
                        exchange.sendResponseHeaders(403, bytes.length);
                        OutputStream outputStream = exchange.getResponseBody();
                        outputStream.write(responseString.getBytes()); 
                        outputStream.flush();
                        outputStream.close();
                        
                    }
                    
                }else {
                    String responseString = "Credentials were not ok ";
                    byte [] bytes = responseString.getBytes("UTF-8");
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(400, bytes.length);
                    OutputStream outputStream = exchange.getResponseBody();
                    outputStream.write(responseString.getBytes()); 
                    outputStream.flush();
                    outputStream.close();
                    
                }
            }catch (Exception e){
                String responseString = "Credentials were not ok , exception catch";
                byte [] bytes = responseString.getBytes("UTF-8");
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(400, bytes.length);
                OutputStream outputStream = exchange.getResponseBody();
                outputStream.write(responseString.getBytes()); 
                outputStream.flush();
                outputStream.close();
            } 
            

            
            exchange.close();
		
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

    private static String getUrl() {
        String dbPath = System.getenv("DATABASE_PATH");
        //String dbPath = "jdbc:sqlite:DB";
        if(dbPath == null || dbPath.isEmpty()){
                throw new IllegalStateException("ei loyda polkua");
        }

        return dbPath;
    }



}