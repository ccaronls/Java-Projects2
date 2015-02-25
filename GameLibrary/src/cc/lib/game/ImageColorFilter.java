package cc.lib.game;

public class ImageColorFilter implements IImageFilter {

    private final int oldColor;
    private final int newColor;
    private final int variance;
    
    public ImageColorFilter(int oldColor, int newColor, int variance) {
       this.oldColor = oldColor;
       this.newColor = newColor;
       this.variance = variance;
    }

    public ImageColorFilter(AColor oldColor, AColor newColor, int variance) {
        this.oldColor = oldColor.toARGB();
        this.newColor = newColor.toARGB();
        this.variance = variance;
     }

    
    @Override
    public int filterRGBA(int x, int y, int argb) {
        int r = argb & 0x00ff0000;
        int g = argb & 0x0000ff00;
        int b = argb & 0x000000ff;
        
        int dr = Math.abs(r - (oldColor & 0x00ff0000));
        int dg = Math.abs(g - (oldColor & 0x0000ff00));
        int db = Math.abs(b - (oldColor & 0x000000ff));
        
        if ( dr <= variance && dg <= variance && db <= variance )
        {
            // Mark the alpha bits as zero - transparent
            return newColor;
        }
        else 
        {
            // nothing to do
            return argb;
        }
        
    }

    
    
}
