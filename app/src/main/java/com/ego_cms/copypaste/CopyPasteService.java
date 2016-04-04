package com.ego_cms.copypaste;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.ego_cms.copypaste.util.Lazy;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class CopyPasteService extends Service {

	private static final String TAG = "CopyPasteService";

	private static final String KEY_SERVICE_IS_RUNNING = TAG + ".keyIsRunning";

	private static final String ACTION_START_CLIENT = TAG + ".action.START_CLIENT";
	private static final String ACTION_START_SERVER = TAG + ".action.START_SERVER";
	private static final String ACTION_STOP         = TAG + ".action.STOP";


	private static final String ACTION_SERVICE_CALLBACK = TAG + ".action.SERVICE_CALLBACK";


	public static final String CALLBACK_EXTRA_CLIENT_CONNECTED = TAG + ".extraClientConnected";

	public static void registerCallbackReceiver(@NonNull Context context,
		@NonNull BroadcastReceiver receiver) {

		LocalBroadcastManager.getInstance(context)
			.registerReceiver(receiver, new IntentFilter(ACTION_SERVICE_CALLBACK));
	}

	public static void unregisterCallbackReceiver(@NonNull Context context,
		@NonNull BroadcastReceiver receiver) {

		LocalBroadcastManager.getInstance(context)
			.unregisterReceiver(receiver);
	}


	public static void startServer(@NonNull Context context) {
		context.startService(
			new Intent(ACTION_START_SERVER, null, context, CopyPasteService.class));
	}

	public static void startClient(@NonNull Context context, // preserve new line
		@NonNull String networkAddress, int port) {

		context.startService(new Intent(ACTION_START_CLIENT,
			Uri.parse(String.format(Locale.US, "http://%s:%d", networkAddress, port)), context,
			CopyPasteService.class));
	}

	public static void stop(@NonNull Context context) {
		context.startService(new Intent(ACTION_STOP, null, context, CopyPasteService.class));
	}


	private static Boolean isRunning;

	public static boolean isRunning(@NonNull Context context) {
		if (isRunning == null) {
			isRunning = CopyPasteApplication.get(context)
				.getCommonPreferences()
				.contains(KEY_SERVICE_IS_RUNNING);
		}
		return isRunning;
	}

	private static void setRunningAction(Context context, String action) {
		isRunning = !TextUtils.isEmpty(action);
		SharedPreferences.Editor editor = CopyPasteApplication.get(context)
			.getCommonPreferences()
			.edit();

		if (isRunning) {
			editor.putString(KEY_SERVICE_IS_RUNNING, action);
		}
		else {
			editor.remove(KEY_SERVICE_IS_RUNNING);
		}
		editor.apply();
	}

	private static String getRunningAction(Context context) {
		return CopyPasteApplication.get(context)
			.getCommonPreferences()
			.getString(KEY_SERVICE_IS_RUNNING, "");
	}


	@Nullable
	public static String getNetworkAddress() {
		try {
			for (Enumeration<NetworkInterface> networkInterfaces
				= NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();

				if (!networkInterface.isLoopback()) {
					for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
						addresses.hasMoreElements(); ) {
						InetAddress address = addresses.nextElement();

						if ((address instanceof Inet4Address) && !address.isLoopbackAddress()) {
							return address.getHostAddress();
						}
					}
				}
			}
		}
		catch (SocketException e) {
			e.printStackTrace();
		}
		return null;
	}


	@NonNull
	private String getClipValue() {
		return getClipValue(
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip());
	}

	private String getClipValue(ClipData clipData) {
		if (clipData != null) {
			for (int i = 0, imax = clipData.getItemCount(); i < imax; ++i) {
				CharSequence item = clipData.getItemAt(i)
					.coerceToText(CopyPasteService.this);

				if (!TextUtils.isEmpty(item)) {
					return item.toString();
				}
			}
		}
		return "";
	}

	private void setClipValue(Handler handler, String clipText) {
		if (!TextUtils.isEmpty(clipText)) {
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
				new ClipData(TAG, new String[]{NanoHTTPD.MIME_PLAINTEXT},
					new ClipData.Item(clipText)));

			handler.post(() -> // preserve new line
				Toast.makeText(CopyPasteService.this, R.string.label_clip_updated,
					Toast.LENGTH_LONG)
					.show());
		}
	}


	private final class ClipboardClient extends WebSocketClient
		implements ClipboardManager.OnPrimaryClipChangedListener {

		private final Handler handler;

		public ClipboardClient(Uri serverURI) {
			super(URI.create(serverURI.toString()));

			handler = new Handler(Looper.getMainLooper());
		}


		private Thread connectionThread;

		@Override
		public void run() {
			if (connectionThread == null) {
				connectionThread = new Thread(super::run);
				connectionThread.start();
			}
		}

		@Override
		public void onOpen(ServerHandshake handshakedata) {
			if (handshakedata.getHttpStatus() == 200) {
				((ClipboardManager) getSystemService(
					CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);

				Intent intent = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
					CopyPasteService.class);
				{
					intent.putExtra(CALLBACK_EXTRA_CLIENT_CONNECTED,
						(int) handshakedata.getHttpStatus());
				}
				LocalBroadcastManager.getInstance(CopyPasteService.this)
					.sendBroadcast(intent);
			}
		}

		@Override
		public void onMessage(String message) {
			setClipValue(handler, message);
		}

		@Override
		public void onClose(int code, String reason, boolean remote) {
			((ClipboardManager) getSystemService(
				CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(this);

			connectionThread = null;
		}

		@Override
		public void onError(Exception ex) {
			Intent intent = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
				CopyPasteService.class);
			{
				intent.putExtra(CALLBACK_EXTRA_CLIENT_CONNECTED, 500);
			}
			LocalBroadcastManager.getInstance(CopyPasteService.this)
				.sendBroadcast(intent);
			setRunningAction(CopyPasteService.this, null);
		}

		@Override
		public void onPrimaryClipChanged() {
			send(getClipValue());
		}
	}


	private ClipboardClient client;

	private int onStartClient(Intent intent) {
		client = new ClipboardClient(intent.getData());
		client.run();

		setRunningAction(this, intent.getAction());

		return START_REDELIVER_INTENT;
	}


	private final class ClipboardServer extends NanoWSD {

		private final Handler handler;

		public ClipboardServer() {
			super(49152 + new Random(System.currentTimeMillis()).nextInt(16384));

			handler = new Handler(Looper.getMainLooper());
		}

		private class Socket extends WebSocket
			implements ClipboardManager.OnPrimaryClipChangedListener {

			public Socket(IHTTPSession handshakeRequest) {
				super(handshakeRequest);
			}

			@Override
			protected void onOpen() {
				((ClipboardManager) getSystemService(
					CLIPBOARD_SERVICE)).addPrimaryClipChangedListener(this);

				ping();
			}

			@Override
			protected void onClose(WebSocketFrame.CloseCode code, String reason,
				boolean initiatedByRemote) {

				((ClipboardManager) getSystemService(
					CLIPBOARD_SERVICE)).removePrimaryClipChangedListener(this);
			}

			@Override
			protected void onMessage(WebSocketFrame message) {
				String clipText = message.getTextPayload();

				if (!TextUtils.isEmpty(clipText)) {
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
						new ClipData(TAG, new String[]{NanoHTTPD.MIME_PLAINTEXT},
							new ClipData.Item(clipText)));

					handler.post(() -> // preserve new line
						Toast.makeText(CopyPasteService.this, R.string.label_clip_updated,
							Toast.LENGTH_LONG)
							.show());
				}
			}

			@Override
			protected void onPong(WebSocketFrame pong) {
				handler.postDelayed(this::ping, 5000);
			}

			@Override
			protected void onException(IOException exception) {
				/* Nothing to do */
			}

			@Override
			public void onPrimaryClipChanged() {
				ClipData clipData = ((ClipboardManager) getSystemService(
					CLIPBOARD_SERVICE)).getPrimaryClip();

				if (!TAG.equals(clipData.getDescription()
					.getLabel())) {

					try {
						send(getClipValue(clipData));
					}
					catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

			private void ping() {
				try {
					ping("".getBytes());
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		protected WebSocket openWebSocket(IHTTPSession handshake) {
			return new Socket(handshake);
		}
	}


	private final Lazy<ClipboardServer> clipboardServerLazy = new Lazy<ClipboardServer>() {
		@Override
		protected ClipboardServer initialize() {
			ClipboardServer result = new ClipboardServer();

			try {
				result.start(15000);
			}
			catch (IOException e) {
				e.printStackTrace();
			}
			return result;
		}
	};

	private static final class ClipboardData {

		public int port;

		public String host;

		public String text;

	}

	private static final String ROOT_FOLDER = "www";


	// GET
	private NanoHTTPD.Response endpointClipboard(String url, NanoHTTPD.IHTTPSession session)
		throws Exception {

		ClipboardData data = new ClipboardData();
		{
			ClipboardServer clipboard = clipboardServerLazy.get();

			data.port = clipboard.getListeningPort();
			data.host = getNetworkAddress();
			data.text = getClipValue();
		}
		return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json",
			new Gson().toJson(data));
	}

	private NanoHTTPD.Response endpointIndex(String url, NanoHTTPD.IHTTPSession session)
		throws Exception {
		return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, NanoHTTPD.MIME_HTML,
			getAssets().open(ROOT_FOLDER + "/index.html"));
	}

	private NanoHTTPD.Response endpointScript(String url, NanoHTTPD.IHTTPSession session)
		throws Exception {
		return NanoHTTPD.newChunkedResponse(NanoHTTPD.Response.Status.OK, "text/javascript",
			getAssets().open(ROOT_FOLDER + session.getUri()));
	}


	private final static class Server extends NanoHTTPD {

		public Server(int port) {
			super(port);
		}


		public interface Endpoint {
			Response process(String uri, IHTTPSession session) throws Exception;
		}

		private final Map<Method, Map<String, Endpoint>> serverMap = new HashMap<>();

		public void register(Method method, String uri, Endpoint endpoint) {
			if (isAlive()) {
				throw new Error("Can't register anything, because server is running.");
			}
			synchronized (serverMap) {
				Map<String, Endpoint> methodMap = serverMap.get(method);

				if (methodMap == null) {
					methodMap = new HashMap<>();

					serverMap.put(method, methodMap);
				}
				methodMap.put(uri, endpoint);
			}
		}


		@Override
		public Response serve(IHTTPSession session) {
			log(session);

			try {
				if (isForbidden(session)) {
					return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT,
						"Access Denied");
				}
				Map<String, Endpoint> endpointMap = serverMap.get(session.getMethod());

				if (endpointMap != null) {
					String uri = session.getUri();

					while (!uri.isEmpty()) {
						Endpoint endpoint = endpointMap.get(uri);

						if (endpoint != null) {
							return endpoint.process(uri, session);
						}
						int pathSeparator = uri.lastIndexOf('/');

						if (pathSeparator > 0) {
							uri = uri.substring(0, pathSeparator);
						}
					}
				}
			}
			catch (Exception e) {
				return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT,
					e.getMessage());
			}
			return super.serve(session);
		}

		private boolean isForbidden(IHTTPSession session) throws SocketException {
			for (Enumeration<NetworkInterface> networkInterfaces
				= NetworkInterface.getNetworkInterfaces(); networkInterfaces.hasMoreElements(); ) {
				NetworkInterface networkInterface = networkInterfaces.nextElement();

				for (Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
					addresses.hasMoreElements(); ) {
					InetAddress address = addresses.nextElement();

					if (TextUtils.equals(address.getHostAddress(), session.getRemoteIpAddress())) {
						return true;
					}
				}
			}
			return false;
		}

		private static void log(IHTTPSession session) {
			Map<String, List<String>> decodedQueryParameters = decodeParameters(
				session.getQueryParameterString());

			StringBuilder sb = new StringBuilder();
			{
				sb.append("**** ⇊ HTTP Session START ⇊ ****\n")

					.append("⇨ URI: ")
					.append(session.getUri())
					.append("\n")

					.append("⇨ METHOD: ")
					.append(session.getMethod())
					.append("\n");

				append(sb, "⇨ HEADERS:", session.getHeaders());
				append(sb, "⇨ PARAMS:", session.getParms());
				append(sb, "⇨ QUERY PARAMS:", decodedQueryParameters);

				Map<String, String> body = new HashMap<>();

				try {
					session.parseBody(body);

					append(sb, "⇨ BODY:", body);
				}
				catch (Exception e) {
					e.printStackTrace();
				}
				sb.append("\n");
				sb.append("**** ⇈ HTTP Session STOP ⇈ ****");
			}
			Log.d(TAG, sb.toString());
		}

		private static void append(StringBuilder sb, String group, Map<String, ?> values) {
			if (!values.isEmpty()) {
				sb.append(group)
					.append(asString(values))
					.append("\n");
			}
		}

		private static String asString(Map<String, ?> map) {
			StringBuilder sb = new StringBuilder("\n");
			{
				for (Map.Entry<String, ?> entry : map.entrySet()) {
					sb.append("\t")
						.append(entry.getKey())
						.append(": ")
						.append(entry.getValue());

					sb.append("\n");
				}
				sb.delete(sb.length() - 1, sb.length());
			}
			return sb.toString();
		}
	}


	private Server server;

	private int onStartServer(Intent intent) {
		server = new Server(BuildConfig.SERVER_PORT);

		server.register(NanoHTTPD.Method.GET, "/", this::endpointIndex);
		server.register(NanoHTTPD.Method.GET, "/script", this::endpointScript);
		server.register(NanoHTTPD.Method.GET, "/clipboard", this::endpointClipboard);

		try {
			server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);

			setRunningAction(this, ACTION_START_SERVER);
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		return START_STICKY;
	}

	private void stopRunningActions(String action) {
		switch (action) {
			case ACTION_START_CLIENT:
				if (client != null) {
					client.close();
					client = null;
				}
				break;
			case ACTION_START_SERVER:
				if (server != null) {
					server.stop();

					if (!clipboardServerLazy.isEmpty()) {
						ClipboardServer clipboard = clipboardServerLazy.get();

						if (clipboard.isAlive()) {
							clipboard.stop();
						}
					}
					server = null;
				}
				break;
		}
	}


	@Override
	public void onCreate() {
		((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).getPrimaryClip();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action, actionCurrent = getRunningAction(this);

		if (intent != null) {
			action = intent.getAction();

			setRunningAction(this, action);
		}
		else {
			action = actionCurrent;
		}
		stopRunningActions(actionCurrent);

		switch (action) {
			case ACTION_START_CLIENT:
				return onStartClient(intent);
			case ACTION_START_SERVER:
				return onStartServer(intent);
			case ACTION_STOP:
				setRunningAction(this, null);
				stopSelf();
		}
		return START_NOT_STICKY;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		throw new Error("Binding is not supported.");
	}
}
