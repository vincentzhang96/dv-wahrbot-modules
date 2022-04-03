package com.divinitor.discord.wahrbot.ext.mod.commands.general;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

import static com.divinitor.discord.wahrbot.core.command.CommandConstraints.hasAny;
import static net.dv8tion.jda.api.Permission.MESSAGE_MANAGE;

public class VanishCmd implements Command {

    public static final String KEY = "ext.mod.commands.general.vanish";
    private final Localizer loc;

    @Inject
    public VanishCmd(Localizer loc) {
        this.loc = loc;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        if (!line.hasNext()) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.no_args"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.rejected();
        }

        String amountStr = line.next();

        int count;

        try {
            count = Integer.parseInt(amountStr);
        } catch (NumberFormatException nfe) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.invalid_number"), l, amountStr, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.rejected();
        }

        if (count < 0 || count > 200) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.invalid_amount"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(new MessageBuilder().setEmbeds(builder.build()).build())
                .queue();
            return CommandResult.rejected();
        }



        return null;
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return hasAny(MESSAGE_MANAGE);
    }

    @Override
    public CommandConstraint<CommandContext> getBotPermissionConstraints() {
        return hasAny(MESSAGE_MANAGE);
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
