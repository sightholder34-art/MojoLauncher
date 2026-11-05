package git.artdeell.mojo.input.buttons;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Line icons used by Zenkai-styled buttons and popup menus. */
public final class ZenkaiIcons {
    private ZenkaiIcons() {}

    public static void draw(Canvas c, Paint p0, String name, float x, float y, float s) {
        float l = x - s * .5f, r = x + s * .5f, t = y - s * .5f, b = y + s * .5f;
        Path p = new Path();
        switch (name) {
            case "menu": {
                float cell = s * .38f, gap = s * .24f;
                for (int row = 0; row < 2; row++) for (int col = 0; col < 2; col++) {
                    float cl = l + s * .04f + col * (cell + gap), ct = t + s * .04f + row * (cell + gap);
                    c.drawRoundRect(cl, ct, cl + cell, ct + cell, s * .08f, s * .08f, p0);
                }
                return;
            }
            case "chat":
                c.drawRoundRect(l, t, r, y + s * .22f, s * .12f, s * .12f, p0);
                p.moveTo(l + s * .22f, y + s * .22f); p.lineTo(l + s * .22f, b); p.lineTo(x, y + s * .22f);
                c.drawPath(p, p0);
                return;
            case "target":
                c.drawCircle(x, y, s * .30f, p0); c.drawCircle(x, y, s * .08f, p0);
                c.drawLine(x, t, x, y - s * .30f, p0); c.drawLine(x, y + s * .30f, x, b, p0);
                c.drawLine(l, y, x - s * .30f, y, p0); c.drawLine(x + s * .30f, y, r, y, p0);
                return;
            case "dash":
                p.moveTo(x + s * .08f, t); p.lineTo(x - s * .28f, y + s * .05f); p.lineTo(x - s * .02f, y + s * .05f);
                p.lineTo(x - s * .12f, b); p.lineTo(x + s * .30f, y - s * .10f); p.lineTo(x + s * .04f, y - s * .10f);
                p.close(); c.drawPath(p, p0);
                return;
            case "jump":
                p.moveTo(x - s * .30f, b); p.lineTo(x - s * .30f, y + s * .05f); p.lineTo(x + s * .20f, y + s * .05f);
                p.lineTo(x + s * .20f, t + s * .18f); c.drawPath(p, p0);
                p.reset(); p.moveTo(x, t + s * .20f); p.lineTo(x + s * .20f, t); p.lineTo(x + s * .40f, t + s * .20f);
                c.drawPath(p, p0);
                return;
            case "boost":
                p.moveTo(l + s * .05f, t + s * .12f); p.lineTo(x + s * .10f, y); p.lineTo(l + s * .05f, b - s * .12f);
                p.moveTo(x - s * .02f, t + s * .12f); p.lineTo(r - s * .05f, y); p.lineTo(x - s * .02f, b - s * .12f);
                c.drawPath(p, p0);
                return;
            case "close":
                c.drawLine(l + s * .1f, t + s * .1f, r - s * .1f, b - s * .1f, p0);
                c.drawLine(r - s * .1f, t + s * .1f, l + s * .1f, b - s * .1f, p0);
                return;
            case "fly":
                p.moveTo(x, y + s * .05f); p.lineTo(l, b - s * .15f); p.lineTo(l + s * .08f, t + s * .10f); p.lineTo(x, y - s * .05f);
                p.lineTo(r - s * .08f, t + s * .10f); p.lineTo(r, b - s * .15f); p.close(); c.drawPath(p, p0);
                c.drawLine(x, y + s * .05f, x, b, p0);
                return;
            case "attack": case "use": case "crouch":
                c.drawRoundRect(x - s * .24f, y - s * .10f, x + s * .24f, y + s * .34f, s * .10f, s * .10f, p0);
                c.drawLine(x - s * .24f, y, x - s * .40f, y - s * .14f, p0);
                c.drawLine(x + s * .24f, y, x + s * .40f, y - s * .14f, p0);
                c.drawLine(x - s * .10f, y - s * .10f, x - s * .10f, t, p0);
                c.drawLine(x + s * .10f, y - s * .10f, x + s * .10f, t, p0);
                return;
            case "charge":
                c.drawCircle(x, y, s * .36f, p0);
                p.moveTo(x + s * .04f, y - s * .24f); p.lineTo(x - s * .14f, y + s * .02f); p.lineTo(x + s * .04f, y + s * .02f);
                p.lineTo(x - s * .04f, y + s * .24f); c.drawPath(p, p0);
                return;
            case "ki":
                c.drawCircle(x + s * .05f, y, s * .20f, p0);
                c.drawLine(x - s * .45f, y + s * .15f, x - s * .22f, y - s * .05f, p0);
                c.drawLine(x - s * .45f, y - s * .10f, x - s * .25f, y - s * .20f, p0);
                c.drawLine(x - s * .40f, y + s * .38f, x - s * .15f, y + s * .20f, p0);
                return;
            case "sneak":
                c.drawCircle(x + s * .12f, t + s * .14f, s * .10f, p0);
                p.moveTo(x + s * .08f, t + s * .28f); p.lineTo(x - s * .08f, y); p.lineTo(x + s * .22f, y + s * .12f);
                p.lineTo(x + s * .38f, b); p.moveTo(x - s * .08f, y); p.lineTo(x - s * .38f, b - s * .05f);
                c.drawPath(p, p0);
                return;
            case "stack":
                c.drawRoundRect(l, y - s * .05f, r - s * .25f, b, s * .08f, s * .08f, p0);
                c.drawRoundRect(l + s * .25f, t, r, y + s * .15f, s * .08f, s * .08f, p0);
                return;
            case "click":
                c.drawRoundRect(x - s * .24f, t, x + s * .24f, b, s * .24f, s * .24f, p0);
                c.drawLine(x, t, x, y - s * .05f, p0); c.drawLine(x - s * .24f, y - s * .05f, x + s * .24f, y - s * .05f, p0);
                return;
            case "keyboard":
                c.drawRoundRect(l, y - s * .30f, r, y + s * .30f, s * .10f, s * .10f, p0);
                c.drawLine(l + s * .18f, y - s * .08f, r - s * .18f, y - s * .08f, p0);
                c.drawLine(l + s * .28f, y + s * .14f, r - s * .28f, y + s * .14f, p0);
                return;
            case "grab":
                c.drawRoundRect(x - s * .30f, y - s * .02f, x + s * .10f, y + s * .36f, s * .08f, s * .08f, p0);
                for (int i = 0; i < 3; i++) c.drawLine(x - s * .22f + i * s * .14f, y - s * .02f, x - s * .22f + i * s * .14f, y - s * .30f, p0);
                c.drawLine(x + s * .16f, y + s * .10f, x + s * .46f, y + s * .10f, p0);
                c.drawLine(x + s * .34f, y - s * .02f, x + s * .46f, y + s * .10f, p0);
                c.drawLine(x + s * .34f, y + s * .22f, x + s * .46f, y + s * .10f, p0);
                return;
            case "chevron":
                p.moveTo(x - s * .30f, y - s * .12f); p.lineTo(x, y + s * .18f); p.lineTo(x + s * .30f, y - s * .12f);
                c.drawPath(p, p0);
                return;
            case "ki_beam":
                c.drawLine(l, y, r - s * .05f, y, p0);
                p.moveTo(x + s * .05f, y - s * .25f); p.lineTo(r - s * .02f, y); p.lineTo(x + s * .05f, y + s * .25f);
                c.drawPath(p, p0);
                c.drawLine(l, y - s * .18f, x - s * .15f, y - s * .18f, p0);
                return;
            case "burst":
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI * i / 4d;
                    c.drawLine(x + (float) Math.cos(a) * s * .16f, y + (float) Math.sin(a) * s * .16f,
                            x + (float) Math.cos(a) * s * .46f, y + (float) Math.sin(a) * s * .46f, p0);
                }
                c.drawCircle(x, y, s * .10f, p0);
                return;
            case "orb":
                c.drawCircle(x, y, s * .40f, p0);
                c.drawLine(x - s * .40f, y, x + s * .40f, y, p0); c.drawLine(x, y - s * .40f, x, y + s * .40f, p0);
                return;
            case "volley":
                for (int i = -1; i <= 1; i++) {
                    float yy = y + i * s * .28f;
                    c.drawLine(l, yy, r - s * .12f, yy, p0);
                    c.drawLine(r - s * .26f, yy - s * .10f, r - s * .08f, yy, p0);
                    c.drawLine(r - s * .26f, yy + s * .10f, r - s * .08f, yy, p0);
                }
                return;
            default:
                c.drawCircle(x, y, s * .34f, p0);
                c.drawLine(x - s * .18f, y, x + s * .18f, y, p0); c.drawLine(x, y - s * .18f, x, y + s * .18f, p0);
        }
    }
}
