package cc.game.soc.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
import java.util.Vector;

import cc.game.soc.core.Board;
import cc.game.soc.core.BuildableType;
import cc.game.soc.core.ResourceType;
import cc.game.soc.core.Rules;
import cc.game.soc.core.SOC;
import cc.game.soc.ui.MenuItem;
import cc.game.soc.ui.RenderConstants;
import cc.game.soc.ui.UIBarbarianRenderer;
import cc.game.soc.ui.UIBoardRenderer;
import cc.game.soc.ui.UIConsoleRenderer;
import cc.game.soc.ui.UIDiceRenderer;
import cc.game.soc.ui.UIEventCardRenderer;
import cc.game.soc.ui.UIPlayer;
import cc.game.soc.ui.UIPlayerRenderer;
import cc.game.soc.ui.UIPlayerUser;
import cc.game.soc.ui.UISOC;
import cc.lib.android.ArrayListAdapter;
import cc.lib.android.CCActivityBase;
import cc.lib.android.CCNumberPicker;
import cc.lib.game.GColor;
import cc.lib.game.Utils;

/**
 * Created by chriscaron on 2/15/18.
 */

public class SOCActivity extends CCActivityBase implements MenuItem.Action {

    UISOC soc = null;
    File rulesFile;
    File gameFile;
    View content;

    SOCView<UIBarbarianRenderer> vBarbarian;
    SOCView<UIEventCardRenderer> vEvent;
    SOCView<UIBoardRenderer> vBoard;
    SOCView<UIDiceRenderer> vDice;
    SOCView<UIPlayerRenderer> vUser;
    SOCView<UIPlayerRenderer> vPlayerTop;
    SOCView<UIPlayerRenderer> vPlayerlMiddle;
    SOCView<UIPlayerRenderer> vPlayerlBottom;
    UIConsoleRenderer console;
    ListView lvMenu;

    Stack<Dialog> dialogStack = new Stack<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RenderConstants.textMargin = getResources().getDimension(R.dimen.margin);
        RenderConstants.textSizeBig = getResources().getDimension(R.dimen.text_big);
        RenderConstants.textSizeSmall = getResources().getDimension(R.dimen.text_sm);
        RenderConstants.thickLineThickness = getResources().getDimension(R.dimen.line_thick);
        RenderConstants.thinLineThickness = getResources().getDimension(R.dimen.line_thin);

        content = View.inflate(this, R.layout.soc_activity, null);
        setContentView(content);
        vBarbarian = (SOCView) findViewById(R.id.soc_barbarian);
        vEvent = (SOCView) findViewById(R.id.soc_event_cards);
        vBoard = (SOCView) findViewById(R.id.soc_board);
        vDice = (SOCView) findViewById(R.id.soc_dice);
        vUser = (SOCView) findViewById(R.id.soc_user);
        vPlayerTop = (SOCView) findViewById(R.id.soc_player_top);
        vPlayerlMiddle = (SOCView) findViewById(R.id.soc_player_middle);
        vPlayerlBottom = (SOCView) findViewById(R.id.soc_player_bottom);
        lvMenu = (ListView) findViewById(R.id.soc_menu_list);
        console = new UIConsoleRenderer(new SOCView<>(this));

        // initially only the board is visible
        vBarbarian.setVisibility(View.GONE);
        vEvent.setVisibility(View.GONE);
        vDice.setVisibility(View.INVISIBLE);
        vUser.setVisibility(View.INVISIBLE);
        vPlayerTop.setVisibility(View.INVISIBLE);
        vPlayerlBottom.setVisibility(View.INVISIBLE);
        vPlayerlMiddle.setVisibility(View.INVISIBLE);
        lvMenu.setVisibility(View.INVISIBLE);

