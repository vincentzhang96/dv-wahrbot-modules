package com.divinitor.discord.wahrbot.ext.fun;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class FunModule implements Module {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    public static final String MODULE_KEY = "ext.mod";
    public static final String BASE_MODULE_PATH = "com.divinitor.discord.wahrbot.ext.mod.commands";
    private final WahrBot bot;
    private final CommandDispatcher dispatcher;

    @Inject
    public FunModule(WahrBot bot, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {

    }

    @Override
    public void shutDown() {

    }

    private void registerBundle(String key) {
        this.registerBundle(key, key);
    }

    private void registerBundle(String key, String bundleLocation) {
        this.bot.getLocalizer().registerBundle(key,
                new ResourceBundleBundle(bundleLocation,
                        this.getClass().getClassLoader()));
    }
}
