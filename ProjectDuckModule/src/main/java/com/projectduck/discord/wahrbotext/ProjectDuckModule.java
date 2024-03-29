package com.projectduck.discord.wahrbotext;

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
import com.projectduck.discord.wahrbotext.command.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;

public class ProjectDuckModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;
    private DuckDNDiscordFeatures duckDNDiscordFeatures;
    private Interactions interactions;

    public static final String MODULE_KEY = "ext.projectduck";
    public static final String BASE_MODULE_PATH = "com.projectduck.discord.wahrbotext.command";

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
            new BasicDuckDnPostUrlCommand(
                    "ti",
                    "If you're having technical issues",
                    "https://news.fatduckdn.com/common-technical-issues/",
                    true
            ),
            new BasicDuckDnPostUrlCommand(
                    "faq",
                    "Frequently asked questions",
                    "https://news.fatduckdn.com/frequently-asked-questions",
                    true
            ),
            new BasicDuckDnPostUrlCommand(
                    "vandar",
                    "Vandar?",
                    "https://news.fatduckdn.com/frequently-asked-questions/#do-you-have-vandar",
                    true
            ),
            new BasicDuckDnPostUrlCommand(
                    "sign",
                    "Don't make me tap the sign",
                    "https://cdn.discordapp.com/attachments/546453205924577281/1057454043267604521/image.png",
                    true
            ),
            new BasicDuckDnPostUrlCommand(
                    "retard",
                    "Retard",
                    "https://cdn.discordapp.com/attachments/287121976500158465/1108161892242960504/image.png",
                    true
            ),
            new DuckTestMakeResourceCommand(),
            new DuckTestRestartCommand(),
            new DuckTestResyncCommand(),
            new DuckRemindUnverifiedCommand()
    );

    @Inject
    public ProjectDuckModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        Injector injector = context.getInjector();

        this.duckDNDiscordFeatures = injector.getInstance(DuckDNDiscordFeatures.class);
        this.interactions = injector.getInstance(Interactions.class);

        AsyncEventBus eventBus = this.bot.getEventBus();
        eventBus.register(this.duckDNDiscordFeatures);
        eventBus.register(this.interactions);

        this.interactions.setUp(this.duckDNDiscordFeatures);

        Unirest.setTimeouts(10000, 240000);

        CommandRegistry registry = this.dispatcher.getRootRegistry();
        Localizer loc = bot.getLocalizer();
        for (BasicMemoryCommand duckCommand : this.duckCommands) {
            duckCommand.register(registry, loc);
        }

//        this.apiServer.start();
    }

    @Override
    public void shutDown() {
        AsyncEventBus eventBus = this.bot.getEventBus();
        if (this.duckDNDiscordFeatures != null) {
            eventBus.unregister(this.duckDNDiscordFeatures);
        }

        if (this.interactions != null) {
            eventBus.unregister(this.interactions);
        }

        Localizer loc = bot.getLocalizer();
        CommandRegistry registry = this.dispatcher.getRootRegistry();
        for (BasicMemoryCommand duckCommand : this.duckCommands) {
            duckCommand.unregister(registry, loc);
        }
    }
}
