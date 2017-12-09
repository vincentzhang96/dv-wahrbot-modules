package com.divinitor.discord.wahrbot.ext.mod;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.mod.commands.farewell.SetFarewellChannelCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.farewell.SetFarewellCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeChannelCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeDMCmd;
import com.divinitor.discord.wahrbot.ext.mod.listeners.JoinLeaveListener;
import com.divinitor.discord.wahrbot.ext.mod.listeners.ReactionListener;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static com.divinitor.discord.wahrbot.core.command.CommandConstraints.hasAny;
import static com.divinitor.discord.wahrbot.core.command.CommandConstraints.isOwner;
import static net.dv8tion.jda.core.Permission.*;

public class ModModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String CORE_REF = "com.divinitor.discord.wahrbot.ext.mod.commands.core";
    public static final String REACTROLE_REF = "com.divinitor.discord.wahrbot.ext.mod.commands.reactrole.core";
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;
    private CommandRegistry modRegistry;
    private CommandRegistry reactRoleRegistry;

    @Inject
    private JoinLeaveListener joinLeaveListener;

    @Inject
    private ReactionListener reactionListener;

    @Inject
    private SetWelcomeCmd setWelcomeCmd;
    @Inject
    private SetWelcomeDMCmd setWelcomeDMCmd;
    @Inject
    private SetWelcomeChannelCmd setWelcomeChannelCmd;
    @Inject
    private SetFarewellCmd setFarewellCmd;
    @Inject
    private SetFarewellChannelCmd setFarewellChannelCmd;

    @Inject
    public ModModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.bot.getLocalizer().registerBundle(CORE_REF,
            new ResourceBundleBundle(CORE_REF,
                this.getClass().getClassLoader()));

        this.registerBundle(this.setWelcomeCmd.key());
        this.registerBundle(this.setWelcomeDMCmd.key());
        this.registerBundle(this.setWelcomeChannelCmd.key());
        this.registerBundle(this.setFarewellCmd.key());
        this.registerBundle(this.setFarewellChannelCmd.key());

        this.modRegistry = this.dispatcher.getRootRegistry()
            .makeRegistries("com.divinitor.discord.wahrbot.ext.mod.commands.mod");

        this.modRegistry.setUserPermissionConstraints(
            isOwner()
                .or(hasAny(ADMINISTRATOR, MESSAGE_MANAGE))
        );

        this.modRegistry.setBotPermissionConstraints(
            hasAny(
                ADMINISTRATOR,
                MESSAGE_MANAGE,
                KICK_MEMBERS,
                BAN_MEMBERS,
                MANAGE_CHANNEL,
                MANAGE_SERVER,
                NICKNAME_CHANGE,
                NICKNAME_MANAGE,
                MANAGE_ROLES,
                MANAGE_PERMISSIONS)
        );

        this.modRegistry.registerCommand(this.setWelcomeCmd, this.setWelcomeCmd.key());
        this.modRegistry.registerCommand(this.setWelcomeDMCmd, this.setWelcomeDMCmd.key());
        this.modRegistry.registerCommand(this.setWelcomeChannelCmd, this.setWelcomeChannelCmd.key());
        this.modRegistry.registerCommand(this.setFarewellCmd, this.setFarewellCmd.key());
        this.modRegistry.registerCommand(this.setFarewellChannelCmd, this.setFarewellChannelCmd.key());


        this.bot.getLocalizer().registerBundle(REACTROLE_REF,
            new ResourceBundleBundle(REACTROLE_REF,
                this.getClass().getClassLoader()));

        reactRoleRegistry = this.modRegistry.makeRegistries(
            "com.divinitor.discord.wahrbot.ext.mod.commands.mod.reactrole");


        this.bot.getEventBus().register(this.joinLeaveListener);
        this.bot.getEventBus().register(this.reactionListener);
        this.reactionListener.start();
    }

    private void registerBundle(String key) {
        this.bot.getLocalizer().registerBundle(key,
            new ResourceBundleBundle(key,
                this.getClass().getClassLoader()));
    }

    @Override
    public void shutDown() {
        this.reactionListener.stop();
        this.bot.getEventBus().unregister(this.reactionListener);
        this.bot.getEventBus().unregister(this.joinLeaveListener);

        this.modRegistry.unregisterCommand(this.setWelcomeCmd.key());
        this.modRegistry.unregisterCommand(this.setWelcomeDMCmd.key());
        this.modRegistry.unregisterCommand(this.setWelcomeChannelCmd.key());
        this.modRegistry.unregisterCommand(this.setFarewellCmd.key());
        this.modRegistry.unregisterCommand(this.setFarewellChannelCmd.key());

        this.bot.getLocalizer().unregisterBundle(this.setWelcomeCmd.key());
        this.bot.getLocalizer().unregisterBundle(this.setWelcomeDMCmd.key());
        this.bot.getLocalizer().unregisterBundle(this.setWelcomeChannelCmd.key());
        this.bot.getLocalizer().unregisterBundle(this.setFarewellCmd.key());
        this.bot.getLocalizer().unregisterBundle(this.setFarewellChannelCmd.key());

        this.bot.getLocalizer().unregisterBundle(CORE_REF);
        this.bot.getLocalizer().unregisterBundle(REACTROLE_REF);
    }
}
