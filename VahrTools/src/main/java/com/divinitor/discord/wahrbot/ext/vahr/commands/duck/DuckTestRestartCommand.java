package com.divinitor.discord.wahrbot.ext.vahr.commands.duck;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.ext.vahr.VahrModule;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.json.JSONObject;

import java.util.NoSuchElementException;

public class DuckTestRestartCommand extends BasicMemoryCommand {

    public DuckTestRestartCommand() {
        super(
                "rtst",
                "Restart test server",
                "Restarts the test server"
        );
    }

    @Override
    public String getKey() {
        return VahrModule.MODULE_KEY + ".commands.rtst";
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        boolean gameOnly ="g".equalsIgnoreCase(context.getCommandLine().peek());

        Message message = context.getMessage();
        message.addReaction("U+1F4AC").queue();
        Unirest.post("http://westus.test.infra.fatduckdn.com:8001/restart" + (gameOnly ? "g" : ""))
                .queryString("key", "p6ukcUBSf3GJ8o6kI4wOCygBLCK3nDqU")
                .asStringAsync(new Callback<String>() {
                    @Override
                    public void completed(HttpResponse<String> response) {
                        String bodyS = response.getBody();
                        if (response.getStatus() != 200) {
                            EmbedBuilder b1 = new EmbedBuilder()
                                    .setColor(0xFF0000)
                                    .setTitle("Duck DN Test Restart")
                                    .addField("Error", bodyS, false)
                                    .appendDescription("Restart FAILED");
                            message.editMessage(b1.build()).queue();
                        } else {
                            message.addReaction("U+1F197").queue();
                            message.removeReaction("U+1F4AC").queue();
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test Restart")
                                .addField("Error", e.toString(), false)
                                .appendDescription("Restart FAILED");
                        message.editMessage(b1.build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test Restart")
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("Restart FAILED");
                        message.editMessage(b1.build()).queue();
                    }
                });

        return CommandResult.ok();
    }

    @Override
    public CommandConstraint<CommandContext> getUserPermissionConstraints() {
        return (context) -> {
            try {
                return context.getUserStorage().getBoolean("pdn.test");
            } catch (NoSuchElementException nsee) {
                return false;
            }
        };
    }

    @Override
    public CommandConstraint<CommandContext> getOtherConstraints() {
        return (context) -> context.getServer().getIdLong() == 544827049752264704L;
    }
}
