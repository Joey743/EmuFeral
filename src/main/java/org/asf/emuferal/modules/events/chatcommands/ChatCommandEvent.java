package org.asf.emuferal.modules.events.chatcommands;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.emuferal.networking.chatserver.ChatClient;
import org.asf.emuferal.networking.chatserver.ChatServer;
import org.asf.emuferal.networking.gameserver.GameServer;

/**
 * 
 * Event called to handle module chat commands
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("chatcommands.run")
public class ChatCommandEvent extends EventObject {

	private Consumer<String> messageCallback;
	private ArrayList<String> args;
	private String cmdID;

	private String permLevel;
	private ChatClient client;
	private ChatServer server;
	private EmuFeralAccount account;

	public ChatCommandEvent(String cmdID, ArrayList<String> commandMessages, ChatClient client, EmuFeralAccount account,
			String permLevel, Consumer<String> messageCallback) {
		this.messageCallback = messageCallback;
		this.cmdID = cmdID;
		this.args = commandMessages;
		this.client = client;
		this.server = client.getServer();
		this.account = account;
		this.permLevel = permLevel;
	}

	@Override
	public String eventPath() {
		return "chatcommands.run";
	}

	/**
	 * Retrieves the ID of the invoked command
	 * 
	 * @return Command ID string
	 */
	public String getCommandID() {
		return cmdID;
	}

	/**
	 * Retrieves the command argument array
	 * 
	 * @return Array of command arguments
	 */
	public String[] getCommandArguments() {
		return args.toArray(t -> new String[t]);
	}

	/**
	 * Retrieves the chat client instance
	 * 
	 * @return Chat client instance
	 */
	public ChatClient getClient() {
		return client;
	}

	/**
	 * Retrieves the chat server instance
	 * 
	 * @return Chat server instance
	 */
	public ChatServer getServer() {
		return server;
	}

	/**
	 * Retrieves the player account object
	 * 
	 * @return EmuFeralAccount instance
	 */
	public EmuFeralAccount getAccount() {
		return account;
	}

	/**
	 * Checks if the player has a specific permission level
	 * 
	 * @param perm Permission level to check
	 * @return True if the player has the given permission level or greater, false
	 *         otherwise
	 */
	public boolean hasPermission(String perm) {
		return GameServer.hasPerm(permLevel, perm);
	}

	/**
	 * Sends a response to the command
	 * 
	 * @param message Message to send
	 */
	public void respond(String message) {
		setHandled();
		messageCallback.accept(message);
	}

	@Override
	public Map<String, String> eventProperties() {
		String argsStr = "";
		for (String str : args) {
			if (!argsStr.isEmpty())
				argsStr += " ";

			if (str.contains(" "))
				argsStr += "\"";
			argsStr = str;
			if (str.contains(" "))
				argsStr += "\"";
		}
		return Map.of("accountId", account.getAccountID(), "playerName", account.getDisplayName(), "accountName",
				account.getLoginName(), "address",
				((InetSocketAddress) client.getSocket().getRemoteSocketAddress()).getAddress().getHostAddress(),
				"command", cmdID, "arguments", argsStr);
	}

}
