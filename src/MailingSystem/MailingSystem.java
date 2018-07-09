package MailingSystem;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import net.risingworld.api.Plugin;
import net.risingworld.api.database.Database;
import net.risingworld.api.database.WorldDatabase;
import net.risingworld.api.events.Listener;
import net.risingworld.api.events.EventMethod;
import net.risingworld.api.events.player.PlayerCommandEvent;
import net.risingworld.api.events.player.PlayerSpawnEvent;
import net.risingworld.api.objects.Player;
import net.risingworld.api.Timer;
import net.risingworld.api.utils.Utils;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;

public class MailingSystem extends Plugin implements Listener {
    
    @Override
    public void onEnable(){
        //Register event listener
        registerEventListener(this);
        
        ReadSettings();
        MailCheckTimer();
        
        Database sqlite = getSQLiteConnection(getPath() + "/assets/mail.db");
        DatabaseStuff db = new DatabaseStuff();
        db.setDB(sqlite);
        db.initDB();
        db.monthlyClear();
    }
    
    public void MailCheckTimer(){
        if (Float.parseFloat(SettingbyName("MailReminderTimer")) > 0){
            Timer timer = new Timer(Float.parseFloat(SettingbyName("MailReminderTimer")),0,-1,()->{
                for (Player player : getServer().getAllPlayers()){
                    DatabaseStuff db =  new DatabaseStuff();
                    if (!db.getMail(player.getName()).isEmpty()){
                        player.sendTextMessage(SettingbyName("PluginTextColour") + "You have unread mail messages! Type [/mail read] to see them.");
                    }
                }
            });
            TimerHolder.setTimer(timer);
            timer.start();
        }
    }
    
    public static class TimerHolder{
        private static Timer timer;
        
        public static Timer getTimer(){
            return timer;
        }
        
        public static void setTimer(Timer T){
            timer = T;
        }
    }
    
    public void ReadSettings(){
        File settingsTxt = new File(getPath() + "/assets/mail.properties");
        if (settingsTxt.exists()){
            String content = Utils.FileUtils.readStringFromFile(settingsTxt);
            if(content != null && !content.isEmpty()){
                String[] lines = content.split("\r\n|\n|\r");
                String[][] settings = new String[lines.length][2];
                int linecount = 0;
                for (String line : lines) {
                    String[] cline = line.split("=", 2);
                    if (cline.length == 2){
                        settings[linecount][0] = cline[0];
                        settings[linecount][1] = cline[1];
                        linecount += 1;
                    }
                }
                Settings.setSettings(settings);
            }
            else{
                String[][] settings = new String[0][0];
                Settings.setSettings(settings);
            }
        }
        else{
            String[][] settings = new String[0][0];
            Settings.setSettings(settings);
        }
    }
    
    public static class Settings{
        private static String[][] settings;
        
        public static String[][] getSettings(){
            return settings;
        }
        
        public static void setSettings(String[][] S){
            settings = S;
        }
    }
    
    public String SettingbyName(String input){
        String[][] settings =  Settings.getSettings();
        String output = "";
        
        for (String[] setting : settings){
            if (setting[0].equals(input)){
                output = setting[1];
            }
        }
        
        return output;
    }
    
