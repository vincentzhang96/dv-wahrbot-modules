package com.divinitor.discord.wahrbot.ext.dn.services.impl;

import com.divinitor.discord.wahrbot.ext.dn.services.DnStatService;
import gnu.trove.impl.Constants;
import gnu.trove.map.TObjectFloatMap;
import gnu.trove.map.hash.TObjectFloatHashMap;

import java.util.HashMap;
import java.util.Map;

public class HardcodedDnStatService implements DnStatService {

    private Map<String, long[]> statCaps;

    private TObjectFloatMap<String> maxStatPercents;

    private TObjectFloatMap<String> baseStatPercents;

    private Map<String, Calc> calcs;

    public HardcodedDnStatService() {
        this.statCaps = new HashMap<>();
        this.maxStatPercents = new TObjectFloatHashMap<>();
        this.baseStatPercents = new TObjectFloatHashMap<>(Constants.DEFAULT_CAPACITY,
            Constants.DEFAULT_LOAD_FACTOR,
            0F);
        this.calcs = new HashMap<>();
    }

    @Override
    public void init() {
        //  CRIT
        this.statCaps.put(STAT_CRIT_KEY, new long[]{1L,
            1000L, 1160L, 1320L, 1480L, 1640L, 1800L, 1960L, 2120L,
            2280L, 2440L, 2600L, 2760L, 2920L, 3080L, 3800L, 4000L,
            4200L, 4400L, 4600L, 4800L, 5000L, 5200L, 5400L, 5600L,
            5900L, 6200L, 6500L, 6800L, 7100L, 7400L, 7700L, 8000L,
            8400L, 8800L, 9200L, 9600L, 10000L, 10400L, 10800L, 11200L,
            12000L, 12800L, 13600L, 14400L, 15300L, 16200L, 17100L,
            18000L, 19000L, 20000L, 21500L, 23000L, 24600L, 26200L,
            27900L, 29600L, 31400L, 33200L, 35200L, 37200L, 40200L,
            43200L, 46200L, 49200L, 52400L, 55600L, 58800L, 62000L,
            65400L, 68800L, 74745L, 80619L, 86438L, 92373L, 98284L,
            104245L, 110176L, 116008L, 121899L, 127685L, 138684L, 149565L,
            160545L, 171433L, 182263L, 192994L, 203931L, 214891L, 225855L,
            236880L, 277830L, 321300L, 367290L, 794640L, 937755L, 1599360L,
            1867320L, 2150400L, 2448600L, 2704380L
        });
        this.maxStatPercents.put(STAT_CRIT_KEY, 0.89F);
        this.calcs.put(STAT_CRIT_KEY, new Calc(STAT_CRIT_KEY));

        //  CRITDMG
        this.statCaps.put(STAT_CRITDMG_KEY, new long[]{1L,
            2650L, 3074L, 3498L, 3922L, 4346L, 4770L, 5194L, 5618L,
            6042L, 6466L, 6890L, 7314L, 7738L, 8162L, 10070L, 10600L,
            11130L, 11660L, 12190L, 12720L, 13250L, 13780L, 14310L,
            14840L, 15635L, 16430L, 17225L, 18020L, 18815L, 19610L,
            20405L, 21200L, 22260L, 23320L, 24380L, 25440L, 26500L,
            27560L, 28620L, 29680L, 31641L, 33575L, 35510L, 37206L,
            39326L, 41419L, 43513L, 45553L, 47832L, 50350L, 55650L,
            59757L, 64580L, 69589L, 74756L, 80109L, 86310L, 93121L,
            99375L, 103350L, 107987L, 113950L, 121237L, 129850L, 139787L,
            151050L, 163637L, 177894L, 193794L, 211337L, 228555L, 245520L,
            262220L, 278587L, 296902L, 320100L, 343263L, 374976L, 407979L,
            431970L, 453390L, 474810L, 496230L, 517650L, 542640L, 567630L,
            592620L, 617610L, 642600L, 671160L, 769692L, 801108L, 832524L,
            1021020L, 1348924L, 1486905L, 1631311L, 1782144L, 1986705L, 2249814L
        });
        this.maxStatPercents.put(STAT_CRITDMG_KEY, 3.0F);
        this.baseStatPercents.put(STAT_CRITDMG_KEY, 2.0F);
        this.calcs.put(STAT_CRITDMG_KEY, new Calc(STAT_CRITDMG_KEY));

        //  DEFENSE
        this.statCaps.put(STAT_DEFENSE_KEY, new long[]{1L,
            750L, 870L, 990L, 1110L, 1230L, 1350L, 1470L, 1590L,
            1710L, 1830L, 1950L, 2070L, 2190L, 2310L, 2850L, 3000L,
            3150L, 3300L, 3450L, 3600L, 3750L, 3900L, 4050L, 4200L,
            4425L, 4650L, 4875L, 5100L, 5325L, 5550L, 5775L, 6000L,
            6300L, 6600L, 6900L, 7200L, 7500L, 7800L, 8100L, 8400L,
            9000L, 9600L, 10200L, 10800L, 11475L, 12150L, 12825L,
            13500L, 14250L, 15000L, 15750L, 16912L, 18277L, 19695L,
            21157L, 22672L, 24427L, 26355L, 28125L, 29250L, 30868L,
            33056L, 35685L, 38771L, 42331L, 46383L, 50943L, 56137L,
            61977L, 68186L, 72523L, 76637L, 80534L, 84201L, 88328L,
            92188L, 95006L, 99837L, 104590L, 106722L, 112014L, 117306L,
            122598L, 127890L, 134064L, 140238L, 146412L, 152586L,
            158760L, 165816L, 187278L, 209916L, 233730L, 258720L,
            286135L, 314874L, 344935L, 376320L, 409027L, 443058L
        });
        this.maxStatPercents.put(STAT_DEFENSE_KEY, 0.85F);
        this.calcs.put(STAT_DEFENSE_KEY, new Calc(STAT_DEFENSE_KEY));

        //  FINAL DAMAGE
        this.statCaps.put(STAT_FD_KEY, new long[]{1L,
            75L, 87L, 99L, 111L, 123L, 135L, 147L, 159L, 171L, 183L,
            195L, 207L, 219L, 231L, 285L, 300L, 315L, 330L, 345L,
            360L, 375L, 390L, 405L, 420L, 442L, 465L, 487L, 510L,
            532L, 555L, 577L, 600L, 630L, 660L, 690L, 720L, 750L,
            780L, 810L, 850L, 894L, 938L, 982L, 1026L, 1070L, 1114L,
            1158L, 1202L, 1246L, 1290L, 1346L, 1402L, 1458L, 1514L,
            1570L, 1626L, 1682L, 1738L, 1794L, 1850L, 1962L, 2074L,
            2187L, 2299L, 2412L, 2524L, 2636L, 2749L, 2861L, 2974L,
            3128L, 3283L, 3437L, 3592L, 3747L, 3901L, 4056L, 4210L,
            4365L, 4520L, 4704L, 4889L, 5074L, 5258L, 5443L, 5628L,
            5812L, 5997L, 6182L, 6367L, 7022L, 7678L, 8333L, 11689L,
            14944L, 17461L, 19977L, 22494L, 25010L, 27527L
        });
        this.maxStatPercents.put(STAT_FD_KEY, 1.0F);
        this.calcs.put(STAT_FD_KEY, new Calc(STAT_FD_KEY, true));

        //  FINAL DAMAGE (LINEAR)
        this.statCaps.put(STAT_FD_LINEAR_KEY, new long[]{1L,
            75L, 87L, 99L, 111L, 123L, 135L, 147L, 159L, 171L, 183L,
            195L, 207L, 219L, 231L, 285L, 300L, 315L, 330L, 345L,
            360L, 375L, 390L, 405L, 420L, 442L, 465L, 487L, 510L,
            532L, 555L, 577L, 600L, 630L, 660L, 690L, 720L, 750L,
            780L, 810L, 850L, 894L, 938L, 982L, 1026L, 1070L, 1114L,
            1158L, 1202L, 1246L, 1290L, 1346L, 1402L, 1458L, 1514L,
            1570L, 1626L, 1682L, 1738L, 1794L, 1850L, 1962L, 2074L,
            2187L, 2299L, 2412L, 2524L, 2636L, 2749L, 2861L, 2974L,
            3128L, 3283L, 3437L, 3592L, 3747L, 3901L, 4056L, 4210L,
            4365L, 4520L, 4704L, 4889L, 5074L, 5258L, 5443L, 5628L,
            5812L, 5997L, 6182L, 6367L, 7022L, 7678L, 8333L, 11689L,
            14944L, 17461L, 19977L, 22494L, 25010L, 27527L
        });
        this.maxStatPercents.put(STAT_FD_LINEAR_KEY, 1.0F);
        this.calcs.put(STAT_FD_LINEAR_KEY, new Calc(STAT_FD_LINEAR_KEY));
    }

