package mchorse.tubby_bot2.users;

import java.util.Objects;

public class TubbyUserEntity
{
    private final String discordUserId;
    private String channelUrl = "";

    public TubbyUserEntity(String discordUserId)
    {
        this.discordUserId = discordUserId;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equals = super.equals(obj);

        if (!equals && obj instanceof TubbyUserEntity)
        {
            TubbyUserEntity user = (TubbyUserEntity) obj;

            return Objects.equals(this.discordUserId, user.discordUserId)
                && Objects.equals(this.channelUrl, user.channelUrl);
        }

        return equals;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(this.discordUserId, this.channelUrl);
    }

    public String getDiscordUserId()
    {
        return discordUserId;
    }

    public String getChannelUrl()
    {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl)
    {
        this.channelUrl = channelUrl;
    }
}