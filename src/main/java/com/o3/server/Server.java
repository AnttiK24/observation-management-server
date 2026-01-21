package com.o3.server;

import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

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
	
    private static SSLContext myServerSSLContext(String location, String passW) throws Exception {

        char[] passphrase = passW.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(location), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

    

    public static void main(String[] args) throws Exception {
        try{ 
            
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001),0);
            UserAuthenticator User = new UserAuthenticator("datarecord");
            HttpContext context = server.createContext("/datarecord", new Server()); 
            context.setAuthenticator(User);
            server.createContext("/registration", new RegistrationHandler(User));
            
            String keystorePath_location = args[0];
            String keystore_passW = args[1];

            SSLContext sslContext = myServerSSLContext(keystorePath_location,keystore_passW);
            server.setHttpsConfigurator (new HttpsConfigurator(sslContext) { 
                public void configure (HttpsParameters params) { 
                    InetSocketAddress remote = params.getClientAddress(); 
                    SSLContext c = getSSLContext(); 
                    SSLParameters sslparams = c.getDefaultSSLParameters(); 
                    params.setSSLParameters(sslparams); 
                }
            });
            server.setExecutor(null); // creates a default executor
            System.out.println("käynnistit serverin http://localhost:8001/datarecord");
            //curl https://127.0.0.1:8001/obervation -d "This is a message"
            server.start();
        } catch (FileNotFoundException e) { 
            // Certificate file not found! 
            System.out.println("Certificate not found!"); 
            e.printStackTrace(); 
        } catch (Exception e) { 
            e.printStackTrace(); 
        }     
            
    }



    

	
	
}
