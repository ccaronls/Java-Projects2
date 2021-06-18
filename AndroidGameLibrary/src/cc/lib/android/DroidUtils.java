package cc.lib.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.text.format.Formatter;
import android.util.TypedValue;

import java.io.File;

import cc.lib.game.AGraphics;
import cc.lib.game.GDimension;
import cc.lib.game.Utils;

public class DroidUtils extends Utils {

    /**
     *
     * @param red
     * @param green
     * @param blue
     * @param alpha
     * @return
     */
    public static int colorToARGB(float red, float green, float blue, float alpha) {
        int a = Math.round(alpha*255);
        int r = Math.round(red*255);
        int g = Math.round(green*255);
        int b = Math.round(blue*255);
        int d = ((a << 24) & 0xff000000) |
                ((r << 16) & 0x00ff0000) |
                ((g << 8)  & 0x0000ff00) |
                ((b << 0)  & 0x000000ff);
        return d;
    }

    public static int darken(int color, float amount) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);

        float R = amount * r;
        float G = amount * g;
        float B = amount * b;
        r = Utils.clamp(Math.round(R-r), 0, 255);
        g = Utils.clamp(Math.round(G-g), 0, 255);
        b = Utils.clamp(Math.round(B-b), 0, 255);

        return Color.argb(a, r, g, b);
    }

    public static int lighten(int color, float amount) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        int a = Color.alpha(color);

        float R = amount * r;
        float G = amount * g;
        float B = amount * b;
        r = Utils.clamp(Math.round(R+r), 0, 255);
        g = Utils.clamp(Math.round(G+g), 0, 255);
        b = Utils.clamp(Math.round(B+b), 0, 255);

        return Color.argb(a, r, g, b);
    }

    public static void multiply(float [] glMatrix16, float [] glVertex4) {
    	float x = glVertex4[0];
    	float y = glVertex4[1];
    	float z = glVertex4[2];
    	float w = glVertex4[3];
    	glVertex4[0] = x*glMatrix16[0] + y*glMatrix16[4] + z*glMatrix16[8] + w*glMatrix16[12];
    	glVertex4[1] = x*glMatrix16[1] + y*glMatrix16[5] + z*glMatrix16[9] + w*glMatrix16[13];
    	glVertex4[2] = x*glMatrix16[2] + y*glMatrix16[6] + z*glMatrix16[10] + w*glMatrix16[14];
    	glVertex4[3] = x*glMatrix16[3] + y*glMatrix16[7] + z*glMatrix16[11] + w*glMatrix16[15];
    }

    public static void debugAssert(boolean expression, String message) {
    	if (BuildConfig.DEBUG && !expression)
    		throw new AssertionError(message);
    }
    
    /**
     * Detemine the minimum rectangle to hold the given text.
     * \n is a delim for each line.
     * @param g
     * @param txt
     * @return
     */
    public static GDimension computeTextDimension(AGraphics g, String txt) {
        String [] lines = txt.split("\n");
        int width = 0;
        final float height = g.getTextHeight() * lines.length;
        for (int i=0; i<lines.length; i++) {
            int w = Math.round(g.getTextWidth(lines[i]));
            if (w > width)
                width = w;
        }
        return new GDimension(width, height);
    }

    public final static int JUSTIY_LEFT = 0;
    public final static int JUSTIY_TOP  = 0;
    public final static int JUSTIY_CENTER = 1;
    public final static int JUSTIY_RIGHT = 2;
    public final static int JUSTIY_BOTTOM = 2;

    public static void drawJustifiedTextCanvas(Canvas c, CharSequence txt, float tx, float ty, int hJustify, int vJustify, Paint p) {
        Rect bounds = new Rect();
        p.getTextBounds(txt.toString(), 0, txt.length(), bounds);

        final float w = bounds.right - bounds.left;
        final float h = bounds.bottom - bounds.top;

        switch (hJustify) {
            case JUSTIY_LEFT: break;
            case JUSTIY_CENTER:
                tx -= w/2; break;
            case JUSTIY_RIGHT:
                tx -= w;
                break;
        }
        switch (vJustify) {
            case JUSTIY_TOP: break;
            case JUSTIY_CENTER:
                ty -= h/2; break;
            case JUSTIY_BOTTOM:
                ty -= h; break;
        }

        c.drawText(txt, 0, txt.length(), tx, ty, p);
    }

    public static Bitmap addShadowToBitmap(final Bitmap bm, int color, int size, int dx, int dy) {

        int dstWidth = bm.getWidth() + dx + size/2;
        int dstHeight = bm.getHeight() + dy + size/2;

        final Bitmap mask = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ALPHA_8);

        final Canvas maskCanvas = new Canvas(mask);
        final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        maskCanvas.drawBitmap(bm, 0, 0, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_OUT));
        maskCanvas.drawBitmap(bm, dx, dy, paint);

        final BlurMaskFilter filter = new BlurMaskFilter(size, BlurMaskFilter.Blur.NORMAL);
        paint.reset();
        paint.setAntiAlias(true);
        paint.setColor(color);
        paint.setMaskFilter(filter);
        paint.setFilterBitmap(true);

        final Bitmap ret = Bitmap.createBitmap(dstWidth, dstHeight, Bitmap.Config.ARGB_8888);
        final Canvas retCanvas = new Canvas(ret);
        retCanvas.drawBitmap(mask, 0,  0, paint);
        retCanvas.drawBitmap(bm, 0, 0, null);
        mask.recycle();
        return ret;
    }

    public static float convertPixelsToDips(Context context, float pixels) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_PX, pixels, context.getResources().getDisplayMetrics());
    }

    public static int convertDipsToPixels(Context context, float dips) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, context.getResources().getDisplayMetrics()));
    }

    public static String getHumanReadableFileSize(Context context, File file) {
        return Formatter.formatFileSize(context, file.length());
    }
}
