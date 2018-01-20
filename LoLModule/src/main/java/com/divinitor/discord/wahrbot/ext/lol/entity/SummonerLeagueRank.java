package com.divinitor.discord.wahrbot.ext.lol.entity;

import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class SummonerLeagueRank implements Comparable<SummonerLeagueRank> {

    private String queue;
    private int wins;
    private int losses;
    private String leagueName;
    private String rank;
    private String tier;
    private int lp;
    private boolean series;
    private String seriesProgress;
    private int seriesTarget;
    private int seriesWin;
    private int seriesLoss;

    @Override
    public int compareTo(@NotNull SummonerLeagueRank o) {
        return queue.compareTo(o.leagueName);
    }
}
