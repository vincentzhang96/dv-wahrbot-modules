package com.divinitor.discord.wahrbot.ext.dn;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.dn.commands.EffectiveHPCommand;
import com.divinitor.discord.wahrbot.ext.dn.commands.StatCommand;
import com.divinitor.discord.wahrbot.ext.dn.services.DnStatService;
import com.divinitor.discord.wahrbot.ext.dn.util.QueueExceptionHandler;
import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Getter
public class DnModule implements Module {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String MODULE_KEY = "ext.dn";
    public static final String BASE_MODULE_PATH = "com.divinitor.discord.wahrbot.ext.dn.commands";
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;
    private CommandRegistry registry;

    private DnStatService statService;
    private StatCommand fdLinCommand;
    private StatCommand critCommand;
    private StatCommand critdmgCommand;
    private StatCommand defenseCommand;
    private EffectiveHPCommand ehpCommand;

    @Inject
    public DnModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        this.registerBundle(MODULE_KEY, BASE_MODULE_PATH + ".dn");

        Injector injector = context.getInjector().createChildInjector(new DnInjectorModule(this));

        this.statService = injector.getInstance(DnStatService.class);
        this.statService.init();

        StatCommand.StatCommandFactory scFactory = injector.getInstance(StatCommand.StatCommandFactory.class);
        this.fdLinCommand = scFactory.create(DnStatService.STAT_FD_LINEAR_KEY);
        this.critCommand = scFactory.create(DnStatService.STAT_CRIT_KEY);
        this.critdmgCommand = scFactory.create(DnStatService.STAT_CRITDMG_KEY);
        this.defenseCommand = scFactory.create(DnStatService.STAT_DEFENSE_KEY);
        this.ehpCommand = injector.getInstance(EffectiveHPCommand.class);

        this.registry = this.dispatcher.getRootRegistry().makeRegistries(MODULE_KEY);

        Localizer loc = this.bot.getLocalizer();
        this.fdLinCommand.register(loc, this.registry);
        this.critCommand.register(loc, this.registry);
        this.critdmgCommand.register(loc, this.registry);
        this.defenseCommand.register(loc, this.registry);
//        this.ehpCommand.register(loc, this.registry);
    }

    @Override
    public void shutDown() {
        this.statService.shutdown();
        Localizer loc = this.bot.getLocalizer();
        this.fdLinCommand.unregister(loc, this.registry);
        this.critCommand.unregister(loc, this.registry);
        this.critdmgCommand.unregister(loc, this.registry);
        this.defenseCommand.unregister(loc, this.registry);
    }

    private void registerBundle(String key, String bundleLocation) {
        this.bot.getLocalizer().registerBundle(key,
            new ResourceBundleBundle(bundleLocation,
                this.getClass().getClassLoader()));
    }

    public QueueExceptionHandler getExceptionHandler() {
        return this::handleQueueException;
    }

    public void handleQueueException(Command command, String id, Throwable throwable) {
        LOGGER.warn("Failed to enqueue request for command {}:{}", command.getClass().getSimpleName(), id, throwable);
    }

    public class Accessor {
        public DnModule get() {
            return DnModule.this;
        }
    }
}
