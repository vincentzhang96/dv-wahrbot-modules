package com.divinitor.discord.wahrbot.ext.mod.event;

import com.divinitor.discord.wahrbot.ext.mod.FeedModule;
import lombok.Getter;

@Getter
public class FeedServiceLoadEvent {
    private final FeedModule module;

    public FeedServiceLoadEvent(FeedModule module) {
        this.module = module;
    }
}
