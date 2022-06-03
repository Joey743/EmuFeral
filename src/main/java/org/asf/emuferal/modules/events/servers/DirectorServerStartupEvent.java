package org.asf.emuferal.modules.events.servers;

import java.util.Map;

import org.asf.emuferal.modules.eventbus.EventObject;
import org.asf.emuferal.modules.eventbus.EventPath;
import org.asf.rats.ConnectiveHTTPServer;

/**
 * 
 * Director Startup Event - used to handle startup of the director server and
 * register handlers via modules.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
@EventPath("director.startup")
public class DirectorServerStartupEvent extends EventObject {

	private ConnectiveHTTPServer server;

	public DirectorServerStartupEvent(ConnectiveHTTPServer server) {
		this.server = server;
	}

	@Override
	public String eventPath() {
		return "director.startup";
	}

	/**
	 * Retrieves the Director server
	 * 
	 * @return ConnectiveHTTPServer instance
	 */
	public ConnectiveHTTPServer getServer() {
		return server;
	}

	@Override
	public Map<String, String> eventProperties() {
		return Map.of();
	}
}
