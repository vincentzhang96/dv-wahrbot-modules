package com.divinitor.discord.wahrbot.ext.lol.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.lol.LoLService;
import com.google.inject.Inject;

import java.util.StringJoiner;

public class RunesCmd implements Command {

    public static final String KEY = "ext.lol.commands.runes";
    private final Localizer loc;
    private LoLService service;

    @Inject
    public RunesCmd(Localizer loc) {
        this.loc = loc;
    }

    @Override
    public CommandResult invoke(CommandContext context) {


        return CommandResult.ok();
    }

    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }

    public void setService(LoLService service) {
        this.service = service;
    }
}
