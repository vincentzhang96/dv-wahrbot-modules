package com.divinitor.discord.wahrbot.ext.basics;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.basics.commands.InfoCmd;
import com.google.inject.Inject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This module implements basic commands.
 */
@Getter
public class BasicsModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final DynConfigStore dynConfigStore;
    private final CommandDispatcher dispatcher;

    @Inject
    private InfoCmd infoCmd;

    @Inject
    public BasicsModule(WahrBot bot, DynConfigStore dynConfigStore, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dynConfigStore = dynConfigStore;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.bot.getLocalizer().registerBundle(infoCmd.key(),
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.basics.commands.info",
                this.getClass().getClassLoader()));

        this.dispatcher.getRootRegistry().registerCommand(infoCmd, infoCmd.key());

        //  Subscription methods are automatically registered on the module
    }

    @Override
    public void shutDown() {

        this.dispatcher.getRootRegistry().unregisterCommand(infoCmd.key());

        this.bot.getLocalizer().unregisterBundle(infoCmd.key());

        //  Subscription methods are automatically unregistered on the module
    }
}

