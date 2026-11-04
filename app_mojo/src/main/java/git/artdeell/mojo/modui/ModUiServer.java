package git.artdeell.mojo.modui;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Local socket bridge between the launcher and the ModUi Bridge mod.
 * Protocol: one JSON object per line, UTF-8. The mod connects and sends a hello with the session token.
 */
public class ModUiServer {
    public static final ModUiServer INSTANCE = new ModUiServer();
    private static final String TAG = "ModUiServer";

    public interface Listener {
        void onModUiUpdate();
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService writer = Executors.newSingleThreadExecutor();
    private ServerSocket server;
    private String token;
    private volatile Writer out;

    public volatile boolean connected;
    public volatile String screen = "";
    public volatile boolean inWorld;
    public volatile JSONArray binds = new JSONArray();
    public volatile JSONArray commands = new JSONArray();
    public volatile Listener listener;
    /** Called on the main thread when the reported screen or world state changes. */
    public volatile Runnable contextListener;

    /** Starts the server once and returns the JVM arguments the game needs. */
    public synchronized List<String> start() throws IOException {
        if (server == null || server.isClosed()) {
            server = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            byte[] raw = new byte[16];
            new SecureRandom().nextBytes(raw);
            StringBuilder sb = new StringBuilder();
            for (byte b : raw) sb.append(String.format("%02x", b));
            token = sb.toString();
            Thread t = new Thread(this::acceptLoop, "ModUiServer");
            t.setDaemon(true);
            t.start();
        }
        List<String> args = new ArrayList<>();
        args.add("-Dmodui.port=" + server.getLocalPort());
        args.add("-Dmodui.token=" + token);
        return args;
    }

    private void acceptLoop() {
        while (true) {
            Socket s;
            try {
                s = server.accept();
            } catch (IOException e) {
                Log.w(TAG, "accept failed", e);
                return;
            }
            serve(s);
        }
    }

    private void serve(Socket s) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {
            s.setSoTimeout(5000);
            String line = r.readLine();
            if (line == null || !isHello(line)) return;
            s.setSoTimeout(0);
            out = new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8);
            connected = true;
            notifyUi();
            while ((line = r.readLine()) != null) handle(line);
        } catch (Exception e) {
            Log.w(TAG, "client error", e);
        } finally {
            connected = false;
            out = null;
            screen = "";
            inWorld = false;
            try { s.close(); } catch (IOException ignored) { }
            notifyUi();
        }
    }

    private boolean isHello(String line) {
        try {
            JSONObject o = new JSONObject(line);
            return "hello".equals(o.optString("t")) && token.equals(o.optString("token"));
        } catch (Exception e) {
            return false;
        }
    }

    private void handle(String line) {
        try {
            JSONObject o = new JSONObject(line);
            switch (o.optString("t")) {
                case "ctx":
                    screen = o.optString("screen");
                    inWorld = o.optBoolean("world");
                    break;
                case "binds": {
                    JSONArray a = o.optJSONArray("list");
                    binds = a != null ? a : new JSONArray();
                    break;
                }
                case "cmds": {
                    JSONArray a = o.optJSONArray("list");
                    commands = a != null ? a : new JSONArray();
                    break;
                }
                default:
                    return;
            }
            notifyUi();
        } catch (Exception e) {
            Log.w(TAG, "bad message", e);
        }
    }

    private void notifyUi() {
        mainHandler.post(() -> {
            Listener l = listener;
            if (l != null) l.onModUiUpdate();
            Runnable c = contextListener;
            if (c != null) c.run();
        });
    }

    private void send(JSONObject o) {
        writer.execute(() -> {
            Writer w = out;
            if (w == null) return;
            try {
                synchronized (w) {
                    w.write(o.toString());
                    w.write('\n');
                    w.flush();
                }
            } catch (IOException e) {
                Log.w(TAG, "send failed", e);
            }
        });
    }

    public void sendChat(String text) {
        try { send(new JSONObject().put("t", "chat").put("text", text)); } catch (Exception ignored) { }
    }

    public void sendCommand(String text) {
        try { send(new JSONObject().put("t", "cmd").put("text", text)); } catch (Exception ignored) { }
    }

    public void closeScreen() {
        try { send(new JSONObject().put("t", "close")); } catch (Exception ignored) { }
    }

    public void selectHotbar(int slot) {
        try { send(new JSONObject().put("t", "hotbar").put("slot", slot)); } catch (Exception ignored) { }
    }
}
