package git.artdeell.mojo.input;

import static android.content.Context.INPUT_METHOD_SERVICE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Insets;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.JsonSyntaxException;
import com.kdt.pickafile.FileListView;
import com.kdt.pickafile.FileSelectedListener;

import git.artdeell.mojo.game.GameView;

import git.artdeell.mojo.R;
import git.artdeell.mojo.Tools;
import git.artdeell.mojo.input.buttons.ControlButton;
import git.artdeell.mojo.input.buttons.ControlDrawer;
import git.artdeell.mojo.input.buttons.ControlInterface;
import git.artdeell.mojo.input.buttons.ControlJoystick;
import git.artdeell.mojo.input.buttons.ControlSubButton;
import git.artdeell.mojo.input.handleview.ActionRow;
import git.artdeell.mojo.input.handleview.ControlHandleView;
import git.artdeell.mojo.input.handleview.EditControlSideDialog;
import git.artdeell.mojo.game.platform.Platform;
import git.artdeell.mojo.modui.ModUiServer;
import git.artdeell.mojo.input.buttons.ZenkaiPopup;
import git.artdeell.mojo.prefs.LauncherPreferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ControlLayout extends FrameLayout {
	protected CustomControls mLayout;
	/* Accessible when inside the game by ControlInterface implementations, cached for perf. */
	private GameView mGameSurface = null;

	/* Cache to buttons for performance purposes */
	private List<ControlInterface> mButtons;
	private boolean mModifiable = false;
	private boolean mIsModified;
	private boolean mControlVisible = false;

	private float mButtonsOpacity = 1.0f;

	private EditControlSideDialog mControlDialog = null;
	private ControlHandleView mHandleView;
	private ControlButtonMenuListener mMenuListener;
	public ActionRow mActionRow = null;
	public String mLayoutFileName;

	public ControlLayout(Context ctx) {
		super(ctx);
	}

	public ControlLayout(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
	}


	public void loadLayout(String jsonPath) throws IOException, JsonSyntaxException {
		Point size = new Point(getWidth(), getHeight());
        try {
            CustomControls layout = LayoutConverter.loadAndConvertIfNecessary(size, jsonPath);
            loadLayout(layout);
            updateLoadedFileName(jsonPath);
        }catch (IOException | JsonSyntaxException e) {
            // Load an empty layout on exception to avoid breakage when adding buttons in the editor
            CustomControls customControls = new CustomControls();
            customControls.mLayoutBitmaps = LayoutBitmaps.createEmpty();
            loadLayout(customControls);
            throw e;
        }
	}

	public void loadLayout(CustomControls controlLayout) {
		this.mButtonsOpacity = (float) LauncherPreferences.PREF_BUTTON_TRANSPARENCY / 100;
		boolean sanitizedModified = false;
		if(controlLayout != null) {
			sanitizedModified = LayoutSanitizer.sanitizeLayout(controlLayout);
		}
		if(mActionRow == null){
			mActionRow = new ActionRow(getContext());
			addView(mActionRow);
		}

		removeAllButtons();
		if(mLayout != null) {
			mLayout.mControlDataList = null;
			mLayout = null;
		}

		System.gc();
		mapTable.clear();

		// Cleanup buttons only when input layout is null
		if (controlLayout == null) return;

		mLayout = controlLayout;
		

		// Joystick(s) first, to workaround the touch dispatch
		for(ControlJoystickData joystick : mLayout.mJoystickDataList){
			addJoystickView(joystick);
		}

		//CONTROL BUTTON
		for (ControlData button : controlLayout.mControlDataList) {
			addControlView(button);
		}

		//CONTROL DRAWER
		for(ControlDrawerData drawerData : controlLayout.mDrawerDataList){
			ControlDrawer drawer = addDrawerView(drawerData);
			if(mModifiable) drawer.areButtonsVisible = true;
		}

		mLayout.scaledAt = LauncherPreferences.PREF_BUTTONSIZE;

		setModified(sanitizedModified);
		mButtons = null;
		getButtonChildren(); // Force refresh
	} // loadLayout

	//CONTROL BUTTON
	public void addControlButton(ControlData controlButton) {
		mLayout.mControlDataList.add(controlButton);
		addControlView(controlButton);
	}

	private void addControlView(ControlData controlButton) {
		final ControlButton view = new ControlButton(this, controlButton);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

		setModified(true);
	}

	// CONTROL DRAWER
	public void addDrawer(ControlDrawerData drawerData){
		mLayout.mDrawerDataList.add(drawerData);
		addDrawerView();
	}

	private void addDrawerView(){
		addDrawerView(null);
	}

	private ControlDrawer addDrawerView(ControlDrawerData drawerData){

		final ControlDrawer view = new ControlDrawer(this,drawerData == null ? mLayout.mDrawerDataList.get(mLayout.mDrawerDataList.size()-1) : drawerData);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);
		//CONTROL SUB BUTTON
		for (ControlData subButton : view.getDrawerData().buttonProperties) {
			addSubView(view, subButton);
		}

		setModified(true);
		return view;
	}

	//CONTROL SUB-BUTTON
	public void addSubButton(ControlDrawer drawer, ControlData controlButton){
		//Yep there isn't much here
		drawer.getDrawerData().buttonProperties.add(controlButton);
		addSubView(drawer, drawer.getDrawerData().buttonProperties.get(drawer.getDrawerData().buttonProperties.size()-1 ));
	}

	private void addSubView(ControlDrawer drawer, ControlData controlButton){
		final ControlSubButton view = new ControlSubButton(this, controlButton, drawer);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}else{
			view.setVisible(true);
		}

		addView(view);
		drawer.addButton(view);


		setModified(true);
	}

	// JOYSTICK BUTTON
	public void addJoystickButton(ControlJoystickData data){
		mLayout.mJoystickDataList.add(data);
		addJoystickView(data);
	}

	private void addJoystickView(ControlJoystickData data){
		ControlJoystick view = new ControlJoystick(this, data);

		if (!mModifiable) {
			view.setAlpha(view.getProperties().opacity * mButtonsOpacity);
			view.setFocusable(false);
			view.setFocusableInTouchMode(false);
		}
		addView(view);

	}


	private void removeAllButtons() {
		dismissZenkaiMenu();
		for(ControlInterface button : getButtonChildren()){
			removeView(button.getControlView());
		}

		System.gc();
		//i wanna be sure that all the removed Views will be removed after a reload
		//because if frames will slowly go down after many control changes it will be warm and bad
	}

	public void saveLayout(String path) throws Exception {
		mLayout.save(path);
		setModified(false);
	}

	public void toggleControlVisible(){
		mControlVisible = !mControlVisible;
		setControlVisible(mControlVisible);
	}

	public float getLayoutScale(){
		return mLayout.scaledAt;
	}

	public CustomControls getLayout(){
		return mLayout;
	}

	public void setControlVisible(boolean isVisible) {
		if (mModifiable) return; // Not using on custom controls activity

		mControlVisible = isVisible;
		for(ControlInterface button : getButtonChildren()){
            // Avoid going through the JNI each time.
            button.setVisible(shouldShow(button.getProperties(), Platform.isGrabbing()) && isVisible);
		}
	}

	/** Visibility rule shared by every control: Zenkai context buttons follow the ModUi screen, the rest follow the grab state. */
	public boolean shouldShow(ControlData data, boolean isGrabbing) {
		if (data.zenkaiContextos != null && !data.zenkaiContextos.isEmpty() && ModUiServer.INSTANCE.connected) {
			return isZenkaiContextActive(data);
		}
		return (data.displayInGame && isGrabbing) || (data.displayInMenu && !isGrabbing);
	}

	public boolean isZenkaiContextActive(ControlData data) {
		String screen = ModUiServer.INSTANCE.screen;
		screen = screen == null ? "" : screen.toLowerCase(java.util.Locale.ROOT);
		boolean world = ModUiServer.INSTANCE.inWorld;
		for (String raw : data.zenkaiContextos) {
			if (raw == null) continue;
			String c = raw.toLowerCase(java.util.Locale.ROOT);
			switch (c) {
				case "mundo":
					if (world && screen.isEmpty()) return true;
					break;
				case "inventario":
				case "container":
					if (screen.contains("container") || screen.contains("inventory") || screen.contains("chest")
							|| screen.contains("shulker") || screen.contains("merchant") || screen.contains("anvil")
							|| screen.contains("furnace") || screen.contains("hopper")) return true;
					break;
				case "dialogo":
					if (screen.contains("chat") || screen.contains("dialog") || screen.contains("sign")
							|| screen.contains("bookedit")) return true;
					break;
				default:
					if (!c.isEmpty() && screen.contains(c)) return true;
			}
		}
		return false;
	}

	private ZenkaiPopup mZenkaiPopup;
	private ControlButton mZenkaiPopupOwner;

	/** Opens (or closes, when tapped again) the popup menu defined by the button, placed next to it. */
	public void toggleZenkaiMenu(ControlButton owner) {
		boolean same = mZenkaiPopupOwner == owner;
		dismissZenkaiMenu();
		if (same || !owner.hasZenkaiMenu()) return;
		final ZenkaiPopup popup = new ZenkaiPopup(getContext(), owner.getProperties().zenkaiMenu, this::dismissZenkaiMenu,
				op -> applyZenkaiPick(owner.getProperties().zenkaiMenu.alvo, op));
		mZenkaiPopup = popup;
		mZenkaiPopupOwner = owner;
		popup.setVisibility(INVISIBLE);
		popup.setTranslationZ(Tools.dpToPx(40f)); // buttons use translationZ 10: stay above them
		addView(popup, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		popup.post(() -> {
			if (mZenkaiPopup != popup) return;
			int pw = popup.getWidth(), ph = popup.getHeight();
			float m = Tools.dpToPx(6f);
			float ox = owner.getX(), oy = owner.getY(), ow = owner.getWidth(), oh = owner.getHeight();
			// candidate spots: above, below, right, left of the button; keep the one covering the fewest other buttons
			float[][] spots = {
					{ox + ow / 2f - pw / 2f, oy - ph - m},
					{ox + ow / 2f - pw / 2f, oy + oh + m},
					{ox + ow + m, oy + oh / 2f - ph / 2f},
					{ox - pw - m, oy + oh / 2f - ph / 2f}};
			float bestX = 0, bestY = 0, bestScore = Float.MAX_VALUE;
			for (float[] sp : spots) {
				float x = Math.max(m, Math.min(sp[0], getWidth() - pw - m));
				float y = Math.max(m, Math.min(sp[1], getHeight() - ph - m));
				RectF r = new RectF(x, y, x + pw, y + ph);
				float score = overlapArea(r, owner);
				if (r.intersects(ox, oy, ox + ow, oy + oh)) score += pw * ph; // never sit on its own button
				if (score < bestScore) { bestScore = score; bestX = x; bestY = y; }
			}
			popup.setX(bestX);
			popup.setY(bestY);
			popup.setVisibility(VISIBLE);
			dimUnder(new RectF(bestX, bestY, bestX + pw, bestY + ph), owner);
		});
	}

	private final java.util.Map<View, Float> mDimmed = new java.util.HashMap<>();

	private float overlapArea(RectF r, View except) {
		float total = 0;
		for (ControlInterface b : getButtonChildren()) {
			View v = b.getControlView();
			if (v == except || v.getVisibility() != VISIBLE) continue;
			float w = Math.min(r.right, v.getX() + v.getWidth()) - Math.max(r.left, v.getX());
			float h = Math.min(r.bottom, v.getY() + v.getHeight()) - Math.max(r.top, v.getY());
			if (w > 0 && h > 0) total += w * h;
		}
		return total;
	}

	/** Fades the buttons that sit under a menu so they do not show through it; restoreDimmed() undoes it. */
	public void dimUnder(RectF area, View except) {
		restoreDimmed();
		for (ControlInterface b : getButtonChildren()) {
			View v = b.getControlView();
			if (v == except || v.getVisibility() != VISIBLE) continue;
			if (area.intersects(v.getX(), v.getY(), v.getX() + v.getWidth(), v.getY() + v.getHeight())) {
				mDimmed.put(v, v.getAlpha());
				v.setAlpha(0f);
			}
		}
	}

	public void restoreDimmed() {
		for (java.util.Map.Entry<View, Float> e : mDimmed.entrySet()) e.getKey().setAlpha(e.getValue());
		mDimmed.clear();
	}

	/** Opens the Shortcuts window (left side of the screen) and hides the buttons it would cover. */
	public void showShortcutsDialog() {
		git.artdeell.mojo.modui.ModUiDialog dialog = new git.artdeell.mojo.modui.ModUiDialog(getContext());
		android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
		float w = Math.min(Tools.dpToPx(430f), dm.widthPixels * 0.62f);
		dimUnder(new RectF(0, dm.heightPixels * 0.03f, w, dm.heightPixels * 0.97f), null);
		dialog.setOnDismissListener(d -> restoreDimmed());
		dialog.show();
	}

	/** Makes the button called "target" act as the picked option (label, icon and key). */
	private void applyZenkaiPick(String target, ControlData.ZenkaiOpcao op) {
		if (target == null) return;
		for (ControlInterface b : getButtonChildren()) {
			if (b instanceof ControlButton && target.equals(b.getProperties().name)) {
				((ControlButton) b).setZenkaiSelection(op, true);
			}
		}
	}

	public void dismissZenkaiMenu() {
		restoreDimmed();
		if (mZenkaiPopup != null) removeView(mZenkaiPopup);
		mZenkaiPopup = null;
		mZenkaiPopupOwner = null;
	}

	private final Runnable mContextRefresh = () -> {
		if (mModifiable || mLayout == null) return;
		for (ControlInterface button : getButtonChildren()) {
			button.setVisible(shouldShow(button.getProperties(), Platform.isGrabbing()) && mControlVisible);
		}
	};

	@Override
	protected void onAttachedToWindow() {
		super.onAttachedToWindow();
		ModUiServer.INSTANCE.contextListener = mContextRefresh;
		// Diagnostic: shows whether this build and layout carry the Zenkai data (remove once everything works)
		postDelayed(() -> {
			int styled = 0, menus = 0;
			for (ControlInterface b : getButtonChildren()) {
				ControlData d = b.getProperties();
				if (Tools.isValidString(d.zenkaiIcone)) styled++;
				if (d.zenkaiMenu != null) menus++;
			}
			android.widget.Toast.makeText(getContext(), "Zenkai patch v9 | styled " + styled + " | menus " + menus
					+ " | binds " + ModUiServer.INSTANCE.binds.length(), android.widget.Toast.LENGTH_LONG).show();
		}, 3000);
	}

	@Override
	protected void onDetachedFromWindow() {
		super.onDetachedFromWindow();
		if (ModUiServer.INSTANCE.contextListener == mContextRefresh) ModUiServer.INSTANCE.contextListener = null;
	}

	public void setModifiable(boolean isModifiable) {
		if(!isModifiable && mModifiable){
			removeEditWindow();
		}
		mModifiable = isModifiable;
		updateButtonOpacity();
	}

	public boolean getModifiable(){
		return mModifiable;
	}

	public void setModified(boolean isModified) {
		mIsModified = isModified;
	}

	public List<ControlInterface> getButtonChildren(){
		if(mModifiable || mButtons == null){
			mButtons = new ArrayList<>();
			for(int i=0; i<getChildCount(); ++i){
				View v = getChildAt(i);
				if(v instanceof ControlInterface)
					mButtons.add(((ControlInterface) v));
			}
		}

		return mButtons;
	}

	public void refreshControlButtonPositions(){
		requestLayout();
	}

    @Override
    public void onViewRemoved(View child) {
        super.onViewRemoved(child);
        if(child instanceof ControlInterface && mControlDialog != null){
			mControlDialog.disappearColor();
            mControlDialog.disappear(false);
        }
    }

    /**
	 * Load the layout if needed, and pass down the burden of filling values
	 * to the button at hand.
	 */
	public void editControlButton(ControlInterface button){
		if(mControlDialog == null){
			// When the panel is null, it needs to inflate first.
			// So inflate it, then process it on the next frame
			mControlDialog = new EditControlSideDialog(getContext(), this);
			post(() -> editControlButton(button));
			return;
		}

		mControlDialog.internalChanges = true;
		mControlDialog.setCurrentlyEditedButton(button);

		mControlDialog.appear(button.getControlView().getX() + button.getControlView().getWidth()/2f < getWidth()/2f);
		button.loadEditValues(mControlDialog);

		mControlDialog.internalChanges = false;

		mControlDialog.disappearColor();

		if(mHandleView == null){
			mHandleView = new ControlHandleView(getContext());
			addView(mHandleView);
		}
		mHandleView.setControlButton(button);

		//mHandleView.show();
	}

	/** Swap the panel if the button position requires it */
	public void adaptPanelPosition(){
		if(mControlDialog != null) mControlDialog.adaptPanelPosition();
	}


	final HashMap<View, ControlInterface> mapTable = new HashMap<>();

	private static boolean eventInViewBounds(MotionEvent event, View view) {
		float x = event.getX();
		float y = event.getY();
		return x > view.getLeft() && x < view.getRight() && y > view.getTop() && y < view.getBottom();
	}

	//While this is called onTouch, this should only be called from a ControlButton.
	public void onTouch(View v, MotionEvent ev) {
		int action = ev.getActionMasked();
		ControlInterface lastControlButton = mapTable.get(v);

		// Map location to screen coordinates
		ev.offsetLocation(v.getX(), v.getY());


		//Check if the action is cancelling, reset the lastControl button associated to the view
		if (action == MotionEvent.ACTION_UP
				|| action == MotionEvent.ACTION_CANCEL
				|| action == MotionEvent.ACTION_POINTER_UP) {
			if (lastControlButton != null) lastControlButton.handleReleased();
			mapTable.put(v, null);
			return;
		}

		if (action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_DOWN) return;

		//Optimization pass to avoid looking at all children again
		if (lastControlButton != null) {
			if (eventInViewBounds(ev, lastControlButton.getControlView())) {
				return;
			}
		}

		//Release last keys
		if (lastControlButton != null) lastControlButton.handleReleased();
		mapTable.remove(v);

		// Update the state of all swipeable buttons
		for (ControlInterface button : getButtonChildren()) {
			if (!button.getProperties().isSwipeable) continue;
			if (eventInViewBounds(ev, button.getControlView())) {
				//Press the new key
				if (!button.equals(lastControlButton)) {
					button.handlePressed();
					mapTable.put(v, button);
					return;
				}

			}
		}
	}

    @RequiresApi(30)
    private boolean isKeyboardShown() {
        WindowInsets windowInsets = getRootWindowInsets();
        Insets imeInsets = windowInsets.getInsets(WindowInsets.Type.ime());
        return imeInsets.bottom != 0 || imeInsets.left != 0 || imeInsets.top != 0 || imeInsets.right != 0;
    }

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (mModifiable && event.getActionMasked() != MotionEvent.ACTION_UP || mControlDialog == null)
			return true;
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

        boolean isKeyboardHidden;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean keyboardShown = isKeyboardShown();
            isKeyboardHidden = !keyboardShown;
            if(keyboardShown) imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }else {
            // When the input window cannot be hidden (meaning it's already hidden), it returns false
            // Docs don't seem to suggest that this is the case anymore. But it is on a10 and i
            // don't want to mess with the way insets are done on a10
            isKeyboardHidden = !imm.hideSoftInputFromWindow(getWindowToken(), 0);
        }
        if(isKeyboardHidden){
			if(mControlDialog.disappearLayer()){
				mActionRow.setFollowedButton(null);
				mHandleView.hide();
			}
		}
		return true;
	}

	public void removeEditWindow() {
		InputMethodManager imm = (InputMethodManager) getContext().getSystemService(INPUT_METHOD_SERVICE);

		// When the input window cannot be hidden, it returns false
		imm.hideSoftInputFromWindow(getWindowToken(), 0);
		if(mControlDialog != null) {
			mControlDialog.disappearColor();
			mControlDialog.disappear(true);
		}

		if(mActionRow != null) mActionRow.setFollowedButton(null);
		if(mHandleView != null) mHandleView.hide();
	}

	public void save(String path){
		try {
			mLayout.save(path);
		} catch (IOException e) {Log.e("ControlLayout", "Failed to save the layout at:" + path);}
	}


	public boolean hasMenuButton() {
		for(ControlInterface controlInterface : getButtonChildren()){
			for (int keycode : controlInterface.getProperties().keycodes) {
				if (keycode == ControlData.SPECIALBTN_MENU) return true;
			}
		}
		return false;
	}

	public void setMenuListener(ControlButtonMenuListener menuListener) {
		this.mMenuListener = menuListener;
	}

	public void notifyAppMenu() {
		if(mMenuListener != null) mMenuListener.onClickedMenu();
	}

	/** Cached getter for perf purposes */
	public GameView getGameSurface(){
		if(mGameSurface == null){
			mGameSurface = findViewById(R.id.main_game_render_view);
		}
		return mGameSurface;
	}

	public void askToExit(EditorExitable editorExitable) {
		if(mIsModified) {
			openSaveDialog(editorExitable);
		}else{
			openExitDialog(editorExitable);
		}
	}

	public void updateLoadedFileName(String path) {
		path = path.replace(Tools.CTRLMAP_PATH, ".");
		path = path.substring(0, path.length() - 5);
		mLayoutFileName = path;
	}

	public String saveToDirectory(String name) throws Exception{
		String jsonPath = Tools.CTRLMAP_PATH + "/" + name + ".json";
		saveLayout(jsonPath);
		return jsonPath;
	}

	class OnClickExitListener implements View.OnClickListener {
		private final AlertDialog mDialog;
		private final EditText mEditText;
		private final EditorExitable mListener;

		public OnClickExitListener(AlertDialog mDialog, EditText mEditText, EditorExitable mListener) {
			this.mDialog = mDialog;
			this.mEditText = mEditText;
			this.mListener = mListener;
		}

		@Override
		public void onClick(View v) {
			Context context = v.getContext();
			if (mEditText.getText().toString().isEmpty()) {
				mEditText.setError(context.getString(R.string.global_error_field_empty));
				return;
			}
			try {
				String jsonPath = saveToDirectory(mEditText.getText().toString());
				Toast.makeText(context, context.getString(R.string.global_save) + ": " + jsonPath, Toast.LENGTH_SHORT).show();
				mDialog.dismiss();
				if(mListener != null) mListener.exitEditor();
			} catch (Throwable th) {
				Tools.showError(context, th, mListener != null);
			}
		}
	}

	public void openSaveDialog(EditorExitable editorExitable) {
		final Context context = getContext();
		final EditText edit = new EditText(context);
		edit.setSingleLine();
		edit.setText(mLayoutFileName);

		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(R.string.global_save);
		builder.setView(edit);
		builder.setPositiveButton(android.R.string.ok, null);
		builder.setNegativeButton(android.R.string.cancel, null);
		if(editorExitable != null) builder.setNeutralButton(R.string.global_save_and_exit, null);
		final AlertDialog dialog = builder.create();
		dialog.setOnShowListener(dialogInterface -> {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE)
					.setOnClickListener(new OnClickExitListener(dialog, edit, null));
			if(editorExitable != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
					.setOnClickListener(new OnClickExitListener(dialog, edit, editorExitable));
		});
		dialog.show();
	}

	public void openLoadDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.global_load);
		builder.setPositiveButton(android.R.string.cancel, null);

		final AlertDialog dialog = builder.create();
		FileListView flv = new FileListView(dialog, "json");
		if(Build.VERSION.SDK_INT < 29)flv.listFileAt(new File(Tools.CTRLMAP_PATH));
		else flv.lockPathAt(new File(Tools.CTRLMAP_PATH));
		flv.setFileSelectedListener(new FileSelectedListener(){

			@Override
			public void onFileSelected(File file, String path) {
				try {
					loadLayout(path);
				}catch (IOException e) {
					Tools.showError(getContext(), e);
				}
				dialog.dismiss();
			}
		});
		dialog.setView(flv);
		dialog.show();
	}

	public void openSetDefaultDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.customctrl_selectdefault);
		builder.setPositiveButton(android.R.string.cancel, null);

		final AlertDialog dialog = builder.create();
		FileListView flv = new FileListView(dialog, "json");
		flv.lockPathAt(new File(Tools.CTRLMAP_PATH));
		flv.setFileSelectedListener(new FileSelectedListener(){

			@Override
			public void onFileSelected(File file, String path) {
				try {
					LauncherPreferences.DEFAULT_PREF.edit().putString("defaultCtrl", path).apply();
					LauncherPreferences.PREF_DEFAULTCTRL_PATH = path;loadLayout(path);
				}catch (IOException|JsonSyntaxException e) {
					Tools.showError(getContext(), e);
				}
				dialog.dismiss();
			}
		});
		dialog.setView(flv);
		dialog.show();
	}

	public void openExitDialog(EditorExitable exitListener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
		builder.setTitle(R.string.customctrl_editor_exit_title);
		builder.setMessage(R.string.customctrl_editor_exit_msg);
		builder.setPositiveButton(R.string.global_yes, (d,w)->exitListener.exitEditor());
		builder.setNegativeButton(R.string.global_no, (d,w)->{});
		builder.show();
	}

	// Copied from https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/FrameLayout.java
	// (and edited to avoid laying out control buttons)
	@SuppressWarnings("RtlHardcoded") // Handled explicitly via getAbsoluteGravity()
	private void layoutNonButtonChildren(int left, int top, int right, int bottom) {
		final int count = getChildCount();
		final int parentLeft = getPaddingLeft();
		final int parentRight = right - left - getPaddingRight();
		final int parentTop = getPaddingTop();
		final int parentBottom = bottom - top - getPaddingBottom();
		final int layoutDirection = getLayoutDirection();
		for (int i = 0; i < count; i++) {
			final View child = getChildAt(i);
			if(child instanceof ControlInterface || child.getVisibility() == GONE) continue;
			final LayoutParams lp = (LayoutParams) child.getLayoutParams();
			final int width = child.getMeasuredWidth();
			final int height = child.getMeasuredHeight();
			int childLeft, childTop;
			int gravity = lp.gravity;
			if (gravity == -1) {
				gravity = Gravity.START | Gravity.TOP;
			}
			final int absoluteGravity = Gravity.getAbsoluteGravity(gravity, layoutDirection);
			switch (absoluteGravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
				case Gravity.CENTER_HORIZONTAL:
					childLeft = parentLeft + (parentRight - parentLeft - width) / 2 +
							lp.leftMargin - lp.rightMargin;
					break;
				case Gravity.RIGHT:
					childLeft = parentRight - width - lp.rightMargin;
					break;
				case Gravity.LEFT:
				default:
					childLeft = parentLeft + lp.leftMargin;
			}
			switch (gravity & Gravity.VERTICAL_GRAVITY_MASK) {
				case Gravity.TOP:
				default:
					childTop = parentTop + lp.topMargin;
					break;
				case Gravity.CENTER_VERTICAL:
					childTop = parentTop + (parentBottom - parentTop - height) / 2 +
							lp.topMargin - lp.bottomMargin;
					break;
				case Gravity.BOTTOM:
					childTop = parentBottom - height - lp.bottomMargin;
					break;
			}
			child.layout(childLeft, childTop, childLeft + width, childTop + height);
		}
	}

	@Override
	protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
		layoutNonButtonChildren(left, top, right, bottom);
		int w = right - left;
		int h = bottom - top;

		for(ControlInterface controlInterface : getButtonChildren()) {
			ControlData properties = controlInterface.getProperties();
			View interfaceView = controlInterface.getControlView();

			int width = (int) properties.getWidth();
			int height = (int) properties.getHeight();

			interfaceView.measure(
					MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
					MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
			);

			if(!changed && !interfaceView.isLayoutRequested()) {
				interfaceView.layout(
						interfaceView.getLeft(), interfaceView.getTop(),
						interfaceView.getRight(), interfaceView.getBottom()
				);
			} else {
				int l = (int) (properties.insertDynamicPos(properties.dynamicX, w, h) + left);
				int t = (int) (properties.insertDynamicPos(properties.dynamicY, w, h) + top);

				int r = l + width;
				int b = t + height;
				interfaceView.layout(l, t, r, b);
			}
		}
	}

	public boolean areControlVisible(){
		return mControlVisible;
	}

	public LayoutBitmaps getBitmaps() {
		return mLayout.mLayoutBitmaps;
	}

	public void updateButtonOpacity() {
		mButtonsOpacity = Math.clamp((float) LauncherPreferences.PREF_BUTTON_TRANSPARENCY / 100, 0, 1);
		for(ControlInterface button : getButtonChildren()) {
			// In edit mode, all controls have to be shown
			if(mModifiable) button.setVisible(true);
			button.getControlView().setAlpha(mModifiable ? button.getProperties().opacity : mButtonsOpacity * button.getProperties().opacity);
		}
	}
}