    private float getBaseStatPercent(String key) {
        //  No-entry value is 0
        return this.baseStatPercents.get(key);
    }

    private float clampStatPercent(String key, float value) {
        return Math.min(this.getMaxStatPercent(key), value);
    }

    private float getMaxStatPercent(String key) {
        return this.maxStatPercents.get(key);
    }

    @Override
    public StatCalc getStatCalculator(String statKey) {
        return this.calcs.get(statKey);
    }

    private class Calc implements StatCalc {
        static final float FD_POW = 2.2F;
        static final float FD_COEFF = 0.35F;
        static final float FD_BREAKING_POINT = 0.417F;
        static final float FD_INVERSE_POWER = 0.454545454545454545454545454545F;
        static final float FD_INVERSE_COEFF = 2.85714285714285714286F;
        static final float FD_INVERSE_BREAKING_POINT = 0.146F;

        private final String key;
        private final boolean useFdCurve;

        private Calc(String key) {
            this(key, false);
        }

        private Calc(String key, boolean useFdCurve) {
            this.key = key;
            this.useFdCurve = useFdCurve;
        }

        @Override
        public long calculate(float percent, int level, boolean forceLinear) {
            boolean actuallyUseFdCurve = this.useFdCurve && !forceLinear;
            long stat;
            float base = this.getBasePercent();
            float capPercent = this.getCapPercent();
            percent = Math.max(base, Math.min(capPercent, percent));
            percent -= base;
            if (actuallyUseFdCurve) {
                if (percent < FD_INVERSE_BREAKING_POINT) {
                    stat = (long) (FD_INVERSE_COEFF * percent * this.getCap(level));
                } else {
                    stat = (long) (this.getCap(level) * Math.pow(percent, FD_INVERSE_POWER));
                }
            } else {
                stat = Math.round((double) percent * this.getCap(level));
            }

            return stat;
        }

        @Override
        public float calculatePercent(long stat, int level, boolean forceLinear) {
            boolean actuallyUseFdCurve = this.useFdCurve && !forceLinear;
            float percent;
            if (actuallyUseFdCurve) {
                float cap = this.getCap(level);
                float ratio = ((float) stat) / cap;
                if (ratio < FD_BREAKING_POINT) {
                    percent = (FD_COEFF * stat) / cap;
                } else {
                    percent = (float) Math.pow(ratio, FD_POW);
                }
            } else {
                float cap = this.getCap(level);
                percent = (float) stat / cap;
            }

            float base = this.getBasePercent();
            float capPercent = this.getCapPercent();
            return Math.max(base, Math.min(capPercent, base + percent));
        }

        @Override
        public float getBasePercent() {
            return HardcodedDnStatService.this.getBaseStatPercent(this.key);
        }

        @Override
        public float getCapPercent() {
            return HardcodedDnStatService.this.getMaxStatPercent(this.key);
        }

        @Override
        public long getCap(int level) {
            long[] caps = HardcodedDnStatService.this.statCaps.get(this.key);
            if (caps == null) {
                return -1;
            }
            if (level < 0 || level >= caps.length) {
                return -1;
            }

            return caps[level];
        }
    }
}
