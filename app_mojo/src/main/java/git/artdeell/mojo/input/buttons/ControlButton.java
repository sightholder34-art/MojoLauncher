package git.artdeell.mojo.input.buttons;

import static git.artdeell.mojo.CallbackBridge.sendMouseButton;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import git.artdeell.mojo.game.GameActivity;

import git.artdeell.mojo.R;

import git.artdeell.mojo.Tools;
import git.artdeell.mojo.input.ControlData;
import git.artdeell.mojo.input.ControlLayout;
import git.artdeell.mojo.input.handleview.EditControlSideDialog;
import git.artdeell.mojo.prefs.LauncherPreferences;

import git.artdeell.mojo.CallbackBridge;
import git.artdeell.mojo.modui.ModUiInput;
import git.artdeell.mojo.modui.ModUiServer;
import org.json.JSONObject;

import static git.artdeell.mojo.input.buttons.BackgroundTint.DEFAULT_TINT_LIST;
import static git.artdeell.mojo.input.buttons.BackgroundTint.TOGGLE_TINT_LIST;
import static git.artdeell.mojo.game.platform.Platform.PLATFORM;

@SuppressLint({"ViewConstructor", "AppCompatCustomView"})
public class ControlButton extends TextView implements ControlInterface {
    private final Paint mRectPaint = new Paint();
    private final Paint mZenkaiPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mZenkaiTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private boolean mZenkaiStyled;
    private boolean mMenuLongPending;
    private final Runnable mOpenMenuRunnable = () -> {
        mMenuLongPending = false;
        ControlLayout parent = getControlLayoutParent();
        if (parent != null) parent.toggleZenkaiMenu(this);
    };
    private final Runnable mTapReleaseRunnable = () -> sendKeyPresses(false);
    protected ControlData mProperties;
    private final ControlLayout mControlLayout;

    /* Cache value from the ControlData radius for drawing purposes */
    private float mComputedRadius;
    private boolean mHasBitmap;

    protected boolean mIsToggled = false;

    public ControlButton(ControlLayout layout, ControlData properties) {
        super(layout.getContext());
        mControlLayout = layout;
        setGravity(Gravity.CENTER);
        setAllCaps(LauncherPreferences.PREF_BUTTON_ALL_CAPS);
        setTextColor(Color.WHITE);
        setPadding(4, 4, 4, 4);
        setTextSize(14); // Nullify the default size setting
        setOutlineProvider(null); // Disable shadow casting, removing one drawing pass

        //setOnLongClickListener(this);

        //When a button is created, the width/height has yet to be processed to fit the scaling.
        setProperties(preProcessProperties(properties, layout));

        injectBehaviors();
    }

    @Override
    public View getControlView() {return this;}

    public ControlData getProperties() {
        return mProperties;
    }

    private void setupBitmapTint() {
        BackgroundTint.applyToggleTint(getContext());
        ColorStateList tintStateList = mProperties.isToggle ? TOGGLE_TINT_LIST : DEFAULT_TINT_LIST;
        setBackgroundTintList(tintStateList);
        setBackgroundTintMode(PorterDuff.Mode.SRC_ATOP);
    }

    private void setupNormalTint() {
        mComputedRadius = ControlInterface.super.computeCornerRadius(mProperties.cornerRadius);
        setBackgroundTintList(null);
        if (mProperties.isToggle) {
            //For the toggle layer
            final TypedValue value = new TypedValue();
            getContext().getTheme().resolveAttribute(R.attr.colorAccent, value, true);
            mRectPaint.setColor(value.data);
            mRectPaint.setAlpha(BackgroundTint.BACKGROUND_TOGGLE_TINT_ALPHA);
        } else {
            mRectPaint.setColor(Color.WHITE);
            mRectPaint.setAlpha(BackgroundTint.BACKGROUND_DEFAULT_TINT_ALPHA);
        }
    }

    public void setProperties(ControlData properties, boolean changePos) {
        mProperties = properties;
        ControlInterface.super.setProperties(properties, changePos);

        mHasBitmap = Tools.isValidString(mProperties.bitmapTag);

        if(mHasBitmap) setupBitmapTint();
        else setupNormalTint();

        mZenkaiStyled = Tools.isValidString(properties.zenkaiIcone);
        setText(mZenkaiStyled ? "" : properties.name);
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Bitmap uses a tint list, so don't do any custom rendering
        if(mZenkaiStyled) drawZenkaiContent(canvas);
        if(mHasBitmap || !isActivated()) return;
        canvas.drawRoundRect(0, 0, getWidth(), getHeight(), mComputedRadius, mComputedRadius, mRectPaint);
    }

