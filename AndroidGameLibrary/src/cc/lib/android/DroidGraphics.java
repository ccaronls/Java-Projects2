package cc.lib.android;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;

import java.util.Vector;

import cc.lib.game.*;
import cc.lib.math.*;

/**
 * Created by chriscaron on 2/12/18.
 *
 * Create a graphics interface based on Android Canvas
 */

public class DroidGraphics extends APGraphics {

    private final Context context;
    private Canvas canvas;
    final Paint paint = new Paint();
    final Path path = new Path();
    final RectF rectf = new RectF();
    final Rect rect = new Rect();

    public DroidGraphics(Context context, Canvas canvas) {
        super(canvas.getWidth(), canvas.getHeight());
        this.context = context;
        this.canvas = canvas;
        r.setOrtho(0, canvas.getWidth(), 0, canvas.getHeight());
        paint.setStrokeWidth(1);
    }

    public final void shutDown() {
        for (Bitmap bm : bitmaps) {
            if (bm != null) {
                bm.recycle();
            }
        }
        bitmaps.clear();
    }

    public void setCanvas(Canvas c) {
        this.canvas = c;
        initViewport(c.getWidth(), c.getHeight());
    }

    /**
     * @return
     */
    public final Paint getPaint() {
        return paint;
    }

    @Override
    public final void setColor(GColor color) {
        paint.setColor(color.toARGB());
    }

    @Override
    public final void setColorARGB(int argb) {
        paint.setColor(argb);
    }

    @Override
    public final void setColorRGBA(int rgba) {
        int r = (rgba >>> 24) & 0xff;
        int g = (rgba >> 16) & 0xff;
        int b = (rgba >> 8) & 0xff;
        int a = (rgba >> 0) & 0xff;
        paint.setColor(Color.argb(a, r, g, b));
    }

    @Override
    public final void setColor(int r, int g, int b, int a) {
        paint.setColor(Color.argb(a, r, g, b));
    }

    @Override
    public final GColor getColor() {
        return new GColor(paint.getColor());
    }

    @Override
    public final int getTextHeight() {
        return Math.round(paint.getTextSize());
    }

    @Override
    public final void setTextHeight(float height) {
        paint.setTextSize(height);
    }

