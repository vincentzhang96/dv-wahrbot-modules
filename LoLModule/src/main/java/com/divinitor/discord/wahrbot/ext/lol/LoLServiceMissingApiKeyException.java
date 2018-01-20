package com.divinitor.discord.wahrbot.ext.lol;

public class LoLServiceMissingApiKeyException extends RuntimeException {

    public LoLServiceMissingApiKeyException() {
    }

    public LoLServiceMissingApiKeyException(String message) {
        super(message);
    }

    public LoLServiceMissingApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoLServiceMissingApiKeyException(Throwable cause) {
        super(cause);
    }

    public LoLServiceMissingApiKeyException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
