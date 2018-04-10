package com.divinitor.discord.wahrbot.ext.dn.commands;

import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandRegistry;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.ext.dn.DnModule;
import com.divinitor.discord.wahrbot.ext.dn.services.DnStatService;
import com.divinitor.discord.wahrbot.ext.dn.util.QueueExceptionHandler;
import com.google.inject.Inject;
import net.dv8tion.jda.core.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Consumer;

public class StatCommand implements DnCommand {

    public static final String KEY_BASE = DnModule.MODULE_KEY + ".commands.stat";
    public static final int[] DEFAULT_LEVELS = new int[] { 95, 93 };

    private final DnStatService statService;
    private final Localizer loc;
    private final String statKey;
    private final DnStatService.StatCalc calc;
    private final QueueExceptionHandler exceptionHandler;

    private static final int LEVEL_SUMMARY = -1;

    public StatCommand(DnStatService statService, Localizer loc, String statKey, QueueExceptionHandler exceptionHandler) {
        this.statService = statService;
        this.loc = loc;
        this.statKey = statKey;
        this.exceptionHandler = exceptionHandler;

        this.calc = this.statService.getStatCalculator(this.statKey);
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        CommandLine line = context.getCommandLine();
        if (!line.hasNext()) {
            return this.rejectMissingArgs(context);
        }

        String next = line.next();

        int level = LEVEL_SUMMARY;
        String lvl = line.peek();
        if (!lvl.isEmpty()) {
            try {
                level = Integer.parseInt(lvl);
            } catch (Exception e) {
                return this.rejectNotANumber(context);
            }
        }

        float percent = -1;
        long stat = -1;
        boolean isPercent = this.isPercentage(next);
        boolean cap = false;

        float basePercent = this.calc.getBasePercent();
        float capPercent = this.calc.getCapPercent();
        if (next.equalsIgnoreCase(this.loc.localizeToLocale("ext.dn.commands.stat.base.cap", locale, nlcp))) {
            percent = capPercent;
            cap = true;
            isPercent = true;
        }

        try {
            if (percent < 0F) {
                if (isPercent) {
                    percent = this.parsePercentStat(next);

                    //  Cap it
                    if (percent < basePercent) {
                        percent = basePercent;
                    } else if (percent > capPercent) {
                        percent = capPercent;
                    }
                } else {
                    stat = this.parseNumberStat(next, this.loc, locale);
                    if (stat < 0) {
                        //  TODO base error.under_range.number

                        return CommandResult.error();
                    }
                }
            }
        } catch (NumberFormatException nfe) {
            return this.rejectNotANumber(context);
        }

        EmbedBuilder builder = new EmbedBuilder();
        String statName = this.loc.localizeToLocale(this.key("name"), locale, nlcp);
        String titleUrl = this.loc.localizeToLocale(this.baseKey("minerva_url"), locale, nlcp);

        if (level != LEVEL_SUMMARY) {
            if (cap) {
                stat = (long) (this.calc.getCap(level) * (capPercent - basePercent));
            } else {
                if (isPercent) {
                    stat = this.calc.calculate(percent, level);
                } else {
                    percent = this.calc.calculatePercent(stat, level);
                }
            }

            String percentStr = this.loc.localizeToLocale(this.baseKey("stat.percent"), locale,
                percent * 100F, nlcp);
            String statStr = this.loc.localizeToLocale(this.baseKey("stat.number"), locale,
                stat, nlcp);

            String titleParam;
            String valueParam;
            if (isPercent) {
                titleParam = percentStr;
                valueParam = statStr;
            } else {
                titleParam = statStr;
                valueParam = percentStr;
            }

            String title = this.loc.localizeToLocale(this.baseKey("title"), locale,
                level, titleParam, statName, nlcp);
            builder.setTitle(title, titleUrl);

            builder.setDescription(valueParam);
        } else {
            if (cap) {
                String percentStr = this.loc.localizeToLocale(this.baseKey("stat.percent"), locale,
                    percent * 100F, nlcp);

                for (int defLvl : DEFAULT_LEVELS) {
                    String levelTitle = this.loc.localizeToLocale(this.baseKey("level"), locale, defLvl, nlcp);
                    long capForLevel = this.calc.getCap(defLvl);
                    stat = (long) (capForLevel * (percent - basePercent));
                    String statStr = this.loc.localizeToLocale(this.baseKey("stat.number"), locale,
                        stat, nlcp);
                    builder.addField(levelTitle, statStr, true);
                }

                String title = this.loc.localizeToLocale(this.baseKey("title.summary"), locale,
                    percentStr, statName, nlcp);
                builder.setTitle(title, titleUrl);
            } else {
                if (isPercent) {
                    String percentStr = this.loc.localizeToLocale(this.baseKey("stat.percent"), locale,
                        percent * 100F, nlcp);

                    for (int defLvl : DEFAULT_LEVELS) {
                        String levelTitle = this.loc.localizeToLocale(this.baseKey("level"), locale, defLvl, nlcp);
                        stat = this.calc.calculate(percent, defLvl);
                        String statStr = this.loc.localizeToLocale(this.baseKey("stat.number"), locale,
                            stat, nlcp);
                        builder.addField(levelTitle, statStr, true);
                    }

                    String title = this.loc.localizeToLocale(this.baseKey("title.summary"), locale,
                        percentStr, statName, nlcp);
                    builder.setTitle(title, titleUrl);
                } else {
                    String statStr = this.loc.localizeToLocale(this.baseKey("stat.number"), locale,
                        stat, nlcp);

                    for (int defLvl : DEFAULT_LEVELS) {
                        String levelTitle = this.loc.localizeToLocale(this.baseKey("level"), locale, defLvl, nlcp);
                        percent = this.calc.calculatePercent(stat, defLvl);
                        String percentStr = this.loc.localizeToLocale(this.baseKey("stat.percent"), locale,
                            percent * 100F, nlcp);
                        builder.addField(levelTitle, percentStr, true);
                    }

                    String title = this.loc.localizeToLocale(this.baseKey("title.summary"), locale,
                        statStr, statName, nlcp);
                    builder.setTitle(title, titleUrl);
                }
            }
        }

        builder.setColor(5941733);
        builder.setFooter(this.loc.localizeToLocale(this.baseKey("footer"), locale, nlcp),
            this.loc.localizeToLocale(this.baseKey("footer.url"), locale, nlcp));

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, this.handleQueueException("result"));

