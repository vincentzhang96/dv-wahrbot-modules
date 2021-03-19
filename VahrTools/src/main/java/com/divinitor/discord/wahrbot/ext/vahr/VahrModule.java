package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.vahr.commands.duck.*;
import com.google.common.collect.Lists;
import com.google.common.eventbus.AsyncEventBus;
import com.google.inject.Inject;
import com.google.inject.Injector;
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
    private KupoFeatures kupoFeatures;
    private DuckDNDiscordFeatures duckDNDiscordFeatures;

    private List<BasicMemoryCommand> duckCommands = Lists.newArrayList(
            new BasicDuckDnPostUrlCommand(
                    "council",
                    "The council will decide your fate",
                    "https://static.divinitor.com/site/common/memes/the_council.jpg"
            ),
            new BasicDuckDnPostUrlCommand(
                    "idiots",
                    "Do you choose to be idiots?",
                    "https://static.divinitor.com/site/common/memes/idiots.png"
            ),
            new BasicDuckDnPostUrlCommand(
                    "worthless",
                    "Then your argument is worthless",
                    "https://static.divinitor.com/site/common/memes/worthless.png"
            ),
            new BasicDuckDnPostUrlCommand(
                    "support",
                    "Introducing basic minimums",
                    "https://static.divinitor.com/site/common/memes/support.png"
            ),
            new BasicDuckDnPostUrlCommand(
                    "certified",
                    "Certified retard",
                    "https://static.divinitor.com/site/common/memes/certified.png"
            ),
            new BasicDuckDnPostUrlCommand(
                    "guide",
                    "If you missed the guide the first 20 times",
                    "https://news.fatduckdn.com/guide/"
            ),
            new DuckTestMakeResourceCommand(),
            new DuckTestRestartCommand(),
            new DuckTestResyncCommand()
    );

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
        this.kupoFeatures = injector.getInstance(KupoFeatures.class);
        this.duckDNDiscordFeatures = injector.getInstance(DuckDNDiscordFeatures.class);

        AsyncEventBus eventBus = this.bot.getEventBus();
        eventBus.register(this.commonFeatures);
        eventBus.register(this.divinitorDiscordFeatures);
        eventBus.register(this.dnnacdDiscordFeatures);
        eventBus.register(this.kupoFeatures);
        eventBus.register(this.duckDNDiscordFeatures);

        CommandRegistry registry = this.dispatcher.getRootRegistry();
        Localizer loc = bot.getLocalizer();
        for (BasicMemoryCommand duckCommand : this.duckCommands) {
            duckCommand.register(registry, loc);
        }
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

        if (this.kupoFeatures != null) {
            eventBus.unregister(this.kupoFeatures);
        }

        if (this.duckDNDiscordFeatures != null) {
            eventBus.unregister(this.duckDNDiscordFeatures);
        }

        Localizer loc = bot.getLocalizer();
        CommandRegistry registry = this.dispatcher.getRootRegistry();
        for (BasicMemoryCommand duckCommand : this.duckCommands) {
            duckCommand.unregister(registry, loc);
        }
    }
}
