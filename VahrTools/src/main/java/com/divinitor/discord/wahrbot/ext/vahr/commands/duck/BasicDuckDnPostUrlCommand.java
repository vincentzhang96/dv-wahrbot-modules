package com.divinitor.discord.wahrbot.ext.vahr.commands.duck;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.ext.vahr.VahrModule;

public class BasicDuckDnPostUrlCommand extends BasicMemoryCommand {

    private final String key;
    private final String url;

    public BasicDuckDnPostUrlCommand(String command, String help, String url) {
        super(command, help);
        this.key = VahrModule.MODULE_KEY + ".commands." + command;
        this.url = url;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        context.getFeedbackChannel().sendMessage(this.url)
                .queue();
        return CommandResult.ok();
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public CommandConstraint<CommandContext> getOtherConstraints() {
        return (context) -> context.getServer().getIdLong() == 544827049752264704L;
    }
}
