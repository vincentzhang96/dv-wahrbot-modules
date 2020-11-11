package com.divinitor.discord.wahrbot.ext.core;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.module.ModuleInformation;
import com.divinitor.discord.wahrbot.core.module.ModuleLoadException;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.core.util.gson.StandardGson;
import com.github.zafarkhaja.semver.Version;
import com.mashape.unirest.http.Unirest;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.GenericPrivateMessageEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class CommandHandler {

    public static final String MANAGEMENT_KEY = "ext.core.management.ids";
    public static final String MANAGEMENT_PREFIX = "ext.core.management.prefix";
    private final CoreModule core;
    private final DynConfigHandle managementAccountIds;
    private final DynConfigHandle managementPrefix;

    public CommandHandler(CoreModule core) {
        this.core = core;
        DynConfigStore configStore = this.core.getDynConfigStore();
        this.managementAccountIds = configStore.getStringHandle(MANAGEMENT_KEY);
        this.managementPrefix = configStore.getStringHandle(MANAGEMENT_PREFIX);
    }

    public void onPrivateMessage(PrivateMessageReceivedEvent event) {
        WahrBot bot = this.core.getBot();
        //  Instead of using the command bus for these commands, we listen for these raw as to avoid misbehaving modules
        //  so that we can always perform core management tasks

        String content = event.getMessage().getContentDisplay();

        CommandLine line = new CommandLine(content);

        //  Remove prefix
        String prefix = this.managementPrefix.get();
        if (!line.hasPrefixAndTake(prefix)) {
            return;
        }

        //  Only the management account can perform these actions
        User author = event.getAuthor();
        if (author == null) {
            return;
        }

        if (!this.checkId(author.getId())) {
            CoreModule.LOGGER.warn("User " + author.toString() + " does not have valid credentials for managing " +
                "the bot; access denied");
            return;
        }

        //  Process command
        String cmd = line.next();

        try {
            switch (cmd) {
                case "help": {
                    event.getMessage().getChannel().sendMessage("Available commands: " +
                        "help, shutdown, restart,\n" +
                        "load <moduleid> [version],\n" +
                        "add [url]|<attachment>,\n" +
                        "unload <moduleid>,\n" +
                        "reload <moduleid> [version]\n" +
                        "admin <userid>\n")
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
                    this.loadModule(line, event);
                    break;
                }
                case "add": {
                    this.addModule(line, event);
                    break;
                }
                case "unload": {
                    this.unloadModule(line, event);
                    break;
                }
                case "reload": {
                    this.reloadModule(line, event);
                    break;
                }
                case "sudo": {
                    this.setSuperAdmin(line, event);
                    break;
                }
            }
        } catch (Exception e) {
            CoreModule.LOGGER.warn("Unable to execute command \"{}\"", content, e);
            event.getMessage().getChannel().sendMessage("Command execution error: " + e.toString())
                .queue();
        }
    }

    private boolean checkId(String userId) {
        String vals = this.managementAccountIds.get();
        String[] v = vals.split(",");
        for (String s : v) {
            if (s.equalsIgnoreCase(userId)) {
                return true;
            }
        }

        return false;
    }

    private void setSuperAdmin(CommandLine line, PrivateMessageReceivedEvent event) {
        MessageChannel ch = event.getChannel();
        if (!line.hasNext()) {
            ch.sendMessage("Missing user ID")
                .queue();
            return;
        }

        String id = line.next();
        if (id.startsWith(SnowflakeUtils.PREFIX)) {
            id = SnowflakeUtils.decodeToString(id);
        }

        User user = event.getJDA().getUserById(id);
        if (user == null) {
            ch.sendMessage("Cannot find user with that ID")
                .queue();
            return;
        }

        UserStore userStore = this.core.getBot().getUserStorage().forUser(user);
        boolean sudo = userStore.getBoolean("sudo");
        userStore.put("sudo", !sudo);

        if (sudo) {
            ch.sendMessage(String.format("User %s#%s (%s) is no longer a super admin",
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user.getIdLong())))
                .queue();
        } else {
            ch.sendMessage(String.format("User %s#%s (%s) is now a super admin",
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user.getIdLong())))
                .queue();
        }
    }

    private void addModule(CommandLine line, PrivateMessageReceivedEvent event) {
        MessageChannel ch = event.getChannel();
        List<Message.Attachment> attachments = event.getMessage().getAttachments();
        String urlStr;
        if (!attachments.isEmpty()) {
            Message.Attachment first = attachments.get(0);
            urlStr = first.getUrl();
        } else {
            //  Look for a URL
            if (!line.hasNext()) {
                urlStr = line.remainder();
            } else {
                ch.sendMessage("Missing module URL or attachment")
                    .queue();
                return;
            }
        }
        try {
            Path temp = Files.createTempFile("wahrbot", ".jar");
            InputStream body = Unirest.get(urlStr)
                .asBinary()
                .getBody();
            byte[] buffer = new byte[8192];
            int read = 0;
            try (OutputStream os = Files.newOutputStream(temp)) {
                while ((read = body.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }

            //  Inspect!
            ModuleInformation info;
            Path modDir;
            Path outfile;
            try (JarFile jarFile = new JarFile(temp.toFile())) {
                ZipEntry entry = jarFile.getEntry("moduleinfo.json");

                InputStream inputStream = jarFile.getInputStream(entry);
                info = StandardGson.instance().fromJson(
                    new InputStreamReader(inputStream),
                    ModuleInformation.class);
            } catch (IOException e) {
                throw new ModuleLoadException("Failed to read module from " + urlStr, e);
            }

            modDir = this.core.getBot().getBotDir().resolve("module").resolve(info.getId());
            Files.createDirectories(modDir);
            outfile = modDir.resolve(String.format("%s-%s.jar", info.getId(), info.getVersion()));
            Files.copy(temp, outfile, StandardCopyOption.REPLACE_EXISTING);

            if (this.core.getBot().getModuleManager().getLoadedModules().containsKey(info.getId())) {
                this.reloadModuleImpl(ch, info.getId(), info.getVersion());
            } else {
                this.loadModuleImpl(ch, info.getId(), info.getVersion());
            }

        } catch (Exception e) {
            UUID uuid = UUID.randomUUID();
            ch.sendMessage("Failed to load module: " + e.toString() + " <" + uuid.toString() + ">")
                .queue();
            CoreModule.LOGGER.warn("Module {} load failed <{}>", urlStr, uuid, e);
        }
    }

    private void loadModule(CommandLine line, GenericPrivateMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (!line.hasNext()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
            return;
        }

        String id = line.next();
        Version version = line.hasNext() ? Version.valueOf(line.next()) : null;
        this.loadModuleImpl(ch, id, version);
    }

    private void loadModuleImpl(MessageChannel ch, String id, Version version) {
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

    private void unloadModule(CommandLine line, GenericPrivateMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (!line.hasNext()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
            return;
        }

        String id = line.remainder();

        if (id.equals("core")) {
            ch.sendMessage("Cannot unload core module!")
                .queue();
            return;
        }

        this.unloadModuleImpl(ch, id);
    }

    private void unloadModuleImpl(MessageChannel ch, String id) {
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

    private void reloadModule(CommandLine line, GenericPrivateMessageEvent event) {
        MessageChannel ch = event.getChannel();
        if (!line.hasNext()) {
            ch.sendMessage("Missing module ID and/or version")
                .queue();
        }

        String id = line.next();
        Version version = line.hasNext() ? Version.valueOf(line.next()) : null;
        this.reloadModuleImpl(ch, id, version);
    }

    private void reloadModuleImpl(MessageChannel ch, String id, Version version) {
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
