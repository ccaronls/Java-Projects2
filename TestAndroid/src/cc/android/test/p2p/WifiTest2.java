package cc.android.test.p2p;

import android.Manifest;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.LinkedList;

import cc.android.test.R;
import cc.lib.android.CCActivityBase;
import cc.lib.android.SpinnerTask;
import cc.lib.game.GColor;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;
import cc.lib.net.GameServer;

public class WifiTest2 extends CCActivityBase implements
        View.OnClickListener,
        GameServer.Listener,
        GameClient.Listener,
        Runnable {

    static final String TAG = WifiTest2.class.getSimpleName();

    GameServer server;
    GameClient client;
    ListView listView;
    EditText editText;
    View bSearchHosts, bBecomeHost, bDisconnect;
    final LinkedList<ListItem> items = new LinkedList<>();
    final MyAdapter adapter = new MyAdapter();

    final int PORT = 31313;
    final String VERSION = "1.0";
    final int MAX_CONNECTIONS = 10;
    int maxClients = 2;

    enum State {
        DISCONNECTED,
        HOSTING,
        CLIENT_SEARCHING,
        CLIENT_CONNECTED
    }

    static class ListItem {
        final String text;
        final int color;
        final boolean leftJustify;

        public ListItem(String text, int color, boolean leftJustify) {
            this.text = text;
            this.color = color;
            this.leftJustify = leftJustify;
        }
    }

    class MyAdapter extends BaseAdapter {


        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                view = new TextView(WifiTest2.this);
            }

            TextView tv = (TextView)view;
            //tv.setTextSize(DroidUtils.convertDipsToPixels(WifiTest2.this, 18));
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
            ListItem item = items.get(i);

            tv.setText(item.text);
            tv.setTextColor(item.color);
            tv.setGravity(item.leftJustify ? Gravity.LEFT : Gravity.RIGHT);

            return view;
        }
    }

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.wifi2_activity);
        (bBecomeHost = findViewById(R.id.bBecomeHosts)).setOnClickListener(this);
        (bSearchHosts = findViewById(R.id.bSearchHosts)).setOnClickListener(this);
        (bDisconnect = findViewById(R.id.bDisconnect)).setOnClickListener(this);
        findViewById(R.id.bSend).setOnClickListener(this);
        listView = findViewById(R.id.listview);
        listView.setAdapter(adapter);
        editText = findViewById(R.id.edittext);
        initButtons(State.DISCONNECTED);
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkPermissions(getRequiredPermissions());
    }

    @Override
    protected void onAllPermissionsGranted() {
        hideKeyboard();
    }

    GameCommandType TEXT_MSG = new GameCommandType("TEXT_MSG");
    GameCommandType ASSIGN_COLOR = new GameCommandType("ASSIGN_COLOR");

    GameCommand getMessageCommand(String msg, int color, String source) {
        return new GameCommand(TEXT_MSG).setArg("color", color).setName(source).setMessage(msg);
    }

    private void sendText() {
        String text = editText.getText().toString();
        if (text.length() > 0) {
            addListEntry(text, Color.BLACK, true);
            if (client != null) {
                client.sendCommand(getMessageCommand(text, Color.RED, null));
            } else if (server != null) {
                server.broadcastCommand(getMessageCommand(text, Color.RED, getClientName()));
            }
        }
        editText.getText().clear();
    }

    // *********************************************************************************
    //
    //       CLIENT
    //
    // *********************************************************************************

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bBecomeHosts:
                server = new GameServer(getClientName(), PORT, 20000, VERSION, null, MAX_CONNECTIONS);
                server.addListener(this);
                new P2PServerWaitingDialog(this, server, getClientName(), PORT);                initButtons(State.HOSTING);
                break;
            case R.id.bSearchHosts:
                client = new GameClient(getClientName(), VERSION, null);
                client.addListener(this);
                new P2PJoinGameDialog(this, client, getClientName(), PORT);
                initButtons(State.CLIENT_SEARCHING);
                break;
            case R.id.bDisconnect:
                killMPGame();
                initButtons(State.DISCONNECTED);
                break;
            case R.id.bSend:
                sendText();
                break;
        }
    }

    synchronized void addListEntry(String text, int color, boolean leftJustify) {
        items.addFirst(new ListItem(text, color, leftJustify));
        runOnUiThread(this);
    }

    public void run() {
        adapter.notifyDataSetChanged();
    }

    void initButtons(State state) {
        switch (state) {
            case DISCONNECTED:
                bDisconnect.setEnabled(false);
                bSearchHosts.setEnabled(true);
                bBecomeHost.setEnabled(true);
                break;
            case HOSTING:
            case CLIENT_CONNECTED:
            case CLIENT_SEARCHING:
                bDisconnect.setEnabled(true);
                bSearchHosts.setEnabled(false);
                bBecomeHost.setEnabled(false);
                break;
        }
    }

    String[] getRequiredPermissions() {
        return new String[]{
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.CHANGE_NETWORK_STATE,
                    Manifest.permission.INTERNET
            };
    }

    String getClientName() {
        return String.format("%s-%s-%s WifiTest2-%s", Build.MANUFACTURER ,Build.PRODUCT, Build.VERSION.SDK_INT, VERSION);
    }

    int [] colors = {
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            GColor.ORANGE.toARGB(),
            Color.RED,
            Color.GREEN,
            Color.DKGRAY,
            Color.GRAY,
            Color.LTGRAY
    };

    int curColor = 0;

    // GameServer callbacks

    @Override
    public synchronized void onConnected(ClientConnection conn) {
        addListEntry("Client Connected: " + conn.getDisplayName(), colors[curColor], false);
        conn.sendCommand(new GameCommand(ASSIGN_COLOR).setArg("color", colors[curColor]));
        curColor = (curColor+1) % colors.length;
    }

    @Override
    public void onReconnection(ClientConnection conn) {
        addListEntry("Client Reconnected: " + conn.getDisplayName(), Color.YELLOW, false);
    }

    @Override
    public void onClientDisconnected(ClientConnection conn) {
        addListEntry("Client Disconnected: " + conn.getDisplayName(), Color.RED, false);
    }

    @Override
    public void onCommand(ClientConnection conn, GameCommand cmd) {
        if (cmd.getType() == TEXT_MSG) {
            int color = cmd.getInt("color");
            addListEntry(conn.getDisplayName() + ":" + cmd.getMessage(), color, false);

            for (ClientConnection c : server.getConnectionValues()) {
                if (c == conn)
                    continue;
                c.sendCommand(cmd.setName(conn.getDisplayName()));
            }

        }
    }

    // GameClient callbacks

    int myColor = Color.BLACK;

    @Override
    public void onCommand(GameCommand cmd) {
        if (cmd.getType() == TEXT_MSG) {
            int color = cmd.getInt("color");
            addListEntry(cmd.getMessage(), color, false);
        } else if (cmd.getType() == ASSIGN_COLOR) {
            myColor = cmd.getInt("color");
            addListEntry("Color Assigned", myColor, true);
        } else {
            addListEntry(cmd.getType().name() + ":" + cmd.getMessage(), Color.RED, false);
        }
    }

    @Override
    public void onMessage(String msg) {
        addListEntry("MSG: " + msg, Color.BLUE, false);
    }

    @Override
    public void onDisconnected(String reason) {
        addListEntry("Disconnected: " + reason, Color.RED, true);
    }

    @Override
    public void onConnected() {
        addListEntry("Connected", Color.GREEN, true);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                initButtons(State.CLIENT_CONNECTED);
            }
        });
    }

    public void killMPGame() {
        if (server != null) {
            new SpinnerTask(this) {
                @Override
                protected void doIt(String... args) throws Exception {
                    server.stop();
                }
            }.execute();
        }
        if (client != null) {
            client.disconnect();
        }
    }
}
