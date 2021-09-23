package cc.lib.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.ViewGroup;

/**
 * Created by chriscaron on 2/13/18.
 *
 * Convenience class for getting fullscreen game up without a layout file.
 *
 * Just override the draw method
 *
 */

public abstract class DroidActivity extends CCActivityBase {

    private DroidGraphics g = null;
    private DroidView content = null;
    private ViewGroup topBar = null;
    private int margin = 0;

    /**
     *
     * @param margin
     */
    public void setMargin(int margin) {
        this.margin = margin;
        content.postInvalidate();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewId());
        content = (DroidView)findViewById(R.id.droid_view);
        topBar = (ViewGroup)findViewById(R.id.top_bar_layout);
    }

    protected int getContentViewId() {
        return R.layout.droid_activity;
    }

    /**
     * Called after onCreate
     * @return
     */
    public ViewGroup getTopBar() {
        return topBar;
    }

    public DroidView getContent() {
        return content;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (g != null)
            g.releaseBitmaps();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    protected abstract void onDraw(DroidGraphics g);

    protected void onTap(float x, float y) {}

    protected void onTouchDown(float x, float y) {}

    protected void onTouchUp(float x, float y) {}

    protected void onDrag(float x, float y) {}

    protected void onDragStart(float x, float y) {}

    protected void onDragStop(float x, float y) {}

    private AlertDialog currentDialog = null;

    protected int getDialogTheme() {
        return R.style.DialogTheme;
    }

    public final AlertDialog.Builder newDialogBuilder() {
        final AlertDialog previous = currentDialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this, getDialogTheme()) {
            @Override
            public AlertDialog show() {
                if (currentDialog != null) {
                    currentDialog.dismiss();
                }
                currentDialog = super.show();
                onDialogShown(currentDialog);
                return currentDialog;
            }
        }.setCancelable(false);
        if (shouldDialogAddBackButton() && currentDialog != null && currentDialog.isShowing()) {
                builder.setNeutralButton(R.string.popup_button_back, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    currentDialog = previous;
                    previous.show();
                    }
                });
            }
        return builder;
    }

    protected boolean shouldDialogAddBackButton() {
        return true;
    }

    protected void onDialogShown(Dialog d) {}

    public void dismissCurrentDialog() {
        if (currentDialog != null) {
            currentDialog.dismiss();
            currentDialog = null;
        }
    }

    public boolean isCurrentDialogShowing() {
        return currentDialog != null && currentDialog.isShowing();
    }

    public void redraw() {
        getContent().postInvalidate();
    }
}
