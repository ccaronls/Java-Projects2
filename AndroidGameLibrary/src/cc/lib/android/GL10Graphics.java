package cc.lib.android;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import javax.microedition.khronos.opengles.GL10;
import javax.microedition.khronos.opengles.GL11;

import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.IImageFilter;
import cc.lib.game.Justify;
import cc.lib.math.Matrix3x3;
import cc.lib.math.Vector2D;
import cc.lib.utils.GException;

public class GL10Graphics extends AGraphics {

    //////////////////////////////////////////////////////////////////////////
    // PRIVATE STUFF /////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////

    private final String TAG = getClass().getSimpleName();

    
    // Android
    private ByteBuffer sVbb = null;
    private ByteBuffer sTbb = null;
    private FloatBuffer sVfb;
    private FloatBuffer sTfb;
    private int sVCount = 0;
    //private int sTCount = 0;
    private GL10 sGl;
    private Context sContext;
    private float sLineWidth = 1;
    private float sPointSize = 1;
    private boolean sTextureEnabled = false;
    private int [] textures;
    private int numTextures;

    private Map<Integer, GL10Image> images = new HashMap<Integer, GL10Graphics.GL10Image>();
    
    public final GL10 getGl10() {
        return sGl;
    }
    
    private static class GL10Image extends AImage {

    	final int width, height;
    	
    	GL10Image(Bitmap bitmap) {
    		this.width = bitmap.getWidth();
    		this.height = bitmap.getHeight();
    	}
    	
		@Override
		public float getWidth() {
			return width;
		}

		@Override
		public float getHeight() {
			return height;
		}

		@Override
		public int[] getPixels() {
			throw new RuntimeException("Not implemented");
		}
    	
    }
    
    /**
     * 
     * @param gl
     * @param context
     */
    public GL10Graphics(GL10 gl, Context context) {
        if (gl == null || context == null)
            throw new NullPointerException();
        sGl = gl;
        sContext = context;
        sGl.glMatrixMode(GL10.GL_MODELVIEW);
        setColor(GColor.WHITE);
    }

    /*
    public final GDimension drawWrapString(float x, float y, float maxWidth, Justify hJust, Justify vJust, String text) {
        String [] lines = generateWrappedLines(text, maxWidth);
        float th = getTextHeight() * this.getViewportScaleY();
        switch (vJust) {
            case TOP: break;
            case BOTTOM: y -= lines.length * th; break;
            case CENTER: y -= lines.length * th / 2; break;
            default:
            	throw new IllegalArgumentException("Illegal value for vertical justify: " + vJust);
        }
        float mw = 0;
        for (int i=0; i<lines.length; i++) {
            mw = Math.max(mw, drawStringLine(x, y, hJust, lines[i]));
            y += th;
        }
        return new GDimension(mw, th*lines.length);
    }
    
    /**
     * 
     * @return
     */
    public final float getTextHeight() {
        return textHeight;
    }

    private float textHeight = 20;

    @Override
    public final void setTextHeight(float height) {
        this.textHeight = Math.round(height);
    }

    @Override
    public void setTextStyles(TextStyle... styles) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * 
     * @param text
     * @return
     */
    public final float getTextWidth(String text) {        
     // find the row in bitmap to read from
        FontInfo info = getFontInfo();
        float size = getTextHeight();
        float textWidth = 0;
        int widthRow = -1;
        for (int i=0; i<info.sizes.length; i++) {
            if (size <= info.sizes[i]) {
                widthRow = i;
                size = info.sizes[i]; // set to actual size
                break;
            }
        }
        
        for (int i=0; i<text.length(); i++) {
            int ch = (int)text.charAt(i) - CHAR_START_POS;
            textWidth += info.widths[widthRow][ch];
        }        
        
        return textWidth;
    }
    
    
    /*
    public final GDimension drawJustifiedString(float x, float y, Justify hJust, Justify vJust, String text) {
        if (text==null || text.length() == 0)
            return GDimension.EMPTY;
        String [] lines = text.split("\n");
        final float textHeight = (float)getTextHeight()*getViewportScaleY();
        switch (vJust) {
        case TOP: 
            break;
        case CENTER: 
            y -= (lines.length * (textHeight)) / 2; 
            break;
        case BOTTOM: 
            y -= lines.length * textHeight; 
            break;
        default:
            DroidUtils.unhandledCase(vJust);
            break;
        }
        float mw = 0;
        for (int i=0; i<lines.length; i++) {
            mw = Math.max(mw, priv_drawJustifiedString(sGl, x, y, hJust, lines[i]));
            y += textHeight;
        }
        return new GDimension(mw, textHeight * lines.length);
    }
    
    /**
     * Draw a single line of justified text and return the width of the text
     * @param x
     * @param y
     * @param hJust
     * @param text
     * @return
     */
    public final float drawStringLine(float x, float y, Justify hJust, String text) {
        return priv_drawJustifiedString(sGl, x, y, hJust, text);
    }

