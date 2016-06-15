package cc.games.android.soc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import android.graphics.Color;
import android.opengl.GLSurfaceView;
import cc.game.soc.core.Board;
import cc.game.soc.core.Island;
import cc.game.soc.core.Route;
import cc.game.soc.core.RouteType;
import cc.game.soc.core.Tile;
import cc.game.soc.core.TileType;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.game.AColor;
import cc.lib.game.AGraphics;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;

public abstract class BoardView extends GLSurfaceView {
	
    public enum PickMode {

    	PM_NONE,
    	PM_EDGE,
    	PM_TILE,
    	PM_VERTEX,
    	PM_CUSTOM, // CUSTOM must be associated with a CustomPickHandler
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
        SHOW_ISLAND_INFO,
        
        ;
        RenderFlag() {
            assert(1 << ordinal() > 0);
        }
    }
    
    interface PickHandler {
    	
    	PickMode getPickMode();
    	
    	/**
    	 * Called when mouse pressed on a pickable element
    	 * @param bc
    	 * @param pickedValue
    	 */
    	void onPick(BoardView bc, int pickedValue);
    	
    	/**
    	 * Called when rendering an index that passes the isPickableIndex test
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param index
    	 */
    	void onDrawPickable(BoardView bc, AGraphics g, int index);
    	
    	/**
    	 * Called after tiles, edges and verts are rendered for pick handler to render it own stuff
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param highlightedIndex
    	 */
    	void onDrawOverlay(BoardView bc, AGraphics g);
    	
    	/**
    	 * Render a highlighted index
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param highlightedIndex
    	 */
    	void onHighlighted(BoardView bc, AGraphics g, int highlightedIndex);
    	
    	/**
    	 * Return whether the index is pickable
    	 * @param bc
    	 * @param index
    	 * @return
    	 */
    	boolean isPickableIndex(BoardView bc, int index);
    }
    
    public static interface CustomPickHandler extends PickHandler {

    	/**
    	 * Return number of custom pickable elements 
    	 * @return
    	 */
		int getNumElements();

		/**
		 * Pick a custom element
		 * 
		 * Example:
		 *   for (int i : getNumElements())
		 *      render.setName(i)
		 *      g.vertex(...)
		 *      
		 *   return r.pickPoints(10);
		 *   
		 * @param render
		 * @param x
		 * @param y
		 * @return
		 */
		int pickElement(AGraphics render, int x, int y);
    	
    }

    private Board board;
	
	Board getBoard() {
	    return board;
	}
	
	protected abstract AColor getPlayerColor(int playerNum);

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
	final AColor bkColor;
	final AColor outlineColor;

	private PickMode pickMode = PickMode.PM_NONE;
	private int pickedValue = -1;
	private PickHandler pickHandler = null;
    final int padding;
	
    private int renderFlag = 0;
    private List<Animation> animations = new ArrayList<Animation>(32);
    
    private int edgeInfoIndex = -1;
    private int cellInfoIndex = -1;
    private int vertexInfoIndex = -1;
    
    private int [] knightImages = new int[6];
    
	BoardView(Board board) {
		this.board = board;
	}
	
	void initImages(AGraphics g) {
		
		setFocusable(true);
	    renderFlag          = 0;
        bkColor             = (g.LIGHT_GRAY);
        roadLineThickness   = (4);
        padding             = (20);
        outlineColor        = (g.BLACK);
        
        desertImage  = g.loadImage("desert.png", g.WHITE);
		woodImage    = g.loadImage("wood.png",   g.WHITE);
		wheatImage   = g.loadImage("wheat.png",  g.WHITE);
		oreImage     = g.loadImage("ore.png",    g.WHITE);
		brickImage   = g.loadImage("brick.png",  g.WHITE);
		sheepImage   = g.loadImage("sheep.png",  g.WHITE);
		waterImage   = g.loadImage("water.png",  g.WHITE);
		robberImage  = g.loadImage("robber.png", g.WHITE);
		pirateImage	 = g.loadImage("pirate.png");
		goldImage    = g.loadImage("gold.png");
		
		mountainshexImage 	= g.loadImage("mountainshex.png");
		hillshexImage 		= g.loadImage("hillshex.png");
		pastureshexImage 	= g.loadImage("pastureshex.png");
		fieldshexImage 		= g.loadImage("fieldshex.png");
		foresthexImage 		= g.loadImage("foresthex.png");
		
		undiscoveredImage = g.loadImage("undiscoveredtile.png");
		
		cardFrameImage = g.loadImage("cardFrame.png", g.WHITE);
		knightImages[0] = g.loadImage("knight_basic_inactive.png");
		knightImages[1] = g.loadImage("knight_basic_active.png");
		knightImages[2] = g.loadImage("knight_strong_inactive.png");
		knightImages[3] = g.loadImage("knight_strong_active.png");
		knightImages[4] = g.loadImage("knight_mighty_inactive.png");
		knightImages[5] = g.loadImage("knight_mighty_active.png");
	}	    
    void setRenderFlag(RenderFlag flag, boolean enabled) {
        if (enabled)
            renderFlag |= (1 << flag.ordinal());
        else
            renderFlag &= ~(1 << flag.ordinal());
        postInvalidate();
    }
    
    boolean getRenderFlag(RenderFlag flag) {
        return (renderFlag & (1 << flag.ordinal())) != 0;
    }
    
    void addAnimation(Animation anim) {
        synchronized (animations) {
            animations.add(anim);
        }
        postInvalidate();
        anim.start();
    }

	public void drawTileOutline(AGraphics g, Tile cell, int borderThickness) {
	    g.begin();
		for (int i : cell.getAdjVerts()) {
			Vertex v = board.getVertex(i);
			g.vertex(v.getX(), v.getY());
		}
		g.drawLineLoop(borderThickness);
	}

