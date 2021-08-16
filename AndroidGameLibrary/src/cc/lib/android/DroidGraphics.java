package cc.lib.android;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.Log;

import java.util.Vector;

import cc.lib.game.AImage;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IImageFilter;
import cc.lib.game.IRectangle;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.math.CMath;
import cc.lib.math.Vector2D;

/**
 * Created by chriscaron on 2/12/18.
 *
 * Create a graphics interface based on Android Canvas
 */

public abstract class DroidGraphics extends APGraphics {

    private final Context context;
    private Canvas canvas;
    private final Paint paint = new Paint();
    private final TextPaint textPaint = new TextPaint();
    private final Path path = new Path();
    private final RectF rectf = new RectF();
    private final Rect rect = new Rect();
    private Bitmap screenCapture = null;
    private Canvas savedCanvas = null;
    private final Vector<Bitmap> bitmaps = new Vector<>();
    private boolean textModePixels = false;
    private boolean lineThicknessModePixels = true;
    private float curStrokeWidth = 1;

    public DroidGraphics(Context context, Canvas canvas, int width, int height) {
        super(width, height);
        this.context = context;
        this.canvas = canvas;
        r.setOrtho(0, width, 0, height);
        paint.setStrokeWidth(1);
        textPaint.setStrokeWidth(1);
        curStrokeWidth = 1;
        paint.setAntiAlias(true);
        textPaint.setAntiAlias(true);
    }

    public void setTextModePixels(boolean textModePixels) {
        this.textModePixels = textModePixels;
    }

    public void setLineThicknessModePixels(boolean lineThicknessModePixels) {
        this.lineThicknessModePixels = lineThicknessModePixels;
    }

    public final Canvas getCanvas() {
        return canvas;
    }

    public void setCanvas(Canvas c, int width, int height) {
        this.canvas = c;
        initViewport(width, height);
        r.setOrtho(0, width, 0, height);
    }

    public float convertPixelsToDips(float pixels) {
        return DroidUtils.convertPixelsToDips(context, pixels);
    }

    public int convertDipsToPixels(float dips) {
        return DroidUtils.convertDipsToPixels(context, dips);
    }

    /**
     * @return
     */
    public final Paint getPaint() {
        return paint;
    }

    @Override
    public final void setColor(GColor color) {
        int intColor = color.toARGB();
        paint.setColor(intColor);
        textPaint.setColor(intColor);
    }

    @Override
    public final void setColorARGB(int argb) {
        paint.setColor(argb);
        textPaint.setColor(argb);
    }

    @Override
    public final void setColor(int r, int g, int b, int a) {
        int intColor = Color.argb(a, r, g, b);
        paint.setColor(intColor);
        textPaint.setColor(intColor);
    }

    @Override
    public final GColor getColor() {
        return new GColor(paint.getColor());
    }



    @Override
    public final float getTextHeight() {
        if (textModePixels)
            return textPaint.getTextSize();
        return convertPixelsToDips(textPaint.getTextSize());
    }

    @Override
    public final void setTextHeight(float height) {
        if (textModePixels)
            textPaint.setTextSize(height);
        else
            textPaint.setTextSize(convertDipsToPixels(height));
    }

