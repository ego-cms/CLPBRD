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
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.ego_cms.copypaste.util.Delegate;
import com.ego_cms.copypaste.util.Lazy;
import com.google.gson.Gson;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoWSD;

public class CopyPasteService extends Service {

	private static final String TAG = "CopyPasteService";


	private static CopyPasteApplication application;

	public static void initialize(CopyPasteApplication application) {
		CopyPasteService.application = application;
	}

	private static final String KEY_SERVICE_IS_RUNNING = TAG + ".keyIsRunning";

	private static final String ACTION_START_CLIENT = TAG + ".action.START_CLIENT";
	private static final String ACTION_START_SERVER = TAG + ".action.START_SERVER";
	private static final String ACTION_STOP         = TAG + ".action.STOP";


	private static final int ROLE_UNKNOWN = 0;
	public static final  int ROLE_CLIENT  = 1;
	public static final  int ROLE_SERVER  = 2;

	@IntDef(value = {
		ROLE_CLIENT,
		ROLE_SERVER
	})
	public @interface RoleDef {
	}

	public interface Callback {

		void onStart(@RoleDef int role, String ipAddress);

		void onStop();

		void onError();


		void onClipChanged(ClipData clipData);

	}


	private static final int CALLBACK_SIGNAL_ON_START        = 1;
	private static final int CALLBACK_SIGNAL_ON_STOP         = 2;
	private static final int CALLBACK_SIGNAL_ON_ERROR        = 3;
	private static final int CALLBACK_SIGNAL_ON_CLIP_CHANGED = 4;


	private static final String ACTION_SERVICE_CALLBACK = TAG + ".action.SERVICE_CALLBACK";

	private static final String EXTRA_CALLBACK_SIGNAL = "CopyPasteService.extraCallbackSignal";
	private static final String EXTRA_CLIP            = "CopyPasteService.extraClip";
	private static final String EXTRA_IP_ADDRESS      = "CopyPasteService.extraIPAddress";
	private static final String EXTRA_ROLE            = "CopyPasteService.extraRole";

	private static final class CallbackBroadcastReceiver extends BroadcastReceiver {

		final Set<Callback> callbacks = new HashSet<>();

		private void forEachCallback(Delegate<Callback> callbackDelegate) {
			//noinspection Convert2streamapi
			for (Callback callback : new LinkedList<>(callbacks)) {
				callbackDelegate.invoke(callback);
			}
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			switch (intent.getIntExtra(EXTRA_CALLBACK_SIGNAL, 0)) {
				case CALLBACK_SIGNAL_ON_START: {
					final int role = intent.getIntExtra(EXTRA_ROLE, ROLE_UNKNOWN);

					forEachCallback(
						target -> target.onStart(role, intent.getStringExtra(EXTRA_IP_ADDRESS)));
					break;
				}
				case CALLBACK_SIGNAL_ON_STOP:
					forEachCallback(Callback::onStop);
					break;
				case CALLBACK_SIGNAL_ON_ERROR:
					forEachCallback(Callback::onError);
					break;
				case CALLBACK_SIGNAL_ON_CLIP_CHANGED: {
					final ClipData clipData = intent.getParcelableExtra(EXTRA_CLIP);

					forEachCallback(target -> target.onClipChanged(clipData));
					break;
				}
			}
		}
	}

	private static final Lazy<CallbackBroadcastReceiver> callbackReceiverLazy
		= new Lazy<CallbackBroadcastReceiver>() {

		@Override
		protected CallbackBroadcastReceiver initialize() {
			return new CallbackBroadcastReceiver();
		}
	};

	public static void registerCallback(Callback callback) {
		boolean needRegister = callbackReceiverLazy.isEmpty();

		CallbackBroadcastReceiver callbackBroadcastReceiver = callbackReceiverLazy.get();

		if (needRegister) {
			LocalBroadcastManager.getInstance(application)
				.registerReceiver(callbackBroadcastReceiver,
					new IntentFilter(ACTION_SERVICE_CALLBACK));
		}
		callbackBroadcastReceiver.callbacks.add(callback);
	}

