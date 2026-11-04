package git.artdeell.mojo.modui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shortcuts panel styled after ZenkaiMobile: keys reported by the mod, server commands, favourites and a chat bar. */
public class ModUiDialog extends Dialog implements ModUiServer.Listener {
    private static final int BG = 0xF2171109, ROW = 0xFF1E1710, FIELD = 0xFF120E0B;
    private static final int GOLD = 0xFFB07F2F, ORANGE = 0xFFF0A030, CREAM = 0xFFF1E3C8, MUTED = 0xFF8A7A62;
    private static final String PREF_FAVS = "modui_favs";
    private static final Set<String> HELD = new HashSet<>();

    private static class Item {
        String id, title, sub, right;
        boolean cmd;
        JSONObject raw;
    }

    private final SharedPreferences prefs;
    private final Set<String> favs;
    private final List<Item> shown = new ArrayList<>();
    private final TextView[] tabViews = new TextView[3];
    private int tab = 0;
    private String query = "";
    private BaseAdapter adapter;
    private TextView status;
    private EditText chat;

    public ModUiDialog(Context ctx) {
        super(ctx, android.R.style.Theme_Material_Dialog_NoActionBar);
        prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        favs = new HashSet<>(prefs.getStringSet(PREF_FAVS, new HashSet<>()));
    }

    private int dp(int v) {
        return Math.round(v * getContext().getResources().getDisplayMetrics().density);
    }

    private GradientDrawable box(int fill, int stroke, int strokeDp, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        if (strokeDp > 0) g.setStroke(dp(strokeDp), stroke);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView iconButton(String glyph) {
        TextView b = text(glyph, 18, CREAM, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(box(ROW, GOLD, 1, 5));
        return b;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            android.util.DisplayMetrics m = getContext().getResources().getDisplayMetrics();
            w.setLayout(Math.min(dp(430), (int) (m.widthPixels * 0.62f)), (int) (m.heightPixels * 0.94f));
            w.setDimAmount(0.25f);
        }
        setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(box(BG, GOLD, 2, 6));
        root.setPadding(dp(10), dp(8), dp(10), dp(8));

        // Header
        LinearLayout head = new LinearLayout(getContext());
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(getContext());
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(text("MODUI", 8, MUTED, true));
        titles.addView(text("SHORTCUTS", 17, CREAM, true));
        head.addView(titles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView esc = text("Esc", 13, CREAM, true);
        esc.setGravity(Gravity.CENTER);
        esc.setBackground(box(ROW, GOLD, 1, 5));
        esc.setOnClickListener(v -> ModUiServer.INSTANCE.closeScreen());
        head.addView(esc, new LinearLayout.LayoutParams(dp(52), dp(36)));
        TextView close = iconButton("✕");
        close.setOnClickListener(v -> dismiss());
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(36), dp(36));
        cp.leftMargin = dp(6);
        head.addView(close, cp);
        root.addView(head);

        // Tabs
        LinearLayout tabs = new LinearLayout(getContext());
        tabs.setPadding(0, dp(6), 0, dp(2));
        String[] names = {"Teclas", "Comandos", "FAVS"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            TextView t = text(names[i], 14, CREAM, false);
            t.setPadding(dp(4), dp(4), dp(14), dp(4));
            t.setOnClickListener(v -> { tab = idx; rebuild(); });
            tabViews[i] = t;
            tabs.addView(t);
        }
        root.addView(tabs);

        // Search
        EditText search = new EditText(getContext());
        search.setHint("Find an action...");
        search.setHintTextColor(MUTED);
        search.setTextColor(CREAM);
        search.setSingleLine();
        search.setTextSize(14);
        search.setBackground(box(FIELD, GOLD, 1, 3));
        search.setPadding(dp(10), dp(8), dp(10), dp(8));
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            public void onTextChanged(CharSequence s, int a, int b, int c) { }
            public void afterTextChanged(Editable e) {
                query = e.toString().toLowerCase(Locale.ROOT).trim();
                rebuild();
            }
        });
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = dp(4);
        root.addView(search, sp);

