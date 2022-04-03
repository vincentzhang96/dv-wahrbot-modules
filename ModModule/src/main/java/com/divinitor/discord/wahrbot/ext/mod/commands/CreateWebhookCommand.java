package com.divinitor.discord.wahrbot.ext.mod.commands;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.mod.ModModule;
import com.google.common.base.Strings;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Icon;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.requests.restaction.WebhookAction;

import java.awt.*;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

public class CreateWebhookCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "webhook.create";
    public static final String KEY = ModModule.MODULE_KEY + ".commands." + COMMAND_ID;
    private final Localizer loc;

    @Inject
    public CreateWebhookCommand(Localizer loc) {
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

        String name = line.next();

        String avatar = line.remainder();

        TextChannel ch = (TextChannel) context.getInvocationChannel();
        WebhookAction webhook = ch.createWebhook(name);
        if (!Strings.isNullOrEmpty(avatar)) {
            byte[] avatarData = Base64.getDecoder().decode(avatar);
            webhook = webhook.setAvatar(Icon.from(avatarData));
        }

        Webhook result = webhook.complete();

        context.getFeedbackChannel().sendMessage(String.format("New webhook URL: <%s>", result.getUrl()))
                .queue();

        return CommandResult.ok();
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