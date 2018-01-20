package com.divinitor.discord.wahrbot.ext.lol;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.CommandDispatcher;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigStore;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.module.Module;
import com.divinitor.discord.wahrbot.core.module.ModuleContext;
import com.divinitor.discord.wahrbot.ext.lol.commands.BuildsCmd;
import com.divinitor.discord.wahrbot.ext.lol.commands.GameCmd;
import com.divinitor.discord.wahrbot.ext.lol.commands.RankCmd;
import com.divinitor.discord.wahrbot.ext.lol.commands.RunesCmd;
import com.google.inject.Inject;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

@Getter
public class LoLModule implements Module {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final WahrBot bot;
    private final DynConfigStore dynConfigStore;
    private final CommandDispatcher dispatcher;

    private LoLService service;

    @Inject
    private BuildsCmd buildsCmd;
    @Inject
    private GameCmd gameCmd;
    @Inject
    private RankCmd rankCmd;
    @Inject
    private RunesCmd runesCmd;

    private CommandRegistry registry;

    @Inject
    public LoLModule(WahrBot bot, DynConfigStore dynConfigStore, CommandDispatcher dispatcher) {
        this.bot = bot;
        this.dynConfigStore = dynConfigStore;
        this.dispatcher = dispatcher;
    }

    @Override
    public void init(ModuleContext context) throws Exception {

        this.service = new LoLService(this);
        this.service.init();

        this.bot.getLocalizer().registerBundle(buildsCmd.key(),
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.lol.commands.builds",
                this.getClass().getClassLoader()));
        this.bot.getLocalizer().registerBundle(gameCmd.key(),
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.lol.commands.game",
                this.getClass().getClassLoader()));
        this.bot.getLocalizer().registerBundle(rankCmd.key(),
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.lol.commands.rank",
                this.getClass().getClassLoader()));
        this.bot.getLocalizer().registerBundle(runesCmd.key(),
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.lol.commands.runes",
                this.getClass().getClassLoader()));


        this.bot.getLocalizer().registerBundle("ext.lol",
            new ResourceBundleBundle("com.divinitor.discord.wahrbot.ext.lol.lol",
                this.getClass().getClassLoader()));

        this.registry = this.dispatcher.getRootRegistry().makeRegistries("ext.lol");

        buildsCmd.setService(this.service);
        gameCmd.setService(this.service);
        rankCmd.setService(this.service);
        runesCmd.setService(this.service);

//        this.registry.registerCommand(buildsCmd, buildsCmd.key());
//        this.registry.registerCommand(gameCmd, gameCmd.key());
        this.registry.registerCommand(rankCmd, rankCmd.key());
//        this.registry.registerCommand(runesCmd, runesCmd.key());
    }

    @Override
    public void shutDown() {
        this.registry.unregisterCommand(buildsCmd.key());
        this.registry.unregisterCommand(gameCmd.key());
        this.registry.unregisterCommand(rankCmd.key());
        this.registry.unregisterCommand(runesCmd.key());

        this.bot.getLocalizer().unregisterBundle(buildsCmd.key());
        this.bot.getLocalizer().unregisterBundle(gameCmd.key());
        this.bot.getLocalizer().unregisterBundle(rankCmd.key());
        this.bot.getLocalizer().unregisterBundle(runesCmd.key());
    }
}
