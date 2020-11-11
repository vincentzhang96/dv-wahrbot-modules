package com.divinitor.discord.wahrbot.ext.dn.commands;

import com.divinitor.discord.wahrbot.core.command.AbstractKeyedCommand;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.ext.dn.DnModule;
import com.divinitor.discord.wahrbot.ext.dn.util.QueueExceptionHandler;
import com.google.inject.Inject;
import net.dv8tion.jda.api.EmbedBuilder;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class LabPointsCommand extends AbstractKeyedCommand {
    public static final String COMMAND_ID = "labpoints";
    public static final String KEY = DnModule.MODULE_KEY + ".commands." + COMMAND_ID;

    private static final int[] LP = new int[]{7, 9, 11, 13, 14, 15, 16, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
        29, 31, 32, 34, 35, 37, 39, 41, 43, 45, 47, 50, 52, 55, 58, 61, 64, 67, 70, 74, 78, 82, 87, 91, 96, 101, 106,
        111, 117, 123, 129, 136};
    public static final int MAX_FLOOR = 50;
    public static final int MIN_FLOOR = 1;
    public static final int MIN_LP = 1;

    private final QueueExceptionHandler exceptionHandler;

    @Inject
    public LabPointsCommand(QueueExceptionHandler exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }


    @Override
    public String getKey() {
        return KEY;
    }

    @Override
    protected String getResourcePath() {
        return DnModule.BASE_MODULE_PATH + "." + COMMAND_ID;
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        CommandLine line = context.getCommandLine();
        if (!line.hasNext()) {
            return handleLpSummary(context);
        }

        String next = line.next().toLowerCase();

        if (next.contains("f") || next.contains("l")) {
            return handleFloorLookup(context, next);
        } else {
            return handlePointLookup(context, next);
        }
    }

    private CommandResult handlePointLookup(CommandContext context, String points) {
        Locale locale = context.getLocale();
        Localizer loc = context.getLocalizer();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        int lp = 0;
        try {
            lp = Integer.parseInt(points);
        } catch (NumberFormatException nfe) {
            return rejectNotANumber(context);
        }

        if (lp < MIN_LP) {
            return rejectLpRange(context);
        }

        int resultFloor = -1;
        int resultLp = 0;
        for (int floor = 1; floor <= LP.length; floor++) {
            int floorLp = LP[floor - 1];

            if (floorLp >= lp) {
                resultFloor = floor;
                resultLp = floorLp;
                break;
            }
        }

        if (resultFloor == -1) {
            return rejectNoFloor(context);
        }

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("resp.lp.title"), locale, lp, nlcp));
        builder.addField(loc.localizeToLocale(this.key("resp.field.floor.key"), locale, resultFloor, nlcp),
            loc.localizeToLocale(this.key("resp.field.floor.val"), locale, resultLp, nlcp), false);
        if (lp <= MAX_FLOOR) {
            builder.setFooter(loc.localizeToLocale(this.key("resp.lp.footer"), locale, lp, nlcp), null);
        }

        builder.setColor(0xefb237);

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, this.handleQueueException("result"));

        return CommandResult.ok();
    }

    private CommandResult handleFloorLookup(CommandContext context, String floorStr) {
        Locale locale = context.getLocale();
        Localizer loc = context.getLocalizer();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        floorStr = floorStr.replace("f", "").replace("l", "");
        int floor = 0;
        try {
            floor = Integer.parseInt(floorStr);
        } catch (NumberFormatException nfe) {
            return rejectNotANumber(context);
        }

        if (floor < MIN_FLOOR || floor > MAX_FLOOR) {
            return rejectFloorRange(context);
        }

        int floorIndex = floor - 1;
        int resultLp = LP[floorIndex];

        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("resp.floor.title"), locale, floor, nlcp));
        builder.setDescription(loc.localizeToLocale(this.key("resp.field.floor.val"), locale, resultLp, nlcp));
        builder.setFooter(loc.localizeToLocale(this.key("resp.floor.footer"), locale, floor, nlcp), null);
        builder.setColor(0xefb237);

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, this.handleQueueException("result"));

        return CommandResult.ok();
    }

    private CommandResult handleLpSummary(CommandContext context) {
        Locale locale = context.getLocale();
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        return CommandResult.ok();
    }

    @NotNull
    private CommandResult basicReject(CommandContext context, String errorKey, String exceptionKey) {
        Map<String, Object> nlcp = context.getNamedLocalizationContextParams();
        Locale l = context.getLocale();
        Localizer loc = context.getLocalizer();
        EmbedBuilder builder = new EmbedBuilder();
        builder.setTitle(loc.localizeToLocale(this.key("error.title"), l, nlcp));
        builder.setDescription(loc.localizeToLocale(this.key(errorKey), l, nlcp));
        builder.setColor(Color.RED);

        if (loc.contains(this.key("remark"))) {
            builder.addField(loc.localizeToLocale(this.key("remark"), l, nlcp),
                loc.localizeToLocale(this.key("remark.body"), l, nlcp), false);
        }

        context.getFeedbackChannel().sendMessage(builder.build())
            .queue(null, this.handleQueueException(exceptionKey));
        return CommandResult.rejected();
    }

    @NotNull
    private CommandResult rejectNotANumber(CommandContext context) {
        return this.basicReject(context, "error.not_a_number", "not_a_number");
    }

    @NotNull
    private CommandResult rejectLpRange(CommandContext context) {
        return this.basicReject(context, "error.lp_range", "lp_range");
    }

    @NotNull
    private CommandResult rejectFloorRange(CommandContext context) {
        return this.basicReject(context, "error.floor_range", "floor_range");
    }

    @NotNull
    private CommandResult rejectNoFloor(CommandContext context) {
        return this.basicReject(context, "error.no_floor", "no_floor");
    }

    private Consumer<Throwable> handleQueueException(String id) {
        return QueueExceptionHandler.handler(this, id, this.exceptionHandler);
    }
}
