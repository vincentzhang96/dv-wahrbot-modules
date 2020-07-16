package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.inject.Inject;

public class DNNACDDiscordFeatures {

    private final WahrBot bot;

    @Inject
    public DNNACDDiscordFeatures(WahrBot bot) {
        this.bot = bot;
    }

}
