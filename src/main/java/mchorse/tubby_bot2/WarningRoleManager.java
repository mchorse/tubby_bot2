package mchorse.tubby_bot2;

import com.google.gson.*;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WarningRoleManager extends ListenerAdapter
{
    private File db;
    private Guild server;
    private final Map<String, Warning> warnings = new HashMap<>();
    private final Map<Long, UserWarning> users = new HashMap<>();

    public WarningRoleManager(String filename)
    {
        this.db = new File(filename);
    }

    private JsonObject getJsonDB() throws IOException {
        StringBuilder json = new StringBuilder();

        BufferedReader reader = new BufferedReader(new FileReader(this.db, StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null)
        {
            json.append(line).append("\n");
        }
        reader.close();

        return JsonParser.parseString(json.toString()).getAsJsonObject();
    }

    private void readDB() throws IOException
    {
        if (this.server == null) return;

        JsonObject dbJson = this.getJsonDB();
        JsonObject levels = dbJson.getAsJsonObject("levels");

        for (String key : levels.keySet())
        {
            JsonObject warning = levels.getAsJsonObject(key);
            if (this.server.getRoleById(warning.get("id").getAsLong()) == null) continue;

            this.warnings.put(key, new Warning(key,
                    warning.get("id").getAsLong(),
                    warning.get("expiration").getAsLong()));
        }

        if (this.updateUsers())
        {
            this.saveUsers();
        }
    }

    public void saveUsers() throws IOException
    {
        JsonObject dbJson = this.getJsonDB();

        JsonArray usersJson = new JsonArray();
        for(Map.Entry<Long, UserWarning> user : this.users.entrySet())
        {
            usersJson.add(user.getValue().toJson());
        }

        dbJson.add("users", usersJson);
    }

    /**
     * Updates the users' waring roles or removes them if they
     * either left, warning role expired, have no waring role anymore on the server or the data is corrupt.
     * @return true if a user has been modified in the data. False if nothing has been updated.
     * @throws IOException
     */
    public boolean updateUsers() throws IOException
    {
        if (this.server == null) return false;

        this.users.clear();
        JsonObject dbJson = this.getJsonDB();

        JsonArray users = dbJson.getAsJsonArray("users");

        boolean update = false;
        for (JsonElement element : users.asList())
        {
            JsonObject user = element.getAsJsonObject();
            long userID = user.get("id").getAsLong();
            Member member = this.server.getMemberById(userID);

            if (member == null)
            {
                users.remove(element);
                update = true;
                continue;
            }


            UserWarning userWarning;
            try
            {
                userWarning = UserWarning.fromJson(user, this.warnings);
            }
            catch (IOException e) {
                users.remove(element);
                update = true;
                continue;
            }

            if (System.currentTimeMillis() > user.get("expiration").getAsLong()
                    && user.get("expiration").getAsLong() != -1)
            {
                Role role = this.server.getRoleById(userWarning.warning.roleID);
                UserSnowflake userSnowflake = this.server.getMemberById(userWarning.userID);

                if (userSnowflake != null && role != null)
                {
                    this.server.removeRoleFromMember(userSnowflake, role).queue();
                }

                update = true;
                continue;
            }

            this.users.put(userID, userWarning);
        }

        return update;
    }

    @Override
    public void onGuildMemberRemove(GuildMemberRemoveEvent event)
    {
        if (this.server == null) return;

        if (this.warnings.containsKey(event.getMember().getIdLong()))
        {
            try
            {
                this.updateUsers();
                this.saveUsers();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onReady(ReadyEvent event)
    {
        /* McHorse's Pub, noice. */
        this.server = event.getJDA().getGuildById(252148970338385921L);

        if (this.server != null)
        {
            try
            {
                this.readDB();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event)
    {
        if (this.server == null) return;

        List<Role> added = event.getRoles();
        for (Role role : added)
        {
            long userID = event.getMember().getIdLong();
            Warning warning = this.warnings.get(role.getIdLong());

            if (warning != null)
            {
                this.users.put(userID, new UserWarning(userID,System.currentTimeMillis() + warning.expirationTime, warning));

                try
                {
                    this.saveUsers();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }

                return;
            }
        }
    }

    private static class UserWarning
    {
        private long expirationTimeStamp;
        private Warning warning;
        private long userID;

        public UserWarning(long userID, long expirationTimeStamp, Warning warning)
        {
            this.expirationTimeStamp = expirationTimeStamp;
            this.warning = warning;
            this.userID = userID;
        }

        public static UserWarning fromJson(JsonObject user, Map<String, Warning> warnings) throws IOException
        {
            Warning warn = null;
            for(Map.Entry<String, Warning> element : warnings.entrySet()) {
                if (user.get("level").getAsString().equals(element.getKey())) {
                    warn = element.getValue();
                }
            }

            if (warn == null) {
                throw new IOException("Warning level could not be found.");
            }

            return new UserWarning(user.get("id").getAsLong(), user.get("expiration").getAsLong(), warn);

        }

        public JsonObject toJson()
        {
            JsonObject userWarning = new JsonObject();

            userWarning.addProperty("level", this.warning.level);
            userWarning.addProperty("id", this.userID);
            userWarning.addProperty("expiration", this.expirationTimeStamp);

            return userWarning;
        }
    }

    private static class Warning
    {
        private String level;
        /**
         * Time in millis how long the warning role should last
         */
        private long expirationTime;
        private long roleID;

        public Warning(String level, long roleID, long expirationTime)
        {
            this.level = level;
            this.roleID = roleID;
            this.expirationTime = expirationTime;
        }
    }
}
