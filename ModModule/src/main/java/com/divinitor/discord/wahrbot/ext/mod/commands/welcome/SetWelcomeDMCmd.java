package com.divinitor.discord.wahrbot.ext.mod.commands.welcome;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.mod.ModModule;
import com.divinitor.discord.wahrbot.ext.mod.commands.JLMessageHelper;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.awt.*;
import java.util.Locale;
import java.util.Map;

public class SetWelcomeDMCmd extends AbstractKeyedCommand {

    public static final String COMMAND_ID = "welcome.setdm";
    public static final String KEY = ModModule.MODULE_KEY + ".commands." + COMMAND_ID;
    private final Localizer loc;

    @Inject
    public SetWelcomeDMCmd(Localizer loc) {
        this.loc = loc;
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

        Map<String, String> jlConfig = context.getServerStorage().getObject("mod.jl", Map.class);

        String welcomeMessage = line.remainder();
        if ("default".equalsIgnoreCase(welcomeMessage)) {
            jlConfig.put("welcomeDM", "");
            context.getFeedbackChannel().sendMessage(loc.localizeToLocale(this.key("resp.default"), l, nlcp))
                .queue();
        } else if ("none".equalsIgnoreCase(welcomeMessage)) {
            jlConfig.remove("welcomeDM");
            context.getFeedbackChannel().sendMessage(loc.localizeToLocale(this.key("resp.none"), l, nlcp))
                .queue();
        } else {
            jlConfig.put("welcomeDM", welcomeMessage);
            String example = JLMessageHelper.format(welcomeMessage, context.getMember());

            EmbedBuilder builder = new EmbedBuilder();
            builder.setTitle(loc.localizeToLocale(this.key("resp.title"), l, nlcp));
            builder.setDescription(loc.localizeToLocale(this.key("resp.desc"), l, nlcp));
            builder.setColor(Color.GREEN);
            builder.addField(
                loc.localizeToLocale(this.key("resp.example"), l, nlcp),
                loc.localizeToLocale(this.key("resp.example.value"), l, example, nlcp),
                false
            );

            context.getFeedbackChannel().sendMessage(builder.build())
                .queue();
        }

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