        return CommandResult.ok();
    }

    private boolean isPercentage(String s) {
        return s.trim().endsWith("%");
    }

    private long parseNumberStat(String s, Localizer loc, Locale locale) throws NumberFormatException {
        s = s.replace(",", "");
        //  Check if it ends in thousand or million
        String thousandSuffix = loc.localizeToLocale(this.baseKey("suffix.thousand"), locale);
        int start = s.indexOf(thousandSuffix);
        double working;
        long ret = 0;
        if (start == 0) {
            //  Invalid, cannot be just "k"
            throw new NumberFormatException();
        }
        if (start != -1) {
            String num = s.substring(0, start);
            try {
                working = Double.parseDouble(num);
            } catch (NumberFormatException nfe) {
                throw new NumberFormatException();
            }
            ret = (long) (working * 1000.0);
        } else {
            String millionSuffix = loc.localizeToLocale(this.baseKey("suffix.million"), locale);
            start = s.indexOf(millionSuffix);
            if (start == 0) {
                //  Invalid, cannot be just "m"
                throw new NumberFormatException();
            }
            if (start != -1) {
                String num = s.substring(0, start);
                try {
                    working = Double.parseDouble(num);
                } catch (NumberFormatException nfe) {
                    throw new NumberFormatException();
                }
                ret = (long) (working * 1000000.0);
            } else {
                String billionSuffix = loc.localizeToLocale(this.baseKey("suffix.billion"), locale);
                start = s.indexOf(billionSuffix);
                if (start == 0) {
                    //  Invalid, cannot be just "b"
                    throw new NumberFormatException();
                }
                if (start != -1) {
                    String num = s.substring(0, start);
                    try {
                        working = Double.parseDouble(num);
                    } catch (NumberFormatException nfe) {
                        throw new NumberFormatException();
                    }
                    ret = (long) (working * 1000000000.0);
                } else {
                    String trillionSuffix = loc.localizeToLocale(this.baseKey("suffix.trillion"), locale);
                    start = s.indexOf(trillionSuffix);
                    if (start == 0) {
                        //  Invalid, cannot be just "t"
                        throw new NumberFormatException();
                    }
                    if (start != -1) {
                        String num = s.substring(0, start);
                        try {
                            working = Double.parseDouble(num);
                        } catch (NumberFormatException nfe) {
                            throw new NumberFormatException();
                        }
                        ret = (long) (working * 1000000000000.0);
                    } else {
                        ret = Long.parseLong(s);
                    }
                }
            }
        }

        return ret;
    }

    private float parsePercentStat(String s) throws NumberFormatException {
        if (s.endsWith("%")) {
            s = s.substring(0, s.length() - 1);
        }

        return Float.parseFloat(s) / 100F;
    }

    @Override
    public String key(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY_BASE).add(this.statKey);
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }


    private String baseKey(String... children) {
        StringJoiner joiner = new StringJoiner(".");
        joiner.add(KEY_BASE).add("base");
        for (String child : children) {
            joiner.add(child);
        }

        return joiner.toString();
    }

    @Override
    public void register(Localizer localizer, CommandRegistry commandRegistry) {
        localizer.registerBundle(this.key(),
            new ResourceBundleBundle(DnModule.BASE_MODULE_PATH + ".stat." + this.statKey,
                this.getClass().getClassLoader()));
        commandRegistry.registerCommand(this, this.key());
    }

    @Override
    public void unregister(Localizer localizer, CommandRegistry commandRegistry) {
        localizer.unregisterBundle(this.key());
        commandRegistry.unregisterCommand(this.key());
    }

    @NotNull
    private CommandResult rejectMissingArgs(CommandContext context) {
        return this.basicReject(context, "error.no_args", "no_args");
    }

    @NotNull
    private CommandResult rejectNotANumber(CommandContext context) {
        return this.basicReject(context, "error.not_a_number", "not_a_number");
    }

    @NotNull
    private CommandResult basicReject(CommandContext context, String baseDescKey, String exceptionKey) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(this.loc.localizeToLocale(this.baseKey("error.title"), l, nlcp));
        builder.setDescription(this.loc.localizeToLocale(this.baseKey(baseDescKey), l, nlcp));
        builder.setColor(Color.RED);

        if (this.loc.contains(this.key("remark"))) {
            builder.addField(this.loc.localizeToLocale(this.key("remark"), l, nlcp),
                this.loc.localizeToLocale(this.key("remark.body"), l, nlcp), false);
        }

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, this.handleQueueException(exceptionKey));
        return CommandResult.rejected();
    }

    private Consumer<Throwable> handleQueueException(String id) {
        return QueueExceptionHandler.handler(this, id, this.exceptionHandler);
    }

    public static class StatCommandFactory {
        private final DnStatService statService;
        private final Localizer loc;
        private final QueueExceptionHandler exceptionHandler;

        @Inject
        public StatCommandFactory(DnStatService statService, Localizer loc, QueueExceptionHandler exceptionHandler) {
            this.statService = statService;
            this.loc = loc;
            this.exceptionHandler = exceptionHandler;
        }

        public StatCommand create(String statKey) {
            return new StatCommand(this.statService, this.loc, statKey, this.exceptionHandler);
        }
    }
}
