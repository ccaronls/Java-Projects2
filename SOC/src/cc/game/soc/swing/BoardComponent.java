package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.*;

import javax.swing.JComponent;

import cc.game.soc.core.*;
import cc.lib.game.*;
import cc.lib.math.*;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.ImageMgr;
import cc.lib.swing.AWTRenderer;

@SuppressWarnings("serial")
public class BoardComponent extends JComponent implements KeyListener, MouseMotionListener, MouseListener, MouseWheelListener, Renderable {

    public enum PickMode {

        PM_NONE,
        PM_ROAD,
        PM_SHIP,
        PM_MOVABLE_SHIPS,
        PM_SETTLEMENT,
        PM_CITY,
        PM_WALLED_CITY,
        PM_ROBBER,
        PM_MERCHANT,
        PM_CELLPAINT,
        PM_CELL,
        PM_ISLAND,
        PM_EDGE,
        PM_VERTEX,
        PM_PATH,
        PM_ROUTE,
        PM_ROUTE2,
        PM_KNIGHT,
        PM_PROMOTE_KNIGHT,
        PM_METROPOLIS_TRADE,
        PM_METROPOLIS_POLITICS,
        PM_METROPOLIS_SCIENCE,
    }
    
    enum RenderFlag {
        DRAW_CELL_CENTERS,
        NUMBER_CELLS,
        NUMBER_VERTS,
        NUMBER_EDGES,
        DONT_DRAW_TEXTURES,
        DRAW_CELL_OUTLINES, 
        DONT_DRAW_ROADS, 
        DONT_DRAW_STRUCTURES,
        SHOW_CELL_INFO,
        SHOW_EDGE_INFO,
        SHOW_VERTEX_INFO,
        
        ;
        RenderFlag() {
            assert(1 << ordinal() > 0);
        }
    }
    
    interface BoardListener {
        void onPick(PickMode mode, int pickedValue);

		Color getPlayerColor(int playerNum);
    }

    private BoardListener listener;
    private Board board;
	
	Board getBoard() {
	    return board;
	}
	
	final ImageMgr images;
	final AWTRenderer render = new AWTRenderer(this);

	final int desertImage;
	final int woodImage;
	final int wheatImage;
	final int oreImage;
	final int brickImage;
	final int sheepImage;
	final int waterImage;
	final int goldImage;
	final int undiscoveredImage;
	final int robberImage;
	final int pirateImage;
	// CAK extension
	final int foresthexImage;
	final int hillshexImage;
	final int mountainshexImage;
	final int pastureshexImage;
	final int fieldshexImage;
	
	final int cardFrameImage;

    final int roadLineThickness;
	final Color bkColor;

	private PickMode pickMode = PickMode.PM_NONE;
    private int pickPlayerNum = 0;
	private List<Integer> pickIndices = null;
	private int pickedValue = -1;
	private TileType paintCellType = TileType.NONE;
    final int padding;
	
    private int renderFlag = 0;
    private List<Animation> animations = new ArrayList<Animation>(32);
    
    private int edgeInfoIndex = -1;
    private int cellInfoIndex = -1;
    private int vertexInfoIndex = -1;
    
    private int [] knightImages = new int[6];
    
	BoardComponent(BoardListener listener, Board board, ImageMgr images) {
		this.listener = listener;
		this.images = images;
		
		setFocusable(true);
	    renderFlag          = GUI.instance.getProps().getIntProperty("board.renderFlag", 0);
        bkColor             = GUI.instance.getProps().getColorProperty("board.bkcolor", Color.LIGHT_GRAY);
        roadLineThickness   = GUI.instance.getProps().getIntProperty("board.roadLineThickness", 4);
        renderFlag          = GUI.instance.getProps().getIntProperty("board.renderFlag", 0);
        padding             = GUI.instance.getProps().getIntProperty("board.padding", 20);
		this.board = board;
        addMouseMotionListener(this);
        addMouseListener(this);
        addMouseWheelListener(this);
        addKeyListener(this);
        
        desertImage  = images.loadImage("desert.GIF", Color.WHITE);
		woodImage    = images.loadImage("wood.GIF",   Color.WHITE);
		wheatImage   = images.loadImage("wheat.GIF",  Color.WHITE);
		oreImage     = images.loadImage("ore.GIF",    Color.WHITE);
		brickImage   = images.loadImage("brick.GIF",  Color.WHITE);
		sheepImage   = images.loadImage("sheep.GIF",  Color.WHITE);
		waterImage   = images.loadImage("water.GIF",  Color.WHITE);
		robberImage  = images.loadImage("robber.GIF", Color.WHITE);
		pirateImage	 = images.loadImage("pirate.GIF");
		goldImage    = images.loadImage("gold.GIF");
		
		mountainshexImage 	= images.loadImage("mountainshex.GIF");
		hillshexImage 		= images.loadImage("hillshex.GIF");
		pastureshexImage 	= images.loadImage("pastureshex.GIF");
		fieldshexImage 		= images.loadImage("fieldshex.GIF");
		foresthexImage 		= images.loadImage("foresthex.GIF");
		
		undiscoveredImage = images.loadImage("undiscoveredtile.GIF");
		
		cardFrameImage = images.loadImage("cardFrame.GIF", Color.WHITE);
		knightImages[0] = images.loadImage("knight_basic_inactive.GIF");
		knightImages[1] = images.loadImage("knight_basic_active.GIF");
		knightImages[2] = images.loadImage("knight_strong_inactive.GIF");
		knightImages[3] = images.loadImage("knight_strong_active.GIF");
		knightImages[4] = images.loadImage("knight_mighty_inactive.GIF");
		knightImages[5] = images.loadImage("knight_mighty_active.GIF");
	}	    
    void setRenderFlag(RenderFlag flag, boolean enabled) {
        if (enabled)
            renderFlag |= (1 << flag.ordinal());
        else
            renderFlag &= ~(1 << flag.ordinal());
        GUI.instance.getProps().setProperty("board.renderFlag", renderFlag);
        repaint();
    }
    
    boolean getRenderFlag(RenderFlag flag) {
        return (renderFlag & (1 << flag.ordinal())) != 0;
    }
    
    void setPaintMode(TileType type) {
        this.pickMode = PickMode.PM_CELLPAINT;
        this.paintCellType = type;
        pickIndices = null;
    }
    
    void addAnimation(Animation anim) {
        synchronized (animations) {
            animations.add(anim);
        }
        repaint();
        anim.start();
    }

	private void drawCellOutline(Graphics g, Tile cell) {
	    render.clearVerts();
		for (int i : cell.getAdjVerts()) {
			Vertex v = board.getVertex(i);
			render.addVertex(v.getX(), v.getY());
		}
		render.drawLineLoop(g, 2);
	}
	
//	private void setPlayerColor(Graphics g, int playerNum) {
//		g.setColor(listener.getPlayerColor(playerNum));
//	}
	
	private static class Face {
		final float darkenAmount;
		final int   numVerts;
		final int [] xVerts;
		final int [] yVerts;