	enum FaceType {
		SETTLEMENT,
		CITY,
		CITY_WALL,
		PIRATE_FORTRESS,
		SHIP,
		WAR_SHIP,
		KNIGHT_ACTIVE_BASIC,
		KNIGHT_ACTIVE_STRONG,
		KNIGHT_ACTIVE_MIGHTY,
		KNIGHT_INACTIVE_BASIC,
		KNIGHT_INACTIVE_STRONG,
		KNIGHT_INACTIVE_MIGHTY,
		METRO_TRADE,
		METRO_POLITICS,
		METRO_SCIENCE,
		MERCHANT,
		ROBBER,
	}
	
	private static class Face {
		final float darkenAmount;
		final int   numVerts;
		final int [] xVerts;
		final int [] yVerts;
		final int added;
		FaceType [] structures;
		
		Face(float darkenAmount, int ... verts) {
			this(Color.argb(0, 0, 0, 0), darkenAmount, verts);
		}
		
		Face(int added, float darkenAmount, int ... verts) {
			this.darkenAmount = darkenAmount;
			this.numVerts = verts.length/2;
			this.xVerts = new int[numVerts];
			this.yVerts = new int[numVerts];
			this.added = added;
			int index=0;
			for (int i=0; i<numVerts*2; i+=2) {
				xVerts[index] = verts[i];
				yVerts[index] = verts[i+1];
				index++;
			}
		}
		
		Face setFaceTypes(FaceType ... structures) {
			this.structures = structures;
			return this;
		}
	};
	