    public static class FontInfo {
        
        int id = -1;
        int [] sizes;
        float [][] widths;
        float width, height;
    }
    
    FontInfo fontInfo = null;
    FontInfo getFontInfo() {
        if (fontInfo == null) {
            fontInfo = new FontInfo();
            buildFontBitmap(fontInfo);
        }
        return fontInfo;
    }
    
    private final int CHAR_START_POS = 32;

    // TODO: padding and font height should be tunable for different devices
    private final int FONT_BITMAP_VPADDING = 8;
    
    private void buildFontBitmap(FontInfo info) {
        
        final String BITMAP_FILE = "DefaultFontFile.alpha8"; 
        
        Bitmap bitmap = null;
// TODO: Once font map is built we shouldn't have to so it again so
//       archive the bitmap and meta data for entire app.
        
        try {
        	/*
dout.writeFloat(info.width);
dout.writeFloat(info.height);
dout.writeInt(info.sizes.length);
for (int s : info.sizes)
	dout.writeInt(s);
dout.writeInt(info.widths.length);
dout.writeInt(info.widths[0].length);
for (float [] W : info.widths) {
	for (float w : W) {
		dout.writeFloat(w);
	}
}
int [] pixels = new int[texWidth * texHeight];
bitmap.getPixels(pixels, 0, texWidth, 0, 0, texWidth, texHeight);
int num = texWidth * texHeight;
for (int pix : pixels) {
    dout.write(pix);
}        	 */
            // try to load from file if already generated
            DataInputStream in = new DataInputStream(sContext.openFileInput(BITMAP_FILE));
            FontInfo temp = new FontInfo();
            int width = in.readInt();
            int height = in.readInt();
            int len = in.readInt();
            temp.sizes = new int[len];
            for (int i=0; i<len; i++) {
            	info.sizes[i] = in.readInt();
            }
            len = in.readInt();
            info.widths = new float[len][];
            len = in.readInt();
            for (int i=0; i<info.widths.length; i++) {
            	info.widths[i] = new float[len];
            	for (int ii=0; ii<len; ii++) {
            		info.widths[i][ii] = in.readFloat();
            	}
            }
            int [] pixels = new int[width*height];
            for (int i=0; i<pixels.length; i++) {
            	pixels[i] = in.readInt();
            }
            bitmap = //BitmapFactory.decodeStream(in);;
            		Bitmap.createBitmap(pixels, width, height, Config.ALPHA_8);
            in.close();
        } catch  (Exception e) {}
        
        /*
        try {
            info.loadFromFile(new File(PATH, META_DATA_FILE));
            int width = Math.round(info.width);
            int height = Math.round(info.height);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ALPHA_8);
            int [] pixels = new int[width * height];
            BufferedInputStream fin = new BufferedInputStream(new FileInputStream(new File(PATH, BITMAP_FILE)));
            try {
                for (int i=0; i<pixels.length; i++) {
                    int pix = fin.read();
                    if (pix < 0)
                        throw new EOFException();
                    pixels[i] = pix;
                }
            } finally {
                fin.close();
            }
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load from file: generating ...");
            bitmap = null;
        }*/
        if (bitmap == null) {
            Paint paint = new Paint();
            paint.setARGB(0xff, 0xff, 0xff, 0xff);    
            paint.setAntiAlias(true);
            paint.setTextAlign(Align.LEFT);
            //paint.setTypeface(Typeface.MONOSPACE);
            int [] textSizes = { 8, 10, 12, 16, 20, 24, 32 };//, 40, 48, 56, 64, 72, 80 };
            info.sizes = textSizes;
            // compute all the widths
            String text = "";
            for (int i=CHAR_START_POS; i<128; i++) {
                text += Character.toString((char)i);
            }
            info.widths = new float[textSizes.length][text.length()];
            float maxWidth = 0;
            for (int i=0; i<textSizes.length; i++) {
                paint.setTextSize(textSizes[i]);
                paint.getTextWidths(text, info.widths[i]);
                float wid = DroidUtils.sum(info.widths[i]);
                if (wid > maxWidth)
                    maxWidth = wid;
            }
            
            int texWidth = Math.round(maxWidth + 0.5f);
            int texHeight = DroidUtils.sum(textSizes) + FONT_BITMAP_VPADDING*textSizes.length;
            bitmap = Bitmap.createBitmap(texWidth, texHeight, Bitmap.Config.ALPHA_8);
            info.width = texWidth;
            info.height = texHeight;
            Canvas canvas = new Canvas(bitmap);
            canvas.drawARGB(0, 0, 0, 0);
            int y = FONT_BITMAP_VPADDING/2;
            for (int h : textSizes) {
                y += h;
                paint.setTextSize(h);
                canvas.drawText(text, 0, y, paint);
                y += FONT_BITMAP_VPADDING;
            }
            try {
                DataOutputStream dout = new DataOutputStream(sContext.openFileOutput(BITMAP_FILE, Context.MODE_PRIVATE));
                try {
                	dout.writeInt(texWidth);
                	dout.writeInt(texHeight);
                	dout.writeInt(info.sizes.length);
                	for (int s : info.sizes)
                		dout.writeInt(s);
                	dout.writeInt(info.widths.length);
                	dout.writeInt(info.widths[0].length);
                	for (float [] W : info.widths) {
                		for (float w : W) {
                			dout.writeFloat(w);
                		}
                	}
                    int [] pixels = new int[texWidth * texHeight];
                    bitmap.getPixels(pixels, 0, texWidth, 0, 0, texWidth, texHeight);
                    for (int pix : pixels) {
                        dout.write(pix);
                    }
                    dout.flush();
                } finally {
                    dout.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        info.id = bindBitmap(bitmap);
    }
    
    private int bindBitmap(Bitmap bitmap) {
        
        if (textures == null) {
            textures = new int[512];
        }
        
        if (numTextures >= textures.length) {
            Log.w(TAG, "Too many textures");
            return 0;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        int w2 = DroidUtils.nearestPowerOf2(width);
        int h2 = DroidUtils.nearestPowerOf2(height);

        if (width != w2 || height != h2) {
            // need to scale the bitmap to be a power of 2
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, w2, h2, false);
            bitmap.recycle();
            bitmap = scaled;
        }
        
        
        //Generate one texture pointer...
        //sGl.glPixelStorei(GL10.GL_PACK_ALIGNMENT, 1);
        sGl.glGenTextures(1, textures, numTextures);
        //...and bind it to our array
        int id = textures[numTextures++];
        sGl.glBindTexture(GL10.GL_TEXTURE_2D, id);

        //Create Nearest Filtered Texture
        sGl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR);
        sGl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR);

        //Different possible texture parameters, e.g. GL10.GL_CLAMP_TO_EDGE
        sGl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT);
        sGl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT);

