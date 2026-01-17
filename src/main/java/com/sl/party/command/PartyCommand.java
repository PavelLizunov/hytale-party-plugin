package com.sl.party.command;

import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.sl.party.cache.PartyCache;
import com.sl.party.command.impl.*;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

public class PartyCommand extends CommandBase {

    private final PartyCache partyCache;

    public PartyCommand(PartyCache partyCache) {
        super("party", "Party commands - create groups with friends");
        this.partyCache = partyCache;

        addSubCommand(new PartyCreateSubCommand(partyCache));
        addSubCommand(new PartyInfoSubCommand(partyCache));
        addSubCommand(new PartyJoinSubCommand(partyCache));
        addSubCommand(new PartyPublicSubCommand(partyCache));
        addSubCommand(new PartyInviteSubCommand(partyCache));
        addSubCommand(new PartyKickSubCommand(partyCache));
        addSubCommand(new PartyDisbandSubCommand(partyCache));
        addSubCommand(new PartyLeaveSubCommand(partyCache));
        addSubCommand(new PartyChatSubCommand(partyCache));
        addSubCommand(new PartyTestHudSubCommand());
        addSubCommand(new PartyTestMarkerSubCommand());
    }

    @Override
    protected boolean canGeneratePermission() {
        // Disable permission generation - allow all players to use
        return false;
    }

    @Override
    protected void executeSync(@NonNullDecl CommandContext commandContext) {
        commandContext.sendMessage(Message.raw("=== Party Commands ===").color(java.awt.Color.YELLOW));
        commandContext.sendMessage(Message.raw("/party create - Create a new party").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party info - Show party information").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party invite <player> - Invite a player").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party join <leader> - Join a party").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party kick <player> - Kick a player (leader only)").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party leave - Leave your party").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party disband - Disband your party").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party public - Toggle public/private").color(java.awt.Color.WHITE));
        commandContext.sendMessage(Message.raw("/party chat <msg> - Party chat").color(java.awt.Color.WHITE));
    }
}
