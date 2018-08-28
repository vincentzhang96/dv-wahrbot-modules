package com.divinitor.discord.wahrbot.ext.fun.command;

import com.divinitor.discord.wahrbot.core.command.AbstractKeyedCommand;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.ext.fun.FunModule;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.Locale;
import java.util.Map;

public class StabCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "labpoints";
    public static final String KEY = FunModule.MODULE_KEY + ".commands." + COMMAND_ID;

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected String getResourcePath() {
        return FunModule.BASE_MODULE_PATH + "." + COMMAND_ID;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        CommandLine line = context.getCommandLine();

        User invoker = context.getInvoker();




        return null;
    }

    private void stab(CommandContext context, Member source, Member target) {
        String message = "";
        if (source.getUser().getIdLong() == target.getUser().getIdLong()) {
            // Self stab

        } else {



        }
    }

    private String getAlternateStab() {
        return "";
    }
}
