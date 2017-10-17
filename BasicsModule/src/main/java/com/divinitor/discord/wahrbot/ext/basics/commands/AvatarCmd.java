package com.divinitor.discord.wahrbot.ext.basics.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.discord.MemberResolution;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.util.Locale;
import java.util.StringJoiner;

public class AvatarCmd implements Command {

    public static final String KEY = "com.divinitor.discord.wahrbot.ext.basics.commands.avatar";
    private final Localizer loc;

    @Inject
    public AvatarCmd(Localizer loc) {
        this.loc = loc;
    }

    @Override
    public CommandResult invoke(CommandContext context) {

        Message message = context.getMessage();
        String userLookup = context.getCommandLine().remainder();
        Member member;
        if (userLookup.isEmpty()) {
            member = context.getMember();
        } else if (!message.getMentionedUsers().isEmpty()) {
            member = context.getServer().getMember(message.getMentionedUsers().get(0));
        } else {
            member = MemberResolution.findMember(userLookup, context);
        }

        Message ret;
        if (member == null) {
            ret = this.notFound(userLookup, context.getLocale());
            context.getFeedbackChannel().sendMessage(ret)
                .queue();
            return CommandResult.rejected();
        } else {
            ret = this.forMember(member, context);
            context.getFeedbackChannel().sendMessage(ret)
                .queue();
            return CommandResult.ok();
        }
    }

    private Message forMember(Member member, CommandContext context) {
        User user = member.getUser();
        Locale l = context.getLocale();

        EmbedBuilder builder = new EmbedBuilder();

        //  AUTHOR
        {
            builder.setColor(member.getColor());
            builder.setAuthor(loc.localizeToLocale(key("author"), l, user.getName(), user.getDiscriminator()),
                null,
                user.getEffectiveAvatarUrl());
        }


        //  IMAGE
        {
            builder.setImage(user.getEffectiveAvatarUrl() + "?size=1024");
        }

        //  FOOTER
        {
            builder.setFooter(loc.localizeToLocale(key("footer"), l,
                user.getId(), SnowflakeUtils.encode(user.getIdLong())), null);
        }

        return new MessageBuilder()
            .setEmbed(builder.build())
            .build();
    }


    private Message notFound(String query, Locale locale) {
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(loc.localizeToLocale(key("err", "not_found"), locale));
        builder.setDescription(loc.localizeToLocale(key("err", "not_found", "desc"), locale, query));
        builder.setColor(Color.ORANGE);

        return new MessageBuilder()
            .setEmbed(builder.build())
            .build();
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
