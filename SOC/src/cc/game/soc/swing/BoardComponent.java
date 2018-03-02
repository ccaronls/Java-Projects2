package cc.game.soc.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.*;

import javax.swing.JComponent;

import cc.game.soc.core.*;
import cc.lib.game.*;
import cc.lib.math.*;
import cc.lib.swing.AWTRenderer;
import cc.lib.swing.AWTUtils;
import cc.lib.swing.ImageMgr;

@SuppressWarnings("serial")
public abstract class BoardComponent extends JComponent implements MouseMotionListener, MouseListener, Renderable {

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
    	void onPick(BoardComponent bc, int pickedValue);
    	
    	/**
    	 * Called when rendering an index that passes the isPickableIndex test
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param index
    	 */
    	void onDrawPickable(BoardComponent bc, AWTRenderer r, Graphics g, int index);
    	
    	/**
    	 * Called after tiles, edges and verts are rendered for pick handler to render it own stuff
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param highlightedIndex
    	 */
    	void onDrawOverlay(BoardComponent bc, AWTRenderer r, Graphics g);
    	
    	/**
    	 * Render a highlighted index
    	 * @param bc
    	 * @param r
    	 * @param g
    	 * @param highlightedIndex
    	 */
    	void onHighlighted(BoardComponent bc, AWTRenderer r, Graphics g, int highlightedIndex);
    	
