package com.divinitor.discord.wahrbot.ext.mod.commands;

import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class JLMessageHelper {

    public static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss z");

    private JLMessageHelper() {
    }

    public static String format(String pattern, Member member) {
        User user = member.getUser();
        Guild server = member.getGuild();
        return pattern.
            replace("$n", user.getName()).
            replace("$d", user.getDiscriminator()).
            replace("$i", user.getId()).
            replace("$j", SnowflakeUtils.encode(user.getIdLong())).
            replace("$s", server.getName()).
            replace("$t", FORMAT.format(member.getTimeJoined().toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault()))).
            replace("$m", "<@" + user.getId() + ">");
    }

    public static TextChannel getDefaultChannel(Guild guild) {
        TextChannel oldDefault = guild.getTextChannelById(guild.getIdLong());
        if (oldDefault == null) {
            return guild.getDefaultChannel();
        }

        return oldDefault;
    }

}
