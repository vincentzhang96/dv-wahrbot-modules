package com.divinitor.discord.wahrbot.ext.mod.util;

import com.divinitor.discord.wahrbot.core.util.discord.SnowflakeUtils;

public class ReactionUtils {

    public static final String SERVER_STORE_MESSAGE_SET_KEY = "mod.reactrole.messages";
    public static final String SERVER_STORE_MESSAGE_ROLE_MAP_KEY_FMT = "mod.reactrole.message.%s";

    private ReactionUtils() {}

    public static String serverStoreMessageRoleMapKey(long messageId) {
        return String.format(SERVER_STORE_MESSAGE_ROLE_MAP_KEY_FMT, SnowflakeUtils.encode(messageId));
    }

    public static String messageChannelPair(String chId, String msgId) {
        return msgId + ":" + chId;
    }

    public static String messageChannelPairMsgId(String pair) {
        return pair.split(":", 2)[0];
    }

    public static String messageChannelPairChId(String pair) {
        return pair.split(":", 2)[1];
    }
}
