package com.divinitor.discord.wahrbot.ext.mod;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.mod.commands.farewell.SetFarewellChannelCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.farewell.SetFarewellCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.reactrole.ReactRoleAddCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.reactrole.ReactRoleInitCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeChannelCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeCmd;
import com.divinitor.discord.wahrbot.ext.mod.commands.welcome.SetWelcomeDMCmd;
import com.divinitor.discord.wahrbot.ext.mod.listeners.JoinLeaveListener;
import com.divinitor.discord.wahrbot.ext.mod.listeners.ReactionService;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

import static com.divinitor.discord.wahrbot.core.command.CommandConstraints.hasAny;
import static com.divinitor.discord.wahrbot.core.command.CommandConstraints.isOwner;
import static net.dv8tion.jda.core.Permission.*;

public class ModModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String MODULE_KEY = "ext.mod";
    public static final String BASE_MODULE_PATH = "com.divinitor.discord.wahrbot.ext.mod.commands";
    public static final String REACTROLE_KEY = "ext.mod.commands.reactrole";
    public static final String REACTROLE_PATH = "com.divinitor.discord.wahrbot.ext.mod.commands.reactrole";

    private final WahrBot bot;
    private final CommandDispatcher dispatcher;
    private CommandRegistry modRegistry;
    private CommandRegistry reactRoleRegistry;

    @Inject
    private JoinLeaveListener joinLeaveListener;

    @Inject
    private ReactionService reactionService;

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
    private ReactRoleInitCmd reactRoleInitCmd;
    @Inject
    private ReactRoleAddCmd reactRoleAddCmd;

    @Inject
    public ModModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.registerBundle(MODULE_KEY, BASE_MODULE_PATH + ".mod");

        Localizer loc = this.bot.getLocalizer();
        this.modRegistry = this.dispatcher.getRootRegistry().makeRegistries(MODULE_KEY);
        this.modRegistry.setUserPermissionConstraints(
            isOwner()
                .or(hasAny(ADMINISTRATOR, MESSAGE_MANAGE, MANAGE_SERVER, MANAGE_PERMISSIONS, MANAGE_ROLES))
        );

        this.setWelcomeDMCmd.register(this.modRegistry, loc);
        this.setWelcomeCmd.register(this.modRegistry, loc);
        this.setWelcomeChannelCmd.register(this.modRegistry, loc);

        this.setFarewellCmd.register(this.modRegistry, loc);
        this.setFarewellChannelCmd.register(this.modRegistry, loc);

        this.registerBundle(REACTROLE_KEY, REACTROLE_PATH + ".reactrole");

        this.reactRoleRegistry = this.modRegistry.makeRegistries(REACTROLE_KEY);

        this.reactRoleAddCmd.register(this.reactRoleRegistry, loc);
        this.reactRoleInitCmd.register(this.reactRoleRegistry, loc);

        this.bot.getEventBus().register(this.joinLeaveListener);
        this.bot.getEventBus().register(this.reactionService);

        this.reactionService.start();
        this.reactRoleInitCmd.setListener(this.reactionService);
        this.reactRoleAddCmd.setListener(this.reactionService);
    }

    private void registerBundle(String key) {
        this.registerBundle(key, key);
    }

    private void registerBundle(String key, String bundleLocation) {
        this.bot.getLocalizer().registerBundle(key,
            new ResourceBundleBundle(bundleLocation,
                this.getClass().getClassLoader()));
    }

    @Override
    public void shutDown() {
        Localizer loc = this.bot.getLocalizer();
        this.reactionService.stop();
        this.bot.getEventBus().unregister(this.reactionService);
        this.bot.getEventBus().unregister(this.joinLeaveListener);

        this.setWelcomeCmd.unregister(this.modRegistry, loc);
        this.setWelcomeDMCmd.unregister(this.modRegistry, loc);
        this.setWelcomeChannelCmd.unregister(this.modRegistry, loc);

        this.setFarewellCmd.unregister(this.modRegistry, loc);
        this.setFarewellChannelCmd.unregister(this.modRegistry, loc);

        this.reactRoleAddCmd.unregister(this.reactRoleRegistry, loc);
        this.reactRoleInitCmd.unregister(this.reactRoleRegistry, loc);

        loc.unregisterBundle(MODULE_KEY);
        loc.unregisterBundle(REACTROLE_KEY);
    }
}
