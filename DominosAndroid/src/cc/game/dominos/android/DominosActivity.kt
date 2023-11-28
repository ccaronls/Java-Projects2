package cc.game.dominos.android

import android.os.Bundle
import android.view.View
import android.widget.RadioGroup
import cc.game.dominos.core.Dominos
import cc.lib.android.DroidActivity
import cc.lib.android.DroidGraphics
import cc.lib.annotation.Keep
import cc.lib.utils.FileUtils
import java.io.File

/**
 * Created by chriscaron on 2/15/18.
 */
class DominosActivity : DroidActivity() {
	private lateinit var dominos: Dominos
	private lateinit var saveFile: File
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		//AndroidLogger.setLogFile(new File(Environment.getExternalStorageDirectory(), "dominos.log"));
		val padding = resources.getDimensionPixelSize(R.dimen.border_padding)
		setMargin(padding)
		saveFile = File(externalStorageDirectory, "dominos.save")
		Dominos.SPACING = resources.getDimension(R.dimen.element_spacing)
		Dominos.TEXT_SIZE = resources.getDimension(R.dimen.info_txt_size)
		dominos = object : Dominos() {
			override fun redraw() {
				this@DominosActivity.redraw()
			}

			@Keep
			override fun onGameOver(winner: Int) {
				super.onGameOver(winner)
				content.postDelayed({ showNewGameDialog() }, 5000)
			}

			override fun onMenuClicked() {
				showNewGameDialog()
			}
		}
		dominos.startDominosIntroAnimation { if (!isCurrentDialogShowing) showNewGameDialog() }
	}

	fun copyFileToExt() {
		try {
			FileUtils.copyFile(saveFile, externalStorageDirectory)
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}

	override fun onResume() {
		super.onResume()
		dominos.redraw()
	}

	override fun onPause() {
		super.onPause()
		dominos.stopGameThread()
	}

	var tx = -1
	var ty = -1
	var dragging = false
	override fun onDraw(g: DroidGraphics) {
		synchronized(this) { dominos.draw(g, tx, ty) }
	}

	override fun onTouchDown(x: Float, y: Float) {
		tx = Math.round(x)
		ty = Math.round(y)
		redraw()
	}

	override fun onTouchUp(x: Float, y: Float) {
		if (dragging) {
			dominos.stopDrag()
			dragging = false
		}
		tx = -1 //Math.round(x);
		ty = -1 //Math.round(y);
		redraw()
	}

	override fun onDrag(x: Float, y: Float) {
		if (!dragging) {
			dominos.startDrag()
			dragging = true
		}
		tx = Math.round(x)
		ty = Math.round(y)
		redraw()
	}

	override fun onTap(x: Float, y: Float) {
		tx = Math.round(x)
		ty = Math.round(y)
		redraw()
		content.postDelayed({
			ty = -1
			tx = ty
			dominos.onClick()
		}, 100)
	}

	fun showNewGameDialog() {
		val v = View.inflate(this, R.layout.new_game_type_dialog, null)
		val b = newDialogBuilder().setTitle(R.string.popup_title_choose_game_type)
			.setView(v).setNegativeButton(R.string.popup_button_cancel, null)
		if (dominos.isInitialized()) {
			b.setNegativeButton(R.string.popup_button_cancel) { dialog, which -> if (!dominos.isGameRunning) dominos.startGameThread() }
		}
		b.show()
		v.findViewById<View>(R.id.bSinglePlayer).setOnClickListener { showNewSinglePlayerSetupDialog() }
		/*
        v.findViewById(R.id.bMultiPlayerHost).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dominos.isGameRunning()) {
                    // this wont work until we get rid of MPPlayer stuff
                    showHostMultiplayerDialog();
                } else {
                    showNewMultiplayerPlayerSetupDialog(true);
                }
            }
        });
        v.findViewById(R.id.bMultiPlayerSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSearchMultiplayerHostsDialog();
            }
        });*/
		val button = v.findViewById<View>(R.id.bResumeSP)
		if (saveFile.exists()) {
			button.setOnClickListener {
				dominos.clear()
				if (dominos.tryLoadFromFile(saveFile) && dominos.isInitialized()) {
					dominos.startGameThread()
					dismissCurrentDialog()
				} else {
					dominos.clear()
					newDialogBuilder().setTitle(R.string.popup_title_error).setMessage(R.string.popup_msg_failed_to_load).setNegativeButton(R.string.popup_button_ok) { dialog, which -> showNewGameDialog() }.show()
				}
			}
		} else {
			button.visibility = View.GONE
		}
	}

	/*
    void showHostMultiplayerDialog() {
        new SpinnerTask() {
            @Override
            protected void doIt() throws Exception {
                server.listen();
                helper = new WifiP2pHelper(DominosActivity.this) {
                    @Override
                    public void onGroupInfo(WifiP2pGroup group) {
                        super.onGroupInfoAvailable(group);
                        server.setPassword(group.getPassphrase());
                    }
                };
                helper.p2pInitialize();
                String build =
                         "\nPRODUCT   :"+Build.PRODUCT
                        +"\nDEVICE    :"+Build.DEVICE
                        +"\nBOARD     :"+Build.BOARD
                        +"\nBRAND     :"+Build.BRAND
                        +"\nHARDWARE  :"+Build.HARDWARE
                        +"\nHOST      :"+Build.HOST
                        +"\nMANFUC    :"+Build.MANUFACTURER
                        +"\nMODEL     :"+Build.MODEL
                        +"\nTYPE      :"+Build.TYPE
                        +"\nVERSION   :"+Build.VERSION.CODENAME;
                log.debug("Device info=%s", build);
                helper.setDeviceName(server.getName() + "-" + getString(R.string.app_name));
                helper.startGroup(); // make sure we are the group owner
            }

            @Override
            protected void onDone() {
                showWaitingForPlayersDialog();
            }

            @Override
            protected void onError(Exception e) {
                newDialogBuilder().setTitle(R.string.popup_title_error).setMessage(getString(R.string.popup_msg_failed_to_start_server, e.getLocalizedMessage())).setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask() {
                            @Override
                            protected void doIt() throws Exception {
                                killGame();
                            }

                            @Override
                            protected void onDone() {
                                showNewGameDialog();
                            }
                        }.run();
                    }
                }).show();
            }

        }.run();
    }*/
	fun killGame() {
		dominos.stopGameThread()
		dominos.clear()
	}

	/*
    void showWaitingForPlayersDialog() {
        dominos.stopGameThread();
        ListView lvPlayers = new ListView(this);
        final BaseAdapter playersAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return server.getNumClients();
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
            public View getView(int position, View v, ViewGroup parent) {
                if (v == null) {
                    v = new TextView(DominosActivity.this);
                }

                AClientConnection conn = server.getConnection(position);
                TextView tv = (TextView)v;
                tv.setText(conn.getName());
                tv.setTextColor(conn.isConnected() ? Color.GREEN : Color.RED);
                tv.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);

                return v;
            }
        };
        lvPlayers.setAdapter(playersAdapter);
        newDialogBuilder().setTitle(R.string.popup_title_waiting_for_players)
                .setView(lvPlayers)
                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new SpinnerTask() {
                            @Override
                            protected void doIt() throws Exception {
                                killGame();
                            }

                            @Override
                            protected void onDone() {
                                showNewGameDialog();
                            }
                        }.run();
                    }
                }).show();
        server.addListener(new GameServer.Listener() {
            @Override
            public synchronized void onConnected(AClientConnection conn) {
                int maxClients = dominos.getNumPlayers()-1;

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

                if (server.getNumConnectedClients() == maxClients) {
                    server.removeListener(this);
                    dominos.startGameThread();
                    dismissCurrentDialog();
                } else {
                    int num = maxClients - server.getNumConnectedClients();
                    server.broadcastMessage(getString(R.string.server_broadcast_waiting_for_n_more_players, num));
                }
            }

            @Override
            public void onReconnection(AClientConnection conn) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onClientDisconnected(AClientConnection conn) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        playersAdapter.notifyDataSetChanged();
                    }
                });

            }
        });
    }

    void showSearchMultiplayerHostsDialog() {
        final ListView lvHost = new ListView(this);
        final List<WifiP2pDevice> p2pDevices = new ArrayList<>();
        final List<BonjourThread.BonjourRecord> dnsDevices = new ArrayList<>();
        final List<Object> devices = new ArrayList<>();
        final BaseAdapter adapter = new BaseAdapter() {
            @Override
            public int getCount() {
                return devices.size();
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
            public View getView(int position, View v, ViewGroup parent) {
                if (v == null) {
                    v = View.inflate(DominosActivity.this, R.layout.list_item_peer, null);
                }

                synchronized (devices) {
                    Object d = devices.get(position);
                    v.setTag(d);
                    TextView tvPeer = (TextView)v.findViewById(R.id.tvPeer);
                    if (d instanceof WifiP2pDevice) {
                        WifiP2pDevice device = (WifiP2pDevice)d;
                        tvPeer.setText(device.deviceName + " " + WifiP2pHelper.statusToString(device.status, DominosActivity.this));
                    } else if (d instanceof BonjourThread.BonjourRecord) {
                        BonjourThread.BonjourRecord record = (BonjourThread.BonjourRecord)d;
                        tvPeer.setText("DNS: " + record.getHostAddress());
                    }
                    tvPeer.setBackgroundColor(position % 2 == 0 ? Color.BLACK : Color.DKGRAY);
                }

                return v;
            }
        };
        lvHost.setAdapter(adapter);

        helper = new WifiP2pHelper(this) {
            @Override
            protected AlertDialog.Builder newDialog() {
                return newDialogBuilder();
            }

            @Override
            protected synchronized void onPeersAvailable(Collection<WifiP2pDevice> peers) {
                p2pDevices.clear();
                for (WifiP2pDevice p : peers) {
                    if (p.isGroupOwner())
                        p2pDevices.add(p); // we only want to connect to dominos servers
                }
                synchronized (devices) {
                    devices.clear();
                    devices.addAll(p2pDevices);
                    devices.addAll(dnsDevices);
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

            }

            boolean connecting = false;

            @Override
            public void onConnectionAvailable(final WifiP2pInfo info) {
                if (connecting || client.isConnected()) {
                    return;
                }
                connecting = true;
                dismissCurrentDialog();
                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        dominos.clear();
                        stopPeerDiscovery();
                        client.connect(info.groupOwnerAddress, MPConstants.PORT);
                        new MPPlayerUser(client, DominosActivity.this, dominos);
                    }

                    @Override
                    protected void onDone() {
                        connecting = false;
                        synchronized (helper) {
                            helper.notify();
                        }
                    }

                    @Override
                    protected void onError(Exception e) {
                        connecting = false;
                        newDialogBuilder().setTitle(R.string.popup_title_error)
                                .setMessage(getString(R.string.popup_msg_failed_to_connect_reason, e.getMessage()))
                                .setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask() {
                                            @Override
                                            protected void doIt() throws Exception {
                                                killGame();
                                            }

                                            @Override
                                            protected void onDone() {
                                                showNewGameDialog();
                                            }
                                        }.run();
                                    }
                                }).show();
                    }
                }.run();

            }

            @Override
            public void onGroupInfo(WifiP2pGroup group) {
                client.setPassphrase(group.getPassphrase());
            }
        };

        final BonjourThread bonjour = new BonjourThread("dom");
        //bonjour.attach(this);
        bonjour.addListener(new BonjourThread.BonjourListener() {
            @Override
            public synchronized void onRecords(Map<String, BonjourThread.BonjourRecord> records) {
                dnsDevices.clear();
                dnsDevices.addAll(records.values());
                synchronized (devices) {
                    devices.clear();
                    devices.addAll(p2pDevices);
                    devices.addAll(dnsDevices);
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });
            }

            @Override
            public void onWifiStateChanged(String ssid) {

            }
        });

        lvHost.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final WifiP2pDevice d = (WifiP2pDevice)view.getTag();
                new SpinnerTask() {

                    @Override
                    protected Dialog showSpinner() {
                        return newDialogBuilder().setTitle(R.string.popup_title_connecting)
                                .setMessage(R.string.popup_msg_please_wait_forconnect_accept)
                                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        new SpinnerTask() {
                                            @Override
                                            protected void doIt() throws Exception {
                                                helper.cancelConnect();
                                                cancel(false);
                                                killGame();
                                                synchronized (helper) {
                                                    helper.notify();
                                                }
                                            }

                                            @Override
                                            protected void onDone() {
                                                showNewGameDialog();
                                            }
                                        }.run();
                                    }
                                }).show();
                    }

                    @Override
                    protected void doIt() {
                        if (d.status == WifiP2pDevice.CONNECTED)
                            return;
                        helper.connect(d);
                        Utils.waitNoThrow(helper, 60*1000);
                    }

                    @Override
                    protected void onDone() {
                        if (client.isConnected()) {
                            //showWaitingForPlayersDialogClient(canceleble);
                            Toast.makeText(DominosActivity.this, R.string.toast_connect_success, Toast.LENGTH_LONG).show();
                        } else if (!isCancelled()) {
                            newDialogBuilder().setTitle(R.string.popup_title_error)
                                    .setMessage(R.string.popup_msg_failed_connect_host)
                                    .setNegativeButton(R.string.popup_button_ok, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            showNewGameDialog();
                                        }
                                    }).setCancelable(false).show();
                        }
                    }
                }.run();
            }
        });

        new SpinnerTask() {
            @Override
            protected void doIt() {
                helper.p2pInitialize();
                helper.discoverPeers();
            }

            @Override
            protected void onDone() {
                newDialogBuilder().setTitle(R.string.popup_title_hosts)
                        .setView(lvHost)
                        .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                helper.destroy();
                                bonjour.detatch();
                                showNewGameDialog();
                            }
                        }).setCancelable(false).show();

            }
        }.run();

    }

    void showWaitingForPlayersDialogClient(final boolean cancelable) {
        final AlertDialog d = newDialogBuilder().setTitle(R.string.popup_title_waiting)
                .setMessage(R.string.popup_msg_waiting_for_players)
                .setNegativeButton(R.string.popup_button_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        client.disconnect();
                        showNewGameDialog();
                    }
                }).setCancelable(false).show();
        client.addListener(new GameClient.Listener() {
            @Override
            public void onCommand(GameCommand cmd) {
                client.removeListener(this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.dismiss();
                    }
                });
            }

            @Override
            public void onMessage(final String msg) {
                client.removeListener(this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        d.setMessage(msg);
                    }
                });
            }

            @Override
            public void onDisconnected(String reason) {

            }

            @Override
            public void onConnected() {

            }
        });
    }

    final static String NAME= Build.MODEL;

    void showNewMultiplayerPlayerSetupDialog(final boolean firstGame) {
        final View v = View.inflate(this, R.layout.game_setup_dialog, null);
        final RadioGroup rgNumPlayers = (RadioGroup)v.findViewById(R.id.rgNumPlayers);
        final RadioGroup rgDifficulty = (RadioGroup)v.findViewById(R.id.rgDifficulty);
        final RadioGroup rgTiles      = (RadioGroup)v.findViewById(R.id.rgTiles);
        final RadioGroup rgMaxPoints  = (RadioGroup)v.findViewById(R.id.rgMaxPoints);
        rgDifficulty.setVisibility(View.GONE);
        if (!firstGame)
            rgNumPlayers.setVisibility(View.GONE);
        switch (dominos.getNumPlayers()) {
            case 2:
                rgNumPlayers.check(R.id.rbPlayersTwo); break;
            case 3:
                rgNumPlayers.check(R.id.rbPlayersThree); break;
            case 4:
                rgNumPlayers.check(R.id.rbPlayersFour); break;
        }
        switch (dominos.getDifficulty()) {
            case 0:
                rgDifficulty.check(R.id.rbDifficultyEasy); break;
            case 1:
                rgDifficulty.check(R.id.rbDifficultyMedium); break;
            case 2:
                rgDifficulty.check(R.id.rbDifficultyHard); break;
        }
        switch (dominos.getMaxPips()) {
            case 6:
                rgTiles.check(R.id.rbTiles6x6); break;
            case 9:
                rgTiles.check(R.id.rbTiles9x9); break;
            case 12:
                rgTiles.check(R.id.rbTiles12x12); break;
        }
        switch (dominos.getMaxScore()) {
            case 150:
                rgMaxPoints.check(R.id.rbMaxPoints150); break;
            case 200:
                rgMaxPoints.check(R.id.rbMaxPoints200); break;
            case 250:
                rgMaxPoints.check(R.id.rbMaxPoints250); break;
        }
        newDialogBuilder().setTitle(R.string.popup_title_new_mp_game)
                .setView(v)
                .setPositiveButton(R.string.popup_button_start, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        int difficulty = 0;
                        int numPlayers = 4;
                        int maxPoints = 150;
                        int maxPips = 9;

                        switch (rgDifficulty.getCheckedRadioButtonId()) {
                            case R.id.rbDifficultyEasy:
                                difficulty = 0; break;
                            case R.id.rbDifficultyMedium:
                                difficulty = 1; break;
                            case R.id.rbDifficultyHard:
                                difficulty = 2; break;
                        }

                        switch (rgNumPlayers.getCheckedRadioButtonId()) {
                            case R.id.rbPlayersTwo:
                                numPlayers = 2; break;
                            case R.id.rbPlayersThree:
                                numPlayers = 3; break;
                            case R.id.rbPlayersFour:
                                numPlayers = 4; break;
                        }

                        switch (rgMaxPoints.getCheckedRadioButtonId()) {
                            case R.id.rbMaxPoints150:
                                maxPoints = 150; break;
                            case R.id.rbMaxPoints200:
                                maxPoints = 200; break;
                            case R.id.rbMaxPoints250:
                                maxPoints = 250; break;
                        }

                        switch (rgTiles.getCheckedRadioButtonId()) {
                            case R.id.rbTiles6x6:
                                maxPips = 6; break;
                            case R.id.rbTiles9x9:
                                maxPips = 9; break;
                            case R.id.rbTiles12x12:
                                maxPips = 12; break;
                        }

                        if (firstGame) {
                            dominos.initGame(maxPips, maxPoints, 0);
                            // now populate the game with remote players so that when they connect we can send them
                            // the game state right away.
                            Player [] players = new Player[numPlayers];
                            players[0] = new PlayerUser(0);
                            players[0].setName(server.getName());
                            for (int i=1; i<players.length; i++) {
                                players[i] = new Player(i);
                            }
                            dominos.setPlayers(players);
                            dominos.startNewGame();
                            showHostMultiplayerDialog();
                        } else {
                            dominos.initGame(maxPips, maxPoints, 0);
                            dominos.startNewGame();
                            server.broadcastMessage(getString(R.string.server_broadcast_starting_new_game));
                            dominos.startGameThread();
                        }
                    }


                }).setNegativeButton(R.string.popup_button_quit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                new SpinnerTask() {
                    @Override
                    protected void doIt() throws Exception {
                        killGame();
                    }

                    @Override
                    protected void onDone() {
                        showNewGameDialog();
                    }
                }.run();
            }
        }).show();
    }

    void startNewGame() {
        dominos.startNewGame();
        server.broadcastMessage(getString(R.string.server_broadcast_starting_new_game));
        dominos.startGameThread();
    }*/
	fun showNewSinglePlayerSetupDialog() {
		val v = View.inflate(this, R.layout.game_setup_dialog, null)
		val rgNumPlayers = v.findViewById<View>(R.id.rgNumPlayers) as RadioGroup
		val rgDifficulty = v.findViewById<View>(R.id.rgDifficulty) as RadioGroup
		val rgTiles = v.findViewById<View>(R.id.rgTiles) as RadioGroup
		val rgMaxPoints = v.findViewById<View>(R.id.rgMaxPoints) as RadioGroup
		val numPlayer = prefs.getInt("numPlayers", 0)
		val difficulty = prefs.getInt("difficulty", 0)
		val maxPips = prefs.getInt("maxPips", 0)
		val maxScore = prefs.getInt("maxScore", 0)
		when (numPlayer) {
			2 -> rgNumPlayers.check(R.id.rbPlayersTwo)
			3 -> rgNumPlayers.check(R.id.rbPlayersThree)
			4 -> rgNumPlayers.check(R.id.rbPlayersFour)
		}
		when (difficulty) {
			0 -> rgDifficulty.check(R.id.rbDifficultyEasy)
			1 -> rgDifficulty.check(R.id.rbDifficultyMedium)
			2 -> rgDifficulty.check(R.id.rbDifficultyHard)
		}
		when (maxPips) {
			6 -> rgTiles.check(R.id.rbTiles6x6)
			9 -> rgTiles.check(R.id.rbTiles9x9)
			12 -> rgTiles.check(R.id.rbTiles12x12)
		}
		when (maxScore) {
			150 -> rgMaxPoints.check(R.id.rbMaxPoints150)
			200 -> rgMaxPoints.check(R.id.rbMaxPoints200)
			250 -> rgMaxPoints.check(R.id.rbMaxPoints250)
		}
		newDialogBuilder().setTitle(R.string.popup_title_new_sp_game)
			.setView(v)
			.setPositiveButton(R.string.popup_button_start) { dialog, which ->
				var difficulty = 0
				var numPlayers = 4
				var maxPoints = 150
				var maxPips = 9
				when (rgDifficulty.checkedRadioButtonId) {
					R.id.rbDifficultyEasy -> difficulty = 0
					R.id.rbDifficultyMedium -> difficulty = 1
					R.id.rbDifficultyHard -> difficulty = 2
				}
				when (rgNumPlayers.checkedRadioButtonId) {
					R.id.rbPlayersTwo -> numPlayers = 2
					R.id.rbPlayersThree -> numPlayers = 3
					R.id.rbPlayersFour -> numPlayers = 4
				}
				when (rgMaxPoints.checkedRadioButtonId) {
					R.id.rbMaxPoints150 -> maxPoints = 150
					R.id.rbMaxPoints200 -> maxPoints = 200
					R.id.rbMaxPoints250 -> maxPoints = 250
				}
				when (rgTiles.checkedRadioButtonId) {
					R.id.rbTiles6x6 -> maxPips = 6
					R.id.rbTiles9x9 -> maxPips = 9
					R.id.rbTiles12x12 -> maxPips = 12
				}
				prefs.edit()
					.putInt("numPlayers", numPlayers)
					.putInt("difficulty", difficulty)
					.putInt("maxPips", maxPips)
					.putInt("maxScore", maxScore)
					.apply()
				dominos.stopGameThread()
				dominos.initGame(maxPips, maxPoints, difficulty)
				dominos.setNumPlayers(numPlayers)
				dominos.startNewGame()
				dominos.startGameThread()
			}.setOnCancelListener { showNewGameDialog() }.show()
	}

	companion object {
		private val TAG = DominosActivity::class.java.simpleName
	}
}