    @Override
    public void setTextStyles(TextStyle... style) {
        textPaint.setUnderlineText(false);
        for (TextStyle st : style) {
            switch (st) {
                case NORMAL:
                    textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.NORMAL));
                    break;
                case BOLD:
                    textPaint.setTypeface(Typeface.DEFAULT_BOLD);//Typeface.create(paint.getTypeface(), Typeface.NORMAL));
                    break;
                case ITALIC:
                    textPaint.setTypeface(Typeface.create(textPaint.getTypeface(), Typeface.ITALIC));
                    break;
                case MONOSPACE:
                    textPaint.setTypeface(Typeface.MONOSPACE);//paint.getTypeface(), Typeface.NORMAL));
                    break;
                case UNDERLINE:
                    textPaint.setUnderlineText(true);
                    break;
            }
        }
    }

    @Override
    public final float getTextWidth(String string) {
        if (string.length() == 0)
            return 0;
        float[] widths = new float[string.length()];
        textPaint.getTextWidths(string, widths);
        return CMath.sum(widths);
    }

    @Override
    public final float drawStringLine(float x, float y, Justify hJust, String text) {
        Paint.FontMetrics fm = textPaint.getFontMetrics();
        y -= fm.ascent;
        switch (hJust) {
            case LEFT:
                textPaint.setTextAlign(Paint.Align.LEFT);
                break;
            case RIGHT:
                textPaint.setTextAlign(Paint.Align.RIGHT);
                break;
            case CENTER:
                textPaint.setTextAlign(Paint.Align.CENTER);
                break;
        }
        textPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawText(text, x, y, textPaint);
        return getTextWidth(text);
    }

    @Override
    public final float setLineWidth(float newWidth) {
        if (lineThicknessModePixels) {
            float curWidth = curStrokeWidth;//paint.getStrokeWidth();
            paint.setStrokeWidth(newWidth);
            curStrokeWidth = newWidth;
            return curWidth;
        } else {
            float strokeWidth = curStrokeWidth;//paint.getStrokeWidth();
            float curWidth = convertPixelsToDips(strokeWidth);
            float pixWidth = convertDipsToPixels(newWidth);
            paint.setStrokeWidth(pixWidth);
            curStrokeWidth = newWidth;
            return curWidth;
        }
    }

    private float pointSize = 1;

    @Override
    public final float setPointSize(float newSize) {
        float oldPtSize = pointSize;
        pointSize = newSize;
        return oldPtSize;
    }

    @Override
    public final void drawPoints() {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        for (int i = 0; i < r.getNumVerts(); i++) {
            Vector2D v = r.getVertex(i);
            canvas.drawCircle(v.getX(), v.getY(), pointSize / 2, paint);
        }
    }

    @Override
    public final void drawLines() {
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i < r.getNumVerts() - 1; i += 2) {
            Vector2D v0 = r.getVertex(i);
            Vector2D v1 = r.getVertex(i + 1);
            canvas.drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), paint);
        }
    }

    @Override
    public final void drawLineStrip() {
        paint.setStyle(Paint.Style.STROKE);
        int num = r.getNumVerts();
        if (num > 1) {
            Vector2D v0 = r.getVertex(0);
            for (int i = 1; i < r.getNumVerts(); i++) {
                Vector2D v1 = r.getVertex(i);
                canvas.drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), paint);
                v0 = v1;
            }
        }
    }

    @Override
    public final void drawLineLoop() {
        paint.setStyle(Paint.Style.STROKE);
        int num = r.getNumVerts();
        if (num > 1) {
            Vector2D v0 = r.getVertex(0);
            for (int i = 1; i < r.getNumVerts(); i++) {
                Vector2D v1 = r.getVertex(i);
                canvas.drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), paint);
                v0 = v1;
            }
            if (num > 2) {
                Vector2D v1 = r.getVertex(0);
                canvas.drawLine(v0.getX(), v0.getY(), v1.getX(), v1.getY(), paint);
            }
        }
    }

    private void drawPolygon(Vector2D... verts) {
        path.reset();
        path.moveTo(verts[0].getX(), verts[0].getY());
        for (int i = 1; i < verts.length; i++)
            path.lineTo(verts[i].getX(), verts[i].getY());
        path.close();
        canvas.drawPath(path, paint);
    }

    @Override
    public final void drawTriangles() {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        int num = r.getNumVerts();
        for (int i = 0; i <= num - 3; i += 3) {
            Vector2D v0 = r.getVertex(i);
            Vector2D v1 = r.getVertex(i + 1);
            Vector2D v2 = r.getVertex(i + 2);
            drawPolygon(v0, v1, v2);
        }
    }

    @Override
    public final void drawTriangleFan() {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        int num = r.getNumVerts();
        if (num < 3)
            return;
        Vector2D ctr = r.getVertex(0);
        Vector2D v0 = r.getVertex(1);
        for (int i = 2; i < num; i++) {
            Vector2D v1 = r.getVertex(i);
            drawPolygon(ctr, v0, v1);
            v0 = v1;
        }
    }

    @Override
    public final void drawTriangleStrip() {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        int num = r.getNumVerts();
        if (num < 3)
            return;
        Vector2D v0 = r.getVertex(0);
        Vector2D v1 = r.getVertex(1);
        for (int i = 2; i < num; i++) {
            Vector2D v2 = r.getVertex(i);
            drawPolygon(v0, v1, v2);
            v0 = v1;
            v1 = v2;
        }
    }

    @Override
    public final void drawQuadStrip() {
        paint.setStyle(Paint.Style.FILL);
        int num = r.getNumVerts();
        if (num < 4)
            return;
        Vector2D v0 = r.getVertex(0);
        Vector2D v1 = r.getVertex(1);
        for (int i = 2; i <= num - 1; i += 2) {
            Vector2D v3 = r.getVertex(i);
            Vector2D v2 = r.getVertex(i + 1);
            drawPolygon(v0, v1, v2, v3);
            v0 = v2;
            v1 = v3;
        }
    }

    @Override
    public void drawRects() {
        paint.setStyle(Paint.Style.STROKE);
        for (int i = 0; i <= r.getNumVerts() - 2; i += 2) {
            Vector2D v0 = r.getVertex(i);
            Vector2D v1 = r.getVertex(i + 1);
            float x = Math.min(v0.X(), v1.X());
            float y = Math.min(v0.Y(), v1.Y());
            float w = Math.abs(v0.X() - v1.X());
            float h = Math.abs(v0.Y() - v1.Y());
            //canvas.drawRect(x, y, w, h, paint);
            canvas.drawLine(x, y, x + w, y, paint);
            canvas.drawLine(x + w, y, x + w, y + h, paint);
            canvas.drawLine(x + w, y + h, x, y + h, paint);
            canvas.drawLine(x, y + h, x, y, paint);
        }
    }

    @Override
    public final void drawFilledOval(float x, float y, float w, float h) {
        setRectF(x, y, x+w, y+h);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawOval(rectf, paint);
    }

    @Override
    public void drawFilledRects() {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i <= r.getNumVerts()-1; i += 2) {
            Vector2D v0 = r.getVertex(i);
            Vector2D v1 = r.getVertex(i + 1);
            Vector2D min = v0.min(v1);
            Vector2D max = v0.max(v1);
            canvas.drawRect(min.X(), min.Y(), max.X(), max.Y(), paint);
        }

    }

    private int addImage(Bitmap bm) {
        int id = bitmaps.size();
        bitmaps.add(bm);
        return id;
    }

    /**
     * Recycle all saved bitmaps
     */
    public void releaseBitmaps() {
        for (Bitmap bm : bitmaps) {
            if (bm != null)
                bm.recycle();
        }
        bitmaps.clear();
    }

    private final Bitmap transformImage(Bitmap in, int outWidth, int outHeight, IImageFilter transform) {
        if (outWidth > 0 || outHeight > 0) {
            if (outWidth <= 0)
                outWidth = in.getWidth();
            if (outHeight <= 0)
                outHeight = in.getHeight();
            if (in.getWidth() != outWidth || in.getHeight() != outHeight) {
                Bitmap newBM = Bitmap.createScaledBitmap(in, outWidth, outHeight, true);
                in.recycle();
                in = newBM;
            }
        }

        if (transform != null) {
            if (!in.isMutable() || in.getConfig() != Bitmap.Config.ARGB_8888) {
                Bitmap newBM = in.copy(Bitmap.Config.ARGB_8888, true);
                in.recycle();
                in = newBM;
            }
            for (int i = 0; i < in.getWidth(); i++) {
                for (int ii = 0; ii < in.getHeight(); ii++) {
                    in.setPixel(i, ii, transform.filterRGBA(i, ii, in.getPixel(i, ii)));
                }
            }
        }

        return in;
    }

    @Override
    public final int loadImage(String assetPath, final GColor transparent) {
        Bitmap bm;
        try {
            bm = BitmapFactory.decodeStream(context.getAssets().open(assetPath));
        } catch (Exception e) {
            bm = BitmapFactory.decodeFile(assetPath);
        }
        if (bm == null) {
            Log.e("DroidGraphics", "Failed to open '" + assetPath + "'");
            return -1;
        }
        if (transparent != null) {
            bm = transformImage(bm, -1, -1, new IImageFilter() {
                @Override
                public int filterRGBA(int x, int y, int argb) {
                    int c0 = argb & 0x00ffffff;
                    int c1 = transparent.toRGB();
                    return c0 == c1 ? 0 : c0;
                }
            });
        }
        return addImage(bm);
    }



    @Override
    public final int[] loadImageCells(String assetPath, int w, int h, int numCellsX, int numCells, boolean bordered, GColor transparent) {
        int source = loadImage(assetPath, transparent);

        final int cellDelta = bordered ? 1 : 0;

        int x = cellDelta;
        int y = cellDelta;
        int[] result = new int[numCells];

        int nx = 0;
        for (int i = 0; i < numCells; i++) {
            result[i] = newSubImage(source, x, y, w, h);
            if (++nx == numCellsX) {
                nx = 0;
                x = bordered ? 1 : 0;
                y += h + cellDelta;
            } else {
                x += w + cellDelta;
            }
        }

        deleteImage(source);

        return result;
    }

    /**
     *
     * @param file
     * @param cells
     * @return
     */
    public synchronized int [] loadImageCells(String file, int [][] cells) {
        int source = loadImage(file);
        try {
            return loadImageCells(source, cells);
        } finally {
            deleteImage(source);
        }
    }

    /**
     *
     * @param source
     * @param cells
     * @return
     */
    public synchronized int [] loadImageCells(int source, int [][] cells) {
        int [] result = new int[cells.length];
        for (int i=0; i<result.length; i++) {
            int x = cells[i][0];
            int y = cells[i][1];
            int w = cells[i][2];
            int h = cells[i][3];

            result[i] = newSubImage(source, x, y, w, h);
        }
        return result;
    }

    private static class DroidImage extends AImage {

        final Bitmap bm;

        DroidImage(Bitmap bm) {
            this.bm = bm;
        }

        @Override
        public float getWidth() {
            return bm.getWidth();
        }

        @Override
        public float getHeight() {
            return bm.getHeight();
        }

        @Override
        public int[] getPixels() {
            int[] pixels = new int[bm.getWidth() * bm.getHeight()];
            bm.getPixels(pixels, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
            return pixels;
        }
    }

    @Override
    public final void drawImage(int imageKey, int x, int y, int w, int h) {
        if (imageKey >= bitmaps.size()) {
            Drawable d = context.getResources().getDrawable(imageKey);
            rect.set(x, y, x+w, y+h);
            d.setBounds(rect);
            d.setColorFilter(tint);
            d.draw(canvas);
        } else {
            rectf.set(x, y, x+w, y+h);
            Bitmap bm = bitmaps.get(imageKey);
            canvas.drawBitmap(bm, null, rectf, paint);
        }
    }

    @Override
    public void drawImage(int imageKey) {
        canvas.save();
        Matrix M = getCurrentTransform();
//        Matrix I = new Matrix();
  //      I.reset();
    //    canvas.setMatrix(I);
        Bitmap bm = getBitmap(imageKey);
        canvas.drawBitmap(bm, M, paint);
        canvas.restore();
    }

    @Override
    public final AImage getImage(int id) {
        return new DroidImage(getBitmap(id));
    }

    @Override
    public final AImage getImage(int id, int width, int height) {
        throw new RuntimeException("Not Implemented");

    }

    @Override
    public final void deleteImage(int id) {
        Bitmap bm = bitmaps.get(id);
        if (bm != null) {
            bm.recycle();
            bitmaps.set(id, null);
        }

    }

    @Override
    public final int newSubImage(int id, int x, int y, int w, int h) {
        Bitmap bm = getBitmap(id);
        Bitmap newBm = Bitmap.createBitmap(bm, x, y, w, h);
        return addImage(newBm);
    }

    @Override
    public final int newRotatedImage(int id, int degrees) {
        Matrix m = new Matrix();
        m.setRotate(degrees);
        Bitmap bm = getBitmap(id);
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
        return addImage(newBm);
    }

    public Bitmap getBitmap(int id) {
        if (id < bitmaps.size())
            return bitmaps.get(id);
        Drawable d = context.getDrawable(id);
        if (d instanceof BitmapDrawable) {
            return ((BitmapDrawable)d).getBitmap();
        }
        return BitmapFactory.decodeResource(context.getResources(),id);
    }

    @Override
    public final int newTransformedImage(int id, IImageFilter filter) {
        return addImage(transformImage(bitmaps.get(id), -1, -1, filter));
    }

    @Override
    public final void enableTexture(int id) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public final void disableTexture() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public final void texCoord(float s, float t) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public final boolean isTextureEnabled() {
        return false;
    }

    @Override
    public final void clearScreen(GColor color) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        int savecolor = paint.getColor();
        paint.setColor(color.toARGB());
        canvas.save();
        Matrix I = new Matrix();
        I.reset();
        canvas.setMatrix(I);
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        canvas.restore();
        paint.setColor(savecolor);
    }

    @Override
    public final void drawRoundedRect(float x, float y, float w, float h, float radius) {
        paint.setStyle(Paint.Style.STROKE);
        renderRoundRect(x, y, w, h, radius);
    }

    @Override
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        float oldWidth = setLineWidth(0);
        renderRoundRect(x, y, w, h, radius);
        setLineWidth(oldWidth);
    }

    private Matrix getCurrentTransform() {
        Matrix m = new Matrix();
        float [] arr = r.getCurrentTransform().transpose().toFloatArray();
        m.setValues(arr);
        return m;
    }

    @Override
    public void drawFilledRect(float x, float y, float w, float h) {
        paint.setStyle(Paint.Style.FILL);
        setRectF(x, y, x+w, y+h);
        canvas.drawRect(rectf, paint);
    }

    @Override
    public void drawArc(float x, float y, float radius, float startDegrees, float sweepDegrees) {
        paint.setStyle(Paint.Style.STROKE);
        setRectF(x-radius, y-radius, x+radius, y+radius);
        canvas.drawArc(rectf, startDegrees, sweepDegrees, false, paint);
    }

    @Override
    public void drawWedge(float x, float y, float radius, float startDegrees, float sweepDegrees) {
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        setRectF(x-radius, y-radius, x+radius, y+radius);
        canvas.drawArc(rectf, startDegrees, sweepDegrees, false, paint);
    }

    @Override
    public void drawLine(float x0, float y0, float x1, float y1) {
        setRectF(x0, y0, x1, y1);
        paint.setStyle(Paint.Style.STROKE);
        float strokeWidth = paint.getStrokeWidth();
        canvas.drawLine(rectf.left, rectf.top, rectf.right, rectf.bottom, paint);
    }

    @Override
    public void drawRect(float x, float y, float w, float h) {
        setRectF(x, y, x+w, y+h);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(rectf, paint);
    }

    final float [] T = { 0,0 };

    public RectF setRectF(float l, float t, float r, float b) {
        transform(l, t, T);
        rectf.left = T[0];
        rectf.top = T[1];
        transform(r, b, T);
        rectf.right = T[0];
        rectf.bottom = T[1];
        return rectf;
    }

    public RectF setRectF(IRectangle rect) {
        IVector2D tl = rect.getTopLeft();
        IVector2D br = rect.getBottomRight();
        return setRectF(tl.getX(), tl.getY(), br.getX(), br.getY());
    }

    @Override
    public void drawCircle(float x, float y, float radius) {
        setRectF(x-radius, y-radius, x+radius, y+radius);
        float width = rectf.width();
        float height = rectf.height();
        if (width < height) {
            rectf.top = rectf.centerY() - width/2;
            rectf.bottom = rectf.centerY() + width/2;
        } else {
            rectf.left = rectf.centerX() - height/2;
            rectf.right = rectf.centerX() + height/2;
        }
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(rectf, paint);
    }

    @Override
    public void drawFilledCircle(float x, float y, float radius) {
        setRectF(x-radius, y-radius, x+radius, y+radius);
        float width = rectf.width();
        float height = rectf.height();
        if (width < height) {
            rectf.top = rectf.centerY() - width/2;
            rectf.bottom = rectf.centerY() + width/2;
        } else {
            rectf.left = rectf.centerX() - height/2;
            rectf.right = rectf.centerX() + height/2;
        }
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawOval(rectf, paint);
    }

    @Override
    public void drawOval(float x, float y, float w, float h) {
        setRectF(x, y, x+w, y+h);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawOval(rectf, paint);
    }

    private void renderRoundRect(float x, float y, float w, float h, float radius) {
        setRectF(x, y, x+w, y+h);
        canvas.drawRoundRect(rectf, radius, radius,paint);
    }

    @Override
    public void setClipRect(float x, float y, float w, float h) {
        Vector2D v0 = transform(x, y);
        Vector2D v1 = transform(x+w, y+h);
        GRectangle r = new GRectangle(v0, v1);
        canvas.save();
        canvas.clipRect(r.x, r.y, r.x+r.w, r.y+r.h);
    }

    @Override
    public GRectangle getClipRect() {
        Rect r = new Rect();
        if (canvas.getClipBounds(r)) {
            Vector2D v0 = screenToViewport(r.top, r.left);
            Vector2D v1 = screenToViewport(r.right, r.bottom);
            return new GRectangle(v0, v1);
        }

        return new GRectangle(screenToViewport(0, 0), screenToViewport(getViewportWidth(), getViewportHeight()));
    }

    @Override
    public void clearClip() {
        canvas.restore();
    }

    private boolean captureModeSupported = true;

    @Override
    public boolean isCaptureAvailable() {
        return captureModeSupported;
    }

    /**
     *
     * @param supported
     */
    public final void setCaptureModeSupported(boolean supported) {
        this.captureModeSupported = supported;
    }

    @Override
    public void beginScreenCapture() {
        if (screenCapture != null) {
            System.err.println("screen capture bitmap already exist, deleting it.");
            screenCapture.recycle();
            screenCapture = null;
        }

        screenCapture = Bitmap.createBitmap(getViewportWidth(), getViewportHeight(), Bitmap.Config.ARGB_8888);
        savedCanvas = canvas;
        canvas = new Canvas(screenCapture);
    }

    @Override
    public int captureScreen(int x, int y, int w, int h) {
        Bitmap subBM = Bitmap.createBitmap(screenCapture, x, y, w, h);
        screenCapture.recycle();
        screenCapture = null;
        int id = addImage(subBM);
        canvas = savedCanvas;
        return id;
    }

    @Override
    public void setTransparencyFilter(float alpha) {
        paint.setColorFilter(new PorterDuffColorFilter(GColor.WHITE.withAlpha(alpha).toARGB(), PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void removeFilter() {
        paint.setColorFilter(null);
    }

    ColorFilter tint = null;

    @Override
    public void setTint(GColor inColor, GColor outColor) {
        paint.setColorFilter(tint = new PorterDuffColorFilter(outColor.toARGB(), PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void removeTint() {
        paint.setColorFilter(tint = null);
    }
}