package mchorse.tubby_bot2;

import mchorse.tubby_bot2.TubbyUsers.TubbyUserDatabase;
import mchorse.tubby_bot2.TubbyUsers.TubbyUserEntity;
import mchorse.tubby_bot2.utils.UrlValidator;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main extends ListenerAdapter
{
    public static final Pattern PATTERN = Pattern.compile("((?<=[^@]\\!)[\\w\\d\\-]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static Words FAQ;
    public static Words responses;

    public static TubbyUserDatabase tubbyUserDatabase;
    public static List<String> channelUrlGivenOnlyChannels = List.of("945032094344687657");


    public static void main(String[] args)
    {
        FAQ = new Words("faq", new File("./faq.json"));
        responses = new Words("responses", new File("./responses.json"));

        JDABuilder builder = JDABuilder.createDefault(args[0]);

        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.disableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setActivity(Activity.playing("/faq"));
        builder.addEventListeners(new Main());

        JDA jda = builder.build();


        jda.updateCommands().addCommands(
            /* FAQ */
            Commands.slash("faq", "Display an FAQ")
                .setGuildOnly(true)
                .addOption(OptionType.STRING, "entry", "FAQ entry you want the bot to display in the chat"),
            Commands.slash("faq-all", "Display all FAQs")
                .setGuildOnly(true),
            Commands.slash("faq-set", "Update (or insert) an FAQ")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.STRING, "entry", "FAQ entry you want the bot to update")
                .addOption(OptionType.STRING, "content", "Content of that FAQ entry"),

            /* Responses */
            Commands.slash("response-set", "Update (or insert) a response")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
                .addOption(OptionType.STRING, "entry", "Response you want the bot to update")
                .addOption(OptionType.STRING, "content", "Content of that response"),

            /* Channels */
            Commands.slash("get-channel", "Get the channel of a user!")
                .setGuildOnly(true)
                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND))
                .addOption(OptionType.USER, "user", "User you want to get the channel of"),

            Commands.slash("set-my-channel", "Set your YouTube/Bilibili or whatever channel!!")
                    .setGuildOnly(true)
                    .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MESSAGE_SEND))
                    .addOption(OptionType.STRING, "channel_url", "Your channelUrl!")
        ).queue();

        /* Tubby User Database */
        try {
            tubbyUserDatabase = new TubbyUserDatabase(new File("./users.json"));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static String wrapFaq(String key, String content)
    {
        return "> *FAQ entry for* `!" + key + "`*:*\n" + content;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event)
    {
        if (event.getAuthor().isBot())
        {
            return;
        }

        processChannelUrlOnlyGivenChannels(event);
        processFaqChatMessage(event);
    }

    /**
     * Process the message and check if it was sent in a channel that requires the channel_url in the TubbyUserEntity to be set
     * @param event
     */
    public void processChannelUrlOnlyGivenChannels(MessageReceivedEvent event) {
        Channel channel = event.getChannel();
        User eventAuthor = event.getAuthor();
        TubbyUserEntity tubbyUser = tubbyUserDatabase.getTubbyUser(eventAuthor.getId());

        if(!channelUrlGivenOnlyChannels.contains(channel.getId())) return;
        if(!tubbyUser.getChannelUrl().isBlank()) return; // Checking if user defined a channel in Tubby Bot

        event.getMessage().getChannel().sendMessage("Hey, " + eventAuthor.getAsMention() + " you cannot send messages in this channel. To be able to send messages, please update your channel url using the command /set-my-channel [channel-url].").queue();
        event.getMessage().delete().queue();
    }

    /**
     * Process the message and send the corresponding FAQ
     * @param event
     */
    public void processFaqChatMessage(MessageReceivedEvent event) {
        /* Matcher can't find if the entire string matches the pattern, hence empty space */
        String message = event.getMessage().getContentDisplay().trim();

        if (responses.has(message))
        {
            event.getChannel().sendMessage(responses.get(message)).queue();
        }

        if (!message.contains("!"))
        {
            return;
        }

        Matcher matcher = PATTERN.matcher(" " + message);
        List<String> matches = new ArrayList<String>();

        while (matcher.find())
        {
            matches.add(matcher.group(1));
        }

        if (matches.isEmpty())
        {
            return;
        }

        for (String key : matches)
        {
            String content = FAQ.get(key);

            event.getChannel().sendMessage(wrapFaq(key, content)).queue();
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event)
    {
        String name = event.getName();

        if (name.equals("faq"))
        {
            this.printFAQEntry(event);
        }
        else if (name.equals("faq-all"))
        {
            this.printAllFAQEntries(event);
        }
        else if (name.equals("faq-set"))
        {
            this.updateEntry(FAQ, "FAQ entry", event);
        }
        else if (name.equals("response-set"))
        {
            this.updateEntry(responses, "Response", event);
        }
        else if (name.equals("get-channel"))
        {
            this.printChannel(event);
        }
        else if (name.equals("set-my-channel"))
        {
            this.setMyChannel(event);
        }
    }

    private void printFAQEntry(SlashCommandInteractionEvent event)
    {
        String key = event.getOption("entry", OptionMapping::getAsString);

        event.deferReply().queue();

        if (key != null)
        {
            event.getHook().editOriginal(wrapFaq(key, FAQ.get(key))).queue();
        }
        else
        {
            event.getHook().editOriginal("Please provide `entry` option!").queue();
        }
    }

    /**
     * get-channel slash command handler
     * @param event
     */
    private void printChannel(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        User user = event.getOption("user", OptionMapping::getAsUser);
        User eventAuthor = event.getUser();

        if (user == null)
        {
            event.getHook().editOriginal("Please provide `user` option!").queue();
            return;
        }



        TubbyUserEntity tubbyUser = tubbyUserDatabase.getTubbyUser(user.getId());

        if (tubbyUser.getChannelUrl() == null) {
            // Profile do not exists, therefore channel is not set
            event.getHook().editOriginal("User do not have a YouTube channel registered in my database!").queue();
            return;
        }

        event.getHook().editOriginal("Here's the channel of " + user.getName() + ". \nPlease note that crediting in the channel description is not enough. Credit must be in every videos.\n" + tubbyUser.getChannelUrl()).queue();
    }

    /**
     * set-my-channel slash command handler
     * @param event
     */
    private void setMyChannel(SlashCommandInteractionEvent event) {
        event.deferReply().queue();
        User eventAuthor = event.getUser();

        // Arguments
        String channelUrl = event.getOption("channel_url", OptionMapping::getAsString);

        if(!UrlValidator.isValidURL(channelUrl)) {
            event.getHook().editOriginal("The url you gave is not valid!").queue();
            return;
        }

        TubbyUserEntity tubbyUser = tubbyUserDatabase.getTubbyUser(eventAuthor.getId());
        tubbyUser.setChannelUrl(channelUrl);

        tubbyUserDatabase.saveTubbyUserEntity(tubbyUser);
        try {
            tubbyUserDatabase.save();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        event.getHook().editOriginal("Your channel url has been updated, thank you!").queue();
    }



    private void printAllFAQEntries(SlashCommandInteractionEvent event)
    {
        List<String> messages = new ArrayList<String>();
        String separator = ", ";

        if (FAQ.strings.isEmpty())
        {
            messages.add("none...");
        }
        else
        {
            String m = FAQ.listAll();

            m = "Following FAQ entries are available:\n\n" + m;
            messages.add(m);
        }

        while (messages.get(messages.size() - 1).length() >= 2000)
        {
            String m = messages.get(messages.size() - 1);
            int i = m.lastIndexOf(separator);

            while (i >= 2000)
            {
                i = m.lastIndexOf(separator, i - 1);
            }

            String first = m.substring(0, i);
            String second = m.substring(i + separator.length());

            messages.set(messages.size() - 1, first);
            messages.add(second);
        }

        event.deferReply().queue();
        event.getHook().editOriginal(messages.get(0)).queue();

        if (messages.size() > 1)
        {
            for (int i = 1; i < messages.size(); i++)
            {
                event.getChannel().sendMessage(messages.get(i)).queue();
            }
        }
    }

    private void updateEntry(Words words, String type, SlashCommandInteractionEvent event)
    {
        String key = event.getOption("entry", OptionMapping::getAsString);
        String content = event.getOption("content", OptionMapping::getAsString);

        event.deferReply().queue();

        if (key != null)
        {
            if (content == null || content.trim().isEmpty())
            {
                words.remove(key);

                event.getHook().editOriginal(type + " `" + key + "` was removed!").queue();
            }
            else
            {
                words.put(key, content);

                event.getHook().editOriginal(type + " `" + key + "` was updated!").queue();
            }
        }
        else
        {
            event.getHook().editOriginal("Please provide `entry` and `content` options!").queue();
        }
    }
}