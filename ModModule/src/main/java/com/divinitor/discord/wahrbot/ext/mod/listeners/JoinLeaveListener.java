package com.divinitor.discord.wahrbot.ext.mod.listeners;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.divinitor.discord.wahrbot.core.i18n.Localizer;
import com.divinitor.discord.wahrbot.core.store.ServerStorage;
import com.divinitor.discord.wahrbot.core.store.ServerStore;
import com.divinitor.discord.wahrbot.ext.mod.commands.JLMessageHelper;
import com.google.common.eventbus.Subscribe;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;

import java.util.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class JoinLeaveListener {

    private final ServerStorage serverStorage;
    private final Localizer loc;

    public JoinLeaveListener(WahrBot bot) {
        this.serverStorage = bot.getServerStorage();
        this.loc = bot.getLocalizer();
    }

    @Subscribe
    public void onMemberJoin(GuildMemberJoinEvent event) {
        ServerStore store = this.serverStorage.forServer(event.getGuild());

        Map<String, String> jlConfig = store.getObject("mod.jl", Map.class);
        String message = jlConfig.get("welcomeMsg");
        if (message == null) {
            return;
        } else if (message.isEmpty()) {
            message = loc.localize("com.divinitor.discord.wahrbot.ext.mod.jl.welcome.default");
        }

        List<TextChannel> postChannels;
        Set<String> channels = store.getObject("mod.jl.welcomeChannels", Set.class, String.class);
        if (channels.isEmpty()) {
            postChannels = Collections.singletonList(JLMessageHelper.getDefaultChannel(event.getGuild()));
        } else {
            postChannels = channels.stream()
                .map(event.getGuild()::getTextChannelById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }

        String msg = JLMessageHelper.format(message, event.getMember());

        postChannels.forEach(c -> {
            c.sendMessage(msg)
                .queue();
        });
    }

    @Subscribe
    public void onMemberJoinDM(GuildMemberJoinEvent event) {
        ServerStore store = this.serverStorage.forServer(event.getGuild());

        Map<String, String> jlConfig = store.getObject("mod.jl", Map.class);
        String message = jlConfig.get("welcomeDM");
        if (message == null) {
            return;
        } else if (message.isEmpty()) {
            message = loc.localize("com.divinitor.discord.wahrbot.ext.mod.jl.welcome.dm.default");
        }

        String msg = JLMessageHelper.format(message, event.getMember());

        event.getMember().getUser().openPrivateChannel()
            .queue(pc -> pc.sendMessage(msg)
                .queue());
    }

    @Subscribe
    public void onMemberLeave(GuildMemberLeaveEvent event) {
        ServerStore store = this.serverStorage.forServer(event.getGuild());

        Map<String, String> jlConfig = store.getObject("mod.jl", Map.class);
        String message = jlConfig.get("farewellMsg");
        if (message == null) {
            return;
        } else if (message.isEmpty()) {
            message = loc.localize("com.divinitor.discord.wahrbot.ext.mod.jl.farewell.default");
        }

        List<TextChannel> postChannels;
        Set<String> channels = store.getObject("mod.jl.farewellChannels", Set.class, String.class);
        if (channels.isEmpty()) {
            postChannels = Collections.singletonList(JLMessageHelper.getDefaultChannel(event.getGuild()));
        } else {
            postChannels = channels.stream()
                .map(event.getGuild()::getTextChannelById)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        }


        String msg = JLMessageHelper.format(message, event.getMember());

        postChannels.forEach(c -> {
            c.sendMessage(msg)
                .queue();
        });
    }

}
