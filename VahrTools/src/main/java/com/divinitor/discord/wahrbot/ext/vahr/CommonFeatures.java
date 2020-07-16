package com.divinitor.discord.wahrbot.ext.vahr;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.google.common.eventbus.Subscribe;
import com.google.inject.Inject;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class CommonFeatures {

    private final WahrBot bot;

    @Inject
    public CommonFeatures(WahrBot bot) {
        this.bot = bot;
    }

    @Subscribe
    public void handleServerMessage(GuildMessageReceivedEvent event) {
        Member author = event.getMember();

        Message message = event.getMessage();
        String stripped = message.getContentStripped();
        String strippedLower = stripped.toLowerCase();
        boolean isSpam = strippedLower.contains("sex dating") && strippedLower.contains("http://discord.amazingsexdating.com");
        if (isSpam) {
            boolean canBan = true;
            if (!event.getGuild().getSelfMember().hasPermission(Permission.BAN_MEMBERS)) {
                VahrModule.LOGGER.warn("No permission in {} ({}) to ban, cannot ban sex bot spammer",
                    event.getGuild().getName(), event.getGuild().getId());
                canBan = false;
            }

            boolean canDelete = true;
            if (!event.getGuild().getSelfMember().hasPermission(event.getChannel(), Permission.MESSAGE_MANAGE)) {
                VahrModule.LOGGER.warn("No permission in {} ({}) to delete messages, cannot delete sex bot spammer message",
                    event.getGuild().getName(), event.getGuild().getId());
                canDelete = false;
            }

            if (!canDelete && !canBan) {
                return;
            }
            // Delete the message
            if (canDelete) {
                VahrModule.LOGGER.info("Deleting sex bot spam message from {}#{} ({}) in {} ({})",
                    author.getUser().getName(), author.getUser().getDiscriminator(), SnowflakeUtils.encode(author.getUser()),
                    event.getGuild().getName(), SnowflakeUtils.encode(event.getGuild()));
                message.delete().reason("Sex bot spam").queue();
            }

            // Apply ban
            if (canBan) {
                VahrModule.LOGGER.info("Banning sex bot spammer {}#{} ({}) from {} ({})",
                    author.getUser().getName(), author.getUser().getDiscriminator(), SnowflakeUtils.encode(author.getUser()),
                    event.getGuild().getName(), SnowflakeUtils.encode(event.getGuild()));
                event.getGuild().getController().ban(author, 1, "Auto spam bot").queue((v) -> {
                    VahrModule.LOGGER.info("Successfully banned {}#{} ({})",
                        author.getUser().getName(), author.getUser().getDiscriminator(), SnowflakeUtils.encode(author.getUser()));
                }, throwable -> VahrModule.LOGGER.warn("Failed to ban {}#{} ({})",
                    author.getUser().getName(), author.getUser().getDiscriminator(), SnowflakeUtils.encode(author.getUser()), throwable));
            }
        }
    }
}