	public static void unregisterCallback(Callback callback) {
		if (!callbackReceiverLazy.isEmpty()) {
			CallbackBroadcastReceiver callbackBroadcastReceiver = callbackReceiverLazy.get();

			if (callbackBroadcastReceiver.callbacks.remove(callback)
				&& callbackBroadcastReceiver.callbacks.isEmpty()) {

				LocalBroadcastManager.getInstance(application)
					.unregisterReceiver(callbackBroadcastReceiver);
			}
		}
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
			ClipData clipData = new ClipData(TAG, new String[]{NanoHTTPD.MIME_PLAINTEXT},
				new ClipData.Item(clipText));

			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(clipData);

			Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
				CopyPasteService.class);
			{
				broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_CLIP_CHANGED);
				broadcast.putExtra(EXTRA_CLIP, clipData);
			}
			LocalBroadcastManager.getInstance(CopyPasteService.this)
				.sendBroadcast(broadcast);
		}
	}


	private final class ClipboardClient extends WebSocketClient
		implements ClipboardManager.OnPrimaryClipChangedListener {

		private final Handler handler;

		public ClipboardClient(Uri serverURI) {
			super(URI.create(serverURI.toString()), new Draft_17());

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
			((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)) // preserve new line
				.addPrimaryClipChangedListener(this);
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
			Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
				CopyPasteService.class);
			{
				broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_ERROR);
			}
			LocalBroadcastManager.getInstance(CopyPasteService.this)
				.sendBroadcast(broadcast);
			setRunningAction(CopyPasteService.this, null);
		}

		@Override
		public void onPrimaryClipChanged() {
			send(getClipValue());
		}
	}


	private static final class ClipboardData {

		public int port;

		public String host;

		public String text;

	}


	private ClipboardClient client;

	private int onStartClient(Intent intent) {
		ThreadPoolExecutor executor = new ThreadPoolExecutor(2, 4, 1, TimeUnit.MINUTES,
			new LinkedBlockingQueue<>());

		executor.execute(() -> {
			try {
				URLConnection connection = new URL(
					intent.getDataString() + "/clipboard").openConnection();

				connection.connect();

				BufferedReader is = new BufferedReader(
					new InputStreamReader(connection.getInputStream()));

				try {
					StringBuilder sb = new StringBuilder();

					String inputLine;
					while ((inputLine = is.readLine()) != null) {
						sb.append(inputLine);
					}
					ClipboardData data = new Gson().fromJson(sb.toString(), ClipboardData.class);
					Handler handler = new Handler(Looper.getMainLooper());

					handler.post(() -> {
						client = new ClipboardClient(
							Uri.parse("ws://" + data.host + ":" + data.port));

						executor.execute(client);
						setRunningAction(this, intent.getAction());

						Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null,
							CopyPasteService.this, CopyPasteService.class);
						{
							broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_START);
							broadcast.putExtra(EXTRA_IP_ADDRESS, data.host);
							broadcast.putExtra(EXTRA_ROLE, ROLE_CLIENT);
						}
						LocalBroadcastManager.getInstance(CopyPasteService.this)
							.sendBroadcast(broadcast);
					});
				}
				finally {
					is.close();
				}
			}
			catch (IOException e) {
				e.printStackTrace();

				Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
					CopyPasteService.class);
				{
					broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_ERROR);
				}
				LocalBroadcastManager.getInstance(CopyPasteService.this)
					.sendBroadcast(broadcast);
			}
		});
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
				setClipValue(handler, message.getTextPayload());
			}

			@Override
			protected void onPong(WebSocketFrame pong) {
				handler.postDelayed(this::ping, 5000);
			}

			@Override
			protected void onException(IOException exception) {
				Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
					CopyPasteService.class);
				{
					broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_ERROR);
				}
				LocalBroadcastManager.getInstance(CopyPasteService.this)
					.sendBroadcast(broadcast);
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

			Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
				CopyPasteService.class);
			{
				broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_START);
				broadcast.putExtra(EXTRA_IP_ADDRESS, getNetworkAddress());
				broadcast.putExtra(EXTRA_ROLE, ROLE_SERVER);
			}
			LocalBroadcastManager.getInstance(CopyPasteService.this)
				.sendBroadcast(broadcast);
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

	private void onStopService() {
		setRunningAction(this, null);
		stopSelf();

		Intent broadcast = new Intent(ACTION_SERVICE_CALLBACK, null, CopyPasteService.this,
			CopyPasteService.class);
		{
			broadcast.putExtra(EXTRA_CALLBACK_SIGNAL, CALLBACK_SIGNAL_ON_STOP);
		}
		LocalBroadcastManager.getInstance(CopyPasteService.this)
			.sendBroadcast(broadcast);
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
		switch (action) {
			case ACTION_START_CLIENT:
				stopRunningActions(actionCurrent);
				return onStartClient(intent);
			case ACTION_START_SERVER:
				stopRunningActions(actionCurrent);
				return onStartServer(intent);
			case ACTION_STOP:
				stopRunningActions(actionCurrent);
				onStopService();
				return START_NOT_STICKY;
		}
		throw new Error("Unknown action is given.");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		throw new Error("Binding is not supported.");
	}
}
