package com.divinitor.discord.wahrbot.ext.lol;

import com.divinitor.discord.wahrbot.core.config.dyn.DynConfigHandle;
import com.divinitor.discord.wahrbot.ext.lol.entity.SummonerLeagueRank;
import com.divinitor.discord.wahrbot.ext.lol.entity.SummonerSummary;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.mingweisamuel.zyra.RiotApi;
import com.mingweisamuel.zyra.enums.Region;
import com.mingweisamuel.zyra.league.LeaguePosition;
import com.mingweisamuel.zyra.league.MiniSeries;
import com.mingweisamuel.zyra.lolStaticData.Realm;
import com.mingweisamuel.zyra.summoner.Summoner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class LoLService {
    public static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final LoLModule module;
    private DynConfigHandle apiKey;
    private RiotApi api;
    private LoadingCache<Region, Realm> realms;

    @Inject
    public LoLService(LoLModule module) {
        this.module = module;
        this.realms = CacheBuilder.newBuilder()
            .expireAfterWrite(4, TimeUnit.HOURS)
            .build(new CacheLoader<Region, Realm>() {
                @Override
                public Realm load(Region key) {
                    return LoLService.this.api.staticData.getRealm(key);
                }
            });
    }

    public void init() {
        this.apiKey = this.module.getDynConfigStore().getStringHandle("ext.lol.apikey");
        this.checkInitApiClient();
    }

    private boolean checkInitApiClient() {
        if (this.api == null) {
            String key = this.apiKey.get();
            if (Strings.isNullOrEmpty(key)) {
                LOGGER.warn("Missing Riot API key. Please specify one at 'ext.lol.apikey'.");
                LOGGER.warn("Unable to init LoLService. Will re-attempt with the next API request.");
                return false;
            }

            this.api = RiotApi.builder(key)
                .setConcurrentInstances(2)
                .setConcurrentRequestsMax(4).build();

            this.getRealm(Region.NA);
        }

        return true;
    }

    private void checkInitApiClientThrowing() {
        if (!checkInitApiClient()) {
            throw new LoLServiceMissingApiKeyException();
        }
    }

    private Realm getRealm(Region region) {
        return this.realms.getUnchecked(region);
    }

    public String getSummonerIconUrl(int iconId, Region region) {
        Realm realm = this.getRealm(region);
        return String.format("%s/%s/img/profileicon/%d.png",
            realm.cdn, realm.n.get("profileicon"), iconId);
    }

    public SummonerSummary getSummonerSummary(String summonerName, Region region) {
        this.checkInitApiClientThrowing();
        if (Strings.isNullOrEmpty(summonerName)) {
            throw new IllegalArgumentException("Please specify a summoner name");
        }

        if (region == null) {
            region = Region.NA;
        }

        try {
            Summoner result = this.api.summoners.getBySummonerName(region, summonerName);

            SummonerSummary ret = new SummonerSummary();
            ret.setName(result.name);
            ret.setLevel(result.summonerLevel);
            ret.setId(result.id);
            ret.setProfileIconId(result.profileIconId);
            ret.setProfileIconUrl(this.getSummonerIconUrl(result.profileIconId, region));

            List<SummonerLeagueRank> ranks = new ArrayList<>();
            List<LeaguePosition> pos = this.api.leagues.getAllLeaguePositionsForSummoner(region, result.id);
            for (LeaguePosition po : pos) {
                SummonerLeagueRank rank = new SummonerLeagueRank();
                rank.setQueue(po.queueType);
                rank.setWins(po.wins);
                rank.setLosses(po.losses);
                rank.setLeagueName(po.leagueName);
                rank.setRank(po.rank);
                rank.setTier(po.tier);
                rank.setLp(po.leaguePoints);
                MiniSeries miniSeries = po.miniSeries;
                if (miniSeries != null) {
                    rank.setSeries(true);
                    rank.setSeriesTarget(miniSeries.target);
                    rank.setSeriesWin(miniSeries.wins);
                    rank.setSeriesLoss(miniSeries.losses);
                    rank.setSeriesProgress(miniSeries.progress);
                } else {
                    rank.setSeries(false);
                }

                ranks.add(rank);
            }

            Collections.sort(ranks);

            ret.setRanked(ranks);

            return ret;
        } catch (Exception e) {
            LOGGER.warn("Exception during API call", e);
            throw new LoLServiceApiException(e);
        }
    }
}
