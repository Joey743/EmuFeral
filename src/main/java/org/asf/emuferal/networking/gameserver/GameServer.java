package org.asf.emuferal.networking.gameserver;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Base64;
import java.util.ConcurrentModificationException;
import java.util.Random;

import org.asf.emuferal.EmuFeral;
import org.asf.emuferal.accounts.AccountManager;
import org.asf.emuferal.accounts.EmuFeralAccount;
import org.asf.emuferal.networking.smartfox.BaseSmartfoxServer;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.smartfox.ISmartfoxPacket;
import org.asf.emuferal.packets.xml.handshake.auth.ClientToServerAuthPacket;
import org.asf.emuferal.packets.xml.handshake.version.ClientToServerHandshake;
import org.asf.emuferal.packets.xml.handshake.version.ServerToClientOK;
import org.asf.emuferal.packets.xt.gameserver.InventoryItemDownloadPacket;
import org.asf.emuferal.packets.xt.gameserver.PrefixedPacket;
import org.asf.emuferal.players.Player;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class GameServer extends BaseSmartfoxServer {

	public GameServer(ServerSocket socket) {
		super(socket);
	}

	private Random rnd = new Random();
	private XmlMapper mapper = new XmlMapper();
	private ArrayList<Player> players = new ArrayList<Player>();

	private Player[] getPlayers() {
		while (true) {
			try {
				return players.toArray(t -> new Player[t]);
			} catch (ConcurrentModificationException e) {
			}
		}
	}

	@Override
	protected void registerPackets() {
		mapper = new XmlMapper();

		// Handshake
		registerPacket(new ClientToServerHandshake(mapper));
		registerPacket(new ClientToServerAuthPacket(mapper));

		// Game
		registerPacket(new PrefixedPacket());
		registerPacket(new InventoryItemDownloadPacket());
	}

	@Override
	protected void startClient(SmartfoxClient client) throws IOException {
		// Read first handshake packet
		ClientToServerHandshake pk = client.readPacket(ClientToServerHandshake.class);

		// Check version
		boolean badClient = false;
		if (!pk.actionField.equals("verChk") || !pk.clientBuild.equals("165")) {
			badClient = true; // Ok, bad version, lets make sure the client disconnects
		}

		// Send response so that the client moves on
		client.sendPacket(new ServerToClientOK(mapper));

		// Random key
		client.readPacket(ISmartfoxPacket.class);
		String key = Long.toString(rnd.nextLong(), 16);
		client.sendPacket("<msg t='sys'><body action='rndK' r='-1'><k>" + key + "</k></body></msg>");

		// Authenticate the player
		ClientToServerAuthPacket auth = client.readPacket(ClientToServerAuthPacket.class);

		// If the client is out of date, send error
		if (badClient) {
			JsonObject response = new JsonObject();
			JsonObject b = new JsonObject();
			b.addProperty("r", auth.rField);
			JsonObject o = new JsonObject();
			o.addProperty("statusId", -24);
			o.addProperty("_cmd", "login");
			o.addProperty("status", -24);
			b.add("o", o);
			response.add("b", b);
			response.addProperty("t", "xt");
			sendPacket(client, response.toString());
			client.disconnect();
			return;
		}

		// Load token
		String token = auth.pword;

		// Verify signature
		String verifyD = token.split("\\.")[0] + "." + token.split("\\.")[1];
		String sig = token.split("\\.")[2];
		if (!EmuFeral.verify(verifyD.getBytes("UTF-8"), Base64.getUrlDecoder().decode(sig))) {
			client.disconnect();
			return;
		}

		// Parse JWT
		JsonObject payload = JsonParser
				.parseString(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), "UTF-8"))
				.getAsJsonObject();

		// Locate account
		EmuFeralAccount acc = AccountManager.getInstance().getAccount(payload.get("uuid").getAsString());
		if (acc == null) {
			client.disconnect();
			return;
		}

		// Build Player object
		Player plr = new Player();
		plr.client = client;
		plr.account = acc;
		plr.activeLook = acc.getActiveLook();
		plr.activeSanctuaryLook = acc.getActiveSanctuaryLook();

		// Save player in the client object
		client.container = plr;

		// Send response
		JsonObject response = new JsonObject();
		JsonObject b = new JsonObject();
		b.addProperty("r", auth.rField);
		JsonObject o = new JsonObject();
		o.addProperty("statusId", 1);
		o.addProperty("_cmd", "login");
		JsonObject params = new JsonObject();
		params.addProperty("jamaaTime", System.currentTimeMillis() / 1000);
		params.addProperty("pendingFlags", (plr.account.isPlayerNew() ? 2 : 3));
		params.addProperty("activeLookId", plr.activeLook);
		params.addProperty("sanctuaryLookId", plr.activeSanctuaryLook);
		params.addProperty("sessionId", plr.account.getAccountID());
		params.addProperty("userId", plr.account.getAccountNumericID());
		params.addProperty("avatarInvId", 0);
		o.add("params", params);
		o.addProperty("status", 1);
		b.add("o", o);
		response.add("b", b);
		response.addProperty("t", "xt");
		sendPacket(client, response.toString());

		// Initial login
		System.out.println(
				"Player connected: " + plr.account.getLoginName() + " (as " + plr.account.getDisplayName() + ")");
		sendPacket(client, "%xt%ulc%-1%");
		String playerMsg = "%xt%rfl%-1%true%";
		for (Player player : getPlayers()) {
			playerMsg += player.account.getAccountID() + "%-1%";
		}
		sendPacket(client, playerMsg); // TODO: verify that this is the player list packet!
		players.add(plr);
	}

	@Override
	protected void clientDisconnect(SmartfoxClient client) {
		if (client.container != null && client.container instanceof Player) {
			Player plr = (Player) client.container;
			if (players.contains(plr)) {
				players.remove(plr);
				System.out.println("Player disconnected: " + plr.account.getLoginName() + " (was "
						+ plr.account.getDisplayName() + ")");
			}
		}
	}

	@Override
	protected void onStart() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void onStop() {
		// TODO Auto-generated method stub

	}

}
