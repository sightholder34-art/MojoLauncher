package git.artdeell.mojo.modui;

import static git.artdeell.mojo.game.platform.Platform.PLATFORM;

import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.json.JSONObject;

import git.artdeell.mojo.LwjglGlfwKeycode;
import git.artdeell.mojo.game.platform.Platform;
import git.artdeell.mojo.input.LayoutConverter;

/**
 * Sends key and mouse events for a bind reported by the mod.
 * The mod reports GLFW codes, but the launcher's input layer takes ANDROID keycodes and MotionEvent buttons,
 * so every code is converted first (sending a GLFW code as-is made e.g. "2" (50) arrive as "V" (50)).
 */
public final class ModUiInput {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private ModUiInput() { }

    public static void press(JSONObject bind, boolean down) {
        int code = bind.optInt("glfw", -1);
        if (code < 0) return;
        boolean mouse = bind.optInt("type") == 1;
        int modBits = 0;
        int modKey = KeyEvent.KEYCODE_UNKNOWN;
        switch (bind.optString("mod", "NONE")) {
            case "ALT":
                modBits = LwjglGlfwKeycode.GLFW_MOD_ALT;
                modKey = KeyEvent.KEYCODE_ALT_LEFT;
                break;
            case "CONTROL":
                modBits = LwjglGlfwKeycode.GLFW_MOD_CONTROL;
                modKey = KeyEvent.KEYCODE_CTRL_LEFT;
                break;
            case "SHIFT":
                modBits = LwjglGlfwKeycode.GLFW_MOD_SHIFT;
                modKey = KeyEvent.KEYCODE_SHIFT_LEFT;
                break;
        }
        if (mouse) {
            int button = mouseButton(code);
            if (button == 0) return;
            if (down && modKey != KeyEvent.KEYCODE_UNKNOWN) PLATFORM.sendKeyEvent(modKey, true, modBits);
            Platform.sendMouseButtonEvent(button, down ? 1 : 0, modBits);
            if (!down && modKey != KeyEvent.KEYCODE_UNKNOWN) PLATFORM.sendKeyEvent(modKey, false, 0);
            return;
        }
        int key = LayoutConverter.keyCodeFromGLFW(code);
        if (key == KeyEvent.KEYCODE_UNKNOWN) return; // no Android equivalent
        if (down && modKey != KeyEvent.KEYCODE_UNKNOWN) PLATFORM.sendKeyEvent(modKey, true, modBits);
        PLATFORM.sendKeyEvent(key, down, down ? modBits : 0);
        if (!down && modKey != KeyEvent.KEYCODE_UNKNOWN) PLATFORM.sendKeyEvent(modKey, false, 0);
    }

    /** GLFW mouse button (0 left, 1 right, 2 middle, 3, 4) to the MotionEvent button the platform layer expects. */
    private static int mouseButton(int glfw) {
        switch (glfw) {
            case 0: return MotionEvent.BUTTON_PRIMARY;
            case 1: return MotionEvent.BUTTON_SECONDARY;
            case 2: return MotionEvent.BUTTON_TERTIARY;
            case 3: return MotionEvent.BUTTON_BACK;
            case 4: return MotionEvent.BUTTON_FORWARD;
            default: return 0;
        }
    }

    public static void tap(JSONObject bind) {
        press(bind, true);
        HANDLER.postDelayed(() -> press(bind, false), 60);
    }
}
