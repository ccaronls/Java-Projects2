package cc.game.zombicide.android;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import cc.lib.ui.IButton;

public class ZButton extends ConstraintLayout {

    static ZButton build(Context context, IButton button, boolean enabled) {
        ZButton b = (ZButton)View.inflate(context, R.layout.zbutton_layout, null);
        b.init(button, enabled);
        return b;
    }

    public ZButton(Context context) {
        super(context);
    }

    public ZButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init(IButton button, boolean enabled) {
        TextView tv = findViewById(R.id.text);
        View arrow = findViewById(R.id.bInfo);
        tv.setText(button.getLabel());
        if (button.getTooltipText() == null) {
            arrow.setVisibility(View.GONE);
        } else {
            arrow.setVisibility(View.VISIBLE);
        }
        setTag(button);
        setEnabled(enabled);
        //setOnClickListener(listener);
    }

}