    /** Icon and a small one-line label on top of the regular rounded background. */
    private void drawZenkaiContent(Canvas canvas) {
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        int ink = Color.rgb(242, 232, 207);
        String icon = mProperties.zenkaiIcone;
        String label = mProperties.name == null ? "" : mProperties.name;
        boolean chip = "chip".equals(mProperties.zenkaiEstilo);
        boolean iconOnly = "icon".equals(mProperties.zenkaiEstilo);
        if (hasZenkaiMenu() && !isMenuOnTap()) { // little arrow in the corner: long-press opens a menu
            mZenkaiPaint.setStyle(Paint.Style.STROKE);
            mZenkaiPaint.setStrokeWidth(Tools.dpToPx(1.2f));
            mZenkaiPaint.setColor(ink);
            ZenkaiIcons.draw(canvas, mZenkaiPaint, "chevron", w - Tools.dpToPx(8f), Tools.dpToPx(7f), Tools.dpToPx(9f));
        }
        float iconSize = Math.min(Tools.dpToPx(24f), Math.min(w, h) * (chip ? 0.48f : (iconOnly ? 0.55f : 0.40f)));

        mZenkaiPaint.setStyle(Paint.Style.STROKE);
        mZenkaiPaint.setStrokeWidth(Math.max(Tools.dpToPx(1.5f), iconSize * 0.085f));
        mZenkaiPaint.setStrokeCap(Paint.Cap.ROUND);
        mZenkaiPaint.setStrokeJoin(Paint.Join.ROUND);
        mZenkaiPaint.setColor(ink);
        mZenkaiTextPaint.setColor(ink);

        if (iconOnly) {
            ZenkaiIcons.draw(canvas, mZenkaiPaint, icon, w * 0.5f, h * 0.5f, iconSize);
        } else if (chip) {
            ZenkaiIcons.draw(canvas, mZenkaiPaint, icon, Math.max(iconSize * 0.9f, w * 0.26f), h * 0.5f, iconSize * 0.75f);
            mZenkaiTextPaint.setTextAlign(Paint.Align.LEFT);
            fitText(label, w * 0.50f, Math.min(Tools.dpToPx(14f), h * 0.32f));
            canvas.drawText(label, w * 0.44f, h * 0.5f - (mZenkaiTextPaint.descent() + mZenkaiTextPaint.ascent()) / 2f, mZenkaiTextPaint);
        } else {
            ZenkaiIcons.draw(canvas, mZenkaiPaint, icon, w * 0.5f, h * 0.38f, iconSize);
            mZenkaiTextPaint.setTextAlign(Paint.Align.CENTER);
            fitText(label, w * 0.90f, Math.min(Tools.dpToPx(11f), h * 0.21f));
            canvas.drawText(label, w * 0.5f, h * 0.86f, mZenkaiTextPaint);
        }
    }

    public boolean hasZenkaiMenu() {
        return mProperties.zenkaiMenu != null && mProperties.zenkaiMenu.opcoes != null && !mProperties.zenkaiMenu.opcoes.isEmpty();
    }

    private boolean isMenuOnTap() {
        return !"longo".equals(mProperties.zenkaiMenuGatilho);
    }

    /** Press/release plain Android keycodes (the same path normal buttons use); 0 means "not assigned". */
    public static void sendPlainKeys(int[] keycodes, boolean isDown) {
        if (keycodes == null) return;
        for (int keycode : keycodes) {
            if (keycode <= KeyEvent.KEYCODE_UNKNOWN) continue;
            CallbackBridge.setModifiers(keycode, isDown);
            PLATFORM.sendKeyEvent(keycode, isDown ? 1 : 0, CallbackBridge.getCurrentMods());
        }
    }

    private void fitText(String text, float maxWidth, float startSize) {
        float min = Tools.dpToPx(6f);
        mZenkaiTextPaint.setTextSize(startSize);
        while (mZenkaiTextPaint.measureText(text) > maxWidth && mZenkaiTextPaint.getTextSize() > min)
            mZenkaiTextPaint.setTextSize(mZenkaiTextPaint.getTextSize() - Tools.dpToPx(0.5f));
    }


    @Override
    public boolean isActivated() {
        // Any possible side effects?
        return super.isActivated() || (mProperties.isToggle && mIsToggled);
    }

    public void loadEditValues(EditControlSideDialog editControlPopup){
        editControlPopup.loadValues(getProperties());
    }

    /** Add another instance of the ControlButton to the parent layout */
    public void cloneButton(){
        ControlData cloneData = new ControlData(getProperties());
        cloneData.dynamicX = "0.5 * ${screen_width}";
        cloneData.dynamicY = "0.5 * ${screen_height}";
        ((ControlLayout) getParent()).addControlButton(cloneData);
    }

    /** Remove any trace of this button from the layout */
    public void removeButton() {
        ControlLayout parent = getControlLayoutParent();
        if(parent == null) return;
        parent.getLayout().mControlDataList.remove(getProperties());
        parent.removeView(this);
    }

