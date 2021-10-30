package com.projectduck.discord.wahrbotext;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.store.UserStore;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class DuckDNDiscordFeatures {

    @Getter
    @Setter
    static class CacheEntry {
        String id;

        Map<String, Set<String>> messageToChannels = new HashMap<>();
        Map<String, List<String>> messageToIds = new HashMap<>();

        CacheEntry(String id) {
            this.id = id;
        }

        Pair<Integer, String> addMessage(Message msg, String channelId) {
            String hash = this.hashMessage(msg.getContentRaw());

            Set<String> channels = this.messageToChannels.computeIfAbsent(hash, (k) -> new HashSet<>());
            channels.add(channelId);

            List<String> ids = this.messageToIds.computeIfAbsent(hash, (k) -> new ArrayList<>());
            ids.add(channelId +  ":" + msg.getId());

            return Pair.of(channels.size(), hash);
        }

        @SneakyThrows
        String hashMessage(String msg) {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(msg.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02X", (int) b));
            }

            return sb.toString();
        }
    }

    private final WahrBot bot;

    private LoadingCache<String, CacheEntry> messageCache;

    @Inject
    public DuckDNDiscordFeatures(WahrBot bot) {
        this.bot = bot;
        this.messageCache = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.SECONDS)
                .build(new CacheLoader<String, CacheEntry>() {
                    @Override
                    public CacheEntry load(String key) throws Exception {
                        return new CacheEntry(key);
                    }
                });
    }

    @SneakyThrows
    @Subscribe
    public void handleSpamMessage(GuildMessageReceivedEvent event) {
        Guild guild = event.getGuild();
        if (guild.getIdLong() != 544827049752264704L) {
            return;
        }

        Message message = event.getMessage();
        if (message.isWebhookMessage()) {
            return;
        }

        User author = event.getAuthor();
        if (author.isBot()) {
            return;
        }

        TextChannel channel = event.getChannel();

        CacheEntry cacheEntry = this.messageCache.get(author.getId());
        Pair<Integer, String> entry = cacheEntry.addMessage(message, channel.getId());
        int count = entry.getLeft();
        if (count >= 4) {
            ProjectDuckModule.LOGGER.warn("User {} has posted the same message in {} channels",
                    author.getAsTag(), count);

            Role role = guild.getRoleById(551451726247493634L);
            if (role == null) {
                ProjectDuckModule.LOGGER.warn("Missing timeout role, cannot time out user");
                return;
            }

            ProjectDuckModule.LOGGER.warn("Timing out {} for multichannel message spam", author.getAsTag());
            guild.addRoleToMember(author.getId(), role).queue();

            MessageChannel reportCh = guild.getTextChannelById(575081485825081344L);
            if (reportCh != null) {
                Message msg = new MessageBuilder().append(String.format("Timed out %s for multichannel message spam\n", author.getAsTag()))
                        .appendCodeBlock(message.getContentRaw(), "")
                        .build();

                reportCh.sendMessage(msg).queue();
            }

            String key = entry.getRight();
            List<String> ids = cacheEntry.messageToIds.get(key);
            if (ids != null) {
                ProjectDuckModule.LOGGER.warn("Deleting {} messages", ids.size());

                for (String id : ids) {
                    String[] split = id.split(":");
                    String chId = split[0];
                    String msgId = split[1];

                    MessageChannel spamChannel = guild.getTextChannelById(chId);
                    if (spamChannel != null) {
                        spamChannel.deleteMessageById(msgId).queue();
                    }
                }
            }

        }
    }

    @Subscribe
    public void handleShipReaction(GuildMessageReactionAddEvent event) {
        TextChannel channel = event.getChannel();
        if (channel.getIdLong() != 684227396110385152L && channel.getIdLong() != 808457392169549835L) {
            return;
        }

        MessageReaction.ReactionEmote emote = event.getReactionEmote();
        // Ship emoji
        if (emote.isEmote() || !"U+1f6a2".equalsIgnoreCase(emote.getAsCodepoints())) {
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
