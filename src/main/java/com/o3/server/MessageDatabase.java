package com.o3.server;
import java.sql.Connection ;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException; 
import java.sql.Statement;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

import java.io.File;

import java.util.concurrent.Executors; 
import java.security.SecureRandom;  
import org.apache.commons.codec.digest.Crypt;




public class MessageDatabase {

    private Connection dbConnection = null;
    private static MessageDatabase dbinstance;
    private String url = null;    

    public static synchronized MessageDatabase getInstance(){
        if (dbinstance == null) {
            dbinstance = new MessageDatabase();
        }
        return dbinstance;
    }

    private MessageDatabase(){
        try{
            init();

        }catch (SQLException e) {
            System.err.println("error" + e);
        }
    }


     public void init() throws SQLException {
        
        if(null != dbConnection) {
            var newsqlTable = "CREATE TABLE IF NOT EXISTS users (username varchar(50) NOT NULL, password TEXT NOT NULL, email varchar(50) NOT NULL, nickname varchar(50) NOT NULL, primary key(username))";
            String messageTable = "CREATE TABLE IF NOT EXISTS message (" +
                      "id INTEGER PRIMARY KEY AUTOINCREMENT, " + 
                      "target VARCHAR(50) NOT NULL, " +
                      "center VARCHAR(50) NOT NULL, " +
                      "epoch VARCHAR(50) NOT NULL, " +
                      "elements TEXT , " +
                      "vector TEXT , " +
                      "meta TEXT NOT NULL," +
                      "record_time_received TEXT NOT NULL," +
                      "record_owner TEXT NOT NULL" +
                      ");";
            
            String collection_id = "CREATE TABLE IF NOT EXISTS collections (" +
                       "id INTEGER PRIMARY KEY AUTOINCREMENT)";

            String collectiontable = "CREATE TABLE IF NOT EXISTS collection_messages (" +
                        "collection_id INTEGER, " +
                        "message_id INTEGER, " +
                        "FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE, " +
                        "FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE)";     


            Statement statementfq = dbConnection.createStatement();
            Statement statementfm = dbConnection.createStatement();
            Statement statementc = dbConnection.createStatement();
            Statement statementci = dbConnection.createStatement();

            statementfq.executeUpdate(newsqlTable);
            statementfm.executeUpdate(messageTable);
            statementc.executeUpdate(collectiontable);
            statementci.executeUpdate(collection_id);
            statementfq.close();
            statementfm.close();
            statementc.close();
        }
        
    }

    public void open(String dbName) throws SQLException {
        //create connection using url
        File dbfile = new File(dbName);
        boolean dbconn = dbfile.exists();
        
       

        String dbPath = "jdbc:sqlite:" + dbName;
        String url = dbPath;
        this.dbConnection = DriverManager.getConnection(url);
        System.out.println("Connection to SQLite has been done correctly");      
        init();
        
    
        
    }

    public void closeDB() throws SQLException {
        dbConnection.close();
        System.err.println("connection to database closed!");
        dbConnection = null;
    }  
    
    public void savedata(ObservationRecord record, String path) throws SQLException {
        System.err.println("savedata testi");

        String target = record.getTargetBodyName();
        String center = record.getCenterBodyName();
        String epoch = record.getEpoch();

        String meta = record.getMetadata().toString();

        String elements = (record.getOrbitalElements() != null) ? record.getOrbitalElements().toString() : "{}";
        String vector = (record.getStateVector() != null) ? record.getStateVector().toString() : "{}";

        
        String record_time_string = record.getMetadata().getString("record_time_received");
        ZonedDateTime zdt = ZonedDateTime.parse(record_time_string);
        long record_time_long = zdt.toInstant().toEpochMilli();

        String record_owner = record.getMetadata().getString("record_owner");

        String sql_query = "INSERT INTO message (target, center, epoch, elements, vector, meta, record_time_received, record_owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        if(dbConnection == null) {
            String[] parts = path.split(":");
            open(parts[2]);
        }

        var stmnt = dbConnection.prepareStatement(sql_query);

        //stmnt.setInt(0, id);
        stmnt.setString(1,target);
        stmnt.setString(2, center);
        stmnt.setString(3, epoch);
        stmnt.setString(4, elements);
        stmnt.setString(5, vector);
        stmnt.setString(6, meta);
        stmnt.setLong(7, record_time_long);
        stmnt.setString(8, record_owner);

