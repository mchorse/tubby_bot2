package mchorse.tubby_bot2;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Main extends ListenerAdapter
{
    public static final Pattern PATTERN = Pattern.compile("((?<=[^@]\\!)[\\w\\d\\-]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

    public static Words FAQ;
    public static Words responses;

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
                .addOption(OptionType.STRING, "content", "Content of that response")
        ).queue();
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

    private void printAllFAQEntries(SlashCommandInteractionEvent event)
    {
        List<String> keys = new ArrayList<String>(FAQ.strings.keySet());
        List<String> messages = new ArrayList<String>();
        String separator = ", ";

        if (keys.isEmpty())
        {
            messages.add("none...");
        }
        else
        {
            keys.sort(String::compareTo);

            String m = keys.stream().map(elem -> "` " + elem + " `").collect(Collectors.joining(separator));

            m = "Following FAQ entries are available: " + m;
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

                event.getHook().editOriginal(type + " " + key + " was removed!").queue();
            }
            else
            {
                words.put(key, content);

                event.getHook().editOriginal(type + " " + key + " was updated!").queue();
            }
        }
        else
        {
            event.getHook().editOriginal("Please provide `entry` and `content` options!").queue();
        }
    }
}