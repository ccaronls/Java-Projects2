package cc.game.soc.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import cc.game.soc.core.Board;
import cc.game.soc.core.Island;
import cc.game.soc.core.Route;
import cc.game.soc.core.RouteType;
import cc.game.soc.core.Tile;
import cc.game.soc.core.Vertex;
import cc.game.soc.core.VertexType;
import cc.lib.game.AAnimation;
import cc.lib.game.AGraphics;
import cc.lib.game.APGraphics;
import cc.lib.game.GColor;
import cc.lib.game.GDimension;
import cc.lib.game.IVector2D;
import cc.lib.game.Justify;
import cc.lib.game.Utils;
import cc.lib.math.CMath;
import cc.lib.math.MutableVector2D;
import cc.lib.math.Vector2D;
import cc.lib.ui.UIComponent;

public class UIBoardRenderer extends UIRenderer {

    public Board board = null;

    public UIBoardRenderer(UIComponent component) {
        super(component);
    }

	protected GColor getPlayerColor(int playerNum) {
        return UISOC.getInstance().getPlayerColor(playerNum);
    }
	
	private int desertImage;
    private int waterImage;
    private int goldImage;
    private int undiscoveredImage;
    private int foresthexImage;
    private int hillshexImage;
    private int mountainshexImage;
    private int pastureshexImage;
    private int fieldshexImage;
    private int [] knightImages;

    private GColor outlineColorDark = GColor.BLACK;
    private GColor outlineColorLight = GColor.WHITE;
    private GColor textColor = GColor.CYAN;

	private PickMode pickMode = PickMode.PM_NONE;
	private int pickedValue = -1;
	private PickHandler pickHandler = null;
	
    public int renderFlag = 0;
    private LinkedList<AAnimation<AGraphics>> animations = new LinkedList<>();

    private int edgeInfoIndex = -1;
    private int cellInfoIndex = -1;
    private int vertexInfoIndex = -1;

    public void initImages(int desertImage, int waterImage, int goldImage, int undiscoveredImage, int foresthexImage, int hillshexImage, int mountainshexImage, int pastureshexImage, int fieldshexImage,
                           int knightBasicInactive, int knightBasicActive, int knightStrongInactive, int knightStrongActive, int knightMightlyInactive, int knightMightlyActive) {
        this.desertImage = desertImage;
        this.waterImage = waterImage;
        this.goldImage = goldImage;
        this.undiscoveredImage = undiscoveredImage;
        this.foresthexImage = foresthexImage;
        this.hillshexImage = hillshexImage;
        this.mountainshexImage = mountainshexImage;
        this.pastureshexImage = pastureshexImage;
        this.fieldshexImage = fieldshexImage;
        this.knightImages = new int[]{
                knightBasicInactive,
                knightBasicActive,
                knightStrongInactive,
                knightStrongActive,
                knightMightlyInactive,
                knightMightlyActive
        };
    }

    public final PickHandler getPickHandler() {
        return pickHandler;
    }

    public final void setRenderFlag(RenderFlag flag, boolean enabled) {
        if (enabled)
            renderFlag |= (1 << flag.ordinal());
        else
            renderFlag &= ~(1 << flag.ordinal());
        //getProperties().setProperty("renderFlag", renderFlag);
        getComponent().redraw();
    }
    
    public final boolean getRenderFlag(RenderFlag flag) {
        return (renderFlag & (1 << flag.ordinal())) != 0;
    }

    public final void addAnimation(AAnimation<AGraphics> anim, boolean block) {
        addAnimation(false, anim, block);
    }

    public final void addAnimation(boolean front, AAnimation<AGraphics> anim, boolean block) {
        synchronized (animations) {
            if (front)
                animations.addFirst(anim);
            else
                animations.addLast(anim);
        }
        getComponent().redraw();
        if (!anim.isStarted())
            anim.start();
        if (block) {
            Utils.waitNoThrow(anim, anim.getDuration()+500);
        }
    }

    public final Board getBoard() {
        if (UISOC.getInstance() == null)
            return board;

        return UISOC.getInstance().getBoard();
    }

    public final void drawTileOutline(AGraphics g, Tile cell, float thickness) {
        g.begin();
        for (int i : cell.getAdjVerts()) {
            Vertex v = getBoard().getVertex(i);
            g.vertex(v.getX(), v.getY());
        }
        g.drawLineLoop(thickness);
    }

	public final void drawTileOutline(AGraphics g, Tile cell) {
        drawTileOutline(g, cell, RenderConstants.thinLineThickness);
	}

