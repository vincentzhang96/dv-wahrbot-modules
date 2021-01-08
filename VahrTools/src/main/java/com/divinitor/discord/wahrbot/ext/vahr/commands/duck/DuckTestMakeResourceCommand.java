package com.divinitor.discord.wahrbot.ext.vahr.commands.duck;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.divinitor.discord.wahrbot.ext.vahr.VahrModule;
import com.google.common.util.concurrent.Futures;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import org.json.JSONObject;

import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

public class DuckTestMakeResourceCommand extends BasicMemoryCommand {

    public DuckTestMakeResourceCommand() {
        super(
                "makeres",
                "Make resource pak and deploy",
                "Builds a resource pak from the given staging folder, optionally deploying it",
                "<staging folder name> <deploy>",
                "`staging folder name` - The name of the staging folder to deploy, e.g. 645\n`deploy` - Specify 'deploy' to deploy the resources to gameres"
        );
    }

    @Override
    public String getKey() {
        return VahrModule.MODULE_KEY + ".commands.makeres";
    }

    @Override
    public CommandResult invoke(CommandContext context) {
        CommandLine line = context.getCommandLine();
        if (!line.hasNext()) {
            context.getFeedbackChannel().sendMessage("Missing staging folder name, please specify a folder")
                    .queue();
            return CommandResult.rejected();
        }

        String stagingName = line.next();
        boolean deploy = line.hasNext() && "deploy".equalsIgnoreCase(line.next());

        EmbedBuilder builder = new EmbedBuilder()
                .setColor(0xFFA500)
                .addField("Staging Folder", stagingName, false)
                .addField("Deploy?", deploy ? "Yes" : "No", false)
                .appendDescription("Building resources...");

        Message message = context.getFeedbackChannel().sendMessage(builder.build()).complete();

        Unirest.post("http://westus.test.infra.fatduckdn.com:8001/makeres")
                .queryString("name", stagingName)
                .queryString("deploy", deploy ? "true" : "false")
                .queryString("key", "p6ukcUBSf3GJ8o6kI4wOCygBLCK3nDqU")
                .asStringAsync(new Callback<String>() {
                    @Override
                    public void completed(HttpResponse<String> response) {
                        String bodyS = response.getBody();
                        if (response.getStatus() != 200) {
                            EmbedBuilder b1 = new EmbedBuilder()
                                    .setColor(0xFF0000)
                                    .setTitle("Duck DN Resource Build")
                                    .addField("Staging Folder", stagingName, false)
                                    .addField("Error", bodyS, false)
                                    .appendDescription("Build/deploy FAILED");
                            message.editMessage(b1.build()).queue();
                        } else {
                            JSONObject body = new JSONObject(bodyS);
                            String pakUrl = body.getString("pak_url");
                            String zipUrl = body.getString("zip_url");
                            String pakName = pakUrl.substring(pakUrl.lastIndexOf('/'));
                            String zipName = zipUrl.substring(zipUrl.lastIndexOf('/'));
                            boolean deployed = body.getBoolean("deployed");
                            EmbedBuilder b1 = new EmbedBuilder()
                                    .setColor(0x00FF00)
                                    .setTitle("Duck DN Resource Build")
                                    .addField("Staging Folder", stagingName, false)
                                    .addField("Pak", String.format("[Download %s](%s)", pakName, pakUrl), false)
                                    .addField("Zip", String.format("[Download %s](%s)", zipName, zipUrl), false);
                            if (deployed) {
                                b1.addField("Deployed", "This pak has been deployed to test (pending restart)", false);
                            }
                            message.editMessage(b1.build()).queue();
                        }
                    }

                    @Override
                    public void failed(UnirestException e) {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Resource Build")
                                .addField("Staging Folder", stagingName, false)
                                .addField("Error", e.toString(), false)
                                .appendDescription("Build/deploy FAILED");
                        message.editMessage(b1.build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Resource Build")
                                .addField("Staging Folder", stagingName, false)
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("Build/deploy FAILED");
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