        stmnt.executeUpdate();
        stmnt.close();
    }

    public void saveUser(User user, String path) throws SQLException {
        System.err.println("saveUser testi");
        
        
        String username = user.getName();
        String password = user.getPassword();
        String email = user.getEmail();
        String nickname = user.getNicname(); 

        password = HashPassword(password, null);

        
        String sql_query = "INSERT INTO users (username, password, email, nickname) VALUES (?, ?, ?, ?)";

        
        if(dbConnection == null) {
            String[] parts = path.split(":");
            open(parts[2]);
        }

    
    var stmnt = dbConnection.prepareStatement(sql_query);

    
    stmnt.setString(1, username);
    stmnt.setString(2, password);
    stmnt.setString(3, email);
    stmnt.setString(4, nickname);

    
    stmnt.executeUpdate();
    stmnt.close();
    }


    public List<ObservationRecord> getMessage() {

        List<ObservationRecord> messagelist = new ArrayList<>();
        var sql_message = "SELECT * FROM message";
        try(Statement stmnt = dbConnection.createStatement();
            ResultSet rs = stmnt.executeQuery(sql_message)) {
            
            
            
            
            while(rs.next()){
                
                String target = rs.getString("target");
                String center = rs.getString("center");
                String epoch = rs.getString("epoch");

                String elString = rs.getString("elements");
                String vecString = rs.getString("vector");
                String metaString = rs.getString("meta");

                
                JSONObject elements = (elString == null || elString.equals("{}")) ? null : new JSONObject(elString);
                JSONObject vector = (vecString == null || vecString.equals("{}")) ? null : new JSONObject(vecString);
                
                
                JSONObject meta = (metaString != null) ? new JSONObject(metaString) : new JSONObject();
                

                long timeLong = rs.getLong("record_time_received");
                meta.put("record_time_received", formatEpoch(timeLong));
                meta.put("id", rs.getInt("id"));

                ObservationRecord new_message = new ObservationRecord(target,center,epoch,elements,vector,meta);
                messagelist.add(new_message);

                
            }

            
        }catch (SQLException e) {
            System.err.println(e);
        }
        return messagelist;
    }
    
    public String getRegisterationInfo(String user) {

        var sql = "SELECT * FROM users WHERE username = ?";
        
        try(PreparedStatement stmnt = dbConnection.prepareStatement(sql)){
            stmnt.setString(1, user);
            try(ResultSet rs = stmnt.executeQuery()){
                if(rs.next()){
                    
                        String username = rs.getString("username");
                        String password = rs.getString("password");
                        String email = rs.getString("email");
                        String nickname = rs.getString("nickname");

                        String combo =  username + ":" + password + ":" + email + ":" + nickname;
                        
                        return combo;
                }
                
            }

        }catch(SQLException e){
            System.err.println(e);
        }
        
        return null;


        
    }
    public Integer createUniqueID(){
        try{
            var sql = "SELECT count(*) FROM message";
            var stmnt = dbConnection.createStatement();
            var rs = stmnt.executeQuery(sql);
            
            if(rs.next()){
                return rs.getInt(1) + 1;
            }  
            

        }catch(SQLException e) {
            System.err.println(e);
        }


        return 1;
    }
    
    private String formatEpoch(long epoch) {
        ZonedDateTime zdt = ZonedDateTime.ofInstant(java.time.Instant.ofEpochMilli(epoch), ZoneId.of("UTC"));
        return zdt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX"));
    }

    public String HashPassword(String password, String salting){
        
       

        if(salting == null) {
            return Crypt.crypt(password);
        }
        
        return Crypt.crypt(password, salting);
    }

    public boolean CheckMessageID(int id){
        var sql_id = "SELECT 1 FROM message WHERE id = ?";
        try(PreparedStatement statemnt = dbConnection.prepareStatement(sql_id)){
          
            statemnt.setInt(1,id);
            try(ResultSet rs = statemnt.executeQuery()){
                return rs.next();
            }


        }catch(Exception e){

        }
       return false;



    }

    public int createCollection() throws SQLException {
        String sql = "INSERT INTO collections DEFAULT VALUES";
        try (PreparedStatement st = dbConnection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            st.executeUpdate();
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        try (Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
            if (rs.next()) return rs.getInt(1);
        }
        throw new SQLException("Could not create collection id");
    }

    public void addMessagesToCollection(int collectionId, int[] messageIds) throws SQLException {
        String sql = "INSERT INTO collection_messages (collection_id, message_id) VALUES (?, ?)";
        try (PreparedStatement st = dbConnection.prepareStatement(sql)) {
            for (int messageI : messageIds) {
                st.setInt(1, collectionId);
                st.setInt(2, messageI);
                st.addBatch();
            }
            st.executeBatch();
        }
    }

    public List<Integer> getAllCollectionIds() throws SQLException {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM collections";
        try (Statement st = dbConnection.createStatement();
            ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) ids.add(rs.getInt("id"));
        }
        return ids;
    }

    public List<ObservationRecord> getMessagesFromCollection(int collectionId) throws SQLException {
        List<ObservationRecord> list = new ArrayList<>();


        String SQLids = "SELECT message_id FROM collection_messages WHERE collection_id = ?";
        List<Integer> messgIds = new ArrayList<>();
        try (PreparedStatement st = dbConnection.prepareStatement(SQLids)) {
            st.setInt(1, collectionId);
            try (ResultSet rs = st.executeQuery()) {
                while (rs.next()) messgIds.add(rs.getInt("message_id"));
            }
        }
        String sqlMsg = "SELECT * FROM message WHERE id = ?";
        try (PreparedStatement st = dbConnection.prepareStatement(sqlMsg)) {
            for (int id : messgIds) {
                st.setInt(1, id);
                try (ResultSet rs = st.executeQuery()) {
                    if (!rs.next()) continue;

                    String target = rs.getString("target");
                    String center = rs.getString("center");
                    String epoch = rs.getString("epoch");

                    String elString = rs.getString("elements");
                    String vecString = rs.getString("vector");
                    String metaString = rs.getString("meta");

                    JSONObject elements = (elString == null || elString.equals("{}")) ? null : new JSONObject(elString);
                    JSONObject vector = (vecString == null || vecString.equals("{}")) ? null : new JSONObject(vecString);
                    JSONObject meta = (metaString != null) ? new JSONObject(metaString) : new JSONObject();

                    long timeLong = rs.getLong("record_time_received");
                    meta.put("record_time_received", formatEpoch(timeLong));
                    meta.put("id", rs.getInt("id"));

                    ObservationRecord rec = new ObservationRecord(target, center, epoch, elements, vector, meta);
                    list.add(rec);
                }
            }
        }
        return list;
    }

}