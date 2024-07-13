package mchorse.tubby_bot2.utils;

import java.util.regex.Pattern;

public class UrlValidator
{
    private static final Pattern URL_PATTERN = Pattern.compile("^(https?:\\/\\/)?([\\da-z.-]+)\\.([a-z.]{2,6})([\\/\\w .-@]*)*\\/?$");

    /**
     * Determines whether the channelUrl is a valid url
     *
     * @param channelUrl
     * @return true if a valid url, else false
     */
    public static boolean isValidURL(String channelUrl)
    {
        return URL_PATTERN.matcher(channelUrl).matches();
    }
}