    public final void startTilesInventedAnimation(final Tile tile0, final Tile tile1) {
        final int NUM_PTS = 30;

        final int t1 = tile1.getDieNum();
        final int t0 = tile0.getDieNum();
        final Vector2D [] pts0 = new Vector2D[NUM_PTS+1];
        final Vector2D [] pts1 = new Vector2D[NUM_PTS+1];
        final Vector2D mid = Vector2D.newTemp(tile0).add(tile1).scaleEq(0.5f);
        final Vector2D d = Vector2D.newTemp(mid).sub(tile0).scaleEq(0.5f);
        final Vector2D mid0 = Vector2D.newTemp(tile0).add(d);
        final Vector2D mid1 = Vector2D.newTemp(tile1).sub(d);
        final MutableVector2D dv = d.norm().scaleEq(0.7f);
        Utils.computeBezierCurvePoints(pts0, NUM_PTS, tile0, mid0.add(dv), mid1.add(dv), tile1);
        pts0[NUM_PTS]=pts0[NUM_PTS-1];
        Utils.computeBezierCurvePoints(pts1, NUM_PTS, tile1, mid1.sub(dv), mid0.sub(dv), tile0);
        pts1[NUM_PTS]=pts1[NUM_PTS-1];
        tile0.setDieNum(0);
        tile1.setDieNum(0);

        addAnimation(new UIAnimation(3000) {

            public void drawChit(AGraphics g, Vector2D [] pts, float position, int num) {
                int index0 = (int)(position*(NUM_PTS-2));
                int index1 = index0+1;
                float delta = ((position*(NUM_PTS-2))-index0);
                assert(position >= 0 && position <= 1);
                Vector2D pos = pts[index0].scaledBy(1.0f-delta).add(pts[index1].scaledBy(delta));
                drawCellProductionValue(g, pos.getX(), pos.getY(), num);
            }

            @Override
            public void draw(AGraphics g, float position, float dt) {
                drawChit(g, pts0, position, t1);
                drawChit(g, pts1, position, t0);
            }

        }, true);
        tile0.setDieNum(t0);
        tile1.setDieNum(t1);
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
		final GColor added;
		FaceType [] structures;
		
		Face(float darkenAmount, int ... verts) {
			this(GColor.TRANSPARENT, darkenAmount, verts);
		}
		
		Face(GColor added, float darkenAmount, int ... verts) {
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
	    new Face(new GColor(255,255,255,0), 0.8f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),
	    // hilt
	    new Face(new GColor(255,255,255,0), 0.8f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_INACTIVE_STRONG),

	    // SAA lightened for active
	    // blade
	    new Face(new GColor(160,160,160,0), 0.1f, 0,-4, 1,-6, 2,-4, 2,8, 0,8).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    // hilt
	    new Face(new GColor(100,100,100,0), 0.1f, -2,5, 4,5, 4,4, -2,4).setFaceTypes(FaceType.KNIGHT_ACTIVE_STRONG),
	    
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
		return getBoard().getTileWidth()/(6*3);
	}
	
	public final void drawSettlement(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.SETTLEMENT, outline);
	}

