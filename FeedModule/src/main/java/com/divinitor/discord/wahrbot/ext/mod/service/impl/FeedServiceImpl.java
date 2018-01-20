package com.divinitor.discord.wahrbot.ext.mod.service.impl;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.ext.mod.FeedModule;
import com.divinitor.discord.wahrbot.ext.mod.service.FeedEventRenderer;
import com.divinitor.discord.wahrbot.ext.mod.service.FeedService;
import com.google.inject.Inject;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.util.Objects.requireNonNull;

public class FeedServiceImpl implements FeedService {

    private static final String BASE_KEY = "ext.feed";


    private final Map<String, FeedEventRenderer<?>> renderers;

    private final WahrBot bot;

    @Inject
    public FeedServiceImpl(WahrBot bot) {
        this.bot = bot;
        this.renderers = new HashMap<>();
    }

    @SuppressWarnings("unchecked")
    private Collection<TextChannel> getChannelsSubscribedToFeed(String feedKey) {
        Set<String> servers = bot.getDynConfigStore().getObject(feedDynKey(feedKey), Set.class, String.class);
        if (servers.isEmpty()) {
            return Collections.emptyList();
        }

        List<TextChannel> ret = new ArrayList<>();
        for (String server : servers) {

        }

        //  TODO
        return ret;
    }

    private void validate(String feedKey) throws IllegalArgumentException {
        if (!FEED_PATTERN.asPredicate().test(feedKey)) {
            throw new IllegalArgumentException("Invalid feed key: Feed key must match "
                + FEED_PATTERN.toString()
                + ", got "
                + feedKey);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void publish(@NotNull String feedKey, @NotNull Object feedEvent) {
        requireNonNull(feedKey, "Feed key cannot be null");
        requireNonNull(feedEvent, "Feed event cannot be null");
        validate(feedKey);

        FeedEventRenderer renderer = this.renderers.get(feedKey);
        if (renderer == null) {
            //  Silently swallow
            return;
        }

        Class<?> eventClass = renderer.getEventClass();
        if (!eventClass.isAssignableFrom(feedEvent.getClass())) {
            throw new IllegalArgumentException(String.format("Expected %s, got %s",
                eventClass.getSimpleName(), feedEvent.getClass().getSimpleName()));
        }

        Collection<TextChannel> subChannels = this.getChannelsSubscribedToFeed(feedKey);
        if (!subChannels.isEmpty()) {
            try {
                renderer.render(feedEvent, subChannels);
            } catch (Error error) {
                throw error;
            } catch (Throwable throwable) {
                //  Log exception
                FeedModule.LOGGER.warn("Exception while executing feed renderer for feed '{}'", feedKey, throwable);
            }
        }
    }

    @Override
    public void subscribe(@NotNull Guild server, @NotNull TextChannel targetChannel, @NotNull String feedKey) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(targetChannel, "Target channel cannot be null");
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);


    }

    @Override
    public boolean unsubscribe(@NotNull Guild server, @NotNull TextChannel targetChannel, @NotNull String feedKey) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(targetChannel, "Target channel cannot be null");
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return false;
    }

    @Override
    public int unsubscribeAllChannels(@NotNull Guild server, @NotNull String feedKey) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return 0;
    }

    @Override
    public Collection<String> unsubscribeAllFeeds(@NotNull Guild server, @Nullable TextChannel targetChannel) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(targetChannel, "Target channel cannot be null");

        return null;
    }

    @Override
    public Collection<String> unsubscribeAllFeeds(@NotNull Guild server) {
        requireNonNull(server, "Server cannot be null");

        return null;
    }

    @NotNull
    @Override
    public Collection<String> getSubscribedFeeds(@NotNull Guild server) {
        requireNonNull(server, "Server cannot be null");

        return null;
    }

    @NotNull
    @Override
    public Collection<String> getSubscribedFeeds(@NotNull Guild server, @Nullable TextChannel targetChannel) {
        requireNonNull(server, "Server cannot be null");

        return null;
    }

    @Override
    public boolean isSubscribed(@NotNull Guild server, @Nullable TextChannel targetChannel, @NotNull String feedKey) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return false;
    }

    @Override
    public boolean isSubscribed(@NotNull Guild server, @NotNull String feedKey) {
        requireNonNull(server, "Server cannot be null");
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return false;
    }

    @Override
    public boolean register(@NotNull String feedKey, @NotNull FeedEventRenderer<?> renderer) {
        requireNonNull(feedKey, "Feed key cannot be null");
        requireNonNull(renderer, "Feed event renderer cannot be null");

        return false;
    }

    @Override
    public boolean unregister(@NotNull String feedKey) {
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return false;
    }

    @Nullable
    @Override
    public FeedEventRenderer<?> getRenderer(@NotNull String feedKey) {
        requireNonNull(feedKey, "Feed key cannot be null");
        validate(feedKey);

        return null;
    }

    private static String feedDynKey(String feedKey) {
        return BASE_KEY + "." + feedKey;
    }

    private static String feedDynKeyServer(String feedKey, String serverIdShort) {
        if (!serverIdShort.startsWith("$")) {
            throw new IllegalArgumentException("Not a short ID");
        }

        return BASE_KEY + "." + feedKey + ".srv." + serverIdShort;
    }
}
