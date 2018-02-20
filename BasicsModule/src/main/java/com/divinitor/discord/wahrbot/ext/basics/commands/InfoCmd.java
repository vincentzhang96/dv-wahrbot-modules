package com.divinitor.discord.wahrbot.ext.basics.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.util.discord.MemberResolution;
import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;
import com.divinitor.discord.wahrbot.ext.basics.BasicsModule;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.User;

import java.awt.*;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;

public class InfoCmd implements Command {

    public static final String KEY = "com.divinitor.discord.wahrbot.ext.basics.commands.info";
    private final Localizer loc;

    @Inject
    public InfoCmd(Localizer localizer) {
        this.loc = localizer;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        Message message = context.getMessage();
        String userLookup = context.getCommandLine().remainder();
        Member member;
        if (userLookup.isEmpty()) {
            member = context.getMember();
        } else if (!message.getMentionedUsers().isEmpty()) {
            member = context.getServer().getMember(message.getMentionedUsers().get(0));
        } else {
            member = MemberResolution.findMember(userLookup, context);
        }

        Message ret;
        if (member == null) {
            ret = this.notFound(userLookup, context);
            context.getFeedbackChannel().sendMessage(ret)
                .queue();
            return CommandResult.rejected();
        } else {
            ret = this.forMember(member, context);
            context.getFeedbackChannel().sendMessage(ret)
                .queue();
            return CommandResult.ok();
        }
    }

    private Message forMember(Member member, CommandContext context) {
        //  TODO
        User user = member.getUser();
        Locale l = context.getLocale();

        EmbedBuilder builder = new EmbedBuilder();

        //  AUTHOR
        {
            builder.setColor(member.getColor());
            builder.setAuthor(loc.localizeToLocale(key("author"), l, user.getName(), user.getDiscriminator()),
                null,
                user.getEffectiveAvatarUrl());
        }

        //  NICKNAME
        if (member.getNickname() != null) {
            builder.addField(loc.localizeToLocale(key("nick"), l),
                loc.localizeToLocale(key("nick", "value"), l, member.getNickname()), true);
        }

        //  STATUS
        {
            String statusBody;
            String onlineStatus = loc.localizeToLocale(key("status", member.getOnlineStatus().getKey()), l);
            Game game = member.getGame();
            if (game == null) {
                statusBody = onlineStatus;
            } else {
                if (game.getType() == Game.GameType.STREAMING) {
                    statusBody = loc.localizeToLocale(key("status", "streaming"),
                        l,
                        onlineStatus, game.getName(), game.getUrl());
                } else if (game.getType() == Game.GameType.LISTENING) {
                    statusBody = loc.localizeToLocale(key("status", "listening"),
                        l,
                        onlineStatus, game.getName());
                    BasicsModule.LOGGER.info(new GsonBuilder().setPrettyPrinting().create().toJson(game.asRichPresence()));
                } else {
                    statusBody = loc.localizeToLocale(key("status", "playing"),
                        l,
                        onlineStatus, game.getName());
                }
            }

            builder.addField(loc.localizeToLocale(key("status"), l), statusBody, true);
        }

        //  SERVER AGE
        {
            ZonedDateTime joinDate = member.getJoinDate().toZonedDateTime().withZoneSameInstant(ZoneId.systemDefault());
            Duration duration = Duration.between(
                joinDate,
                OffsetDateTime.now()).abs();
            String ago;
            if (duration.compareTo(Duration.ofDays(7)) > 0) {
                ago = this.formatDurationLong(duration, l);
            } else {
                ago = this.formatDuration(duration, l);
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(loc.localizeToLocale(key("joined", "dateformat"),
                l), l);

            builder.addField(loc.localizeToLocale(key("joined"), l), loc.localizeToLocale(key("joined", "value"), l,
                formatter.format(joinDate), ago), true);
        }

        //  DISCORD AGE
        {
            ZonedDateTime registerDate = user.getCreationTime()
                .toZonedDateTime()
                .withZoneSameInstant(ZoneId.systemDefault());
            Duration registerDuration = Duration.between(
                registerDate,
                OffsetDateTime.now()).abs();
            String registerAgo;
            if (registerDuration.compareTo(Duration.ofDays(7)) > 0) {
                registerAgo = this.formatDurationLong(registerDuration, l);
            } else {
                registerAgo = this.formatDuration(registerDuration, l);
            }

            DateTimeFormatter registerDateFormatter = DateTimeFormatter.ofPattern(loc.localizeToLocale(
                key("age", "dateformat"), l), l);

            builder.addField(loc.localizeToLocale(key("age"), l), loc.localizeToLocale(key("age", "value"), l,
                registerDateFormatter.format(registerDate), registerAgo), true);
        }

        //  AVATAR
        {
            String avatarBody = loc.localizeToLocale(key("avatar", "link"), l, user.getEffectiveAvatarUrl());
            builder.addField(loc.localizeToLocale(key("avatar"), l),
                avatarBody, true);

            builder.setThumbnail(user.getEffectiveAvatarUrl());
        }

        //  PERMISSIONS
        {
            builder.addField(loc.localizeToLocale(key("perms"), l),
                loc.localizeToLocale(key("perms", "value"), l,
                    permissionsToLong(member.getPermissions()),
                    permissionsToLong(member.getPermissions(context.getFeedbackChannel()))), true);
        }

        //  ATTRIBUTES
        {
            StringJoiner attribs = new StringJoiner(" | ");
            if (member.isOwner()) {
                attribs.add(loc.localizeToLocale(key("attrib", "owner"), l));
            }

            //  TODO bot admin and blacklist

            String attribBody = attribs.toString();
            if (!attribBody.isEmpty()) {
                builder.addField(loc.localizeToLocale(key("attrib"), l),
                    attribBody, true);
            }
        }

        //  FOOTER
        {
            builder.setFooter(loc.localizeToLocale(key("footer"), l,
                user.getId(), SnowflakeUtils.encode(user.getIdLong())), null);
        }

        return new MessageBuilder()
            .setEmbed(builder.build())
            .build();
    }

    private Message forUser(User user, Locale locale) {
        //  TODO
        EmbedBuilder builder = new EmbedBuilder();


        return new MessageBuilder()
            .setEmbed(null)
            .build();
    }

    private Message notFound(String query, CommandContext context) {
        Locale locale = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();

        builder.setTitle(loc.localizeToLocale(key("err", "not_found"), locale));
        builder.setDescription(loc.localizeToLocale(key("err", "not_found", "desc"), locale, query,
            context.getNamedLocalizationContextParams()));
        builder.setColor(Color.ORANGE);

        return new MessageBuilder()
            .setEmbed(builder.build())
            .build();
    }

    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }

