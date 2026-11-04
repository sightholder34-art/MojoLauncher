package git.artdeell.mojo.modui;

import static git.artdeell.mojo.game.platform.Platform.PLATFORM;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import git.artdeell.mojo.LwjglGlfwKeycode;
import git.artdeell.mojo.game.platform.Platform;

/** Sends real GLFW key and mouse events for a bind reported by the mod. */
public final class ModUiInput {
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    private ModUiInput() { }

    public static void press(JSONObject bind, boolean down) {
        int code = bind.optInt("glfw", -1);
        if (code < 0) return;
        boolean mouse = bind.optInt("type") == 1;
        int modBits = 0;
        int modKey = 0;
        switch (bind.optString("mod", "NONE")) {
            case "ALT":
                modBits = LwjglGlfwKeycode.GLFW_MOD_ALT;
                modKey = LwjglGlfwKeycode.GLFW_KEY_LEFT_ALT;
                break;
            case "CONTROL":
                modBits = LwjglGlfwKeycode.GLFW_MOD_CONTROL;
                modKey = LwjglGlfwKeycode.GLFW_KEY_LEFT_CONTROL;
                break;
            case "SHIFT":
                modBits = LwjglGlfwKeycode.GLFW_MOD_SHIFT;
                modKey = LwjglGlfwKeycode.GLFW_KEY_LEFT_SHIFT;
                break;
        }
        if (down && modKey != 0) PLATFORM.sendKeyEvent(modKey, true, modBits);
        if (mouse) Platform.sendMouseButtonEvent(code, down ? 1 : 0, modBits);
        else PLATFORM.sendKeyEvent(code, down, down ? modBits : 0);
        if (!down && modKey != 0) PLATFORM.sendKeyEvent(modKey, false, 0);
    }

    public static void tap(JSONObject bind) {
        press(bind, true);
        HANDLER.postDelayed(() -> press(bind, false), 60);
    }
}
