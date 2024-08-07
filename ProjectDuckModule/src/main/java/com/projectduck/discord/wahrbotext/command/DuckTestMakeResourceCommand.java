package com.projectduck.discord.wahrbotext.command;

import com.divinitor.discord.wahrbot.core.command.CommandConstraint;
import com.divinitor.discord.wahrbot.core.command.CommandContext;
import com.divinitor.discord.wahrbot.core.command.CommandLine;
import com.divinitor.discord.wahrbot.core.command.CommandResult;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.projectduck.discord.wahrbotext.DuckDNDiscordFeatures;
import com.projectduck.discord.wahrbotext.ProjectDuckModule;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.json.JSONObject;

import java.util.NoSuchElementException;

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
        return ProjectDuckModule.MODULE_KEY + ".command.makeres";
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
        boolean deploy = false;
        boolean zip = false;
        while (line.hasNext()) {
            String option = line.next().toLowerCase();
            if ("deploy".equals(option)) {
                deploy = true;
            } else if ("zip".equals(option)) {
                zip = true;
            }
        }

        makeRes(context.getFeedbackChannel(), context.getInvoker(), stagingName, deploy, zip);

        return CommandResult.ok();
    }

    public static void makeRes(MessageChannel channel, User user, String stagingName, boolean deploy, boolean zip) {
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(0xFFA500)
                .addField("Staging Folder", stagingName, false)
                .addField("Deploy?", deploy ? "Yes" : "No", false)
                .appendDescription("Building resources...");

        Message message = channel.sendMessage(new MessageBuilder().setEmbeds(builder.build()).build()).complete();

        Unirest.post("http://westus.test.infra.fatduckdn.com:8001/makeres")
                .queryString("name", stagingName)
                .queryString("deploy", deploy ? "true" : "false")
                .queryString("zip", zip ? "true" : "false")
                .queryString("key", DuckDNDiscordFeatures.DEPLOY_KEY)
                .queryString("obo", user.getId())
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
                            message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                        } else {
                            JSONObject body = new JSONObject(bodyS);
                            String pakUrl = body.getString("pak_url");
                            String zipUrl = null;
                            String zipName = null;
                            if (body.has("zip_url")) {
                                zipUrl = body.getString("zip_url");
                                zipName = zipUrl.substring(zipUrl.lastIndexOf('/'));
                            }
                            String pakName = pakUrl.substring(pakUrl.lastIndexOf('/'));
                            boolean deployed = body.getBoolean("deployed");
                            EmbedBuilder b1 = new EmbedBuilder()
                                    .setColor(0x00FF00)
                                    .setTitle("Duck DN Resource Build")
                                    .addField("Staging Folder", stagingName, false)
                                    .addField("Pak", String.format("[Download %s](%s)", pakName, pakUrl), false);
                            if (zipUrl != null) {
                                b1.addField("Zip", String.format("[Download %s](%s)", zipName, zipUrl), false);
                            }

                            if (deployed) {
                                b1.addField("Deployed", "This pak has been deployed to test (pending restart)", false);
                            }
                            message.editMessage(new MessageBuilder()
                                    .setEmbeds(b1.build())
                                    .setActionRows(ActionRow.of(
                                            Button.primary(
                                                    String.format("makeres;%s;%s", true, stagingName),
                                                            String.format("Remake and deploy %s", stagingName))
                                                    .withEmoji(Emoji.fromUnicode("\uD83D\uDEF3")),
                                            Button.primary(
                                                            String.format("makeres;%s;%s", false, stagingName),
                                                            String.format("Remake %s only", stagingName))
                                                    .withEmoji(Emoji.fromUnicode("\uD83D\uDD04"))
                                    ))
                                    .build()).queue();
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
                        message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
                    }

                    @Override
                    public void cancelled() {
                        EmbedBuilder b1 = new EmbedBuilder()
                                .setColor(0xFF0000)
                                .setTitle("Duck DN Resource Build")
                                .addField("Staging Folder", stagingName, false)
                                .addField("Error", "Request cancelled", false)
                                .appendDescription("Build/deploy FAILED");
                        message.editMessage(new MessageBuilder().setEmbeds(b1.build()).build()).queue();
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
