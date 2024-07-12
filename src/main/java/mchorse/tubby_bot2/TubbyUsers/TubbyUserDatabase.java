package mchorse.tubby_bot2.TubbyUsers;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.*;

/**
 * Class TubbyUserDatabase
 * Saves and fetches users
 */
public class TubbyUserDatabase {

    private File jsonFile;
    private JsonObject usersJsonObject;

    /**
     * Constructor of TubbyUserDatabase.
     * If the jsonFile doesn't exists, it will create it and initialize it with the default json string.
     * It automatically takes the json string inside the jsonFile and turns it into a JsonObject
     * @param jsonFile
     * @throws FileNotFoundException
     */
    public TubbyUserDatabase(File jsonFile) throws FileNotFoundException {
        this.jsonFile = jsonFile;

        if(!jsonFile.exists()) {
            try {
                jsonFile.createNewFile();
                FileWriter fileWriter = new FileWriter(jsonFile);
                fileWriter.write("{\"users\":[]}");
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        JsonReader jsonReader = new Gson().newJsonReader(new FileReader(jsonFile));
        usersJsonObject = new Gson().fromJson(jsonReader, JsonObject.class);
    }

    /**
     * Save the tubby user entity in the cached JsonObject
     * @param tubbyUser
     */
    public void saveTubbyUserEntity(TubbyUserEntity tubbyUser) {
        JsonObject tubbyUserJsonObj = new Gson().fromJson(new Gson().toJson(tubbyUser), JsonObject.class);

        int tubbyUserIndexInDB = doesTubbyUserEntityExists(tubbyUser);
        if(tubbyUserIndexInDB >= 0) {
            // User exists, we need to replace the data
            usersJsonObject.getAsJsonArray("users").set(tubbyUserIndexInDB, tubbyUserJsonObj);
            return;
        }

        usersJsonObject.getAsJsonArray("users").add(tubbyUserJsonObj);
    }

    /**
     * Write the JsonObject in the disk at the specified location in constructor
     */
    public void save() throws IOException {
        FileWriter fileWriter = new FileWriter(jsonFile);
        String jsonString = new Gson().toJson(usersJsonObject);

        fileWriter.write(jsonString);
        fileWriter.close();

        System.out.println("Tubby Bot saved Tubby User Data with success!");
    }


    /**
     * Checks whether a TubbyUser exists or not. If yes, it will returns the index of the user in the json. If not
     * it returns -1
     * @param tubbyUser
     * @return index of user, if do not exists then -1
     */
    private int doesTubbyUserEntityExists(TubbyUserEntity tubbyUser) {
        JsonArray users = usersJsonObject.getAsJsonArray("users");
        int idx = 0;

        for(JsonElement elm : users.asList()) {
            JsonObject user = elm.getAsJsonObject();

            if(user.get("discordUserId").getAsString().equals(tubbyUser.getDiscordUserId())) return idx;

            idx++;
        }

        return -1;
    }

    /**
     * Get a tubby user
     * @param discordUserId
     * @return TubbyUserEntity of the discordUserId
     */
    public TubbyUserEntity getTubbyUser(String discordUserId) {
        JsonArray users = usersJsonObject.getAsJsonArray("users");
        for (JsonElement user : users) {
            JsonObject current = user.getAsJsonObject();

            if (current.has("discordUserId")) {
                if (current.get("discordUserId").getAsString().equals(discordUserId)) {
                    return new Gson().fromJson(current, TubbyUserEntity.class);
                }
            }
        }

        TubbyUserEntity newUser = new TubbyUserEntity(discordUserId);
        try {
            save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return newUser;
    }
    
}
