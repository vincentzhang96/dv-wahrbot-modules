package com.divinitor.discord.wahrbot.ext.mod.commands.reactrole;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.mod.listeners.ReactionService;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class ReactRoleInitCmd implements Command {

    public static final String KEY = "com.divinitor.discord.wahrbot.ext.mod.commands.reactrole.commands.init";
    private final Localizer loc;
    private ReactionService listener;

    @Inject
    public ReactRoleInitCmd(Localizer loc) {
        this.loc = loc;
    }

    public void setListener(ReactionService listener) {
        this.listener = listener;
    }

    @SuppressWarnings("unchecked")
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
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.rejected();
        }

        String target = line.next();
        long targetMessageId = 0;
        if (target.startsWith(SnowflakeUtils.PREFIX)) {
            targetMessageId = SnowflakeUtils.decode(target);
        } else {
            try {
                targetMessageId = Long.parseUnsignedLong(target);
            } catch (Exception ignored) {
            }
        }

        Message message = context.getMessage();
        Guild server = context.getServer();
        TextChannel targetChannel = null;
        if (line.hasNext()) {
            long targetChannelId;
            target = line.next();
            if (target.startsWith(SnowflakeUtils.PREFIX)) {
                targetChannelId = SnowflakeUtils.decode(target);
                targetChannel = server.getTextChannelById(targetChannelId);
            } else {
                List<TextChannel> mentionedChannels = message.getMentionedChannels();
                if (mentionedChannels.isEmpty()) {
                    try {
                        targetChannelId = Long.parseUnsignedLong(target);
                        targetChannel = server.getTextChannelById(targetChannelId);
                    } catch (Exception ignored) {
                        //  Try name lookup
                        List<TextChannel> textChannelsByName = server.getTextChannelsByName(target, true);
                        if (!textChannelsByName.isEmpty()) {
                            targetChannel = textChannelsByName.get(0);
                        }
                    }
                } else {
                    targetChannel = mentionedChannels.get(0);
                }
            }

            if (targetChannel == null) {
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
                builder.setDescription(this.loc.localizeToLocale(this.key("error.channel_not_found"), l, target, nlcp));
                builder.setColor(Color.RED);
                context.getFeedbackChannel().sendMessage(builder.build())
                    .queue();
                return CommandResult.rejected();
            }
        } else {
            targetChannel = context.getInvocationChannel();
        }


        Message targetMessage = targetChannel.getMessageById(targetMessageId).complete();
        if (targetMessage == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.message_not_found"),
                l, SnowflakeUtils.encode(targetMessageId), nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.rejected();
        }

        if (!PermissionUtil.checkPermission(targetChannel, server.getSelfMember(), Permission.MESSAGE_ADD_REACTION)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.noperm.bot.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.noperm.bot.body"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.handled();
        }


        ServerStore serverStore = context.getServerStorage();
        this.listener.initReactRoleMessage(serverStore, targetMessageId, targetChannel.getIdLong());

        //  Respond
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("success.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("success.body"), l,
            SnowflakeUtils.encode(targetMessageId), nlcp));
        builder.setColor(Color.GREEN);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();

        return CommandResult.ok();
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return CommandConstraints.hasAny(
            Permission.MANAGE_ROLES
        );
    }

    @Override
    public CommandConstraint<CommandContext> getBotPermissionConstraints() {
        return CommandConstraints.hasAll(
            Permission.MESSAGE_READ,
            Permission.MANAGE_ROLES,
            Permission.MESSAGE_ADD_REACTION
        );
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
