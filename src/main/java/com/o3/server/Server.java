package com.o3.server;

import com.sun.net.httpserver.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;



public class Server implements HttpHandler {
		
	private List<ObservationRecord> observationRecords = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void handle(HttpExchange exchange) throws IOException {


        
        String requestParamValue=null; 

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {   
            

            InputStream stream = exchange.getRequestBody();
            String text = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
            
            stream.close();
            
            try {
                JSONObject JObject = new JSONObject(text);    
                
                boolean body_and_epoch = !JObject.isNull("target_body_name") &&
                                       !JObject.isNull("center_body_name") &&
                                       !JObject.isNull("epoch");
                
                boolean orbit_and_state = !JObject.isNull("orbital_elements") || 
                                         !JObject.isNull("state_vector");
                boolean meta_data = !JObject.isNull("metadata");                         
                
                if(body_and_epoch && orbit_and_state && meta_data) {

                    if (!JObject.isNull("orbital_elements")) {
                        JSONObject orbital = JObject.getJSONObject("orbital_elements");
                        orbital.getDouble("eccentricity");
                        orbital.getDouble("semi_major_axis_au");
                        orbital.getDouble("inclination_deg");
                        orbital.getDouble("longitude_ascending_node_deg");
                        orbital.getDouble("argument_of_periapsis_deg");
                        orbital.getDouble("mean_anomaly_deg");
                    }

                    if (!JObject.isNull("state_vector")) {
                        JSONObject state = JObject.getJSONObject("state_vector");
                        
                        if (state.has("position_au") && state.has("velocity_au_per_day")) {
                            JSONArray position = state.getJSONArray("position_au");
                            JSONArray velocity = state.getJSONArray("velocity_au_per_day");
                            if(position.length() > 0) {
                                position.getDouble(0);
                            }
                            if(velocity.length() > 0){
                               velocity.getDouble(0); 
                            } 
                        }else{
                            throw new JSONException("invalid format in State_vector");
                        }
                    }
                    

                    //creating metadata with utc time --->
                    ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));  
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");  
                    String dateText = now.format(formatter);  
                    

                    String user = exchange.getPrincipal().getUsername();

                    JSONObject metadata = new JSONObject();
                    

                    MessageDatabase sql = MessageDatabase.getInstance();
                    String userinfo = sql.getRegisterationInfo(user);
                    String[] userparts = userinfo.split(":");
                    
                    
                    metadata.put("record_time_received", dateText);
                    metadata.put("record_owner", userparts[3]);
                    metadata.put("id",sql.createUniqueID());
                    metadata.put("record_payload",JObject.optString("record_payload", ""));

                    
                    //metadata created <---

                    ObservationRecord record = new ObservationRecord(
                    JObject.getString("target_body_name"),
                    JObject.getString("center_body_name"),
                    JObject.getString("epoch"),
                    JObject.optJSONObject("orbital_elements"),
                    JObject.optJSONObject("state_vector"),
                    metadata
                    );

                    observationRecords.add(record);
                    
                    MessageDatabase sqldata = MessageDatabase.getInstance();
                    String dbPath = getUrl();
                    sqldata.savedata(record ,dbPath);

                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, -1);
                }else{
                    String error = "Missing elements";
                    byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(400, bytes.length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(bytes);
                    os.close();
                }
            }catch (Exception e) {
                
                String error = "Invalid format, expception";
                byte[] bytes = error.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(400, bytes.length);
                OutputStream os = exchange.getResponseBody();
                os.write(bytes);
                os.close();
            }
    
            exchange.close();
		// Handle POST requests here (users send this for sending messages) 
		} else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) { 

            exchange.getResponseHeaders().set("Content-Type", "application/json");

            MessageDatabase sql = MessageDatabase.getInstance();
            List<ObservationRecord> messages = sql.getMessage();
           
            if (messages.isEmpty()) {
                exchange.sendResponseHeaders(204, -1);
                exchange.close();
                return;
            }

            
            JSONArray responseArray = new JSONArray();
            
            
            synchronized (observationRecords) {
                for (ObservationRecord record : messages) {
                    responseArray.put(record.toJSONObject());
                }
            }

            String responseString = responseArray.toString();
            byte[] bytes = responseString.getBytes(StandardCharsets.UTF_8);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);

            OutputStream outputStream = exchange.getResponseBody();
            outputStream.write(bytes);
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

    private static String getUrl() {
        String dbPath = System.getenv("DATABASE_PATH");
        //String dbPath = "jdbc:sqlite:DB";
        if(dbPath == null || dbPath.isEmpty()){
                throw new IllegalStateException("ei loyda polkua");
        }

        return dbPath;
    }
    

    public static void main(String[] args) throws Exception {
       
       
        
        try{ 

            String dbPath = getUrl();
            String[] arrays = dbPath.split(":");
            MessageDatabase sql = MessageDatabase.getInstance();
            sql.open(arrays[2]);
            MessageDatabase.getInstance();

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
