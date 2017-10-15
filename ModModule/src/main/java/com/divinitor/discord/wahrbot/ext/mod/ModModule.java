package com.divinitor.discord.wahrbot.ext.mod;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class ModModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final DynConfigStore dynConfigStore;
    private final CommandDispatcher dispatcher;
    private CommandRegistry modRegistry;


    @Inject
    public ModModule(WahrBot bot, DynConfigStore dynConfigStore, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dynConfigStore = dynConfigStore;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.modRegistry = this.dispatcher.getRootRegistry()
            .makeRegistries("com.divinitor.discord.wahrbot.ext.mod.commands.mod");


    }

    @Override
    public void shutDown() {

    }
}
