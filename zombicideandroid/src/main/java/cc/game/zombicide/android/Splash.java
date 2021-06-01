package cc.game.zombicide.android;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import cc.lib.android.DroidActivity;
import cc.lib.android.DroidGraphics;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.AImage;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.math.Vector2D;

public class Splash extends DroidActivity {

    AAnimation<AGraphics> animation;
    GRectangle rect;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        animation = new AAnimation<AGraphics>(3000) {
            @Override
            protected void draw(AGraphics g, float position, float dt) {
                g.clearScreen(GColor.WHITE);
                Vector2D cntr = new Vector2D(g.getViewportWidth()/2, g.getViewportHeight()/2);
                GDimension minDim = new GDimension(g.getViewportWidth()/4, g.getViewportHeight()/4);
                GDimension maxDim = new GDimension(g.getViewportWidth()/2, g.getViewportHeight()/2);
                rect = new GRectangle().withDimension(minDim.interpolateTo(maxDim, position)).withCenter(cntr);
                //g.setColor(GColor.RED);
                //rect.drawOutlined(g, 5);
                AImage img = g.getImage(R.drawable.zgravestone);
                g.drawImage(R.drawable.zgravestone, rect.fit(img));
            }

            @Override
            protected void onDone() {
                animation = new AAnimation<AGraphics>(2000) {
                    @Override
                    protected void draw(AGraphics g, float position, float dt) {
                        float popupTime = 300;
                        if (getElapsedTime() < popupTime) {
                            position = (float)getElapsedTime() / popupTime;
                            //g.clearScreen(GColor.WHITE.interpolateTo(GColor.BLACK, position));
                        } else {
                            position = 1;
                            g.setColor(GColor.WHITE);
                            float yPos = g.getViewportHeight()/6;
                            g.setTextHeight(yPos/2);
                            g.drawJustifiedString(g.getViewportWidth()/2, yPos, Justify.CENTER, Justify.CENTER, getString(R.string.app_name));
                        }
                        //g.clearScreen(GColor.BLACK);
                        GRectangle handRect = new GRectangle(rect);
                        handRect.w /= 2;
                        handRect.h /= 2;
                        handRect.x += handRect.w;
                        handRect.y += handRect.h;
                        AImage img = g.getImage(R.drawable.zgravestone);
                        g.drawImage(R.drawable.zgravestone, rect.fit(img));
                        img = g.getImage(R.mipmap.zicon);
                        handRect.y += handRect.h * (1f-position);
                        handRect.h *= position;
                        g.drawImage(R.mipmap.zicon, handRect.fit(img));

                    }

                    @Override
                    protected void onDone() {
                        transition();
                    }
                }.start();
            }
        }.start();
    }

    @Override
    protected void onDraw(DroidGraphics g) {
        g.setTextModePixels(true);
        animation.update(g);
        getContent().postInvalidate();
    }

    public void transition() {
        Intent intent = new Intent(this, ZombicideActivity.class);
        startActivity(intent);
        finish();

    }
}
