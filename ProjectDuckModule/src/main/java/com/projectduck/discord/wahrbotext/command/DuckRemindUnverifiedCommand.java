package com.projectduck.discord.wahrbotext.command;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandConstraints;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.google.common.collect.Lists;
import com.projectduck.discord.wahrbotext.ProjectDuckModule;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class DuckRemindUnverifiedCommand extends BasicMemoryCommand {
    public DuckRemindUnverifiedCommand() {
        super("remind", "");
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        Guild server = context.getServer();

        if (context.getCommandLine().hasNext()) {
            String id = context.getCommandLine().next();
            Member m = server.getMemberById(id);
            if (m == null) {
                m = server.retrieveMemberById(id).complete();
            }

            if (m == null) {
                context.getFeedbackChannel().sendMessage("User ID not found").queue();
                return CommandResult.ok();
            }

            sendReminder(Collections.singletonList(m));
            context.getFeedbackChannel().sendMessage("Sent").queue();
            return CommandResult.ok();
        }


        List<Member> roleless = server.loadMembers().get().stream()
                .filter((m) -> m.getRoles().isEmpty() && !m.getUser().isBot())
                .collect(Collectors.toList());

        Instant cutoff = Instant.now();//.plus(-7, ChronoUnit.DAYS);
        Instant cutoffB = Instant.now().plus(-30, ChronoUnit.DAYS);
        List<Member> rolelessOld = roleless.stream().filter((m) -> {
            OffsetDateTime timeJoined;
            if (!m.hasTimeJoined()) {
                Member data = server.retrieveMemberById(m.getId()).complete();
                timeJoined = data.getTimeJoined();
            } else {
                timeJoined = m.getTimeJoined();
            }

            Instant instant = timeJoined.toInstant();
            return instant.isBefore(cutoff) && instant.isAfter(cutoffB);
        }).collect(Collectors.toList());

        context.getFeedbackChannel().sendMessage(String.format("There are %s roleless members 30 days", rolelessOld.size())).queue();

        sendReminder(rolelessOld);

        return CommandResult.ok();
    }

    private void sendReminder(List<Member> rolelessOld) {
        for (Member member : rolelessOld) {
            member.getUser().openPrivateChannel()
                    .flatMap((c) -> {
                        return c.sendMessage("Hi! It's been several days since you joined Project Duck, " +
                                "but you have not verified as a member. Please verify as a member by going to this channel " +
                                "https://discord.com/channels/544827049752264704/881666897567969390 and adding the required reaction\n\n" +
                                "If you do not verify soon, you will be automatically removed from the Discord");
                    }).queue();
            ProjectDuckModule.LOGGER.info("Sent reminder to {}", member.getUser().getAsTag());
        }
    }

    @Override
    public String getKey() {
        return ProjectDuckModule.MODULE_KEY + ".command.remind";
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return CommandConstraints.hasAny(Permission.ADMINISTRATOR);
    }
}
