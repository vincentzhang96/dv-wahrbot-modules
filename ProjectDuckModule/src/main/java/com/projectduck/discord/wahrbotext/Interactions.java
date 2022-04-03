package com.projectduck.discord.wahrbotext;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.google.common.base.Strings;
import com.google.common.eventbus.Subscribe;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.projectduck.discord.wahrbotext.command.DuckTestMakeResourceCommand;
import com.projectduck.discord.wahrbotext.command.DuckTestRestartCommand;
import com.projectduck.discord.wahrbotext.command.DuckTestResyncCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.GenericInteractionCreateEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.Interaction;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import java.util.NoSuchElementException;

public class Interactions {

    private final WahrBot bot;
    private DuckDNDiscordFeatures features;

    @Inject
    public Interactions(WahrBot bot) {
        this.bot = bot;
    }

    public void setUp(DuckDNDiscordFeatures features) {
        this.features = features;
    }

    @Subscribe
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String[] split = Strings.nullToEmpty(event.getButton().getId()).split(";", 2);
        switch (split[0].toLowerCase()) {
            case "deploy": {
                this.shipDnt(event);
                break;
            }
            case "resync": {
                this.resync(event);
                break;
            }
            case "rtst": {
                this.restartTest(event, false);
                break;
            }
            case "rtst g": {
                this.restartTest(event, true);
                break;
            }
            case "makeres": {
                String[] split2 = split[1].split(";", 2);
                this.makeRes(event, "true".equals(split2[0]), split2[1]);
                break;
            }
            default: {
                event.reply("No matching action: " + split[0]).queue();
                break;
            }
        }
    }

    private void shipDnt(ButtonInteractionEvent event) {
        Message message = event.getMessage();
        User user = event.getUser();
        TextChannel channel = event.getTextChannel();
        UserStore store = this.bot.getUserStorage().forUser(user);

        try {
            if (!store.getBoolean("pdn.test")) {
                event.reply("You do not have permission").queue();
                return;
            }
        } catch (NoSuchElementException nsee) {
            event.reply("You do not have permission").queue();
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
            event.reply("Unable to parse deployment URL").queue();
            return;
        }

        int idx = urlField.lastIndexOf("/dnt/");
        if (idx == -1) {
            event.reply("Invalid deployment URL").queue();
            return;
        }

        String url = urlField.substring(idx + 5, urlField.length() - 1);

        InteractionHook hook = event.deferReply().complete();
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
                            hook.sendMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                        } else {
                            Emote deployedEmote = bot.getApiClient().getEmoteById(770796418783772752L);
                            if (deployedEmote == null) {
                                message.addReaction("U+1F44D").queue();
                            } else {
                                message.addReaction(deployedEmote).queue();
                            }

                            message.removeReaction("U+1F4AC").queue();

                            hook.deleteOriginal().queue();
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
                        hook.sendMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test DNT Deploy")
                                .addField("Package", url, false)
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("DNT deploy FAILED");
                        hook.sendMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                    }
                });
    }

    private void restartTest(ButtonInteractionEvent event, boolean gameOnly) {
        User user = event.getUser();
        TextChannel channel = event.getTextChannel();
        UserStore store = this.bot.getUserStorage().forUser(user);

        try {
            if (!store.getBoolean("pdn.test")) {
                event.reply("You do not have permission").queue();
                return;
            }
        } catch (NoSuchElementException nsee) {
            event.reply("You do not have permission").queue();
            return;
        }

        InteractionHook hook = event.reply("Requested restart" + (gameOnly ? " of game server" : "")).setEphemeral(true).complete();
        DuckTestRestartCommand.restartTest(event.getMessage(), gameOnly, (err) -> {
            if (Strings.isNullOrEmpty(err)) {
                hook.editOriginal("Restarting").queue();
            } else {
                hook.editOriginal(err).queue();
            }
        });
    }

    private void resync(ButtonInteractionEvent event) {
        User user = event.getUser();
        TextChannel channel = event.getTextChannel();
        UserStore store = this.bot.getUserStorage().forUser(user);

        try {
            if (!store.getBoolean("pdn.test")) {
                event.reply("You do not have permission").queue();
                return;
            }
        } catch (NoSuchElementException nsee) {
            event.reply("You do not have permission").queue();
            return;
        }

        InteractionHook hook = event.reply("Requested resync").setEphemeral(true).complete();

        DuckTestResyncCommand.resyncTest(event.getMessage(), (err) -> {
            if (Strings.isNullOrEmpty(err)) {
                hook.editOriginal("Resync completed").queue();
            } else {
                hook.editOriginal(err).queue();
            }
        });
    }

    private void makeRes(ButtonInteractionEvent event, boolean deploy, String branch) {
        User user = event.getUser();
        TextChannel channel = event.getTextChannel();
        UserStore store = this.bot.getUserStorage().forUser(user);

        try {
            if (!store.getBoolean("pdn.test")) {
                event.reply("You do not have permission").queue();
                return;
            }
        } catch (NoSuchElementException nsee) {
            event.reply("You do not have permission").queue();
            return;
        }

        InteractionHook hook = event.reply("Rerunning").setEphemeral(true).complete();
        DuckTestMakeResourceCommand.makeRes(event.getChannel(), branch, deploy);
    }
}