	private static final Face [] structureFaces = {
		// house front
	    new Face(0.0f, 	0,0, 2,3, 4,0, 4,-4, 0,-4).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // house roof
	    new Face(0.25f, 0,0, 2,3, 0,4, -2,1).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // house side
	    new Face(0.45f, 0,0, -2,1, -2,-3, 0,-4).setFaceTypes(FaceType.SETTLEMENT, FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // city front panel
	    new Face(0.0f, 	0,0, -2,0, -2,-4, 0,-4).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // city side panel
	    new Face(0.45f, -2,0,-4,1,-4,-3,-2,-4).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // city roof
	    new Face(0.1f, 	0,0,-2,1,-4,1,-2,0).setFaceTypes(FaceType.CITY, FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // walled city
	    // wall right
	    new Face(0.6f, 4,0, 6,-2, 4,-2).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // wall front
	    new Face(0.1f, 6,-2, 6,-5, -3,-5, -3,-2).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // wall left
	    new Face(0.5f, -5,0, -3,-2, -3,-5, -5,-3).setFaceTypes(FaceType.CITY_WALL, FaceType.PIRATE_FORTRESS),
	    // pirate flag
	    new Face(0.7f, -5,1, -3,0, -3,4, -5,5).setFaceTypes(FaceType.PIRATE_FORTRESS),
	    new Face(0.9f, -3,0, -3,4, 0,5, 0,1).setFaceTypes(FaceType.PIRATE_FORTRESS),
	    new Face(0.0f, -4,4, -3,3, -4,3).setFaceTypes(FaceType.PIRATE_FORTRESS),
	    new Face(0.0f, -2,4, -1,4, -1,3, -2,3).setFaceTypes(FaceType.PIRATE_FORTRESS),
	    new Face(0.0f, -4,2, -1,2, -3,1).setFaceTypes(FaceType.PIRATE_FORTRESS),
	    
	    // ship
	    // hull bottom
	    new Face(0.5f, -4,0, 4,0, 3,-2, -3,-2).setFaceTypes(FaceType.SHIP, FaceType.WAR_SHIP),
	    new Face(0.3f, -4,0, -2,1, 2,1, 4,0, 2,-1, -2,-1).setFaceTypes(FaceType.SHIP, FaceType.WAR_SHIP),
	    
	    // traingle sail (ship)
	    new Face(0.1f, 1,0, 1,5, -2,0).setFaceTypes(FaceType.SHIP),
	    
	    // square sail (warship)
	    new Face(0.2f, 2,2, 2,6, -2,4, -2,0).setFaceTypes(FaceType.WAR_SHIP),
	    new Face(0.0f, 0,2, 1,3, 1,5, -1,4, -1,2).setFaceTypes(FaceType.WAR_SHIP),
	    
	    // 3D Shield
	    // full darkened (inactive)
	    new Face(0.2f, 4,5, 3,5, 0,4, 1,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
	    new Face(0.1f, -2,5, -3,5, -3,0, -2,-1).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
	    new Face(0.8f, 1,-4, 4,-1, 4,5, 1,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),
	    new Face(0.6f, 1,-4, -2,-1, -2,5, 1,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_BASIC, FaceType.KNIGHT_INACTIVE_STRONG, FaceType.KNIGHT_INACTIVE_MIGHTY),

	    // SAA only lightened
	    new Face(0.2f, 4,5, 3,5, 0,4, 1,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
	    new Face(0.1f, -2,5, -3,5, -3,0, -2,-1).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
	    new Face(0.1f, 1,-4, 4,-1, 4,5, 1,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
	    new Face(0.1f, 1,-4, -2,-1, -2,5, 1,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_BASIC, FaceType.KNIGHT_ACTIVE_STRONG, FaceType.KNIGHT_ACTIVE_MIGHTY),
	    
	    // single sword, strong dark (inactive)
	    // blade
	    new Face(Color.argb(0,255,255,255), 0.8f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),
	    // hilt
	    new Face(Color.argb(0,255,255,255), 0.8f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),

	    // SAA lightened for active
	    // blade
	    new Face(Color.argb(0,160,160,160), 0.1f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    // hilt
	    new Face(Color.argb(0,100,100,100), 0.1f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    
	    // double crossed sword mighty dark (inactive)
	    // handle at top right
	    // blade
	    new Face(0.8f, 5,8, 7,7, -1,-5, -3,-6, -3,-4).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),
	    // hilt
	    new Face(0.8f, 3,7, 2,6, 6,3, 7,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),
	    // handle at top left
	    // blade
	    new Face(0.8f, -6,7, -4,8, 4,-4, 4,-6, 2,-5).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),
	    // hilt
	    new Face(0.8f, -6,4, -5,3, -1,6, -2,7).setFaceTypes(FaceType.KNIGHT_INACTIVE_MIGHTY),

	    // SAA lightened for active
	    // handle at top right
	    // blade
	    new Face(0.2f, 5,8, 7,7, -1,-5, -3,-6, -3,-4).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),
	    // hilt
	    new Face(0.1f, 3,7, 2,6, 6,3, 7,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),
	    // handle at top left
	    // blade
	    new Face(0.2f, -6,7, -4,8, 4,-4, 4,-6, 2,-5).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),
	    // hilt
	    new Face(0.1f, -6,4, -5,3, -1,6, -2,7).setFaceTypes(FaceType.KNIGHT_ACTIVE_MIGHTY),

	    // Trade Metropolis
	    // right most building
	    new Face(0.2f, 2,-4, 5,-4, 5,4, 2,5).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.4f, 1,5,  2,4,  2,0, 1,0).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.0f,  2,4, 5,4, 4,5, 1,5).setFaceTypes(FaceType.METRO_TRADE),
	    // middle building
	    new Face(0.0f, 3,0, 1,2, 1,0).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.2f, -2,4, 1,4, 1,0, -2,0).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.2f, -2,0, 3,0, 3,-5, -2,-5).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.4f, -2,4, -2,-5, -4,-3, -4,6).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.0f, -2,4, -4,6, -1,6, 1,4).setFaceTypes(FaceType.METRO_TRADE),
	    // left most building
	    new Face(0.2f, -3,1, -5,1, -5,-4, -3,-4).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.4f, -6,2, -5,1, -5,-4, -6,-3).setFaceTypes(FaceType.METRO_TRADE),
	    new Face(0.0f, -3,1, -4,2, -6,2, -5,1).setFaceTypes(FaceType.METRO_TRADE),
	    
	    // Politics Metropolis
	    // roofs from right to left
	    new Face(0.0f, 5,0, 7,-1, 4,-1, 2,0).setFaceTypes(FaceType.METRO_POLITICS),
	    new Face(0.1f, 1,3, -1,4, -3,3, -1,2).setFaceTypes(FaceType.METRO_POLITICS),
	    new Face(0.2f, -1,2, -2,0, -4,1, -3,3).setFaceTypes(FaceType.METRO_POLITICS),
	    new Face(0.3f, -2,0, -4,1, -4,0, -2,-1).setFaceTypes(FaceType.METRO_POLITICS),
	    new Face(0.0f, -4,0, -7,0, -5,-1, -2,-1).setFaceTypes(FaceType.METRO_POLITICS),
	    // front
	    new Face(0.5f, 1,3, 3,2, 4,0, 4,-1, -2,-1, -2,0, -1,2).setFaceTypes(FaceType.METRO_POLITICS),
	    new Face(0.5f, 7,-1, 7,-4, -5,-4, -5,-1).setFaceTypes(FaceType.METRO_POLITICS),
	    // left wall
	    new Face(0.6f, -7,0, -5,-1, -5,-4, -7,-3).setFaceTypes(FaceType.METRO_POLITICS),
	    // door
	    new Face(1.0f, 0,0, 2,0, 2,-4, 0,-4).setFaceTypes(FaceType.METRO_POLITICS),
	    
	    // Science metropolis
	    // column sides
	    new Face(0.8f, -3,3, 2,3, 2,-2, -3,-2).setFaceTypes(FaceType.METRO_SCIENCE),
	    // base top
	    new Face(0.7f, 2,-1, 3,-2, -2,-2, -3,-1).setFaceTypes(FaceType.METRO_SCIENCE),
	    // base angled side
	    new Face(0.2f, -3,-1, -2,-2, -3,-3, -4,-2).setFaceTypes(FaceType.METRO_SCIENCE),
	    // base side
	    new Face(0.4f, -4,-2, -4,-3, -3,-4, -3,-3).setFaceTypes(FaceType.METRO_SCIENCE),
	    // base front
	    new Face(0.5f, -2,-2, 3,-2, 4,-3, 4,-4, -3,-4, -3,-3).setFaceTypes(FaceType.METRO_SCIENCE),
	    // right column front
	    new Face(0.5f, 2,2, 3,2, 3,-2, 2,-2).setFaceTypes(FaceType.METRO_SCIENCE),
	    // center column front
	    new Face(0.5f, 0,2, 1,2, 1,-2, 0,-2).setFaceTypes(FaceType.METRO_SCIENCE),
	    // left column front
	    new Face(0.5f, -2,2, -1,2, -1,-2, -2,-2).setFaceTypes(FaceType.METRO_SCIENCE),
	    // roof top
	    new Face(0.0f, -2,5, 1,5, 2,4, -1,4).setFaceTypes(FaceType.METRO_SCIENCE),
	    // roof left 
	    new Face(0.2f, -2,5, -1,4, -4,2, -5,3).setFaceTypes(FaceType.METRO_SCIENCE),
	    // roof front
	    new Face(0.5f, -4,2, -1,4, 2,4, 5,2).setFaceTypes(FaceType.METRO_SCIENCE),

	    // Merchant
	    // Dome
	    new Face(0.0f, 1,3, 3,2, 4,0, 4,-4, -4,-4, -4,0, -3,2, -1,3).setFaceTypes(FaceType.MERCHANT),
	    // door
	    new Face(1.0f, 1,-1, 1,-4, -1,-4, -1,-1).setFaceTypes(FaceType.MERCHANT),
	    // door flap
	    new Face(0.2f, 1,-1, -1,-4, -2,-1).setFaceTypes(FaceType.MERCHANT),
	    // flag
	    new Face(0.0f, 0,3, 0,6, 3,4).setFaceTypes(FaceType.MERCHANT),
	    
	    // Robber
	    // Head
	    new Face(0.0f, 3,2, 3,-4, -3,-4, -3,2).setFaceTypes(FaceType.ROBBER),
	    // Right Hat
	    new Face(1.0f, 0,2, 4,2, 4,3, 2,3, 1,5, 0,4).setFaceTypes(FaceType.ROBBER),
	    // Left Hat
	    new Face(1.0f, 0,2, -4,2, -4,3, -2,3, -1,5, 0,4).setFaceTypes(FaceType.ROBBER),
	    // Right Eye
	    new Face(1.0f, 1,0, 1,1, 3,1, 3,0).setFaceTypes(FaceType.ROBBER),
	    // Left Eye
	    new Face(1.0f, -1,0, -1,1, -3,1, -3,0).setFaceTypes(FaceType.ROBBER),
	    // Right Coat
	    new Face(1.0f, 1,-2, 4,0, 4,-5, 1,-5).setFaceTypes(FaceType.ROBBER),
	    // LeftCoat
	    new Face(1.0f, -1,-2, -4,0, -4,-5, -1,-5).setFaceTypes(FaceType.ROBBER),

	    
	};
	
	void drawSettlement(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.SETTLEMENT, outline);
	}
	
	void drawCity(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.CITY, outline);
	}
	
	void drawWalledCity(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.CITY_WALL, outline);
	}
	
	void drawMetropolisTrade(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.METRO_TRADE, outline);
	}

	void drawMetropolisPolitics(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.METRO_POLITICS, outline);
	}

	void drawMetropolisScience(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, board.getTileWidth()/6, FaceType.METRO_SCIENCE, outline);
	}

	void drawMerchant(AGraphics g, Tile t, int playerNum) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, t, 0, board.getTileWidth()/6, FaceType.MERCHANT, false);
		g.setColor(g.WHITE);
		String txt = "\n2:1\n" + t.getResource().name();
		Vector2D v = g.transform(t);
		
		g.drawJustifiedString(v.Xi()-2, v.Yi()-2-g.getTextHeight()*2, Justify.CENTER, Justify.TOP, txt);
	}

	void drawKnight_image(AGraphics g, float _x, float _y, int playerNum, int level, boolean active, boolean outline) {
		final int x = Math.round(_x);
		final int y = Math.round(_y);
		final int r = (int)(board.getTileWidth()/8) + 1;
		final int r2 = r+3;
		int index = level * (active ? 2 : 1) - 1;
		g.drawOval(x-r2/2, y-r2/2, r2, r2);
		g.drawImage(knightImages[index], x-r/2, y-r/2, r, r);
	}
	void drawKnight(AGraphics g, IVector2D pos, int playerNum, int level, boolean active, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		final float radius = board.getTileWidth()/8;
		float xRad = 3;
		float scale = radius / xRad;
		FaceType structure = null;
		switch (level) {
			case 0:
			case 1: structure = active ? FaceType.KNIGHT_ACTIVE_BASIC : FaceType.KNIGHT_INACTIVE_BASIC; break;
			case 2: structure = active ? FaceType.KNIGHT_ACTIVE_STRONG : FaceType.KNIGHT_INACTIVE_STRONG; break;
			case 3: structure = active ? FaceType.KNIGHT_ACTIVE_MIGHTY : FaceType.KNIGHT_INACTIVE_MIGHTY; break;
			default: assert(false); break;
		}
		drawFaces(g, pos, 0, scale*2/3, scale*2/3, structure, outline);
	}
	
	void drawPirateFortress(AGraphics g, Vertex v, boolean outline) {
		MutableVector2D mv = g.transform(v);
		int x = mv.Xi()-10;
		int y = mv.Yi()-10;
		for (int i=0; i<v.getPirateHealth(); i++) {
			int rw = 40;
			int rh = 30;
			g.setColor(g.GRAY);
			g.drawFilledOval(x,y,rw,rh);
			g.setColor(g.RED);
			g.drawOval(x,y,rw,rh);
			x += 10;
			y += 5;
		}
		g.setColor(g.GRAY);
		drawFaces(g, v, 0, board.getTileWidth()/6, FaceType.PIRATE_FORTRESS, outline);
	}

	void drawFaces(AGraphics g, IVector2D pos, float angle, float radius, FaceType structure, boolean outline) {
	    final float xRad = 3; // actual radius as defined above
		float scale = radius / xRad;
		drawFaces(g, pos, angle, scale, scale, structure, outline);
	}

	private HashMap<FaceType, Face[]> faceMap = new HashMap<BoardView.FaceType, BoardView.Face[]>();
	
	Face [] getStructureFaces(FaceType s) {
		Face [] faces = faceMap.get(s);
		if (faces == null) {
			ArrayList<Face> a = new ArrayList<Face>();
			for (int i=0; i<structureFaces.length; i++) {
				if (structureFaces[i].structures != null) {
    				if (Arrays.binarySearch(structureFaces[i].structures, s) >= 0) {
    					a.add(structureFaces[i]);
    				}
				}
			}
			faces = a.toArray(new Face[a.size()]);
			faceMap.put(s, faces);
		}
		return faces;
	}
	
	void drawFaces(AGraphics g, IVector2D pos, float angle, float w, float h, FaceType structure, boolean outline) {
		g.pushMatrix();
		g.translate(pos);
		g.rotate(angle);
		g.scale(w, -h);
		AColor saveColor = g.getColor();

		Face [] faces = getStructureFaces(structure);
	    for (Face face : faces) {
		    g.begin();
	    	for (int i=0; i<face.numVerts; i++)
	    		g.vertex(face.xVerts[i], face.yVerts[i]);
	    	
	    	if (outline) {
	    	    g.setColor(outlineColor);
	    	    g.drawLineLoop(2);
	    	}

	    	AColor c = AWTUtils.addColors(face.added, AWTUtils.darken(saveColor, face.darkenAmount));
            g.setColor(c);
            g.drawTriangleFan();
	    }
	    g.setColor(saveColor);
	    g.popMatrix();
	}
	
	private int pickEdge(AGraphics g, int mouseX, int mouseY) {
		g.begin();
		for (int index=0; index<getBoard().getNumRoutes(); index++) {
			g.setName(index);
			renderEdge(board.getRoute(index));
		}
		return g.pickLines(mouseX, mouseY, this.roadLineThickness*2);
	}
	
	private int pickVertex(AGraphics g, int mouseX, int mouseY) {
		g.begin();
		for (int index=0; index<board.getNumVerts(); index++) {
			render.setName(index);
			Vertex v = board.getVertex(index);
			g.vertex(v);
		}
		return render.pickPoints(mouseX, mouseY, 10);
	}
	
	private int pickTile(AGraphics g, int mouseX, int mouseY) {
		g.begin();
		final int dim = Math.round(board.getTileWidth() * getWidth());
		for (int index=0; index<board.getNumTiles(); index++) {
			render.setName(index);
			Tile cell = board.getTile(index);
			g.vertex(cell);
		}
		return render.pickPoints(mouseX, mouseY, dim);
	}
	
	private void renderEdge(AGraphics g, Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		g.vertex(v0);
		g.vertex(v1);
	}
	
	private void renderDamagedEdge(AGraphics g, Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		g.begin();
		g.vertex(v0);
		Vector2D v = board.getRouteMidpoint(e);
		g.vertex(v);
		Vector2D dv = Vector2D.newTemp(v).subEq(v0).normEq().addEq(v);
		g.vertex(dv);
	}
	
	public void drawDamagedRoad(AGraphics g, Route e, boolean outline) {
		g.begin();
	    if (outline) {
	        AColor old = g.getColor();
	        g.setColor(outlineColor);
            renderDamagedEdge(g, e);
            g.drawLines(roadLineThickness+2);
            g.setColor(old);
	    } 
	    renderDamagedEdge(g, e);
	    g.drawLineStrip(roadLineThickness);	
	}

	public void drawEdge(AGraphics g, Route e, RouteType type, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		switch (type) {
			case OPEN:
				break;
			case DAMAGED_ROAD:
				drawDamagedRoad(g, e, outline);
				break;
			case ROAD:
				drawRoad(g, e, outline);
				break;
			case SHIP:
				drawShip(g, e, outline);
				break;
			case WARSHIP:
				drawWarShip(g, e, outline);
				break;
		}
	}
	
	public void drawVertex(AGraphics g, Vertex v, VertexType type, int playerNum, boolean outline) {
		switch (type) {
			case OPEN:
				break;
			case PIRATE_FORTRESS:
				drawPirateFortress(g, v, outline);
				break;
			case SETTLEMENT:
				drawSettlement(g, v, playerNum, outline);
				break;
			case CITY:
				drawCity(g, v, playerNum, outline);
				break;
			case WALLED_CITY:
				drawWalledCity(g, v, playerNum, outline);
				break;
			case METROPOLIS_SCIENCE:
				drawMetropolisScience(g, v, playerNum, outline);
				break;
			case METROPOLIS_POLITICS:
				drawMetropolisPolitics(g, v, playerNum, outline);
				break;
			case METROPOLIS_TRADE:
				drawMetropolisTrade(g, v, playerNum, outline);
				break;
			case BASIC_KNIGHT_ACTIVE:
			case BASIC_KNIGHT_INACTIVE:
			case STRONG_KNIGHT_ACTIVE:
			case STRONG_KNIGHT_INACTIVE:
			case MIGHTY_KNIGHT_ACTIVE:
			case MIGHTY_KNIGHT_INACTIVE:
				drawKnight(g, v, playerNum, type.getKnightLevel(), type.isKnightActive(), outline);
				break;
		}		
	}
	
	public void drawRoad(AGraphics g, Route e, boolean outline) {
		g.begin();
	    if (outline) {
	        AColor old = g.getColor();
	        g.setColor(outlineColor);
            renderEdge(g, e);
            g.drawLines(roadLineThickness+2);
            g.setColor(old);
	    } 
	    renderEdge(g, e);
	    g.drawLineStrip(roadLineThickness);
	}
	
	public int getEdgeAngle(Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		if (v1.getX() < v0.getX()) {
			Vertex t = v0;
			v0 = v1;
			v1 = t;
		}
		int ang = Math.round(Vector2D.newTemp(v1).sub(v0).angleOf());
		// we want eight 60, 300 or 0
		return ang;
	}
	
	void drawShip(AGraphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawFaces(g, mp, getEdgeAngle(e), board.getTileWidth()/8, FaceType.SHIP, outline);
	}
	
	void drawShip(AGraphics g, IVector2D v, int angle, boolean outline) {
		drawFaces(g, v, angle, board.getTileWidth()/8, FaceType.SHIP, outline);
	}
	
	void drawWarShip(AGraphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawFaces(g, mp, getEdgeAngle(e), board.getTileWidth()/8, FaceType.WAR_SHIP, outline);
	}
	
	public void drawRobber(AGraphics g, Tile cell) {
		g.setColor(g.LIGHT_GRAY);
		drawFaces(g, cell, 0, getBoard().getTileWidth()/7, FaceType.ROBBER, false);
		/*
	    //float [] v = {0,0};
	    //g.transform(board.getCellWidth(), board.getCellHeight(), v);
		g.begin();
	    float sx = board.getTileWidth() / bw * (getWidth()-padding);
	    float sy = board.getTileHeight() / bh * (getHeight()-padding);
	    float [] v = {0,0};
        int w = Math.round(sx) / 2;
        int h = Math.round(sy) / 2;
        g.transform(cell.getX(), cell.getY(), v);
        int x = Math.round(v[0]) - w/2;
        int y = Math.round(v[1]) - h/2;
        //int x = Math.round(sx * cell.getX()) - w/2;
        //int y = Math.round(sy * cell.getY()) - h/2;
        if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES))
            g.drawImage(g, robberImage, x, y, w,h);
        else {
            g.setColor(g.BLACK);
            g.fillOval(x,y,w,h);
        }*/
	}
	
	public void drawPirate(AGraphics g, IVector2D v) {
		g.setColor(g.BLACK);
		drawFaces(g, v, 0, getBoard().getTileWidth()/7, FaceType.WAR_SHIP, false);
		/*
	    //float [] v = {0,0};
	    //g.transform(board.getCellWidth(), board.getCellHeight(), v);
		g.begin();
	    float sx = board.getTileWidth() / bw * (getWidth()-padding);
	    float sy = board.getTileHeight() / bh * (getHeight()-padding);
        int w = Math.round(sx) / 2;
        int h = Math.round(sy) / 2;
        MutableVector2D V = g.transform(v);
        int x = Math.round(V.X()) - w/2;
        int y = Math.round(V.Y()) - h/2;
        //int x = Math.round(sx * cell.getX()) - w/2;
        //int y = Math.round(sy * cell.getY()) - h/2;
        if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES))
            g.drawImage(g, pirateImage, x, y, w,h);
        else {
            g.setColor(g.BLACK);
            g.fillOval(x,y,w,h);
        }*/
	}

	/**
	 * Set the current pick mode.
	 *  
	 * @param mode
	 * @param indices set of acceptable indices or null to pick all indices
	 */
	void setPickHandler(PickHandler handler) {
		if (handler == null) {
			this.pickMode = PickMode.PM_NONE;
		} else {
			this.pickMode = handler.getPickMode();
		}
		this.pickHandler = handler;
		pickedValue = -1;
		postInvalidate();
	}
	
	public void drawIslandOutlined(AGraphics g, int tileIndex) {
		Collection<Integer> islandEdges = board.findIslandShoreline(tileIndex);
		g.begin();
    	for (int eIndex : islandEdges) {
    		renderEdge(g, board.getRoute(eIndex));
    	}
    	g.drawLines(5);

    	Tile cell = board.getTile(tileIndex);
    	if (cell.getIslandNum() > 0) {
    		drawIslandInfo(g, getBoard().getIsland(cell.getIslandNum()));
    	}
	}
	
	public void drawIslandInfo(AGraphics g, Island i) {
		g.begin();
		g.setColor(g.BLUE);
    	for (int eIndex : i.getShoreline()) {
    		renderEdge(g, board.getRoute(eIndex));
    	}
    	g.drawLines(5);
    	MutableVector2D midpoint = new MutableVector2D();
    	int num = 0;
    	for (int eIndex : i.getShoreline()) {
    		midpoint.addEq(board.getRouteMidpoint(board.getRoute(eIndex)));
    		num++;
    	}
    	midpoint.scaleEq(1.0f / num);
    	g.transform(midpoint);
    	String txt = "ISLAND\n" + i.getNum();
    	Dimension dim = AWTUtils.computeTextDimension(g, txt);
    	g.setColor(new AColor(0,0,0,0.5f));
    	int x = midpoint.Xi() - (dim.width/2+5);
    	int y = midpoint.Yi() - (dim.height/2+5);
    	int w = dim.width + 10;
    	int h = dim.height + 10;
    	g.fillRect(x, y, w, h);
    	g.setColor(g.white);
    	AWTUtils.drawJustifiedString(g, midpoint.Xi(), midpoint.Yi(), Justify.CENTER, Justify.CENTER, txt);
	}
	
    private void drawTilesOutlined(AGraphics g) {
        AColor outlineColor = getProperties().getColorProperty("board.outlineColor", g.WHITE);
        AColor textColor = getProperties().getColorProperty("board.textcolor", g.CYAN);
        
        float [] v = {0,0};
        
        for (int i=0; i <board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            g.transform(cell.getX(), cell.getY(), v);
            int x = Math.round(v[0]);
            int y = Math.round(v[1]);
            g.setColor(outlineColor);
            drawTileOutline(g, cell, 2);
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
    
    private void drawTilesTextured(AGraphics g) {
        float cellW = board.getTileWidth();
        float cellH = board.getTileHeight();
        int dim = Math.min(getWidth(), getHeight());
        int w = Math.round(cellW / bw * (dim-padding));
        int h = Math.round(cellH / bh * (dim-padding));
        float [] v = {0,0};

        AColor outlineColor = (g.WHITE);
        AColor textColor = (g.CYAN);

        if (bold == null)
            bold = g.getFont().deriveFont(Font.BOLD);
        g.setFont(bold);
        for (int i=0; i <board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            g.transform(cell.getX(), cell.getY(), v);
            int x = Math.round(v[0]);
            int y = Math.round(v[1]);
            switch (cell.getType()) {
            case NONE:
                g.setColor(outlineColor);
                drawTileOutline(g, cell, 2);
                //Utils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"NONE");
                break;
            case DESERT:
                g.drawImage(g, desertImage, x-w/2, y-h/2, w, h);
                break;
            case WATER:
                g.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                break;
            case PORT_WHEAT:
            case PORT_WOOD:
            case PORT_BRICK:
            case PORT_ORE:
            case PORT_SHEEP:
                g.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"2:1\n" + cell.getResource().name());
                break;
            case PORT_MULTI:
                g.drawImage(g, waterImage, x-w/2, y-h/2, w, h);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"3:1\n?");
                break;
            case GOLD:
            	g.drawImage(g, goldImage, x-w/2, y-h/2, w, h);
            	break;

            case UNDISCOVERED:
            	g.drawImage(g, undiscoveredImage, x-w/2, y-h/2, w, h);
            	break;

            // used for random generation
            case RANDOM_RESOURCE_OR_DESERT:
                g.setColor(outlineColor);
                drawTileOutline(g, cell, 2);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResourse or\nDesert");
                break;
            case RANDOM_RESOURCE:
                g.setColor(outlineColor);
                drawTileOutline(g, cell, 2);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nResource");
                break;
            case RANDOM_PORT_OR_WATER:
                g.setColor(outlineColor);
                drawTileOutline(g, cell, 2);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random Port\nor\nWater");
                break;
            case RANDOM_PORT:
                g.setColor(outlineColor);
                drawTileOutline(g, cell, 2);
                g.setColor(textColor);
                AWTUtils.drawJustifiedString(g,x,y,Justify.CENTER,Justify.CENTER,"Random\nPort");
                break;
            case FIELDS:
            	g.drawImage(g, fieldshexImage, x-w/2, y-h/2, w, h);
				break;
			case FOREST:
				g.drawImage(g, foresthexImage, x-w/2, y-h/2, w, h);
				break;
			case HILLS:
				g.drawImage(g, hillshexImage, x-w/2, y-h/2, w, h);
				break;
			case MOUNTAINS:
				g.drawImage(g, mountainshexImage, x-w/2, y-h/2, w, h);
				break;
			case PASTURE:
				g.drawImage(g, pastureshexImage, x-w/2, y-h/2, w, h);
				break;                
            }
            
            if (cell.getDieNum() > 0) {
            	drawCellProductionValue(g, x, y, cell.getDieNum());
            }
        }   
        
        
    }

    public void drawCellProductionValue(AGraphics g, int x, int y, int num) {
        g.setColor(g.BLACK);
        AWTUtils.fillCircle(g, x, y+1, 20);
        g.setColor(g.CYAN);
        AWTUtils.drawJustifiedString(g, x, y, Justify.CENTER, Justify.CENTER, String.valueOf(num));
    }
    
    private void computeBoardRect() {
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
	public void paint(AGraphics g) {
	    
        try {
    	    if (getWidth() <= padding+5 || getHeight() <= padding+5)
    	        return;
    	    
    	    long enterTime = System.currentTimeMillis();
    	    
    	    computeBoardRect();
    		g.setColor(bkColor);
    		g.fillRect(0,0,getWidth(),getHeight());
    		//float xs = (float)getWidth();
    		//float ys = (float)getHeight();
    		render.setOrtho(0, getWidth(), 0, getHeight());
    		render.makeIdentity();
    		render.translate(padding, padding);
    		float dim = Math.min(getWidth(),  getHeight()); // TODO: keep aspect ratio and center
    		render.translate((getWidth()-dim)/2, (getHeight()-dim)/2);
    		render.scale(1f/bw, 1f/bh);
    		render.scale(dim-2*padding, dim-2*padding);
            render.translate(-bx, -by);
            //render.translate(bx, by);
            if (!getRenderFlag(RenderFlag.DONT_DRAW_TEXTURES)) {
                drawTilesTextured(g);
            } 
            
            if (getRenderFlag(RenderFlag.DRAW_CELL_OUTLINES)) {
                drawTilesOutlined(g);
            }

            if (pickMode == PickMode.PM_TILE) {
                for (int i=0; i<board.getNumTiles(); i++) {
                	if (pickHandler.isPickableIndex(this, i)) {
                		if (i == pickedValue) {
                			pickHandler.onHighlighted(this, render, g, i);
                		} else {
                			pickHandler.onDrawPickable(this, render, g, i);
                		}
                	} 
                }
            }
            
            if (!getRenderFlag(RenderFlag.DONT_DRAW_ROADS)) {
        		// draw the roads
                for (int i=0; i<board.getNumRoutes(); i++) {
                	if (pickMode == PickMode.PM_EDGE) {
                    	if (pickHandler.isPickableIndex(this, i)) {
                    		if (i == pickedValue) {
                    			pickHandler.onHighlighted(this, render, g, i);
                    		} else {
                    			pickHandler.onDrawPickable(this, render, g, i);
                    		}
                    		continue;
                    	} 
                    }
                	Route e = board.getRoute(i);
        			g.setColor(getPlayerColor(e.getPlayer()));
        			drawEdge(g, e, e.getType(), e.getPlayer(), false);
        		}
            }
    		
    		// draw the structures
            if (!getRenderFlag(RenderFlag.DONT_DRAW_STRUCTURES)) {
        		for (int i=0; i<board.getNumVerts(); i++) {
        			if (pickMode == PickMode.PM_VERTEX) {
        				if (pickHandler.isPickableIndex(this, i)) {
                    		if (i == pickedValue) {
                    			pickHandler.onHighlighted(this, render, g, i);
                    		} else {
                    			pickHandler.onDrawPickable(this, render, g, i);
                    		}
                    		continue;
        				}
        			}
        			Vertex v = board.getVertex(i);
        			drawVertex(g, v, v.getType(), v.getPlayer(), false);
        		}
            }
            
            if (pickMode == PickMode.PM_CUSTOM) {
            	CustomPickHandler handler = (CustomPickHandler)pickHandler;
            	for (int i=0; i<handler.getNumElements(); i++) {
            		if (i == pickedValue) {
            			handler.onHighlighted(this, render, g, i);
            		} else {
            			handler.onDrawPickable(this, render, g, i);
            		}
            	}
            }
    		
    		int robberTile = board.getRobberTileIndex();
    		int pirateTile = board.getPirateTileIndex();
    		int merchantTile = board.getMerchantTileIndex();
    		int merchantPlayer = board.getMerchantPlayer();
    		
            if (pickedValue >= 0) {
            	pickHandler.onHighlighted(this, render, g, pickedValue);
            }

    		if (robberTile >= 0)
    		    drawRobber(g, board.getTile(robberTile));
    		if (pirateTile >= 0)
    			drawPirate(g, board.getTile(pirateTile));
    		if (merchantTile >= 0)
    			drawMerchant(g, board.getTile(merchantTile), merchantPlayer);
    		
    		if (pickMode != PickMode.PM_NONE)
    			pickHandler.onDrawOverlay(this, render, g);
    		
    		
    		{
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
    		}
    		
    		if (getRenderFlag(RenderFlag.DRAW_CELL_CENTERS)) {
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        g.vertex(c.getX(), c.getY());
    		    }
    		    g.setColor(g.yellow);
    		    render.drawPoints(g, 8);
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_CELLS)) {
    		    g.setColor(g.RED);
    		    float [] v = {0,0};
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        g.transform(c.getX(), c.getY(), v);
    		        AWTUtils.drawJustifiedString(g, Math.round(v[0]), Math.round(v[1])+5, Justify.CENTER, Justify.TOP, String.valueOf(i));
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_VERTS)) {
                g.setColor(g.WHITE);
                float []v = {0,0};
                for (int i=0; i<board.getNumVerts(); i++) {
                    Vertex vx = board.getVertex(i);
                    g.transform(vx.getX(), vx.getY(), v);
                    AWTUtils.drawJustifiedString(g, Math.round(v[0]), Math.round(v[1]), Justify.CENTER, Justify.TOP, String.valueOf(i));
                }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_EDGES)) {
    		    g.setColor(g.yellow);
    		    for (int i=0; i<board.getNumRoutes(); i++) {
    		        Route e = board.getRoute(i);
    		        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(e));
    		        g.transform(m);
    		        AWTUtils.drawJustifiedString(g, Math.round(m.getX()), Math.round(m.getY()), Justify.CENTER, Justify.TOP, String.valueOf(i));
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.SHOW_ISLAND_INFO)) {
    			for (Island i : getBoard().getIslands()) {
    				drawIslandInfo(g, i);
    			}
    		}

    		if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
    		    if (cellInfoIndex >= 0) {
    		        this.drawTileInfo(g, cellInfoIndex);
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
                postInvalidate();
    		}
    		
    		// notify anyone waiting on me
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
	public void mouseDragged(MouseEvent ev) {
		mouseMoved(ev);
		mouseClicked(ev);
	}

	@Override
	public void mouseMoved(MouseEvent ev) {
		int index = -1;
		switch (pickMode) {
    		case PM_NONE: 
    		    break;
    		case PM_EDGE:
    			index = pickEdge(ev.getX(), ev.getY());
    			break;
    		case PM_VERTEX:
    		    index = pickVertex(ev.getX(), ev.getY());
    			break;
    		case PM_TILE:
    		    index = pickTile(ev.getX(), ev.getY());
    			break;
    		case PM_CUSTOM:
    			index = ((CustomPickHandler)pickHandler).pickElement(render, ev.getX(), ev.getY());
    			break;
		}
		
		if (index >= 0 && pickHandler.isPickableIndex(this, index)) {
			pickedValue = index;
		} else {
			pickedValue = -1;
		}

        if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
            cellInfoIndex = pickTile(ev.getX(), ev.getY());
        }

        if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
            edgeInfoIndex = pickEdge(ev.getX(), ev.getY());
        }

        if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
            vertexInfoIndex = pickVertex(ev.getX(), ev.getY());
        }
        
		postInvalidate();
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
        	pickHandler.onPick(this, pickedValue);
            postInvalidate();
            pickedValue = -1;
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
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    }

    public void setBoard(Board board) {
        this.board = board;
    }

    private void drawInfo(AGraphics g, int x, int y, String info) {
        String [] lines = info.split("\n");
        int maxW = 0;
        for (int i=0; i<lines.length; i++) {
            maxW = Math.max(maxW, (int)g.getTextWidth(lines[i]));
        }
        final int padding = 5;
        int width = maxW + padding*2;
        int height = lines.length * g.getTextHeight() + 2*padding;
        g.setColor(g.DARK_GRAY.setAlpha(180.0f/255));
        g.drawFilledRect(x, y, width, height);
        g.setColor(g.WHITE);
        y -= 2; // slight visual adjustment to center vertically
        
        // make sure we dont draw off screen
        if (x + width > getWidth()) {
            x = getWidth() - width;
        }
        if (y + height > getHeight()) {
            y = getHeight() - height;
        }
        
        for (int i=0; i<lines.length; i++) {
            y += g.getTextHeight();
            g.drawString(lines[i], x+padding, y+padding);
        }
    }
    
    private void drawTileInfo(AGraphics g, int cellIndex) {
        if (cellIndex < 0)
            return;
        Tile cell = board.getTile(cellIndex);
        String info = "CELL " + cellIndex + "\n  " + cell.getType() + "\nadj:" + cell.getAdjVerts();
        if (cell.getResource() != null) {
            info += "\n  " + cell.getResource();
        }
        if (cell.getIslandNum() > 0) {
        	info += "\n  Island " + cell.getIslandNum();
        }
        
        float [] v = {0,0};
        g.transform(cell.getX(), cell.getY(), v);
        int x = Math.round(v[0]);
        int y = Math.round(v[1]);

        drawInfo(g, x, y, info);
    }
    
    private void drawEdgeInfo(AGraphics g, int edgeIndex) {
        if (edgeIndex < 0)
            return;
        Route edge = board.getRoute(edgeIndex);
        String info = "EDGE " + edgeIndex;
        if (edge.getPlayer() > 0)
            info += "\n  Player " + edge.getPlayer();
        info += "\n  " + edge.getFlagsString();
        info += "\n  ang=" + getEdgeAngle(edge);
        info += "\n  tiles=" + edge.getTile(0) + "/" + edge.getTile(1);
        
        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(edge));
        g.transform(m);
        
        drawInfo(g, Math.round(m.getX()), Math.round(m.getY()), info);
    }
    
    private void drawVertexInfo(AGraphics g, int vertexIndex) {
        if (vertexIndex < 0)
            return;
        Vertex vertex = board.getVertex(vertexIndex);
        String info = "VERTEX " + vertexIndex;
        if (vertex.isAdjacentToWater())
        	info += " WAT";
        if (vertex.isAdjacentToLand())
        	info += " LND";
        if (vertex.getPlayer() > 0) {
            info += "\n  Player " + vertex.getPlayer() + "\n  " + (vertex.isCity() ? 
                    "City +2" : 
                    "Settlement +1");
        } else {
            int pNum = board.checkForPlayerRouteBlocked(vertexIndex);
            info += "\n  Blocks player " + pNum + "'s roads";
        }
        float [] v = { vertex.getX(), vertex.getY() };
        g.transform(v);
        
        drawInfo(g, Math.round(v[0]), Math.round(v[1]), info);
    }

}
