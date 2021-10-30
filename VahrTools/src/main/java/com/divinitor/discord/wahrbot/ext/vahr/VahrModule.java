package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class VahrModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;

    private CommonFeatures commonFeatures;
    private DivinitorDiscordFeatures divinitorDiscordFeatures;
    private DNNACDDiscordFeatures dnnacdDiscordFeatures;

    public static final String MODULE_KEY = "ext.vahr";
    public static final String BASE_MODULE_PATH = "com.divinitor.discord.wahrbot.ext.vahr.commands";

    @Inject
    public VahrModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        Injector injector = context.getInjector();
        this.commonFeatures = injector.getInstance(CommonFeatures.class);
        this.divinitorDiscordFeatures = injector.getInstance(DivinitorDiscordFeatures.class);
        this.dnnacdDiscordFeatures = injector.getInstance(DNNACDDiscordFeatures.class);

        AsyncEventBus eventBus = this.bot.getEventBus();
        eventBus.register(this.commonFeatures);
        eventBus.register(this.divinitorDiscordFeatures);
        eventBus.register(this.dnnacdDiscordFeatures);

        Unirest.setTimeouts(10000, 240000);
    }

    @Override
    public void shutDown() {
        AsyncEventBus eventBus = this.bot.getEventBus();
        if (this.commonFeatures != null) {
            eventBus.unregister(this.commonFeatures);
        }

        if (this.divinitorDiscordFeatures != null) {
            eventBus.unregister(this.divinitorDiscordFeatures);
        }

        if (this.dnnacdDiscordFeatures != null) {
            eventBus.unregister(this.dnnacdDiscordFeatures);
        }
    }
}
