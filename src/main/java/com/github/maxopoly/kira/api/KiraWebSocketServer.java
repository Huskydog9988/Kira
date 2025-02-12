package com.github.maxopoly.kira.api;

import net.civmc.kira.Kira;
import com.github.maxopoly.kira.api.input.APISupplier;
import com.github.maxopoly.kira.api.token.APIToken;
import com.github.maxopoly.kira.api.token.APITokenManager;
import com.github.maxopoly.kira.ConfigManager;
import org.apache.logging.log4j.Logger;
import org.java_websocket.WebSocket;
import org.java_websocket.framing.CloseFrame;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;
import org.java_websocket.server.WebSocketServer;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

public class KiraWebSocketServer extends WebSocketServer {

	private static final int API_VERSION = 1;

	/**
	 * Takes an URI and returns the last key-value mapping for each query parameter pair.
	 * If the URI contains no query parameters, returns an empty map.
	 * Ignores the fragment part of the URI.
	 * Malformed keys/values (unsupported encoding) are ignored.
	 * Note that keys are case sensitive as per RFC 3986. https://tools.ietf.org/html/rfc3986#page-11
	 */
	public static Map<String, String> getQueryParams(String uri) {
		Map<String, String> queryPairs = new HashMap<>();

		int paramsSepIdx = uri.indexOf('?');
		if (paramsSepIdx < 0) return queryPairs; // no query params in URI
		String query = uri.substring(paramsSepIdx + 1);

		int fragmentSepIdx = query.indexOf('#');
		if (fragmentSepIdx >= 0) {
			query = query.substring(0, fragmentSepIdx);
		}

		String[] pairs = query.split("&");
		for (String pair : pairs) {
			int kvSep = pair.indexOf('=');
			try {
			String key = URLDecoder.decode(pair.substring(0, kvSep), "UTF-8");
			String value = URLDecoder.decode(pair.substring(kvSep + 1), "UTF-8");
			queryPairs.put(key, value);
			} catch (UnsupportedEncodingException ignored) {
			}
		}
		return queryPairs;
	}
	
	private Map <WebSocket, APISession> connections;

	private Logger logger;

    public KiraWebSocketServer(Logger logger, ConfigManager config) {
		super(new InetSocketAddress(config.getAPIInetAdress(), config.getAPIPort()));
		this.logger = logger;
		connections = new HashMap<>();
		logger.info("Starting Web socket API server");
		SSLContext sslContext = genSSLContext();
		if (sslContext == null) {
			logger.warn("No SSL certificate loaded for API, API will not use TLS");
		}
		else {
			this.setWebSocketFactory(new DefaultSSLWebSocketServerFactory(sslContext));
			logger.info("SSL certificate loaded for API websocket server, using TLSv1.2");
		}
		start();
	}

	private SSLContext genSSLContext() {
        ConfigManager config = Kira.Companion.getInstance().getConfig();
		String path = config.getAPISSLCertPath();
		String pw = config.getAPISSLCertPassword();
		if (path == null || pw == null) {
			return null;
		}
		return SSLContextFactory.generate(new File(path), pw);
	}

	private APISession getAPISession(WebSocket socket) {
		return connections.get(socket);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		Kira.Companion.getInstance().getLogger().warn("Closing connection with " + conn.getRemoteSocketAddress() + ", because of: " + reason);
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		String remoteAddr = conn == null ? "(no connection)" : conn.getRemoteSocketAddress().toString();
		Kira.Companion.getInstance().getLogger().warn("Error occured in API handling of " + remoteAddr, ex);
		if (conn != null) {
			conn.close(CloseFrame.UNEXPECTED_CONDITION);
		}
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		Kira.Companion.getInstance().getApiSessionManager().getInputHandler().handle(message, new APISupplier(getAPISession(conn)));
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		APISession session = setupSession(conn, handshake);
		if (session != null) {
			logger.info("Set up API session with " + conn.getRemoteSocketAddress());
			Kira.Companion.getInstance().getApiSessionManager().registerSession(session);
			connections.put(conn, session);
			session.sendAuthMessage();
		}
	}

	@Override
	public void onStart() {
		//already did everything in constructor, but still have to override this
	}
	
	private APISession setupSession(WebSocket conn, ClientHandshake handshake) {
		Map<String, String> queryParams = getQueryParams(handshake.getResourceDescriptor());
		String tokenString = queryParams.get("apiToken");
		if (tokenString == null || tokenString.equals("")) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because no api token was given");
			conn.close(CloseFrame.POLICY_VALIDATION, "No token supplied");
			return null;
		}
		String appId = queryParams.get("applicationId");
		if (appId == null || appId.equals("")) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because no application id was given");
			conn.close(CloseFrame.POLICY_VALIDATION, "No app id supplied");
			return null;
		}
		String versionString = queryParams.get("apiVersion");
		if (versionString == null || versionString.equals("")) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because no api version was given");
			conn.close(CloseFrame.POLICY_VALIDATION, "No api version supplied");
			return null;
		}
		int version;
		try {
			version = Integer.parseInt(versionString);
		}
		catch (NumberFormatException e) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because illegal version " + versionString + " was supplied");
			conn.close(CloseFrame.POLICY_VALIDATION, "Illegal version, not a number");
			return null;
		}
		if (version != API_VERSION) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because they are using an outdated api version");
			conn.close(CloseFrame.POLICY_VALIDATION, "Outdated version, newest one is " + API_VERSION);
			return null;
		}
		APITokenManager tokenMan = Kira.Companion.getInstance().getApiSessionManager().getTokenManager();
		APIToken token = tokenMan.getToken(tokenString);
		if (token == null) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because they supplied an invalid token");
			conn.close(CloseFrame.POLICY_VALIDATION, "Invalid token");
			return null;
		}
		if (token.isOutdated()) {
			logger.info("Closing connection with " + conn.getRemoteSocketAddress() + ", because their token had timed out");
			conn.close(CloseFrame.POLICY_VALIDATION, "Outdated token");
		}
		return token.generateSession(conn);
	}
}
