package mchorse.tubby_bot2.utils;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class UrlValidator {

    private static final String URL_PATTERN =
            "^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-@]*)*\\/?$";

    /**
     * Determines whether the channelUrl is a valid url
     * @param channelUrl
     * @return true if a valid url, else false
     */
    public static boolean isValidURL(String channelUrl) {
        Pattern pattern = Pattern.compile(URL_PATTERN);
        Matcher matcher = pattern.matcher(channelUrl);

        return matcher.matches();
    }
}
