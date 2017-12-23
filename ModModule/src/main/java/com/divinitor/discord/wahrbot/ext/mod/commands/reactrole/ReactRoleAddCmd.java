package com.divinitor.discord.wahrbot.ext.mod.commands.reactrole;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.mod.listeners.ReactionService;
import com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.lang.invoke.MethodHandles;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

import static com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils.SERVER_STORE_MESSAGE_SET_KEY;
import static com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils.serverStoreMessageRoleMapKey;

public class ReactRoleAddCmd implements Command {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String KEY = "com.divinitor.discord.wahrbot.ext.mod.commands.reactrole.commands.add";
    private final Localizer loc;
    private ReactionService listener;

    private final Pattern CHANNEL_NAME_PATTERN = Pattern.compile("[0-9a-z]([0-9a-z_\\-]+)");

    @Inject
    public ReactRoleAddCmd(Localizer loc) {
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
            return rejectMissingArgs(context);
        }

        Message msg = context.getMessage();
        Guild server = context.getServer();
        long mid = 0;
        Channel channel = null;

        //  Message ID
        if (!line.hasNext()) {
            return rejectMissingArgs(context);
        }
        String target = line.next();
        if (target.startsWith(SnowflakeUtils.PREFIX)) {
            mid = SnowflakeUtils.decode(target);
        } else {
            try {
                mid = Long.parseUnsignedLong(target);
            } catch (Exception ignored) {
            }
        }

        if (mid == 0) {
            return rejectMissingMessageId(context, target);
        }

        //  We have to do some lookahead for this since channel ID can come next
        //  Channel IDs can only be channel mentions or [0-9a-z]([0-9a-z_\-]+)
        if (!line.hasNext()) {
            return rejectMissingArgs(context);
        }

        boolean defaultChannel = false;

        String next = line.next();
        if (next.startsWith("<#")) {
            //  Channel mention
            List<TextChannel> mentioned = msg.getMentionedChannels();
            if (!mentioned.isEmpty()) {
                channel = mentioned.get(0);
            }
        } else if (next.startsWith("$")) {
            long cid = SnowflakeUtils.decode(next);
            channel = server.getTextChannelById(cid);
        } else if (CHANNEL_NAME_PATTERN.matcher(next).matches()) {
            List<TextChannel> matches = server.getTextChannelsByName(next, true);
            if (!matches.isEmpty()) {
                channel = matches.get(0);
            }
        } else {
            //  Try number parse
            try {
                long cid = Long.parseUnsignedLong(next);
                channel = server.getTextChannelById(cid);
            } catch (Exception e) {
                channel = msg.getTextChannel();
                defaultChannel = true;
            }
        }

        if (channel == null) {
            return rejectUnknownChannel(context, next);
        }

        ServerStore serverStore = context.getServerStorage();
        Set<String> config = serverStore.getObject(SERVER_STORE_MESSAGE_SET_KEY, Set.class);
        String encodedTargetMessageId = SnowflakeUtils.encode(mid);
        String encodedTargetChannelId = SnowflakeUtils.encode(channel);
        if (!config.contains(ReactionUtils.messageChannelPair(encodedTargetChannelId, encodedTargetMessageId))) {
            return rejectUnregisteredReactRole(context, mid);
        }

        if (!defaultChannel) {
            if (!line.hasNext()) {
                return rejectMissingArgs(context);
            }

            next = line.next();
        }

        String emojiKey;
        if (next.startsWith("<:")) {
            List<Emote> emotes = msg.getEmotes();
            if (emotes.isEmpty()) {
                return rejectParseFail(context, next);
            }
            Emote emote = emotes.get(0);
            emojiKey = SnowflakeUtils.encode(emote);
        } else {
            emojiKey = next;
        }









        Map<String, String> rolesForMsg = serverStore.getObject(serverStoreMessageRoleMapKey(mid), Map.class);



//        String encodedTargetMessageId = SnowflakeUtils.encode(targetMessageId);
//        String encodedTargetChannelId = SnowflakeUtils.encode(targetChannel);
//        config.add(ReactionUtils.messageChannelPair(encodedTargetChannelId, encodedTargetMessageId));
        this.listener.invalidateGuild(server);

        //  Respond
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("success.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("success.body"), l, nlcp));
        builder.setColor(Color.GREEN);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();

        return CommandResult.ok();
    }

    @NotNull
    private CommandResult rejectMissingArgs(CommandContext context) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.no_args"), l, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectMissingMessageId(CommandContext context, String target) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.message_not_found"), l,
            target, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectUnknownChannel(CommandContext context, String target) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.channel_not_found"), l,
            target, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectUnregisteredReactRole(CommandContext context, long mid) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.message_not_init"), l,
            SnowflakeUtils.encode(mid), nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectParseFail(CommandContext context, String msg) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.message_not_init"), l,
            msg, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();
        return CommandResult.rejected();
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
