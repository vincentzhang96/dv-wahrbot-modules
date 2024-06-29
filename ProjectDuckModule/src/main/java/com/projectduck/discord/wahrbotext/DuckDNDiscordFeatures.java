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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.internal.utils.tuple.Pair;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class DuckDNDiscordFeatures {

    public static String DEPLOY_KEY = "qk7vBbnCcndYjgz36hoC";
    public static String AUTH_KEY = "DWrnq7ZOjNeFcH3yTzLV";

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
    public void wipChangelog(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

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

        if (author.getIdLong() == bot.getApiClient().getSelfUser().getIdLong()) {
            return;
        }

        MessageChannel ch = event.getChannel();

        if (ch.getIdLong() != 834195297285701642L) {
            return;
        }

        String raw = message.getContentRaw();
        if (!Pattern.compile("^v[0-9]+ -").matcher(raw).find()) {
            ProjectDuckModule.LOGGER.warn("Message {} did not match", raw);
            return;
        }

        String[] contentSplit = raw.split(" -", 2);
        if (contentSplit.length < 2) {
            return;
        }

        String content = contentSplit[1].trim();

        MessageBuilder builder = new MessageBuilder();
        builder.setEmbeds(new EmbedBuilder()
                .appendDescription(content)
                .setTitle(contentSplit[0])
                .build()
        );

        MessageAction messageAction = ch.sendMessage(builder.build());
        MessageAction attachmentAction = null;
        ArrayList<File> files = new ArrayList<>();
        if (!message.getAttachments().isEmpty()) {
            for (Message.Attachment attachment : message.getAttachments()) {
                try {
                    File tempFile = File.createTempFile("wip-changelog-", attachment.getFileName());
                    try {
                        attachment.downloadToFile(tempFile).get();
                        files.add(tempFile);
                        if (attachmentAction == null) {
                            attachmentAction = ch.sendFile(tempFile);
                        } else {
                            attachmentAction = attachmentAction.addFile(tempFile);
                        }
                    } catch (Exception e) {
                        ProjectDuckModule.LOGGER.warn("Failed to download attachment {}", attachment.getFileName());
                        tempFile.delete();
                    }
                } catch (Exception e) {
                    ProjectDuckModule.LOGGER.warn("Failed to create attachment temp file for {}", attachment.getFileName(), e);
                }
            }
        }

        final MessageAction finalAttachmentAction = attachmentAction;
        messageAction.queue((m) -> {
            if (finalAttachmentAction != null) {
                finalAttachmentAction.queue((m1) -> {
                    ProjectDuckModule.LOGGER.info("message replaced");
                    // delete the original message
                    message.delete().queue();

                    // delete temp files
                    for (File file : files) {
                        try {
                            file.delete();
                        } catch (Exception e) {
                            ProjectDuckModule.LOGGER.warn("Failed to delete temp file {}", file.getPath(), e);
                        }
                    }
                }, (ex) -> {
                    // delete temp files
                    for (File file : files) {
                        try {
                            file.delete();
                        } catch (Exception e) {
                            ProjectDuckModule.LOGGER.warn("Failed to delete temp file {}", file.getPath(), e);
                        }
                    }
                });
            } else {
                ProjectDuckModule.LOGGER.info("message replaced");
                // delete the original message
                message.delete().queue();
            }
        }, (ex) -> {
            // delete temp files
            for (File file : files) {
                try {
                    file.delete();
                } catch (Exception e) {
                    ProjectDuckModule.LOGGER.warn("Failed to delete temp file {}", file.getPath(), e);
                }
            }
        });
    }

    @SneakyThrows
    @Subscribe
    public void handleSpamMessage(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

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

        MessageChannel ch = event.getChannel();
        if (!(ch instanceof TextChannel)) {
            return;
        }

        TextChannel channel = (TextChannel) ch;

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

    public String shipDnt(Message message, User user, TextChannel channel) {
        UserStore store = this.bot.getUserStorage().forUser(user);

        try {
            if (!store.getBoolean("pdn.test")) {
                return "You do not have permission";
            }
        } catch (NoSuchElementException nsee) {
            return "You do not have permission";
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
            return "Unable to parse deployment URL";
        }

        int idx = urlField.lastIndexOf("/dnt/");
        if (idx == -1) {
            return "Invalid deployment URL";
        }

        String url = urlField.substring(idx + 5, urlField.length() - 1);

        message.addReaction("U+1F4AC").queue();

        Unirest.post("http://westus.test.infra.fatduckdn.com:8001/dnt")
                .queryString("name", url)
                .queryString("key", DuckDNDiscordFeatures.DEPLOY_KEY)
                .queryString("obo", user.getId())
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
                            channel.sendMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
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
                        channel.sendMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test DNT Deploy")
                                .addField("Package", url, false)
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("DNT deploy FAILED");
                        message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                    }
                });

        return "Deployment queued";
    }

    @Subscribe
    public void handleShipReaction(MessageReactionAddEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        MessageChannel ch = event.getChannel();
        if (!(ch instanceof TextChannel)) {
            return;
        }

        TextChannel channel = (TextChannel) ch;

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

        shipDnt(message, event.getUser(), channel);
    }

    private Instant lastTierListTime = null;

    static class JobClass {
        int id;
        String name;
        long emoteId;
        JobClass(int id, String name, long emoteId) {
            this.id = id;
            this.name = name;
            this.emoteId = emoteId;
        }
    }

    static List<JobClass> jobClasses = new ArrayList<>();
    static {
        jobClasses.add(new JobClass(23, "Gladiator",  897275591571689522L));
//        jobClasses.add(new JobClass(24, "Lunar Knight", 897275591282278491L));
        jobClasses.add(new JobClass(25, "Barbarian", 897275591370342440L));
        jobClasses.add(new JobClass(26, "Destroyer", 897275591097737246L));
        jobClasses.add(new JobClass(76, "Dark Avenger", 897275591072579657L));
//        jobClasses.add(new JobClass(70, "Mystic Knight", 930241328883859476L));
        jobClasses.add(new JobClass(71, "Grand Master", 930241365852454932L));
        jobClasses.add(new JobClass(29, "Sniper", 897275270577401897L));
        jobClasses.add(new JobClass(30, "Warden", 897275440283123733L));
        jobClasses.add(new JobClass(31, "Tempest", 897275269956632618L));
        jobClasses.add(new JobClass(32, "Windwalker", 897275270602555455L));
        jobClasses.add(new JobClass(81, "Silver Hunter", 897275393768300575L));
        jobClasses.add(new JobClass(35, "Pyromancer", 897275680759365662L));
        jobClasses.add(new JobClass(36, "Ice Witch", 897275680432222269L));
        jobClasses.add(new JobClass(37, "War Mage", 897275681287839764L));
        jobClasses.add(new JobClass(38, "Chaos Mage", 897275680625131561L));
        jobClasses.add(new JobClass(85, "Black Mara", 897275680381865985L));
        jobClasses.add(new JobClass(41, "Guardian", 897275350722154546L));
        jobClasses.add(new JobClass(42, "Crusader", 897275350378225745L));
        jobClasses.add(new JobClass(43, "Saint", 897275350197878815L));
        jobClasses.add(new JobClass(44, "Inquisitor", 897275350357270539L));
        jobClasses.add(new JobClass(83, "Arch Heretic", 897275349589721110L));
        jobClasses.add(new JobClass(47, "Shooting Star", 897275640405950486L));
        jobClasses.add(new JobClass(48, "Gearmaster", 897275640158498828L));
        jobClasses.add(new JobClass(50, "Adept", 897275640288518204L));
        jobClasses.add(new JobClass(51, "Physician", 897275640150122547L));
        jobClasses.add(new JobClass(87, "Ray Mechanic", 897275764616093726L));
        jobClasses.add(new JobClass(55, "Dark Summoner", 897275772610420777L));
        jobClasses.add(new JobClass(56, "Soul Eater", 897275772845326357L));
        jobClasses.add(new JobClass(58, "Blade Dancer", 897275773029859339L));
        jobClasses.add(new JobClass(59, "Spirit Dancer", 897275772874678312L));
        jobClasses.add(new JobClass(63, "Reaper", 897275318660911184L));
        jobClasses.add(new JobClass(64, "Raven", 897275318316961822L));
//        jobClasses.add(new JobClass(65, "Jotunn", 1061446275872075816L));
        jobClasses.add(new JobClass(66, "Rai", 1061446323645186158L));
        jobClasses.add(new JobClass(68, "Light Fury", 897275318426017843L));
        jobClasses.add(new JobClass(69, "Abyss Walker", 897275318396649482L));
        jobClasses.add(new JobClass(91, "Blood Phantom", 897275317759131678L));
        jobClasses.add(new JobClass(73, "Dragoon", 897275721439916042L));
        jobClasses.add(new JobClass(74, "Valkyrie", 897275718986264587L));
        jobClasses.add(new JobClass(93, "Avalanche", 897275842890178620L));
        jobClasses.add(new JobClass(94, "Randgrid", 897275834124107776L));
        jobClasses.add(new JobClass(99, "Vena Plaga", 1049727072416845874L));
        jobClasses.add(new JobClass(78, "Defensio", 897275589893947422L));
        jobClasses.add(new JobClass(79, "Ruina", 897275589944295454L));
        jobClasses.add(new JobClass(96, "Impactor", 897275589713621005L));
        jobClasses.add(new JobClass(97, "Lustre", 897275589759729665L));
        jobClasses.add(new JobClass(102, "Duelist", 1121233488327213227L));
//        jobClasses.add(new JobClass(103, "Trickster", 1121233510095659028L));
    }

    @SneakyThrows
    @Subscribe
    public void autoreplyTierList(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }

        Instant now = Instant.now();
        if (lastTierListTime != null) {
            long deltaSec = Duration.between(lastTierListTime, now).abs().getSeconds();
            if (deltaSec < 60 * 5) {
                return;
            }
        }

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

        String raw = message.getContentRaw().toLowerCase();
        if (raw.contains("tier list") || raw.contains("tierlist")) {
            lastTierListTime = now;

            Random random = new Random();
            int idx = random.nextInt(jobClasses.size());
            JobClass job = jobClasses.get(idx);
            Emote emote = guild.getEmoteById(job.emoteId);
            if (emote != null) {
                Message msg = new MessageBuilder()
                        .append(author)
                        .append(" The current top DPS class is ")
                        .append(emote.getAsMention())
                        .append(" ")
                        .append(job.name)
                        .append(". You should try it out!")
                        .mention(author)
                        .build();

                message.getChannel().sendMessage(msg).queue();
            } else {
                Message msg = new MessageBuilder()
                        .append(author)
                        .append(" Your skill level on a class generally matters more, most classes are fairly close. Stop monkeying and caring about DPS so much and just play what you like.")
                        .mention(author)
                        .build();

                message.getChannel().sendMessage(msg).queue();
            }
        }
        else if (raw.contains("no such item exists")) {
            lastTierListTime = now;
            Message msg = new MessageBuilder()
                    .append("Unequip all your rune amps and re-equip them")
                    .build();

            message.reply(msg).queue();
        }
    }


    @SneakyThrows
    @Subscribe
    public void autoreplyWelcomeChannel(MessageReceivedEvent event) {
        if (!event.isFromGuild()) {
            return;
        }
        Guild guild = event.getGuild();
        if (guild.getIdLong() != 544827049752264704L) {
            return;
        }

        Message message = event.getMessage();
        if (message.isWebhookMessage()) {
            return;
        }

        if (message.getChannel().getIdLong() != 882346314858397776L) {
            return;
        }

        Member member = event.getMember();
        User author = event.getAuthor();
        if (author.isBot()) {
            return;
        }

        if (member != null && member.hasPermission(Permission.MESSAGE_MANAGE)) {
            return;
        }

        Message msg = new MessageBuilder()
                .append(author)
                .append(" you must verify to become a member to send messages and view the rest of the Discord server\n\n")
                .append("Verify by following the instructions here https://discord.com/channels/544827049752264704/882346314858397776/1061535316462280795")
                .build();

        message.reply(msg).queue((m) -> {

        });

    }
}
