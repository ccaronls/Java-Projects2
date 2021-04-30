package cc.game.zombicide.android;

import android.content.Context;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import cc.lib.ui.IButton;

public class ZButton extends ConstraintLayout implements View.OnClickListener, View.OnLongClickListener {

    IButton b;

    static ZButton build(Context context, IButton button, OnClickListener listener) {
        ZButton b = (ZButton)View.inflate(context, R.layout.zbutton_layout, null);
        b.init(button, listener);
        return b;
    }

    public ZButton(Context context) {
        super(context);
    }

    public ZButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void init(IButton button, OnClickListener listener) {
        TextView tv = findViewById(R.id.text);
        View arrow = findViewById(R.id.bInfo);
        tv.setText(button.getLabel());
        if (button.getTooltipText() == null) {
            arrow.setVisibility(View.GONE);
        } else {
            b = button;
            arrow.setOnClickListener(this);
            setOnLongClickListener(this);
        }
        setTag(button);
        setOnClickListener(listener);
    }

    @Override
    public boolean onLongClick(View v) {
        onClick(v);
        return true;
    }

    @Override
    public void onClick(View v) {
        View popup = View.inflate(getContext(), R.layout.tooltippopup_layout, null);
        ((TextView)popup.findViewById(R.id.header)).setText(b.getLabel());
        ((TextView)popup.findViewById(R.id.text)).setText(b.getTooltipText());
        ((ZombicideActivity)v.getContext()).newDialogBuilder().setView(popup).setCancelable(true).show().setCanceledOnTouchOutside(true);
    }
}
