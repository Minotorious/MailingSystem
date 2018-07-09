package MailingSystem;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Connection;
import java.sql.PreparedStatement;
import net.risingworld.api.database.Database;
import java.util.ArrayList;
import java.util.Date;

public class DatabaseStuff {
    
    public static Database data;
    public MailingSystem MS = new MailingSystem();
    
    public void setDB(Database db){
        data = db;
    }
    
    public void initDB(){
        data.execute("CREATE TABLE IF NOT EXISTS 'Messages' ('SenderName' VARCHAR(64), 'ReceiverName' VARCHAR(64), 'Message' VARCHAR(1000), 'Timestamp' TIMESTAMP, 'ReadReceipt' BOOLEAN, 'Admins' BOOLEAN)");
    }
    
    public class mail{
        String SenderName;
        String ReceiverName;
        String Message;
        Timestamp timestamp;
        boolean readreceipt;
        boolean admins;
    }
    
    public ArrayList<mail> getMail(String name){
        ArrayList<mail> messages = new ArrayList<>();
        Connection con = data.getConnection();
        try{
            PreparedStatement prep = con.prepareStatement("SELECT * FROM Messages WHERE ReceiverName LIKE ?");
            prep.setString(1, name); 
            ResultSet result = prep.executeQuery();
            while (result.next()){
                mail m = new mail();
                m.SenderName = result.getString("SenderName");
                m.ReceiverName = result.getString("ReceiverName");
                m.Message = result.getString("Message");
                m.timestamp = result.getTimestamp("Timestamp");
                m.readreceipt = result.getBoolean("ReadReceipt");
                m.admins = result.getBoolean("Admins");
                messages.add(m);
            }
        }
        catch(SQLException e){
        }
        
        return messages;
    }
    
    public void sendMail(String senderName, String receiverName, String message, boolean readreceipt, boolean admins){
        Date date = new Date();
        Timestamp timestamp = new Timestamp(date.getTime());
        Connection con = data.getConnection();
        try{
            PreparedStatement prep = con.prepareStatement("INSERT INTO Messages (SenderName, ReceiverName, Message, Timestamp, ReadReceipt, Admins) VALUES (?,?,?,?,?,?)");
            prep.setString(1, senderName);
            prep.setString(2, receiverName);
            prep.setString(3, message);
            prep.setTimestamp(4, timestamp);
            prep.setBoolean(5, readreceipt);
            prep.setBoolean(6, admins);
            prep.executeUpdate();
        }
        catch (SQLException e){
        }
    }
    
    public void clearMail(String name){
        Connection con = data.getConnection();
        try{
            PreparedStatement prep = con.prepareStatement("DELETE FROM Messages WHERE ReceiverName LIKE ?");
            prep.setString(1, name);
            prep.executeUpdate();
        } 
        catch (SQLException e) {
        }
    }
    
    public void cancelMail(String regarding){
        Connection con = data.getConnection();
        try{
            PreparedStatement prep = con.prepareStatement("DELETE FROM Messages WHERE Message LIKE ?");
            prep.setString(1, regarding + "%");
            prep.executeUpdate();
        } 
        catch (SQLException e) {
        }
    }
    
    public void monthlyClear(){
        Connection con = data.getConnection();
        try{
            PreparedStatement prep = con.prepareStatement("SELECT * FROM Messages");
            ResultSet result = prep.executeQuery();
            
            while (result.next()){
                Timestamp olddate = result.getTimestamp("Timestamp");
                Date date = new Date();
                long difference = (date.getTime() - olddate.getTime())/(1000*60*60*24);
                
                if (difference >= Integer.parseInt(MS.SettingbyName("MailDeletionPeriod"))){
                    PreparedStatement prep2 = con.prepareStatement("DELETE FROM Messages WHERE Timestamp LIKE " + Long.toString(olddate.getTime()));
                    prep2.executeUpdate();
                }
            }
        }
        catch(SQLException e){
        }
    }
}
