package com.divinitor.discord.wahrbot.ext.mod.commands.farewell;

import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.mod.commands.JLMessageHelper;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;

public class SetFarewellCmd implements Command {

    public static final String KEY = "com.divinitor.discord.wahrbot.ext.mod.commands.farewell.set";
    private final Localizer loc;

    @Inject
    public SetFarewellCmd(Localizer loc) {
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

        String farewellMessage = line.remainder();
        if ("default".equalsIgnoreCase(farewellMessage)) {
            jlConfig.put("farewellMsg", "");
            context.getFeedbackChannel().sendMessage(loc.localizeToLocale(this.key("resp.default"), l, nlcp))
                .queue();
        } else if ("none".equalsIgnoreCase(farewellMessage)) {
            jlConfig.remove("farewellMsg");
            context.getFeedbackChannel().sendMessage(loc.localizeToLocale(this.key("resp.none"), l, nlcp))
                .queue();
        } else {
            jlConfig.put("farewellMsg", farewellMessage);
            String example = JLMessageHelper.format(farewellMessage, context.getMember());

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

    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }
}
