package com.divinitor.discord.wahrbot.ext.dn.util;

import com.divinitor.discord.wahrbot.core.command.Command;

import java.util.function.Consumer;

@FunctionalInterface
public interface QueueExceptionHandler {

    void handleException(Command command, String id, Throwable throwable);

    static Consumer<Throwable> handler(Command command, String id, QueueExceptionHandler handler) {
        return (throwable -> handler.handleException(command, id, throwable));
    }
}
