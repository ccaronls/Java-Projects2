package cc.lib.swing;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cc.lib.game.AGraphics;
import cc.lib.game.Utils;
import cc.lib.logger.Logger;
import cc.lib.logger.LoggerFactory;
import cc.lib.monopoly.Card;
import cc.lib.monopoly.MoveType;
import cc.lib.monopoly.Piece;
import cc.lib.monopoly.Player;
import cc.lib.monopoly.Rules;
import cc.lib.monopoly.UIMonopoly;
import cc.lib.utils.FileUtils;

import java.awt.*;
import javax.swing.*;

public class AWTMonopoly extends AWTComponent {
    private final static Logger log = LoggerFactory.getLogger(AWTMonopoly.class);

    public static void main(String[] args) {
        AGraphics.DEBUG_ENABLED = true;
        Utils.DEBUG_ENABLED = true;
        new AWTMonopoly();
    }

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
            return ids[Images.board.ordinal()];
        }

        @Override
        protected MoveType showChooseMoveMenu(final Player player, final List<MoveType> moves) {
            final MoveType [] result = new MoveType[1];
            frame.showListChooserDialog(new AWTFrame.OnListItemChoosen() {
                @Override
                public void itemChoose(int index) {
                    result[0] = moves.get(index);
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
            }, "Choose Move " + player.getPiece(), Utils.toStringArray(moves.toArray(new MoveType[moves.size()])));
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }

        @Override
        protected Card showChooseCardMenu(final Player player, final List<Card> cards, final Player.CardChoiceType type) {
            final Card [] result = new Card[1];
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
            }, player.getPiece() +  " " + Utils.getPrettyString(type.name()), Utils.toStringArray(cards));
            Utils.waitNoThrow(monopoly, -1);
            return result[0];
        }
    };

    final File saveFile;

    AWTMonopoly() {
        setMouseEnabled(true);
        setPadding(5);
        frame = new AWTFrame("Monopoly") {
            @Override
            protected void onWindowClosing() {
                if (monopoly.isGameRunning())
                    monopoly.trySaveToFile(saveFile);
            }

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
                                                monopoly.startGameThread();
                                            }

                                            @Override
                                            public void cancelled() {}
                                        }, "Choose Piece", Utils.toStringArray(Piece.values()));

                                    }
                                    @Override
                                    public void cancelled() {}
                                }, "How Many Players?", "2", "3", "4");
                                break;
                            case "Resume":
                                if (monopoly.tryLoadFromFile(saveFile)) {
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
                                /*final AWTButton jailBump = new AWTButton("Jail Bump", rules.jailBumpEnabled) {
                                    @Override
                                    protected void onAction() {
                                        toggleSelected();
                                    }
                                };
                                */
                                final AWTToggleButton jailBump = new AWTToggleButton("Jail Bump", rules.jailBumpEnabled);
                                content.addTop(new AWTLabel("RULES", 1, 20, true));
                                AWTPanel buttons = new AWTPanel(0, 1);
                                AWTPanel pickers = new AWTPanel(1, 0);
                                pickers.add(npStart);
                                pickers.add(npWin);
                                pickers.add(npTaxScale);
                                content.add(pickers);
                                content.addRight(buttons);
                                buttons.add(jailBump);
                                buttons.add(new AWTButton("Apply") {
                                    @Override
                                    protected void onAction() {
                                        rules.valueToWin = npWin.getValue();
                                        rules.taxScale = 0.01f * npTaxScale.getValue();
                                        rules.startMoney = npStart.getValue();
                                        rules.jailBumpEnabled = jailBump.isSelected();
                                        popup.closePopup(frame);
                                    }
                                });
                                content.addBottom(new AWTButton("Cancel") {
                                    @Override
                                    protected void onAction() {
                                        popup.closePopup(frame);
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
         saveFile = new File(settings, "monopoly.save");
        if (!frame.loadFromFile(new File(settings, "monopoly.properties")))
            frame.centerToScreen(800, 600);

        if (monopoly.tryLoadFromFile(saveFile))
            monopoly.startGameThread();

    }


    int numImagesLoaded = 0;

    enum Images {
        board, car, dog, iron, ship, shoe, thimble, tophat, wheelbarrow;
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