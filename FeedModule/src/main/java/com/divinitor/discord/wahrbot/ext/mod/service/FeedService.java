package com.divinitor.discord.wahrbot.ext.mod.service;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.regex.Pattern;

public interface FeedService {

    /**
     * Pattern used to validate feed IDs. Alphanumeric with dashes, cannot start with a number or dash, cannot end
     * with a dash.
     */
    Pattern FEED_PATTERN = Pattern.compile("[a-z][a-z0-9-]+[a-z0-9]");

    /**
     * Publishes an event to all servers and channels that subscribe to the given
     * feed. The event object will be passed to the registered event handler for
     * the given feed to generate the Discord message for.
     * <br/>
     * If there is no renderer registered for the given feed, the event is
     * silently swallowed.
     * <br/>
     * If the renderer expects a certain type and the feedEvent is not of the type,
     * an IllegalArgumentException may be thrown.
     *
     * @param feedKey   ID of the feed to publish to
     * @param feedEvent The event object
     * @throws IllegalArgumentException if the feedEvent is not of the correct type, or if the feedKey is invalid
     */
    void publish(@NotNull String feedKey,
                 @NotNull Object feedEvent)
        throws IllegalArgumentException;

    /**
     * Subscribe a server and channel to a feed. Events for the given feed will be
     * published in the channel.
     *
     * @param server        The server to publish to
     * @param targetChannel The channel in the server to publish to
     * @param feedKey       ID of the feed to subscribe to
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    void subscribe(@NotNull Guild server,
                   @NotNull TextChannel targetChannel,
                   @NotNull String feedKey);

    /**
     * Unsubscribe a channel from a feed. Events for the given feed will no longer
     * be published in the channel.
     *
     * @param server        The server containing the channel to unsubscribe
     * @param targetChannel The channel to unsubscribe
     * @param feedKey       ID of the feed to unsubscribe from
     * @return true if the channel was previously subscribed and is now unsubscribed,
     * false otherwise
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    boolean unsubscribe(@NotNull Guild server,
                        @NotNull TextChannel targetChannel,
                        @NotNull String feedKey);

    /**
     * Unsubscribe all channels in a server from a feed. Events for the given feed will
     * no longer be published in any channel in the server.
     *
     * @param server  The server to unsubscribe
     * @param feedKey ID of the feed to unsubscribe from
     * @return The number of channels that were unsubscribed, or 0 if no channels
     * were previously subscribed
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    int unsubscribeAllChannels(@NotNull Guild server,
                               @NotNull String feedKey);

    /**
     * Unsubscribe a channel from all feeds.
     *
     * @param server        The server containing the channel to unsubscribe
     * @param targetChannel The channel to unsubscribe
     * @return The IDs of the feeds that were unsubscribed
     */
    Collection<String> unsubscribeAllFeeds(@NotNull Guild server,
                                           @Nullable TextChannel targetChannel);

    /**
     * Unsubscribe a server from all feeds.
     *
     * @param server The server to unsubscribe
     * @return The IDs of the feeds that were unsubscribed
     */
    Collection<String> unsubscribeAllFeeds(@NotNull Guild server);

    /**
     * Get the IDs of the feeds that a server's channels are subscribed to.
     *
     * @param server The server to query
     * @return A possibly empty collection of feed IDs that this server is subscribed to,
     * without duplicates
     */
    @NotNull
    Collection<String> getSubscribedFeeds(@NotNull Guild server);

    /**
     * Get the IDs of the feeds that a channel is subscribed to.
     *
     * @param server        The server containing the channel
     * @param targetChannel The channel to query
     * @return A possibly empty collection of feed IDs that this channel is subscribed to
     */
    @NotNull
    Collection<String> getSubscribedFeeds(@NotNull Guild server,
                                          @Nullable TextChannel targetChannel);

    /**
     * Whether or not a given channel is subscribed to a given feed.
     *
     * @param server        The server containing the channel
     * @param targetChannel The channel to query
     * @param feedKey       The feed ID to query
     * @return Whether or not the given channel is subscribed to the given feed
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    boolean isSubscribed(@NotNull Guild server,
                         @Nullable TextChannel targetChannel,
                         @NotNull String feedKey);

    /**
     * Whether or not a given server is subscribed to a given feed. If any of the server's
     * channels are subscribed, then this method will return true.
     *
     * @param server  The server to query
     * @param feedKey The feed ID to query
     * @return Whether or not the given server is subscribed to the given feed
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    boolean isSubscribed(@NotNull Guild server,
                         @NotNull String feedKey);


    /**
     * Registers a feed and its renderer. If a feed with the same ID already exists,
     * it is replaced. Note that references to the renderer are weak, and therefore
     * a hard reference must be held to the renderer somewhere.
     *
     * @param feedKey  An ID for the feed
     * @param renderer A renderer for the feed
     * @return Whether or not the feed was previously registered
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    boolean register(@NotNull String feedKey,
                     @NotNull FeedEventRenderer<?> renderer);

    /**
     * Unregister a feed and associated renderer.
     *
     * @param feedKey The ID of the feed to unregister
     * @return Whether or not a feed with the given ID existed
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    boolean unregister(@NotNull String feedKey);

    /**
     * Get the currently registered renderer for a given feed, or null if no
     * such feed is registered.
     *
     * @param feedKey The ID of the feed to query
     * @return The feed's event renderer, or null if no such feed exists
     * @throws IllegalArgumentException if the feedKey is invalid
     */
    @Nullable
    FeedEventRenderer<?> getRenderer(@NotNull String feedKey);
}
