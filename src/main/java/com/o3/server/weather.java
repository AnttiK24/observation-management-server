package com.o3.server;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.XML;


public class weather {
    

    weather(){}


    public JSONObject getWeatherData(String lat, String lon){

        String url = "http://localhost:4001/wfs?latlon=" + lat  + "," + lon;

        try{
            HttpURLConnection apiconnection = fetchApiResponse(url);
            if (apiconnection == null) return null;


            if(apiconnection.getResponseCode() != 200){
                System.err.println("System could not connect to weater server");
                return null;
            }

            String reponse = readApiResponse(apiconnection);

            JSONObject result = XML.toJSONObject(reponse);


            JSONArray members = result.getJSONObject("wfs:FeatureCollection").getJSONArray("wfs:member");
            JSONObject returned_data = new JSONObject();
           
            for (int i = 0; i < members.length(); i++) {
                  
                JSONObject member = members.getJSONObject(i);

                JSONObject element = member.getJSONObject("BsWfs:BsWfsElement");

                    
                String parameterName = element.getString("BsWfs:ParameterName");
                String parameterValue = element.getString("BsWfs:ParameterValue");

                

                if (parameterName.equals("Temperature")) {
                    returned_data.put("temperature_in_kelvins", Double.parseDouble(parameterValue));
                }

                if (parameterName.equals("TotalCloudCover")) {
                    returned_data.put("cloudiness_percentage", Double.parseDouble(parameterValue));
                }

                if (parameterName.equals("RadiationGlobalAccumulation")) {
                    returned_data.put("background_light_volume", Double.parseDouble(parameterValue));
                }
                    
                
            }
            
            
            return returned_data;



        }catch(Exception e){
            System.err.println(e);
        }
        return null;
        
    }

    private static HttpURLConnection fetchApiResponse(String urlString) {
        try {
            
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            
            conn.setRequestMethod("GET");

            return conn;
        }catch (IOException e) {
            e.printStackTrace();
        }

        

        return null;
    } 

    private static String readApiResponse(HttpURLConnection apiConnection) {
        try {
            StringBuilder resultJson = new StringBuilder();      
            Scanner scanner = new Scanner(apiConnection.getInputStream());

            
            while (scanner.hasNext()) {
                
                resultJson.append(scanner.nextLine());
            }

            scanner.close();

            
            return resultJson.toString();

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


}
