package git.artdeell.mojo.input.buttons;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.input.ControlData;
import git.artdeell.mojo.modui.ModUiInput;
import git.artdeell.mojo.modui.ModUiServer;
import org.json.JSONObject;

/** Dark panel with a title, a close button and option tiles, like the Zenkai menus (Flight, Ki attacks, Energy & form). */
public class ZenkaiPopup extends LinearLayout {
    static final int INK = Color.rgb(242, 232, 207);
    static final int GOLD = Color.rgb(214, 160, 70);
    static final int MUTED = Color.rgb(160, 148, 124);

    public interface OnPick { void onPick(ControlData.ZenkaiOpcao op); }

    private final Runnable mOnClose;
    private final OnPick mOnPick;

    public ZenkaiPopup(Context context, ControlData.ZenkaiMenu menu, Runnable onClose, OnPick onPick) {
        super(context);
        mOnClose = onClose;
        mOnPick = menu.selecionar ? onPick : null;
        setOrientation(VERTICAL);
        setClickable(true); // swallow touches so they do not reach the game
        int pad = (int) Tools.dpToPx(8f);
        setPadding(pad, pad, pad, pad);
        setBackground(box(Color.argb(240, 26, 21, 16), GOLD, 6f, 1.5f));

        int cols = Math.max(1, menu.colunas);
        int cellW = (int) Tools.dpToPx(cols == 1 ? 210f : (cols == 2 ? 104f : 74f));
        int gap = (int) Tools.dpToPx(4f);

        // header
        FrameLayout header = new FrameLayout(context);
        TextView title = new TextView(context);
        title.setText(menu.titulo == null ? "" : menu.titulo.toUpperCase());
        title.setTextColor(GOLD);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextSize(11f);
        title.setLetterSpacing(0.08f);
        header.addView(title, new FrameLayout.LayoutParams(-2, -2, Gravity.START | Gravity.CENTER_VERTICAL));
        View close = new GlyphView(context, "close", null, null, false);
        close.setBackground(box(Color.argb(200, 48, 38, 28), GOLD, 4f, 1f));
        close.setOnClickListener(v -> mOnClose.run());
        int cs = (int) Tools.dpToPx(26f);
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(cs, cs, Gravity.END);
        header.addView(close, clp);
        addView(header, new LayoutParams(-1, -2));

        // option rows
        LinearLayout row = null;
        int used = 0;
        for (ControlData.ZenkaiOpcao op : menu.opcoes) {
            boolean wide = op.largo || cols == 1;
            if (wide || row == null || used >= cols) {
                row = new LinearLayout(context);
                row.setOrientation(HORIZONTAL);
                LayoutParams rlp = new LayoutParams(-1, -2);
                rlp.topMargin = gap;
                addView(row, rlp);
                used = 0;
            }
            GlyphView tile = new GlyphView(context, op.icone, op.nome, op.dica, !wide);
            tile.setBackground(box(Color.argb(235, 44, 35, 26), Color.argb(255, 96, 74, 44), 4f, 1f));
            bindOption(tile, op);
            int h = (int) Tools.dpToPx(wide ? 46f : 58f);
            LayoutParams tlp;
            if (wide) {
                tlp = new LayoutParams(cols == 1 ? cellW : cellW * cols + gap * (cols - 1), h);
                used = cols;
            } else {
                tlp = new LayoutParams(cellW, h);
                if (used > 0) tlp.leftMargin = gap;
                used++;
            }
            row.addView(tile, tlp);
        }

        if (menu.nota != null && !menu.nota.isEmpty()) {
            TextView note = new TextView(context);
            note.setText(menu.nota);
            note.setTextColor(MUTED);
            note.setTextSize(8.5f);
            note.setGravity(Gravity.CENTER);
            LayoutParams nlp = new LayoutParams(-1, -2);
            nlp.topMargin = gap * 2;
            addView(note, nlp);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void bindOption(View tile, ControlData.ZenkaiOpcao op) {
        boolean hold = "segurar".equals(op.modo);
        tile.setOnTouchListener((v, e) -> {
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    v.setPressed(true);
                    v.setAlpha(0.7f);
                    if (hold && mOnPick == null) fire(op, true);
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.setPressed(false);
                    v.setAlpha(1f);
                    if (mOnPick != null) { // picker: choosing replaces the quick button, it does not fire the ability
                        if (e.getActionMasked() == MotionEvent.ACTION_UP) { mOnPick.onPick(op); mOnClose.run(); }
                    } else if (hold) {
                        fire(op, false);
                    } else if (e.getActionMasked() == MotionEvent.ACTION_UP) {
                        fire(op, true);
                        v.postDelayed(() -> fire(op, false), 60);
                        mOnClose.run();
                    }
                    return true;
            }
            return true;
        });
    }

    /** Uses the key the mod reports for op.bind; falls back to the fixed keycodes when it is unknown. */
    private void fire(ControlData.ZenkaiOpcao op, boolean down) {
        JSONObject b = ModUiServer.INSTANCE.findBind(op.bind);
        if (b != null) { ModUiInput.press(b, down); return; }
        if (op.bind != null && !op.bind.isEmpty()) ModUiServer.INSTANCE.warnMissing(getContext(), op.bind);
        ControlButton.sendPlainKeys(op.keycodes, down);
    }

    static GradientDrawable box(int fill, int stroke, float radiusDp, float strokeDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setStroke(Math.max(1, (int) Tools.dpToPx(strokeDp)), stroke);
        d.setCornerRadius(Tools.dpToPx(radiusDp));
        return d;
    }

    /** Icon plus up to two lines of text, drawn by hand. Wide = icon left, tile = icon on top. */
    static class GlyphView extends View {
        private final String icon, name, hint;
        private final boolean tile;
        private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);

        GlyphView(Context c, String icon, String name, String hint, boolean tile) {
            super(c);
            this.icon = icon == null ? "" : icon;
            this.name = name;
            this.hint = hint;
            this.tile = tile;
            setClickable(true);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            stroke.setStyle(Paint.Style.STROKE);
            stroke.setStrokeCap(Paint.Cap.ROUND);
            stroke.setStrokeJoin(Paint.Join.ROUND);
            stroke.setColor(name == null ? INK : GOLD);
            if (name == null) { // close button
                stroke.setStrokeWidth(Tools.dpToPx(1.6f));
                ZenkaiIcons.draw(c, stroke, icon, w / 2f, h / 2f, Math.min(w, h) * 0.45f);
                return;
            }
            float is = tile ? Math.min(h * 0.40f, Tools.dpToPx(22f)) : Math.min(h * 0.50f, Tools.dpToPx(24f));
            stroke.setStrokeWidth(Math.max(Tools.dpToPx(1.3f), is * 0.085f));
            if (tile) {
                ZenkaiIcons.draw(c, stroke, icon, w / 2f, h * 0.32f, is);
                text.setTextAlign(Paint.Align.CENTER);
                text.setColor(INK); text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setTextSize(Tools.dpToPx(11f));
                c.drawText(name, w / 2f, h * 0.70f, text);
                if (hint != null) {
                    text.setColor(MUTED); text.setTypeface(Typeface.DEFAULT);
                    text.setTextSize(Tools.dpToPx(8f));
                    c.drawText(hint, w / 2f, h * 0.90f, text);
                }
            } else {
                ZenkaiIcons.draw(c, stroke, icon, Tools.dpToPx(24f), h / 2f, is);
                text.setTextAlign(Paint.Align.LEFT);
                text.setColor(INK); text.setTypeface(Typeface.DEFAULT_BOLD);
                text.setTextSize(Tools.dpToPx(14f));
                float x = Tools.dpToPx(48f);
                c.drawText(name, x, hint == null ? h * 0.58f : h * 0.46f, text);
                if (hint != null) {
                    text.setColor(MUTED); text.setTypeface(Typeface.DEFAULT);
                    text.setTextSize(Tools.dpToPx(9f));
                    c.drawText(hint, x, h * 0.74f, text);
                }
            }
        }
    }
}
