package cc.game.zombicide.android;

import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import cc.lib.game.GColor;
import cc.lib.zombicide.ZColor;
import cc.lib.zombicide.ZPlayerName;
import cc.lib.zombicide.ZSkill;

/**
 * Created by Chris Caron on 8/9/21.
 */
class SkillsDialog extends PagerAdapter {

    final ZombicideActivity activity;
    final ZSkill[][][] skills = new ZSkill[ZPlayerName.values().length][][];
    final String[] labels = new String[skills.length];

    SkillsDialog(ZombicideActivity activity) {
        this.activity = activity;
        // series of TABS, one for each character and then one for ALL

        int idx = 0;
        for (ZPlayerName pl : ZPlayerName.values()) {
            skills[idx] = new ZSkill[ZColor.values().length][];
            labels[idx] = pl.getLabel();
            for (ZColor lvl : ZColor.values()) {
                skills[idx][lvl.ordinal()] = pl.getSkillOptions(lvl);
            }
            idx++;
        }

        View view = View.inflate(activity, R.layout.viewpager_dialog, null);
        ViewPager pager = view.findViewById(R.id.view_pager);
        //pager.setAdapter(new Page
        pager.setAdapter(this);

        if (activity.game.getCurrentCharacter() != null) {
            pager.setCurrentItem(activity.game.getCurrentCharacter().ordinal());
        }

        activity.newDialogBuilder().setTitle("Skills").setView(view).setNegativeButton("Close", null).show();
    }

    @Override
    public int getCount() {
        return skills.length;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        View page = View.inflate(activity, R.layout.skills_page, null);
        TextView title = page.findViewById(R.id.tv_title);
        title.setText(labels[position]);
        ListView lv = page.findViewById(R.id.lv_list);
        lv.setAdapter(new SkillAdapter(ZPlayerName.values()[position], skills[position]));
        container.addView(page);
        return page;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View) object);
    }

    class Item {
        final String name;
        final String description;
        final GColor color;
        final boolean owned;

        public Item(String name, String description, GColor color, boolean owned) {
            this.name = name;
            this.description = description;
            this.color = color;
            this.owned = owned;
        }
    }

    class SkillAdapter extends BaseAdapter {

        final List<Item> items = new ArrayList<>();

        SkillAdapter(ZPlayerName pl, ZSkill[][] skills) {
            for (ZColor lvl : ZColor.values()) {
                items.add(new Item(lvl.name() + " " + lvl.dangerPts + " Danger Points", null, lvl.color, false));
                for (ZSkill skill : skills[lvl.ordinal()]) {
                    boolean owned = pl.getCharacter() != null && pl.getCharacter().hasSkill(skill);
                    items.add(new Item(skill.getLabel(), skill.description, null, owned));
                }
            }
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(activity, R.layout.skill_list_item, null);
            }
            TextView name = convertView.findViewById(R.id.tv_label);
            TextView desc = convertView.findViewById(R.id.tv_description);

            Item item = items.get(position);
            if (item.color == null) {
                name.setTextColor(GColor.WHITE.toARGB());
                desc.setVisibility(View.VISIBLE);
                desc.setText(item.description);
                if (item.owned) {
                    SpannableString content = new SpannableString(item.name);
                    content.setSpan(new UnderlineSpan(), 0, content.length(), 0);
                    name.setText(content);
                } else {
                    name.setText(item.name);
                }
            } else {
                name.setText(item.name);
                name.setTextColor(item.color.toARGB());
                desc.setVisibility(View.GONE);
            }

            return convertView;
        }
    }

}
