package com.divinitor.discord.wahrbot.ext.mod.commands.reactrole;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.mod.ModModule;
import com.divinitor.discord.wahrbot.ext.mod.listeners.ReactionService;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class ReactRoleInitCmd extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "init";
    public static final String KEY = ModModule.REACTROLE_KEY + ".commands." + COMMAND_ID;
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
                .queue(null, handleQueueException());
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
                    .queue(null, handleQueueException());
                return CommandResult.rejected();
            }
        } else {
            targetChannel = context.getInvocationChannel();
        }


        Message targetMessage = null;
        try {
            targetMessage = targetChannel.retrieveMessageById(targetMessageId).complete();
        } catch (Exception e) {
        }
        if (targetMessage == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.message_not_found"),
                l, SnowflakeUtils.encode(targetMessageId), nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue(null, handleQueueException());
            return CommandResult.rejected();
        }

        if (!PermissionUtil.checkPermission(targetChannel, server.getSelfMember(), Permission.MESSAGE_ADD_REACTION)) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.noperm.bot.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.noperm.bot.body"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue(null, handleQueueException());
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
            .queue(null, handleQueueException());

        return CommandResult.ok();
    }

    @NotNull
    private Consumer<Throwable> handleQueueException() {
        return e -> {
            throw new RuntimeException(e);
        };
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

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected String getResourcePath() {
        return ModModule.REACTROLE_PATH + "." + COMMAND_ID;
    }
}
