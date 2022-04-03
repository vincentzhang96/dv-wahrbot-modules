package com.divinitor.discord.wahrbot.ext.basics.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class SIDCmd implements Command {


    public static final String KEY = "com.divinitor.discord.wahrbot.ext.basics.commands.sid";
    private final Localizer loc;


    @Inject
    public SIDCmd(Localizer loc) {
        this.loc = loc;
    }


    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        if (!line.hasNext()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("err.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("err.no_args"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.rejected();
        }

        String next = line.next();
        String converted;
        try {
            if (next.startsWith(SnowflakeUtils.PREFIX)) {
                converted = SnowflakeUtils.decodeToString(next);
            } else {
                converted = SnowflakeUtils.encode(next);
            }

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(loc.localizeToLocale(this.key("resp.title"), l, nlcp));
            builder.setDescription(loc.localizeToLocale(this.key("resp.desc"), l, next, converted, nlcp));
            builder.setColor(Color.GREEN);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.ok();
        } catch (Exception e) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("err.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("err.invalid"), l, next, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.rejected();
        }
    }

    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }
}

