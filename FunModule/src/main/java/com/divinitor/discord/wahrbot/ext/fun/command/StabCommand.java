package com.divinitor.discord.wahrbot.ext.fun.command;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.command.AbstractKeyedCommand;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.util.RateLimiter;
import com.divinitor.discord.wahrbot.core.util.discord.MemberResolution;
import com.divinitor.discord.wahrbot.ext.fun.FunModule;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import net.dv8tion.jda.core.entities.Channel;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class StabCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "labpoints";
    public static final String KEY = FunModule.MODULE_KEY + ".commands." + COMMAND_ID;
    public static final String STORAGE_KEY_ALT_RATE = "ext.fun.stab.altrate";

    private final WahrBot bot;

    private final LoadingCache<String, RateLimiter> rateLimiters;

    private final Random random;

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
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        CommandLine line = context.getCommandLine();

        User invoker = context.getInvoker();
        Member invokerMember = context.getServer().getMember(invoker);
        if (invokerMember == null) {
            return CommandResult.error();
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
        String message = "";
        if (this.isAdmin(target)) {
            if (this.isAdmin(source)) {

            }
        }



        return message;
    }


    private boolean isAdmin(Member member) {
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

    private String getAlternateStabKey() {


        return "";
    }

    private boolean checkRateLimit(Member member, Channel channel) {
        if (!this.isAdmin(member)) {
            RateLimiter limiter = rateLimiters.getUnchecked(member.getUser().getId() + "." + channel.getId());
            if (limiter.tryMark() > 0) {
                return true;
            }
        }
        return false;
    }
}
