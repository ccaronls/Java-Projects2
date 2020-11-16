package cc.lib.zombicide;

import cc.lib.game.AGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GRectangle;
import cc.lib.game.Justify;
import cc.lib.utils.Grid;

public class ZVaultDoor extends ZDoor {

    static {
        addAllFields(ZVaultDoor.class);
    }

    final Grid.Pos cellPosStart, cellPosExit;
    boolean opened = false;
    final ZDir moveDirection;

    public ZVaultDoor() {
        this(null, null, null);
    }

    public ZVaultDoor(Grid.Pos cellPosStart, Grid.Pos cellPosExit, ZDir moveDirection) {
        this.cellPosStart = cellPosStart;
        this.cellPosExit = cellPosExit;
        this.moveDirection = moveDirection;
    }

    @Override
    public ZVaultDoor getOtherSide(ZBoard board) {
        for (ZDoor other : board.zones.get(board.grid.get(cellPosExit).zoneIndex).doors) {
            if (other instanceof ZVaultDoor && other.getCellPos().equals(cellPosExit))
                return (ZVaultDoor)other;
        }
        assert(false);
        return null;
    }

    @Override
    public String name() {
        return "Vault Door";
    }

    @Override
    public Grid.Pos getCellPos() {
        return cellPosStart;
    }

    @Override
    public Grid.Pos getCellPosEnd() {
        return cellPosExit;
    }

    @Override
    public ZDir getMoveDirection() {
        return moveDirection;
    }

    @Override
    public GRectangle getRect(ZBoard board) {
        return board.getCell(cellPosStart).rect.scaledBy(.5f,.5f);
    }

    @Override
    public boolean isClosed(ZBoard board) {
        return !opened;
    }

    @Override
    public void toggle(ZBoard board) {
        ZVaultDoor other = getOtherSide(board);
        assert(other.opened == opened);
        other.opened = opened = !opened;
    }

    @Override
    public void draw(AGraphics g, ZBoard b) {
        final int outline = 2;
        GRectangle vaultRect = getRect(b);
        if (opened) {
            g.setColor(GColor.BLACK);
            vaultRect.drawFilled(g);
            // draw the 'lid' opened
            g.begin();
            g.vertex(vaultRect.getTopRight());
            g.vertex(vaultRect.getTopLeft());
            float dh = vaultRect.h/3;
            float dw = vaultRect.w/5;
            g.moveTo(-dw, -dh);
            g.moveTo(vaultRect.w+dw*2, 0);
            g.setColor(GColor.BROWN);
            g.drawTriangleFan();
            g.setColor(GColor.YELLOW);
            g.end();
            vaultRect.drawOutlined(g, outline);
            g.drawLineLoop(outline);
        } else {
            g.setColor(GColor.BROWN);
            vaultRect.drawFilled(g);
            g.setColor(GColor.YELLOW);
            vaultRect.drawOutlined(g, outline);
            g.drawJustifiedString(vaultRect.getCenter(), Justify.CENTER, Justify.CENTER, "VAULT");
        }
    }

    @Override
    public boolean isJammed() {
        return false;
    }

    @Override
    public boolean canBeClosed(ZCharacter c) {
        return true;
    }
}
