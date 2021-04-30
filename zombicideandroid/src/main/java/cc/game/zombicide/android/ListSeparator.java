package cc.game.zombicide.android;

import android.content.Context;
import android.widget.ImageView;

public class ListSeparator extends ImageView {
    public ListSeparator(Context context) {
        super(context);
        int padding = (int)context.getResources().getDimension(R.dimen.list_sep_padding);
        setPadding(0, padding, 0, padding);
        setImageResource(R.drawable.divider_horz);
    }
}
