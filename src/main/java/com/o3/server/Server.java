package com.o3.server;

import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.io.*;



public class Server implements HttpHandler {
		
	private  List<String> message = new ArrayList<>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {



        String requestParamValue=null; 

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {   
            
            InputStream stream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            
            stream.close();
            message.add(text);

            exchange.sendResponseHeaders(200, -1);
            exchange.close();
		// Handle POST requests here (users send this for sending messages) 
		} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) { 

            String responseString = "";

            if (message.isEmpty()) {
                responseString = "No messages";
            }else {
                responseString = message.toString();
            }

           

            byte [] bytes = responseString.getBytes("UTF-8");
            exchange.sendResponseHeaders(200, bytes.length); 

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseString.getBytes()); 
            outputStream.flush();
            outputStream.close();
            exchange.close();
            


		// Handle GET requests here (users use this to get messages) 
		} else { 

            String responseString = "Not Supported";

            
            byte [] bytes = responseString.getBytes("UTF-8");
            exchange.sendResponseHeaders(400, bytes.length); 

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(responseString.getBytes()); 
            outputStream.flush();
            outputStream.close();
            exchange.close();
            

		// Inform user here that only POST and GET functions are supported and send an error code 
		//400 with a message “Not supported” (witouth the “). 
	}


    }
	
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8001),0);
        server.createContext("/datarecord", new Server());
        server.setExecutor(null); // creates a default executor
        System.out.println("käynnistit serverin http://localhost:8001/datarecord");
        //curl http://127.0.0.1:8001/datarecord -d "This is a message"
		server.start();
    }

	
	
}