    @Override
    public void handlePressed() {
        ControlLayout layout = getControlLayoutParent();
        if (hasZenkaiMenu() && layout != null && !layout.getModifiable()) {
            if (isMenuOnTap()) { layout.toggleZenkaiMenu(this); return; }
            mMenuLongPending = true;
            postDelayed(mOpenMenuRunnable, 450);
            return;
        }
        if(!getProperties().isToggle){
            sendKeyPresses(true);
        }
    }

    @Override
    public void handleReleased() {
        ControlLayout layout = getControlLayoutParent();
        if (hasZenkaiMenu() && layout != null && !layout.getModifiable()) {
            if (isMenuOnTap()) return;
            if (mMenuLongPending) { // short tap: behave like a normal button
                mMenuLongPending = false;
                removeCallbacks(mOpenMenuRunnable);
                sendKeyPresses(true);
                postDelayed(mTapReleaseRunnable, 60);
            }
            return;
        }
        if(!triggerToggle()) {
            sendKeyPresses(false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        ControlData properties = getProperties();
        int action = event.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP: // 1
            case MotionEvent.ACTION_CANCEL: // 3
            case MotionEvent.ACTION_POINTER_UP: // 6
                if(properties.passThruEnabled){
                    //Send the event to be taken as a mouse action
                    View gameSurface = getControlLayoutParent().getGameSurface();
                    if(gameSurface != null) gameSurface.dispatchTouchEvent(event);
                }
                break;
        }

        if(getProperties().isSwipeable) {
            getControlLayoutParent().onTouch(this, event);
            return true;
        }

        switch (action){
            case MotionEvent.ACTION_DOWN: // 0
            case MotionEvent.ACTION_POINTER_DOWN: // 5
                handlePressed();
                break;
            case MotionEvent.ACTION_UP: // 1
            case MotionEvent.ACTION_CANCEL: // 3
            case MotionEvent.ACTION_POINTER_UP: // 6
                handleReleased();
                break;
            default:
                return false;
        }

        return super.onTouchEvent(event);
    }



    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean triggerToggle(){
        //returns true a the toggle system is triggered
        if(mProperties.isToggle){
            mIsToggled = !mIsToggled;
            invalidate();
            sendKeyPresses(mIsToggled);
            return true;
        }
        return false;
    }

    public void sendKeyPresses(boolean isDown){
        setActivated(isDown);
        if (Tools.isValidString(mProperties.zenkaiBind)) {
            JSONObject bind = ModUiServer.INSTANCE.findBind(mProperties.zenkaiBind);
            if (bind != null) { // the key the mod reports (works even if the player rebinds it)
                ModUiInput.press(bind, isDown);
                return;
            }
            ModUiServer.INSTANCE.warnMissing(getContext(), mProperties.zenkaiBind);
        }
        for(int keycode : mProperties.keycodes){
            if(keycode >= KeyEvent.KEYCODE_UNKNOWN){
                CallbackBridge.setModifiers(keycode, isDown);
                int modifiers = CallbackBridge.getCurrentMods();
                PLATFORM.sendKeyEvent(keycode, isDown ? 1 : 0, modifiers);
            }else{
                Log.i("punjabilauncher", "sendSpecialKey("+keycode+","+isDown+")");
                sendSpecialKey(keycode, isDown);
            }
        }
    }

    private void sendSpecialKey(int keycode, boolean isDown){
        switch (keycode) {
            case ControlData.SPECIALBTN_KEYBOARD:
                if(isDown) GameActivity.switchKeyboardState(false);
                break;

            case ControlData.SPECIALBTN_KEYBOARDPAN:
                if(isDown) GameActivity.switchKeyboardState(true);
                break;

            case ControlData.SPECIALBTN_TOGGLECTRL:
                if(isDown)getControlLayoutParent().toggleControlVisible();
                break;

            case ControlData.SPECIALBTN_VIRTUALMOUSE:
                if(isDown) GameActivity.toggleMouse(getContext());
                break;

            case ControlData.SPECIALBTN_MOUSEPRI:
                sendMouseButton(MotionEvent.BUTTON_PRIMARY, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSEMID:
                sendMouseButton(MotionEvent.BUTTON_TERTIARY, isDown);
                break;

            case ControlData.SPECIALBTN_MOUSESEC:
                sendMouseButton(MotionEvent.BUTTON_SECONDARY, isDown);
                break;

            case ControlData.SPECIALBTN_SCROLLDOWN:
                if (!isDown) CallbackBridge.sendScroll(0, 1d);
                break;

            case ControlData.SPECIALBTN_SCROLLUP:
                if (!isDown) CallbackBridge.sendScroll(0, -1d);
                break;
            case ControlData.SPECIALBTN_MENU:
                mControlLayout.notifyAppMenu();
                break;
            case ControlData.SPECIALBTN_SHORTCUTS:
                if(isDown) new git.artdeell.mojo.modui.ModUiDialog(getContext()).show();
                break;
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }
}
