package cc.game.zombicide.android;

import android.app.Dialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import cc.game.zombicide.android.databinding.AssignDialogItemBinding;
import cc.game.zombicide.android.databinding.AssignDialogSpBinding;
import cc.game.zombicide.android.databinding.AssignDialogSpListviewItemBinding;
import cc.lib.zombicide.ZQuests;

/**
 * Created by Chris Caron on 3/22/22.
 */
abstract class CharacterChooserDialogSP extends BaseAdapter implements ViewPager.OnPageChangeListener {

    final static String TAG = CharacterChooserDialogSP.class.getSimpleName();

    final ZombicideActivity activity;
    final LayoutInflater inflater;
    final ZombicideActivity.CharLock[] charLocks;
    final Set<String> selectedPlayers;
    final AssignDialogSpBinding binding;
    final ZQuests quest;

    public CharacterChooserDialogSP(ZombicideActivity activity, ZQuests quest) {
        this.activity = activity;
        this.quest = quest;
        inflater = LayoutInflater.from(activity);
        charLocks = activity.charLocks;
        selectedPlayers = new HashSet(activity.getStoredCharacters());
        binding = AssignDialogSpBinding.inflate(inflater);
        binding.listView.setAdapter(this);
        binding.viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return charLocks.length;
            }

            @Override
            public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
                return view == o;
            }

            @NonNull
            @Override
            public Object instantiateItem(@NonNull ViewGroup container, int position) {
                AssignDialogItemBinding item = AssignDialogItemBinding.inflate(inflater);
                item.tvP2PName.setVisibility(View.GONE);
                ZombicideActivity.CharLock lock = charLocks[position];
                item.checkbox.setVisibility(View.GONE);
                if (!lock.isUnlocked() && !selectedPlayers.contains(lock.player.name())) {
                    item.lockedOverlay.setVisibility(View.VISIBLE);
                    item.tvLockedReason.setVisibility(View.VISIBLE);
                    item.tvLockedReason.setText(lock.unlockMessageId);
                } else {
                    item.lockedOverlay.setVisibility(View.INVISIBLE);
                    item.tvLockedReason.setVisibility(View.GONE);
                }
                item.image.setImageResource(lock.player.cardImageId);
                container.addView(item.getRoot());
                return item.getRoot();
            }

            @Override
            public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
                container.removeView((View) object);
            }

            @Override
            public int getItemPosition(@NonNull Object object) {
                return POSITION_NONE;
            }
        });
        binding.viewPager.addOnPageChangeListener(this);
        Dialog d = activity.newDialogBuilder().setTitle(R.string.popup_title_choose_players).setView(binding.getRoot()).show();
        binding.bCancel.setOnClickListener(v -> d.dismiss());
        binding.bClear.setOnClickListener(v -> {
            selectedPlayers.clear();
            notifyDataSetChanged();
            binding.viewPager.getAdapter().notifyDataSetChanged();
        });
        binding.bStart.setOnClickListener(v -> {
            Log.d(TAG, "Selected players: " + selectedPlayers);
            if (selectedPlayers.size() < 1) {
                Toast.makeText(activity, R.string.toast_msg_minplayers, Toast.LENGTH_LONG).show();
                return;
            }

            onStarted();
            d.dismiss();
        });
    }

    @Override
    public int getCount() {
        return charLocks.length;
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
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = AssignDialogSpListviewItemBinding.inflate(inflater).getRoot();
        }

        ZombicideActivity.CharLock[] charLocks = activity.charLocks;
        ZombicideActivity.CharLock lock = charLocks[position];
        CheckBox cb = view.findViewById(R.id.checkbox);
        TextView tv = view.findViewById(R.id.textview);
        tv.setText(lock.player.getLabel());
        cb.setChecked(selectedPlayers.contains(lock.player.name()));
        if (cb.isChecked() || lock.isUnlocked()) {
            cb.setClickable(true);
            cb.setEnabled(true);
            cb.setOnTouchListener((v,event) -> {
                if (event.getAction() != MotionEvent.ACTION_DOWN)
                    return false;
                if (cb.isChecked()) {
                    selectedPlayers.remove(lock.player.name());
                    cb.setChecked(false);
                } else if (selectedPlayers.size() < activity.MAX_PLAYERS) {
                    selectedPlayers.add(lock.player.name());
                    cb.setChecked(true);
                } else {
                    Toast.makeText(activity, activity.getString(R.string.toast_msg_maxplayers, activity.MAX_PLAYERS), Toast.LENGTH_LONG).show();
                }
                return true;
            });
        } else {
            cb.setClickable(false);
            cb.setEnabled(false);
        }
        view.setOnClickListener(v -> binding.viewPager.setCurrentItem(position, true));
        return view;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        binding.listView.setItemChecked(position, true);
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    abstract void onStarted();
}
