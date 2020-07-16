package com.divinitor.discord.wahrbot.ext.fun;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerBundle;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.fun.command.StabCommand;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class FunModule implements Module {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String MODULE_KEY = "ext.fun";
    public static final String BASE_MODULE_PATH = "com.divinitor.discord.wahrbot.ext.fun.commands";
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;

    @Inject
    private StabCommand stabCommand;

    @Inject
    public FunModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {
        Localizer localizer = this.bot.getLocalizer();
        CommandRegistry rootRegistry = this.bot.getCommandDispatcher().getRootRegistry();

        this.stabCommand.register(rootRegistry, localizer);
    }

    @Override
    public void shutDown() {
        Localizer localizer = this.bot.getLocalizer();
        CommandRegistry rootRegistry = this.bot.getCommandDispatcher().getRootRegistry();

        this.stabCommand.unregister(rootRegistry, localizer);
    }

    private LocalizerBundle registerBundle(String key) {
        return this.registerBundle(key, key);
    }

    private LocalizerBundle registerBundle(String key, String bundleLocation) {
        ResourceBundleBundle bundle = new ResourceBundleBundle(bundleLocation,
            this.getClass().getClassLoader());
        this.bot.getLocalizer().registerBundle(key,
            bundle);
        return bundle;
    }
}
