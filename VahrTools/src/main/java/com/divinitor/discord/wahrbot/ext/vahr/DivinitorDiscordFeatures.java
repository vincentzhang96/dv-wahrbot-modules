package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.inject.Inject;

public class DivinitorDiscordFeatures {

    private final WahrBot bot;

    @Inject
    public DivinitorDiscordFeatures(WahrBot bot) {
        this.bot = bot;
    }


}