        // List
        ListView listView = new ListView(getContext());
        listView.setDivider(new ColorDrawable(0x33B07F2F));
        listView.setDividerHeight(dp(1));
        listView.setVerticalScrollBarEnabled(false);
        adapter = new RowAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> onTap(shown.get(pos)));
        listView.setOnItemLongClickListener((p, v, pos, id) -> onHold(shown.get(pos)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        lp.topMargin = dp(6);
        root.addView(listView, lp);

        // Chat / run commands bar
        LinearLayout bar = new LinearLayout(getContext());
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackground(box(0xFF2A1F12, ORANGE, 1, 4));
        chat = new EditText(getContext());
        chat.setHint("CHAT / RUN COMMANDS");
        chat.setHintTextColor(CREAM);
        chat.setTextColor(CREAM);
        chat.setTextSize(14);
        chat.setSingleLine();
        chat.setInputType(InputType.TYPE_CLASS_TEXT);
        chat.setImeOptions(EditorInfo.IME_ACTION_SEND);
        chat.setBackground(null);
        chat.setPadding(dp(12), dp(10), dp(8), dp(10));
        chat.setOnEditorActionListener((v, actionId, ev) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) { sendBar(); return true; }
            return false;
        });
        TextView send = text("➤", 18, ORANGE, true);
        send.setGravity(Gravity.CENTER);
        send.setOnClickListener(v -> sendBar());
        bar.addView(chat, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(send, new LinearLayout.LayoutParams(dp(44), dp(40)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(6);
        root.addView(bar, bp);

        status = text("", 10, MUTED, false);
        status.setPadding(dp(2), dp(4), 0, 0);
        root.addView(status);

        setContentView(root);
    }

    @Override
    protected void onStart() {
        super.onStart();
        ModUiServer.INSTANCE.listener = this;
        rebuild();
    }

    @Override
    protected void onStop() {
        if (ModUiServer.INSTANCE.listener == this) ModUiServer.INSTANCE.listener = null;
        super.onStop();
    }

    @Override
    public void onModUiUpdate() {
        rebuild();
    }

    private void rebuild() {
        ModUiServer s = ModUiServer.INSTANCE;
        for (int i = 0; i < 3; i++) {
            TextView t = tabViews[i];
            t.setTextColor(i == tab ? ORANGE : CREAM);
            t.setPaintFlags(i == tab ? t.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG
                    : t.getPaintFlags() & ~Paint.UNDERLINE_TEXT_FLAG);
        }
        if (s.connected) {
            String scr = s.screen.isEmpty() ? "mundo" : s.screen.substring(s.screen.lastIndexOf('.') + 1);
            status.setText("Mod conectado · " + scr + " · mantén pulsada una tecla para dejarla apretada");
        } else {
            status.setText("Esperando al mod ModUi Bridge… (instálalo en la instancia y abre el mundo)");
        }

        List<Item> all = new ArrayList<>();
        JSONArray binds = s.binds;
        for (int i = 0; i < binds.length(); i++) {
            JSONObject b = binds.optJSONObject(i);
            if (b == null || b.optInt("glfw", -1) < 0) continue;
            Item it = new Item();
            it.id = "k:" + b.optString("name");
            it.title = b.optString("title");
            it.sub = b.optString("cat");
            it.right = b.optString("label");
            it.raw = b;
            all.add(it);
        }
        JSONArray cmds = s.commands;
        for (int i = 0; i < cmds.length(); i++) {
            JSONObject c = cmds.optJSONObject(i);
            if (c == null) continue;
            Item it = new Item();
            it.cmd = true;
            it.id = "c:" + c.optString("n");
            it.title = "/" + c.optString("n");
            JSONArray subs = c.optJSONArray("subs");
            boolean hasSubs = subs != null && subs.length() > 0;
            it.sub = hasSubs ? "Subcomandos" : (c.optBoolean("args") ? "Requiere argumentos" : "Ejecutar");
            it.right = hasSubs ? "›" : "↗";
            it.raw = c;
            all.add(it);
        }

        shown.clear();
        for (Item it : all) {
            if (tab == 0 && it.cmd) continue;
            if (tab == 1 && !it.cmd) continue;
            if (tab == 2 && !favs.contains(it.id)) continue;
            String hay = (it.title + " " + it.sub).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !hay.contains(query)) continue;
            shown.add(it);
        }
        adapter.notifyDataSetChanged();
    }

    private void onTap(Item it) {
        if (!it.cmd) {
            if (HELD.remove(it.id)) ModUiInput.press(it.raw, false);
            else ModUiInput.tap(it.raw);
            return;
        }
        String name = it.raw.optString("n");
        JSONArray subs = it.raw.optJSONArray("subs");
        if (subs != null && subs.length() > 0) {
            String[] opts = new String[subs.length() + 1];
            opts[0] = "/" + name;
            for (int i = 0; i < subs.length(); i++) opts[i + 1] = "/" + name + " " + subs.optString(i);
            new AlertDialog.Builder(getContext())
                    .setItems(opts, (d, which) -> fillBar(opts[which] + " "))
                    .show();
        } else if (it.raw.optBoolean("run") && !it.raw.optBoolean("args")) {
            ModUiServer.INSTANCE.sendCommand(name);
            Toast.makeText(getContext(), "/" + name, Toast.LENGTH_SHORT).show();
        } else {
            fillBar("/" + name + " ");
        }
    }

    private boolean onHold(Item it) {
        if (it.cmd) return false;
        if (HELD.remove(it.id)) {
            ModUiInput.press(it.raw, false);
        } else {
            HELD.add(it.id);
            ModUiInput.press(it.raw, true);
        }
        adapter.notifyDataSetChanged();
        return true;
    }

    private void fillBar(String s) {
        chat.setText(s);
        chat.setSelection(s.length());
        chat.requestFocus();
    }

    private void sendBar() {
        String t = chat.getText().toString().trim();
        if (t.isEmpty()) return;
        if (t.length() > 256) {
            Toast.makeText(getContext(), "Máximo 256 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }
        if (t.startsWith("/")) ModUiServer.INSTANCE.sendCommand(t.substring(1));
        else ModUiServer.INSTANCE.sendChat(t);
        chat.setText("");
    }

    private void toggleFav(Item it) {
        if (!favs.remove(it.id)) favs.add(it.id);
        prefs.edit().putStringSet(PREF_FAVS, new HashSet<>(favs)).apply();
        rebuild();
    }

    private class RowAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            Item it = shown.get(pos);
            LinearLayout row = new LinearLayout(getContext());
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(6), dp(8), dp(4), dp(8));

            TextView icon = text(it.cmd ? "⌁" : "⌨", 18, GOLD, true);
            icon.setGravity(Gravity.CENTER);
            row.addView(icon, new LinearLayout.LayoutParams(dp(30), ViewGroup.LayoutParams.WRAP_CONTENT));

            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(text(it.title, 15, CREAM, true));
            if (!it.sub.isEmpty()) col.addView(text(it.sub, 11, MUTED, false));
            LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            cl.leftMargin = dp(4);
            row.addView(col, cl);

            boolean held = HELD.contains(it.id);
            TextView right = text(held ? "● " + it.right : it.right, 14, held ? ORANGE : MUTED, false);
            right.setPadding(dp(8), 0, dp(10), 0);
            row.addView(right);

            TextView star = text(favs.contains(it.id) ? "★" : "☆", 22, favs.contains(it.id) ? ORANGE : MUTED, false);
            star.setPadding(dp(6), dp(2), dp(6), dp(2));
            star.setOnClickListener(v -> toggleFav(it));
            row.addView(star);
            return row;
        }
    }
                     }