    private String formatDuration(Duration duration, Locale l) {
        StringJoiner joiner = new StringJoiner(", ");
        long days = duration.toDays();
        if (days > 0) {
            long years = days / 365;
            if (years > 0) {
                long centuries = years / 100;
                if (centuries > 0) {
                    long millenia = centuries / 10;
                    joiner.add(loc.localizeToLocale(key("time.millenia"), l, millenia));
                    centuries = centuries % 10;
                    if (centuries > 0) {
                        joiner.add(loc.localizeToLocale(key("time.centuries"), l, centuries));
                    }
                }
                years = years % 100;
                if (years > 0) {
                    joiner.add(loc.localizeToLocale(key("time.years"), l, years));
                }
            }
            days = days % 365;
            joiner.add(loc.localizeToLocale(key("time.days"), l, days));
        }
        long hours = duration.toHours() % 24L;
        if (hours > 0) {
            joiner.add(loc.localizeToLocale(key("time.hours"), l, hours));
        }
        long minutes = duration.toMinutes() % 60L;
        if (minutes > 0) {
            joiner.add(loc.localizeToLocale(key("time.minutes"), l, minutes));
        } else {
            joiner.add(loc.localizeToLocale(key("time.lessthanminute"), l));
        }
//        long seconds = duration.getSeconds() % 60L;
//        if (seconds > 0) {
//            joiner.add(loc.localize(key("time.seconds"), seconds));
//        }
        return joiner.toString();
    }


    private String formatDurationLong(Duration duration, Locale l) {
        StringJoiner joiner = new StringJoiner(", ");
        long days = duration.toDays();
        if (days > 0) {
            long years = days / 365;
            if (years > 0) {
                long centuries = years / 100;
                if (centuries > 0) {
                    long millenia = centuries / 10;
                    joiner.add(loc.localizeToLocale(key("time.millenia"), l, millenia));
                    centuries = centuries % 10;
                    if (centuries > 0) {
                        joiner.add(loc.localizeToLocale(key("time.centuries"), l, centuries));
                    }
                }
                years = years % 100;
                if (years > 0) {
                    joiner.add(loc.localizeToLocale(key("time.years"), l, years));
                }
            }
            days = days % 365;
            joiner.add(loc.localizeToLocale(key("time.days"), l, days));
        }
        return joiner.toString();
    }

    private long permissionsToLong(List<Permission> permissions) {
        long ret = 0L;
        for (Permission permission : permissions) {
            ret |= permission.getRawValue();
        }

        return ret;
    }
}
