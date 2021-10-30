package com.projectduck.discord.wahrbotext.command;


import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerBundle;

import java.util.Locale;

/**
 * An abstract Command implementation that takes care of basics such as registration and resource registration, using
 * in memory resources
 */
public abstract class BasicMemoryCommand implements Command {

    private final String command;
    private final String help;
    private final String helpDesc;
    private final String syntax;
    private final String syntaxParams;

    public BasicMemoryCommand(String command, String help, String helpDesc, String syntax, String syntaxParams) {
        this.command = command;
        this.help = help;
        this.helpDesc = helpDesc;
        this.syntax = syntax;
        this.syntaxParams = syntaxParams;
    }

    public BasicMemoryCommand(String command, String help, String helpDesc) {
        this(command, help, helpDesc, "", "");
    }

    public BasicMemoryCommand(String command, String help) {
        this(command, help, help, "", "");
    }

    /**
     * Get the unique key that identifies this command
     * @return The unique key for this command
     */
    public abstract String getKey();

    /**
     * Register this command to the given registry and localizer
     * @param commandRegistry The registry to add this command to
     * @param localizer The localizer to add this command to
     */
    public void register(CommandRegistry commandRegistry, Localizer localizer) {
        localizer.registerBundle(this.getKey(), new LocalizerBundle() {
            @Override
            public String get(String key, Locale locale) {
                String baseKey = BasicMemoryCommand.this.getKey();
                if (!key.startsWith(baseKey)) {
                    return null;
                }

                String stripped = key.substring(baseKey.length());
                switch (stripped) {
                    case "":
                        return BasicMemoryCommand.this.command;
                    case ".help":
                        return BasicMemoryCommand.this.help;
                    case ".help.desc":
                        return BasicMemoryCommand.this.helpDesc;
                    case ".help.syntax.list":
                        return BasicMemoryCommand.this.syntax;
                    case ".help.syntax.params":
                        return BasicMemoryCommand.this.syntaxParams;
                    default:
                        return null;
                }
            }

            @Override
            public boolean contains(String key, Locale locale) {
                String baseKey = BasicMemoryCommand.this.getKey();
                if (!key.startsWith(baseKey)) {
                    return false;
                }
                String stripped = key.substring(baseKey.length());
                switch (stripped) {
                    case "":
                    case ".help":
                    case ".help.desc":
                    case ".help.syntax.list":
                    case ".help.syntax.params":
                        return true;
                    default:
                        return false;
                }
            }
        });
        commandRegistry.registerCommand(this, this.getKey());
    }

    /**
     * Unregister this command from the given registry and localizer
     * @param commandRegistry The command to remove this command to
     * @param localizer The localizer to unregister from
     */
    public void unregister(CommandRegistry commandRegistry, Localizer localizer) {
        localizer.unregisterBundle(this.getKey());
        commandRegistry.unregisterCommand(this.getKey());
    }
}
