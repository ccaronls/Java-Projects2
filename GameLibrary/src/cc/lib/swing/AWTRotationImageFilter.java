package cc.lib.swing;

import java.awt.image.RGBImageFilter;

class AWTRotationImageFilter extends RGBImageFilter {

    final int [] source;
    final int degrees;
    final int srcWid, srcHgt, dstWid, dstHgt;
    
    
    public AWTRotationImageFilter(int[] source, int degrees, int srcWid, int srcHgt, int dstWid, int dstHgt) {
        super();
        this.source = source;
        this.degrees = degrees;
        this.srcWid = srcWid;
        this.srcHgt = srcHgt;
        this.dstWid = dstWid;
        this.dstHgt = dstHgt;
    }


    @Override
    public int filterRGB(int x, int y, int rgb) {

        int sx = 0;
        int sy = 0;
        switch (degrees) {
            case 0:break;
            case 90:
                sx = y; sy = x; break;
            case 180:
                sx = srcWid - x; 
                sy = srcHgt - y; break;
            case 270:
                sx = srcHgt - y; 
                sy = srcWid - x; break;
        }
        
        return source [sx + sy * srcWid];
    }

}
