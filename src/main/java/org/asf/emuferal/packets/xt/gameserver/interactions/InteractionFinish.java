package org.asf.emuferal.packets.xt.gameserver.interactions;

import java.io.IOException;
import java.util.ArrayList;

import org.asf.emuferal.data.XtReader;
import org.asf.emuferal.data.XtWriter;
import org.asf.emuferal.interactions.InteractionManager;
import org.asf.emuferal.interactions.NetworkedObjects;
import org.asf.emuferal.interactions.dataobjects.NetworkedObject;
import org.asf.emuferal.interactions.dataobjects.StateInfo;
import org.asf.emuferal.modules.eventbus.EventBus;
import org.asf.emuferal.modules.events.interactions.InteractionSuccessEvent;
import org.asf.emuferal.networking.smartfox.SmartfoxClient;
import org.asf.emuferal.packets.xt.IXtPacket;
import org.asf.emuferal.players.Player;

public class InteractionFinish implements IXtPacket<InteractionFinish> {

	private static final String PACKET_ID = "oaf";

	private String target;
	private int currentState;

	@Override
	public InteractionFinish instantiate() {
		return new InteractionFinish();
	}

	@Override
	public String id() {
		return PACKET_ID;
	}

	@Override
	public void parse(XtReader reader) throws IOException {
		target = reader.read();
		currentState = reader.readInt();
	}

	@Override
	public void build(XtWriter writer) throws IOException {
	}

	@Override
	public boolean handle(SmartfoxClient client) throws IOException {
		// Interaction finish
		Player plr = (Player) client.container;

		if (System.getProperty("debugMode") != null) {
			System.out.println("[INTERACTION] [FINISH] Client to server (target: " + target + ")");
		}

		// Find object
		NetworkedObject obj = NetworkedObjects.getObject(target);
		if (obj == null)
			return true;

		// Controls if the resource will be destroyed
		boolean destroy = true;

		// Dispatch event
		InteractionSuccessEvent ev = new InteractionSuccessEvent(plr, target, obj, currentState);
		ev.setDestroyResource(destroy);
		EventBus.getInstance().dispatchEvent(ev);
		destroy = ev.shouldDestroyResource();
		if (!ev.isHandled()) {
			// Handle interaction
			destroy = InteractionManager.handleInteraction(plr, target, obj, currentState, destroy);
		}

		// Send qcmd
		if (obj.stateInfo.containsKey(Integer.toString(currentState))) {
			ArrayList<StateInfo> states = obj.stateInfo.get(Integer.toString(currentState));
			for (StateInfo st : states) {
				// Build quest command
				XtWriter pk = new XtWriter();
				pk.writeString("qcmd");
				pk.writeInt(-1); // Data prefix
				pk.writeString(st.command); // command
				pk.writeInt(0); // State
				pk.writeString(target); // Interactable
				pk.writeInt(0); // Position
				// Parameters
				for (String param : st.params)
					pk.writeString(param);
				pk.writeString(""); // Data suffix
				client.sendPacket(pk.encode());
			}
		}

		// Build response
		XtWriter pk = new XtWriter();
		pk.writeString("oaf");
		pk.writeInt(-1); // Data prefix
		pk.writeString(target); // Interactable
		pk.writeInt(obj.primaryObjectInfo.type); // Type
		pk.writeString(destroy ? "2" : "0");
		pk.writeString(""); // Data suffix
		client.sendPacket(pk.encode());

		return true;
	}

}
