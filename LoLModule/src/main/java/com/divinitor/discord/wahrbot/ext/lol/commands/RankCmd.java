package com.divinitor.discord.wahrbot.ext.lol.commands;

import com.divinitor.discord.wahrbot.core.command.Command;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.lol.LoLService;
import com.divinitor.discord.wahrbot.ext.lol.entity.SummonerLeagueRank;
import com.divinitor.discord.wahrbot.ext.lol.entity.SummonerSummary;
import com.google.inject.Inject;
import com.mingweisamuel.zyra.enums.Region;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class RankCmd implements Command {

    public static final String KEY = "ext.lol.commands.rank";
    private final Localizer loc;
    private LoLService service;

    @Inject
    public RankCmd(Localizer loc) {
        this.loc = loc;
    }


    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        if (!line.hasNext()) {
            return this.rejectMissingArgs(context);
        }

        String summonerName = line.next();
        String regionCode = null;
        if (line.hasNext()) {
            regionCode = line.next();
        }

        Region region = null;
        if (regionCode != null) {
            region = Region.parse(regionCode);
            if (region == null) {
                return this.rejectUnknownRegion(context, regionCode);
            }
        }

        SummonerSummary summary = this.service.getSummonerSummary(summonerName, region);

        EmbedBuilder b = new EmbedBuilder()
            .setTitle(this.loc.localizeToLocale(key("resp.title"), l,
                summary.getName(), summary.getLevel()),
                this.loc.localizeToLocale(key("resp.url"), l, safeUrlEncode(summary.getName()), nlcp))
            .setFooter(this.loc.localizeToLocale(key("resp.footer"), l),
                this.loc.localizeToLocale(key("resp.footer.img"), l))
            .setThumbnail(summary.getProfileIconUrl());

        for (SummonerLeagueRank rank : summary.getRanked()) {
            b = b.addField(this.loc.localizeToLocale("ext.lol.commands.common.data.queues." + rank.getQueue(), l),
                this.buildQueueBody(context, rank), false);
        }

        if (summary.getRanked().isEmpty()) {
            b.addField(this.loc.localizeToLocale(key("resp.unranked"), l),
                this.loc.localizeToLocale(key("resp.unranked.desc"), l, summary.getName()), false);
        }

        MessageEmbed message = b.build();

        context.getFeedbackChannel().sendMessage(message)
            .queue(null, handleQueueException());

        return CommandResult.ok();
    }

    private String safeUrlEncode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildQueueBody(CommandContext context, SummonerLeagueRank rank) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();

        String league = this.loc.localizeToLocale(this.key("resp.league"), l,
            this.firstCase(rank.getTier()), rank.getRank(), rank.getLeagueName(), nlcp);

        String lpOrPromo;
        if (rank.isSeries()) {
            StringJoiner joiner = new StringJoiner(" ");
            String winEmoji = this.loc.localizeToLocale(this.key("resp.promo.win"), l);
            String lossEmoji = this.loc.localizeToLocale(this.key("resp.promo.loss"), l);
            String pendingEmoji = this.loc.localizeToLocale(this.key("resp.promo.pending"), l);

            for (char c : rank.getSeriesProgress().toCharArray()) {
                switch (c) {
                    case 'W':
                        joiner.add(winEmoji);
                        break;
                    case 'L':
                        joiner.add(lossEmoji);
                        break;
                    case 'N':
                        joiner.add(pendingEmoji);
                        break;
                }
            }

            String seriesStr = joiner.toString();

            lpOrPromo = this.loc.localizeToLocale(this.key("resp.promo"), l,
                seriesStr, rank.getSeriesWin(), rank.getSeriesLoss(), nlcp);
        } else {
            lpOrPromo = this.loc.localizeToLocale(this.key("resp.lp"), l,
                rank.getLp(), nlcp);
        }

        String winLoss = this.loc.localizeToLocale(this.key("resp.winloss"), l,
            rank.getWins(), rank.getLosses(),
            (rank.getWins() * 100) / (rank.getWins() + rank.getLosses()), nlcp);


        return this.loc.localizeToLocale(this.key("resp.queue"), l,
            league, winLoss, lpOrPromo);
    }

    @NotNull
    private Consumer<Throwable> handleQueueException() {
        return e -> {
            throw new RuntimeException(e);
        };
    }

    @NotNull
    private CommandResult rejectMissingArgs(CommandContext context) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.no_args"), l, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, handleQueueException());
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectUnknownRegion(CommandContext context, String region) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.key("error.unk_region"), l, region, nlcp));
        builder.setColor(Color.RED);
        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, handleQueueException());
        return CommandResult.rejected();
    }

    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }

    public void setService(LoLService service) {
        this.service = service;
    }

    private String firstCase(String s) {
        char first = s.charAt(0);
        return Character.toUpperCase(first) + s.substring(1).toLowerCase();
    }
}
