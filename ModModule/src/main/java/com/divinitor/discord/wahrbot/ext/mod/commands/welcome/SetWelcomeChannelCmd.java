package com.divinitor.discord.wahrbot.ext.mod.commands.welcome;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.mod.ModModule;
import com.divinitor.discord.wahrbot.ext.mod.commands.JLMessageHelper;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.IMentionable;
import net.dv8tion.jda.core.entities.TextChannel;

import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class SetWelcomeChannelCmd extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "welcome.channel";
    public static final String KEY = ModModule.MODULE_KEY + ".commands." + COMMAND_ID;
    private final Localizer loc;

    @Inject
    public SetWelcomeChannelCmd(Localizer loc) {
        this.loc = loc;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();

        String command = line.peek().toLowerCase();
        if (!command.isEmpty()) {
            //  Consume
            line.next();
        }

        switch (command) {
            case "":
            case "list": {
                //  LIST
                return this.list(context);
            }
            case "default": {
                //  DEFAULT
                return this.setDefault(context, line);
            }
            case "set": {
                //  SET
                return this.set(context, line);
            }
            case "add": {
                //  ADD
                return this.add(context, line);
            }
            case "remove": {
                //  REMOVE
                return this.remove(context, line);
            }
            default:
                EmbedBuilder builder = new EmbedBuilder();
                builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
                builder.setDescription(this.loc.localizeToLocale(this.key("error.unkarg"), l, nlcp));
                builder.setColor(Color.RED);
                context.getFeedbackChannel().sendMessage(builder.build())
                    .queue();
                return CommandResult.rejected();
        }
    }

    private CommandResult remove(CommandContext context, CommandLine line) {
        TextChannel channel = resolveTextChannel(context, line);
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();

        if (channel == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.unkch"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.rejected();
        }

        Set<String> channels = context.getServerStorage().getObject("mod.jl.welcomeChannels",
            Set.class, String.class);

        channels.remove(channel.getId());
        this.ensureDefault(context, channels);

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("resp.title"), l, nlcp));
        builder.setDescription(loc.localizeToLocale(this.key("resp.desc.remove"), l, channel.getAsMention(), nlcp));
        builder.setColor(Color.GREEN);

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();

        return CommandResult.ok();
    }

    private CommandResult add(CommandContext context, CommandLine line) {
        TextChannel channel = resolveTextChannel(context, line);
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();

        if (channel == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.unkch"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.rejected();
        }

        Set<String> channels = context.getServerStorage().getObject("mod.jl.welcomeChannels",
            Set.class, String.class);

        this.ensureDefault(context, channels);

        channels.add(channel.getId());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("resp.title"), l, nlcp));
        builder.setDescription(loc.localizeToLocale(this.key("resp.desc.add"), l, channel.getAsMention(), nlcp));
        builder.setColor(Color.GREEN);

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();

        return CommandResult.ok();
    }

    private void ensureDefault(CommandContext context, Set<String> channels) {
        if (channels.isEmpty()) {
            //  Add default too
            TextChannel defaultChannel = JLMessageHelper.getDefaultChannel(context.getServer());
            if (defaultChannel != null) {
                channels.add(defaultChannel.getId());
            }
        }
    }

    private CommandResult set(CommandContext context, CommandLine line) {
        TextChannel channel = resolveTextChannel(context, line);

        return this.set(context, line, channel);
    }

    private CommandResult set(CommandContext context, CommandLine line, TextChannel channel) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();

        if (channel == null) {
            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
            builder.setDescription(this.loc.localizeToLocale(this.key("error.unkch"), l, nlcp));
            builder.setColor(Color.RED);
            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
            return CommandResult.rejected();
        }

        Set<String> channels = context.getServerStorage().getObject("mod.jl.welcomeChannels",
            Set.class, String.class);

        channels.clear();
        channels.add(channel.getId());

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("resp.title"), l, nlcp));
        builder.setDescription(loc.localizeToLocale(this.key("resp.desc.set"), l, channel.getAsMention(), nlcp));
        builder.setColor(Color.GREEN);

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue();

        return CommandResult.ok();
    }

    private CommandResult setDefault(CommandContext context, CommandLine line) {
        return this.set(context, line, JLMessageHelper.getDefaultChannel(context.getServer()));
    }

    private CommandResult list(CommandContext context) {
        Set<String> channels = context.getServerStorage().getObject("mod.jl.welcomeChannels",
            Set.class, String.class);

        String list = channels.stream()
            .map(context.getServer()::getTextChannelById)
            .map(IMentionable::getAsMention)
            .collect(Collectors.joining("\n"));

        if (list.isEmpty()) {
            TextChannel defaultChannel = JLMessageHelper.getDefaultChannel(context.getServer());
            if (defaultChannel != null) {
                list = defaultChannel.getAsMention();
            }
        }

        context.getFeedbackChannel()
            .sendMessage(loc.localizeToLocale(this.key("resp.desc.list"),
                context.getLocale(),
                list, context.getNamedLocalizationContextParams()))
            .queue();

        return CommandResult.ok();
    }

    private TextChannel resolveTextChannel(CommandContext context, CommandLine line) {
        TextChannel channel;
        if (line.hasNext()) {
            String next = line.next();
            List<TextChannel> mentions = context.getMessage().getMentionedChannels();
            if (mentions.isEmpty()) {
                if (next.startsWith(SnowflakeUtils.PREFIX)) {
                    next = SnowflakeUtils.decodeToString(next);
                }

                channel = context.getServer().getTextChannelById(next);

                if (channel == null) {
                    //  Find it by text
                    List<TextChannel> list = context.getServer().getTextChannelsByName(next, true);
                    if (!list.isEmpty()) {
                        channel = list.get(0);
                    }
                }
            } else {
                channel = mentions.get(0);
            }
        } else {
            channel = context.getFeedbackChannel();
        }
        return channel;
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return CommandConstraints.hasAny(
            Permission.MANAGE_SERVER
        );
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected String getResourcePath() {
        return ModModule.BASE_MODULE_PATH + "." + COMMAND_ID;
    }
}
