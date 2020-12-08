package cc.lib.swing;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.PlayerUser;
import cc.lib.monopoly.Rules;
import cc.lib.monopoly.Square;
import cc.lib.monopoly.Trade;
import cc.lib.monopoly.UIMonopoly;
import cc.lib.utils.FileUtils;
import cc.lib.utils.Reflector;

public class AWTMonopoly extends AWTComponent {
    private final static Logger log = LoggerFactory.getLogger(AWTMonopoly.class);

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.setDebugEnabled();
        new AWTMonopoly();
    }

    final File SAVE_FILE;
    final File RULES_FILE;

    final AWTFrame frame;
    final UIMonopoly monopoly = new UIMonopoly() {
        @Override
        public void repaint() {
            AWTMonopoly.this.repaint();
        }

        @Override
        public int getImageId(Piece p) {
            return pieceMap.get(p);
        }

        @Override
        public int getBoardImageId() {
            return ids[Images.monopoly_board.ordinal()];
        }

        @Override
        protected MoveType showChooseMoveMenu(final Player player, final List<MoveType> moves) {
            final MoveType [] result = new MoveType[1];
            final String [] strings = new String[moves.size()];
            int index = 0;
            for (MoveType mt : moves) {
                switch (mt) {
                    case PURCHASE_UNBOUGHT: {
                        Square sq = getPurchasePropertySquare();
                        strings[index++] = "Purchase " + Utils.toPrettyString(sq.name()) + " $" + sq.getPrice();
                        break;
                    }
                    case PAY_BOND: {
                        strings[index++] = String.format("%s $%d", Utils.toPrettyString(mt.name()), player.getJailBond());
                        break;
                    }
                    default:
                        strings[index++] = Utils.toPrettyString(mt.name());
                        break;
                }
            }
            monopoly.trySaveToFile(SAVE_FILE);
            frame.showListChooserDialog(new AWTFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = moves.get(index);
                    switch (result[0]) {
                        case PURCHASE_UNBOUGHT:{
                            showPropertyPopup("Purchase?", "Buy",
                                    getPurchasePropertySquare(), () -> {
                                        synchronized (monopoly) {
                                            monopoly.notify();
                                        }
                                    }, () -> {
                                        result[0] = null;
                                        synchronized (monopoly) {
                                            monopoly.notify();
                                        }

                                    }
                            );
                            break;
                        }
                        case PURCHASE: {
                            showPropertyPopup("Purchase?", "Buy", 
                                    player.getSquare(), () -> {
                                        synchronized (monopoly) {
                                            monopoly.notify();
                                        }
                                    }, () -> {
                                        result[0] = null;
                                        synchronized (monopoly) {
                                            monopoly.notify();
                                        }

                                    }
                            );
                            break;
                        }
                        default:
                            synchronized (monopoly) {
                                monopoly.notify();
                            }
                    }
                }

                @Override
                public void cancelled() {
                    if (monopoly.canCancel())
                        monopoly.cancel();
                    else
                        monopoly.stopGameThread();
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
                }
            }, "Choose Move " + player.getPiece(), strings);
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected Card showChooseCardMenu(final Player player, final List<Card> cards, final Player.CardChoiceType type) {
            final Card [] result = new Card[1];
            final String [] items = new String[cards.size()];
            int index = 0;
            for (Card c : cards) {
                switch (type) {

                    case CHOOSE_CARD_TO_MORTGAGE:
                        items[index++] = Utils.toPrettyString(c.getProperty().name()) + " Mortgage $" + c.getProperty().getMortgageValue(c.getHouses());
                        break;
                    case CHOOSE_CARD_TO_UNMORTGAGE:
                        items[index++] = Utils.toPrettyString(c.getProperty().name()) + " Buyback $" + c.getProperty().getMortgageBuybackPrice();
                        break;
                    case CHOOSE_CARD_FOR_NEW_UNIT:
                        items[index++] = Utils.toPrettyString(c.getProperty().name()) + " Unit $" + c.getProperty().getUnitPrice();
                        break;
                }
            }
            frame.showListChooserDialog(new AWTFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = cards.get(index);
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
                }

                @Override
                public void cancelled() {
                    if (monopoly.canCancel())
                        monopoly.cancel();
                    else
                        monopoly.stopGameThread();
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
                }
            }, player.getPiece() +  " " + Utils.toPrettyString(type.name()), items);
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected Trade showChooseTradeMenu(Player player, List<Trade> trades) {
            final Trade [] result = new Trade[1];
            frame.showListChooserDialog(new AWTFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    final Trade t = trades.get(index);
                    final Player trader = t.getTrader();
                    String title = "Buy " + Utils.toPrettyString(t.getCard().getProperty().name()) + " from " + Utils.toPrettyString(trader.getPiece().name());
                    showPropertyPopup(title, "Buy for $" +t.getPrice(), t.getCard().getProperty(), () -> {
                        result[0] = t;
                        synchronized (monopoly) {
                            monopoly.notify();
                        }
                    }, ()->{
                        synchronized (monopoly) {
                            monopoly.notify();
                        }
                    });
                }

                @Override
                public void cancelled() {
                    if (monopoly.canCancel())
                        monopoly.cancel();
                    else
                        monopoly.stopGameThread();
                    synchronized (monopoly) {
                        monopoly.notify();
                    }
                }
            }, "Choose Trade" + player.getPiece(), Utils.toStringArray(trades, true));
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected boolean showMarkSellableMenu(PlayerUser user, List<Card> sellable) {
            final AWTFrame popup = new AWTFrame();
            final ActionListener listener = (ActionEvent e) -> {
                synchronized (monopoly) {
                    monopoly.notify();
                }
                popup.closePopup();
            };
            AWTPanel content = new AWTPanel(new BorderLayout());
            AWTPanel list = new AWTPanel(0, 2);
            for (final Card card : sellable) {
                int price = user.getSellableCardCost(card);
                list.add(new JLabel(card.getProperty().name()));
                final JSpinner spinner = new JSpinner(new SpinnerNumberModel(price, 0, 2000, 50));
                spinner.addChangeListener((ChangeEvent e) -> {
                    user.setSellableCard(card, (Integer)spinner.getValue());
                });
                list.add(spinner);
            }
            content.add(new AWTLabel("Mark Sellable " + user.getPiece().name(), 1, 20, true), BorderLayout.NORTH);
            content.add(list, BorderLayout.CENTER);
            content.add(new AWTButton("DONE", listener), BorderLayout.SOUTH);
            popup.setContentPane(content);
            popup.showAsPopup(frame);
            Utils.waitNoThrow(monopoly, -1);
            return true;
        }
    };

    public AWTFrame showPropertyPopup(String title, String yesLabel, final Square sq, final Runnable onBuyRunnable, final Runnable onDontBuyRunnble) {
        final AWTFrame popup = new AWTFrame();
        AWTPanel content = new AWTPanel(new BorderLayout());
        content.add(new AWTLabel(title, 1, 20, true), BorderLayout.NORTH);
        AWTComponent sqPanel = new AWTComponent() {
            @Override
            protected void paint(AWTGraphics g, int mouseX, int mouseY) {
                monopoly.drawPropertyCard(g, sq, g.getViewportWidth(), g.getViewportHeight());
            }
        };
        sqPanel.setPreferredSize(monopoly.DIM/3, monopoly.DIM/4*2);
        content.add(sqPanel);
        content.add(new AWTPanel(1, 2, new AWTButton(yesLabel) {
            @Override
            protected void onAction() {
                popup.closePopup();
                onBuyRunnable.run();
            }
        }, new AWTButton("Dont Buy") {
            @Override
            protected void onAction() {
                popup.closePopup();
                onDontBuyRunnble.run();
            }
        }), BorderLayout.SOUTH);
        popup.setContentPane(content);
        popup.showAsPopup(frame);
        return popup;
    }

    AWTMonopoly() {
        setMouseEnabled(true);
        setPadding(5);
        frame = new AWTFrame("Monopoly") {
            /*
            @Override
            protected void onWindowClosing() {
                if (monopoly.isGameRunning())
                    monopoly.trySaveToFile(SAVE_FILE);
            }*/

            @Override
            protected void onMenuItemSelected(String menu, String subMenu) {
                switch (menu) {
                    case "File":
                        switch (subMenu) {
                            case "New Game":
                                frame.showListChooserDialog(new OnListItemChoosen() {
                                    @Override
                                    public void itemChoose(final int item) {
                                        frame.showListChooserDialog(new OnListItemChoosen() {
                                            @Override
                                            public void itemChoose(int pc) {
                                                monopoly.stopGameThread();
                                                monopoly.initPlayers(item + 2, Piece.values()[pc]);
                                                monopoly.newGame();
                                                monopoly.startGameThread();
                                            }

                                            @Override
                                            public void cancelled() {}
                                        }, "Choose Piece", Utils.toStringArray(Piece.values(), true));

                                    }
                                    @Override
                                    public void cancelled() {}
                                }, "How Many Players?", "2", "3", "4");
                                break;
                            case "Resume":
                                if (monopoly.tryLoadFromFile(SAVE_FILE)) {
                                    monopoly.startGameThread();
                                }
                                break;
                            case "Rules": {
                                final Rules rules = monopoly.getRules();
                                final AWTFrame popup = new AWTFrame();
                                AWTPanel content= new AWTPanel(new BorderLayout());
                                final AWTNumberPicker npStart = new AWTNumberPicker.Builder().setLabel("Start $").setMin(500).setMax(1500).setStep(100).setValue(rules.startMoney).build(null);
                                final AWTNumberPicker npWin   = new AWTNumberPicker.Builder().setLabel("Win $").setMin(2000).setMax(5000).setStep(500).setValue(rules.valueToWin).build(null);
                                final AWTNumberPicker npTaxScale = new AWTNumberPicker.Builder().setLabel("Tax Scale %").setMin(0).setMax(200).setStep(50).setValue(Math.round(100f * rules.taxScale)).build(null);
                                final AWTToggleButton jailBump = new AWTToggleButton("Jail Bump", rules.jailBumpEnabled);
                                final AWTToggleButton jailMulti = new AWTToggleButton("Jail Multiplier", rules.jailMultiplier);
                                content.addTop(new AWTLabel("RULES", 1, 20, true));
                                AWTPanel buttons = new AWTPanel(0, 1);
                                AWTPanel pickers = new AWTPanel(1, 0);
                                pickers.add(npStart);
                                pickers.add(npWin);
                                pickers.add(npTaxScale);
                                content.add(pickers);
                                content.addRight(buttons);
                                buttons.add(jailBump);
                                buttons.add(jailMulti);
                                buttons.add(new AWTButton("Apply") {
                                    @Override
                                    protected void onAction() {
                                        rules.valueToWin = npWin.getValue();
                                        rules.taxScale = 0.01f * npTaxScale.getValue();
                                        rules.startMoney = npStart.getValue();
                                        rules.jailBumpEnabled = jailBump.isSelected();
                                        rules.jailMultiplier = jailMulti.isSelected();
                                        popup.closePopup();
                                        try {
                                            Reflector.serializeToFile(rules, RULES_FILE);
                                        } catch (Exception e) {
                                            frame.showMessageDialog("ERROR", "Error save rules to file " + RULES_FILE + "\n" + e.getClass().getSimpleName() + " " + e.getMessage());
                                        }
                                    }
                                });
                                content.addBottom(new AWTButton("Cancel") {
                                    @Override
                                    protected void onAction() {
                                        popup.closePopup();
                                    }
                                });
                                popup.setContentPane(content);
                                popup.showAsPopup(frame);
                                break;
                            }
                        }
                }
            }
        };

        frame.addMenuBarMenu("File", "New Game", "Resume", "Rules");
        frame.add(this);

        File settings = FileUtils.getOrCreateSettingsDirectory(getClass());
        SAVE_FILE = new File(settings, "game.save");
        RULES_FILE = new File(settings, "rules.save");
        if (!frame.loadFromFile(new File(settings, "monopoly.properties")))
            frame.centerToScreen(800, 600);
        if (RULES_FILE.isFile()) {
            try {
                Rules rules = Reflector.deserializeFromFile(RULES_FILE);
                monopoly.getRules().copyFrom(rules);
            } catch (Exception e) {
                frame.showMessageDialog("ERROR", "Failed to load rules from " + RULES_FILE + "\n " + e.getClass().getSimpleName() + " " + e.getMessage());
            }
        }
    }


    int numImagesLoaded = 0;

    enum Images {
        monopoly_board, car, dog, iron, ship, shoe, thimble, tophat, wheelbarrow;
    }

    int [] ids = new int[Images.values().length];
    Map<Piece, Integer> pieceMap = new HashMap<>();

    @Override
    protected void init(AWTGraphics g) {
        for (int i=0; i<ids.length; i++) {
            ids[i] = g.loadImage("images/" + Images.values()[i].name() + ".png");
            numImagesLoaded++;
        }
        pieceMap.put(Piece.BOAT, Images.ship.ordinal());
        pieceMap.put(Piece.CAR, Images.car.ordinal());
        pieceMap.put(Piece.DOG, Images.dog.ordinal());
        pieceMap.put(Piece.HAT, Images.tophat.ordinal());
        pieceMap.put(Piece.IRON, Images.iron.ordinal());
        pieceMap.put(Piece.SHOE, Images.shoe.ordinal());
        pieceMap.put(Piece.THIMBLE, Images.thimble.ordinal());
        pieceMap.put(Piece.WHEELBARROW, Images.wheelbarrow.ordinal());
    }

    @Override
    protected float getInitProgress() {
        return (float)numImagesLoaded / ids.length;
    }


    @Override
    protected void paint(AWTGraphics g, int mouseX, int mouseY) {
        monopoly.paint(g, mouseX, mouseY);
    }


}