package mchorse.tubby_bot2.users;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Class TubbyUserDatabase
 *
 * Saves and fetches users
 */
public class TubbyUserDatabase
{
    private File jsonFile;
    private Map<String, TubbyUserEntity> users = new HashMap<>();

    /**
     * Reads the given JSON file as a JSON object
     *
     * @param jsonFile
     */
    public TubbyUserDatabase(File jsonFile)
    {
        this.jsonFile = jsonFile;

        try
        {
            this.users = new Gson().fromJson(new JsonReader(new FileReader(jsonFile)), new TypeToken<Map<String, TubbyUserEntity>>(){}.getRawType());
        }
        catch (Exception e)
        {}
    }

    /**
     * Save the tubby user entity in the cached JsonObject
     *
     * @param tubbyUser
     */
    public void addUser(TubbyUserEntity tubbyUser)
    {
        this.users.put(tubbyUser.getDiscordUserId(), tubbyUser);
    }

    /**
     * Writes all the users stored in the map to a JSON file
     */
    public boolean save()
    {
        try
        {
            FileWriter fileWriter = new FileWriter(this.jsonFile);
            String jsonString = new Gson().toJson(this.users);

            fileWriter.write(jsonString);
            fileWriter.close();

            System.out.println("Tubby Bot saved Tubby User Data with success!");

            return true;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Get a tubby user
     *
     * @param discordUserId
     * @return TubbyUserEntity of the discordUserId
     */
    public TubbyUserEntity getTubbyUser(String discordUserId)
    {
        return this.users.get(discordUserId);
    }
}