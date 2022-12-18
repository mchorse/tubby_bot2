package mchorse.tubby_bot2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

public class Words
{
    public String id;
    public File file;
    public Map<String, String> strings;

    private static String readFile(File file)
    {
        StringBuilder contentBuilder = new StringBuilder();

        try (Stream<String> stream = Files.lines(Paths.get(file.getAbsolutePath()), StandardCharsets.UTF_8))
        {
            stream.forEach(s -> contentBuilder.append(s).append("\n"));
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return contentBuilder.toString();
    }

    private static Map<String, String> parse(File file)
    {
        String code = readFile(file);
        JsonObject object = JsonParser.parseString(code).getAsJsonObject();
        Map<String, String> map = new HashMap<String, String>();

        for (String key : object.keySet())
        {
            JsonElement element = object.getAsJsonPrimitive(key);

            map.put(key, element.getAsString());
        }

        return map;
    }

    private static void writeFile(File file, String content)
    {
        try
        {
            FileOutputStream stream = new FileOutputStream(file);

            stream.write(content.getBytes());
            stream.close();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public Words(String id, File file)
    {
        this.id = id;
        this.file = file;
        this.strings = parse(file);
    }

    public void save(String key)
    {
        StringWriter writer = new StringWriter();
        JsonWriter jsonWriter = new JsonWriter(writer);
        Gson gson = (new GsonBuilder()).setPrettyPrinting().create();

        jsonWriter.setIndent("    ");
        gson.toJson(this.strings, Map.class, jsonWriter);

        writeFile(this.file, writer.toString());

        System.out.println(this.id + " was saved! Latest key is: " + key);
    }

    public boolean has(String key)
    {
        key = key.trim().toLowerCase();

        return this.strings.containsKey(key);
    }

    public String get(String key)
    {
        key = key.trim().toLowerCase();

        if (this.has(key))
        {
            return this.strings.get(key);
        }

        return "An FAQ named " + key + " doesn't exist!";
    }

    public boolean put(String key, String value)
    {
        key = key.trim().toLowerCase();

        boolean had = this.has(key);

        this.strings.put(key, value);
        this.save(key);

        return had;
    }

    public boolean remove(String key)
    {
        key = key.trim().toLowerCase();

        if (this.has(key))
        {
            this.strings.remove(key);

            this.save(key);

            return true;
        }

        return false;
    }
}
