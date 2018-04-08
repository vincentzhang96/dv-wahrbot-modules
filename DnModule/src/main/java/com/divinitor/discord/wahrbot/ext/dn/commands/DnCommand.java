package com.divinitor.discord.wahrbot.ext.dn.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;

public interface DnCommand extends Command {

    String key(String... args);

    void register(Localizer localizer, CommandRegistry commandRegistry);

    void unregister(Localizer localizer, CommandRegistry commandRegistry);
}
