package com.o3.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class CollectionsHandler implements HttpHandler {
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String fullPath = exchange.getRequestURI().getPath(); 

        
        final String base = "/collections";
        
        String partPath;
        if (fullPath.startsWith(base)) {
            partPath = fullPath.substring(base.length());
        } else {
            partPath = fullPath;
        }
        if (partPath.isEmpty()) partPath = "/";

        try {
            if (method.equalsIgnoreCase("GET") && (partPath.equals("/") )) {
                handleGetCollections(exchange);
                return;
            }

            if (method.equalsIgnoreCase("POST") && (partPath.equals("/create") || partPath.equals("/create/"))) {
                String body = readbody(exchange);
                handleCreateCollection(exchange, body);
                return;
            }
            if (method.equalsIgnoreCase("POST") && (partPath.equals("/add") || partPath.equals("/add/"))) {
                String body = readbody(exchange);
                handleAddToCollection(exchange, body);
                return;
            }


            if (method.equalsIgnoreCase("GET") && partPath.matches("/\\d+/?")) {
                String id = partPath.substring(1); 
                if (id.endsWith("/")){

                 id = id.substring(0, id.length() - 1);
                } 
                int idforGet = Integer.parseInt(id);
                handleGetCollection(exchange, idforGet);
                return;
            }

            sendText(exchange, 400, "Not Supported");
        } catch (Exception e) {
            System.err.println(e);
            sendText(exchange, 400, "Invalid format, exception");
        }
    }

    private void handleGetCollections(HttpExchange exchange) throws IOException, SQLException {
        MessageDatabase db = MessageDatabase.getInstance();
        List<Integer> ids = db.getAllCollectionIds();

        JSONArray arrayt = new JSONArray(ids);
        sendJson(exchange, 200, arrayt.toString());
    }

    private void handleCreateCollection(HttpExchange exchange, String body) throws IOException, SQLException {
        MessageDatabase db = MessageDatabase.getInstance();

        int newId = db.createCollection();

        int[] Ids = new int[0];
        String Text;
        if (body == null) {
            Text = "";
        } else {
            Text = body.trim();
        }

        if (!Text.isEmpty()) {
            JSONArray arr = new JSONArray(Text);
            Ids = new int[arr.length()];
            for (int i = 0; i < arr.length(); i++) {
                Ids[i] = arr.getInt(i);
            }
            if (Ids.length > 0) {
                db.addMessagesToCollection(newId, Ids);
            }
        }

        JSONObject response = new JSONObject();
        response.put("created_collection_id", newId);
        sendJson(exchange, 200, response.toString());
    }

    private void handleAddToCollection(HttpExchange exchange, String body) throws IOException, SQLException {
        JSONObject obj = new JSONObject(body);

        int collectionId = obj.getInt("collection_id");
        JSONArray arrayt = obj.getJSONArray("message_ids");

        int[] ids = new int[arrayt.length()];
        for (int i = 0; i < arrayt.length(); i++) ids[i] = arrayt.getInt(i);

        MessageDatabase db = MessageDatabase.getInstance();
        db.addMessagesToCollection(collectionId, ids);

        
        sendJson(exchange, 200, "{}");
    }

    private void handleGetCollection(HttpExchange exchange, int collectionId) throws IOException, SQLException {
        MessageDatabase db = MessageDatabase.getInstance();
        List<ObservationRecord> records = db.getMessagesFromCollection(collectionId);

        JSONArray arr = new JSONArray();
        for (ObservationRecord r : records) {
            arr.put(r.toJSONObject());
        }

        sendJson(exchange, 200, arr.toString());
    }

    private String readbody(HttpExchange exchange) throws IOException {
        try (var stream = exchange.getRequestBody()) {
            return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }

    private void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
        exchange.close();
    }


}
