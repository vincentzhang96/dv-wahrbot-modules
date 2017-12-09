package com.divinitor.discord.wahrbot.ext.mod.listeners;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionRemoveEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReactionListener {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WahrBot bot;
    private final ServerStorage serverStorage;
    private final Localizer loc;
    private final AtomicReference<ScheduledFuture> configPollTaskFuture;
    private final ReadWriteLock configLock;

    @Inject
    public ReactionListener(WahrBot bot) {
        this.bot = bot;
        this.serverStorage = bot.getServerStorage();
        this.loc = bot.getLocalizer();
        this.configPollTaskFuture = new AtomicReference<>();
        this.configLock = new ReentrantReadWriteLock();
    }

    public void start() {
        if (!this.configPollTaskFuture.compareAndSet(null,
            this.bot.getExecutorService().scheduleWithFixedDelay(this::configPollLoop,
                0, 1, TimeUnit.MINUTES))) {
            LOGGER.warn("Attempted to start reaction config poller while one was already running");
        } else {
            LOGGER.info("Reaction config poller started");
        }
    }

    public void stop() {
        ScheduledFuture old = this.configPollTaskFuture.getAndSet(null);
        if (old != null) {
            old.cancel(true);
            LOGGER.info("Reaction config poller stopped");
        } else {
            LOGGER.info("Reaction config poller already stopped");
        }
    }

    @Subscribe
    public void onReactionAdded(MessageReactionAddEvent event) {

    }

    @Subscribe
    public void onReactionDeleted(MessageReactionRemoveEvent event) {

    }

    private void configPollLoop() {
        try {

        } catch (Exception e) {
            LOGGER.warn("Exception during reaction config poll", e);
        }
    }
}
