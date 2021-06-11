package cc.game.zombicide.android;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import cc.lib.ui.IButton;

public class ZButton extends ConstraintLayout implements View.OnClickListener, View.OnLongClickListener {

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
            arrow.setOnClickListener(this);
        }
        setTag(button);
        setEnabled(enabled);
        //setOnClickListener(listener);
    }

    @Override
    public boolean onLongClick(View v) {
        onClick(v);
        return true;
    }

    IButton getButton() {
        return (IButton)getTag();
    }

    @Override
    public void onClick(View v) {
        View popup = View.inflate(getContext(), R.layout.tooltippopup_layout, null);
        ((TextView)popup.findViewById(R.id.header)).setText(getButton().getLabel());
        ((TextView)popup.findViewById(R.id.text)).setText(getButton().getTooltipText());
        ((ZombicideActivity)v.getContext()).newDialogBuilder().setView(popup).setCancelable(true).show().setCanceledOnTouchOutside(true);
    }
}