    	/**
    	 * Return whether the index is pickable
    	 * @param bc
    	 * @param index
    	 * @return
    	 */
    	boolean isPickableIndex(BoardComponent bc, int index);
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
		 *      render.addVertex(...)
		 *      
		 *   return r.pickPoints(10);
		 *   
		 * @param render
		 * @param x
		 * @param y
		 * @return
		 */
		int pickElement(AWTRenderer render, int x, int y);
    	
    }

    private Board board;
	
	Board getBoard() {
	    return board;
	}
	
	protected abstract GUIProperties getProperties();
	
	protected abstract Color getPlayerColor(int playerNum);
	
	
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
	final Color outlineColor;

	private PickMode pickMode = PickMode.PM_NONE;
	private int pickedValue = -1;
	private PickHandler pickHandler = null;
    final int padding;
	
    private int renderFlag = 0;
    private List<AAnimation<Graphics>> animations = new ArrayList<>(32);
    
    private int edgeInfoIndex = -1;
    private int cellInfoIndex = -1;
    private int vertexInfoIndex = -1;
    
    private int [] knightImages = new int[6];
    
	BoardComponent(Board board, ImageMgr images) {
		this.images = images;
		
		setFocusable(true);
	    renderFlag          = getProperties().getIntProperty("board.renderFlag", 0);
        bkColor             = getProperties().getColorProperty("board.bkcolor", Color.LIGHT_GRAY);
        roadLineThickness   = getProperties().getIntProperty("board.roadLineThickness", 4);
        padding             = getProperties().getIntProperty("board.padding", 20);
        outlineColor        = getProperties().getColorProperty("board.outline.color", Color.BLACK);
		this.board = board;
        addMouseMotionListener(this);
        addMouseListener(this);
        images.addSearchPath("images");
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
        getProperties().setProperty("board.renderFlag", renderFlag);
        repaint();
    }
    
    boolean getRenderFlag(RenderFlag flag) {
        return (renderFlag & (1 << flag.ordinal())) != 0;
    }
    
    void addAnimation(GAnimation anim, boolean block) {
        synchronized (animations) {
            animations.add(anim);
        }
        anim.start();
        repaint();
        if (block) {
            synchronized (anim) {
                try {
                    anim.wait(anim.getDuration() + 500);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

	public void drawTileOutline(Graphics g, Tile cell, int borderThickness) {
	    render.clearVerts();
		for (int i : cell.getAdjVerts()) {
			Vertex v = board.getVertex(i);
			render.addVertex(v.getX(), v.getY());
		}
		render.drawLineLoop(g, borderThickness);
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
		final Color added;
		FaceType [] structures;
		
		Face(float darkenAmount, int ... verts) {
			this(AWTUtils.TRANSPARENT, darkenAmount, verts);
		}
		
		Face(Color added, float darkenAmount, int ... verts) {
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
	    new Face(new Color(255,255,255,0), 0.8f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),
	    // hilt
	    new Face(new Color(255,255,255,0), 0.8f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),

	    // SAA lightened for active
	    // blade
	    new Face(new Color(160,160,160,0), 0.1f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    // hilt
	    new Face(new Color(100,100,100,0), 0.1f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    
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
	
	float getStructureRadius() {
		return board.getTileWidth()/(6*3);
	}
	
	void drawSettlement(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.SETTLEMENT, outline);
	}
	
	void drawCity(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.CITY, outline);
	}
	
	void drawWalledCity(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.CITY_WALL, outline);
	}
	
	void drawMetropolisTrade(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_TRADE, outline);
	}

	void drawMetropolisPolitics(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_POLITICS, outline);
	}

	void drawMetropolisScience(Graphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_SCIENCE, outline);
	}

	void drawMerchant(Graphics g, Tile t, int playerNum) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, t, 0, getStructureRadius(), FaceType.MERCHANT, false);
		g.setColor(Color.WHITE);
		String txt = "\n2:1\n" + t.getResource().name();
		Vector2D v = render.transformXY(t);
		
		AWTUtils.drawJustifiedString(g, v.Xi()-2, v.Yi()-2-AWTUtils.getFontHeight(g)*2, Justify.CENTER, Justify.TOP, txt);
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
	
	float getKnightRadius() {
		return board.getTileWidth()*2/(8*3*3);
	}
	
	void drawKnight(Graphics g, IVector2D pos, int playerNum, int level, boolean active, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		FaceType structure = null;
		switch (level) {
			case 0:
			case 1: structure = active ? FaceType.KNIGHT_ACTIVE_BASIC : FaceType.KNIGHT_INACTIVE_BASIC; break;
			case 2: structure = active ? FaceType.KNIGHT_ACTIVE_STRONG : FaceType.KNIGHT_INACTIVE_STRONG; break;
			case 3: structure = active ? FaceType.KNIGHT_ACTIVE_MIGHTY : FaceType.KNIGHT_INACTIVE_MIGHTY; break;
			default: assert(false); break;
		}
		float radius = getKnightRadius();
		drawFaces(g, pos, 0, radius, structure, outline);
	}
	
	void drawCircle(Graphics g, IVector2D pos) {
		render.pushMatrix();
		render.clearVerts();
		render.translate(pos);
		int angle = 0;
		float rad = board.getTileWidth()/5;
		render.scale(rad, rad);
		int pts = 10;
		for (int i=0; i<pts; i++) {
			render.addVertex(CMath.cosine(angle), CMath.sine(angle));
			angle += 360/pts;
		}
		render.drawLineLoop(g, 2);
		render.popMatrix();
	}
	
	void drawPirateFortress(Graphics g, Vertex v, boolean outline) {
		MutableVector2D mv = render.transformXY(v);
		int x = mv.Xi()-10;
		int y = mv.Yi()-10;
		for (int i=0; i<v.getPirateHealth(); i++) {
			int rw = 40;
			int rh = 30;
			g.setColor(Color.GRAY);
			g.fillOval(x,y,rw,rh);
			g.setColor(Color.RED);
			g.drawOval(x,y,rw,rh);
			x += 10;
			y += 5;
		}
		g.setColor(Color.GRAY);
		drawFaces(g, v, 0, getStructureRadius(), FaceType.PIRATE_FORTRESS, outline);
	}

	void drawFaces(Graphics g, IVector2D pos, float angle, float radius, FaceType structure, boolean outline) {
	    //final float xRad = 3; // actual radius as defined above
		//float scale = radius / xRad;
		drawFaces(g, pos, angle, radius, radius, structure, outline);
	}

	private HashMap<FaceType, Face[]> faceMap = new HashMap<BoardComponent.FaceType, BoardComponent.Face[]>();
	
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
	
	void drawFaces(Graphics g, IVector2D pos, float angle, float w, float h, FaceType structure, boolean outline) {
		render.pushMatrix();
		render.translate(pos);
		render.rotate(angle);
		render.scale(w, -h);
		Color saveColor = g.getColor();

		Face [] faces = getStructureFaces(structure);
	    for (Face face : faces) {
		    render.clearVerts();
	    	for (int i=0; i<face.numVerts; i++)
	    		render.addVertex(face.xVerts[i], face.yVerts[i]);
	    	
	    	if (outline) {
	    	    g.setColor(outlineColor);
	    	    render.drawLineLoop(g, 2);
	    	}

	    	Color c = AWTUtils.addColors(face.added, AWTUtils.darken(saveColor, face.darkenAmount));
            g.setColor(c);
            render.fillPolygon(g);
	    }
	    g.setColor(saveColor);
	    render.popMatrix();
	}
	
	private int pickEdge(int mouseX, int mouseY) {
		render.clearVerts();
		for (int index=0; index<getBoard().getNumRoutes(); index++) {
			render.setName(index);
			renderEdge(board.getRoute(index));
		}
		return render.pickLines(mouseX, mouseY, this.roadLineThickness*2);
	}
	
	private int pickVertex(int mouseX, int mouseY) {
		render.clearVerts();
		for (int index=0; index<board.getNumVerts(); index++) {
			render.setName(index);
			Vertex v = board.getVertex(index);
			render.addVertex(v);
		}
		return render.pickPoints(mouseX, mouseY, 10);
	}
	
	private int pickTile(int mouseX, int mouseY) {
		render.clearVerts();
		final int dim = Math.round(board.getTileWidth() * getWidth());
		for (int index=0; index<board.getNumTiles(); index++) {
			render.setName(index);
			Tile cell = board.getTile(index);
			render.addVertex(cell);
		}
		return render.pickPoints(mouseX, mouseY, dim);
	}
	
	private void renderEdge(Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		render.addVertex(v0);
		render.addVertex(v1);
	}
	
	private void renderDamagedEdge(Route e) {
		Vertex v0 = board.getVertex(e.getFrom());
		Vertex v1 = board.getVertex(e.getTo());
		render.clearVerts();
		render.addVertex(v0);
		Vector2D v = board.getRouteMidpoint(e);
		render.addVertex(v);
		Vector2D dv = Vector2D.newTemp(v).subEq(v0).normEq().addEq(v);
		render.addVertex(dv);
	}
	
	public void drawDamagedRoad(Graphics g, Route e, boolean outline) {
		render.clearVerts();
	    if (outline) {
	        Color old = g.getColor();
	        g.setColor(outlineColor);
            renderDamagedEdge(e);
            render.drawLines(g, roadLineThickness+2);
            g.setColor(old);
	    } 
	    renderDamagedEdge(e);
	    render.drawLineStrip(g, roadLineThickness);	
	}

	public void drawEdge(Graphics g, Route e, RouteType type, int playerNum, boolean outline) {
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
	
	public void drawVertex(Graphics g, Vertex v, VertexType type, int playerNum, boolean outline) {
		switch (type) {
			case OPEN:
				break;
			case PIRATE_FORTRESS:
				drawPirateFortress(g, v, outline);
				break;
			case OPEN_SETTLEMENT:
				g.setColor(Color.LIGHT_GRAY);
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
	
	public void drawRoad(Graphics g, Route e, boolean outline) {
		render.clearVerts();
	    if (outline) {
	        Color old = g.getColor();
	        g.setColor(outlineColor);
            renderEdge(e);
            render.drawLines(g, roadLineThickness+2);
            g.setColor(old);
	    } 
	    renderEdge(e);
	    render.drawLineStrip(g, roadLineThickness);
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
	
	float getShipRadius() {
		return board.getTileWidth()/(8*3);
	}
	
	float getRobberRadius() {
		return getBoard().getTileWidth()/(7*3);
	}
	
	void drawShip(Graphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawFaces(g, mp, getEdgeAngle(e), getShipRadius(), FaceType.SHIP, outline);
	}
	
	void drawShip(Graphics g, IVector2D v, int angle, boolean outline) {
		drawFaces(g, v, angle, getShipRadius(), FaceType.SHIP, outline);
	}
	
	void drawWarShip(Graphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawFaces(g, mp, getEdgeAngle(e), getShipRadius(), FaceType.WAR_SHIP, outline);
	}
	
	public void drawRobber(Graphics g, Tile cell) {
		g.setColor(Color.LIGHT_GRAY);
		drawFaces(g, cell, 0, getRobberRadius(), FaceType.ROBBER, false);
	}
	
	public void drawPirate(Graphics g, IVector2D v) {
		g.setColor(Color.BLACK);
		drawFaces(g, v, 0, getRobberRadius(), FaceType.WAR_SHIP, false);
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
		repaint();
	}
	
	public void drawIslandOutlined(Graphics g, int tileIndex) {
		Collection<Integer> islandEdges = board.findIslandShoreline(tileIndex);
		render.clearVerts();
    	for (int eIndex : islandEdges) {
    		renderEdge(board.getRoute(eIndex));
    	}
    	render.drawLines(g, 5);

    	Tile cell = board.getTile(tileIndex);
    	if (cell.getIslandNum() > 0) {
    		drawIslandInfo(g, getBoard().getIsland(cell.getIslandNum()));
    	}
	}
	
	public void drawIslandInfo(Graphics g, Island i) {
		render.clearVerts();
		g.setColor(Color.BLUE);
    	for (int eIndex : i.getShoreline()) {
    		renderEdge(board.getRoute(eIndex));
    	}
    	render.drawLines(g, 5);
    	MutableVector2D midpoint = new MutableVector2D();
    	int num = 0;
    	for (int eIndex : i.getShoreline()) {
    		midpoint.addEq(board.getRouteMidpoint(board.getRoute(eIndex)));
    		num++;
    	}
    	midpoint.scaleEq(1.0f / num);
    	render.transformXY(midpoint);
    	String txt = "ISLAND\n" + i.getNum();
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
	
    private void drawTilesOutlined(Graphics g) {
        Color outlineColor = getProperties().getColorProperty("board.outlineColor", Color.WHITE);
        Color textColor = getProperties().getColorProperty("board.textcolor", Color.CYAN);
        
        float [] v = {0,0};
        
        for (int i=0; i <board.getNumTiles(); i++) {
            Tile cell = board.getTile(i);
            render.transformXY(cell.getX(), cell.getY(), v);
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
    
    public final static int TILE_CELL_NUM_RADIUS = 20;
    
    private void drawTilesTextured(Graphics g) {
        float cellW = board.getTileWidth();
        float cellH = board.getTileHeight();
        int dim = Math.min(getWidth(), getHeight());
        int w = Math.round(cellW / bw * (dim-padding));
        int h = Math.round(cellH / bh * (dim-padding));
        float [] v = {0,0};

        Color outlineColor = getProperties().getColorProperty("board.outlineColor", Color.WHITE);
        Color textColor = getProperties().getColorProperty("board.textcolor", Color.CYAN);

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
                drawTileOutline(g, cell, 2);
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
            case GOLD:
            	images.drawImage(g, goldImage, x-w/2, y-h/2, w, h);
            	break;

            case UNDISCOVERED:
            	images.drawImage(g, undiscoveredImage, x-w/2, y-h/2, w, h);
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
            	drawCellProductionValue(g, x, y, cell.getDieNum(), TILE_CELL_NUM_RADIUS);
            }
        }   
        
        
    }

    public void drawCellProductionValue(Graphics g, int x, int y, int num, int radius) {
        g.setColor(Color.BLACK);
        AWTUtils.fillCircle(g, x, y+1, radius);
        g.setColor(Color.CYAN);
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
	public void paint(Graphics g) {
	    
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
        		List<AAnimation<Graphics>> t = null;
        		synchronized (animations) {
        		    t = new ArrayList<>(animations);
        		    animations.clear();
        		}
    		
        		// draw animations
        		for (int i=0; i<t.size(); ) {
        		    AAnimation<Graphics> anim = t.get(i);
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
    		        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(e));
    		        render.transformXY(m);
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
                repaint();
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
        	pickHandler.onPick(this, pickedValue);
            repaint();
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
    
    private void drawTileInfo(Graphics g, int cellIndex) {
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
        info += "\n  ang=" + getEdgeAngle(edge);
        info += "\n  tiles=" + edge.getTile(0) + "/" + edge.getTile(1);
        
        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(edge));
        render.transformXY(m);
        
        drawInfo(g, Math.round(m.getX()), Math.round(m.getY()), info);
    }
    
    private void drawVertexInfo(Graphics g, int vertexIndex) {
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
        render.transformXY(v);
        
        drawInfo(g, Math.round(v[0]), Math.round(v[1]), info);
    }
}




