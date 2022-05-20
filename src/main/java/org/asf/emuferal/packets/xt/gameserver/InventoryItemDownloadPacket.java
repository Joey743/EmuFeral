package org.asf.emuferal.packets.xt.gameserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.UUID;

import org.asf.emuferal.accounts.PlayerInventory;
import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class InventoryItemDownloadPacket implements IXtPacket<InventoryItemDownloadPacket> {

	private String slot = "";

	@Override
	public InventoryItemDownloadPacket instantiate() {
		return new InventoryItemDownloadPacket();
	}

	@Override
	public String id() {
		return "ilt";
	}

	@Override
	public void parse(XtReader reader) {
		slot = reader.read();
	}

	@Override
	public void build(XtWriter writer) {
		writer.writeString(slot);
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		Player plr = (Player) client.container;
		PlayerInventory inv = plr.account.getPlayerInventory();

		// Save if the item is not found
		if (!inv.containsItem(slot)) {
			if (slot.equals("200")) {
				// Avatar slots are not stored yet so lets build the json
				JsonArray avatars = buildDefaultLooksFile(plr);

				// Save
				inv.setItem("avatars", avatars);
			} else {
				// Copy item from resources
				InputStream strm = getClass().getClassLoader()
						.getResourceAsStream("defaultitems/inventory-" + slot + ".json");
				if (strm != null) {
					inv.setItem(slot, JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")));
					strm.close();
				} else {
					throw new IOException("File not found");
				}
			}
		}

		// Load the item
		JsonElement item = inv.getItem(slot.equals("200") ? "avatars" : slot);

		// Send the item to the client
		InventoryItemPacket pkt = new InventoryItemPacket();
		pkt.item = item;
		client.sendPacket(pkt);

		return true;
	}

	private JsonArray buildDefaultLooksFile(Player plr) throws IOException {
		JsonArray items = new JsonArray();

		// Load the helper from resources
		System.out.println("Generating avatar file for " + plr.account.getDisplayName());
		InputStream strm = getClass().getClassLoader().getResourceAsStream("defaultitems/avatarhelper.json");
		JsonObject helper = JsonParser.parseString(new String(strm.readAllBytes(), "UTF-8")).getAsJsonObject()
				.get("Avatars").getAsJsonObject();

		// Construct the avatar list
		ArrayList<String> ids = new ArrayList<String>();
		for (String avatarSpecies : helper.keySet()) {
			JsonObject speciesData = helper.get(avatarSpecies).getAsJsonObject();
			System.out.println("Generating avatar species object " + avatarSpecies + " for "
					+ plr.account.getDisplayName() + "...");

			// Build 11 look files and set the first to primary
			boolean primary = true;
			for (int i = 0; i < 13; i++) {
				// Generate look ID
				String lID = UUID.randomUUID().toString();
				while (ids.contains(lID))
					lID = UUID.randomUUID().toString();
				ids.add(lID);

				// Timestamp
				JsonObject ts = new JsonObject();
				ts.addProperty("ts", System.currentTimeMillis());

				// Name
				JsonObject nm = new JsonObject();
				nm.addProperty("name", "");

				// Avatar info
				JsonObject al = new JsonObject();
				al.addProperty("gender", 0);
				al.add("info", speciesData.get("info").getAsJsonObject());

				// Build components
				JsonObject components = new JsonObject();
				if (primary)
					components.add("PrimaryLook", new JsonObject());
				components.add("Timestamp", ts);
				components.add("AvatarLook", al);
				components.add("Name", nm);

				// Build data container
				JsonObject lookObj = new JsonObject();
				lookObj.addProperty("defId", speciesData.get("defId").getAsInt());
				lookObj.add("components", components);
				lookObj.addProperty("id", lID);
				lookObj.addProperty("type", 200);

				// Add the avatar
				items.add(lookObj);
				primary = false;
			}
		}

		return items;
	}

}
