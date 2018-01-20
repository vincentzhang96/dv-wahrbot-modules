package com.divinitor.discord.wahrbot.ext.lol.entity;

import lombok.Data;

import java.util.List;

@Data
public class SummonerSummary {
    private String name;
    private long level;
    private long id;
    private int profileIconId;
    private String profileIconUrl;
    private List<SummonerLeagueRank> ranked;

}
