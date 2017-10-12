package com.divinitor.discord.wahrbot.ext.core;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.config.dyn.LongDynConfigHandle;
import com.divinitor.discord.wahrbot.core.module.ModuleLoadException;
import com.github.zafarkhaja.semver.Version;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.GenericMessageEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.UUID;

public class CommandHandler {

    public static final String MANAGEMENT_KEY = "com.divinitor.discord.wahrbot.ext.core.management.id";
    public static final String MANAGEMENT_PREFIX = "com.divinitor.discord.wahrbot.ext.core.management.prefix";
    private final CoreModule core;
    private final LongDynConfigHandle managementAccountId;
    private final DynConfigHandle managementPrefix;

    public CommandHandler(CoreModule core) {
        this.core = core;
        DynConfigStore configStore = this.core.getDynConfigStore();
        this.managementAccountId = configStore.getLongHandle(MANAGEMENT_KEY);
        this.managementPrefix = configStore.getStringHandle(MANAGEMENT_PREFIX);
    }

    public void onPrivateMessage(PrivateMessageReceivedEvent event) {
        WahrBot bot = this.core.getBot();
        //  Instead of using the command bus for these commands, we listen for these raw as to avoid misbehaving modules
        //  so that we can always perform core management tasks

        String content = event.getMessage().getContent();
        String prefix = managementPrefix.get();
        if (!content.startsWith(prefix)) {
            return;
        }

        //  Only the management account can perform these actions
        User author = event.getAuthor();
        if (author == null) {
            return;
        }

        if (author.getIdLong() != managementAccountId.getLong()) {
            CoreModule.LOGGER.warn("User " + author.toString() + " does not have valid credentials for managing " +
                "the bot; access denied");
            return;
        }

        //  Remove prefix
        content = content.substring(prefix.length());

        //  Process command
        String[] split = content.split(" ", 2);
        String cmd = split[0].toLowerCase();
        String args = split.length == 2 ? split[1] : "";


        try {
            switch (cmd) {
                case "help": {
                    event.getMessage().getChannel().sendMessage("Available commands: " +
                        "help, shutdown, restart,\n" +
                        "load <moduleid> [version],\n" +
                        "unload <moduleid>,\n" +
                        "reload <moduleid> [version]")
                        .queue();
                    break;
                }
                case "shutdown": {
                    bot.shutdown();
                    break;
                }
                case "restart": {
                    bot.restart();
                    break;
                }
                case "load": {
                    this.loadModule(args, event);
                    break;
                }
                case "unload": {
                    this.unloadModule(args, event);
                    break;
                }
                case "reload": {
                    this.reloadModule(args, event);
                    break;
                }
            }
        } catch (Exception e) {
            CoreModule.LOGGER.warn("Unable to execute command \"{}\"", content, e);
            event.getMessage().getChannel().sendMessage("Command execution error: " + e.toString())
                .queue();
        }
    }

    private void loadModule(String args, GenericMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (args.isEmpty()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
            return;
        }

        String[] split = args.split(" ", 2);
        String id = split[0].toLowerCase();
        Version version = split.length == 2 ? Version.valueOf(split[1]) : null;
        try {
            this.core.getBot().getModuleManager().loadModule(id, version);
            ch.sendMessage(String.format("Module `%s` loaded", id))
                .queue();

            this.core.getBot().getModuleManager().saveCurrentModuleList();
        } catch (IllegalStateException ise) {
            ch.sendMessage("Module is already loaded")
                .queue();
        } catch (ModuleLoadException mle) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to load module: " + mle.toString() + " <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Module {}:{} load failed <{}>", id, version, uuid, mle);
        } catch (IOException e) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to save module list, changes might not persist <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Failed to save module list <{}>", uuid, e);
        }
    }

    private void unloadModule(String args, GenericMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (args.isEmpty()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
            return;
        }

        String id = args;

        if (id.equals("core")) {
            ch.sendMessage("Cannot unload core module!")
                .queue();
            return;
        }

        try {
            this.core.getBot().getModuleManager().unloadModule(id);
            ch.sendMessage(String.format("Module `%s` unloaded", id))
                .queue();
            this.core.getBot().getModuleManager().saveCurrentModuleList();
        } catch (NoSuchElementException nsee) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to unload module: " + nsee.toString() + " <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Module {} unload failed <{}>", id, uuid, nsee);
        } catch (IOException e) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to save module list, changes might not persist <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Failed to save module list <{}>", uuid, e);
        }
    }

    private void reloadModule(String args, GenericMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (args.isEmpty()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
        }

        String[] split = args.split(" ", 2);
        String id = split[0].toLowerCase();
        Version version = split.length == 2 ? Version.valueOf(split[1]) : null;
        try {
            this.core.getBot().getModuleManager().reloadModule(id, version);
            ch.sendMessage(String.format("Module `%s` reloaded", id))
                .queue();
            this.core.getBot().getModuleManager().saveCurrentModuleList();
        } catch (ModuleLoadException mle) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to reload module: " + mle.toString() + " <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Module {}:{} reload failed <{}>", id, version, uuid, mle);
        } catch (IOException e) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to save module list, changes might not persist <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Failed to save module list <{}>", uuid, e);
        }
    }
}
