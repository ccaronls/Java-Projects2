package cc.android.test.p2p;

import android.content.DialogInterface;
import android.graphics.Color;
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
import cc.lib.game.GColor;
import cc.lib.mp.android.P2PActivity;
import cc.lib.net.ClientConnection;
import cc.lib.net.GameClient;
import cc.lib.net.GameCommand;
import cc.lib.net.GameCommandType;
import cc.lib.net.GameServer;

public class WifiTest2 extends P2PActivity implements
        View.OnClickListener,
        GameClient.Listener,
        GameServer.Listener,
        Runnable {

    ListView listView;
    EditText editText;
    View bStart, bShowClients, bDisconnect;
    final LinkedList<ListItem> items = new LinkedList<>();
    final MyAdapter adapter = new MyAdapter();
    P2PServer serverController = null;

//    final int PORT = 31313;
//    final String VERSION = "1.0";
//    final int MAX_CONNECTIONS = 10;
//    int maxClients = 2;


    @Override
    protected int getConnectPort() {
        return 31313;
    }

    @Override
    protected String getVersion() {
        return "1.0";
    }

    @Override
    protected int getMaxConnections() {
        return 2;
    }

    enum State {
        DISCONNECTED,
        HOSTING,
        CLIENT
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
        (bStart = findViewById(R.id.bStart)).setOnClickListener(this);
        (bShowClients = findViewById(R.id.bClients)).setOnClickListener(this);
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
        p2pInit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideKeyboard();
    }

    static GameCommandType TEXT_MSG = new GameCommandType("TEXT_MSG");
    static GameCommandType ASSIGN_COLOR = new GameCommandType("ASSIGN_COLOR");

    GameCommand getMessageCommand(String msg, int color, String source) {
        return new GameCommand(TEXT_MSG).setArg("color", color).setName(source).setMessage(msg);
    }

    @Override
    protected void onP2PReady() {
        String handle = getPrefs().getString("handle", null);
        EditText et = new EditText(this);
        et.setHint("Handle");
        et.setText(handle);

        newDialogBuilder().setTitle("Set Handle")
                .setView(et)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        getPrefs().edit().putString("handle", et.getText().toString()).apply();
                    }
                }).show();




        // do nothing
    }

    @Override
    protected void onP2PClient(P2PClient p2pClient) {
        p2pClient.getClient().addListener(this);
        p2pClient.getClient().setDisplayName(getPrefs().getString("handle", "Unknown"));
        runOnUiThread(new UpdateButtonsRunnable(State.CLIENT));
    }

    @Override
    protected void onP2PServer(P2PServer p2pServer) {
        p2pServer.openConnections();
        p2pServer.getServer().addListener(this);
        this.serverController = p2pServer;
        runOnUiThread(new UpdateButtonsRunnable(State.HOSTING));
    }

    private void sendText() {
        String text = editText.getText().toString();
        if (text.length() > 0) {
            addListEntry(text, Color.BLACK, true);
            if (getClient() != null) {
                getClient().sendCommand(getMessageCommand(text, myColor, null));
            } else if (getServer() != null) {
                getServer().broadcastCommand(getMessageCommand(text, Color.RED, getDeviceName()));
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
            case R.id.bStart:
                p2pStart();
                break;
            case R.id.bClients:
                serverController.openConnections();
                break;
            case R.id.bDisconnect:
                p2pShutdown();
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
                bShowClients.setEnabled(false);
                bStart.setEnabled(true);
                break;
            case HOSTING:
                bDisconnect.setEnabled(true);
                bShowClients.setEnabled(true);
                bStart.setEnabled(false);
                break;
            case CLIENT:
                bDisconnect.setEnabled(true);
                bShowClients.setEnabled(false);
                bStart.setEnabled(false);
                break;
        }
    }

    int [] colors = {
            Color.BLUE,
            Color.CYAN,
            Color.MAGENTA,
            GColor.ORANGE.toARGB(),
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
        addListEntry("Client Reconnected: " + conn.getDisplayName(), GColor.ORANGE.toARGB(), false);
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

            cmd.setName(conn.getDisplayName());
            for (ClientConnection c : getServer().getConnectionValues()) {
                if (c == conn)
                    continue;
                c.sendCommand(cmd);
            }

        }
    }

    @Override
    public void onServerStopped() {
        runOnUiThread(new UpdateButtonsRunnable(State.DISCONNECTED));
    }

    // GameClient callbacks

    int myColor = Color.BLACK;

    @Override
    public void onCommand(GameCommand cmd) {
        if (cmd.getType() == TEXT_MSG) {
            int color = cmd.getInt("color");
            addListEntry(cmd.getName() + ":" + cmd.getMessage(), color, false);
        } else if (cmd.getType() == ASSIGN_COLOR) {
            myColor = cmd.getInt("color");
            addListEntry("Color Assigned", myColor, true);
        } else {
            addListEntry(cmd.getType().name() + ":" + cmd.getMessage(), Color.RED, true);
        }
    }

    @Override
    public void onMessage(String msg) {
        addListEntry("MSG: " + msg, Color.BLUE, false);
    }

    @Override
    public void onDisconnected(String reason, boolean serverInitiated) {
        addListEntry("Disconnected: " + reason, Color.RED, true);
        runOnUiThread(new UpdateButtonsRunnable(State.DISCONNECTED));
    }

    @Override
    public void onConnected() {
        addListEntry("Connected", Color.GREEN, true);
        runOnUiThread(new UpdateButtonsRunnable(State.CLIENT));
    }

    class UpdateButtonsRunnable implements Runnable {
        final State state;

        public UpdateButtonsRunnable(State state) {
            this.state = state;
        }

        public void run() {
            initButtons(state);
        }
    }
}
