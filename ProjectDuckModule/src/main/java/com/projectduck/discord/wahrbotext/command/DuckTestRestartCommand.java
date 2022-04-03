package com.projectduck.discord.wahrbotext.command;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.projectduck.discord.wahrbotext.ProjectDuckModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Message;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

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
        return ProjectDuckModule.MODULE_KEY + ".command.rtst";
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        boolean gameOnly ="g".equalsIgnoreCase(context.getCommandLine().peek());

        Message message = context.getMessage();
        restartTest(message, gameOnly, (err) -> {});
        return CommandResult.ok();
    }

    public static void restartTest(Message message, boolean gameOnly, Consumer<String> handler) {
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
                            message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                            handler.accept("Restart FAILED");
                        } else {
                            message.addReaction("U+1F197").queue();
                            message.removeReaction("U+1F4AC").queue();
                            handler.accept("");
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test Restart")
                                .addField("Error", e.toString(), false)
                                .appendDescription("Restart FAILED");
                        message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                        handler.accept("Restart FAILED");
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Test Restart")
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("Restart FAILED");
                        message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                        handler.accept("Restart FAILED");
                    }
                });
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
