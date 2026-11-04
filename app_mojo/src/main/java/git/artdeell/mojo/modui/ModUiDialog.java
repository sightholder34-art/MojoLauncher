package git.artdeell.mojo.modui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
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
import android.widget.BaseAdapter;
import android.widget.Button;
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

/** Native shortcuts drawer: keys reported by the mod, server commands, favourites and a chat bar. */
public class ModUiDialog extends Dialog implements ModUiServer.Listener {
    private static final int INK = 0xFF121826, INK2 = 0xFF1B2436, LINE = 0xFF2E3B57;
    private static final int ACC = 0xFF4FD1C5, ACC2 = 0xFFF2B84B, TXT = 0xFFE8EDF7, MUT = 0xFF8C9AB5;
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
    private final Button[] tabButtons = new Button[3];
    private int tab = 1;
    private String query = "";
    private ListView listView;
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

    private GradientDrawable box(int fill, int stroke, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setStroke(dp(1), stroke);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(getContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    private Button button(String s) {
        Button b = new Button(getContext());
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(TXT);
        b.setBackground(box(INK2, LINE, 9));
        b.setMinWidth(0);
        b.setMinimumWidth(0);
        b.setPadding(dp(12), 0, dp(12), 0);
        return b;
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            w.setGravity(Gravity.START);
            w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            int screenW = getContext().getResources().getDisplayMetrics().widthPixels;
            w.setLayout(Math.min(dp(540), (int) (screenW * 0.94f)), ViewGroup.LayoutParams.MATCH_PARENT);
        }
        setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(box(INK, LINE, 14));
        root.setPadding(dp(10), dp(10), dp(10), dp(10));

        LinearLayout head = new LinearLayout(getContext());
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("Atajos", 18, TXT);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button esc = button("Esc");
        esc.setOnClickListener(v -> ModUiServer.INSTANCE.closeScreen());
        Button close = button("✕");
        close.setOnClickListener(v -> dismiss());
        head.addView(title);
        head.addView(esc);
        head.addView(close);
        root.addView(head);

        status = text("", 12, MUT);
        root.addView(status);

        LinearLayout tabs = new LinearLayout(getContext());
        String[] names = {"★ Favoritos", "Teclas", "Comandos"};
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            Button b = button(names[i]);
            b.setOnClickListener(v -> { tab = idx; rebuild(); });
            tabButtons[i] = b;
            tabs.addView(b, new LinearLayout.LayoutParams(0, dp(40), 1));
        }
        root.addView(tabs);

        EditText search = new EditText(getContext());
        search.setHint("Buscar");
        search.setHintTextColor(MUT);
        search.setTextColor(TXT);
        search.setSingleLine();
        search.setBackground(box(INK2, LINE, 9));
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
        sp.topMargin = dp(8);
        root.addView(search, sp);

        listView = new ListView(getContext());
        listView.setDivider(new ColorDrawable(LINE));
        listView.setDividerHeight(dp(1));
        adapter = new RowAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((p, v, pos, id) -> onTap(shown.get(pos)));
        listView.setOnItemLongClickListener((p, v, pos, id) -> onHold(shown.get(pos)));
        root.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout bar = new LinearLayout(getContext());
        bar.setGravity(Gravity.CENTER_VERTICAL);
        chat = new EditText(getContext());
        chat.setHint("CHAT / RUN COMMANDS");
        chat.setHintTextColor(MUT);
        chat.setTextColor(TXT);
        chat.setSingleLine();
        chat.setInputType(InputType.TYPE_CLASS_TEXT);
        chat.setBackground(box(INK2, LINE, 9));
        chat.setPadding(dp(10), dp(8), dp(10), dp(8));
        Button send = button("Enviar");
        send.setOnClickListener(v -> sendBar());
        bar.addView(chat, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        bar.addView(send, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, dp(40)));
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(8);
        root.addView(bar, bp);

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
            tabButtons[i].setTextColor(i == tab ? ACC : TXT);
        }
        if (s.connected) {
            String scr = s.screen.isEmpty() ? "mundo" : s.screen.substring(s.screen.lastIndexOf('.') + 1);
            status.setText("Mod conectado · " + scr);
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
            it.sub = subs != null && subs.length() > 0 ? "›" : (c.optBoolean("args") ? "↗ con argumentos" : "↗");
            it.right = subs != null && subs.length() > 0 ? "›" : "↗";
            it.raw = c;
            all.add(it);
        }

        shown.clear();
        for (Item it : all) {
            if (tab == 0 && !favs.contains(it.id)) continue;
            if (tab == 1 && it.cmd) continue;
            if (tab == 2 && !it.cmd) continue;
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
            row.setPadding(dp(4), dp(6), dp(4), dp(6));

            TextView star = text(favs.contains(it.id) ? "★" : "☆", 20, ACC2);
            star.setPadding(dp(8), dp(4), dp(12), dp(4));
            star.setOnClickListener(v -> toggleFav(it));
            row.addView(star);

            LinearLayout col = new LinearLayout(getContext());
            col.setOrientation(LinearLayout.VERTICAL);
            col.addView(text(it.title, 15, TXT));
            if (!it.sub.isEmpty()) col.addView(text(it.sub, 11, MUT));
            row.addView(col, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            boolean held = HELD.contains(it.id);
            TextView right = text(held ? "● " + it.right : it.right, 13, held ? ACC2 : ACC);
            right.setPadding(dp(8), 0, dp(8), 0);
            row.addView(right);
            return row;
        }
    }
  }
