package cc.lib.game;

import cc.lib.math.Vector2D;

public interface IDimension {
    float getWidth();

    float getHeight();

    default float getAspect() {
        return getWidth() / getHeight();
    }

    default IVector2D getCenter() {
        return new Vector2D(getWidth() / 2, getHeight() / 2);
    }


    /**
     * Return a rectangle with the aspect ratio of 'this' that contains t
     * entire rect of target grown and 'filled' top keep aspect ratio
     *
     * @param target
     * @return
     */
    default GRectangle fillFit(GRectangle target) {
        float x0, y0, w0, h0;
        float A = getAspect();
        if (A <= 0)
            throw new IllegalArgumentException("Cannot fit empty rect");
        if (A < target.getAspect()) {
            w0 = target.w;
            h0 = w0 / A;
            x0 = 0;
            y0 = target.h / 2 - getHeight() / 2;
        } else {
            h0 = target.h;
            w0 = h0 * A;
            y0 = 0;
            x0 = target.w / 2 - getWidth() / 2;
        }
        return new GRectangle(x0, y0, w0, h0);
    }

    /**
     * Return a rectangle with same aspect as 'this' with maximum amount of rect
     * cropped to keep aspect
     *
     * @param target
     * @return
     */
    default GRectangle cropFit(GRectangle target) {
        float x0, y0, w0, h0;
        float A = getAspect();
        if (A <= 0)
            throw new IllegalArgumentException("Cannot fit empty rect");
        if (A < target.getAspect()) {
            h0 = target.h;
            w0 = h0 * A;
            y0 = 0;
            x0 = target.getWidth() / 2 - w0 / 2;
        } else {
            w0 = target.w;
            h0 = w0 / A;
            x0 = 0;
            y0 = target.getHeight() / 2 - h0 / 2;
        }
        return new GRectangle(x0, y0, w0, h0);
    }

    /**
     * Return a rectangle with aspect ratio of 'this' and entire contents
     * of target grown to meet aspect ratio and adjusted to be in bounds
     *
     * @param target
     * @return
     */
    default GRectangle fitInner(GRectangle target) {
        float A = getAspect();
        if (A <= 0)
            throw new IllegalArgumentException("Cannot fit empty rect");
        GRectangle rect = new GRectangle(target);
        rect.setAspect(A);

        if (rect.w > getWidth() || rect.h > getHeight()) {
            rect.w = getWidth();
            rect.h = getHeight();
        }

        if (rect.x < 0) {
            rect.x = 0;
        }
        if (rect.y < 0) {
            rect.y = 0;
        }
        if (rect.x + rect.w > getWidth()) {
            rect.x = getWidth() - rect.w;
        }
        if (rect.y + rect.h > getHeight()) {
            rect.y = getHeight() - rect.h;
        }
        return rect;
    }
}