    public final void drawCity(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.CITY, outline);
	}

    public final void drawWalledCity(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.CITY_WALL, outline);
	}

    public final void drawMetropolisTrade(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_TRADE, outline);
	}

    public final void drawMetropolisPolitics(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_POLITICS, outline);
	}

    public final void drawMetropolisScience(AGraphics g, IVector2D pos, int playerNum, boolean outline) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, pos, 0, getStructureRadius(), FaceType.METRO_SCIENCE, outline);
	}

    public final void drawMerchant(AGraphics g, Tile t, int playerNum) {
		if (playerNum > 0)
			g.setColor(getPlayerColor(playerNum));
		drawFaces(g, t, 0, getStructureRadius(), FaceType.MERCHANT, false);
		g.setColor(GColor.WHITE);
		String txt = "\n2:1\n" + t.getResource().getName(UISOC.getInstance());
		Vector2D v = g.transform(t);
		
		g.drawJustifiedString( v.Xi()-2, v.Yi()-2-g.getTextHeight()*2, Justify.CENTER, Justify.TOP, txt);
	}

    public final void drawKnight_image(AGraphics g, float _x, float _y, int playerNum, int level, boolean active, boolean outline) {
		final int x = Math.round(_x);
		final int y = Math.round(_y);
		final int r = (int)(getBoard().getTileWidth()/8) + 1;
		final int r2 = r+3;
		int index = level * (active ? 2 : 1) - 1;
		g.drawOval(x-r2/2, y-r2/2, r2, r2);
		g.drawImage(knightImages[index], x-r/2, y-r/2, r, r);
	}

    public final float getKnightRadius() {
		return getBoard().getTileWidth()*2/(8*3*3);
	}

    public final void drawKnight(AGraphics g, IVector2D pos, int playerNum, int level, boolean active, boolean outline) {
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

    public final void drawCircle(AGraphics g, IVector2D pos) {
		g.pushMatrix();
		g.begin();
		g.translate(pos);
		int angle = 0;
		float rad = getBoard().getTileWidth()/5;
		g.scale(rad, rad);
		int pts = 10;
		for (int i=0; i<pts; i++) {
			g.vertex(CMath.cosine(angle), CMath.sine(angle));
			angle += 360/pts;
		}
		g.drawLineLoop( 2);
		g.popMatrix();
	}

    public final void drawPirateFortress(AGraphics g, Vertex v, boolean outline) {
		MutableVector2D mv = g.transform(v);
		int x = mv.Xi()-10;
		int y = mv.Yi()-10;
		for (int i=0; i<v.getPirateHealth(); i++) {
			int rw = 40;
			int rh = 30;
			g.setColor(GColor.GRAY);
			g.drawFilledOval(x,y,rw,rh);
			g.setColor(GColor.RED);
			g.drawOval(x,y,rw,rh);
			x += 10;
			y += 5;
		}
		g.setColor(GColor.GRAY);
		drawFaces(g, v, 0, getStructureRadius(), FaceType.PIRATE_FORTRESS, outline);
	}

    public final void drawFaces(AGraphics g, IVector2D pos, float angle, float radius, FaceType structure, boolean outline) {
	    //final float xRad = 3; // actual radius as defined above
		//float scale = radius / xRad;
		drawFaces(g, pos, angle, radius, radius, structure, outline);
	}

	private HashMap<FaceType, Face[]> faceMap = new HashMap<>();
	
	private Face [] getStructureFaces(FaceType s) {
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

    final void drawFaces(AGraphics g, IVector2D pos, float angle, float w, float h, FaceType structure, boolean outline) {
		g.pushMatrix();
		g.translate(pos);
		g.rotate(angle);
		g.scale(w, -h);
		GColor saveColor = g.getColor();

		Face [] faces = getStructureFaces(structure);
	    for (Face face : faces) {
		    g.begin();
	    	for (int i=0; i<face.numVerts; i++)
	    		g.vertex(face.xVerts[i], face.yVerts[i]);
	    	
	    	if (outline) {
	    	    g.setColor(outlineColorDark);
	    	    g.drawLineLoop( 2);
	    	}

	    	GColor c = face.added.add(saveColor.darkened(face.darkenAmount));
            g.setColor(c);
            g.drawTriangleFan();
	    }
	    g.setColor(saveColor);
	    g.popMatrix();
	}
	
	private int pickEdge(APGraphics g, int mouseX, int mouseY) {
		g.begin();
		for (int index=0; index<getBoard().getNumRoutes(); index++) {
			g.setName(index);
			//renderEdge(g, getBoard().getRoute(index));
            g.vertex(getBoard().getRouteMidpoint(getBoard().getRoute(index)));
		}
		return g.pickClosest(mouseX, mouseY);//g.pickLines(mouseX, mouseY, Math.round(RenderConstants.thickLineThickness*2));
	}
	
	private int pickVertex(APGraphics g, int mouseX, int mouseY) {
		g.begin();
		for (int index=0; index<getBoard().getNumAvailableVerts(); index++) {
			g.setName(index);
			Vertex v = getBoard().getVertex(index);
			g.vertex(v);
		}
		return g.pickClosest(mouseX, mouseY);//g.pickPoints(mouseX, mouseY, 10);
	}
	
	private int pickTile(APGraphics g, int mouseX, int mouseY) {
		g.begin();
		//final int dim = Math.round(getBoard().getTileWidth() * getComponent().getWidth());
		for (int index=0; index<getBoard().getNumTiles(); index++) {
			g.setName(index);
			Tile cell = getBoard().getTile(index);
			g.vertex(cell);
		}
		return g.pickClosest(mouseX, mouseY);//g.pickPoints(mouseX, mouseY, dim);
	}
	
	private void renderEdge(AGraphics g, Route e) {
		Vertex v0 = getBoard().getVertex(e.getFrom());
		Vertex v1 = getBoard().getVertex(e.getTo());
		g.vertex(v0);
		g.vertex(v1);
	}
	
	private void renderDamagedEdge(AGraphics g, Route e) {
		Vertex v0 = getBoard().getVertex(e.getFrom());
		Vertex v1 = getBoard().getVertex(e.getTo());
		// choose v1 or v0 based on which endpoint is touching another route or structure of ours
        Vertex v = v0;
        if (v1.getPlayer() == e.getPlayer() && v1.isStructure()) {
            v = v1;
        } else if (v0.getPlayer() != e.getPlayer() || !v0.isStructure()) {
            for (Route r : getBoard().getRoutesAdjacentToVertex(e.getTo())) {
                if (!r.equals(e)) {
                    if (r.getPlayer() == e.getPlayer()) {
                        v = v1;
                    }
                }
            }
        }


		g.begin();
		g.vertex(v0);
		Vector2D mp = getBoard().getRouteMidpoint(e);
		g.vertex(mp);
		Vector2D dv = Vector2D.newTemp(mp).subEq(v0).normEq().addEq(v);
		g.vertex(dv);
	}
	
	public final void drawDamagedRoad(AGraphics g, Route e, boolean outline) {
		g.begin();
	    if (outline) {
	        GColor old = g.getColor();
	        g.setColor(outlineColorDark);
            renderDamagedEdge(g, e);
            g.drawLines(RenderConstants.thickLineThickness+2);
            g.setColor(old);
	    } 
	    renderDamagedEdge(g, e);
	    g.drawLineStrip(RenderConstants.thickLineThickness);
	}

	public final void drawEdge(AGraphics g, Route e, RouteType type, int playerNum, boolean outline) {
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
	
	public final void drawVertex(AGraphics g, Vertex v, VertexType type, int playerNum, boolean outline) {
		switch (type) {
			case OPEN:
				break;
			case PIRATE_FORTRESS:
				drawPirateFortress(g, v, outline);
				break;
			case OPEN_SETTLEMENT:
				g.setColor(GColor.LIGHT_GRAY);
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
	
	public final void drawRoad(AGraphics g, Route e, boolean outline) {
		g.begin();
	    if (outline) {
	        GColor old = g.getColor();
	        g.setColor(outlineColorDark);
            renderEdge(g, e);
            g.drawLines(RenderConstants.thickLineThickness+2);
            g.setColor(old);
	    } 
	    renderEdge(g, e);
	    g.drawLineStrip(RenderConstants.thickLineThickness);
	}
	
	public final int getEdgeAngle(Route e) {
		Vertex v0 = getBoard().getVertex(e.getFrom());
		Vertex v1 = getBoard().getVertex(e.getTo());
		if (v1.getX() < v0.getX()) {
			Vertex t = v0;
			v0 = v1;
			v1 = t;
		}
		int ang = Math.round(Vector2D.newTemp(v1).sub(v0).angleOf());
		// we want eight 60, 300 or 0
		return ang;
	}

    public final float getShipRadius() {
		return getBoard().getTileWidth()/(8*3);
	}

    public final float getRobberRadius() {
		return getBoard().getTileWidth()/(7*3);
	}

    public final void drawShip(AGraphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawFaces(g, mp, getEdgeAngle(e), getShipRadius(), FaceType.SHIP, outline);
	}

    public final void drawShip(AGraphics g, IVector2D v, int angle, boolean outline) {
		drawFaces(g, v, angle, getShipRadius(), FaceType.SHIP, outline);
	}

    public final void drawWarShip(AGraphics g, IVector2D v, int angle, boolean outline) {
        drawFaces(g, v, angle, getShipRadius(), FaceType.WAR_SHIP, outline);
    }

    public final void drawWarShip(AGraphics g, Route e, boolean outline) {
		IVector2D mp = getBoard().getRouteMidpoint(e);
		drawWarShip(g, mp, getEdgeAngle(e), outline);
	}

    public final void drawVessel(AGraphics g, RouteType type, Route e, boolean outline) {
        IVector2D mp = getBoard().getRouteMidpoint(e);
        drawVessel(g, type, mp, getEdgeAngle(e), outline);
    }

    public final void drawVessel(AGraphics g, RouteType type, IVector2D v, int angle, boolean outline) {
	    switch (type) {
            case SHIP:
                drawShip(g, v, angle, outline);
                break;
            case WARSHIP:
                drawWarShip(g, v, angle, outline);
                break;
        }
    }

	public final void drawRobber(AGraphics g, Tile cell) {
	    drawRobber(g, cell, GColor.LIGHT_GRAY);
	}

    public final void drawRobber(AGraphics g, Tile cell, GColor color) {
        g.setColor(color);
        drawFaces(g, cell, 0, getRobberRadius(), FaceType.ROBBER, false);
    }

    public final void drawPirate(AGraphics g, IVector2D v) {
	    drawPirate(g, v, GColor.BLACK);
    }

    public final void drawPirate(AGraphics g, IVector2D v, GColor color) {
		g.setColor(color);
		drawFaces(g, v, 0, getRobberRadius(), FaceType.WAR_SHIP, false);
	}

	/**
	 * Set the current pick mode.
	 *  
	 * @param handler
	 */
	public final void setPickHandler(PickHandler handler) {
		if (handler == null) {
			this.pickMode = PickMode.PM_NONE;
		} else {
			this.pickMode = handler.getPickMode();
		}
		this.pickHandler = handler;
		pickedValue = -1;
		getComponent().redraw();
	}
	
	public final void drawIslandOutlined(AGraphics g, int tileIndex) {
		Collection<Integer> islandEdges = getBoard().findIslandShoreline(tileIndex);
		g.begin();
    	for (int eIndex : islandEdges) {
    		renderEdge(g, getBoard().getRoute(eIndex));
    	}
    	g.drawLines(5);

    	Tile cell = getBoard().getTile(tileIndex);
    	if (cell.getIslandNum() > 0) {
    		drawIslandInfo(g, getBoard().getIsland(cell.getIslandNum()));
    	}
	}
	
	public final void drawIslandInfo(AGraphics g, Island i) {
		g.begin();
		g.setColor(GColor.BLUE);
    	for (int eIndex : i.getShoreline()) {
    		renderEdge(g, getBoard().getRoute(eIndex));
    	}
    	g.drawLines(5);
    	MutableVector2D midpoint = new MutableVector2D();
    	int num = 0;
    	for (int eIndex : i.getShoreline()) {
    		midpoint.addEq(getBoard().getRouteMidpoint(getBoard().getRoute(eIndex)));
    		num++;
    	}
    	midpoint.scaleEq(1.0f / num);
    	g.transform(midpoint);
    	String txt = "ISLAND\n" + i.getNum();
        GDimension dim = g.getTextDimension(txt, Float.POSITIVE_INFINITY);
    	g.setColor(new GColor(0,0,0,0.5f));
    	float x = midpoint.X() - (dim.width/2+5);
    	float y = midpoint.Y() - (dim.height/2+5);
    	float w = dim.width + 10;
    	float h = dim.height + 10;
    	g.drawFilledRect(x, y, w, h);
    	g.setColor(GColor.WHITE);
    	g.drawJustifiedString( midpoint.Xi(), midpoint.Yi(), Justify.CENTER, Justify.CENTER, txt);
	}
	
    private final void drawTilesOutlined(AGraphics g) {
        //GColor outlineColor = getProperties().getColorProperty("outlineColor", GColor.WHITE);
        //GColor textColor = getProperties().getColorProperty("textcolor", GColor.CYAN);
        
        float [] v = {0,0};
        
        for (int i=0; i <getBoard().getNumTiles(); i++) {
            Tile cell = getBoard().getTile(i);
            g.transform(cell.getX(), cell.getY(), v);
            int x = Math.round(v[0]);
            int y = Math.round(v[1]);
            g.setColor(outlineColorLight);
            drawTileOutline(g, cell);
            g.setColor(textColor);
            String name = cell.getType().getName(UISOC.getInstance());
            switch (cell.getType()) {
                case NONE:
                    break;
                case DESERT:
                    g.drawJustifiedString(x,y,Justify.CENTER,Justify.CENTER,name);
                    break;
                case WATER:
                    g.drawJustifiedString(x,y,Justify.CENTER,Justify.CENTER,name);
                    break;
        		case PORT_ORE:
        		case PORT_SHEEP:
        		case PORT_WHEAT:
        		case PORT_WOOD:
        		case PORT_BRICK:
        			g.drawJustifiedString(x,y,Justify.CENTER,Justify.CENTER,"2:1\n" + cell.getResource().getName(UISOC.getInstance()));
                    break;
                case PORT_MULTI:
                    g.drawJustifiedString(x,y,Justify.CENTER,Justify.CENTER,"3:1\n?");
                    break;
                case GOLD:
                    g.drawJustifiedString( x, y, Justify.CENTER, Justify.CENTER, name + "\n" + String.valueOf(cell.getDieNum()));
                    break;
                // used for random generation
                case RANDOM_RESOURCE_OR_DESERT:
                case RANDOM_RESOURCE:
                case RANDOM_PORT_OR_WATER:
                case RANDOM_PORT:
                    g.drawJustifiedString(x,y,Justify.CENTER,Justify.CENTER,name);
                    break;
				case FIELDS:
				case FOREST:
				case HILLS:
				case MOUNTAINS:
				case PASTURE:
					g.drawJustifiedString( x, y, Justify.CENTER, Justify.CENTER, cell.getResource().getName(UISOC.getInstance()) + "\n" + String.valueOf(cell.getDieNum()));
					break;
				case UNDISCOVERED:
					break;
                
            }
        }        
    }

    private final void drawTilesTextured(AGraphics g) {
        Vector2D cellD = new Vector2D(getBoard().getTileWidth(), getBoard().getTileHeight()).scaledBy(0.5f);
        g.setTextHeight(RenderConstants.textSizeSmall);
        g.setTextStyles(AGraphics.TextStyle.BOLD);

        for (int i=0; i <getBoard().getNumTiles(); i++) {
            Tile cell = getBoard().getTile(i);
            Vector2D v0 = new Vector2D(cell).sub(cellD);
            Vector2D v1 = new Vector2D(cell).add(cellD);
            switch (cell.getType()) {
            case NONE:
                g.setColor(outlineColorLight);
                drawTileOutline(g, cell);
                break;
            case DESERT:
                g.drawImage(desertImage, v0, v1);
                break;
            case WATER:
                g.drawImage(waterImage, v0, v1);
                break;
            case PORT_WHEAT:
            case PORT_WOOD:
            case PORT_BRICK:
            case PORT_ORE:
            case PORT_SHEEP:
                g.drawImage(waterImage, v0, v1);
                g.setColor(textColor);
                g.drawJustifiedString(cell.getX(),cell.getY(),Justify.CENTER,Justify.CENTER,"2:1\n" + cell.getResource().name());
                break;
            case PORT_MULTI:
                g.drawImage(waterImage, v0, v1);
                g.setColor(textColor);
                g.drawJustifiedString(cell.getX(),cell.getY(),Justify.CENTER,Justify.CENTER,"3:1\n?");
                break;
            case GOLD:
            	g.drawImage(goldImage, v0, v1);
            	break;

            case UNDISCOVERED:
            	g.drawImage(undiscoveredImage, v0, v1);
            	break;

            // used for random generation
            case RANDOM_RESOURCE_OR_DESERT:
            case RANDOM_RESOURCE:
            case RANDOM_PORT_OR_WATER:
            case RANDOM_PORT:
                g.setColor(outlineColorLight);
                drawTileOutline(g, cell);
                g.setColor(textColor);
                g.drawJustifiedString(cell.getX(),cell.getY(),Justify.CENTER,Justify.CENTER,cell.getType().getName(UISOC.getInstance()));
                break;
            case FIELDS:
            	g.drawImage(fieldshexImage, v0, v1);
				break;
			case FOREST:
				g.drawImage(foresthexImage, v0, v1);
				break;
			case HILLS:
				g.drawImage(hillshexImage, v0, v1);
				break;
			case MOUNTAINS:
				g.drawImage(mountainshexImage, v0, v1);
				break;
			case PASTURE:
				g.drawImage(pastureshexImage, v0, v1);
				break;                
            }
            
            if (cell.getDieNum() > 0) {
            	drawCellProductionValue(g, cell.getX(),cell.getY(), cell.getDieNum());
            }
        }
    }

    @Override
    public void doClick() {
	    // allow the accept button to do this work now
        if (pickedValue >= 0) {
            pickHandler.onPick(this, pickedValue);
            getComponent().redraw();
            pickedValue = -1;
        }
    }

    public final void drawCellProductionValue(AGraphics g, float x, float y, int num) {
	    final float radius = g.getTextHeight()*2;
        g.setColor(GColor.BLACK);
        g.begin();
        g.vertex(x, y);
        g.drawPoints(radius);
        g.end();
        g.setColor(GColor.CYAN);
        g.drawJustifiedString( x, y, Justify.CENTER, Justify.CENTER, String.valueOf(num));
    }

	public final void draw(APGraphics g, int pickX, int pickY) {

	    final int width = getComponent().getWidth();
	    final int height = getComponent().getHeight();
	    final Board board = getBoard();

        if (width <= 10 || height <= 10 || board == null)
            return; // avoid images getting resized excessively if the window is getting resized

        g.ortho();
	    g.pushMatrix();
	    g.setIdentity();
        try {

            float dim = Math.min(width, height);
            g.translate(width/2, height/2);
            g.scale(dim, dim);
            g.translate(-0.5f, -0.5f);

            if (pickX >= 0 && pickY >= 0)
                doPick(g, pickX, pickY);
    	    long enterTime = System.currentTimeMillis();

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
                			pickHandler.onHighlighted(this, g, i);
                		} else {
                			pickHandler.onDrawPickable(this, g, i);
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
                    			pickHandler.onHighlighted(this, g, i);
                    		} else {
                    			pickHandler.onDrawPickable(this, g, i);
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
        		for (int i=0; i<board.getNumAvailableVerts(); i++) {
        			if (pickMode == PickMode.PM_VERTEX) {
        				if (pickHandler.isPickableIndex(this, i)) {
                    		if (i == pickedValue) {
                    			pickHandler.onHighlighted(this, g, i);
                    		} else {
                    			pickHandler.onDrawPickable(this, g, i);
                    		}
                    		continue;
        				}
        			}
        			Vertex v = board.getVertex(i);
        			drawVertex(g, v, v.getType(), v.getPlayer(), false);
        		}
            }

    		int robberTile = board.getRobberTileIndex();
    		int pirateTile = board.getPirateTileIndex();
    		int merchantTile = board.getMerchantTileIndex();
    		int merchantPlayer = board.getMerchantPlayer();
    		
            if (pickedValue >= 0) {
            	pickHandler.onHighlighted(this, g, pickedValue);
            }

    		if (robberTile >= 0)
    		    drawRobber(g, board.getTile(robberTile));
    		if (pirateTile >= 0)
    			drawPirate(g, board.getTile(pirateTile));
    		if (merchantTile >= 0)
    			drawMerchant(g, board.getTile(merchantTile), merchantPlayer);
    		
    		if (pickMode != PickMode.PM_NONE)
    			pickHandler.onDrawOverlay(this, g);

            synchronized (animations) {
                Iterator<AAnimation<AGraphics> > it = animations.iterator();
                while (it.hasNext()) {
                    AAnimation<AGraphics> a = it.next();
                    a.update(g);
                    getComponent().redraw();
                    if (a.isDone()) {
                        it.remove();
                    }
                }
            }

    		if (getRenderFlag(RenderFlag.DRAW_CELL_CENTERS)) {
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        g.vertex(c.getX(), c.getY());
    		    }
    		    g.setColor(GColor.YELLOW);
    		    g.drawPoints(8);
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_CELLS)) {
    		    g.setColor(GColor.RED);
    		    for (int i=0; i<board.getNumTiles(); i++) {
    		        Tile c = board.getTile(i);
    		        g.drawJustifiedStringOnBackground(c.getX(), c.getY(), Justify.CENTER, Justify.TOP, String.valueOf(i), GColor.TRANSLUSCENT_BLACK, 5);
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_VERTS)) {
                g.setColor(GColor.WHITE);
                for (int i=0; i<board.getNumAvailableVerts(); i++) {
                    Vertex v  = board.getVertex(i);
                    g.drawJustifiedStringOnBackground(v.getX(), v.getY(), Justify.CENTER, Justify.TOP, String.valueOf(i), GColor.TRANSLUSCENT_BLACK, 5);
                }
    		}
    		
    		if (getRenderFlag(RenderFlag.NUMBER_EDGES)) {
    		    g.setColor(GColor.YELLOW);
    		    for (int i=0; i<board.getNumRoutes(); i++) {
    		        Route e = board.getRoute(i);
    		        MutableVector2D m = new MutableVector2D(board.getRouteMidpoint(e));
    		        g.drawJustifiedStringOnBackground(m.getX(), m.getY(), Justify.CENTER, Justify.TOP, String.valueOf(i), GColor.TRANSLUSCENT_BLACK, 5);
    		    }
    		}
    		
    		if (getRenderFlag(RenderFlag.SHOW_ISLAND_INFO)) {
    			for (Island i : board.getIslands()) {
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

            if (pickMode == PickMode.PM_CUSTOM) {
                CustomPickHandler handler = (CustomPickHandler)pickHandler;
                for (int i=0; i<handler.getNumElements(); i++) {
                    if (i == pickedValue) {
                        handler.onHighlighted(this, g, i);
                    } else {
                        handler.onDrawPickable(this, g, i);
                    }
                }
            }

    		// notify anyone waiting on me
    		synchronized (this) {
    		    notifyAll();
    		}
        } catch (Exception e) {
            e.printStackTrace();
        }
        g.popMatrix();
	}

	private void doPick(APGraphics g, int pickX, int pickY) {
		int index = -1;
		switch (pickMode) {
    		case PM_NONE: 
    		    break;
    		case PM_EDGE:
    			index = pickEdge(g, pickX, pickY);
    			break;
    		case PM_VERTEX:
    		    index = pickVertex(g, pickX, pickY);
    			break;
    		case PM_TILE:
    		    index = pickTile(g, pickX, pickY);
    			break;
    		case PM_CUSTOM:
    			index = ((CustomPickHandler)pickHandler).pickElement(this, g, pickX, pickY);
    			break;
		}
		
		if (index >= 0 && pickHandler.isPickableIndex(this, index)) {
			pickedValue = index;
		} else {
			// pickedValue = -1;
            // preserve the most recent
		}

        if (getRenderFlag(RenderFlag.SHOW_CELL_INFO)) {
            cellInfoIndex = pickTile(g, pickX, pickY);
        }

        if (getRenderFlag(RenderFlag.SHOW_EDGE_INFO)) {
            edgeInfoIndex = pickEdge(g, pickX, pickY);
        }

        if (getRenderFlag(RenderFlag.SHOW_VERTEX_INFO)) {
            vertexInfoIndex = pickVertex(g, pickX, pickY);
        }
	}

    private void drawInfo(AGraphics g, IVector2D v, String info) {

        g.setColor(GColor.WHITE);
	    g.drawWrapStringOnBackground(v.getX(), v.getY(), g.getViewportWidth()/2, info, GColor.TRANSLUSCENT_BLACK, 4);
    }
    
    private void drawTileInfo(AGraphics g, int cellIndex) {
        if (cellIndex < 0)
            return;
        Tile cell = getBoard().getTile(cellIndex);
        String info = "CELL " + cellIndex + "\n  " + cell.getType() + "\nadj:" + cell.getAdjVerts();
        if (cell.getResource() != null) {
            info += "\n  " + cell.getResource();
        }
        if (cell.getIslandNum() > 0) {
        	info += "\n  Island " + cell.getIslandNum();
        }
        
        drawInfo(g, cell, info);
    }
    
    private void drawEdgeInfo(AGraphics g, int edgeIndex) {
        if (edgeIndex < 0)
            return;
        Route edge = getBoard().getRoute(edgeIndex);
        String info = "EDGE " + edgeIndex;
        if (edge.getPlayer() > 0)
            info += "\n  Player " + edge.getPlayer();
        info += "\n  " + edge.getFlagsString();
        info += "\n  ang=" + getEdgeAngle(edge);
        info += "\n  tiles=" + edge.getTile(0) + "/" + edge.getTile(1);
        
        drawInfo(g, getBoard().getRouteMidpoint(edge), info);
    }
    
    private void drawVertexInfo(AGraphics g, int vertexIndex) {
        if (vertexIndex < 0)
            return;
        Vertex vertex = getBoard().getVertex(vertexIndex);
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
            int pNum = getBoard().checkForPlayerRouteBlocked(vertexIndex);
            info += "\n  Blocks player " + pNum + "'s roads";
        }

        drawInfo(g, vertex, info);
    }

    public void drawCard(GColor color, AGraphics g, String txt, float x, float y, float cw, float ch) {
	    drawCard(color, g, txt, x, y, cw, ch, 1);
    }

    public void drawCard(GColor color, AGraphics g, String txt, float x, float y, float cw, float ch, float alpha) {
        g.pushMatrix();
        g.setIdentity();
        //g.drawImage(cardFrameImage, x, y, cw, ch);
        float border = RenderConstants.thickLineThickness;
        g.setColor(GColor.BLUE.withAlpha(alpha));
        g.drawFilledRoundedRect(x-border, y-border, cw+border*2, ch+border*2, cw/4+border);
        g.setColor(GColor.CYAN.darkened(0.2f).withAlpha(alpha));
        g.drawFilledRoundedRect(x, y, cw, ch, cw/4);
        g.setColor(color);
        g.drawWrapString(x+cw/2, y+ch/2, cw-border*2, Justify.CENTER, Justify.CENTER, txt);
        g.popMatrix();
    }

    public boolean isPicked() {
        return pickHandler != null && pickedValue >= 0;
    }

    public void acceptPicked() {
	    if (isPicked())
            pickHandler.onPick(this, pickedValue);
    }

    /**
     * Delete screen capture and force a full redraw
     */
    public final void clearCached() {
    }

    public final void reset() {
        setPickHandler(null);
        animations.clear();
    }
}




