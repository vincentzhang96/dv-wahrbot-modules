package com.divinitor.discord.wahrbot.ext.core;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import lombok.Getter;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * This module implements core functionality of the bot, namely management commands.
 */
@Getter
public class CoreModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final DynConfigStore dynConfigStore;

    private final CommandHandler handler;

    @Inject
    public CoreModule(WahrBot bot, DynConfigStore dynConfigStore) {
        this.bot = bot;
        this.dynConfigStore = dynConfigStore;
        this.handler = new CommandHandler(this);
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        //  Subscription methods are automatically registered on the module
    }

    @Override
    public void shutDown() {
        //  Subscription methods are automatically unregistered on the module
    }

    @Subscribe
    public void onPrivateMessage(PrivateMessageReceivedEvent event) {
        this.handler.onPrivateMessage(event);
    }
}