    @Override
    public void setTextStyles(TextStyle... style) {
        paint.setUnderlineText(false);
        for (TextStyle st : style) {
            switch (st) {
                case NORMAL:
                    paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.NORMAL));
                    break;
                case BOLD:
                    paint.setTypeface(Typeface.DEFAULT_BOLD);//Typeface.create(paint.getTypeface(), Typeface.NORMAL));
                    break;
                case ITALIC:
                    paint.setTypeface(Typeface.create(paint.getTypeface(), Typeface.ITALIC));
                    break;
                case MONOSPACE:
                    paint.setTypeface(Typeface.MONOSPACE);//paint.getTypeface(), Typeface.NORMAL));
                    break;
                case UNDERLINE:
                    paint.setUnderlineText(true);
                    break;
            }
        }
    }

    @Override
    public final float getTextWidth(String string) {
        float[] widths = new float[string.length()];
        paint.getTextWidths(string, widths);
        return CMath.sum(widths);
    }

    @Override
    public final float drawStringLine(float x, float y, Justify hJust, String text) {
        y += getTextHeight(); // Android does bottom align by default for some bizzarre reason
        switch (hJust) {
            case LEFT:
                paint.setTextAlign(Paint.Align.LEFT);
                break;
            case RIGHT:
                paint.setTextAlign(Paint.Align.RIGHT);
                break;
            case CENTER:
                paint.setTextAlign(Paint.Align.CENTER);
                break;
        }
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        canvas.drawText(text, x, y, paint);
        return getTextWidth(text);
    }

    @Override
    public final float setLineWidth(float newWidth) {
        float curWidth = paint.getStrokeWidth();
        paint.setStrokeWidth(newWidth);
        return curWidth;
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
        paint.setStyle(Paint.Style.FILL);
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
        paint.setStyle(Paint.Style.FILL);
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
        paint.setStyle(Paint.Style.FILL);
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
        paint.setStyle(Paint.Style.FILL);
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
        for (int i = 2; i <= num - 2; i += 2) {
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
    public void drawFilledRects() {
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i <= r.getNumVerts() - 2; i += 2) {
            Vector2D v0 = r.getVertex(i);
            Vector2D v1 = r.getVertex(i + 1);
            float x = Math.min(v0.X(), v1.X());
            float y = Math.min(v0.Y(), v1.Y());
            float w = Math.abs(v0.X() - v1.X());
            float h = Math.abs(v0.Y() - v1.Y());
            canvas.drawRect(x, y, w, h, paint);
        }

    }

    private final Vector<Bitmap> bitmaps = new Vector<>();

    private int addImage(Bitmap bm) {
        int id = bitmaps.size();
        bitmaps.add(bm);
        return id;
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
        Bitmap bm = BitmapFactory.decodeFile(assetPath);
        if (bm == null)
            return -1;
        bm = transformImage(bm, -1, -1, new IImageFilter() {
            @Override
            public int filterRGBA(int x, int y, int argb) {
                int c0 = argb & 0x00ffffff;
                int c1 = transparent.toRGB();
                return c0 == c1 ? 0 : c0;
            }
        });
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

    private static class DroidImage extends AImage {

        final Bitmap bm;

        DroidImage(Bitmap bm) {
            this.bm = bm;
        }

        @Override
        public int getWidth() {
            return bm.getWidth();
        }

        @Override
        public int getHeight() {
            return bm.getHeight();
        }

        @Override
        public int[] getPixels() {
            int[] pixels = new int[getWidth() * getHeight()];
            bm.getPixels(pixels, 0, getWidth(), 0, 0, getWidth(), getHeight());
            return pixels;
        }
    }

    @Override
    public final void drawImage(int imageKey, int x, int y, int w, int h) {
        if (imageKey >= bitmaps.size()) {
            Drawable d = context.getResources().getDrawable(imageKey);
            rect.set(x, y, x+w, y+h);
            d.setBounds(rect);
            d.draw(canvas);
        } else {
            rectf.set(x, y, x+w, y+h);
            Bitmap bm = bitmaps.get(imageKey);
            canvas.drawBitmap(bm, null, rect, null);
        }
    }

    @Override
    public final AImage getImage(int id) {
        return new DroidImage(bitmaps.get(id));
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
        Bitmap bm = bitmaps.get(id);
        Bitmap newBm = Bitmap.createBitmap(bm, x, y, w, h);
        return addImage(newBm);
    }

    @Override
    public final int newRotatedImage(int id, int degrees) {
        Matrix m = new Matrix();
        m.setRotate(degrees);
        Bitmap bm = bitmaps.get(id);
        Bitmap newBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), m, true);
        return addImage(newBm);
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
        paint.setStyle(Paint.Style.FILL);
        int savecolor = paint.getColor();
        paint.setColor(color.toARGB());
        canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), paint);
        paint.setColor(savecolor);
    }

    @Override
    public final void drawRoundedRect(float x, float y, float w, float h, float thickness, float radius) {
        float oldT = setLineWidth(thickness);
        paint.setStyle(Paint.Style.STROKE);
        renderRoundRect(x, y, w, h, radius);
        paint.setStrokeWidth(oldT);
    }

    @Override
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
        paint.setStyle(Paint.Style.FILL);
        renderRoundRect(x, y, w, h, radius);
    }

    private void renderRoundRect(float x, float y, float w, float h, float radius) {
        /*
        Matrix m = new Matrix();
        m.setValues(r.getCurrentTransform().transpose().toFloatArray());
        canvas.save();
        canvas.setMatrix(m);
        canvas.drawRoundRect(x, y, x+w, y+h, radius, radius, paint);
        canvas.restore();
*/
        Matrix m = new Matrix();
        float [] arr = r.getCurrentTransform().transpose().toFloatArray();
        m.setValues(arr);
        float width = paint.getStrokeWidth();
        paint.setStrokeWidth(0);
        canvas.save();
        canvas.setMatrix(m);
        canvas.drawRoundRect(x, y, x+w, y+h, radius, radius,paint);
        canvas.restore();
        paint.setStrokeWidth(width);




        /*

        MutableVector2D v0 = new MutableVector2D(x, y);
        MutableVector2D v1 = new MutableVector2D(x+w, y+h);
        transform(v0);
        transform(v1);
        float x0 = Math.min(v0.getX(), v1.getX());
        float y0 = Math.min(v0.getY(), v1.getY());
        float x1 = Math.max(v0.getX(), v1.getX());
        float y1 = Math.max(v0.getY(), v1.getY());
        float aspect = w/h;
        float ratio = radius/w;
        float W = Math.abs(v0.getX()-v1.getX());
        float H = Math.abs(v0.getY()-v1.getY());
        if (W/H < aspect) {
            Matrix m = new Matrix();
            m.setValues(r.getCurrentTransform().toFloatArray());
            radius = H*ratio;
        } else {
            radius = W*ratio;
        }
        canvas.drawRoundRect(x0, y0, x1, y1, radius, radius, paint);//*/
    }

}