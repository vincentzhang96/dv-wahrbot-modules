package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.NoSuchElementException;

public class DuckDNDiscordFeatures {

    private final WahrBot bot;

    @Inject
    public DuckDNDiscordFeatures(WahrBot bot) {
        this.bot = bot;
    }

    @Subscribe
    public void onReactionAdded(GuildMessageReactionAddEvent event) {
        TextChannel channel = event.getChannel();
        if (channel.getIdLong() != 684227396110385152L) {
            return;
        }

        if (!"U+1f6a2".equalsIgnoreCase(event.getReactionEmote().getAsCodepoints())) {
            return;
        }

        long messageId = event.getMessageIdLong();
        Message message = channel.retrieveMessageById(messageId).complete();
        if (message == null) {
            return;
        }

        UserStore store = this.bot.getUserStorage().forUser(event.getUser());

        try {
            if (!store.getBoolean("pdn.test")) {
                return;
            }
        } catch (NoSuchElementException nsee) {
            return;
        }

        String urlField = null;
        for (MessageEmbed embed : message.getEmbeds()) {
            for (MessageEmbed.Field field : embed.getFields()) {
                if ("DNTs".equalsIgnoreCase(field.getName())) {
                    urlField = field.getValue();
                }
            }
        }

        if (urlField == null) {
            return;
        }

        int idx = urlField.lastIndexOf("/dnt/");
        if (idx == -1) {
            return;
        }

        String url = urlField.substring(idx + 5, urlField.length() - 1);

        message.addReaction("U+1F4AC").queue();

        Unirest.post("http://westus.test.infra.fatduckdn.com:8001/dnt")
                .queryString("name", url)
                .queryString("key", "p6ukcUBSf3GJ8o6kI4wOCygBLCK3nDqU")
                .asStringAsync(new Callback<String>() {
                    @Override
                    public void completed(HttpResponse<String> response) {
                        String bodyS = response.getBody();
                        if (response.getStatus() != 200) {
                            EmbedBuilder b1 = new EmbedBuilder()
                                    .setColor(0xFF0000)
                                    .setTitle("Duck DN Test DNT Deploy")
                                    .addField("Package", url, false)
                                    .addField("Error", bodyS, false)
                                    .appendDescription("DNT deploy FAILED");
                            channel.sendMessage(b1.build()).queue();
                        } else {
                            Emote deployedEmote = bot.getApiClient().getEmoteById(770796418783772752L);
                            if (deployedEmote == null) {
                                message.addReaction("U+1F44D").queue();
                            } else {
                                message.addReaction(deployedEmote).queue();
                            }

                            message.removeReaction("U+1F4AC").queue();
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test DNT Deploy")
                                .addField("Package", url, false)
                                .addField("Error", e.toString(), false)
                                .appendDescription("DNT deploy FAILED");
                        channel.sendMessage(b1.build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test DNT Deploy")
                                .addField("Package", url, false)
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("DNT deploy FAILED");
                        message.editMessage(b1.build()).queue();
                    }
                });
    }
}
