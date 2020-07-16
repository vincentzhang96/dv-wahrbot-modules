package com.divinitor.discord.wahrbot.ext.fun.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.*;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.i18n.LocalizerBundle;
import com.divinitor.discord.wahrbot.core.i18n.ResourceBundleBundle;
import com.divinitor.discord.wahrbot.core.util.RateLimiter;
import com.divinitor.discord.wahrbot.core.util.discord.MemberResolution;
import com.divinitor.discord.wahrbot.ext.fun.FunModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import lombok.Setter;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.SelfUser;
import net.dv8tion.jda.core.entities.User;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class StabCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "stab";
    public static final String KEY = FunModule.MODULE_KEY + ".commands." + COMMAND_ID;
    public static final String STORAGE_KEY_ALT_RATE = "ext.fun.stab.altrate";

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

        String msg = this.stab(context, invokerMember, targetMember);

        context.getFeedbackChannel().sendMessage(msg).queue();

        return CommandResult.ok();
    }

    private String stab(CommandContext context, Member source, Member target) {
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
            return localizer.localizeToLocale(
                key("resp.self"),
                locale,
                selfName,
                nlcp);
        } else if (this.isSudo(target)) {
            if (this.isSudo(source)) {
                // Admin stabbing self -> bot stabs itself
                return localizer.localizeToLocale(
                    key("resp.self"),
                    locale,
                    selfName,
                    nlcp);
            } else {
                // Person stabbing admin -> bot stabs source
                return localizer.localizeToLocale(
                    key("resp.third_person"),
                    locale,
                    selfName,
                    sourceName,
                    nlcp);
            }
        } else {
            // Regular or alt stab
            float alternateStabRate = getAlternateStabRate(context, source, target);
            float roll = this.random.nextFloat();
            String key = key("resp.default");
            if (roll <= alternateStabRate) {
                key = getAlternateStabKey(context);
            }

            return localizer.localizeToLocale(
                key,
                locale,
                sourceName,
                targetName,
                nlcp);
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
            .filter((k) -> k.startsWith("ext.fun.commands.stab.resp.alt."))
            .collect(Collectors.toList());

        return keys.get(this.random.nextInt(keys.size()));
    }

    private boolean checkRateLimit(Member member, Channel channel) {
        if (!this.isSudo(member)) {
            RateLimiter limiter = this.rateLimiters.getUnchecked(member.getUser().getId() + "." + channel.getId());
            if (limiter.tryMark() > 0) {
                return true;
            }
        }

        return false;
    }

    private static String escape(String text) {
        return text
            .replace("@everyone", "@ everyone")
            .replace("@here", "@ here");
    }

    /**
     * Register this command to the given registry and localizer
     * @param commandRegistry The registry to add this command to
     * @param localizer The localizer to add this command to
     */
    public void register(CommandRegistry commandRegistry, Localizer localizer) {
        this.localizerBundle = new ResourceBundleBundle(this.getResourcePath(),
            this.getClass().getClassLoader());
        localizer.registerBundle(this.key(),
            this.localizerBundle);
        commandRegistry.registerCommand(this, this.key());
    }
}
