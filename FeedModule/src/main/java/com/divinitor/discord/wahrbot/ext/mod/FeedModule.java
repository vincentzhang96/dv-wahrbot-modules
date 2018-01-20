package com.divinitor.discord.wahrbot.ext.mod;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.mod.event.FeedServiceLoadEvent;
import com.divinitor.discord.wahrbot.ext.mod.event.FeedServiceUnloadEvent;
import com.divinitor.discord.wahrbot.ext.mod.service.impl.FeedServiceImpl;
import com.google.inject.Inject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Getter
public class FeedModule implements Module {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WahrBot bot;
    private final CommandDispatcher dispatcher;
    private FeedServiceImpl feedService;

    @Inject
    public FeedModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.feedService = context.getInjector().getInstance(FeedServiceImpl.class);


        if (!context.bulkLoad()) {
            this.emitLoad();
        }
    }

    @Override
    public void postBatchInit() throws Exception {
        this.emitLoad();
    }

    @Override
    public void shutDown() {
        this.emitShutdown();
    }

    private void emitLoad() {
        this.bot.getEventBus().post(new FeedServiceLoadEvent(this));
    }

    private void emitShutdown() {
        this.bot.getEventBus().post(new FeedServiceUnloadEvent());
    }
}
