package com.projectduck.discord.wahrbotext;

import com.divinitor.discord.wahrbot.core.WahrBot;
import com.google.inject.Inject;
import io.javalin.Javalin;
import io.javalin.http.Context;
import net.dv8tion.jda.api.entities.User;

import java.util.HashMap;
import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.*;

public class ApiServer {

    private Javalin server;

    private final WahrBot bot;

    @Inject
    public ApiServer(WahrBot bot) {
        this.bot = bot;
    }

    public void start() {
        if (this.server != null) {
            this.server.stop();
        }
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(String.class.getClassLoader());

        this.server = Javalin.create().start(7250);

        Thread.currentThread().setContextClassLoader(classLoader);

        this.server.routes(() -> {
            path("discord", () -> {
                get("/user/{id}", this::getUserById);
            });
        });
        ProjectDuckModule.LOGGER.info("Started API server");
    }

    public void stop() {
        if (this.server != null) {
            this.server.stop();
            ProjectDuckModule.LOGGER.info("Stopped API server");
        }
    }

    private void getUserById(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            User complete = this.bot.getApiClient().retrieveUserById(id).complete();
            if (complete == null) {
                ctx.status(404).result("User ID " + id + " could not be resolved");
                return;
            }

            Map<String, Object> res = new HashMap<>();
            res.put("username", complete.getName());
            res.put("discriminator", complete.getDiscriminator());
            res.put("id", complete.getId());
            res.put("avatar", complete.getEffectiveAvatarUrl());

            ctx.status(200).json(res);
        } catch (Exception e) {
            ctx.status(500).result("Internal server error: " + e.toString());
        }
    }

}
