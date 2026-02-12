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
import java.sql.Types;

import org.json.JSONObject;

import java.io.File;




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
            var newsqlTable = "CREATE TABLE IF NOT EXISTS users (username varchar(50) NOT NULL, password varchar(50) NOT NULL, email varchar(50) NOT NULL, nickname varchar(50) NOT NULL, primary key(username))";
            String messageTable = "CREATE TABLE message (" +
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

            Statement statementfq = dbConnection.createStatement();
            Statement statementfm = dbConnection.createStatement();
            statementfq.executeUpdate(newsqlTable);
            statementfm.executeUpdate(messageTable);
            statementfq.close();
            statementfm.close();
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
        
        //if(!dbconn){
        //    init();
        //}
        
    }

    public void closeDB() throws SQLException {
        dbConnection.close();
        System.err.println("connection to database closed!");
        dbConnection = null;
    }  
    
    public void savedata(ObservationRecord record, String dbPath) throws SQLException {
    String sql = "INSERT INTO message (target, center, epoch, elements, vector, meta, record_time_received, record_owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    
    try (PreparedStatement pstmt = dbConnection.prepareStatement(sql)) {
        pstmt.setString(1, record.getTargetBodyName());
        pstmt.setString(2, record.getCenterBodyName());
        pstmt.setString(3, record.getEpoch());

        
        if (record.getOrbitalElements() != null) {
            pstmt.setString(4, record.getOrbitalElements().toString());
        } else {
            pstmt.setNull(4, java.sql.Types.VARCHAR);
        }

        
        if (record.getStateVector() != null) {
            pstmt.setString(5, record.getStateVector().toString());
        } else {
            pstmt.setNull(5, java.sql.Types.VARCHAR);
        }

        pstmt.setString(6, record.getMetadata().toString());
        pstmt.setString(7, record.getMetadata().getString("record_time_received"));
        pstmt.setString(8, record.getMetadata().getString("record_owner"));
        
        pstmt.executeUpdate();
    }
    }

    public void saveUser(User user, String path) throws SQLException {
        System.err.println("saveUser testi");
        
        
        String username = user.getName();
        String password = user.getPassword();
        String email = user.getEmail();
        String nickname = user.getNicname(); 

        
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

                JSONObject elements = new JSONObject(rs.getString("elements"));
                JSONObject vector = new JSONObject(rs.getString("vector"));
                JSONObject meta = new JSONObject(rs.getString("meta"));

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
        //make changes to adduser in Userauthentication
        //returning a string
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
}
