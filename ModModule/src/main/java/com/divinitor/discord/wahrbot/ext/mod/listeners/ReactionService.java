package com.divinitor.discord.wahrbot.ext.mod.listeners;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import gnu.trove.set.hash.TLongHashSet;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.exceptions.HierarchyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils.SERVER_STORE_MESSAGE_SET_KEY;
import static com.divinitor.discord.wahrbot.ext.mod.util.ReactionUtils.serverStoreMessageRoleMapKey;

@SuppressWarnings("ALL")
public class ReactionService {

    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final WahrBot bot;
    private final ServerStorage serverStorage;
    private final Localizer loc;
    private final AtomicReference<ScheduledFuture> maintTaskFuture;
    private final ReadWriteLock configLock;
    private final LoadingCache<Guild, CacheEntry> cache;

    @Inject
    public ReactionService(WahrBot bot) {
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
                    return ReactionService.this.load(key);
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
        Guild guild = event.getGuild();
        CacheEntry entry = this.cache.getUnchecked(guild);
        if (entry.skip) {
            //  Guild has no reactroles
            return;
        }

        long mid = event.getMessageIdLong();
        if (!entry.messagesToWatch.contains(mid)) {
            //  Message is not of interest
            return;
        }

        ServerStore serverStore = this.serverStorage.forServer(guild);
        Map<String, String> rolesForMsg = serverStore.getObject(serverStoreMessageRoleMapKey(mid), Map.class);

        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        String key;
        if (emote.isEmote()) {
            key = SnowflakeUtils.encode(emote);
        } else {
            key = emote.getName();
        }

        String roleSidStr = rolesForMsg.get(key);
        if (roleSidStr == null) {
            //  Emote is not registered for reactroles
            return;
        }

        long roleId = SnowflakeUtils.decode(roleSidStr);
        Role role = guild.getRoleById(roleId);
        Member member = event.getMember();
        User user = member.getUser();
        if (role == null) {
            //  Role is set up but does not exist anymore
            LOGGER.warn("Unable to find reaction role for emoji {}:{} on message {} in {}#{} ({}) deleted from {}#{} ({})",
                emote.getName(), SnowflakeUtils.encode(emote),
                SnowflakeUtils.encode(mid),
                guild.getName(), event.getChannel().getName(),
                SnowflakeUtils.encode(guild),
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user));
            return;
        }

        try {
            guild.addRoleToMember(member, role).reason("Adding reactrole managed role due to reaction being added")
                .queue(null, (ex) -> {
                    LOGGER.warn("Failed to update roles for {}#{} ({}): Tried adding role {} ({}) but failed",
                        user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user),
                        role.getName(), SnowflakeUtils.encode(role),
                        ex);
                });
        } catch (HierarchyException he) {
            LOGGER.warn("Failed to update roles for {}#{} ({}): Tried adding role {} ({}) but failed",
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user),
                role.getName(), SnowflakeUtils.encode(role),
                he);
        }
    }

    @Subscribe
    public void onReactionDeleted(MessageReactionRemoveEvent event) {
        Guild guild = event.getGuild();
        CacheEntry entry = this.cache.getUnchecked(guild);
        if (entry.skip) {
            //  Guild has no reactroles
            return;
        }

        long mid = event.getMessageIdLong();
        if (!entry.messagesToWatch.contains(mid)) {
            //  Message is not of interest
            return;
        }

        ServerStore serverStore = this.serverStorage.forServer(guild);
        Map<String, String> rolesForMsg = serverStore.getObject(serverStoreMessageRoleMapKey(mid), Map.class);

        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        String key;
        if (emote.isEmote()) {
            key = SnowflakeUtils.encode(emote);
        } else {
            key = emote.getName();
        }

        String roleSidStr = rolesForMsg.get(key);
        if (roleSidStr == null) {
            //  Emote is not registered for reactroles
            return;
        }

        long roleId = SnowflakeUtils.decode(roleSidStr);
        Role role = guild.getRoleById(roleId);
        Member member = event.getMember();
        User user = member.getUser();
        if (role == null) {
            //  Role is set up but does not exist anymore
            LOGGER.warn("Unable to find reaction role for emoji {}:{} on message {} in {}#{} ({}) deleted from {}#{} ({})",
                emote.getName(), SnowflakeUtils.encode(emote),
                SnowflakeUtils.encode(mid),
                guild.getName(), event.getChannel().getName(),
                SnowflakeUtils.encode(guild),
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user));
            return;
        }

        try {
            guild.removeRoleFromMember(member, role).reason("Removing reactrole managed role due to reaction being removed")
                .queue(null, (ex) -> {
                    LOGGER.warn("Failed to update roles for {}#{} ({}): Tried deleting role {} ({}) but failed",
                        user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user),
                        role.getName(), SnowflakeUtils.encode(role),
                        ex);
                });
        } catch (HierarchyException he) {
            LOGGER.warn("Failed to update roles for {}#{} ({}): Tried adding role {} ({}) but failed",
                user.getName(), user.getDiscriminator(), SnowflakeUtils.encode(user),
                role.getName(), SnowflakeUtils.encode(role),
                he);
        }
    }

    public void invalidateGuild(Guild guild) {
        this.cache.invalidate(guild);
    }

    private CacheEntry load(Guild serverId) {
        CacheEntry ret = new CacheEntry();
        ServerStore store = serverStorage.forServer(serverId);
        Set<String> config = store.getObject(SERVER_STORE_MESSAGE_SET_KEY, Set.class);
        if (config.isEmpty()) {
            ret.skip = true;
        } else {
            config.forEach(s -> {
                String mid = ReactionUtils.messageChannelPairMsgId(s);
                ret.messagesToWatch.add(SnowflakeUtils.decode(mid));
            });
        }

        return ret;
    }

    private void maintTask() {
        try {
            //  TODO clean up dead keys and stuff
        } catch (Exception e) {
            LOGGER.warn("Exception during reaction maint task", e);
        }
    }

    public void initReactRoleMessage(ServerStore serverStore, long targetMessageId, long targetChannelId) {
        Set<String> config = serverStore.getObject(SERVER_STORE_MESSAGE_SET_KEY, Set.class);
        String encodedTargetMessageId = SnowflakeUtils.encode(targetMessageId);
        String encodedTargetChannelId = SnowflakeUtils.encode(targetChannelId);
        config.add(ReactionUtils.messageChannelPair(encodedTargetChannelId, encodedTargetMessageId));
        this.invalidateGuild(serverStore.getServer());
    }

//    public void addReactRoleToMessage(ServerStore serverStore)

    private static class CacheEntry {
        boolean skip;
        TLongHashSet messagesToWatch;

        CacheEntry() {
            this.messagesToWatch = new TLongHashSet();
            this.skip = false;
        }
    }
}
