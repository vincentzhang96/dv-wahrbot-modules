package com.projectduck.discord.wahrbotext.command;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.projectduck.discord.wahrbotext.ProjectDuckModule;

public class BasicDuckDnPostUrlCommand extends BasicMemoryCommand {

    private final String key;
    private final String url;
    private final boolean deleteInvoker;

    public BasicDuckDnPostUrlCommand(String command, String help, String url, boolean deleteInvoker) {
        super(command, help);
        this.key = ProjectDuckModule.MODULE_KEY + ".command." + command;
        this.url = url;
        this.deleteInvoker = deleteInvoker;
    }

    public BasicDuckDnPostUrlCommand(String command, String help, String url) {
        this(command, help, url, false);
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        context.getFeedbackChannel().sendMessage(this.url)
                .queue();
        if (this.deleteInvoker) {
            context.getMessage().delete().queue();
        }

        return CommandResult.ok();
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public CommandConstraint<CommandContext> getOtherConstraints() {
        return (context) -> {
            long id = context.getServer().getIdLong();
            return id == 544827049752264704L || id == 394026352312844298L;
        };
    }
}
