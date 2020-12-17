package cc.game.zombicide.android;

import android.content.Context;
import android.widget.Button;

public class ZButton extends Button {

    public ZButton(Context context, Enum e, OnClickListener listener) {
        this(context, e.name(), e, listener);
    }

    public ZButton(Context context, String label, Object tag, OnClickListener listener) {
        super(context);
        setText(label);
        setTag(tag);
        setOnClickListener(listener);
    }
}
