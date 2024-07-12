package mchorse.tubby_bot2.TubbyUsers;

public class TubbyUserEntity {

    private final String discordUserId;
    private String channelUrl;

    public TubbyUserEntity(String discordUserId) {
        this.discordUserId = discordUserId;
    }

    public String getDiscordUserId() {
        return discordUserId;
    }

    public String getChannelUrl() {
        return channelUrl;
    }

    public void setChannelUrl(String channelUrl) {
        this.channelUrl = channelUrl;
    }
}
