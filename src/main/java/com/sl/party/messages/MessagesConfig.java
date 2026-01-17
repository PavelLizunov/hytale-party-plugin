package com.sl.party.messages;

import com.hypixel.hytale.server.core.Message;

import java.awt.*;

/**
 * Message configuration for party-related messages
 */
public final class MessagesConfig {

    private MessagesConfig() {
        // Utility class
    }

    public static final Message ONLY_PLAYER = Message.raw("This command can only be used by players").color(Color.RED);

    public static final Message ALREADY_IN_A_PARTY = Message.raw("You are already in a party").color(Color.RED);
    public static final Message NOT_IN_A_PARTY = Message.raw("You are not in a party").color(Color.RED);

    public static final Message ONLY_OWNER_CAN_INVITE = Message.raw("Only the party leader can invite players").color(Color.RED);
    public static final Message ONLY_OWNER_CAN_DISBAND = Message.raw("Only the party leader can disband the party").color(Color.RED);
    public static final Message OWNER_CANT_LEAVE_PARTY = Message.raw("Party leader cannot leave. Use /party disband instead").color(Color.RED);

    public static final Message PLAYER_NOT_FOUND = Message.raw("Player not found").color(Color.RED);
    public static final Message PLAYER_DONT_HAVE_PARTY = Message.raw("This player doesn't have a party").color(Color.RED);

    public static final Message PLAYER_DONT_HAVE_INVITE = Message.raw("You don't have an invite to this party").color(Color.RED);

    public static final Message PARTY_CREATED = Message.raw("Party created!").color(Color.GREEN);
    public static final Message PARTY_DISBAND = Message.raw("Party has been disbanded").color(Color.YELLOW);

    public static final Message PARTY_INVITE_SENT = Message.raw("Invite sent!").color(Color.GREEN);
    public static final Message PARTY_INVITE_RECEIVED = Message.raw("You received a party invite! Use /party join <leader> to accept").color(Color.CYAN);

    public static final Message PARTY_PUBLIC_STATUS = Message.raw("Party is now").color(Color.YELLOW);

    public static final Message PLAYER_JOIN_PARTY = Message.raw("joined the party").color(Color.GREEN);
    public static final Message PLAYER_LEAVE_PARTY = Message.raw("left the party").color(Color.YELLOW);

    public static final Message PLAYER_JOIN_PARTY_TITLE = Message.raw("Welcome to the party!").color(Color.WHITE);
    public static final Message PLAYER_LEAVE_PARTY_TITLE = Message.raw("You left the party").color(Color.WHITE);
}