        final ArrayList<Object[]> menu = new ArrayList<>();
        final BaseAdapter adapter = new ArrayListAdapter<Object[]>(this, menu, R.layout.menu_list_item) {
            @Override
            protected void initItem(View v, int position, Object[] item) {
                final MenuItem mi = (MenuItem) item[0];
                final String title = (String) item[1];
                final String helpText = (String) item[2];
                final Object extra = item[3];

                TextView tvTitle = (TextView) v.findViewById(R.id.tvTitle);
                final TextView tvHelp = (TextView) v.findViewById(R.id.tvHelp);

                tvTitle.setText(title);
                tvHelp.setText(helpText);

                v.findViewById(R.id.bAction).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mi.action.onAction(mi, extra);
                    }
                });
                v.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (tvHelp.getVisibility() == View.VISIBLE) {
                            tvHelp.setVisibility(View.GONE);
                        } else {
                            tvHelp.setVisibility(View.VISIBLE);
                        }
                        return true;
                    }
                });
            }
        };
        lvMenu.setAdapter(adapter);

        gameFile = new File(getFilesDir(), "save.txt");
        rulesFile = new File(getFilesDir(), "rules.txt");

        UIPlayerRenderer[] players = {
                vUser.renderer,
                vPlayerTop.renderer,
                vPlayerlMiddle.renderer,
                vPlayerlBottom.renderer
        };

        if (UISOC.getInstance() == null)
            soc = new UISOC(players, vBoard.renderer, vDice.renderer, console, vEvent.renderer, vBarbarian.renderer) {
                @Override
                protected void addMenuItem(MenuItem item, String title, String helpText, Object extra) {
                    menu.add(new Object[]{
                            item, title, helpText, extra
                    });
                }

                @Override
                public void completeMenu() {
                    super.completeMenu();
                    addMenuItem(CONSOLE);
                    addMenuItem(BUILDABLES);
                    addMenuItem(RULES);
                    addMenuItem(QUIT);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void clearMenu() {
                    menu.clear();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            adapter.notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void redraw() {
                    content.postInvalidate();
                }

                @Override
                protected void showOkPopup(final String title, final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            newDialog(true).setTitle(title).setMessage(message).setNeutralButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    synchronized (soc) {
                                        soc.notify();
                                    }
                                }
                            }).show();
                        }
                    });
                    Utils.waitNoThrow(this, -1);
                }

                @Override
                protected String getServerName() {
                    return Build.BRAND + "." + Build.PRODUCT;
                }

                @Override
                public String getString(int resourceId, Object... args) {
                    return getResources().getString(resourceId, args);
                }

                @Override
                protected void onShouldSaveGame() {
                    trySaveToFile(gameFile);
                }
            };
        soc.setBoard(vBoard.getRenderer().getBoard());
    }



    final UIPlayerUser user = new UIPlayerUser();

    final MenuItem QUIT = new MenuItem("Quit", "Exit to main menu", this);
    final MenuItem BUILDABLES = new MenuItem("Buidlables", "See how to build things", this);
    final MenuItem RULES = new MenuItem("Rules", "View or Edit the game rules", this);
    final MenuItem START = new MenuItem("Start", "Start the game", this);
    final MenuItem CONSOLE = new MenuItem("Console", "View console messages", this);

    @Override
    public void onAction(MenuItem item, Object extra) {
        if (item == QUIT) {
            newDialog(true).setTitle("Confirm").setMessage("Ae you sure you want to quit the game?")
                    .setPositiveButton("Quit", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            soc.server.stop();
                            soc.stopRunning();
                            user.client.disconnect("player quit");
                            soc.clearMenu();
                            showStartDialog();
                        }
                    }).show();
        } else if (item == BUILDABLES) {
            showBuildablesDialog();
        } else if (item == RULES) {
            showRulesDialog();
        } else if (item == START) {
            soc.initGame();
            soc.startGameThread();
        } else if (item == CONSOLE) {
            showConsole();
        }
    }

    Dialog consoleDialog = null;

    void showConsole() {
        if (consoleDialog == null) {
            consoleDialog = newDialog(true).setView((SOCView)console.getComponent()).create();
        };
        consoleDialog.show();
    }

    void showError(Exception e) {
        newDialog(true).setTitle("ERROR").setMessage("AN error occured: " + e.getClass().getSimpleName() + " " + e.getMessage()).show();
    }

    void showStartDialog() {
        String [] items = {
                "Single Player",
                "Multiplyer",
                "Resume",
                "Rules",
                "Boards",
                "Scenarios"
        };

        newDialog(false).setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    switch (which) {
                        case 0: {
                            final String [] items = { "2", "3", "4" };
                            newDialog(true).setTitle("Num Players").setItems(items, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    final int numPlayers = which+2;
                                    final String [] colorStrings = {
                                            "Red", "Green", "Blue", "Yellow"
                                    };
                                    final GColor[]  colors = {
                                            GColor.RED, GColor.GREEN, GColor.BLUE, GColor.YELLOW
                                    };
                                    newDialog(true).setTitle("Pick Color").setItems(colorStrings, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            user.setColor(colors[which]);
                                            soc.clear();
                                            soc.addPlayer(user);
                                            for (int i=1; i<numPlayers; i++) {
                                                int nextColor = (which+1)%colors.length;
                                                soc.addPlayer(new UIPlayer(colors[nextColor]));
                                            }
                                            initGame();
                                        }
                                    }).show();
                                }
                            }).setNegativeButton("Cancel", null).show();
                            break;
                        }
                        case 1: {
                            // multiplayer
                            String [] mpItems = {
                                    "Host",
                                    "Join"
                            };
                            newDialog(true).setTitle("MULTIPLAYER")
                                    .setItems(mpItems, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {

                                        }
                                    }).show();
                            break;
                        }
                        case 2: {
                            // resume
                            try {
                                soc.loadFromFile(gameFile);
                                soc.startGameThread();
                            } catch (Exception e) {
                                soc.clear();
                                showError(e);
                            }
                            break;
                        }
                        case 3: {
                            // rules
                            showRulesDialog();
                            break;
                        }
                        case 4: {
                            // boards
                            final String[] boards = getAssets().list("boards");
                            newDialog(true).setTitle("Load Board").setItems(boards, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    try {
                                        InputStream in = getAssets().open("boards/" + boards[which]);
                                        try {
                                            Board b = new Board();
                                            b.deserialize(in);
                                            soc.setBoard(b);
                                        } finally {
                                            in.close();
                                        }
                                    } catch (IOException e) {
                                        showError(e);
                                    }
                                }
                            }).setNegativeButton("Cancel", null).show();
                            break;
                        }
                        case 5: {
                            final String [] scenarios = getAssets().list("scenarios");
                            newDialog(true).setTitle("Load Scenario").setItems(scenarios, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    try {
                                        InputStream in = getAssets().open("scenarios/" + scenarios[which]);
                                        try {
                                            SOC scenario = new SOC();
                                            scenario.deserialize(in);
                                            soc.copyFrom(scenario);
                                        } finally {
                                            in.close();
                                        }
                                    } catch (IOException e) {
                                        showError(e);
                                    }
                                }
                            }).setNegativeButton("Cancel", null).show();
                            break;
                        }
                    }
                } catch (Exception e) {
                    showError(e);
                }
            }
        }).show();
    }

    void initGame() {

        int index = 0;
        SOCView<UIPlayerRenderer> [] opponents = Utils.toArray(vPlayerTop, vPlayerlMiddle, vPlayerlBottom);

        for (int i=1; i<=soc.getNumPlayers(); i++) {
            if (soc.getPlayerByPlayerNum(i) instanceof UIPlayerUser) {
                vUser.renderer.setPlayer(i);
            } else {
                opponents[index++].renderer.setPlayer(i);
            }
        }

        vPlayerTop.setVisibility(View.VISIBLE);
        vUser.setVisibility(View.VISIBLE);
        lvMenu.setVisibility(View.VISIBLE);
        soc.clearMenu();
        soc.addMenuItem(START);
        soc.completeMenu();
        vBarbarian.setVisibility(soc.getRules().isEnableCitiesAndKnightsExpansion() ? View.VISIBLE  : View.GONE);
        if (soc.getRules().isEnableEventCards()) {
            vEvent.setVisibility(View.VISIBLE);
            vDice.setVisibility(View.GONE);
        } else {
            vEvent.setVisibility(View.GONE);
            vDice.setVisibility(View.VISIBLE);
        }

        vPlayerlMiddle.setVisibility(soc.getNumPlayers() > 2 ? View.VISIBLE : View.GONE);
        vPlayerlBottom.setVisibility(soc.getNumPlayers() > 3 ? View.VISIBLE : View.GONE);
        clearDialogs();
    }

    void showBuildablesDialog() {
        Vector<String> columnNames = new Vector<String>();
        columnNames.add("Buildable");
        for (ResourceType r : ResourceType.values()) {
            columnNames.add(r.name());

        }
        Vector<Vector<String>> rowData = new Vector<>();
        for (BuildableType b : BuildableType.values()) {
            if (b.isAvailable(soc)) {
                Vector<String> row = new Vector<>();
                row.add(b.name());
                for (ResourceType r : ResourceType.values())
                    row.add(String.valueOf(b.getCost(r)));
                rowData.add(row);
            }
        }

        TableLayout table = new TableLayout(this);
        TableRow header = new TableRow(this);

        for (String s : columnNames) {
            TextView t = new TextView(this);
            t.setText(s);
            header.addView(t);
        }

        table.addView(header);
        for (Vector<String> r : rowData) {
            TableRow row = new TableRow(this);
            for (String s : r) {
                TextView t = new TextView(this);
                t.setText(s);
                row.addView(t);
            }
            table.addView(row);
        }
        newDialog(true).setTitle("Buildables").setView(table).setNegativeButton("Ok", null).show();
    }

    class RuleItem implements Comparable<RuleItem> {
        final Rules.Variation var;
        final int min, max;
        final int stringId;
        final Field field;

        public RuleItem(Rules.Variation var) {
            this.var = var;
            this.min = max = 0;
            field = null;
            this.stringId = var.stringId;
        }

        public RuleItem(Rules.Rule rule, Field field) {
            this.var = rule.variation();
            this.min = rule.minValue();
            this.max = rule.maxValue();
            this.field = field;
            this.stringId = rule.stringId();
        }

        @Override
        public int compareTo(@NonNull RuleItem o) {
            if (var != o.var)
                return var.compareTo(o.var);
            if (field == null)
                return -1;
            if (o.field == null)
                return 1;
            return field.getName().compareTo(o.field.getName());
        }
    }

    void showRulesDialog() {
        final boolean canEdit = !soc.isRunning() && !user.client.isConnected();
        final Rules r = soc.getRules().deepCopy();

        final List<RuleItem> rules = new ArrayList<>();
        for (Rules.Variation v : Rules.Variation.values())
            rules.add(new RuleItem(v));
        for (Field f : Rules.class.getDeclaredFields()) {
            Annotation[] anno = f.getAnnotations();
            for (Annotation a : anno) {
                if (a.annotationType().equals(Rules.Rule.class)) {
                    f.setAccessible(true);
                    final Rules.Rule ruleVar = (Rules.Rule) a;
                    rules.add(new RuleItem(ruleVar, f));
                }
            }
        }
        Collections.sort(rules);
        ListView lv = new ListView(this);
        BaseAdapter rulesAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return rules.size();
            }

            @Override
            public Object getItem(int position) {
                return null;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View v, ViewGroup parent) {
                if (v == null) {
                    v = View.inflate(SOCActivity.this, R.layout.rules_list_item, null);
                }
                TextView tvHeader  = (TextView)v.findViewById(R.id.tvHeader);
                TextView tvName    = (TextView)v.findViewById(R.id.tvName);
                TextView tvDesc    = (TextView)v.findViewById(R.id.tvDescription);
                CompoundButton cb  = (CompoundButton)v.findViewById(R.id.cbEnabled);
                final Button bEdit = (Button)v.findViewById(R.id.bEdit);

                try {
                    final RuleItem item = rules.get(position);
                    if (item.field == null) {
                        // header
                        tvHeader.setVisibility(View.VISIBLE);
                        tvName.setVisibility(View.GONE);
                        tvDesc.setVisibility(View.GONE);
                        cb.setVisibility(View.GONE);
                        bEdit.setVisibility(View.GONE);
                        tvHeader.setText(item.stringId);
                    } else if (item.field.getType().equals(boolean.class)) {
                        // checkbox
                        tvHeader.setVisibility(View.GONE);
                        tvName.setVisibility(View.VISIBLE);
                        tvDesc.setVisibility(View.VISIBLE);
                        cb.setVisibility(View.VISIBLE);
                        bEdit.setVisibility(View.GONE);
                        tvName.setText(item.field.getName());
                        tvDesc.setText(item.stringId);
                        cb.setChecked(item.field.getBoolean(rules));
                        cb.setEnabled(canEdit);
                        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                try {
                                    item.field.setBoolean(rules, isChecked);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    } else if (item.field.getType().equals(int.class)) {
                        // numberpicker
                        tvHeader.setVisibility(View.GONE);
                        tvName.setVisibility(View.VISIBLE);
                        tvDesc.setVisibility(View.VISIBLE);
                        cb.setVisibility(View.GONE);
                        bEdit.setVisibility(View.VISIBLE);
                        tvName.setText(item.field.getName());
                        tvDesc.setText(item.stringId);
                        final int value = item.field.getInt(rules);
                        bEdit.setText(String.valueOf(item.field.getInt(rules)));
                        bEdit.setEnabled(canEdit);
                        bEdit.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final NumberPicker np = CCNumberPicker.newPicker(SOCActivity.this, value, item.min, item.max, null);
                                newDialog(false).setTitle(item.field.getName()).setView(np).setNegativeButton("Cancel", null)
                                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                try {
                                                    item.field.setInt(rules, np.getValue());
                                                    bEdit.setText(String.valueOf(np.getValue()));
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }).show();
                            }
                        });
                    } else {
                        log.error("Dont know how to handle field: " + item.field.getName());
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return v;
            }
        };
        lv.setAdapter(rulesAdapter);
        AlertDialog.Builder b = newDialog(true).setTitle("Rules").setView(lv);
        if (canEdit) {
            b.setNegativeButton("Discard", null).setNeutralButton("Save", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    r.trySaveToFile(rulesFile);
                    soc.setRules(r);
                }
            }).setPositiveButton("Keep", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    soc.setRules(r);
                }
            }).show();
        } else {
            b.setNegativeButton("Ok", null).show();
        }
    }

    void copyFileToExt() {
        try {
//            FileUtils.copyFile(saveFile, Environment.getExternalStorageDirectory());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showStartDialog();
        /*
        if (soc.getNumPlayers() > 0 && soc.getWinner() == null)
            soc.startGameThread();
        else if (!soc.isGameRunning())
            showNewGameDialog(false);

        if (saveFile.exists()) {
            if (Build.VERSION.SDK_INT >= 23) {
                if (checkPermission()) {
                    copyFileToExt();
                } else {
                    requestPermission();
                }
            }
        }
*/
    }

    final int PERMISSION_REQUEST_CODE = 1001;

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    copyFileToExt();
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        soc.stopRunning();
        soc.trySaveToFile(gameFile);
    }

    protected AlertDialog.Builder newDialog(boolean cancelable) {
        AlertDialog.Builder b = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Dialog) {
            @Override
            public AlertDialog show() {
                AlertDialog d = super.show();
                dialogStack.push(d);
                return d;
            }
        };
        if (cancelable) {
            if (dialogStack.size()>0) {
                b.setNegativeButton("Back", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialogStack.pop().show();
                    }
                });
            } else {
                b.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!soc.isRunning()) {
                            showStartDialog();
                        }
                    }
                });
            }
        }
        return b;
    }

    void clearDialogs() {
        while (dialogStack.size() > 0) {
            dialogStack.pop().dismiss();
        }
    }
}
