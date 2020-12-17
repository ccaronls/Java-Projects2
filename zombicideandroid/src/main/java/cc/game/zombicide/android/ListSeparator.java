package cc.game.zombicide.android;

import android.content.Context;
import android.widget.ImageView;

public class ListSeparator extends ImageView {
    public ListSeparator(Context context) {
        super(context);
        setImageResource(R.drawable.divider_horz);
    }
}
