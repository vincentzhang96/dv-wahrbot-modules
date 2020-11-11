package com.divinitor.discord.wahrbot.ext.fun.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerBundle;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.store.MemberStore;
import com.divinitor.discord.wahrbot.core.util.RateLimiter;
import com.divinitor.discord.wahrbot.core.util.discord.MemberResolution;
import com.divinitor.discord.wahrbot.ext.fun.FunModule;
import com.google.common.base.Strings;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import lombok.Setter;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.SelfUser;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StabCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "stab";
    public static final String KEY = FunModule.MODULE_KEY + ".commands." + COMMAND_ID;
    public static final String STORAGE_KEY_ALT_RATE = "ext.fun.stab.altrate";
    public static final String STORAGE_KEY_MEMBER_HP = "ext.fun.stab.member.hp";
    public static final String STORAGE_KEY_MEMBER_LAST_REGEN_TIME = "ext.fun.stab.member.regen.last";
    public static final String STORAGE_KEY_MEMBER_LAST_KILLED_TIME = "ext.fun.stab.member.killed.last";
    public static final int DEFAULT_HP = 20;
    public static final int REGEN_RATE_PER_MINUTE = 2;
    public static final int RESPAWN_TIME_MINUTES = 5;

    private final WahrBot bot;

    private final LoadingCache<String, RateLimiter> rateLimiters;

    private final Random random;

    @Setter
    private LocalizerBundle localizerBundle;

    @Inject
    public StabCommand(WahrBot bot) {
        this.bot = bot;
        this.rateLimiters = CacheBuilder.newBuilder()
                .expireAfterAccess(5, TimeUnit.MINUTES)
                .build(new CacheLoader<String, RateLimiter>() {
                    @Override
                    public RateLimiter load(String key) throws Exception {
                        return new RateLimiter(key, TimeUnit.SECONDS.toMillis(15), 3);
                    }
                });
        this.random = new Random();
    }

    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected String getResourcePath() {
        return FunModule.BASE_MODULE_PATH + "." + COMMAND_ID;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();

        User invoker = context.getInvoker();
        Member invokerMember = context.getServer().getMember(invoker);
        if (invokerMember == null) {
            return CommandResult.error();
        }

        if (checkRateLimit(invokerMember, context.getFeedbackChannel())) {
            return CommandResult.rejected();
        }

        Member targetMember;
        if (line.hasNext()) {
            String targetName = line.remainder();
            targetMember = MemberResolution.findMember(targetName, context);
        } else {
            targetMember = invokerMember;
        }

        if (targetMember == null) {
            return CommandResult.error();
        }

        this.stab(context, invokerMember, targetMember);


        return CommandResult.ok();
    }

    private void stab(CommandContext context, Member source, Member target) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Localizer localizer = context.getLocalizer();
        SelfUser botUser = context.getApi().getSelfUser();
        Member botMember = context.getServer().getMember(botUser);
        String selfName = escape(botMember.getEffectiveName());
        String sourceName = escape(source.getEffectiveName());
        String targetName = escape(target.getEffectiveName());

        if (target.getUser().getIdLong() == botUser.getIdLong()) {
            // Stab self
            String msg = localizer.localizeToLocale(
                    key("resp.self"),
                    locale,
                    selfName,
                    nlcp);
            context.getFeedbackChannel().sendMessage(msg).queue();
            return;
        } else if (this.isSudo(target)) {
            if (this.isSudo(source)) {
                // Admin stabbing self -> bot stabs itself
                String msg = localizer.localizeToLocale(
                        key("resp.self"),
                        locale,
                        selfName,
                        nlcp);

                context.getFeedbackChannel().sendMessage(msg).queue();
                return;
            } else {
                // Person stabbing admin -> bot stabs source
                String msg = localizer.localizeToLocale(
                        key("resp.third_person"),
                        locale,
                        selfName,
                        sourceName,
                        nlcp);

                context.getFeedbackChannel().sendMessage(msg).queue();
                return;
            }
        } else {
            this.simTick(source, target);

            if (this.getMemberHp(source) <= 0 && !this.isSudo(source)) {
                // Dead people can't stab
                String msg = localizer.localizeToLocale(
                        key("member.are_dead"),
                        locale,
                        nlcp);

                context.getFeedbackChannel().sendMessage(msg).queue();
                return;
            }

            // Regular or alt stab
            float alternateStabRate = getAlternateStabRate(context, source, target);
            float roll = this.random.nextFloat();
            String key = key("resp.default");
            if (roll <= alternateStabRate) {
                key = getAlternateStabKey(context);
            }

            String msg = localizer.localizeToLocale(
                    key,
                    locale,
                    sourceName,
                    targetName,
                    nlcp);
            context.getFeedbackChannel().sendMessage(msg).queue();

            applyHpModAmount(key, source, target, context);
        }
    }


    private boolean isSudo(Member member) {
        return this.bot.getUserStorage().forUser(member.getUser()).getBoolean("sudo", false);
    }

    private float getAlternateStabRate(CommandContext context, Member invoker, Member target) {
        double[] rates = new double[4];
        rates[0] = this.bot.getDynConfigStore()
                .getDouble(STORAGE_KEY_ALT_RATE, 0.05D);

        //  Check if this server has a different rate
        rates[1] = this.bot.getServerStorage().forServer(context.getServer())
                .getDouble(STORAGE_KEY_ALT_RATE, 0D);

        //  Check if the target user has a different rate
        rates[2] = this.bot.getUserStorage().forUser(target.getUser())
                .getDouble(STORAGE_KEY_ALT_RATE, 0D);

        //  Check if the invoking user has a different rate
        rates[3] = this.bot.getUserStorage().forUser(invoker.getUser())
                .getDouble(STORAGE_KEY_ALT_RATE, 0D);

        return (float) Arrays.stream(rates).max().orElse(0);
    }

    private String getAlternateStabKey(CommandContext context) {
        List<String> keys = this.localizerBundle.keys(context.getLocale())
                .filter((k) -> k.startsWith("ext.fun.commands.stab.resp.alt.") && !k.endsWith(".hp"))
                .collect(Collectors.toList());

        return keys.get(this.random.nextInt(keys.size()));
    }

    private boolean checkRateLimit(Member member, TextChannel channel) {
        if (!this.isSudo(member)) {
            RateLimiter limiter = this.rateLimiters.getUnchecked(member.getUser().getId() + "." + channel.getId());
            if (limiter.tryMark() > 0) {
                return true;
            }
        }

        return false;
    }

    private static String escape(String text) {
        String sanitized = text
                .replace("@", "@ ")
                .replace("{", "\\{")
                .replace("[", "\\[");
        return sanitized;
    }

    /**
     * Register this command to the given registry and localizer
     *
     * @param commandRegistry The registry to add this command to
     * @param localizer       The localizer to add this command to
     */
    public void register(CommandRegistry commandRegistry, Localizer localizer) {
        this.localizerBundle = new ResourceBundleBundle(this.getResourcePath(),
                this.getClass().getClassLoader());
        localizer.registerBundle(this.key(),
                this.localizerBundle);
        commandRegistry.registerCommand(this, this.key());
    }

    private MemberStore getMemberStore(Member member) {
        return this.bot.getServerStorage().forServer(member.getGuild()).forMember(member);
    }

    private int getMemberHp(Member member) {
        return getMemberStore(member).getInt(STORAGE_KEY_MEMBER_HP, DEFAULT_HP);
    }

    private void setMemberHp(Member member, int hp) {
        getMemberStore(member).put(STORAGE_KEY_MEMBER_HP, hp);
    }

    private void applyRespawn(Member member) {
        int hp = this.getMemberHp(member);
        if (hp <= 0) {
            // Player is dead, check when they died
            MemberStore store = getMemberStore(member);
            long lastKilledTimeMillis = store.getLong(STORAGE_KEY_MEMBER_LAST_KILLED_TIME, 0);
            if (lastKilledTimeMillis == 0) {
                return;
            }

            Instant lastKilledTime = Instant.ofEpochMilli(lastKilledTimeMillis);
            Instant now = Instant.now();
            long minutes = Duration.between(now, lastKilledTime).abs().toMinutes();
            if (minutes >= RESPAWN_TIME_MINUTES) {
                // Revive the player
                setMemberHp(member, DEFAULT_HP);
                store.put(STORAGE_KEY_MEMBER_LAST_KILLED_TIME, 0);
                store.put(STORAGE_KEY_MEMBER_LAST_REGEN_TIME, now.toEpochMilli());
            }
        }
    }

    private void applyRegen(Member member) {
        int hp = this.getMemberHp(member);
        if (hp < DEFAULT_HP && hp > 0) {
            // Player needs regen, check when they last had regen applied
            MemberStore store = getMemberStore(member);
            long lastRegenTimeMillis = store.getLong(STORAGE_KEY_MEMBER_LAST_REGEN_TIME, 0);
            Instant now = Instant.now();
            if (lastRegenTimeMillis == 0) {
                lastRegenTimeMillis = now.toEpochMilli();
            }

            Instant lastRegenTime = Instant.ofEpochMilli(lastRegenTimeMillis);
            long minutes = Duration.between(now, lastRegenTime).abs().toMinutes();
            if (minutes > 0) {
                int hpToRestore = (int) (minutes * REGEN_RATE_PER_MINUTE);
                this.setMemberHp(member, Math.max(DEFAULT_HP, hp + hpToRestore));
                store.put(STORAGE_KEY_MEMBER_LAST_REGEN_TIME, now.toEpochMilli());
            }
        }
    }

    private void simTick(Member stabber, Member stabbee) {
        this.applyRespawn(stabber);
        this.applyRespawn(stabbee);

        this.applyRegen(stabber);
        this.applyRegen(stabbee);
    }

    private void reportHp(Member stabber, int stabberHp, int stabberDelta, Member stabbee, int stabbeeHp, int stabbeeDelta, CommandContext context) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Localizer localizer = context.getLocalizer();

        boolean stabberChanged = stabberDelta != 0;
        boolean stabbeeChanged = stabbeeDelta != 0;
        boolean stabberDied = stabberHp <= 0;
        boolean stabbeeDied = stabbeeHp <= 0;

        String stabberName = escape(stabber.getEffectiveName());
        String stabbeeName = escape(stabbee.getEffectiveName());

        StringJoiner joiner = new StringJoiner("\n");

        joiner.add(
                localizer.localizeToLocale(
                        key(stabberChanged ? "member.hp.change" : "member.hp.no_change"),
                        locale,
                        stabberName,
                        -stabberDelta,
                        stabberHp,
                        DEFAULT_HP,
                        nlcp
                )
        );
        joiner.add(
                localizer.localizeToLocale(
                        key(stabbeeChanged ? "member.hp.change" : "member.hp.no_change"),
                        locale,
                        stabbeeName,
                        -stabbeeDelta,
                        stabbeeHp,
                        DEFAULT_HP,
                        nlcp
                )
        );

        if (stabberDied && stabberDelta != 0) {
            joiner.add(
                    localizer.localizeToLocale(
                            key("member.died"),
                            locale,
                            stabberName,
                            RESPAWN_TIME_MINUTES,
                            nlcp
                    )
            );
        }
        if (stabbeeDied && stabbeeDelta != 0) {
            joiner.add(
                    localizer.localizeToLocale(
                            key("member.died"),
                            locale,
                            stabbeeName,
                            RESPAWN_TIME_MINUTES,
                            nlcp
                    )
            );
        }

        context.getFeedbackChannel().sendMessage(joiner.toString()).queue();
    }

    private void onDie(Member member) {
        MemberStore store = getMemberStore(member);
        Instant now = Instant.now();
        if (store.getLong(STORAGE_KEY_MEMBER_LAST_KILLED_TIME, 0) == 0) {
            store.put(STORAGE_KEY_MEMBER_LAST_KILLED_TIME, now.toEpochMilli());
            store.put(STORAGE_KEY_MEMBER_LAST_REGEN_TIME, now.toEpochMilli());
        }
    }

    private void applyHpModAmount(String key, Member stabber, Member stabbee, CommandContext context) {
        if (stabber.getUser().getIdLong() == stabbee.getUser().getIdLong()) {
            return;
        }

        String hpKey = key + ".hp";
        String hpParams = this.localizerBundle.get(hpKey, context.getLocale());
        if (Strings.isNullOrEmpty(hpParams)) {
            return;
        }

        String[] split = hpParams.split(";");
        if (split.length != 2) {
            return;
        }

        int stabberHp = getMemberHp(stabber);
        int stabbeeHp = getMemberHp(stabbee);

        int stabberModHp = stabberHp <= 0 ? 0 : getHpModAmount(split[0], stabber);
        int stabbeeModHp = stabbeeHp <= 0 ? 0 : getHpModAmount(split[1], stabbee);

        int newStabberHp = Math.min(DEFAULT_HP, stabberHp - stabberModHp);
        int newStabbeeHp = Math.min(DEFAULT_HP, stabbeeHp - stabbeeModHp);

        this.setMemberHp(stabber, newStabberHp);
        this.setMemberHp(stabbee, newStabbeeHp);

        this.reportHp(stabber, newStabberHp, stabberModHp, stabbee, newStabbeeHp, stabbeeModHp, context);

        if (newStabberHp <= 0) {
            this.onDie(stabber);
        }

        if (newStabbeeHp <= 0) {
            this.onDie(stabbee);
        }
    }

    private int getHpModAmount(String param, Member target) {
        try {
            if (param.startsWith("s")) {
                // Special effect
                String[] split = param.split(",");
                if (split.length < 2) {
                    return 0;
                }

                int effectId = Integer.parseInt(split[1]);

                switch (effectId) {
                    case 159: {
                        // COUP
                        if (split.length >= 5) {
                            int hp = this.getMemberHp(target);
                            int baseDamage = Integer.parseInt(split[2]);
                            float threshold = Float.parseFloat(split[3]);
                            float power = Float.parseFloat(split[4]);
                            int damage = baseDamage;

                            float hpPercent = (float) (hp - baseDamage) / DEFAULT_HP;
                            if (hpPercent <= threshold && hpPercent >= 0) {
                                float diff = threshold - hpPercent;
                                damage += baseDamage * (diff * power * 100);
                            }

                            return damage;
                        }

                        break;
                    }
                }
            } else {
                return Integer.parseInt(param);
            }
        } catch (NumberFormatException ignored) {
        }

        return 0;
    }
}
