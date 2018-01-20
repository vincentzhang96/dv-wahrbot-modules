package com.divinitor.discord.wahrbot.ext.mod.service;

import net.dv8tion.jda.core.entities.TextChannel;

import java.util.Collection;

/**
 * Renders events from feeds into Discord messages for subscribed channels.
 */
public interface FeedEventRenderer<T> {

    /**
     * Render an event to the given subscribed channels.
     *
     * @param event The event to render
     * @param subscribedChannels A collection of TextChannels that are subscribed to this event
     */
    void render(T event, Collection<TextChannel> subscribedChannels);

    /**
     * Get the Class for the event that is expected to be received by this renderer.
     *
     * @return The Class for this renderer
     */
    Class<T> getEventClass();
}
