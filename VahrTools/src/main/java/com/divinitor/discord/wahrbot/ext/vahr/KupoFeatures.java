package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

public class KupoFeatures {
    private final WahrBot bot;

    @Inject
    public KupoFeatures(WahrBot bot) {
        this.bot = bot;
    }

    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        if (event.getGuild().getIdLong() != 469803717265326091L) {
            return;
        }

        if (event.getChannel().getIdLong() != 469803717265326093L) {
            return;
        }

//        List<Emote> emotes = bot.getApiClient().getEmotesByName("diana", true);
//        if (emotes.size() > 0) {
//            event.getMessage().addReaction(emotes.get(0)).queueAfter(500, TimeUnit.MILLISECONDS);
//        }
    }
}
