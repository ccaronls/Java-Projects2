package cc.lib.checkerboard;

import junit.framework.TestCase;

import org.junit.Assert;

import cc.lib.game.Utils;

public class PieceTest extends TestCase {

    public void test1() {
        Piece p = new Piece();

        p.setPlayerNum(1);
        Assert.assertEquals(1 ,p.getPlayerNum());

        p.setPlayerNum(0);
        Assert.assertEquals(0 ,p.getPlayerNum());

        p.addStackBottom(1);
        Assert.assertEquals(0 ,p.getPlayerNum());

        Assert.assertEquals(0, p.removeStackTop());
        Assert.assertEquals(1 ,p.getPlayerNum());

        p.setPlayerNum(-1);
        Assert.assertEquals(-1 ,p.getPlayerNum());

        int [] nums = new int[32];
        int num=0;
        for (int i=0; i<nums.length; i++) {
            int n = Utils.flipCoin() ? 1 : 0;
            nums[num++] = n;
            p.addStackBottom(n);
        }

        for (int i=0; i<nums.length; i++) {
            Assert.assertEquals(nums[i], p.removeStackTop());
        }

        Assert.assertEquals(-1, p.getPlayerNum());

        for (int i=0; i<nums.length; i++) {
            p.addStackTop(nums[i]);
        }

        for (int i=0; i<nums.length; i++) {
            Assert.assertEquals(nums[i], p.removeStackBottom());
        }

        Assert.assertEquals(-1, p.getPlayerNum());

    }


}
