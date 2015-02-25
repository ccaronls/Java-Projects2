package cc.game.soc.swing;

import cc.lib.game.Utils;

public class InGameController implements IController {

    protected DiceComponent  diceComponent;
    
    private class SpinDiceThread implements Runnable {
        public void run() {
            menu.removeAll();
            int delay = 10;
            long startTime = System.currentTimeMillis();
            while (true) {
                long curTime = System.currentTimeMillis();
                if (curTime - startTime > diceSpinTimeSeconds*1000)
                    break;
                int die1 = Utils.rand() % 6 + 1;
                int die2 = Utils.rand() % 6 + 1;
                diceComponent.setDice(die1, die2);
                try {
                    Thread.sleep(delay);
                } catch (Exception e) {}
                delay += 20;
            }
            synchronized (waitObj) {
                waitObj.notify();
            }
        }
    }
}
