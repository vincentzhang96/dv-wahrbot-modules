package com.divinitor.discord.wahrbot.ext.mod.listeners;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.core.entities.Guild;
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
    private final AtomicReference<ScheduledFuture> maintTaskFuture;
    private final ReadWriteLock configLock;
    private final LoadingCache<Guild, CacheEntry> cache;

    @Inject
    public ReactionListener(WahrBot bot) {
        this.bot = bot;
        this.serverStorage = bot.getServerStorage();
        this.loc = bot.getLocalizer();
        this.maintTaskFuture = new AtomicReference<>();
        this.configLock = new ReentrantReadWriteLock();
        this.cache = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .build(new CacheLoader<Guild, CacheEntry>() {
                @Override
                public CacheEntry load(Guild key) throws Exception {
                    return ReactionListener.this.load(key);
                }
            });
    }

    public void start() {
        if (!this.maintTaskFuture.compareAndSet(null,
            this.bot.getExecutorService().scheduleWithFixedDelay(this::maintTask,
                0, 1, TimeUnit.MINUTES))) {
            LOGGER.warn("Attempted to start reaction maint task while one was already running");
        } else {
            LOGGER.info("Reaction maint task started");
        }
    }

    public void stop() {
        ScheduledFuture old = this.maintTaskFuture.getAndSet(null);
        if (old != null) {
            old.cancel(true);
            LOGGER.info("Reaction maint task stopped");
        } else {
            LOGGER.info("Reaction maint task already stopped");
        }
    }

    @Subscribe
    public void onReactionAdded(MessageReactionAddEvent event) {
        CacheEntry entry = this.cache.getUnchecked(event.getGuild());
        if (entry.skip) {
            return;
        }
    }

    @Subscribe
    public void onReactionDeleted(MessageReactionRemoveEvent event) {
        CacheEntry entry = this.cache.getUnchecked(event.getGuild());
        if (entry.skip) {
            return;
        }
    }

    private CacheEntry load(Guild serverId) {
        CacheEntry ret = new CacheEntry();
        ServerStore store = serverStorage.forServer(serverId);
        if (store.getObj)


        return ret;
    }

    private void maintTask() {
        try {

        } catch (Exception e) {
            LOGGER.warn("Exception during reaction maint task", e);
        }
    }

    private static class CacheEntry {
        boolean skip;
        TLongHashSet messagesToWatch;
    }
}