		Face(float darkenAmount, int ... verts) {
			this.darkenAmount = darkenAmount;
			this.numVerts = verts.length/2;
			this.xVerts = new int[numVerts];
			this.yVerts = new int[numVerts];
			int index=0;
			for (int i=0; i<numVerts*2; i+=2) {
				xVerts[index] = verts[i];
				yVerts[index] = verts[i+1];
				index++;
			}
		}
	};
	
	private static final Face [] structureFaces = {
		// 0. house front
	    new Face(0.0f, 	0,0, 2,3, 4,0, 4,-4, 0,-4),
	    // 1. house roof
	    new Face(0.25f, 0,0, 2,3, 0,4, -2,1),
	    // 2. house side
	    new Face(0.45f, 0,0, -2,1, -2,-3, 0,-4),
	    // 3. city front panel
	    new Face(0.0f, 	0,0, -2,0, -2,-4, 0,-4),
	    // 4. city side panel
	    new Face(0.45f, -2,0,-4,1,-4,-3,-2,-4),
	    // 5. city roof
	    new Face(0.1f, 	0,0,-2,1,-4,1,-2,0),
	    // walled city
	    // 6. wall right
	    new Face(0.6f, 4,0, 6,-2, 4,-2),
	    // 7. wall front
	    new Face(0.1f, 6,-2, 6,-5, -3,-5, -3,-2),
	    // 8. wall left
	    new Face(0.5f, -5,0, -3,-2, -3,-5, -5,-3),
	    
	    // ship
	    
	    // 9. hull
	    new Face(0.25f, -4,0, 4,0, 3,-2, -3,-2),
	    // 10. sail 
	    new Face(0.0f, 1,0,1,5,-1,4,-3,1,-1,0),
	    
	    // shield
	    
	    // 11. top left
	    new Face(0.0f, 0,1, 0,3, -3,3, -3,1),
	    // 12. top right
	    new Face(0.2f, 0,1, 0,3, 3,3, 3,1),
	    // 13. bottom left
	    new Face(0.5f, 0,1, -3,1, -3,-1, 0,-4),
	    // 14. bottom right
	    new Face(0.7f, 0,1, 3,1, 3,-1, 0,-4),
	    
	    // 15. Level 2 knight bar
	    new Face(0.5f, -3,4, -3,5, 3,5, 3,4),
	    
	    // 16. Level 3 knight bar
	    new Face(0.1f, -3,6, -3,7, 3,7, 3,6),
	    // Merchant
	    
	    // 17. Diamond
	    new Face(0.5f, 5,0, 0,5, -5,0, 0,-5),
	    // 18. T
	    new Face(0.0f, 1,1, 4,1, 2,3, -2,3, -4,1, -1,1, -1,-4, 1,-4),
	    
	    // 19-29 Trade Metropolis
	    // right most building
	    new Face(0.2f, 2,-4, 5,-4, 5,4, 2,5),
	    new Face(0.4f, 1,5,  2,4,  2,0, 1,0),
	    new Face(0.0f,  2,4, 5,4, 4,5, 1,5),
	    // middle building
	    new Face(0.0f, 3,0, 1,2, 1,0),
	    new Face(0.2f, -2,4, 1,4, 1,0, -2,0),
	    new Face(0.2f, -2,0, 3,0, 3,-5, -2,-5),
	    new Face(0.4f, -2,4, -2,-5, -4,-3, -4,6),
	    new Face(0.0f, -2,4, -4,6, -1,6, 1,4),
	    // left most building
	    new Face(0.2f, -3,1, -5,1, -5,-4, -3,-4),
	    new Face(0.4f, -6,2, -5,1, -5,-4, -6,-3),
	    new Face(0.0f, -3,1, -4,2, -6,2, -5,1),
	    
	    // 30-37 Politics Metropolis
	    // roofs from right to left
	    new Face(0.0f, 5,0, 7,-1, 4,-1, 2,0),
	    new Face(0.1f, 1,3, -1,4, -3,3, -1,2),
	    new Face(0.2f, -1,2, -2,0, -4,1, -3,3),
	    new Face(0.3f, -2,0, -4,1, -4,0, -2,-1),
	    new Face(0.0f, -4,0, -7,0, -5,-1, -2,-1),
	    // front
	    new Face(0.5f, 1,3, 3,2, 4,0, 4,-1, 7,-1, 7,-4, -5,-4, -5,-1, -2,-1, -2,0, -1,2),
	    // left wall
	    new Face(0.6f, -7,0, -5,-1, -5,-4, -7,-3),
	    // door
	    new Face(1.0f, 0,0, 2,0, 2,-4, 0,-4),
	    
	    // 38-48 Science metropolis
	    // column sides
	    new Face(0.8f, -3,3, 2,3, 2,-2, -3,-2),
	    // base top
	    new Face(0.7f, 2,-1, 3,-2, -2,-2, -3,-1),
	    // base angled side
	    new Face(0.2f, -3,-1, -2,-2, -3,-3, -4,-2),
	    // base side
	    new Face(0.4f, -4,-2, -4,-3, -3,-4, -3,-3),
	    // base front
	    new Face(0.5f, -2,-2, 3,-2, 4,-3, 4,-4, -3,-4, -3,-3),
	    // right column front
	    new Face(0.5f, 2,2, 3,2, 3,-2, 2,-2),
	    // center column front
	    new Face(0.5f, 0,2, 1,2, 1,-2, 0,-2),
	    // left column front
	    new Face(0.5f, -2,2, -1,2, -1,-2, -2,-2),
	    // roof top
	    new Face(0.0f, -2,5, 1,5, 2,4, -1,4),
	    // roof left 
	    new Face(0.2f, -2,5, -1,4, -4,2, -5,3),
	    // roof front
	    new Face(0.5f, -4,2, -1,4, 2,4, 5,2),
	    
	    
	    
	};
	    
	private static final int settlementFaceIndex[] 		= { 0,1,2 };
	private static final int cityFaceIndex[]       		= { 0,1,3,4,5 };
	private static final int walledCityFaceIndex[] 		= { 6,0,1,3,4,5,7,8 }; 
	private static final int shipFaceIndex[]       		= { 9, 10 };
	private static final int knightFaceIndex1[]    		= { 11,12,13,14 };
	private static final int knightFaceIndex2[]    		= { 11,12,13,14,15 };
	private static final int knightFaceIndex3[]    		= { 11,12,13,14,15,16 };
	private static final int merchantFaceIndex[]   		= { 17, 18 };
	private static final int metroTradeFaceIndex[] 		= { 19,20,21,22,23,24,25,26,27,28,29 };
	private static final int metroPoliticsFaceIndex[] 	= { 30,31,32,33,34,35,36,37 };
	private static final int metroScienceFaceIndex[] 	= { 38,39,40,41,42,43,44,45,46,47,48 };