    @EventMethod
    public void onCommand(PlayerCommandEvent event){
        Player player = event.getPlayer();
        String command = event.getCommand();

        String[] cmd = command.split(" ");
        if (cmd[0].equals("/mail")){
            if (cmd.length >= 2){
                switch (cmd[1]) {
                    case "send":
                        if (cmd.length >= 4){
                            String receivername = cmd[2];
                            String message = "";
                            for (int i=3;i<cmd.length;i++){
                                message = message + " " + cmd[i];
                            }
                            
                            boolean namecheck = false;
                            WorldDatabase database = getWorldDatabase();
                            try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                while (result.next()){
                                    String name = result.getString("Name");
                                    if (name.equals(receivername)){
                                        namecheck = true;
                                        break;
                                    }
                                }
                            }
                            catch (SQLException e){
                            }
                            
                            if (namecheck == true){
                                DatabaseStuff db =  new DatabaseStuff();
                                db.sendMail(player.getName(), receivername, message, false, false);
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message to " + receivername + " has been successfully sent!");
                                Player receivingplayer = getServer().getPlayer(receivername);
                                if (receivingplayer != null){
                                    receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                }
                            }
                            else{
                                player.sendTextMessage("[#FF0000]Player name " + receivername + " not found! Try [/mail lookup string] to check name spelling.");
                            }
                        }
                        break;
                    case "sendr":
                        if (cmd.length >= 4){
                            String receivername = cmd[2];
                            String message = "";
                            for (int i=3;i<cmd.length;i++){
                                message = message + " " + cmd[i];
                            }
                            
                            boolean namecheck = false;
                            WorldDatabase database = getWorldDatabase();
                            try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                while (result.next()){
                                    String name = result.getString("Name");
                                    if (name.equals(receivername)){
                                        namecheck = true;
                                        break;
                                    }
                                }
                            }
                            catch (SQLException e){
                            }
                            
                            if (namecheck == true){
                                DatabaseStuff db =  new DatabaseStuff();
                                db.sendMail(player.getName(), receivername, message, true, false);
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message to " + receivername + " has been successfully sent!");
                                Player receivingplayer = getServer().getPlayer(receivername);
                                if (receivingplayer != null){
                                    receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                }
                            }
                            else{
                                player.sendTextMessage("[#FF0000]Player name " + receivername + " not found! Try [/mail lookup string] to check name spelling.");
                            }
                        }
                        break;
                    case "sendall":
                        if (player.isAdmin()){
                            if (cmd.length >= 3){
                                String message = "";
                                for (int i=2;i<cmd.length;i++){
                                    message = message + " " + cmd[i];
                                }
                                WorldDatabase database = getWorldDatabase();
                                try(ResultSet result = database.executeQuery("SELECT Name,LastTimeOnline FROM Player")){
                                    while (result.next()){
                                        String receivername = result.getString("Name");
                                        Date olddate = new Date(result.getLong("LastTimeOnline"));
                                        Date date = new Date();
                                        long difference = (date.getTime() - olddate.getTime())/(1000*60*60*24);
                                        if (difference <= Integer.parseInt(SettingbyName("ActivePlayerPeriod"))){
                                            DatabaseStuff db =  new DatabaseStuff();
                                            db.sendMail(player.getName(), receivername, message, false, false);
                                            Player receivingplayer = getServer().getPlayer(receivername);
                                            if (receivingplayer != null){
                                                receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                            }
                                        }
                                    }
                                    player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message has been successfully sent!");
                                }
                                catch(SQLException e){
                                }
                            }
                        }
                        else{
                            player.sendTextMessage("[#FF0000]Insufficient Permission!");
                        }
                        break;
                    case "read":
                        if (cmd.length == 2){
                            DatabaseStuff db =  new DatabaseStuff();
                            ArrayList<DatabaseStuff.mail> messages = db.getMail(player.getName());
                            if (!messages.isEmpty()){
                                for (DatabaseStuff.mail m : messages){
                                    Timestamp ts = m.timestamp;
                                    Date date = new Date();
                                    date.setTime(ts.getTime());
                                    String formattedDate = new SimpleDateFormat("dd MMM yyyy HH:mm").format(date);
                                    if (m.admins == false){
                                        player.sendTextMessage(SettingbyName("PluginTextColour") + "Player " + m.SenderName + " sent you the following message on " + formattedDate + ": " + SettingbyName("MailMessageColour") + m.Message);
                                        if (m.readreceipt == true){
                                            String returnmessage = "Player " + m.ReceiverName + " read your message.";
                                            db.sendMail("ReadReceipt", m.SenderName, returnmessage, false, false);
                                        }
                                    }
                                    else{
                                        player.sendTextMessage(SettingbyName("PluginTextColour") + "Player " + m.SenderName + " sent all admins the following message on " + formattedDate + ": " + SettingbyName("MailMessageColour") + m.Message);
                                    }
                                }
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Re-enter chat (T key) and use PgUp/PgDn to scroll through the messages");
                                db.clearMail(player.getName());
                            }
                            else{
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Mailbox is Empty!");
                            }
                        }
                        break;
                    case "lookup":
                        if (cmd.length == 3){
                            String partialname = cmd[2];
                            WorldDatabase database = getWorldDatabase();
                            boolean findcheck = false;
                            ArrayList<String> playernames = new ArrayList<>();
                            try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Player names starting with " + partialname + ":");
                                while (result.next()){
                                    String playername = result.getString("Name");
                                    if (playername.toLowerCase().startsWith(partialname.toLowerCase())){
                                        playernames.add(playername);
                                        findcheck = true;
                                    }
                                }
                            }
                            catch (SQLException e){
                            }
                            
                            for (String playername : playernames){
                                player.sendTextMessage(SettingbyName("PluginTextColour") + playername);
                            }
                            
                            if (playernames.size() >= 12){
                                player.sendTextMessage(SettingbyName("MailMessageColour") + "Re-enter chat (T key) and use PgUp/PgDn to scroll through the list");
                            }
                            
                            if (findcheck == false){
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "No Player names found!");
                            }
                        }
                        break;
                    case "admins":
                        if (cmd.length >= 3){
                            String message = "";
                            for (int i=2;i<cmd.length;i++){
                                message = message + " " + cmd[i];
                            }
                            WorldDatabase database = getWorldDatabase();
                            try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                while (result.next()){
                                    String receivername = result.getString("Name");
                                    if (getServer().isPlayerAdmin(receivername)){
                                        DatabaseStuff db =  new DatabaseStuff();
                                        db.sendMail(player.getName(), receivername, message, false, true);
                                        Player receivingplayer = getServer().getPlayer(receivername);
                                        if (receivingplayer != null){
                                            receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                        }
                                    }
                                }
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message has been successfully sent!");
                            }
                            catch (SQLException e){
                            }
                        }
                        break;
                    case "group":
                        if (cmd.length >= 4){
                            String[] playernames = cmd[2].split(",");
                            String message = "";
                            for (int i=3;i<cmd.length;i++){
                                message = message + " " + cmd[i];
                            }
                            for (String receivername : playernames){
                                boolean namecheck = false;
                                WorldDatabase database = getWorldDatabase();
                                try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                    while (result.next()){
                                        String name = result.getString("Name");
                                        if (name.equals(receivername)){
                                            namecheck = true;
                                            break;
                                        }
                                    }
                                }
                                catch (SQLException e){
                                }

                                if (namecheck == true){
                                    DatabaseStuff db =  new DatabaseStuff();
                                    db.sendMail(player.getName(), receivername, message, false, false);
                                    player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message to " + receivername + " has been successfully sent!");
                                    Player receivingplayer = getServer().getPlayer(receivername);
                                    if (receivingplayer != null){
                                        receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                    }
                                }
                                else{
                                    player.sendTextMessage("[#FF0000]Player name " + receivername + " not found! Try [/mail lookup string] to check name spelling.");
                                }
                            }
                        }
                        break;
                    case "groupr":
                        if (cmd.length >= 4){
                            String[] playernames = cmd[2].split(",");
                            String message = "";
                            for (int i=3;i<cmd.length;i++){
                                message = message + " " + cmd[i];
                            }
                            for (String receivername : playernames){
                                boolean namecheck = false;
                                WorldDatabase database = getWorldDatabase();
                                try(ResultSet result = database.executeQuery("SELECT Name FROM Player")){
                                    while (result.next()){
                                        String name = result.getString("Name");
                                        if (name.equals(receivername)){
                                            namecheck = true;
                                            break;
                                        }
                                    }
                                }
                                catch (SQLException e){
                                }

                                if (namecheck == true){
                                    DatabaseStuff db =  new DatabaseStuff();
                                    db.sendMail(player.getName(), receivername, message, true, false);
                                    player.sendTextMessage(SettingbyName("PluginTextColour") + "Your message to " + receivername + " has been successfully sent!");
                                    Player receivingplayer = getServer().getPlayer(receivername);
                                    if (receivingplayer != null){
                                        receivingplayer.sendTextMessage(SettingbyName("PluginTextColour") + "You have a new mail message! Type [/mail read] to see it.");
                                    }
                                }
                                else{
                                    player.sendTextMessage("[#FF0000]Player name " + receivername + " not found! Try [/mail lookup string] to check name spelling.");
                                }
                            }
                        }
                        break;
                    case "cancel":
                        if (player.isAdmin()){
                            if (cmd.length >= 3){
                                String regarding = "";
                                for (int i=2;i<cmd.length;i++){
                                    regarding = regarding + " " + cmd[i];
                                }
                                DatabaseStuff db = new DatabaseStuff();
                                db.cancelMail(regarding);
                            }
                        }
                        break;
                    case "reload":
                        if (player.isAdmin()){
                            if (cmd.length == 2){
                                ReadSettings();
                                TimerHolder.getTimer().kill();
                                MailCheckTimer();
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "MailingSystem has been successfully reloaded!");
                            }
                        }
                        break;
                    case "help":
                        if (cmd.length == 2){
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "Available MailingSystem Commands:");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail read, displays all unread messages and then deletes them from the list");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail lookup string, returns all player names starting with the specified string");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail send playername message, sends a mail message to the specified player");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail sendr playername message, sends a mail message to the specified player and you receive a receipt mail when they read it");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail group playername1,playername2,etc. message, sends a mail message to the specified players. Note: no spaces between playernames");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail groupr playername1,playername2,etc. message, sends a mail message to the specified players and you receive a receipt mail when they read it. Note: no spaces between playernames");
                            player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail admins message, sends a mail message to all admins. Please do not abuse!");
                            if (player.isAdmin()){
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail sendall message, sends a mail message to all active players");
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail reload, reads the mail.properties file again to reinitialise the plugin settings without the need to restart your server");
                                player.sendTextMessage(SettingbyName("PluginTextColour") + "/mail cancel regarding, cancels all mail messages starting with regarding");
                            }
                        }
                        break;
                    default:
                        player.sendTextMessage("[#FF0000]You have to provide a correct MailingSystem command!");
                        player.sendTextMessage("[#FF0000]For more information type: /mail help");
                        break;
                }
            }
        }
    }
    
    @EventMethod
    public void onPlayerSpawn(PlayerSpawnEvent event){
        Player player = event.getPlayer();
        DatabaseStuff db =  new DatabaseStuff();
        if (!db.getMail(player.getName()).isEmpty()){
            Timer timer = new Timer(0,2,1,()->{
                player.sendTextMessage(SettingbyName("PluginTextColour") + "You have unread mail messages! Type [/mail read] to see them.");
            });
            timer.start();
        }
    }
    
    @Override
    public void onDisable(){
    }
    
}
