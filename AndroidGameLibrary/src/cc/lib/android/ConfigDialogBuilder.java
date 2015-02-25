package cc.lib.android;

import java.util.*;

import android.content.Context;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.TextView.OnEditorActionListener;

public class ConfigDialogBuilder {

    final Context context;
    
    public ConfigDialogBuilder(Context context) {
        this.context = context;
    }
    
    public enum ValueType {
        BOOLEAN,
        INTEGER,
        STRING,
        INT_CHOICE,
        STRING_CHOICE,
        ENUM_CHOICE
    }
    
    public static abstract class Config<T> {
        public abstract T get();
        public abstract void set(T value);
        
        final ValueType type;
        
        public Config(ValueType type) {
            this.type = type;
        }    
        
        public int getMin() { return 0; }
        public int getMax() { return 100; }
        public T [] getChoices() { return null; }
        public boolean isEnabled() { return true; }
    }
    
    Map<String, Config<?>> values = new LinkedHashMap<String, Config<?>>();
    
    public void addConfig(String name, Config<?> config) {
        values.put(name,  config);
    }
    
    public BaseAdapter constructAdapter() {
        return new ConfigListAdapter();
    }

    class ConfigListAdapter extends BaseAdapter {
        
        @SuppressWarnings("unchecked")
        Map.Entry<String, Config<?>> [] entries = values.entrySet().toArray(new Map.Entry[values.size()]);
        
        @Override
        public int getCount() {
            return entries.length;
        }

        @Override
        public Object getItem(int position) {
            return entries[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private <T> T increment(T currentValue, T ... options) {
            int index = 0;
            for ( ; index < options.length; index++) {
                if (currentValue == options[index])
                    break;
            }
            index = (index+1) % options.length;
            return options[index];
        }
        
        @Override
        public boolean isEnabled(int position) {
            return entries[position].getValue().isEnabled();
        }

        @SuppressWarnings("unchecked")
        @Override
        public View getView(int position, View view, ViewGroup parent) {
            if (view == null)
                view = View.inflate(context, R.layout.configelem, null);
            final TextView tv = (TextView)view.findViewById(R.id.textView1);
            final SeekBar sb  = (SeekBar)view.findViewById(R.id.seekBar1);
            final EditText et = (EditText)view.findViewById(R.id.editText1);
            final TextView sbt = (TextView)view.findViewById(R.id.textView2); 
            final ToggleButton tb = (ToggleButton)view.findViewById(R.id.toggleButton1);
            final Button cb = (Button)view.findViewById(R.id.choiceButton1);
            
            final Map.Entry<String, Config<?>> entry = entries[position];
            tv.setText(entry.getKey());
            
            sb.setVisibility(View.GONE);
            et.setVisibility(View.GONE);
            sbt.setVisibility(View.GONE);
            tb.setVisibility(View.GONE);

            switch (entries[position].getValue().type) {
                case INTEGER: {
                    final Config<Integer> config = (Config<Integer>)entries[position].getValue();
                    int value = config.get();
                    //Main.HorizontalSlider slider = new HorizontalSlider(Main.this);
                    sb.setMax(config.getMax());
                    sb.setProgress(value);
                    sbt.setText("" + value);
                    sb.setVisibility(View.VISIBLE);
                    sbt.setVisibility(View.VISIBLE);
                    
                    sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                        @Override
                        public void onStopTrackingTouch(SeekBar seekBar) { config.set(seekBar.getProgress()); }
                        @Override
                        public void onStartTrackingTouch(SeekBar seekBar) {}
                        @Override
                        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { sbt.setText("" + progress); }
                    });
                    
                    break;
                }
                    
                case BOOLEAN: {
                    final Config<Boolean> config = (Config<Boolean>)entries[position].getValue();
                    boolean value = config.get();
                    tb.setChecked(value);
                    tb.setVisibility(View.VISIBLE);
                    tb.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View arg0) { config.set(tb.isChecked()); }
                    });
                    break;
                }
                case STRING: {
                    final Config<String> config = (Config<String>)entries[position].getValue();                    
                    et.setVisibility(View.VISIBLE);
                    et.setText(config.get());
                    et.setOnEditorActionListener(new OnEditorActionListener() {
                        @Override
                        public boolean onEditorAction(TextView arg0, int arg1, KeyEvent enterKey) {
                            if (enterKey != null) {
                                config.set(tv.getText().toString());
                                return true;
                            }
                            return false;
                        }
                    });
                    break;
                }
                
                case INT_CHOICE: {
                    final Config<Integer> config = (Config<Integer>)entries[position].getValue();
                    cb.setVisibility(View.VISIBLE);
                    cb.setText(config.get().toString());
                    cb.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) { 
                            int newValue = increment(config.get(), config.getChoices());
                            config.set(newValue);
                            cb.setText(String.valueOf(newValue)); 
                        }
                    });
                    break;
                }
                
                case STRING_CHOICE: {
                    final Config<String> config = (Config<String>)entries[position].getValue();
                    cb.setVisibility(View.VISIBLE);
                    cb.setText(config.get());
                    cb.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) { 
                            String newValue = increment(config.get(), config.getChoices());
                            config.set(newValue);
                            cb.setText(newValue); 
                        }
                    });
                    break;
                }                
                
                case ENUM_CHOICE: {
                    final Config<Enum<?>> config = (Config<Enum<?>>)entries[position].getValue();
                    cb.setVisibility(View.VISIBLE);
                    cb.setText(config.get().name());
                    cb.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) { 
                            Enum<?> newValue = increment(config.get(), config.getChoices());
                            config.set(newValue);
                            cb.setText(String.valueOf(newValue)); 
                        }
                    });
                    break;
                }                
            }

            return view;
        }
        
    }
    
}