        //Use the Android GLUtils to specify a two-dimensional texture image from our bitmap
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bitmap, 0);
        images.put(id, new GL10Image(bitmap));
        bitmap.recycle();
        return id;
        
    }

    /*
     * return the width of the text
     */
    private float priv_drawJustifiedString(GL10 gl, float x, float y, Justify justify, String text) {
        if (text == null || text.length() == 0)
            return 0;
        
        FontInfo info = getFontInfo();
        float size = this.getTextHeight();
        //int tx = 0;
        int ty = 0;
        float textWidth = 0;
        int widthRow = 0;
        // find the row in bitmap to read from
        for (int i=0; i<info.sizes.length-1; i++) {
            if (size > info.sizes[i]) {
                ty += info.sizes[i] + FONT_BITMAP_VPADDING;
            } else {
                widthRow = i;
                size = info.sizes[i]; // set to actual size
                break;
            }
        }
        
        for (int i=0; i<text.length(); i++) {
            int ch = (int)text.charAt(i) - CHAR_START_POS;
            textWidth += info.widths[widthRow][ch];
        }

        final float sx = x;
        final float sy = y;
        
        sGl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
        // these setting allow to render using alpha channel from ALPHA_8 bitmap
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL10.GL_TEXTURE_ENV_MODE, GL11.GL_COMBINE);
        
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_RGB, GL10.GL_REPLACE);
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_RGB, GL11.GL_PRIMARY_COLOR);
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_RGB, GL11.GL_SRC_COLOR);
        
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_COMBINE_ALPHA, GL10.GL_MODULATE);
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_SRC0_ALPHA, GL10.GL_TEXTURE);
        sGl.glTexEnvx(GL10.GL_TEXTURE_ENV, GL11.GL_OPERAND0_ALPHA, GL10.GL_SRC_ALPHA);

        enableTexture(info.id);
        pushMatrix();
        translate(sx, sy);
        scale(getViewportScaleX(), getViewportScaleY());

        switch (justify) {
            case LEFT: break;                
            case RIGHT: translate(-textWidth,0); break; 
            case CENTER: translate(-textWidth/2, 0); break; 
            default:
            	throw new IllegalArgumentException("Invalid value for horizontal justify: " + justify);
        }
        

        //final float sizeScaled = size * getViewportScaleY();
        for (int i=0; i<text.length(); i++) {
            final int ch = (int)text.charAt(i) - CHAR_START_POS;
            float chWidth = info.widths[widthRow][ch];
            float tx = DroidUtils.sum(info.widths[widthRow], 0, ch);
            
            final float tx0 = tx/info.width;
            final float tx1 = (tx+chWidth)/info.width;
            final float ty0 = (ty+3)/info.height;
            final float ty1 = (ty+size+FONT_BITMAP_VPADDING)/info.height;
            
            //chWidth *= getViewportScaleX();

            //sGl.glTexSubImage2D(GL10.GL_TEXTURE_2D, 0, tx0, ty0, chWidth, size, GL11.GL_ALPHA, GL10.GL_UNSIGNED_BYTE, pixels)
            
            begin();
            texCoord(tx0, ty0);
            texCoord(tx1, ty0);
            texCoord(tx0, ty1);
            texCoord(tx1, ty1);
            
            //vertex(sx, sy);
            //vertex(sx+chWidth, sy);
            //vertex(sx, sy+sizeScaled);
            //vertex(sx+chWidth, sy+sizeScaled);
            vertex(0, 0);
            vertex(chWidth, 0);
            vertex(0, size);
            vertex(chWidth, size);
            drawTriangleStrip();
            
            //sx += chWidth;
            translate(chWidth, 0);
        }
        disableTexture();
        popMatrix();

        sGl.glTexEnvf(GL10.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);
        sGl.glBlendFunc(GL10.GL_ONE, GL10.GL_ONE_MINUS_SRC_ALPHA);
        
        return textWidth;
    }    
        
    /**
     * 
     * @param viewportWidth
     * @param viewportHeight
     */
    public final void initViewport(int viewportWidth, int viewportHeight) {
        super.initViewport(viewportWidth, viewportHeight);
        sGl.glViewport(0, 0, viewportWidth, viewportHeight);
    }
    
    /**
     * 
     */
    public final void shutDown() {
        if (sGl != null && textures != null) {
            try {
//                sGl.glDeleteTextures(numTextures, textures, 0);
//                sGl.glFinish();
            } catch (Exception e) {
                e.printStackTrace();
            }
            textures =  null;
            numTextures = 0;
        }
        //sStringBitmaps.clear();
        sVbb = null;
        sTbb = null;
        sVfb = null;
        sTfb = null;
    }

    /**
     * 
     */
    public final void beginScene() {
        if (sVbb == null) {
            sVbb = ByteBuffer.allocateDirect(1024 * 4);
            sVbb.order(ByteOrder.nativeOrder());
        }
        sGl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
    }

    /**
     * 
     * @param newWidth
     * @return
     */
    public final float setLineWidth(float newWidth) {
        float oldWidth = sLineWidth;
        sLineWidth = newWidth;
        sGl.glLineWidth(newWidth);
        return oldWidth;
    }

    /**
     * 
     * @param newSize
     * @return
     */
    public final float setPointSize(float newSize) {
        float oldSize = sPointSize;
        sPointSize = newSize;
        sGl.glPointSize(newSize);
        return oldSize;
    }
    
    /**
     * 
     */
    public final void endScene() {
        sGl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        sGl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
    }
    
    /**
     * 
     */
    public final void begin() {
        sVfb = sVbb.asFloatBuffer();
        if (sTbb != null) {
            sTfb = sTbb.asFloatBuffer();
            sTfb.position(0);
        }
        sVfb.position(0);
        sVCount = 0;
    }

    /**
     * 
     * @param x
     * @param y
     */
    public void vertex(float x, float y) {
        sVfb.put(x); sVfb.put(y);
        sVCount ++;
    }

    @Override
    public void moveTo(float dx, float dy) {
        if (sVCount > 0) {
            float x = sVfb.get(sVCount-1);
            float y = sVfb.get(sVCount-1);
            dx = x + dx;
            dy = y + dy;
        }
        vertex(dx, dy);
    }

    /**
     * 
     */
    public final void drawPoints() {
        drawVertices2D(sGl, GL10.GL_POINTS, sVCount);
    }

    /**
     * 
     */
    public final void drawLines() {
        drawVertices2D(sGl, GL10.GL_LINES, sVCount);
    }

    @Override
    public final void drawLineStrip() {
        drawVertices2D(sGl, GL10.GL_LINE_STRIP, sVCount);
    }

    @Override
    public final void drawLineLoop() {
        drawVertices2D(sGl, GL10.GL_LINE_LOOP, sVCount);
    }

    @Override
    public final void drawTriangles() {
        drawVertices2D(sGl, GL10.GL_TRIANGLES, sVCount);
    }

    @Override
    public final void drawTriangleFan() {
        drawVertices2D(sGl, GL10.GL_TRIANGLE_FAN, sVCount);
    }
    
    @Override
    public final void drawQuadStrip() {
    	drawVertices2D(sGl, GL10.GL_TRIANGLE_STRIP, sVCount);
    }

    @Override
    public void drawRects() {

    }

    @Override
    public void drawFilledRects() {

    }

    @Override
    public final void drawTriangleStrip() {
        sGl.glFrontFace(GL10.GL_CCW);
        drawVertices2D(sGl, GL10.GL_TRIANGLE_STRIP, sVCount);
    }
    
    private void drawVertices2D(GL10 gl, int mode, int num) {
        sVfb.position(0);
        if (sTextureEnabled) {
            sTfb.position(0);
            gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, sTfb);
        }
        gl.glVertexPointer(2, GL10.GL_FLOAT, 0, sVfb);
        gl.glDrawArrays(mode, 0, num);        
    }    

    private Bitmap loadBitmap(int resourceId) throws Exception {
        InputStream is = null;
        try {
            is = sContext.getResources().openRawResource(resourceId);
            Bitmap bitmap = BitmapFactory.decodeStream(is);
            return bitmap;
        } finally {
            try {
                if (is != null) {
                    is.close();
                    is = null;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 
     * @param resourceId
     * @return
     */
    public final int loadImage(int resourceId) {
        try {
            Bitmap bitmap = loadBitmap(resourceId);
            return bindBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        } 
    }

    private Bitmap loadBitmap(String assetPath) {
        InputStream in = null;
        try {
            in = sContext.getAssets().open(assetPath);
            return BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            //e.printStackTrace();
            Log.e(TAG, "Failed to load '" + assetPath + "'");
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }

    private Bitmap setTransparency(Bitmap bitmap, GColor transparent) {
        final int w = bitmap.getWidth();
        final int h = bitmap.getHeight();
        final int t = transparent.toARGB();

        final int tr = (t&0xff0000)>>16;
        final int tg = (t&0xff00  )>>8;
        final int tb = (t&0xff    )>>0;
        
        int [] argb = new int[w * h];
        try {
            bitmap.getPixels(argb, 0, w, 0, 0, w, h);
            
            for (int i=0; i<w*h; i++) {
                int x = argb[i];
                final int r = (x&0xff0000)>>16;
                final int g = (x&0xff00  )>>8;
                final int b = (x&0xff    )>>0;
                
                if (Math.abs(r - tr) < 3 && Math.abs(g - tg) < 3 && Math.abs(b - tb) < 3) {
                    // set this color
                    argb[i] = 0;
                }
            }
            Bitmap newBitmap = Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888);
            bitmap.recycle();
            return newBitmap;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to setTransparency for color: '" + transparent + "' " + e.getClass() + " " + e.getMessage());
            e.printStackTrace();
        }
        return bitmap;
    }

    public final int loadImage(String assetPath, GColor transparent) {
    	long startTime = SystemClock.uptimeMillis();
    	try {
            Log.d(TAG, "loadImage: " + assetPath + ", transparent = " + transparent);
            Bitmap bitmap = loadBitmap(assetPath);
            if (bitmap == null)
                return 0;
            if (transparent != null)
                bitmap = setTransparency(bitmap, transparent);
            
            return bindBitmap(bitmap);
    	} finally {
    		Log.d(TAG, "loaded in: " + (SystemClock.uptimeMillis() - startTime) + " msecs");
    	}
    }

    /**
     * 
     * @param id
     */
    public final void enableTexture(int id) {
        if (id == 0)
            return;
        if (sTbb == null) {
            sTbb = ByteBuffer.allocateDirect(1024*4);
            sTbb.order(ByteOrder.nativeOrder());
            sTfb = sTbb.asFloatBuffer();
        }
        sGl.glEnable(GL10.GL_TEXTURE_2D);
        sGl.glBindTexture(GL10.GL_TEXTURE_2D, id);
        sGl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        sTfb.position(0);
        sTextureEnabled = true;
    }

    /**
     * 
     */
    public final void disableTexture() {
        sGl.glDisable(GL10.GL_TEXTURE_2D);
        sGl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);
        sTextureEnabled = false;
    }

    /**
     * 
     * @param s
     * @param t
     */
    public final void texCoord(float s, float t) {
        sTfb.put(s);
        sTfb.put(t);
        //sTCount ++;
    }

    @Override
    public void multMatrix(Matrix3x3 m) {
        sGl.glMultMatrixf(m.toFloatArray(), 0);
    }

    /**
     * 
     */
    public final void pushMatrix() {
        sGl.glPushMatrix();
        viewportScale[pushDepth+1][0] = getViewportScaleX();
        viewportScale[pushDepth+1][1] = getViewportScaleY();
        pushDepth++;
    }
    
    /**
     * 
     */
    public final void popMatrix() {
        sGl.glPopMatrix();
        pushDepth--;
        DroidUtils.debugAssert(pushDepth>=0, "pushDepth invalid: " + pushDepth);
    }

    @Override
    public void getTransform(Matrix3x3 result) {
        throw new RuntimeException("Not implemented");
    }

    /**
     * 
     * @param x
     * @param y
     * @param z
     */
    public final void translate(float x, float y, float z) {
        sGl.glTranslatef(x, y, z);
    }

    /**
     * 
     * @param x
     * @param y
     */
    public final void translate(float x, float y) {
        sGl.glTranslatef(x, y, 0);
    }

    /**
     * 
     * @param x
     * @param y
     * @param z
     */
    public final void scale(float x, float y, float z) {
        sGl.glScalef(x, y, z);
    }
    
    /**
     * 
     * @param imageKey
     * @param x
     * @param y
     * @param w
     * @param h
     */
    public final void drawImage(int imageKey, int x, int y, int w, int h) {
        enableTexture(imageKey);
        drawFilledRect(x, y, w, h);
        disableTexture();
    }

    @Override
    public void drawImage(int imageKey) {
        throw new GException("Not Implemented");
    }

    /**
     * 
     * @param source
     * @param x
     * @param y
     * @param w
     * @param h
     * @return
     */
    public final int newSubImage(Bitmap source, int x, int y, int w, int h) {
        Bitmap subimage = Bitmap.createBitmap(source, x, y, w, h);
        return bindBitmap(subimage);
    }

	@Override
	public void deleteImage(int id) {
		sGl.glDeleteTextures(1, new int [] {id}, 0);
		
	}

    /**
     * 
     * @param source
     * @param width
     * @param height
     * @param num_cells_x
     * @param num_cells
     * @param celled
     * @return
     */
    public final int [] loadImageCells(Bitmap source, int width, int height, int num_cells_x, int num_cells, boolean celled) {
        
        final int cellDelta = celled ? 1 : 0;
        
        int x=cellDelta;
        int y=cellDelta;
        int [] result = new int[num_cells];

        int nx = 0;
        for (int i=0; i<num_cells; i++) {
            result[i] = newSubImage(source, x, y, width, height);
            if (++nx == num_cells_x) {
                nx = 0;
                x=celled ? 1 : 0;
                y+=height + cellDelta;
            } else {
                x += width + cellDelta;
            }           
        }
        
        return result;
    }
    
    /**
     * Convenience method
     * @param fileName
     * @param width
     * @param height
     * @param num_cells_x
     * @param num_cells
     * @param bordered
     * @return
     */
    public final int [] loadImageCells(String fileName, int width, int height, int num_cells_x, int num_cells, boolean bordered, GColor transparentColor) {
        InputStream in = null;
        try {
            in = sContext.getAssets().open(fileName);
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            bitmap = setTransparency(bitmap, transparentColor);
            return loadImageCells(bitmap, width, height, num_cells_x, num_cells, bordered);
        } catch (Exception e) {
            e.printStackTrace();
            return new int[0];
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {}
            }
        }
    }
        

    /**
     * Convenience method, use getSourceImage(sourceId) as source Image.
     * 
     * @param sourceId
     * @param width
     * @param height
     * @param nx
     * @param ny
     * @param celled
     * @return
     */
    public final int [] loadImageCells(int sourceId, int width, int height, int nx, int ny, boolean celled) {
        return loadImageCells(textures[sourceId], width, height, nx, ny, celled);
    }
    
    /**
     * 
     * @param color
     */
    public final void clearScreen(GColor color) {
        sGl.glClearColor(color.getRed(), 
                color.getGreen(), color.getBlue(), color.getAlpha());
        sGl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
    }

    private GColor curColor = GColor.BLACK;
    private GColor backColor = GColor.BLACK;
    
    @Override
    public final void setColor(GColor color) {
        sGl.glColor4f(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
        curColor = color;
    }
    
    @Override
	public void setColorARGB(int argb) {
    	int a = (argb >> 24) & 0xff;
		int r = (argb >> 16) & 0xff;
		int g = (argb >>  8) & 0xff;
		int b = (argb >>  0) & 0xff;
		sGl.glColor4x(r,g,b,a);
	}
	
    @Override
	public void setColor(int r, int g, int b, int a) {
    	sGl.glColor4x(r, g, b, a);
	}

    @Override
    public final GColor getColor() {
        return curColor;
    }

    @Override
    public GColor getBackgroundColor() {
        return backColor;
    }

    /**
     *
     * @param backColor
     */
    public void setBackgroundColor(GColor backColor) {
        this.backColor = backColor;
    }

    @Override
    public final void transform(float x, float y, float[] result) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public final AImage getImage(int id) {
        return images.get(id);
    }
    
	@Override
    public final AImage getImage(int id, int width, int height) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public final int newSubImage(int id, int x, int y, int w, int h) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public final int newRotatedImage(int id, int degrees) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public int newTransformedImage(int id, IImageFilter filter) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public final void setIdentity() {
        this.sGl.glLoadIdentity();
    }

    @Override
    public final void rotate(float degrees) {
        sGl.glRotatef(degrees, 0, 0, 1);
    }

    @Override
    public final void scale(float x, float y) {
        sGl.glScalef(x, y, 1);
    }

    @Override
    public final boolean isTextureEnabled() {
        return sTextureEnabled;
    }

    @Override
    public final void ortho(float left, float right, float top, float bottom) {
        viewportScale[pushDepth][0] = (right-left)/this.getViewportWidth();
        viewportScale[pushDepth][1] = (bottom-top)/this.getViewportHeight();
        sGl.glOrthof(left, right, bottom, top, 1, -1);
    }

    private float getViewportScaleX() {
        return viewportScale[pushDepth][0];
    }

    private float getViewportScaleY() {
        return viewportScale[pushDepth][1];
    }

    private float [][] viewportScale = new float[32][2];
    private int pushDepth = 0;

    private static abstract class StateParam {
        final int param;
        final int numValues;
        StateParam(int param, int numValues) {
            this.param = param;
            this.numValues = numValues;
        }
        abstract void reset(GL10 gl, int [] value, int offset);
    }
    
    private static class EnableStateParam extends StateParam {

        EnableStateParam(int param) {
            super(param,1);
        }

        @Override
        void reset(GL10 gl, int [] value,int offset) {
            if (value[offset] == 0) {
                gl.glDisable(param);
            } else {
                gl.glEnable(param);
            }
        }
        
    }
    
    static StateParam [] stateParams = {
        new EnableStateParam(GL10.GL_BLEND),
        new EnableStateParam(GL10.GL_ALPHA_TEST),
        new EnableStateParam(GL10.GL_DEPTH_TEST),
        new EnableStateParam(GL10.GL_CULL_FACE),
        new EnableStateParam(GL10.GL_LINE_SMOOTH),
        new EnableStateParam(GL10.GL_STENCIL_TEST),
        new EnableStateParam(GL10.GL_DITHER),
        new StateParam(GL11.GL_CURRENT_COLOR, 4) { void reset(GL10 gl, int [] value, int offset) { gl.glColor4x(value[offset+0], value[offset+1], value[offset+2], value[offset+3]); } },
        new StateParam(GL11.GL_DEPTH_FUNC, 1) { void reset(GL10 gl, int [] value, int offset) { gl.glDepthFunc(value[offset]); } },
        new StateParam(GL11.GL_POINT_SIZE, 1) { void reset(GL10 gl, int [] value, int offset) { gl.glPointSize(value[offset]); } },
        new StateParam(GL11.GL_LINE_WIDTH, 1) { void reset(GL10 gl, int [] value, int offset) { gl.glLineWidth(value[offset]); }},
    };
    
    static int stateParamSize = 0;
    
    static {
        for (StateParam p : stateParams) {
            stateParamSize += p.numValues;
        }
    }

    private static class GLState {
        int [] params = new int[stateParamSize];
    }
    
    private Stack<GLState> stateStack = new Stack<GLState>();
    public final void pushGlState() {
        GLState state = new GLState();
        int offset = 0;
        for (StateParam p: stateParams) {
            sGl.glGetIntegerv(p.param, state.params, offset);
            offset += p.numValues;
        }
        stateStack.push(state);
    }
    
    public final void popGlState() {
        GLState state = stateStack.pop();
        int offset = 0;
        for (StateParam p: stateParams) {
            p.reset(sGl, state.params, offset);
            offset += p.numValues;
        }
    }

	@Override
	public Vector2D screenToViewport(int screenX, int screenY) {
		throw new RuntimeException("not implemented: screenToViewport");
	}

	@Override
	public void clearMinMax() {
		throw new RuntimeException("not implemented: clearMinMax");
	}

	@Override
	public Vector2D getMinBoundingRect() {
		throw new RuntimeException("not implemented: getMinBoundingRect");
	}

	@Override
	public Vector2D getMaxBoundingRect() {
		throw new RuntimeException("not implemented: getMaxBoundingRect");
	}

    @Override
    public void setClipRect(float x, float y, float w, float h) {
        throw new RuntimeException("not implemented: setClipRect");
    }

    @Override
    public void clearClip() {
        throw new RuntimeException("not implemented: clearClip");
    }

    @Override
    public GRectangle getClipRect() {
        throw new RuntimeException("not implemented: getClipRect");
    }

    @Override
    public void setTransparencyFilter(float alpha) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void removeFilter() {

    }

    @Override
    public void drawRoundedRect(float x, float y, float w, float h, float radius) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawFilledRoundedRect(float x, float y, float w, float h, float radius) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawWedge(float cx, float cy, float radius, float startDegrees, float sweepDegrees) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawArc(float x, float y, float radius, float startDegrees, float sweepDegrees) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawCircle(float x, float y, float radius) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawOval(float x, float y, float w, float h) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void drawFilledOval(float x, float y, float w, float h) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void setTintFilter(GColor inColor, GColor outColor) {
        throw new RuntimeException("Not implemented");
    }

}