	void drawSettlement(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, settlementFaceIndex, outline);
	}
	
	void drawCity(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, cityFaceIndex, outline);
	}
	
	void drawWalledCity(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, walledCityFaceIndex, outline);
	}
	
	void drawMetropolisTrade(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, metroTradeFaceIndex, outline);
	}

	void drawMetropolisPolitics(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, metroPoliticsFaceIndex, outline);
	}

	void drawMetropolisScience(Graphics g, float x, float y, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, x, y, 0, board.getTileWidth()/6, metroScienceFaceIndex, outline);
	}

	void drawMerchant(Graphics g, Tile t, int playerNum) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		drawFaces(g, t.getX(), t.getY(), 0, board.getTileWidth()/7, merchantFaceIndex, false);
	}

	void drawKnight_image(Graphics g, float _x, float _y, int playerNum, int level, boolean active, boolean outline) {
		final int x = Math.round(_x);
		final int y = Math.round(_y);
		final int r = (int)(board.getTileWidth()/8) + 1;
		final int r2 = r+3;
		int index = level * (active ? 2 : 1) - 1;
		g.drawOval(x-r2/2, y-r2/2, r2, r2);
		images.drawImage(g, knightImages[index], x-r/2, y-r/2, r, r);
	}
	void drawKnight(Graphics g, float x, float y, int playerNum, int level, boolean active, boolean outline) {
		if (playerNum > 0)
			g.setColor(listener.getPlayerColor(playerNum));
		final float radius = board.getTileWidth()/8;
		float xRad = 3;
		float scale = radius / xRad;
		int [] faces = null;
		switch (level) {
			case 1: faces = knightFaceIndex1; break;
			case 2: faces = knightFaceIndex2; break;
			case 3: faces = knightFaceIndex3; break;
			default: assert(false); break;
		}
		drawFaces(g, x, y, 0, scale/2, scale, faces, outline);
	}

	void drawFaces(Graphics g, float x, float y, float angle, float radius, int [] faces, boolean outline) {
	    final float xRad = 3; // actual radius as defined above
		float scale = radius / xRad;
		drawFaces(g, x, y, angle, scale, scale, faces, outline);
	}

	void drawFaces(Graphics g, float x, float y, float angle, float w, float h, int [] faces, boolean outline) {
		render.pushMatrix();
		render.translate(x, y);
		render.rotate(angle);
		render.scale(w, -h);
		Color structureColor = g.getColor();

	    for (int f=0; f<faces.length; f++)
	    {
	    	Face face = structureFaces[faces[f]];	    	

		    render.clearVerts();
	    	for (int i=0; i<face.numVerts; i++)
	    		render.addVertex(face.xVerts[i], face.yVerts[i]);
	    	
	    	if (outline) {
	    	    g.setColor(Color.BLACK);
	    	    render.drawLineLoop(g, 2);
	    	}

            g.setColor(AWTUtils.darken(structureColor, face.darkenAmount));
            render.fillPolygon(g);
	    }
	    g.setColor(structureColor);
	    render.popMatrix();
	}
	
	private int pickEdge(int mouseX, int mouseY, List<Integer> indices) {
		render.clearVerts();
		if (indices == null) {
			for (int index=0; index<board.getNumRoutes(); index++) {
    			render.setName(index);
    			renderEdge(board.getRoute(index));
			}
		} else {
			for (int index: indices) {
    			render.setName(index);
    			renderEdge(board.getRoute(index));
    		}
		}
		return render.pickLines(mouseX, mouseY, this.roadLineThickness*2);
	}
	
	private int pickVertex(int mouseX, int mouseY, List<Integer> indices) {
		render.clearVerts();
		if (indices == null) {
			for (int index=0; index<board.getNumVerts(); index++) {
				render.setName(index);
    			Vertex v = board.getVertex(index);
    			render.addVertex(v.getX(), v.getY());
			}
		} else {
    		for (int index: indices) {
    			render.setName(index);
    			Vertex v = board.getVertex(index);
    			render.addVertex(v.getX(), v.getY());
    		}
		}
		return render.pickPoints(mouseX, mouseY, 10);
	}
	
	private int pickCell(int mouseX, int mouseY, List<Integer> indices) {
		render.clearVerts();
		final int dim = Math.round(board.getTileWidth() * getWidth());
		if (indices == null) {
			for (int index=0; index<board.getNumTiles(); index++) {
    			render.setName(index);
    			Tile cell = board.getTile(index);
    			render.addVertex(cell.getX(), cell.getY());
			}
		} else {
			for (int index : indices) {
    			render.setName(index);
    			Tile cell = board.getTile(index);
    			render.addVertex(cell.getX(), cell.getY());
    		}
		}
		return render.pickPoints(mouseX, mouseY, dim);
	}
	
	private int pickRoad(int mouseX, int mouseY) {
		render.clearVerts();
		if (pickIndices == null) {
			return pickEdge(mouseX, mouseY, SOC.computeRoadRouteIndices(pickPlayerNum, board));
		}	
		return pickEdge(mouseX, mouseY, pickIndices);
	}
	
	private int pickShip(int mouseX, int mouseY) {
		render.clearVerts();
		if (pickIndices == null) {
			return pickEdge(mouseX, mouseY, SOC.computeShipRouteIndices(pickPlayerNum, board));
		}
		return pickEdge(mouseX, mouseY, pickIndices);
	}
	
	private int pickMovableShip(int mouseX, int mouseY) {
		render.clearVerts();
		if (pickIndices == null) {
			return pickEdge(mouseX, mouseY, SOC.computeOpenRouteIndices(pickPlayerNum, board, false, true));
		}
		return pickEdge(mouseX, mouseY, pickIndices);
	}
	
	private int pickSettlement(int mouseX, int mouseY) {
		if (pickIndices == null)
			return pickVertex(mouseX, mouseY, SOC.computeSettlementVertexIndices(null, pickPlayerNum, board));
		return pickVertex(mouseX, mouseY, pickIndices);
	}

	private int pickCity(int mouseX, int mouseY) {
		if (pickIndices == null)
			return pickVertex(mouseX, mouseY, SOC.computeCityVertxIndices(pickPlayerNum, board));
		return pickVertex(mouseX, mouseY, pickIndices);
	}
	
	private void renderEdge(Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		render.addVertex(v0.getX(), v0.getY());
		render.addVertex(v1.getX(), v1.getY());
	}

	private void drawEdge(Graphics g, Route e, boolean outline) {
		if (e.getPlayer() > 0) {
			if (e.isShip())
				drawShip(g, board.getRouteMidpoint(e), outline);
			else
				drawRoad(g, e, outline);
		} else {
			if (e.isAdjacentToLand())
				drawRoad(g, e, outline);
			if (e.isAdjacentToWater())
				drawShip(g, board.getRouteMidpoint(e), outline);
		}
	}
	
	private void drawRoad(Graphics g, Route e, boolean outline) {
		render.clearVerts();
	    if (outline) {
	        Color old = g.getColor();
	        g.setColor(Color.BLACK);
            renderEdge(e);
            render.drawLines(g, roadLineThickness+2);
            g.setColor(old);
            return;
	    } 
	    renderEdge(e);
	    render.drawLines(g, roadLineThickness);
	}
	
	void drawShip(Graphics g, IVector2D v, boolean outline) {
		drawFaces(g, v.getX(), v.getY(), 0, board.getTileWidth()/8, shipFaceIndex, outline);
	}
	
	private void drawRobber(Graphics g, Tile cell) {
	    //float [] v = {0,0};
	    //render.transformXY(board.getCellWidth(), board.getCellHeight(), v);
		render.clearVerts();
	    float sx = board.getTileWidth() / bw * (getWidth()-padding);
	    float sy = board.getTileHeight() / bh * (getHeight()-padding);
	    float [] v = {0,0};
        int w = Math.round(sx) / 2;
        int h = Math.round(sy) / 2;
        render.transformXY(cell.getX(), cell.getY(), v);
        int x = Math.round(v[0]) - w/2;
        int y = Math.round(v[1]) - h/2;
        //int x = Math.round(sx * cell.getX()) - w/2;
        //int y = Math.round(sy * cell.getY()) - h/2;
        if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES))
            images.drawImage(g, robberImage, x, y, w,h);
        else {
            g.setColor(Color.BLACK);
            g.fillOval(x,y,w,h);
        }
	}
	
	private void drawPirate(Graphics g, Tile cell) {
	    //float [] v = {0,0};
	    //render.transformXY(board.getCellWidth(), board.getCellHeight(), v);
		render.clearVerts();
	    float sx = board.getTileWidth() / bw * (getWidth()-padding);
	    float sy = board.getTileHeight() / bh * (getHeight()-padding);
	    float [] v = {0,0};
        int w = Math.round(sx) / 2;
        int h = Math.round(sy) / 2;
        render.transformXY(cell.getX(), cell.getY(), v);
        int x = Math.round(v[0]) - w/2;
        int y = Math.round(v[1]) - h/2;
        //int x = Math.round(sx * cell.getX()) - w/2;
        //int y = Math.round(sy * cell.getY()) - h/2;
        if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES))
            images.drawImage(g, pirateImage, x, y, w,h);
        else {
            g.setColor(Color.BLACK);
            g.fillOval(x,y,w,h);
        }
	}

	/**
	 * Set the current pick mode.
	 *  
	 * @param mode
	 * @param indices set of acceptable indices or null to pick all indices
	 */
	void setPickMode(int pickPlayerNum, PickMode mode, List<Integer> indices) {
		this.pickPlayerNum = pickPlayerNum;
		if (mode == PickMode.PM_ROUTE2)
			this.pickPlayerNum = -pickPlayerNum;
		else if (mode == PickMode.PM_PATH)
			this.pickPlayerNum = 0;
		this.pickMode = mode;
		this.pickIndices = indices;
		pickedValue = -1;
		repaint();
	}

    private void drawCellsOutlined(Graphics g) {
        Color outlineColor = GUI.instance.getProps().getColorProperty("board.outlineColor", Color.WHITE);
        Color textColor = GUI.instance.getProps().getColorProperty("board.textcolor", Color.CYAN);
        
        float [] v = {0,0};
        
        for (int i=0; i <board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            render.transformXY(cell.getX(), cell.getY(), v);
            int x = Math.round(v[0]);
            int y = Math.round(v[1]);
            g.setColor(outlineColor);
            drawCellOutline(g, cell);
            g.setColor(textColor);
            switch (cell.getType()) {
                case NONE:
                    break;
                case DESERT:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Desert");
                    break;
                case WATER:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Water");
                    break;
        		case PORT_ORE:
        		case PORT_SHEEP:
        		case PORT_WHEAT:
        		case PORT_WOOD:
        		case PORT_BRICK:
        			AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"2:1\n" + cell.getResource().name());
                    break;
                case PORT_MULTI:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"3:1\n?");
                    break;
                case GOLD:
                    AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, "GOLD\n" + String.valueOf(cell.getDieNum()));
                    break;
                // used for random generation
                case RANDOM_RESOURCE_OR_DESERT:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResourse or\nDesert");
                    break;
                case RANDOM_RESOURCE:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResource");
                    break;
                case RANDOM_PORT_OR_WATER:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random Port\nor\nWater");
                    break;
                case RANDOM_PORT:
                    AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nPort");
                    break;
				case FIELDS:
				case FOREST:
				case HILLS:
				case MOUNTAINS:
				case PASTURE:
					AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, cell.getResource() + "\n" + String.valueOf(cell.getDieNum()));
					break;
				case UNDISCOVERED:
					break;
                
            }
        }        
    }

    private Font bold = null;
    
    private void drawCellsTextured(Graphics g) {
        float cellW = board.getTileWidth();
        float cellH = board.getTileHeight();
        int w = Math.round(cellW / bw * (getWidth()-padding));
        int h = Math.round(cellH / bh * (getHeight()-padding));
        float [] v = {0,0};

        Color outlineColor = GUI.instance.getProps().getColorProperty("board.outlineColor", Color.WHITE);
        Color textColor = GUI.instance.getProps().getColorProperty("board.textcolor", Color.CYAN);

        if (bold == null)
            bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
        for (int i=0; i <board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            render.transformXY(cell.getX(), cell.getY(), v);
            int x = Math.round(v[0]);
            int y = Math.round(v[1]);
            switch (cell.getType()) {
            case NONE:
                g.setColor(outlineColor);
                drawCellOutline(g, cell);
                //Utils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"NONE");
                break;
            case DESERT:
                images.drawImage(g, desertImage, x-w/2, y-h/2, w, h);
                break;
            case WATER:
                images.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                break;
            case PORT_WHEAT:
            case PORT_WOOD:
            case PORT_BRICK:
            case PORT_ORE:
            case PORT_SHEEP:
                images.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"2:1\n" + cell.getResource().name());
                break;
            case PORT_MULTI:
                images.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"3:1\n?");
                break;
                /*
            case RESOURCE:
                switch (cell.getResource()) {
                case Wood:
                    images.drawImage(g, woodImage, x-w/2, y-h/2, w, h);
                    break;
                case Sheep:
                    images.drawImage(g, sheepImage, x-w/2, y-h/2, w, h);
                    break;
                case Ore:
                    images.drawImage(g, oreImage, x-w/2, y-h/2, w, h);
                    break;
                case Wheat:
                    images.drawImage(g, wheatImage, x-w/2, y-h/2, w, h);
                    break;
                case Brick:
                    images.drawImage(g, brickImage, x-w/2, y-h/2, w, h);
                    break;
                }
                if (cell.getDieNum() > 0) {
                    g.setColor(Color.BLACK);
                    AWTUtils.fillCircle(g, x, y+1, 20);
                    g.setColor(textColor);
                    AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, String.valueOf(cell.getDieNum()));
                }
                break;
                */
            case GOLD:
            	images.drawImage(g, goldImage, x-w/2, y-h/2, w, h);
            	if (cell.getDieNum() > 0) {
                    g.setColor(Color.BLACK);
                    AWTUtils.fillCircle(g, x, y+1, 20);
                    g.setColor(textColor);
                    AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, String.valueOf(cell.getDieNum()));
                }
            	break;

            case UNDISCOVERED:
            	images.drawImage(g, undiscoveredImage, x-w/2, y-h/2, w, h);
            	break;

            // used for random generation
            case RANDOM_RESOURCE_OR_DESERT:
                g.setColor(outlineColor);
                drawCellOutline(g, cell);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResourse or\nDesert");
                break;
            case RANDOM_RESOURCE:
                g.setColor(outlineColor);
                drawCellOutline(g, cell);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResource");
                break;
            case RANDOM_PORT_OR_WATER:
                g.setColor(outlineColor);
                drawCellOutline(g, cell);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random Port\nor\nWater");
                break;
            case RANDOM_PORT:
                g.setColor(outlineColor);
                drawCellOutline(g, cell);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nPort");
                break;
            case FIELDS:
            	images.drawImage(g, fieldshexImage, x-w/2, y-h/2, w, h);
				break;
			case FOREST:
				images.drawImage(g, foresthexImage, x-w/2, y-h/2, w, h);
				break;
			case HILLS:
				images.drawImage(g, hillshexImage, x-w/2, y-h/2, w, h);
				break;
			case MOUNTAINS:
				images.drawImage(g, mountainshexImage, x-w/2, y-h/2, w, h);
				break;
			case PASTURE:
				images.drawImage(g, pastureshexImage, x-w/2, y-h/2, w, h);
				break;                
            }
            
            if (cell.getDieNum() > 0) {
                g.setColor(Color.BLACK);
                AWTUtils.fillCircle(g, x, y+1, 20);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, String.valueOf(cell.getDieNum()));
            }
        }   
        
        
    }
    
    private void computeBoardRect() {
        if (this.pickMode == PickMode.PM_CELLPAINT) {
            bx = by = 0;
            bw = bh = 1;
            return;
        }
        float xmin = Float.MAX_VALUE;
        float ymin = Float.MAX_VALUE;
        float xmax = Float.MIN_VALUE;
        float ymax = Float.MIN_VALUE;
        for (int i=0 ;i<board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            if (cell.getType() != TileType.NONE) {
                for (int ii : cell.getAdjVerts()) {
                    Vertex v = board.getVertex(ii);
                    xmin = Math.min(v.getX(), xmin);
                    ymin = Math.min(v.getY(), ymin);
                    xmax = Math.max(v.getX(), xmax);
                    ymax = Math.max(v.getY(), ymax);
                }
            }
        }
        bx = xmin;
        by = ymin;
        bw = xmax - xmin;
        bh = ymax - ymin;
    }
    
    private float bx, by, bw, bh;
    
    @Override
	public void paint(Graphics g) {
	    
        try {
    	    if (getWidth() <= padding+5 || getHeight() <= padding+5)
    	        return;
    	    
    	    long enterTime = System.currentTimeMillis();
    	    int PICKABLE_ELEM_ALPHA = 120;
    	    
    	    computeBoardRect();
    		g.setColor(bkColor);
    		g.fillRect(0,0,getWidth(),getHeight());
    		//float xs = (float)getWidth();
    		//float ys = (float)getHeight();
    		render.setOrtho(0, getWidth(), 0, getHeight());
    		render.makeIdentity();
    		render.translate(padding, padding);
    		render.scale(1f/bw, 1f/bh);
    		render.scale(getWidth()-2*padding, getHeight()-2*padding);
            render.translate(-bx, -by);
            //render.translate(bx, by);
            if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES)) {
                this.drawCellsTextured(g);
            } 
            
            if (getRenderFlag(RenderFlag.DRAW_CELL_OUTLINES)) {
                drawCellsOutlined(g);
            }

            int ignoreStructureVertex = -1;
            
            switch (pickMode) {
            	case PM_ROAD: {
                	if (pickIndices != null) {
                		g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int eIndex : pickIndices) {
                    		drawRoad(g, board.getRoute(eIndex), false);
                    	}
                	}
                	
                	if (pickedValue >= 0) {
    //                	g.setColor(listener.getPlayerColor(pickPlayerNum));
                        drawRoad(g, board.getRoute(pickedValue), true);
                	}
                	break;
                }
            	
            
            	case PM_SHIP: {
                	if (pickIndices != null) {
                    	g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int eIndex : pickIndices) {
                    		drawShip(g, board.getRouteMidpoint(board.getRoute(eIndex)), false);
                    	}
                	}
                	if (pickedValue >= 0) {
    //                	g.setColor(listener.getPlayerColor(pickPlayerNum));
                        drawShip(g, board.getRouteMidpoint(board.getRoute(pickedValue)), true);
                	}
                	break;
                }
            
            	case PM_MOVABLE_SHIPS: {
                	if (pickIndices != null) {
                    	g.setColor(listener.getPlayerColor(pickPlayerNum));
                    	for (int eIndex : pickIndices) {
                    		drawShip(g, board.getRouteMidpoint(board.getRoute(eIndex)), true);
                    	}
                	}
                	if (pickedValue >= 0) {
                    	g.setColor(Color.YELLOW);
                    	drawShip(g, board.getRouteMidpoint(board.getRoute(pickedValue)), true);
                	}
                	break;
                }
            
            	case PM_SETTLEMENT: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawSettlement(g, x, y, 0, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawSettlement(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
        		}
    		
            	case PM_CITY: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawCity(g, x, y, 0, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawCity(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
        		}        
    		
            	case PM_WALLED_CITY: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawWalledCity(g, x, y, 0, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawWalledCity(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
        		}
            	
            	case PM_METROPOLIS_TRADE: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawMetropolisTrade(g, x, y, pickPlayerNum, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawMetropolisTrade(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
            	}

            	case PM_METROPOLIS_POLITICS: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawMetropolisPolitics(g, x, y, pickPlayerNum, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawMetropolisPolitics(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
            	}

            	case PM_METROPOLIS_SCIENCE: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawMetropolisScience(g, x, y, pickPlayerNum, false);
                    	}
        			}
                	if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				drawMetropolisScience(g, x, y, pickPlayerNum, true);
        				ignoreStructureVertex = pickedValue;
        			}
                	break;
            	}

            	case PM_PROMOTE_KNIGHT:
            	case PM_KNIGHT: {
        			if (pickIndices != null) {
            			g.setColor(AWTUtils.setAlpha(listener.getPlayerColor(pickPlayerNum), PICKABLE_ELEM_ALPHA));
                    	for (int vIndex : pickIndices) {
            				Vertex v = board.getVertex(vIndex);
            				float x = v.getX();
            				float y = v.getY();
            				drawKnight(g, x, y, pickPlayerNum, 1, false, false);
                    	}
        			}
        			if (pickedValue >= 0) {
        				Vertex v = board.getVertex(pickedValue);
        				float x = v.getX();
        				float y = v.getY();
        				boolean active = false;
        				if (v.isKnight()) {
        					if (!v.isActiveKnight())
        						active = true;
        				}
        				drawKnight(g, x, y, pickPlayerNum, 1, active, true);
        			}
        			break;
        		}
            }
            
            if (!getRenderFlag(RenderFlag.DONT_DRAW_ROADS)) {
        		// draw the roads
        		for (int i=0; i<board.getNumRoutes(); i++) {
        			Route e = board.getRoute(i);
        			if (e.getPlayer() <= 0)
        				continue;
        			g.setColor(listener.getPlayerColor(e.getPlayer()));
        			drawEdge(g, e, false);
        		}
            }
            
    		
    		// draw the structures
            if (!getRenderFlag(RenderFlag.DONT_DRAW_STRUCTURES)) {
        		for (int i=0; i<board.getNumVerts(); i++) {
        			if (i == ignoreStructureVertex)
        				continue;
        			Vertex v = board.getVertex(i);
        			if (v.getPlayer() <= 0)
        				continue;
        			float x = v.getX();
        			float y = v.getY();
        			switch (v.getType()) {
        				case SETTLEMENT:
        					drawSettlement(g, x, y, v.getPlayer(), false);
        					break;
        				case CITY:
        					drawCity(g, x, y, v.getPlayer(), false);
        					break;
        				case WALLED_CITY:
        					drawWalledCity(g, x, y, v.getPlayer(), false);
        					break;
						case METROPOLIS_SCIENCE:
							break;
						case METROPOLIS_POLITICS:
							break;
						case METROPOLIS_TRADE:
							break;
						case BASIC_KNIGHT_ACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 1, true, false);
							break;
						case BASIC_KNIGHT_INACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 1, false, false);
							break;
						case STRONG_KNIGHT_ACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 2, true, false);
							break;
						case STRONG_KNIGHT_INACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 2, false, false);
							break;
						case MIGHTY_KNIGHT_ACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 3, true, false);
							break;
						case MIGHTY_KNIGHT_INACTIVE:
							drawKnight(g, x, y, v.getPlayer(), 3, false, false);
							break;
						case OPEN:
							break;
						default:
							break;
        					
        				
        			}
        		}
            }
    		
    		if (pickMode == PickMode.PM_ISLAND && pickedValue >= 0) {
            	Collection<Integer> islandEdges = board.findIslandShoreline(pickedValue);
        		g.setColor(Color.YELLOW);
        		render.clearVerts();
            	for (int eIndex : islandEdges) {
            		renderEdge(board.getRoute(eIndex));
            	}
            	render.drawLines(g, 5);

            	Tile cell = board.getTile(pickedValue);
            	if (cell.getIslandNum() > 0) {
                	MutableVector2D midpoint = new MutableVector2D();
                	int num = 0;
                	for (int eIndex : islandEdges) {
                		midpoint.addEq(board.getRouteMidpoint(board.getRoute(eIndex)));
                		num++;
                	}
                	midpoint.scaleEq(1.0f / num);
                	render.transformXY(midpoint);
                	String txt = "ISLAND\n" + cell.getIslandNum();
                	Dimension dim = AWTUtils.computeTextDimension(g, txt);
                	g.setColor(new Color(0,0,0,0.5f));
                	int x = midpoint.Xi() - (dim.width/2+5);
                	int y = midpoint.Yi() - (dim.height/2+5);
                	int w = dim.width + 10;
                	int h = dim.height + 10;
                	g.fillRect(x, y, w, h);
                	g.setColor(Color.white);
                	AWTUtils.drawJustifiedString(g, midpoint.Xi(), midpoint.Yi(), Justify.CENTER, Justify.CENTER, txt);
            	}
            }
    		
    		if (pickMode == PickMode.PM_EDGE && pickedValue >= 0) {
            	g.setColor(Color.YELLOW);
            	drawRoad(g, board.getRoute(pickedValue), false);
            }

            if (pickMode == PickMode.PM_CELL && pickedValue >= 0) {
            	g.setColor(Color.YELLOW);
            	drawCellOutline(g, board.getTile(pickedValue));
            }
            
            if (pickMode == PickMode.PM_VERTEX && pickedValue >= 0) {
            	g.setColor(Color.YELLOW);
            	Vertex v = board.getVertex(pickedValue);
            	render.clearVerts();
            	render.addVertex(v);
            	render.fillPoints(g, 8);
            }    		
            
            if (pickMode == PickMode.PM_KNIGHT && pickedValue >= 0) {
            	Vertex v = board.getVertex(pickedValue);
            	int knightLevel = 0;
            	switch (v.getType()) {
            		case OPEN:
					case BASIC_KNIGHT_ACTIVE:
					case BASIC_KNIGHT_INACTIVE:
						knightLevel = 1;
						break;
					case STRONG_KNIGHT_ACTIVE:
					case STRONG_KNIGHT_INACTIVE:
						knightLevel = 2;
						break;
					case MIGHTY_KNIGHT_ACTIVE:
					case MIGHTY_KNIGHT_INACTIVE:
						knightLevel = 3;
						break;
					default:
						assert(false);
						break;
            	}
            	drawKnight(g, v.getX(), v.getY(), pickPlayerNum, knightLevel, true, true);
            }
    		
    		if (pickMode == PickMode.PM_PATH || pickMode == PickMode.PM_ROUTE || pickMode == PickMode.PM_ROUTE2) {
        		g.setColor(Color.GREEN);
        		render.clearVerts();
            	if (pickedPathStart >= 0) {
            		if (pickedValue >= 0) {
            			List<Integer> path = board.findShortestRoute(pickedPathStart, pickedValue, pickPlayerNum);
            			for (int vIndex : path) {
            				render.addVertex(board.getVertex(vIndex));
            			}
            			render.drawLineStrip(g, 6);
            		} else {
                		render.addVertex(board.getVertex(pickedPathStart));
                		render.fillPoints(g, 8);
            		}
            	} else if (pickedValue >= 0) {
            		g.setColor(Color.RED);
            		render.addVertex(board.getVertex(pickedValue));
            		render.fillPoints(g, 8);
            	}
            }    		
    		
    		int robberTile = board.getRobberTile();
    		int pirateTile = board.getPirateTile();
    		int merchantTile = board.getMerchantTile();
    		int merchantPlayer = board.getMerchantPlayer();
    		
    		if (pickMode == PickMode.PM_ROBBER && pickedValue >= 0) {
    			Tile cell = board.getTile(pickedValue);
    			if (cell.isWater()) {
    				pirateTile = pickedValue;
    			} else {
    				robberTile = pickedValue;
    			}
    		}
    		
    		if (pickMode == PickMode.PM_MERCHANT && pickedValue >= 0) {
    			merchantTile = pickedValue;
    			merchantPlayer = pickPlayerNum;
    		}
    		
    		if (robberTile >= 0)
    		    drawRobber(g, board.getTile(robberTile));
    		if (pirateTile >= 0)
    			drawPirate(g, board.getTile(pirateTile));
    		if (merchantTile >= 0)
    			drawMerchant(g, board.getTile(merchantTile), merchantPlayer);
    		
    		if (pickMode == PickMode.PM_CELLPAINT) {
    			if (pickedValue >= 0) {
    				g.setColor(Color.RED);
    				drawCellOutline(g, board.getTile(pickedValue));
    			}
    		}
    
    		List<Animation> t = null;
    		synchronized (animations) {
    		    t = new ArrayList<Animation>(animations);
    		    animations.clear();
    		}
    		
    		// draw animations
    		for (int i=0; i<t.size(); ) {
    		    Animation anim = t.get(i);
    		    if (anim.isDone()) {
    		        t.remove(i);
    		    } else {
    		        anim.update(g);
    		        i++;
    		    }
    		}
    		
    		synchronized (animations) {
    		    animations.addAll(t);
    		}
    		
    		if (getRenderFlag(RenderFlag.DRAW_CELL_CENTERS)) {
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        render.addVertex(c.getX(), c.getY());
    		    }
    		    g.setColor(Color.yellow);
    		    render.drawPoints(g, 8);
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_CELLS)) {
    		    g.setColor(Color.RED);
    		    float [] v = {0,0};
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        render.transformXY(c.getX(), c.getY(), v);
    		        AWTUtils.drawJustifiedString(g, Math.round(v[0]), Math.round(v[1])+5, Justify.CENTER, Justify.TOP, String.valueOf(i));
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_VERTS)) {
                g.setColor(Color.WHITE);
                float []v = {0,0};
                for (int i=0; i<board.getNumVerts(); i++) {
                    Vertex vx = board.getVertex(i);
                    render.transformXY(vx.getX(), vx.getY(), v);
                    AWTUtils.drawJustifiedString(g, Math.round(v[0]), Math.round(v[1]), Justify.CENTER, Justify.TOP, String.valueOf(i));
                }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_EDGES)) {
    		    g.setColor(Color.yellow);
    		    for (int i=0; i<board.getNumRoutes(); i++) {
    		        Route e = board.getRoute(i);
    		        //SOCVertex v0 = board.getVertex(e.getFrom());
    		        //SOCVertex v1 = board.getVertex(e.getTo());
    		        //float mx = (v0.getX() + v1.getX()) / 2;
                    //float my = (v0.getY() + v1.getY()) / 2;
    		        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(e));
    		        render.transformXY(m);
    		        AWTUtils.drawJustifiedString(g, Math.round(m.getX()), Math.round(m.getY()), Justify.CENTER, Justify.TOP, String.valueOf(i));
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
    		    if (cellInfoIndex >= 0) {
    		        this.drawCellInfo(g, cellInfoIndex);
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
                if (edgeInfoIndex >= 0) {
                    this.drawEdgeInfo(g, edgeInfoIndex);
                }
    		}
    
    		if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
    		    if (vertexInfoIndex >= 0) {
    		        this.drawVertexInfo(g, vertexInfoIndex);
    		    }
    		}
    
    		if (animations.size() > 0) {
        		do {
        		    long exitTime = System.currentTimeMillis();
        		    long delta = exitTime - enterTime;
        		    if (delta >= 33)
        		        break;
        		} while (true);
                repaint();
    		}
    		
    		// notify anyone wait on me
    		synchronized (this) {
    		    notifyAll();
    		}
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	@Override
	public Dimension getMinimumSize() {
		return new Dimension(256,256);
	}

    @Override
	public void mouseDragged(MouseEvent arg0) {
		mouseMoved(arg0);	
		if (pickMode == PickMode.PM_CELLPAINT && pickedValue >= 0) {
		    board.getTile(pickedValue).setType(this.paintCellType);
		    board.clearRoutes();
		}
	}

    int pickedPathStart = -1;
    
	@Override
	public void mouseMoved(MouseEvent ev) {
		pickedValue = -1;
		switch (pickMode) {
		case PM_NONE: 
		    break;
		case PM_ROAD:
			pickedValue = pickRoad(ev.getX(), ev.getY());
			break;
		case PM_SHIP:
			pickedValue = pickShip(ev.getX(), ev.getY());
			break;
		case PM_EDGE:
		    pickedValue = pickEdge(ev.getX(), ev.getY(), pickIndices);
			break;
		case PM_SETTLEMENT:
			pickedValue = pickSettlement(ev.getX(), ev.getY());
			break;
		case PM_CITY: 
			pickedValue = pickCity(ev.getX(), ev.getY());
			break;
		case PM_WALLED_CITY:
			if (pickIndices == null)
				pickIndices = board.getVertsOfType(pickPlayerNum, VertexType.CITY);
		case PM_METROPOLIS_TRADE:
		case PM_METROPOLIS_POLITICS:
		case PM_METROPOLIS_SCIENCE:
			if (pickIndices == null)
				pickIndices = board.getVertsOfType(pickPlayerNum, VertexType.CITY, VertexType.WALLED_CITY);
			pickedValue = pickCity(ev.getX(), ev.getY());
			break;
		case PM_MOVABLE_SHIPS:
			pickedValue = pickMovableShip(ev.getX(), ev.getY());
			break;
		case PM_PATH:
		case PM_ROUTE:
		case PM_ROUTE2:
			pickedValue = pickEdge(ev.getX(), ev.getY(), pickIndices);
			break;
		case PM_VERTEX:
		case PM_KNIGHT:
		case PM_PROMOTE_KNIGHT:
		    pickedValue = pickVertex(ev.getX(), ev.getY(), pickIndices);
			break;
		case PM_CELLPAINT:
		case PM_ROBBER:
		case PM_CELL:
		case PM_ISLAND:
		case PM_MERCHANT:
		    pickedValue = pickCell(ev.getX(), ev.getY(), pickIndices);
			break;
		default:
		    assert(false); // unhandled case
		}

        if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
            cellInfoIndex = pickCell(ev.getX(), ev.getY(), null);
        }

        if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
            edgeInfoIndex = pickEdge(ev.getX(), ev.getY(), null);
        }

        if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
            vertexInfoIndex = pickVertex(ev.getX(), ev.getY(), null);
        }
        
		repaint();
	}

	@Override
	public int getViewportHeight() {
		return getHeight();
	}

	@Override
	public int getViewportWidth() {
		return getWidth();
	}

    @Override
    public void mouseClicked(MouseEvent arg0) {
        if (pickedValue >= 0) {
            switch (pickMode) {
                case PM_NONE:
                    break;
                case PM_ROAD:
                case PM_SHIP:
                case PM_MOVABLE_SHIPS:
                case PM_SETTLEMENT:
                case PM_METROPOLIS_TRADE:
                case PM_METROPOLIS_POLITICS:
                case PM_METROPOLIS_SCIENCE:
                case PM_CITY:
                case PM_WALLED_CITY:
                case PM_ROBBER:
                case PM_MERCHANT:
                case PM_ISLAND:
                case PM_KNIGHT:
                case PM_PROMOTE_KNIGHT:
				case PM_CELL:
				case PM_EDGE:
				case PM_VERTEX:
                    listener.onPick(pickMode, pickedValue);
                    repaint();
                    break;
                case PM_CELLPAINT:
                    break;
				case PM_PATH:
				case PM_ROUTE:
				case PM_ROUTE2:
					if (pickedValue == pickedPathStart) {
						pickedPathStart = -1;
					} else {
						pickedPathStart = pickedValue;
					}
					break;
            }
        }
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    //final int CLICK_TIME = 800;
    
    @Override
    public void mousePressed(final MouseEvent arg0) {
    	grabFocus();
    	mouseDragged(arg0);
    	/*
        //System.out.println("mouse pressed");
        mouseDownTime = System.currentTimeMillis();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(CLICK_TIME);
                    synchronized (BoardComponent.this) {
                        if (mouseDownTime > 0) // if we havnt release then
                            mouseDragged(arg0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();*/
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    	/*
        long t = System.currentTimeMillis();
        synchronized (this) {
            if (mouseDownTime - t < CLICK_TIME) {
                doMouseClicked();
            }
        }
        mouseDownTime = 0;
        //System.out.println("mouse released");
         
         */
    }

    
    
    @Override
    public void mouseWheelMoved(MouseWheelEvent arg0) {
        if (pickedValue >= 0 && pickMode == PickMode.PM_CELLPAINT) {
            int amount = 0;
            if (arg0.getWheelRotation() < 0)
                amount = 1;
            else
                amount = -1;
            
            Tile cell = board.getTile(pickedValue);
            int type = cell.getDieNum();
            int [] nums = { 0,0,2,3,4,5,6,0,8,9,10,11,12 };
            do {
                type = (type + nums.length + amount) % nums.length;
            } while (nums[type] == 0);
            cell.setDieNum(type);     
            repaint();
        }
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    private void drawInfo(Graphics g, int x, int y, String info) {
        String [] lines = info.split("\n");
        int maxW = 0;
        for (int i=0; i<lines.length; i++) {
            maxW = Math.max(maxW, AWTUtils.getStringWidth(g, lines[i]));
        }
        final int padding = 5;
        int width = maxW + padding*2;
        int height = lines.length * AWTUtils.getFontHeight(g) + 2*padding;
        g.setColor(AWTUtils.setAlpha(Color.DARK_GRAY, 180));
        g.fillRect(x, y, width, height);
        g.setColor(Color.WHITE);
        y -= 2; // slight visual adjustment to center vertically
        
        // make sure we dont draw off screen
        if (x + width > getWidth()) {
            x = getWidth() - width;
        }
        if (y + height > getHeight()) {
            y = getHeight() - height;
        }
        
        for (int i=0; i<lines.length; i++) {
            y += AWTUtils.getFontHeight(g);
            g.drawString(lines[i], x+padding, y+padding);
        }
    }
    
    private void drawCellInfo(Graphics g, int cellIndex) {
        if (cellIndex < 0)
            return;
        Tile cell = board.getTile(cellIndex);
        String info = "CELL " + cellIndex + "\n  " + cell.getType();
        if (cell.getResource() != null) {
            info += "\n  " + cell.getResource();
        }
        if (cell.getIslandNum() > 0) {
        	info += "\n  Island " + cell.getIslandNum();
        }
        
        float [] v = {0,0};
        render.transformXY(cell.getX(), cell.getY(), v);
        int x = Math.round(v[0]);
        int y = Math.round(v[1]);

        drawInfo(g, x, y, info);
    }
    
    private void drawEdgeInfo(Graphics g, int edgeIndex) {
        if (edgeIndex < 0)
            return;
        Route edge = board.getRoute(edgeIndex);
        String info = "EDGE " + edgeIndex;
        if (edge.getPlayer() > 0)
            info += "\n  Player " + edge.getPlayer();
        info += "\n  " + edge.getFlagsString();
        
        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(edge));
        render.transformXY(m);
        
        drawInfo(g, Math.round(m.getX()), Math.round(m.getY()), info);
    }
    
    private void drawVertexInfo(Graphics g, int vertexIndex) {
        if (vertexIndex < 0)
            return;
        Vertex vertex = board.getVertex(vertexIndex);
        String info = "VERTEX " + vertexIndex;
        if (vertex.getPlayer() > 0)
            info += "\n  Player " + vertex.getPlayer() + "\n  " + (vertex.isCity() ? 
                    "City +2" : 
                    "Settlement +1");
        else {
            int numBlocked = board.checkForBlockingRoads(vertexIndex, 3);
            info += "\n  Blocks " + numBlocked + " roads";
        }
        float [] v = { vertex.getX(), vertex.getY() };
        render.transformXY(v);
        
        drawInfo(g, Math.round(v[0]), Math.round(v[1]), info);
    }
	@Override
	public void keyPressed(KeyEvent ev) {
		//Utils.println("keyPressed");
		if (pickMode == PickMode.PM_CELLPAINT && pickedValue >= 0) {
			switch (ev.getKeyCode()) {
			// u/down scroll throgh the numbers
			case KeyEvent.VK_UP:
				scrollTileVertically(pickedValue, 1); break;
			case KeyEvent.VK_DOWN:
				scrollTileVertically(pickedValue, -1); break;
				
			// left/right scroll through the tiles
			case KeyEvent.VK_LEFT:
				scrollTileHorz(pickedValue, -1); break;
			case KeyEvent.VK_RIGHT:
				scrollTileHorz(pickedValue, 1); break;
				
			}
		}
	}
	@Override
	public void keyReleased(KeyEvent arg0) {
		// TODO Auto-generated method stub
		//Utils.println("keyReleased");
	}
	@Override
	public void keyTyped(KeyEvent ev) {
		
	}
	
	private void scrollTileVertically(int cellIndex, int offset) {
		Tile cell = board.getTile(cellIndex);
		if (cell == null)
			return;
		if (cell.isDistributionTile()) {
			int nextDie = cell.getDieNum() + offset;
			if (nextDie > 12)
				nextDie = 2;
			else if (nextDie == 7)
				nextDie += offset;
			else if (nextDie < 2)
				nextDie = 12;
			cell.setDieNum(nextDie);
		} else if (cell.isPort() && cell.getResource() != null) {
			TileType [] portTypes = { TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD };
			TileType type = portTypes[(cell.getResource().ordinal()+offset+SOC.NUM_RESOURCE_TYPES) % SOC.NUM_RESOURCE_TYPES];
			cell.setType(type);			
		}
		repaint();
	}
	
	private void scrollTileHorz(int cellIndex, int offset) {
		Tile cell = board.getTile(cellIndex);
		if (cell == null)
			return;

		TileType [] choices = null;
		switch (cell.getType()) {
			case UNDISCOVERED:
			case DESERT:
			case GOLD:
			case NONE:
				choices = new TileType [] { TileType.UNDISCOVERED, TileType.DESERT, TileType.GOLD, TileType.NONE };
				break;
			case WATER:
			case PORT_MULTI:
			case PORT_BRICK:
			case PORT_ORE:
			case PORT_SHEEP:
			case PORT_WHEAT:
			case PORT_WOOD:
				choices = new TileType [] { TileType.WATER, TileType.PORT_MULTI, TileType.PORT_BRICK, TileType.PORT_ORE, TileType.PORT_SHEEP, TileType.PORT_WHEAT, TileType.PORT_WOOD };
				break;
			case RANDOM_PORT:
			case RANDOM_PORT_OR_WATER:
			case RANDOM_RESOURCE:
			case RANDOM_RESOURCE_OR_DESERT:
				choices = new TileType [] { TileType.RANDOM_PORT, TileType.RANDOM_PORT_OR_WATER, TileType.RANDOM_RESOURCE, TileType.RANDOM_RESOURCE_OR_DESERT };
				break;
			case PASTURE:
			case FOREST:
			case FIELDS:
			case HILLS:
			case MOUNTAINS:
				choices = new TileType [] { TileType.PASTURE, TileType.FOREST, TileType.FIELDS, TileType.HILLS, TileType.MOUNTAINS };
				break;
		}

		if (choices != null) {
			Arrays.sort(choices);
			int index = Arrays.binarySearch(choices, cell.getType());
			index = (index+choices.length+offset) % choices.length;
			cell.setType(choices[index]);
		}

		repaint();
	}
}




