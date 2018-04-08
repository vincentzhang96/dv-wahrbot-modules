package com.divinitor.discord.wahrbot.ext.dn.services;

public interface DnStatService {

    String STAT_FD_KEY = "fd";
    String STAT_FD_LINEAR_KEY = "fdlin";
    String STAT_CRIT_KEY = "crit";
    String STAT_CRITDMG_KEY = "critdmg";
    String STAT_DEFENSE_KEY = "def";

    default void init() {}

    default void shutdown() {}

    StatCalc getStatCalculator(String statKey);

    interface StatCalc {
        default long calculate(float percent, int level) {
            return this.calculate(percent, level, false);
        }

        long calculate(float percent, int level, boolean forceLinear);

        default float calculatePercent(long stat, int level) {
            return this.calculatePercent(stat, level, false);
        }

        float calculatePercent(long stat, int level, boolean forceLinear);

        float getBasePercent();
        float getCapPercent();

        long getCap(int level);
    }
}
