package com.divinitor.discord.wahrbot.ext.lol;

public class LoLServiceApiException extends RuntimeException {

    public LoLServiceApiException() {
    }

    public LoLServiceApiException(String message) {
        super(message);
    }

    public LoLServiceApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public LoLServiceApiException(Throwable cause) {
        super(cause);
    }

    public LoLServiceApiException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
