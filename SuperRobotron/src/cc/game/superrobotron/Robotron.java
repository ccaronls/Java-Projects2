package cc.game.superrobotron;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import cc.lib.game.*;
import cc.lib.math.CMath;
import static java.lang.System.*;

// Independent transferable 
public class Robotron {

    // ---------------------------------------------------------//
    // STRUCTS //
    // ---------------------------------------------------------//
    
    // DATA for projectiles -----------------------------
    
    // TODO: Decide! Objects or arrays!
    private class MissleInt {
        int x, y, dx, dy, duration;
        
        void copy(MissleInt c) {
            x = c.x;
            y = c.y;
            dx = c.dx;
            dy = c.dy;
            duration = c.duration;
        }
        
        void init(int x, int y, int dx, int dy, int duration) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.duration = duration;
        }
    };
    
    private class MissleFloat {
        float x, y, dx, dy;
        
        int duration;
        
        void copy(MissleFloat c) {
            x = c.x;
            y = c.y;
            dx = c.dx;
            dy = c.dy;
            duration = c.duration;
        }
        
        void init(float x, float y, float dx, float dy, int duration) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.duration = duration;
        }
    };
    
    private class MissleSnake {
        int x, y; // position of head
        
        int duration; // counter
        
        int state;
        
        int[] dir = new int[SNAKE_MAX_SECTIONS]; // direction of each section
        
        void copy(MissleSnake c) {
            x = c.x;
            y = c.y;
            duration = c.duration;
            state = c.state;
            //Utils.copyElems(dir, c.dir);
            System.arraycopy(c.dir, 0, dir, 0, SNAKE_MAX_SECTIONS);
        }
        
        void init(int x, int y, int duration, int state) {
            this.x = x;
            this.y = y;
            this.duration = duration;
            this.state = state;
        }
    };
    
    private class Powerup extends MissleFloat {
        int type;
        
        void copy(Powerup c) {
            super.copy(c);
            type = c.type;
        }
        
        void init(float x, float y, float dx, float dy, int duration, int type) {
            super.init(x, y, dx, dy, duration);
            this.type = type;
        }
    }
    
    private class Wall {
        int v0, v1, type;// all
        int state; // door
        int frame; // door, electric 
        int health; // normal
        int p0, p1; // portal
        float frequency; // for rubber walls
        boolean visible = false;
        boolean ending = false;
        
        void clear() {
            type=state=frame=health=0;
            v0=v1=-1;
            visible = false;
            ending = false;
            frequency=0;
        }
        
        void initDoor(int state, int v0, int v1) {
            this.type = WALL_TYPE_DOOR;
            this.state = state;
            this.v0 = v0;
            this.v1 = v1;
        }
        
        void init(int type, int v0, int v1) {
            this.type = type;
            this.v0 = v0;
            this.v1 = v1;
        }
        
        public String toString() {
            String str = getWallTypeString(type) + " [" + v0 + ", " + v1 + "]";
            if (this.ending)
                str += "\nEND";
            switch (type) {
            case WALL_TYPE_NORMAL: str += "\nhealth=" + health; break;
            case WALL_TYPE_ELECTRIC: str += "\nframe=" + frame; break;
            case WALL_TYPE_DOOR: str += "\n state=" + getDoorStateString(state) + "\nframe=" + frame; break;
            case WALL_TYPE_PORTAL: str += "\np0=" + p0 + ", p1=" + p1 + "]"; break;
            case WALL_TYPE_RUBBER: str += "\nfreq=" + frequency; break;
            }
            return str;
        }
    }

    class Player {
        int x, y, dx, dy, dir=DIR_DOWN;
        float scale = 1;
        int score = 0;
        int lives = 0;
        int target_x, target_y;
        int start_x, start_y;
        int powerup = -1;
        int powerup_duration;
        int keys;
        int movement;
        boolean firing = false;
        int [] primary_verts = { -1, -1, -1, -1 };
        //int killed_frame = 0; // the frame the player was killed
        MissleInt[] missles = new MissleInt[PLAYER_MAX_MISSLES];
        int num_missles = 0;
        int hulk_charge_dx = 0;
        int hulk_charge_dy = 0;
        int hulk_charge_frame = 0;
        //boolean teleported = false;
        int state = PLAYER_STATE_SPAWNING;
        int next_state_frame = 0;
        float stun_dx = 0;
        float stun_dy = 0;
        int[] tracer_x = new int[PLAYER_SUPERSPEED_NUM_TRACERS];
        int[] tracer_y = new int[PLAYER_SUPERSPEED_NUM_TRACERS];
        int num_tracers = 0;
        AColor[] tracer_color = new AColor[PLAYER_SUPERSPEED_NUM_TRACERS];
        int[] tracer_dir = new int[PLAYER_SUPERSPEED_NUM_TRACERS];
        int [] barrier_electric_wall = { -1, -1, -1, -1 };

        int hit_type = -1; // these are the 'reset' values
        int hit_index = -1;
        
        int [] cellXY = new int[2];
        List<int[]> path = new LinkedList<int[]>();

        int last_shot_frame = 0;

        Player() {
            for (int i = 0; i < PLAYER_MAX_MISSLES; i++)
                missles[i] = new MissleInt();
        }
    }

    class Particle {
        int x, y, type, duration, start_frame, star, angle;
        int playerIndex;
        public void copy(Particle p) {
            x = p.x;
            y = p.y;
            type = p.type;
            start_frame = p.start_frame;
            star = p.star;
            playerIndex = p.playerIndex;
            angle = p.angle;
        }
    };
    
    // ---------------------------------------------------------//
    // CONSTANTS //
    // ---------------------------------------------------------//
    
    private final int PLAYER_STATE_SPAWNING = 0;
    private final int PLAYER_STATE_ALIVE = 1;
    private final int PLAYER_STATE_EXPLODING = 2;
    private final int PLAYER_STATE_TELEPORTED = 3;
    private final int PLAYER_STATE_SPECTATOR = 4;
    
    // enum
    private final int   SNAKE_STATE_CHASE = 0;
    private final int   SNAKE_STATE_CLIMB_WALL = 1; // slowly climb a wall
    private final int   SNAKE_STATE_CREST_WALL = 2; // crest the wall, start to
    private final int   SNAKE_STATE_DESCEND_WALL = 3;
    private final int   SNAKE_STATE_ATTACHED = 4;
    private final int   SNAKE_STATE_DYING = 5;
    
    // game types
    private final int   GAME_TYPE_CLASSIC = 0; // No maze
    private final int   GAME_TYPE_ROBOCRAZE = 1;
    
    private final int   WORLD_WIDTH_CLASSIC = 1000;
    private final int   WORLD_HEIGHT_CLASSIC = 1000;
    
    //  final int GAME_TYPE_ROBOCRAZE = 1; // Use Maze, level over when
    // player gets to exit (X)
    
    // possible game states
    private final int   GAME_STATE_INTRO = 0;
    private final int   GAME_STATE_PLAY = 1;
    //private final int   GAME_STATE_PLAYER_HIT = 2;
    private final int   GAME_STATE_GAME_OVER = 3;
    
    private final int   MAZE_CELL_WIDTH = 160;
    private final int   MAZE_CELL_HEIGHT = 160;
    
    private final int   MAZE_NUMCELLS_X  = 10;
    private final int   MAZE_NUMCELLS_Y  = 10;
    
    private final int   MAZE_NUM_VERTS = (MAZE_NUMCELLS_X+1) * (MAZE_NUMCELLS_Y+1);
    private final int   MAZE_VERTEX_NOISE = 30; // random noise in placement of
    
    //private final int     MAZE_CELL_X = 14; 
    //private final int     MAZE_CELL_Y = 14;   
    // vertices
    private final int   MAZE_WALL_THICKNESS = 3;
    private final int   MAZE_WIDTH  = MAZE_CELL_WIDTH * MAZE_NUMCELLS_X;//1440; // width of world
    private final int   MAZE_HEIGHT = MAZE_CELL_HEIGHT * MAZE_NUMCELLS_Y;//1240; // height of world
    
    // used to make comparisons close to zero
    private final float EPSILON = 0.00001f;
    private final int   PLAYER_SPEED = 8; // pixels per frame
    private final int   PLAYER_SUPER_SPEED_BONUS = 3;
    private final int   PLAYER_RADIUS = 16;
    private final int   PLAYER_RADIUS_BARRIER = 25; 
    private final int   PLAYER_MAX_MISSLES = 16; // max player missles on screen at one time
    private final int   PLAYER_DEATH_FRAMES = 70; // number of frames to do the 'death' sequence
    private final int   PLAYER_START_LIVES = 3; // number of lives player starts with
    private final int   PLAYER_NEW_LIVE_SCORE = 25000; // number of points to give an extra player
    private final int   PLAYER_SHOT_FREQ = 4; // number of frames between adjacent
    private final int   PLAYER_SHOT_FREQ_MEGAGUN = 3; // missle shots
    private final int   PLAYER_MISSLE_SPEED = 20; // distance missle travels per frame
    private final int   PLAYER_MISSLE_DURATION = 50; // number of frames a missle exists for
    private final int   PLAYER_SPAWN_FRAMES = 30; // number of frames at beginning of level where player is safe   
    private final float PLAYER_HULK_GROW_SPEED = 0.05f;
    private final float PLAYER_HULK_SCALE = 1.6f;
    private final int   PLAYER_HULK_CHARGE_SPEED_BONUS = PLAYER_SPEED;
    
    private final int   PLAYER_POWERUP_DURATION = 500; // frames
    private final int   PLAYER_POWERUP_WARNING_FRAMES = 50; //
    private final int   PLAYER_SUPERSPEED_NUM_TRACERS = 4; // used for POWERUP_SPEED mode
    
    private final int   MAX_ENEMIES = 64; // max number of enemies on the screen per frame
    private final int   MAX_ENEMY_MISSLES = 16; // max number of enemy missles on screen per frame
    
    // Snake type projectiles
    
    private final int   MAX_SNAKE_MISSLES = 8; // max number deployed at one time
    
    private final int   SNAKE_MAX_SECTIONS = 10; // max number of section
    private final int   SNAKE_SECTION_LENGTH = 20; // pixels, should be even multiple of SNAKE_SPEED
    private final int   SNAKE_THICKNESS = 3; // pixels
    private final int   SNAKE_DURATION = 1000; // frames
    private final int   SNAKE_SPEED = 4; // pixels of advancement per frame
    private final float SNAKE_HEURISTIC_FACTOR = 0.4f;
    
    // time
    
    private final int MAX_PARTICLES = 8; // maximum explosions on the screen
    
    private final int PARTICLE_TYPE_BLOOD = 0;
    private final int PARTICLE_TYPE_DYING_ROBOT = 1;
    private final int PARTICLE_TYPE_DYING_TANK = 2;
    private final int PARTICLE_TYPE_PLAYER_STUN = 3;
    
    // if any more particle types are added, then the update funciton
    // will need to get fixed
    private final int PARTICLES_NUM_TYPES = 4; // MUST BE LAST!
    
    private final int PARTICLE_DYING_ROBOT_DURATION = 5;
    
    private final int PARTICLE_DYING_TANK_DURATION = 9;
    
    private final int PARTICLE_BLOOD_DURATION = 60;
    private final int PARTICLE_BLOOD_RADIUS = 13;
    
    private final int PARTICLE_LINES_DURATION = 8;
    
    // enum of enemy types
    private final int ENEMY_INDEX_GEN = 0; // generator
    
    private final int ENEMY_INDEX_ROBOT_N = 1; // always moves toward player
    private final int ENEMY_INDEX_ROBOT_E = 2;
    private final int ENEMY_INDEX_ROBOT_S = 3;
    private final int ENEMY_INDEX_ROBOT_W = 4;
    
    private final int ENEMY_INDEX_THUG_N = 5; // indistructable idiot, walks north
    private final int ENEMY_INDEX_THUG_E = 6; // walks east
    private final int ENEMY_INDEX_THUG_S = 7; // walks south
    private final int ENEMY_INDEX_THUG_W = 8; // walks west
    
    private final int ENEMY_INDEX_BRAIN = 9; // The evil brain
    
    private final int ENEMY_INDEX_ZOMBIE_N = 10; // a brain turns people into zombies
    private final int ENEMY_INDEX_ZOMBIE_E = 11;
    private final int ENEMY_INDEX_ZOMBIE_S = 12;
    private final int ENEMY_INDEX_ZOMBIE_W = 13;
    
    private final int ENEMY_INDEX_TANK_NE = 14; // Tanks only move diagonally and
    private final int ENEMY_INDEX_TANK_SE = 15;
    private final int ENEMY_INDEX_TANK_SW = 16;
    private final int ENEMY_INDEX_TANK_NW = 17;
    
    private final int ENEMY_INDEX_TANK_GEN_NE = 18; // TankGens only move diagonally
    private final int ENEMY_INDEX_TANK_GEN_SE = 19;
    private final int ENEMY_INDEX_TANK_GEN_SW = 20;
    private final int ENEMY_INDEX_TANK_GEN_NW = 21;
    
    private final int ENEMY_INDEX_JAWS = 22;
    private final int ENEMY_INDEX_LAVA = 23;
    
    private final int ENEMY_INDEX_NUM = 24; // MUST BE LAST!!!!!!!!!!   
    
    private final String []  ENEMY_NAMES = {
        "generator",
        "robot", "robot", "robot", "robot",
        "thug", "thug", "thug", "thug",
        "brain",
        "zombie","zombie","zombie","zombie",
        "tank","tank","tank","tank",
        "tankgen","tankgen","tankgen","tankgen",
        "jaws",
        "lavapit"       
    };
    
    private final int ENEMY_SPAWN_SCATTER = 30; // used to scatter thugs and brains around generators   
    
    private final int   ENEMY_ROBOT_RADIUS = 12;
    private final int   ENEMY_ROBOT_SPEED = 7;
    private final int   ENEMY_ROBOT_SPEED_INCREASE = 200; // number of player moves to increase robot speed
    private final int   ENEMY_ROBOT_MAX_SPEED = PLAYER_SPEED;//12;
    private final float ENEMY_ROBOT_HEURISTIC_FACTOR = 0.0f; // likelyhood the robot will choose direction toward player
    private final int   ENEMY_ROBOT_ATTACK_DIST = 300; // max distance a guy will try to shoot from
    private final float ENEMY_ROBOT_FORCE = 10.0f; // stun force from a robot
    
    private final int   ENEMY_PROJECTILE_FREQ = 2; // used to determine frequency of guy fires
    private final int   ENEMY_PROJECTILE_RADIUS = 4;
    private final float ENEMY_PROJECTILE_FORCE = 5.0f;
    private final int   ENEMY_PROJECTILE_DURATION = 60;
    private final float ENEMY_PROJECTILE_GRAVITY = 0.3f;
    
    private final int ENEMY_GEN_INITIAL = 15; // starting generators for level 1
    private final int ENEMY_GEN_SPAWN_MIN = 3; // minimum to spawn when hit
    private final int ENEMY_GEN_SPAWN_MAX = 10; // maximum to spawn when hit
    private final int ENEMY_GEN_RADIUS = 10;
    private final int ENEMY_GEN_PULSE_FACTOR = 3; // gen will pulse +- this
    private final int ENEMY_TANK_GEN_RADIUS = 15;
    
    // amount from GEN_RADIUS
    
    // NOTE: on HEURISTIC_FACTOR, 0.0 values make a bot move toward player, 1.0
    // makes move totally random
    
    private final int   ENEMY_THUG_UPDATE_FREQ = 4;
    private final int   ENEMY_THUG_SPEED = 6;
    private final float ENEMY_THUG_PUSHBACK = 8.0f; // number of units to push the thug on a hit
    private final int   ENEMY_THUG_RADIUS = 17;
    private final float ENEMY_THUG_HEURISTICE_FACTOR = 0.9f;
    
    // ENEMY BRAIN  
    
    private final int ENEMY_BRAIN_RADIUS = 15;
    private final int ENEMY_BRAIN_SPEED = 7;
    private final int ENEMY_BRAIN_ZOMBIFY_FRAMES = 80; // frames taken to zombify a person
    private final int ENEMY_BRAIN_FIRE_CHANCE = 10; // chance = 1-10-difficulty
    private final int ENEMY_BRAIN_FIRE_FREQ = 100; // frames
    private final int ENEMY_BRAIN_UPDATE_SPACING = 10; // frames between updates (with some noise)
    
    // ZOMBIE
    
    private final int   ENEMY_ZOMBIE_SPEED = 10;
    private final int   ENEMY_ZOMBIE_RADIUS = 10;
    private final int   ENEMY_ZOMBIE_UPDATE_FREQ = 2; // number of frames between updates
    private final float ENEMY_ZOMBIE_HEURISTIC_FACTOR = 0.2f; // 
    private final int   ENEMY_ZOMBIE_TRACER_FADE = 3; // higher means slower fade
    private final int   MAX_ZOMBIE_TRACERS = 32;
    
    // TANK
    
    private final int ENEMY_TANK_RADIUS = 24;
    private final int ENEMY_TANK_SPEED = 5;
    private final int ENEMY_TANK_UPDATE_FREQ = 4; // frames between movement and update
    private final int ENEMY_TANK_FIRE_FREQ = 5; // updates between shots fired
    private final int MAX_TANK_MISSLES = 16;
    
    // TANK MISSLE
    
    private final int TANK_MISSLE_SPEED = 8;
    private final int TANK_MISSLE_DURATION = 300;
    private final int TANK_MISSLE_RADIUS = 8;
    
    // -- POINTS --
    
    private final int ENEMY_GEN_POINTS = 100;
    private final int ENEMY_ROBOT_POINTS = 10;
    private final int ENEMY_BRAIN_POINTS = 50;
    private final int ENEMY_TANK_POINTS = 100;
    private final int ENEMY_TANK_GEN_POINTS = 100;
    private final int POWERUP_POINTS_SCALE = 100;
    private final int POWERUP_BONUS_POINTS_MAX = 50;
    
    // -- PEOPLE --
    
    private final int MAX_PEOPLE = 32;
    private final int PEOPLE_NUM_TYPES = 3;
    private final int PEOPLE_RADIUS = 10;
    private final int PEOPLE_SPEED = 4;
    private final int PEOPLE_START_POINTS = 500;
    private final int PEOPLE_INCREASE_POINTS = 100; // * game_level
    private final int PEOPLE_MAX_POINTS = 10000;
    
    // -- MESSGAES --
    
    private final int THROBBING_SPEED = 2; // Affects throbbing_white. higher is slower.
    
    private final int MESSAGE_FADE = 3; // Affects instaMsgs, higher is slower
    private final int MESSAGES_MAX = 4; // max number of inst msgs
    
    // -- lava pit --
    
    private final int ENEMY_LAVA_CLOSED_FRAMES = 30;
    private final int ENEMY_LAVA_DIM = 64;
    private final int ENEMY_JAWS_DIM = 32;
    
    private final int DIFFICULTY_EASY = 0;
    private final int DIFFICULTY_MEDIUM = 1;
    private final int DIFFICULTY_HARD = 2;
    
    // -- BUTTONS ON INTRO SCREEN --
    
    enum Button {
        Classic,
        RoboCraze,
        Easy,
        Medium,
        Hard,
        START
    }
    
    private final int BUTTONS_NUM = Button.values().length;
    private final int BUTTON_WIDTH = 130;
    private final int BUTTON_HEIGHT = 40;
    
    // for other info on intro screen
    private final int TEXT_PADDING = 5; // pixels between hud text lines and border
    
    // -- POWERUPS --
    
    // ENUM
    private final int POWERUP_SUPER_SPEED = 0;
    private final int POWERUP_GHOST = 1;
    private final int POWERUP_BARRIER = 2;
    private final int POWERUP_MEGAGUN = 3;
    private final int POWERUP_HULK = 4;
    private final int POWERUP_BONUS_POINTS = 5;
    private final int POWERUP_BONUS_PLAYER = 6; 
    private final int POWERUP_KEY = 7;
    
    private final int POWERUP_NUM_TYPES = 8; // MUST BE LAST!
    
    // must be NUM_POWERUP_TYPES elems
    private final int[] POWERUP_CHANCE = { 
            2, 
            1,
            2, 
            2, 
            1, 
            4, 
            1,
            3
    }; 
    
    // The first letter is used as the indicator, unless special
    // handling is associated with a powerup
    // (as of this writing: POWERUP_BONUS_PLAYER)
    private final String[] powerup_names = { 
            "SPEEEEEEED",
            "GHOST", 
            "BARRIER", 
            "MEGAGUN", 
            "HULK MODE!", 
            "$$", 
            "EXTRA MAN!",
            "KEY"
    };
    
    private String getPowerupTypeString(int type) {
        return powerup_names[type];
    }
    
    private final int MAX_POWERUPS = 8;
    private final int POWERUP_MAX_DURATION = 500;
    private final int POWERUP_RADIUS = 10;
    private final int POWERUP_CHANCE_BASE = 1000;
    private final int POWERUP_CHANCE_ODDS = 10; // 10+currentLevel in 1000
    
    // Rubber Walls
    private final float RUBBER_WALL_MAX_FREQUENCY = 0.15f;
    private final float RUBBER_WALL_FREQENCY_INCREASE_MISSLE = 0.03f;
    private final float RUBBER_WALL_FREQENCY_COOLDOWN = 0.002f;
    
    // chance
    
    // -- STATIC FIELD --
    // TODO: Why cant I increase this number w/out wierd results?
    private final int   STATIC_FIELD_SECTIONS = 8;
    
    private final float STATIC_FIELD_COS_T = CMath.cosine(360.0f / STATIC_FIELD_SECTIONS);
    
    private final float STATIC_FIELD_SIN_T = CMath.sine(360.0f / STATIC_FIELD_SECTIONS);
    
    // number of repeated shots from megagun required to break wall
    private final int WALL_NORMAL_HEALTH = 30;
    private final int WALL_BREAKING_MAX_RECURSION = 3;
    
    // when a megagun hits an electric wall, the wall is disabled for some time
    private final int WALL_ELECTRIC_DISABLE_FRAMES = 200;
    
    private final int WALL_TYPE_NONE            = 0;
    private final int WALL_TYPE_NORMAL          = 1; // normal wall 
    private final int WALL_TYPE_ELECTRIC        = 2; // electric walls cant be destroyed? (temp disabled)
    private final int WALL_TYPE_INDESTRUCTABLE  = 3; // used for perimiter
    private final int WALL_TYPE_PORTAL          = 4; // teleport to other portal walls
    private final int WALL_TYPE_RUBBER          = 5; // ?
    private final int WALL_TYPE_DOOR            = 6; // need a key too open
    private final int WALL_TYPE_PHASE_DOOR      = 7; // door opens and closes at random intervals
    private final int WALL_NUM_TYPES            = 8; // MUST BE LAST!

    private final float DARKEN_AMOUNT = 0.2f;
    private final float LIGHTEN_AMOUNT = 0.2f;
    
    private int [] getWallChanceForLevel() {
        return new int [] {
                0, 
                80,// - this.game_level,
                2 + this.game_level, 
                10 + this.game_level, 
                5 + game_level, 
                5 + game_level, 
                0 + game_level, 
                0   
        };
    }
    
    private final String [] wall_type_strings = {
            "NONE",
            "Normal",
            "Electric",
            "Indestructable",
            "Portal",
            "Rubber",
            "Door",
            "Phase Door",
    };
    
    private String getWallTypeString(int type) {
        return wall_type_strings[type];
    }
    
    private final int DOOR_STATE_LOCKED = 0;
    private final int DOOR_STATE_CLOSED = 1;
    private final int DOOR_STATE_CLOSING = 2;
    private final int DOOR_STATE_OPENING = 3;
    private final int DOOR_STATE_OPEN = 4;
    
    private final int DOOR_SPEED_FRAMES = 50;
    private final float DOOR_SPEED_FRAMES_INV = 1.0f/DOOR_SPEED_FRAMES;
    
    private final int DOOR_OPEN_FRAMES  = 100;
    private final float DOOR_OPEN_FRAMES_INV = 1.0f/DOOR_OPEN_FRAMES;
    
    private AColor DOOR_COLOR;
    private final int DOOR_THICKNESS = 2;
    
    private final String [] door_state_strings = {
            "LOCKED",
            "CLOSED",
            "CLOSING",
            "OPENING",
            "OPEN"
    };
    
    private String getDoorStateString(int state) {
        return door_state_strings[state];
    }
    
    // ---------------------------------------------------------//
    // GLOBAL DATA //
    // ---------------------------------------------------------//

    // maze data.  Each cell is a bit flag
    // a cell is a bitflag that provides info on what walls remain for that
    // cell. adjacent cells actually have 2 walls between them, so breaking
    // down a wall means removing opposing walls from the cells. For
    // example,
    // if cell A is directly above cell B, the the SOUTH wall of cell A
    // overlaps the NORTH wall of cell B. To create a path between these 
    // cells means removing the SOUTH cell from A and the NORTH cell from B.
    
    // wall flags organized in CCW order, this is important DO NOT
    // REORGANIZE!
    final int WALL_NORTH = 1;
    final int WALL_EAST = 2;
    final int WALL_SOUTH = 4;
    final int WALL_WEST = 8;
    
    private final int[][] maze_cells = new int[MAZE_NUMCELLS_X][];
    private final int[] maze_verts_x = new int[MAZE_NUM_VERTS]; // array of x components
    private final int[] maze_verts_y = new int[MAZE_NUM_VERTS]; // array of y components
    private final Wall[][] wall_lookup = new Wall[MAZE_NUM_VERTS][];
    
    // Rectangle of visible maze
    private int screen_x = 0;
    private int screen_y = 0;
    
    // rectangle of maze dimension
    private int verts_min_x = 0;
    
    private int verts_min_y = 0;
    
    private int verts_max_x = MAZE_WIDTH;
    
    private int verts_max_y = MAZE_HEIGHT;
    
    // position of ending 'spot' or goal
    private int end_x = 0;
    
    private int end_y = 0;
    
    // PLAYER DATA
    
    private final int MAX_PLAYERS = 8;
    
    Player [] players = new Player[MAX_PLAYERS];
    int num_players = 0;
    int this_player = 0;
    
    private Player getPlayer() {
        assert(this_player < num_players);
        return players[this_player];
    }

    private int high_score = 0;

    // down on mouse button and false
    // when released
    
    // GAME STATE --------------------------
    
    private int game_state = GAME_STATE_INTRO; // current game state
    
    private int game_start_frame = 0; // the frame the match started
    
    private int game_type = GAME_TYPE_ROBOCRAZE; // current game type
    
    private int game_level = 1; // current level
    
    private boolean game_visibility = false; // experimental functionality     
    
    private int total_enemies = 0; // number of non-thugs remaining
    
    // set by collisionScanCircle/Line
    private int collision_info_v0 = -1;
    private int collision_info_v1 = -1; 
    private Wall collision_info_wallinfo = null;
    
    // General use arrays
    private final float[] vec = new float[2];
    
    private final float[] transform = new float[4];
    
    private final int[] int_array = new int[4];
    
    private final MissleFloat[] enemy_missle = new MissleFloat[MAX_ENEMY_MISSLES];
    
    private final MissleInt[] tank_missle = new MissleInt[MAX_TANK_MISSLES];
    
    private final MissleSnake[] snake_missle = new MissleSnake[MAX_SNAKE_MISSLES];
    
    private final Powerup[] powerups = new Powerup[MAX_POWERUPS];
    
    // ENEMIES ----------------------------
    
    private final int[] enemy_x = new int[MAX_ENEMIES];
    
    private final int[] enemy_y = new int[MAX_ENEMIES];
    
    private final int[] enemy_type = new int[MAX_ENEMIES];
    
    private final int[] enemy_next_update = new int[MAX_ENEMIES];
    
    private final int [] enemy_spawned_frame = new int[MAX_ENEMIES];
    
    private final boolean [] enemy_killable = new boolean[MAX_ENEMIES]; 

    private final int[] enemy_radius = new int[ENEMY_INDEX_NUM];

    private int enemy_robot_speed;
    
    // EXPLOSIN EFFECTS --------------------
    
    Particle [] particles = new Particle[MAX_PARTICLES];
    
    // MSGS that drift away ---------------------------
    
    private final int[] msg_x = new int[MESSAGES_MAX];
    
    private final int[] msg_y = new int[MESSAGES_MAX];
    
    private final String[] msg_string = new String[MESSAGES_MAX];
    
    private final AColor[] msg_color = new AColor[MESSAGES_MAX];
    
    // PEOPLE ------------------------------
    
    private final int[] people_x = new int[MAX_PEOPLE];
    
    private final int[] people_y = new int[MAX_PEOPLE];
    
    private final int[] people_state = new int[MAX_PEOPLE]; // 0 = unused, 1 =
    
    private final int[] people_type = new int[MAX_PEOPLE];
    
    // north, 2 = east, 3 =
    // south, 4 = west
    
    private int people_points = PEOPLE_START_POINTS;
    
    private int people_picked_up = 0;
    
    // Zombie tracers ----------------------
    
    private final int[] zombie_tracer_x = new int[MAX_ZOMBIE_TRACERS];
    
    private final int[] zombie_tracer_y = new int[MAX_ZOMBIE_TRACERS];
    
    private final AColor[] zombie_tracer_color = new AColor[MAX_ZOMBIE_TRACERS];
    
    // Player tracers ----------------------
    
    // Other lookups
    
    // Maps N=0,E,S,W to a 4 dim array lookup
    private final int[] move_dx = { 0, 1, 0, -1 };
    
    private final int[] move_dy = { -1, 0, 1, 0 };
    
    private final int[] move_diag_dx = { 1, -1, 1, -1 };
    
    private final int[] move_diag_dy = { -1, 1, -1, 1 };
    
    private AColor throbbing_white;// = AColor.WHITE;
    
    private int throbbing_dir = 0; // 0 == darker(), 1 == lighter()
    
    private AColor insta_msg_color;
    
    private String insta_msg_str = null;
    
    // OPTIMIZATION - Use better Add / Remove object algorithm to get O(1) time
    // vs O(n) time
    
    private int num_enemies = 0;
    
    private int num_enemy_missles = 0;
    
    private int num_tank_missles = 0;
    
    private int num_snake_missles = 0;
    
    private int num_people = 0;
    
    private int num_zombie_tracers = 0;
    
    private int num_msgs = 0;
    
    private int num_particles = 0;
    
    private int num_powerups = 0;
    
    private final int[] button_x = new int[BUTTONS_NUM];
    
    private final int[] button_y = new int[BUTTONS_NUM];
    
    private int button_active = -1; // -1 == none, 1 == button 1, 2 == button 2,
    
    // ...
    
    private int difficulty = DIFFICULTY_EASY; // Easy == 0, Med == 1, Hard == 2

    private int screen_width, screen_height;
    private int frame_number;
    
    private AGraphics G;

    private void initColors(AGraphics g) {
        DOOR_COLOR = g.CYAN;
        throbbing_white = g.WHITE;
    }
    
    private final ILocalization localization;
    
    public Robotron(ILocalization localization) {
        Utils.assertTrue(localization != null);
        this.localization = localization;
        initTables();
        buildAndPopulateLevel();
    }
    
    public void setDimension(int screenWidth, int screenHeight) {
        this.screen_height = screenHeight;
        this.screen_width = screenWidth;
        setIntroButtonPositionAndDimension();
        
    }

    private int getFrameNumber() {
        return frame_number;
    }
    
    // DEBUG Drawing stuff
    
    // final int [] debug_player_primary_verts = new int[5]; // used by
    
    // MEMBERS
    // //////////////////////////////////////////////////////////////////////
    
    // -----------------------------------------------------------------------------------------------
    // draw any debug related stuff
    private void drawDebug(AGraphics g) {
        int x, y;
        
        Player player = getPlayer();
        
        //g.glCsetColor(AColor.GREEN);
        g.setColor(g.GREEN);
        final int [] verts = new int[5];
        
        if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
            computePrimaryVerts(player.x, player.y, verts);
            
            g.setColor(g.GREEN);
            int radius = 10;
            for (int i = 0; i < verts.length; i++) {
                int v = verts[i];
                if (v<0 || v>=MAZE_NUM_VERTS)
                    continue;
                x = maze_verts_x[v] - screen_x;
                y = maze_verts_y[v] - screen_y;
                g.drawDisk(x, y, radius);
                radius = 3;
                g.drawString(String.valueOf(v), x+15, y+15);
            }
            
            g.setColor(g.ORANGE);
            computePrimaryQuadrant(player.x, player.y, verts);
            for (int i=0; i<4; i++) {
                int v = verts[i];
                if (v <0 || v>=MAZE_NUM_VERTS)
                    continue;
                x = maze_verts_x[v] - screen_x;
                y = maze_verts_y[v] - screen_y;
                g.drawDisk(x,y,5);
            }
        
            g.setColor(g.GREEN);
            for (int i = 0; i < maze_verts_x.length; i++) {
                x = maze_verts_x[i];
                y = maze_verts_y[i];
                
                if (isOnScreen(x, y)) {
                    x -= screen_x;
                    y -= screen_y;
                    //g.drawOval(x - 2, y - 2, 4, 4);
                    g.drawString(String.valueOf(i), x+10, y+10);
                }
            }
        
            y = 0;
            g.setColor(g.BLUE);
            for (int i = 0; i < MAZE_NUMCELLS_Y; i++) {
                x = 0;
                for (int j = 0; j < MAZE_NUMCELLS_X; j++) {
                    g.drawRect(x - screen_x, y - screen_y, MAZE_CELL_WIDTH, MAZE_CELL_HEIGHT);
                    x += MAZE_CELL_WIDTH;
                }
                y += MAZE_CELL_HEIGHT;
            }
            drawDebugWallInfo(g);
        }
        
        if (isDebugEnabled(Debug.DRAW_PLAYER_INFO)) {
            /*
            g.setColor(AColor.WHITE);
            int px = player.x - screen_x + PLAYER_RADIUS * 2;
            int py = player.y - screen_y - 2; 
            int mx = getMouseX();
            int my = getMouseY();
            String msg = "(" + player.x + ", " + player.y + ")" 
                       + "\n<" + player.dx + ", " + player.dy + ">";
            msg += "(" + mx + ", " + my + ")";
            g.drawJustifiedString(mx, my, Justify.LEFT, Justify.CENTER, msg);
*/
            computePrimaryVerts(player.x, player.y, verts);
            
            g.setColor(g.GREEN);
            int radius = 10;
            for (int i = 0; i < verts.length; i++) {
                int v = verts[i];
                if (v<0 || v>=MAZE_NUM_VERTS)
                    continue;
                x = maze_verts_x[v] - screen_x;
                y = maze_verts_y[v] - screen_y;
                g.drawDisk(x, y, radius);
                radius = 5;
                g.drawString(String.valueOf(v), x+15, y+15);
            }
            g.setColor(g.ORANGE);
            computePrimaryQuadrant(player.x, player.y, verts);
            for (int i=0; i<4; i++) {
                int v = verts[i];
                if (v <0 || v>=MAZE_NUM_VERTS)
                    continue;
                x = maze_verts_x[v] - screen_x;
                y = maze_verts_y[v] - screen_y;
                g.drawDisk(x,y,5);
            }

        }
        
        if (isDebugEnabled(Debug.PATH) && player.path.size() > 1) {
            int [] s = player.path.get(0);
            for (int i=1; i<player.path.size(); i++) {
                int [] n = player.path.get(i);
                int x0 = s[0] * this.MAZE_CELL_WIDTH + MAZE_CELL_WIDTH/2 - screen_x;
                int y0 = s[1] * this.MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT/2 - screen_y;
                int x1 = n[0] * this.MAZE_CELL_WIDTH + MAZE_CELL_WIDTH/2 - screen_x;
                int y1 = n[1] * this.MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT/2 - screen_y;
                s = n;
                g.setColor(g.GREEN);
                g.drawLine(x0, y0, x1, y1, 3);
            }
            
        }
        
        g.setColor(g.WHITE);
        //String msg = "frame: " + getFrameNumber();
        //g.drawJustifiedString(5, screen_height-5, Justify.LEFT, Justify.BOTTOM, msg);
        float dx = g.drawStringLine(5, screen_height-5, Justify.LEFT, "frame: ");
        this.drawNumberString(g, Math.round(5 + dx), Math.round(screen_height-5), Justify.LEFT, getFrameNumber());

    }

    // -----------------------------------------------------------------------------------------------
    private int getPlayerSpeed(Player player) {      
        int speed = PLAYER_SPEED;       
        speed = Utils.clamp(speed-getNumSnakesAttached(), 1, 100);
        if (player.powerup == POWERUP_SUPER_SPEED)
            speed += PLAYER_SUPER_SPEED_BONUS;
        if (isHulkActiveCharging(player))
            speed += PLAYER_HULK_CHARGE_SPEED_BONUS;
        return speed;
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getNumSnakesAttached() {
        int num = 0;
        for (int i = 0; i < this.num_snake_missles; i++) {
            if (snake_missle[i].state == SNAKE_STATE_ATTACHED)
                num++;
        }
        return num;
    }
    
    // -----------------------------------------------------------------------------------------------
    private int addPowerup(int x, int y, int type) {
        
        Powerup p = null;
        if (num_powerups < MAX_POWERUPS) {
            if (Utils.DEBUG_ENABLED)
                Utils.println("addPowerup x[" + x + "] y[" + y + "] type [" + this.getPowerupTypeString(type) + "]");
            p = powerups[num_powerups++];
        } else {
            int index = Utils.rand() % MAX_POWERUPS;
            if (Utils.DEBUG_ENABLED)
                Utils.println("replace Powerup [" + index + "] with x[" + x + "] y[" + y + "] type [" + this.getPowerupTypeString(type) + "]");
            p = powerups[index];
        }
        
        p.init(x, y, Utils.randFloatX(1), Utils.randFloatX(1), 0, type);
        return num_powerups;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void removePowerup(int index) {
        if (index < num_powerups - 1) {
            Powerup p0 = powerups[index];
            Powerup p1 = powerups[num_powerups - 1];
            p0.copy(p1);
        }
        num_powerups -= 1;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawPowerup(AGraphics g, int type, int x, int y) {
        if (type == POWERUP_KEY) {
            final int dim = 20;
            g.setColor(g.WHITE);
            g.drawImage(imageKey, x-dim/2, y-dim/2, dim, dim);
        } else {
            g.setColor(throbbing_white);
            int r = POWERUP_RADIUS + (getFrameNumber() % 12) / 4;
            g.drawCircle(x, y, r);//(x - r, y - r, r * 2, r * 2);
            
            g.setColor(g.RED);
            if (type == POWERUP_BONUS_PLAYER) {
                this.drawStickFigure(g, x, y, POWERUP_RADIUS);
            } else {
                //int h = Utils.getTextHeight();//g.getFontMetrics().getHeight();
                char c = getPowerupTypeString(type).charAt(0);
                //int w = Utils.getTextWidth("" + c);//Utils.cog.getFontMetrics().charWidth(c);
                //g.drawString(String.valueOf(c), x - w / 2 + 1, y + h / 2 - 3);
                g.drawJustifiedString(x, y, Justify.CENTER, Justify.CENTER, String.valueOf(c));
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawPowerups(AGraphics g) {
        for (int i = 0; i < num_powerups; i++) {
            Powerup p = powerups[i];
            
            final int x = Math.round(p.x - screen_x);
            final int y = Math.round(p.y - screen_y);
            
            drawPowerup(g, p.type, x, y);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updatePowerups() {
        for (int i = 0; i < num_powerups;) {
            Powerup p = powerups[i];
            
            if (!isOnScreen(Math.round(p.x), Math.round(p.y))) {
                removePowerup(i);
                continue;
            }
            
            if (p.duration > POWERUP_MAX_DURATION) {
                removePowerup(i);
                continue;
            }
            
            for (int ii=0; ii<num_players; ii++) {
                Player player = players[ii];
                // see if the player is picking me up
                float dx = player.x - p.x;
                float dy = player.y - p.y;
                float min = getPlayerRadius(player) + POWERUP_RADIUS;
                float len2 = dx * dx + dy * dy;
                
                if (len2 < min * min) {
                    
                    int msgX = player.x + Utils.randRange(-30, 30);
                    int msgY = player.y + Utils.randRange(-30, 30);
                    
                    this.addMsg(msgX, msgY, getPowerupTypeString(p.type));
                    
                    switch (p.type) {
                    case POWERUP_BONUS_PLAYER:
                        player.lives += 1;
                        break;
                        
                    case POWERUP_BONUS_POINTS:
                        int points = Utils.randRange(1, this.POWERUP_BONUS_POINTS_MAX) * POWERUP_POINTS_SCALE;
                        this.addPoints(player, points);
                        addMsg(msgX + 20, msgY, String.valueOf(points));
                        break;
                        
                    case POWERUP_KEY:
                        player.keys ++;
                        addPlayerMsg(player, "Found a key!");
                        break;
                        
                    default:
                        setPlayerPowerup(player, p.type);
                    break;
                    }
                    removePowerup(i);
                    continue;
                }
                
                i++;
            }
        }
        
        if (getFrameNumber() % (1000-(game_level*100)) == 0) {
            for (int i=0; i<num_players; i++)
                addRandomPowerup(players[i].x, players[i].y, 100);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void addRandomPowerup(int x, int y, int radius) {
        
        Utils.println("addRandomPowerup x[" + x + "] y[" + y + "] radius + [" + radius + "]");
        
        int powerup = Utils.chooseRandomFromSet(this.POWERUP_CHANCE);
            
        // final int range = radius * 10;
        final int minRange = radius * 2;
        final int maxRange = radius * 20;
            
        int maxTries = 100;
        
        for (int i=0; i<maxTries; i++) {
        
            int dx = Utils.randRange(minRange, maxRange);
            int dy = Utils.randRange(minRange, maxRange);
                
            if (Utils.flipCoin())
                dx = -dx;
            if (Utils.flipCoin())
                dy = -dy;
                
            int newx = x + dx;
            int newy = y + dy;
                
            if (!this.isInMaze(newx, newy))
                continue;

            if (this.collisionScanCircle(newx, newy, this.POWERUP_RADIUS+5))
                continue;
            
            this.addPowerup(newx, newy, powerup);
            break;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void setPlayerPowerup(Player player, int type) {
        player.powerup = type;
        player.powerup_duration = 0;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the tank missle at index
    private void removeTankMissle(int index) {
        num_tank_missles--;
        MissleInt m1 = tank_missle[index];
        MissleInt m2 = tank_missle[num_tank_missles];
        m1.copy(m2);
    }
    
    // -----------------------------------------------------------------------------------------------
    // append a tank missle
    private void addTankMissle(int x, int y) {
        if (num_tank_missles == MAX_TANK_MISSLES)
            return;
        
        MissleInt m = tank_missle[num_tank_missles];
        
        int index = Utils.randRange(0, 3);
        m.init(x, y, TANK_MISSLE_SPEED * move_diag_dx[index], TANK_MISSLE_SPEED * move_diag_dy[index], TANK_MISSLE_DURATION);
        
        num_tank_missles++;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the zombie tracer at index
    private void removeZombieTracer(int index) {
        num_zombie_tracers--;
        zombie_tracer_x[index] = zombie_tracer_x[num_zombie_tracers];
        zombie_tracer_y[index] = zombie_tracer_y[num_zombie_tracers];
        zombie_tracer_color[index] = zombie_tracer_color[num_zombie_tracers];
    }
    
    // -----------------------------------------------------------------------------------------------
    // append a zombie tracer
    private void addZombieTracer(int x, int y) {
        if (num_zombie_tracers == MAX_ZOMBIE_TRACERS)
            return;
        zombie_tracer_x[num_zombie_tracers] = x;
        zombie_tracer_y[num_zombie_tracers] = y;
        zombie_tracer_color[num_zombie_tracers] = G.WHITE;
        
        num_zombie_tracers++;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the zombie tracer at index
    private void removePlayerTracer(Player player, int index) {
        player.num_tracers--;
        player.tracer_x[index] = player.tracer_x[player.num_tracers];
        player.tracer_y[index] = player.tracer_y[player.num_tracers];
        player.tracer_color[index] = player.tracer_color[player.num_tracers];
        player.tracer_dir[index] = player.tracer_dir[player.num_tracers];
    }
    
    // -----------------------------------------------------------------------------------------------
    // append a zombie tracer
    private void addPlayerTracer(Player player, int x, int y, int dir, AColor color) {
        if (player.num_tracers == PLAYER_SUPERSPEED_NUM_TRACERS)
            return;
        player.tracer_x[player.num_tracers] = x;
        player.tracer_y[player.num_tracers] = y;
        player.tracer_color[player.num_tracers] = color;
        player.tracer_dir[player.num_tracers] = dir;
        
        player.num_tracers++;
    }
    
    // -----------------------------------------------------------------------------------------------
    // replace the snake at index with tail elem, and decrement
    private void removeSnakeMissle(int index) {
        Utils.println("Removing snake missle [" + index + "]");
        num_snake_missles--;
        MissleSnake m0 = snake_missle[index];
        MissleSnake m1 = snake_missle[num_snake_missles];
        m0.copy(m1);
    }
    
    // -----------------------------------------------------------------------------------------------
    // append a snake missle at x,y
    private int addSnakeMissle(int x, int y) {
        if (num_snake_missles == MAX_SNAKE_MISSLES)
            return -1;
        MissleSnake m = snake_missle[num_snake_missles];
        m.init(x, y, 0, SNAKE_STATE_CHASE);
        m.dir[0] = m.dir[1] = Utils.randRange(0, 3);
        
        // init the section with -1 as a flag to not draw
        for (int i = 1; i < SNAKE_MAX_SECTIONS; i++) {
            m.dir[i] = -1;
        }
        return num_snake_missles++;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Do colliison scan and motion on all missles
    private void updateSnakeMissles(Player player) {
        final int framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED;
        final int playerRadius = getPlayerRadius(player);
        // cache player position
        final int px = player.x - playerRadius;
        final int py = player.y - playerRadius;
        final int pw = playerRadius * 2;
        final int ph = playerRadius * 2;
        
        for (int i = 0; i < num_snake_missles;) {
            MissleSnake ms = this.snake_missle[i];
            
            if (ms.duration > SNAKE_DURATION) {
                killSnakeMissle(i);
            }
            
            ms.duration++;
            float factor = SNAKE_HEURISTIC_FACTOR;
            
            switch (ms.state) {
            
            case SNAKE_STATE_ATTACHED:
            case SNAKE_STATE_CHASE:
                // advance the head
                int dx = this.move_dx[ms.dir[0]] * SNAKE_SPEED;
                int dy = this.move_dy[ms.dir[0]] * SNAKE_SPEED;
                
                ms.x += dx;
                ms.y += dy;
                
                final int touchSection = collisionMissleSnakeRect(ms, px, py, pw, ph);
                
                if (ms.state == SNAKE_STATE_CHASE) {
                    if (touchSection == 0 && playerHit(player, HIT_TYPE_SNAKE_MISSLE, i)) {
                        Utils.println("Snake [" + i + "] got the player");
                        ms.state = SNAKE_STATE_ATTACHED;
                    }                       
                } else {
                    if (touchSection < 0 || !playerHit(player, HIT_TYPE_SNAKE_MISSLE, i)) {
                        Utils.println("Snake [" + i + "] lost hold of the player");
                        ms.state = SNAKE_STATE_CHASE;
                    }
                }
                
                if (ms.duration % framesPerSection == 0) {
                    // make a new section
                    
                    // shift all directions over
                    for (int d = this.SNAKE_MAX_SECTIONS - 1; d > 0; d--) {
                        ms.dir[d] = ms.dir[d - 1];
                    }
                    
                    // choose a new direction that is not a reverse (it looks
                    // bad
                    // when a snake 'backsup' on itself)
                    final int reverseDir = (ms.dir[0] + 2) % 4;
                    int newDir = enemyDirectionHeuristic(ms.x, ms.y, factor);
                    if (newDir == reverseDir) {
                        // choose right or left
                        if (Utils.flipCoin()) {
                            newDir = (newDir + 1) % 4;
                        } else {
                            newDir = (newDir + 3) % 4;
                        }
                    }
                    
                    ms.dir[0] = newDir;
                    
                }
                break;
                
            case SNAKE_STATE_DYING:
                // shift elems over
                for (int ii = 0; ii < MAX_SNAKE_MISSLES - 1; ii++)
                    ms.dir[ii] = ms.dir[ii + 1];
                ms.dir[MAX_SNAKE_MISSLES - 1] = -1;
                
                if (ms.dir[0] == -1) {
                    removeSnakeMissle(i);
                    continue;
                }
                break;
                
                //case SNAKE_STATE_ATTACHED:
                //  if (this.collisionMissleSnakeRect(ms, px, py, pw, ph) >= 0) {
                //      ms.state = SNAKE_STATE_CHASE;
                //  }
                //  break;
                
            default:
                Utils.unhandledCase(ms.state);
            break;
            }
            
            i++;
            
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // return the section that touched or -1 for no collision
    private int collisionMissleSnakeRect(MissleSnake ms, float x, float y, float w, float h) {
        final int framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED;
        
        final int headLen = ms.duration % framesPerSection;
        final int tailLen = framesPerSection - headLen;
        
        int x0 = ms.x;
        int y0 = ms.y;
        
        for (int i = 0; i < SNAKE_MAX_SECTIONS && ms.dir[i] >= 0; i++) {
            int len = SNAKE_SECTION_LENGTH;
            if (i == 0)
                len = headLen;
            else if (i == SNAKE_MAX_SECTIONS - 1)
                len = tailLen;
            final int rDir = (ms.dir[i] + 2) % 4;
            final int x1 = x0 + move_dx[rDir] * len;
            final int y1 = y0 + move_dy[rDir] * len;
            
            final int xx = Math.min(x0, x1);
            final int yy = Math.min(y0, y1);
            final int ww = Math.abs(x0 - x1);
            final int hh = Math.abs(y0 - y1);
            
            if (Utils.isBoxesOverlapping(x, y, w, h, xx, yy, ww, hh)) {
                // Utils.println("Snake Collision [" + i + "]");
                return i;
            }
            
            x0 = x1;
            y0 = y1;
        }
        return -1;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw all the 'snakes'
    private void drawSnakeMissles(AGraphics g) {
        
        final int framesPerSection = SNAKE_SECTION_LENGTH / SNAKE_SPEED;
        
        for (int i = 0; i < num_snake_missles; i++) {
            final MissleSnake m = snake_missle[i];
            if (!isOnScreen(m.x, m.y))
                continue;
            
            int x0 = m.x - screen_x;
            int y0 = m.y - screen_y;
            
            // draw the first section
            int d = (m.dir[0] + 2) % 4; // get reverse direction
            int frames = m.duration % framesPerSection;
            
            int x1 = x0 + move_dx[d] * SNAKE_SPEED * frames;
            int y1 = y0 + move_dy[d] * SNAKE_SPEED * frames;
            
            g.setColor(g.BLUE);
            drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, 0);
            
            // draw the middle sections
            for (int ii = 1; ii < SNAKE_MAX_SECTIONS - 1 && m.dir[ii] >= 0; ii++) {
                x0 = x1;
                y0 = y1;
                d = (m.dir[ii] + 2) % 4;
                x1 = x0 + move_dx[d] * SNAKE_SPEED * framesPerSection;
                y1 = y0 + move_dy[d] * SNAKE_SPEED * framesPerSection;
                drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, ii);
            }
            
            // draw the tail if we are at max sections
            d = m.dir[SNAKE_MAX_SECTIONS - 1];
            if (d >= 0) {
                d = (d + 2) % 4;
                frames = framesPerSection - frames;
                x0 = x1;
                y0 = y1;
                x1 = x0 + move_dx[d] * SNAKE_SPEED * frames;
                y1 = y0 + move_dy[d] * SNAKE_SPEED * frames;
                drawBar(g, x0, y0, x1, y1, SNAKE_THICKNESS, SNAKE_MAX_SECTIONS - 1);
            }
            
            // draw the head
            g.setColor(throbbing_white);
            g.drawFilledRect(m.x - screen_x - SNAKE_THICKNESS, m.y - screen_y - SNAKE_THICKNESS, SNAKE_THICKNESS * 2, SNAKE_THICKNESS * 2);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void killSnakeMissle(int index) {
        this.snake_missle[index].state = SNAKE_STATE_DYING;
    }
    
    // -----------------------------------------------------------------------------------------------
    // add the maximum number of people randomly around the maze (but not on an
    // edge)
    private void addPeople() {
        for (int i = 0; i < MAX_PEOPLE; i++) {
            while (true) {
                people_x[i] = Utils.randRange(verts_min_x + (MAZE_VERTEX_NOISE * 3), verts_max_x - (MAZE_VERTEX_NOISE * 3));
                people_y[i] = Utils.randRange(verts_min_y + (MAZE_VERTEX_NOISE * 3), verts_max_y - (MAZE_VERTEX_NOISE * 3));
                if (!collisionScanCircle(people_x[i], people_y[i], PEOPLE_RADIUS * 2))
                    break;
            }
            people_state[i] = Utils.randRange(1, 4);
            people_type[i] = Utils.rand() % PEOPLE_NUM_TYPES;
        }
        num_people = MAX_PEOPLE;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the people at an iindex
    private void removePeople(int index) {
        num_people--;
        people_x[index] = people_x[num_people];
        people_y[index] = people_y[num_people];
        people_state[index] = people_state[num_people];
        people_type[index] = people_type[num_people];
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove enemy tank at index
    private void removeEnemy(int index) {
        num_enemies--;
        enemy_x[index] = enemy_x[num_enemies];
        enemy_y[index] = enemy_y[num_enemies];
        enemy_type[index] = enemy_type[num_enemies];
        enemy_next_update[index] = enemy_next_update[num_enemies];
        enemy_spawned_frame[index] = enemy_spawned_frame[num_enemies];
        enemy_killable[index] = enemy_killable[num_enemies];
    }
    
    // -----------------------------------------------------------------------------------------------
    // Add an enemy. If the table is full, replace the oldest GUY.
    private void addEnemy(int x, int y, int type, boolean killable) {
        int best = -1;
        int oldest = getFrameNumber();
        
        if (num_enemies == MAX_ENEMIES) {
            // look for oldest
            for (int i = 0; i < MAX_ENEMIES; i++) {
                if (isOnScreen(enemy_x[i], enemy_y[i]))
                    continue; // dont swap with guys on the screen
                
                if (enemy_type[i] >= ENEMY_INDEX_ROBOT_N && enemy_type[i] <= ENEMY_INDEX_ROBOT_W && enemy_next_update[i] < oldest) {
                    oldest = enemy_next_update[i];
                    best = i;
                }
            }
        } else {
            best = num_enemies++;
        }
        
        if (best >= 0) {
            enemy_type[best] = type;
            enemy_x[best] = x;
            enemy_y[best] = y;
            enemy_next_update[best] = getFrameNumber() + Utils.randRange(5, 10);
            enemy_spawned_frame[best] = getFrameNumber() + Utils.randRange(10,20);
            enemy_killable[best] = killable;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the tracer when > black and update fade away color
    private void updateAndDrawZombieTracers(AGraphics g) {
        
        boolean update = getFrameNumber() % ENEMY_ZOMBIE_TRACER_FADE == 0;
        
        for (int i = 0; i < num_zombie_tracers;) {
            if (!isOnScreen(zombie_tracer_x[i], zombie_tracer_y[i]) || zombie_tracer_color[i].equals(G.BLACK)) {
                removeZombieTracer(i);
                continue;
            }
            
            g.setColor(zombie_tracer_color[i]);
            drawStickFigure(g, zombie_tracer_x[i] - screen_x, zombie_tracer_y[i] - screen_y, ENEMY_ZOMBIE_RADIUS);
            
            if (update)
                zombie_tracer_color[i] = zombie_tracer_color[i].darkened(DARKEN_AMOUNT);
            i++;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the tracer when > black and update fade away color
    private void updateAndDrawPlayerTracers(Player player, AGraphics g) {
        
        // boolean update = getFrameNumber() % ENEMY_ZOMBIE_TRACER_FADE == 0;
        
        for (int i = 0; i < player.num_tracers;) {
            if (!isOnScreen(player.tracer_x[i], player.tracer_y[i]) || player.tracer_color[i].equals(g.BLACK)) {
                removePlayerTracer(player, i);
                continue;
            }
            
            this.drawPlayerBody(player, g, player.tracer_x[i] - screen_x, player.tracer_y[i] - screen_y, player.tracer_dir[i], player.tracer_color[i]);
            
            player.tracer_color[i] = player.tracer_color[i].darkened(DARKEN_AMOUNT);
            i++;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // this draws the collapsing indicator box a the beginning of play
    private void drawPlayerHighlight(Player player, AGraphics g) {
        int frame = getFrameNumber() - game_start_frame;
        
        int left = ((player.x - screen_x) * frame) / PLAYER_SPAWN_FRAMES;
        int top = ((player.y - screen_y) * frame) / PLAYER_SPAWN_FRAMES;
        int w = screen_width - ((screen_width * frame) / PLAYER_SPAWN_FRAMES);
        int h = screen_height - ((screen_height * frame) / PLAYER_SPAWN_FRAMES);
        
        g.setColor(g.RED);
        g.drawRect(left, top, w, h, 1);
    }
    
    // -----------------------------------------------------------------------------------------------
    // Add points to the player's score and update highscore and players lives
    private void addPoints(Player player, int amount) {
        int before = player.score / PLAYER_NEW_LIVE_SCORE;
        if ((player.score + amount) / PLAYER_NEW_LIVE_SCORE > before) {
            setInstaMsg("EXTRA MAN!");
            player.lives++;
        }
        player.score += amount;
        if (player.score > high_score)
            high_score = player.score;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Update the throbbing white text
    private void updateThrobbingWhite() {
        if (getFrameNumber() % THROBBING_SPEED == 0) {
            if (throbbing_dir == 0) {
                throbbing_white = throbbing_white.darkened(DARKEN_AMOUNT);
                if (throbbing_white.equals(G.BLACK))
                    throbbing_dir = 1;
            } else {
                throbbing_white = throbbing_white.lightened(LIGHTEN_AMOUNT);
                if (throbbing_white.equals(G.WHITE))
                    throbbing_dir = 0;
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the game over text
    private void drawGameOver(AGraphics g) {
        int x = screen_width / 2;
        int y = screen_height / 2;
        g.setColor(throbbing_white);
        g.drawJustifiedString(x, y, Justify.CENTER, localization.getString(ILocalization.StringID.GAME_OVER));
    }
    
    // -----------------------------------------------------------------------------------------------
    // Set the insta message
    private void setInstaMsg(String s) {
        insta_msg_color = G.YELLOW;
        insta_msg_str = s;
    }
    
    private int drawNumberString(AGraphics g, int x, int y, Justify hJust, int number) {
        String s = String.valueOf(number);
        for (int i=0; i<s.length(); i++) {
            float w = g.drawJustifiedString(x, y, hJust, String.valueOf(s.charAt(i)));
            x += Math.round(w);
        }
        return x;
    }
    
    
    // -----------------------------------------------------------------------------------------------
    // draw the score, high score, and number of remaining players
    private void drawPlayerInfo(Player player, AGraphics g) {
        int text_height = g.getTextHeight();
        // draw the score
        int x = TEXT_PADDING;
        int y = TEXT_PADDING;
        final int x0 = x;
        Justify hJust = Justify.LEFT;
        g.setColor(g.WHITE);
        x += g.drawStringLine(x, y, hJust, "Score    ");
        x += drawNumberString(g, x, y, hJust, player.score);
        y += text_height; x = x0;
        x += g.drawStringLine(x, y, hJust, "Lives  X ");
        x += drawNumberString(g, x, y, hJust, player.lives);
        y += text_height; x = x0;
        x += g.drawStringLine(x, y, hJust, "People X ");
        x += drawNumberString(g, x, y, hJust, people_picked_up);
        y += text_height; x = x0;
        x += g.drawStringLine(x, y, hJust, "Keys   X ");
        x += drawNumberString(g, x, y, hJust, player.keys);
        
        x = screen_width / 2;
        y = TEXT_PADDING;
        hJust = Justify.CENTER;
        x += g.drawStringLine(x, y, hJust, "High Score ");
        x += drawNumberString(g, x, y, hJust, high_score);
        x = screen_width - TEXT_PADDING;
        y = TEXT_PADDING;
        hJust = Justify.RIGHT;
        x += g.drawStringLine(x,  y, hJust, "Level   ");
        x -= 20;
        drawNumberString(g, x, y, hJust, game_level);
        
        // draw the instmsg
        if (insta_msg_str != null) {
            if (insta_msg_color.equals(g.BLACK)) {
                insta_msg_str = null;
            } else {
                g.setColor(insta_msg_color);
                g.drawString(insta_msg_str, TEXT_PADDING, (TEXT_PADDING + text_height) * 3);
                if (getFrameNumber() % 3 == 0)
                    insta_msg_color = insta_msg_color.darkened(DARKEN_AMOUNT);
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw the player exploding after being killed
    private void drawPlayerExploding(Player player, AGraphics g) {
        int x = player.x - screen_x + Utils.randRange(-5, 5);
        int y = player.y - screen_y + Utils.randRange(-5, 5);
        // drawPlayer(g, x, y, 2);
        drawPlayerBody(player, g, x, y, player.dir, g.RED);
        
        // outline the thing that hit the player with a blinking yellow circle
        if (getFrameNumber() % 40 < 20)
            return;
        
        g.setColor(g.YELLOW);
        
        int rad;
        switch (player.hit_type) {
        case HIT_TYPE_ENEMY:
            rad = getEnemyRadius(player.hit_index);//enemy_radius[enemy_type[hit_index]] + 5;
            g.drawOval(enemy_x[player.hit_index] - screen_x - rad, enemy_y[player.hit_index] - screen_y - rad, rad * 2, rad * 2);
            break;
        case HIT_TYPE_ROBOT_MISSLE:
            rad = ENEMY_PROJECTILE_RADIUS + 5;
            g.drawOval(Math.round(enemy_missle[player.hit_index].x - screen_x - rad), Math.round(enemy_missle[player.hit_index].y - screen_y - rad), rad * 2, rad * 2);
            break;
        case HIT_TYPE_TANK_MISSLE:
            rad = TANK_MISSLE_RADIUS + 5;
            g.drawOval(tank_missle[player.hit_index].x - screen_x - rad, tank_missle[player.hit_index].y - screen_y - rad, rad * 2, rad * 2);
            break;
        default:
            // This is too much error checking maybe?
            // Utils.unhandledCase(hit_type);
            break;
        
        }
    }
    
    private void updatePlayers() {
        for (int i=0; i<num_players; i++) {
            updatePlayer(players[i]);
        }
        
    }
    
    // -----------------------------------------------------------------------------------------------
    // do collision scans and make people walk around stupidly
    private void updatePeople() {
        int frame_num = getFrameNumber();
        int dx, dy;
        
        for (int i = 0; i < num_people;) {
            if (people_state[i] == 0) {
                removePeople(i);
                continue;
            }
            
            if (people_state[i] < 0) {
                // this guy is turning into a zombie
                if (++people_state[i] == 0) {
                    // add a zombie
                    addEnemy(people_x[i], people_y[i], Utils.randRange(ENEMY_INDEX_ZOMBIE_N, ENEMY_INDEX_ZOMBIE_W), true);
                    removePeople(i);
                    continue;
                }
                i++;
                continue;
            }
            
            if (!isOnScreen(people_x[i], people_y[i])) {
                i++;
                continue;
            }
            
            boolean done = false;
            // look for a colliison with the player
            for (int ii=0; ii<num_players; ii++) {
                Player player = players[ii];
                if (Utils.isPointInsideCircle(player.x, player.y, people_x[i], people_y[i], PEOPLE_RADIUS + getPlayerRadius(player))) {
                    if (isHulkActiveCharging(player)) {
                        if (Utils.randRange(0,3)==3)
                            addPlayerMsg(player, "HULK SMASH!");
                        addPoints(player, -people_points);
                        if (people_picked_up > 0)
                            people_picked_up--;
                        addParticle(people_x[i], people_y[i], PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0);
                    } else {
                        addMsg(people_x[i], people_y[i], String.valueOf(people_points));// + " points");
                        addPoints(player, people_points);
                        people_picked_up++;
                        if (people_points < PEOPLE_MAX_POINTS)
                            people_points += PEOPLE_INCREASE_POINTS * game_level;
                    }
                    removePeople(i);
                    done = true;
                    break;
                }
            }
            
            if (done)
                continue;
            
            if (frame_num % 5 == people_state[i]) {
                dx = move_dx[people_state[i] - 1] * PEOPLE_SPEED;
                dy = move_dy[people_state[i] - 1] * PEOPLE_SPEED;
                
                if (collisionScanCircle(people_x[i] + dx, people_y[i] + dy, PEOPLE_RADIUS)) {
                    // reverse direction
                    if (people_state[i] <= 2)
                        people_state[i] += 2;
                    else
                        people_state[i] -= 2;
                } else {
                    people_x[i] += dx;
                    people_y[i] += dy;
                    
                    // look for random direction changes
                    if (Utils.randRange(0, 10) == 0)
                        people_state[i] = Utils.randRange(1, 4);
                }
            }
            i++;
        }
    }

    private int [][] animPeople;
    
    private void drawPerson(AGraphics g, int index) {
        
        if (animPeople != null) {
            int type = people_type[index]; 
            int dir = people_state[index]-1;
            if (dir >= 0 && dir < 4) {
                int dim = 32;
                int x = people_x[index] - screen_x;
                int y = people_y[index] - screen_y;
                
                drawPerson(g, x, y, dim, type, dir);
            }
        }        
    }
    
    private void drawPerson(AGraphics g, int x, int y, int dimension, int type, int dir) {
        if (dir >= 0 && dir < 4) {
            int animIndex = dir*4 + (getFrameNumber()/8)%4;
            g.setColor(g.WHITE);
            g.drawImage(animPeople[type][animIndex], x-dimension/2, y-dimension/2, dimension, dimension);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw all the people
    private void drawPeople(AGraphics g) {
        for (int i = 0; i < num_people; i++) {
            if (!isOnScreen(people_x[i], people_y[i]))
                continue;
            
            if (people_state[i] < 0) {
                // turning into a zombie, draw in white, shaking
                g.setColor(g.WHITE);
                drawStickFigure(g, people_x[i] - screen_x, people_y[i] - screen_y + Utils.randRange(-2, 2), PEOPLE_RADIUS);
            } else {
                // normal
                drawPerson(g, i);
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw a stupid looking stick figure
    private void drawStickFigure(AGraphics g, int x0, int y0, int radius) {
        int x1, y1;
        
        // draw the legs
        g.drawLine(x0, y0, x0 + radius / 2, y0 + radius);
        g.drawLine(x0, y0, x0 - radius / 2, y0 + radius);
        g.drawLine(x0 - 1, y0, x0 + radius / 2 - 1, y0 + radius);
        g.drawLine(x0 - 1, y0, x0 - radius / 2 - 1, y0 + radius);
        // draw the body
        x1 = x0;
        y1 = y0 - radius * 2 / 3;
        g.drawLine(x0, y0, x1, y1);
        g.drawLine(x0 - 1, y0, x1 - 1, y1);
        
        // draw the arms
        g.drawLine(x1 - radius * 2 / 3, y1, x1 + radius * 2 / 3, y1);
        g.drawLine(x1 - radius * 2 / 3, y1 + 1, x1 + radius * 2 / 3, y1 + 1);
        
        // draw the head
        g.drawFilledOval(x1 - radius / 4 - 1, y1 - radius + 1, radius/2, radius/2+2);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void removeMsg(int index) {
        num_msgs--;
        msg_x[index] = msg_x[num_msgs];
        msg_y[index] = msg_y[num_msgs];
        msg_string[index] = msg_string[num_msgs];
        msg_color[index] = msg_color[num_msgs];
    }
    
    // -----------------------------------------------------------------------------------------------
    private void addPlayerMsg(Player player, String msg) {
        int x = player.x + Utils.randRange(10,20);
        int y = player.y + Utils.randRange(10,20);
        addMsg(x, y, msg);
    }
    
    // -----------------------------------------------------------------------------------------------
    // Add a a message to the table if possible
    private void addMsg(int x, int y, String str) {
        if (num_msgs == MESSAGES_MAX) {
            Utils.println("TOO MANY MESSAGES");
            return;
        }
        
        msg_x[num_msgs] = x;
        msg_y[num_msgs] = y;
        msg_string[num_msgs] = str;
        msg_color[num_msgs] = G.WHITE;
        num_msgs++;
    }
    
    // -----------------------------------------------------------------------------------------------
    // update all the messages
    private void updateAndDrawMessages(AGraphics g) {
        int frame_num = getFrameNumber();
        
        for (int i = 0; i < num_msgs;) {
            if (!isOnScreen(msg_x[i], msg_y[i])) {
                removeMsg(i);
                continue;
            }
            
            g.setColor(msg_color[i]);
            g.drawString(msg_string[i], msg_x[i] - screen_x, msg_y[i] - screen_y);
            msg_y[i] -= 1;
            
            if (frame_num % MESSAGE_FADE == 0) {
                msg_color[i] = msg_color[i].darkened(DARKEN_AMOUNT);
                if (msg_color[i].equals(g.BLACK)) {
                    removeMsg(i);
                    continue;
                }
            }
            i++;
        }
    }
    
    private final String [] particle_stars = { "*", "!", "?", "@" };

    
    
    // -----------------------------------------------------------------------------------------------
    // add an explosion if possible
    private void addParticle(int x, int y, int type, int duration, int playerIndex, int angle) {
        if (num_particles == MAX_PARTICLES)
            return;
        
        Particle p = particles[num_particles++];
        
        p.x = x;
        p.y = y;
        p.star = Utils.randRange(0, particle_stars.length-1);
        p.angle = angle;
        p.type = type;
        p.duration = duration;
        p.start_frame = getFrameNumber();
        p.playerIndex = playerIndex;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove explosion at index
    private void removeParticle(int index) {
        num_particles--;
        particles[index].copy(particles[num_particles]);
    }
    
    // -----------------------------------------------------------------------------------------------
    // update and draw all explosions
    private void updateAndDrawParticles(AGraphics g) {
        int x, y, radius, width, v_spacing, j, top;
        
        for (int i = 0; i < num_particles;) {
            Particle p = particles[i];
            int duration = getFrameNumber() - p.start_frame;
            if (duration >= p.duration) {
                removeParticle(i);
                continue;
            } else if (duration < 1) {
                duration = 1;
            }
            
            if (!isOnScreen(p.x, p.y)) {
                removeParticle(i);
                continue;
            }
            
            x = p.x - screen_x;
            y = p.y - screen_y;
            
            
            switch (p.type) {
            case PARTICLE_TYPE_BLOOD:
                
            {
                final int pd2 = p.duration/2;
                // draw a missle command type expl
                if (duration <= pd2) {
                    g.setColor(g.RED);
                    // draw expanding disk
                    radius = PARTICLE_BLOOD_RADIUS * duration / pd2;
                    g.drawFilledOval(x - radius, y - radius, radius * 2, radius * 2);
                } else {
                    // draw 2nd half of explosion sequence
                    radius = PARTICLE_BLOOD_RADIUS * (duration-pd2) / pd2;
                    g.setColor(g.RED);
                    g.drawFilledOval(x - PARTICLE_BLOOD_RADIUS, y - PARTICLE_BLOOD_RADIUS, PARTICLE_BLOOD_RADIUS * 2, PARTICLE_BLOOD_RADIUS * 2);
                    g.setColor(g.BLACK);
                    g.drawFilledOval(x - radius, y - radius, radius * 2, radius * 2);
                }
            }
            break;
            
            case PARTICLE_TYPE_DYING_ROBOT:
                
                g.setColor(g.DARK_GRAY);
                
                v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1);
                width = ENEMY_ROBOT_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4;
                top = -(ENEMY_ROBOT_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration));
                
                for (j = 0; j < 8; j++) {
                    g.drawLine(x - width / 2, y + top, x + width / 2, y + top);
                    top += v_spacing;
                }
                
                break;
                
            case PARTICLE_TYPE_DYING_TANK:
                
                g.setColor(g.RED);
                
                v_spacing = 2 * (PARTICLE_DYING_ROBOT_DURATION - duration + 1);
                width = ENEMY_TANK_RADIUS * 3 - (PARTICLE_DYING_ROBOT_DURATION - duration) * 4;
                top = -(ENEMY_TANK_RADIUS + (PARTICLE_DYING_ROBOT_DURATION - duration));
                
                for (j = 0; j < 8; j++) {
                    g.drawLine(x - width / 2, y + top, x + width / 2, y + top);
                    top += v_spacing;
                }
                
                break;

            case PARTICLE_TYPE_PLAYER_STUN:
                // we will assume anything else is a stun
                g.setColor(g.WHITE);
                Player player = players[p.playerIndex];
                float rad = getPlayerRadius(player);
                
                // draw swirling ?
                float px = player.x - screen_x;
                float py = Math.round(player.y - screen_y - rad*2);
                
                float rx = rad;
                float ry = rad*0.5f;
                
                float deg = p.angle;
                int tx = Math.round(px + rx * CMath.cosine(deg));
                int ty = Math.round(py + ry * CMath.sine(deg));
                p.angle += 25;
                
                String star = particle_stars[p.star];
                
                g.drawString(star, tx, ty);
                break;

            default:
                
                Utils.unhandledCase(p.type);
            
            
            } // end switch
            i++;
        }
    }
    
    private final int WALL_FLAG_VISITED = 256;
    
    /**
     * Compute the distance in maze coordinates between to points
     * using the path between the 2 cells
     *  
     * @param x0
     * @param y0
     * @param x1
     * @param y1
     * @return
     */
    private int computeCellDistance(int x0, int y0, int x1, int y1) {
       int [] c0 = new int[2];
       int [] c1 = new int[2];
       this.computeCell(x0, y0, c0);
       this.computeCell(x1, y1, c1);
       List<int[]> path = new LinkedList<int[]>();
       findPath(this.maze_cells, c0[0], c0[1], c1[0], c1[1], path);
       int dist = 0;
       if (path.size() > 1) {
           int [] sc = path.get(0);
           int dx = x1 - sc[0]*MAZE_CELL_WIDTH;
           int dy = y1 - sc[1]*MAZE_CELL_HEIGHT;
           dist += Utils.fastLen(dx, dy);
           for (int i=1; i<path.size(); i++) {
               int [] dc = path.get(i);
               dx = Math.abs(sc[0] - dc[0]);
               dy = Math.abs(sc[1] - dc[1]);
               dist += dx * this.MAZE_CELL_WIDTH + dy * this.MAZE_CELL_HEIGHT;
               sc = dc;
           }
           dx = x0 - sc[0]*MAZE_CELL_WIDTH;
           dy = y0 - sc[1]*MAZE_CELL_HEIGHT;
           dist += Utils.fastLen(dx, dy);
       } else {
           dist = Utils.fastLen(x0-x1,y0-y1);
       }
       return dist;
    }
    
    // compute the path between 2 cells and return its distance 
    private int distCells(int cx0, int cy0, int cx1, int cy1) {
        List<int[]> path = new LinkedList<int[]>();
        findPath(maze_cells, cx0, cy0, cx1, cy1, path);
        return path.size();
    }

    private void findPath(int [][] cells, int cx0, int cy0, int cx1, int cy1, List<int[]> path) {
        for (int i=0; i<cells.length; i++) {
            for (int ii=0; ii<cells[i].length; ii++) {
                cells[i][ii] &= ~WALL_FLAG_VISITED; // unmark if marked 
            }
        }
        findPath_r(cells, cx0, cy0, cx1, cy1, path);
    }
    
    // TODO: Optimize the search by sorting the direction to choose
    
    private boolean findPath_r(int [][] cells, int cx0, int cy0, int cx1, int cy1, List<int[]> path) {
        boolean found = false;
        if (cx0 == cx1 && cy0 == cy1) {
            found = true;
        } else {
            if ((cells[cx0][cy0] & WALL_FLAG_VISITED) == 0) {
                // mark this cell as visited
                cells[cx0][cy0] |= WALL_FLAG_VISITED;
                if ((cells[cx0][cy0] & this.WALL_NORTH) == 0) {
                    found = findPath_r(cells, cx0, cy0-1, cx1, cy1, path);
                }
                if (!found && (cells[cx0][cy0] & this.WALL_SOUTH) == 0) {
                    found = findPath_r(cells, cx0, cy0+1, cx1, cy1, path);
                }
                if (!found && (cells[cx0][cy0] & this.WALL_WEST) == 0) {
                    found = findPath_r(cells, cx0-1, cy0, cx1, cy1, path);
                }
                if (!found && (cells[cx0][cy0] & this.WALL_EAST) == 0) {
                    found = findPath_r(cells, cx0+1, cy0, cx1, cy1, path);
                }
            }
        }
        if (found) {
            path.add(new int [] { cx0, cy0 });
        }
        return found;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Do enemy hit event on enemy e
    // return true if the source object killed the enemy 
    //
    // DO NOT CALL playerHit from this func, inf loop possible!
    private boolean enemyHit(Player player, int e, int dx, int dy) { 
        
        if (enemy_type[e] == ENEMY_INDEX_GEN) {
            // spawn a bunch of guys in my place
            int count = Utils.randRange(ENEMY_GEN_SPAWN_MIN, ENEMY_GEN_SPAWN_MAX);
            for (int i = 0; i < count; i++) {
                addEnemy(enemy_x[e] + Utils.randRange(-10, 10), enemy_y[e] + Utils.randRange(-10, 10), Utils.randRange(ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_W), true);
            }
            
            int distGen = this.computeCellDistance(enemy_x[e], enemy_y[e], end_x, end_y);
            int distStart = this.computeCellDistance(player.start_x, player.start_y, end_x, end_y);
            
            
            
            // if this is closer to the end than the player start, then make this the start
            //int distGen = Utils.fastLen(enemy_x[e] - end_x, enemy_y[e] - end_x);
            //int distStart = Utils.fastLen(player.start_x - end_x, player.start_y - end_y);
            
            out.println("distGen = " + distGen + ", distStart = " + distStart);
            
            if (distGen < distStart) {
                player.start_x = enemy_x[e];
                player.start_y = enemy_y[e];
            }
            
            addRandomPowerup(enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]]);
            
            addPoints(player, ENEMY_GEN_POINTS);
            removeEnemy(e);
            return true;
        }
        
        else if (enemy_type[e] <= ENEMY_INDEX_ROBOT_W) {
            addPoints(player, ENEMY_ROBOT_POINTS);
            addParticle(enemy_x[e], enemy_y[e], PARTICLE_TYPE_DYING_ROBOT, 5, -1, 0);
            removeEnemy(e);
            return true;
        }
        
        else if (enemy_type[e] <= ENEMY_INDEX_THUG_W) {
            if (!collisionScanCircle(enemy_x[e] + dx, enemy_y[e] + dy, ENEMY_THUG_RADIUS)) {
                enemy_x[e] += dx;
                enemy_y[e] += dy;
            }
            return false;
        }
        
        else if (enemy_type[e] == ENEMY_INDEX_BRAIN) {
            // chance for powerup
            addRandomPowerup(enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]]);
            // spawn some blood
            addPoints(player, ENEMY_BRAIN_POINTS);
            addParticle(enemy_x[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        enemy_y[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0);
            
            addParticle(enemy_x[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        enemy_y[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0);
            
            addParticle(enemy_x[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        enemy_y[e] + Utils.randRange(-ENEMY_BRAIN_RADIUS / 2, ENEMY_BRAIN_RADIUS / 2), 
                        PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0);
            
            removeEnemy(e);
            return true;
        }
        
        else if (enemy_type[e] <= ENEMY_INDEX_TANK_NW) {
            addPoints(player, ENEMY_TANK_POINTS);
            addParticle(enemy_x[e], enemy_y[e], PARTICLE_TYPE_DYING_TANK, 8, -1, 0);
            removeEnemy(e);
            return true;
        }
        
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw all enemies
    private void drawEnemies(AGraphics g) {
        // int radius;
        // int mod;
        boolean debugEnemyDrawn = false;
        for (int i = 0; i < num_enemies; i++) {
            if (!isOnScreen(enemy_x[i], enemy_y[i]))
                continue;
            
            //if (isVisibilityEnabled()) {
            //    if (!canSee(player.x, player.y, enemy_x[i], enemy_y[i]))
            //        continue;
            //}
            
            int x0 = enemy_x[i] - screen_x;
            int y0 = enemy_y[i] - screen_y;
            
            switch (enemy_type[i]) {
            case ENEMY_INDEX_GEN:
                drawGenerator(g, x0, y0);
                break;
                
            case ENEMY_INDEX_ROBOT_N:
            case ENEMY_INDEX_ROBOT_E:
            case ENEMY_INDEX_ROBOT_S:
            case ENEMY_INDEX_ROBOT_W:
                drawRobot(g, x0, y0, enemy_type[i] - ENEMY_INDEX_ROBOT_N);
                break;
                
            case ENEMY_INDEX_THUG_N:
            case ENEMY_INDEX_THUG_E:
            case ENEMY_INDEX_THUG_S:
            case ENEMY_INDEX_THUG_W:
                drawThug(g, x0, y0, enemy_type[i] - ENEMY_INDEX_THUG_N);
                break;
                
            case ENEMY_INDEX_BRAIN:
                drawBrain(g, x0, y0, ENEMY_BRAIN_RADIUS);
                break;
                
            case ENEMY_INDEX_ZOMBIE_N:
            case ENEMY_INDEX_ZOMBIE_E:
            case ENEMY_INDEX_ZOMBIE_S:
            case ENEMY_INDEX_ZOMBIE_W:
                g.setColor(g.YELLOW);
                drawStickFigure(g, x0, y0, ENEMY_ZOMBIE_RADIUS);
                break;
                
            case ENEMY_INDEX_TANK_NE:
            case ENEMY_INDEX_TANK_SE:
            case ENEMY_INDEX_TANK_SW:
            case ENEMY_INDEX_TANK_NW:
                drawTank(g, x0, y0, enemy_type[i] - ENEMY_INDEX_TANK_NE);
                break;
                
            case ENEMY_INDEX_JAWS:
                int index = getFrameNumber() - enemy_next_update[i];
                if (index >= animJaws.length) {
                    enemy_next_update[i] = getFrameNumber()+1;
                    index = 0;
                }
                else if (index <= 0) {
                    index = 0;
                    for (int ii=0; ii<num_players; ii++) {
                        Player player = players[ii];
                        float rad2 = PLAYER_RADIUS*PLAYER_RADIUS + 100*100;
                        if (Utils.distSqPointPoint(player.x, player.y, enemy_x[i], enemy_y[i]) > rad2) {
                            enemy_next_update[i] = getFrameNumber() + animJaws.length;
                        }                   
                    }
                }
                drawJaws(g, x0, y0, index);
                break;
                
            case ENEMY_INDEX_LAVA:
                drawLavaPit(g, x0, y0, getLavaPitFrame(i));
                break;
            } // end switch
            
            if (!debugEnemyDrawn && isDebugEnabled(Debug.DRAW_ENEMY_INFO)) {
                int r = getEnemyRadius(i);
                int x = x0-r;
                int y = y0-r;
                int w = r*2;
                int h = r*2;
                Player player = getPlayer();
                if (Utils.isPointInsideRect(player.target_x, player.target_y, x, y, w, h)) {
                    g.setColor(g.YELLOW);
                    g.drawOval(x, y, w, h);
                    String msg = "index [" + i + "]"
                               + "\nradius [" + getEnemyRadius(i) + "]"
                               + "\ntype   [" + this.getEnemyTypeString(this.enemy_type[i]) + "]";
                     
                    g.drawJustifiedString(x, y, Justify.RIGHT, Justify.TOP, msg);
                    debugEnemyDrawn = true;
                }
            }
        }
    }
    
    private String getEnemyTypeString(int enemy_type) {
        return ENEMY_NAMES[enemy_type];
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw a robot at x0,y0 facing direction dir [0123] == [NESW]
    private void drawRobot(AGraphics g, int x0, int y0, int dir) {
        g.setColor(g.DARK_GRAY);
        int walk = (getFrameNumber() % 12) / 4 - 1;
        if (dir == 0 || dir == 2) {
            // draw head
            g.drawFilledRect(x0 - 8, y0 - 14, 16, 12);
            // draw the arms
            g.drawFilledRect(x0 - 12, y0 - 6, 4, 12);
            g.drawFilledRect(x0 + 8, y0 - 6, 4, 12);
            // draw the body
            g.drawFilledRect(x0 - 6, y0 - 2, 12, 4);
            g.drawFilledRect(x0 - 4, y0 + 2, 8, 6);
            // draw the legs
            g.drawFilledRect(x0 - 6, y0 + 8, 4, 8 + walk);
            g.drawFilledRect(x0 + 2, y0 + 8, 4, 8 - walk);
            // draw the feet
            g.drawFilledRect(x0 - 8, y0 + 12 + walk, 2, 4);
            g.drawFilledRect(x0 + 6, y0 + 12 - walk, 2, 4);
            // draw the eyes if walking S
            if (dir == 2) {
                g.setColor(throbbing_white);
                g.drawFilledRect(x0 - 4, y0 - 12, 8, 4);
            }
        } else {
            // draw the robot sideways
            
            // draw the head
            g.drawFilledRect(x0 - 6, y0 - 14, 12, 8);
            // draw the body, eyes ect.
            if (dir == 1) {
                // body
                g.drawFilledRect(x0 - 6, y0 - 6, 10, 10);
                g.drawFilledRect(x0 - 8, y0 + 4, 14, 4);
                // draw the legs
                g.drawFilledRect(x0 - 8, y0 + 8, 4, 8 + walk);
                g.drawFilledRect(x0 + 2, y0 + 8, 4, 8 - walk);
                // draw feet
                g.drawFilledRect(x0 - 4, y0 + 12 + walk, 4, 4);
                g.drawFilledRect(x0 + 6, y0 + 12 - walk, 4, 4);
                // draw the eyes
                g.setColor(throbbing_white);
                g.drawFilledRect(x0 + 2, y0 - 12, 4, 4);
            } else {
                // body
                g.drawFilledRect(x0 - 4, y0 - 6, 10, 10);
                g.drawFilledRect(x0 - 6, y0 + 4, 14, 4);
                // draw the legs
                g.drawFilledRect(x0 - 6, y0 + 8, 4, 8 + walk);
                g.drawFilledRect(x0 + 4, y0 + 8, 4, 8 - walk);
                // draw feet
                g.drawFilledRect(x0 - 10, y0 + 12 + walk, 4, 4);
                g.drawFilledRect(x0, y0 + 12 - walk, 4, 4);
                // draw the eyes
                g.setColor(throbbing_white);
                g.drawFilledRect(x0 - 6, y0 - 12, 4, 4);
            }
            // draw the arm
            g.setColor(g.BLACK);
            g.drawFilledRect(x0 - 2, y0 - 6, 4, 12);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw a generator
    private void drawGenerator(AGraphics g, int x, int y) {
        this.drawTankGen(g, x, y);
        /*
         int radius;
         // draw a throbbing circle in yellow
          int mod = getFrameNumber() % (ENEMY_GEN_PULSE_FACTOR * 4);
          if (mod < ENEMY_GEN_PULSE_FACTOR) // shrinking
          radius = ENEMY_GEN_RADIUS - mod;
          else if (mod >= 15) // shrinking
          radius = ENEMY_GEN_RADIUS + ENEMY_GEN_PULSE_FACTOR - (mod - ENEMY_GEN_PULSE_FACTOR * 3);
          else
          // expanding
           radius = ENEMY_GEN_RADIUS - ENEMY_GEN_PULSE_FACTOR + (mod - ENEMY_GEN_PULSE_FACTOR);
           Utils.YELLOW.set();
           Utils.drawFilledOval(x - radius, y - radius, radius * 2, radius * 2);*/
    }
    
    private final int[] brain_pts_x = { 15, 13, 13, 8, 7, 6, 4, 2, 2, 3, 3, 4, 4, 8, 11, 12, 14, 17, 20, 21, 21, 23, 25, 27, 29, 29, 28, 29, 25, 24, 22, 20,
            20, 18 };
    
    private final int[] brain_pts_y = { 16, 19, 21, 21, 20, 21, 21, 19, 15, 14, 11, 9, 7, 4, 4, 2, 1, 2, 2, 3, 3, 4, 8, 7, 8, 13, 14, 16, 21, 20, 21, 20, 18,
            19 };
    
    private final int[] brain_nerves_x = { 8, 6, 10, 11, 13, 17, 15, 17, 16, 16, 18, 19, 21, 21, 23, 22, 23, 25, 26, 28 };
    
    private final int[] brain_nerves_y = { 7, 10, 10, 12, 12, 2, 4, 7, 9, 10, 10, 11, 9, 7, 8, 20, 18, 18, 17, 19 };
    
    private final int[] brain_legs_x = { 15, 13, 13, 10, 14, 16, 18, 22, 20, 20, 18 };
    
    private final int[] brain_legs_y = { 16, 19, 21, 29, 29, 24, 29, 29, 20, 18, 19 };
    
    private final int[] brain_nerves_len = { 5, 4, 6, 5 };
    
    // -----------------------------------------------------------------------------------------------
    // Draw the Evil Brain
    private void drawBrain(AGraphics g, int x0, int y0, int radius) {
        // big head, little arms and legs
        g.setColor(g.BLUE);
        g.translate(x0 - radius, y0 - radius);
        g.drawFilledPolygon(brain_pts_x, brain_pts_y, brain_pts_x.length);
        g.setColor(g.RED);
        g.drawFilledPolygon(brain_legs_x, brain_legs_y, brain_legs_x.length);
        g.translate(-(x0 - radius), -(y0 - radius));
        
        // draw some glowing lines to look like brain nerves
        g.setColor(throbbing_white);
        x0 -= radius;
        y0 -= radius;
        int i = 1;
        for (int l = 0; l < 4; l++) {
            for (int c = 0; c < brain_nerves_len[l] - 1; c++) {
                g.drawLine(x0 + brain_nerves_x[i - 1], y0 + brain_nerves_y[i - 1], x0 + brain_nerves_x[i], y0 + brain_nerves_y[i]);
                i++;
            }
            i++;
        }
        x0 += radius;
        y0 += radius;
        // draw the eyes
        g.setColor(g.YELLOW);
        g.drawFilledRect(x0 - 5, y0 + 1, 2, 2);
        g.drawFilledRect(x0 + 3, y0, 3, 2);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawTankGen(AGraphics g, int x0, int y0) {
        float degrees = (getFrameNumber() % 360);
        int dx = Math.round(5.0f * CMath.cosine(degrees));
        int dy = Math.round(5.0f * CMath.sine(degrees));
        
        g.setColor(g.GREEN);
        g.drawRect(x0 - ENEMY_TANK_GEN_RADIUS + dx, y0 - ENEMY_TANK_GEN_RADIUS + dy, ENEMY_TANK_GEN_RADIUS * 2, ENEMY_TANK_GEN_RADIUS * 2);
        g.setColor(g.BLUE);
        g.drawRect(x0 - ENEMY_TANK_GEN_RADIUS - dx, y0 - ENEMY_TANK_GEN_RADIUS - dy, ENEMY_TANK_GEN_RADIUS * 2, ENEMY_TANK_GEN_RADIUS * 2);
    }
    
    private final int[] tank_pts_x = { 12, 6, 6, 18, 32, 41, 41, 36, 36, 12 };
    
    private final int[] tank_pts_y = { 10, 10, 26, 38, 38, 26, 10, 10, 4, 4 };
    
    // -----------------------------------------------------------------------------------------------
    private void drawTank(AGraphics g, int x0, int y0, int dir) {
        g.setColor(g.DARK_GRAY);
        g.drawFilledRect(x0 - 12, y0 - 20, 24, 16);
        g.setColor(g.RED);
        g.translate(x0 - ENEMY_TANK_RADIUS, y0 - ENEMY_TANK_RADIUS);
        g.drawFilledPolygon(tank_pts_x, tank_pts_y, tank_pts_x.length);
        g.translate(-(x0 - ENEMY_TANK_RADIUS), -(y0 - ENEMY_TANK_RADIUS));
        g.setColor(g.DARK_GRAY);
        g.drawFilledRect(x0 - 12, y0 - 2, 24, 4);
        // draw the wheels
        g.setColor(g.CYAN);
        g.drawFilledOval(x0 - 22, y0 + 6, 12, 12);
        g.drawFilledOval(x0 + 10, y0 + 6, 12, 12);
    }
    
    private int [] animJaws = null;
    
    // -----------------------------------------------------------------------------------------------
    private void drawJaws(AGraphics g, int x0, int y0, int index) {
        if (animJaws == null)
            return;
        index %= animJaws.length;
        int jw = 32;
        int jh = 32;
        g.setColor(g.WHITE);
        g.drawImage(animJaws[index], x0-jw/2, y0-jh/2, jw, jh);
    }
    
    private int [] animLava = null;
    
    // -----------------------------------------------------------------------------------------------
    private void drawLavaPit(AGraphics g, int x0, int y0, int index) {
        if (animLava == null)
            return;
        if (index < animLava.length) {
            g.setColor(g.WHITE);
            g.drawImage(animLava[index], 
                    x0-ENEMY_LAVA_DIM/2, 
                    y0-ENEMY_LAVA_DIM/2, 
                    ENEMY_LAVA_DIM, 
                    ENEMY_LAVA_DIM);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw a dumb looking thug
    private void drawThug(AGraphics g, int x0, int y0, int dir) {
        // draw the body
        g.setColor(g.GREEN);
        g.drawFilledRect(x0 - 12, y0 - 12, 24, 21);
        g.setColor(g.RED);
        if (dir == 0 || dir == 2) {
            // draw 2 arms at the sides
            g.drawFilledRect(x0 - 15, y0 - 10, 3, 15);
            g.drawFilledRect(x0 + 12, y0 - 10, 3, 15);
            
            // draw 2 legs
            g.drawFilledRect(x0 - 8, y0 + 9, 5, 10);
            g.drawFilledRect(x0 + 3, y0 + 9, 5, 10);
        } else {
            // draw 1 arm in the middle
            g.drawFilledRect(x0 - 3, y0 - 10, 6, 15);
            
            // draw 1 leg
            g.drawFilledRect(x0 - 3, y0 + 9, 6, 10);
        }
        // draw the head
        g.setColor(g.BLUE);
        g.drawFilledRect(x0 - 5, y0 - 19, 10, 7);
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw a bar with endpoints p0, p1 with thickness t
    private void drawBar(AGraphics g, int x0, int y0, int x1, int y1, int t, int index) {
        int w = Math.abs(x0 - x1) + t * 2;
        int h = Math.abs(y0 - y1) + t * 2;
        int x = Math.min(x0, x1) - t;
        int y = Math.min(y0, y1) - t;
        
        g.drawFilledRect(x, y, w, h);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawStaticField(AGraphics g, int x, int y, int radius) {
        float x0 = 1;
        float y0 = 0;
        float r0 = Utils.randFloatX(3) + radius;
        float sr = r0;
        for (int i = 0; i < STATIC_FIELD_SECTIONS - 1; i++) {
            float x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T;
            float y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T;
            float r1 = Utils.randFloatX(3) + radius;
            g.drawLine(Math.round(x + x0 * r0), Math.round(y + y0 * r0), Math.round(x + x1 * r1), Math.round(y + y1 * r1));
            x0 = x1;
            y0 = y1;
            r0 = r1;
        }
        g.drawLine(Math.round(x + x0 * r0), Math.round(y + y0 * r0), Math.round(x + 1.0f * sr), Math.round(y + 0 * sr));
    }
    
    // -----------------------------------------------------------------------------------------------
    // do any one time table inits here
    private void initTables() {
        players[0] = new Player();
        num_players = 1;
        
        for (int i=0; i < MAX_PARTICLES; i++) {
            particles[i] = new Particle();
        }
        
        for (int i = 0; i < MAX_ENEMY_MISSLES; i++)
            enemy_missle[i] = new MissleFloat();
        
        for (int i = 0; i < MAX_TANK_MISSLES; i++)
            tank_missle[i] = new MissleInt();
        
        for (int i = 0; i < MAX_SNAKE_MISSLES; i++)
            snake_missle[i] = new MissleSnake();
        
        for (int i = 0; i < MAX_POWERUPS; i++) {
            powerups[i] = new Powerup();
        }
        
        for (int i=0; i<MAZE_NUM_VERTS; i++)
            wall_lookup[i] = new Wall[MAZE_NUM_VERTS];
        
        for (int i = 0; i < MAZE_NUMCELLS_X; i++) {
            for (int j = 0; j < MAZE_NUMCELLS_Y; j++) {
                int upleft = i + j * (MAZE_NUMCELLS_X + 1);
                int upright = i + 1 + j * (MAZE_NUMCELLS_X + 1);
                int downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1);
                int downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1);
                
                initWall(upleft, upright);
                initWall(upright, downright);
                initWall(downleft, downright);
                initWall(upleft, downleft);
            }
        }
        
        enemy_radius[ENEMY_INDEX_GEN] = ENEMY_GEN_RADIUS;
        enemy_radius[ENEMY_INDEX_ROBOT_N] = enemy_radius[ENEMY_INDEX_ROBOT_E] = enemy_radius[ENEMY_INDEX_ROBOT_S] = enemy_radius[ENEMY_INDEX_ROBOT_W] = ENEMY_ROBOT_RADIUS;
        enemy_radius[ENEMY_INDEX_THUG_N] = enemy_radius[ENEMY_INDEX_THUG_E] = enemy_radius[ENEMY_INDEX_THUG_S] = enemy_radius[ENEMY_INDEX_THUG_W] = ENEMY_THUG_RADIUS;
        enemy_radius[ENEMY_INDEX_BRAIN] = ENEMY_BRAIN_RADIUS;
        enemy_radius[ENEMY_INDEX_ZOMBIE_N] = enemy_radius[ENEMY_INDEX_ZOMBIE_E] = enemy_radius[ENEMY_INDEX_ZOMBIE_S] = enemy_radius[ENEMY_INDEX_ZOMBIE_W] = ENEMY_ZOMBIE_RADIUS;
        enemy_radius[ENEMY_INDEX_TANK_NE] = enemy_radius[ENEMY_INDEX_TANK_SW] = enemy_radius[ENEMY_INDEX_TANK_SE] = enemy_radius[ENEMY_INDEX_TANK_NW] = ENEMY_TANK_RADIUS;
        enemy_radius[ENEMY_INDEX_JAWS] = ENEMY_JAWS_DIM/2-2;
        enemy_radius[ENEMY_INDEX_LAVA] = ENEMY_LAVA_DIM/2-5;
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getEnemyRadius(int enemyIndex) {
        int type = enemy_type[enemyIndex];
        int radius = enemy_radius[type];
        if (type == this.ENEMY_INDEX_LAVA) {
            //if (this.enemy_spawned_frame)
            int frame = this.getLavaPitFrame(enemyIndex);
            if (frame >= this.animLava.length) {
                return -1;
            }
            // make so the radius follows the expand/contract animation
            final float R = ENEMY_LAVA_DIM/2;
            final float F = animLava.length;
            final float M = R/F;
            
            float f = frame;

            if (f < F/2) {
                radius = Math.round(M * f);
            } else {
                radius = Math.round(-M * f + R);
            }
        }
        return radius;
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getLavaPitFrame(int enemyIndex) {
        return ((getFrameNumber() + enemy_spawned_frame[enemyIndex]) % (animLava.length + this.ENEMY_LAVA_CLOSED_FRAMES));
    }
    
    // -----------------------------------------------------------------------------------------------
    // update all enemies position and collision
    private void updateEnemies() {
        int p, vx, vy, mag, radius, closest, mindist;
        
        // cache the frame number
        final int frame_num = getFrameNumber();
        
        total_enemies = 0;
        for (int i = 0; i < num_enemies;) {
            //if (enemy_type[i] < ENEMY_INDEX_THUG_N || enemy_type[i] > ENEMY_INDEX_THUG_W
            //  && enemy_type[i] != ENEMY_INDEX)
            
                total_enemies++;
            
            if (!isOnScreen(enemy_x[i], enemy_y[i])) {
                i++;
                continue;
            }
            
            // get the radius of this enemy type from the lookup table
            radius = getEnemyRadius(i);//enemy_radius[enemy_type[i]];
            
            for (int ii=0; ii<num_players; ii++) {
                Player player = players[ii];
                if (player.state == PLAYER_STATE_ALIVE) {
                    // see if we have collided with the player
                    if (radius > 0 && Utils.isPointInsideCircle(player.x, player.y, enemy_x[i], enemy_y[i], getPlayerRadius(player) + radius)) {
                        playerHit(player, HIT_TYPE_ENEMY, i);
                    }
                }
            }
            
            // see if we have squashed some people
            if (enemy_type[i] >= ENEMY_INDEX_THUG_N && enemy_type[i] <= ENEMY_INDEX_THUG_W) {
                for (p = 0; p < num_people;) {
                    if (people_state[p] > 0 && isOnScreen(people_x[p], people_y[p])
                            && Utils.isPointInsideCircle(people_x[p], people_y[p], enemy_x[i], enemy_y[i], radius + PEOPLE_RADIUS)) {
                        addParticle(people_x[p], people_y[p], PARTICLE_TYPE_BLOOD, PARTICLE_BLOOD_DURATION, -1, 0);
                        addMsg(enemy_x[i] + Utils.randRange(-10, 10), enemy_y[i] + Utils.randRange(-10, 10), "NOOOO!!!");
                        removePeople(p);
                        continue;
                    }
                    p++;
                }
            }
            
            if (enemy_next_update[i] > frame_num) {
                i++;
                continue;
            }
            
            switch (enemy_type[i]) {
            case ENEMY_INDEX_GEN: // ------------------------------------------------------------
                // create a new guy
                int ex = enemy_x[i] + Utils.randRange(-10, 10);
                int ey = enemy_y[i] + Utils.randRange(-10, 10);
                int ed = Utils.randRange(ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_W);
                addEnemy(ex, ey, ed, true);
                enemy_next_update[i] = frame_num + 50 + Utils.randRange(-20, 20);
                break;
                
            case ENEMY_INDEX_ROBOT_N:
            case ENEMY_INDEX_ROBOT_E: // ------------------------------------------------------------
            case ENEMY_INDEX_ROBOT_S:
            case ENEMY_INDEX_ROBOT_W:
                
                // mag = 0.3f;
                
                for (int ii=0; ii<num_players; ii++) {
                    Player player = players[ii];
                    if (player.state == PLAYER_STATE_ALIVE) {
                        if (Utils.randRange(0, 4) == 0) {
                            // see if a vector from me too player intersects a wall
                            if (collisionScanLine(player.x, player.y, enemy_x[i], enemy_y[i])) {
                                enemy_type[i] = ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], 1.0f);
                            } else {
                                enemy_type[i] = ENEMY_INDEX_ROBOT_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_ROBOT_HEURISTIC_FACTOR);
                            }
                        }
                    }
                }
                
                vx = (enemy_robot_speed) * move_dx[enemy_type[i] - ENEMY_INDEX_ROBOT_N];
                vy = (enemy_robot_speed) * move_dy[enemy_type[i] - ENEMY_INDEX_ROBOT_N];
                
                if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_ROBOT_RADIUS)) {
                    vx = -vx;
                    vy = -vy;
                    if (enemy_type[i] < ENEMY_INDEX_ROBOT_N + 2)
                        enemy_type[i] += 2;
                    else
                        enemy_type[i] -= 2;
                }
                
                enemy_x[i] += vx;
                enemy_y[i] += vy;
                
                enemy_next_update[i] = frame_num + ((ENEMY_ROBOT_MAX_SPEED + difficulty) + 1 - enemy_robot_speed) + Utils.randRange(-3, 3);
                
                // look for lobbing a missle at the player
                if (Utils.randRange(0, 200) < (ENEMY_PROJECTILE_FREQ + difficulty + game_level)) {
                    for (int ii=0; ii<num_players; ii++) {
                        Player player = players[ii];
                        if (player.state == PLAYER_STATE_ALIVE) {
                            int dx = player.x - enemy_x[i];
                            // int dy = player.y - enemy_y[i];
                            
                            if (Math.abs(dx) > 10 && Math.abs(dx) < (ENEMY_ROBOT_ATTACK_DIST + game_level * 5)) {
                                enemyFireMissle(player, i);
                            }
                        }
                    }
                }
                break;
                
            case ENEMY_INDEX_THUG_N: // ------------------------------------------------------------
            case ENEMY_INDEX_THUG_E: // ------------------------------------------------------------
            case ENEMY_INDEX_THUG_S: // ------------------------------------------------------------
            case ENEMY_INDEX_THUG_W: // ------------------------------------------------------------
                vx = (ENEMY_THUG_SPEED + game_level / 2) * move_dx[enemy_type[i] - ENEMY_INDEX_THUG_N];
                vy = (ENEMY_THUG_SPEED + game_level / 2) * move_dy[enemy_type[i] - ENEMY_INDEX_THUG_N];
                // see if we will walk into wall
                if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, radius)) {
                    // turn around
                    if (enemy_type[i] < ENEMY_INDEX_THUG_S)
                        enemy_type[i] += 2;
                    else
                        enemy_type[i] -= 2;
                } else {
                    // walk forward
                    enemy_x[i] += vx;
                    enemy_y[i] += vy;
                }
                // roll dice
                if (Utils.randRange(0, 5) == 0) {
                    // pick a new directiorn
                    enemy_type[i] = ENEMY_INDEX_THUG_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_THUG_HEURISTICE_FACTOR);
                }
                enemy_next_update[i] = frame_num + ENEMY_THUG_UPDATE_FREQ + Utils.randRange(-2, 2);
                break;
                
            case ENEMY_INDEX_BRAIN: // --------------------------------------
                
                if (getFrameNumber() % ENEMY_BRAIN_FIRE_FREQ == 0 && Utils.randRange(0, ENEMY_BRAIN_FIRE_CHANCE) <= (1 + difficulty) * game_level)
                    addSnakeMissle(enemy_x[i], enemy_y[i]);
                
                // search for a person to walk toward
                closest = -1;
                mindist = Integer.MAX_VALUE;
                for (p = 0; p < num_people; p++) {
                    if (people_state[p] <= 0 || !isOnScreen(people_x[p], people_y[p]))
                        continue;
                    
                    if (Utils.isPointInsideCircle(enemy_x[i], enemy_y[i], people_x[p], people_y[p], ENEMY_BRAIN_RADIUS + PEOPLE_RADIUS)) {
                        // turn this people into a zombie
                        people_state[p] = -ENEMY_BRAIN_ZOMBIFY_FRAMES;
                        enemy_next_update[i] = frame_num + ENEMY_BRAIN_ZOMBIFY_FRAMES;
                        mindist = 0;
                        break;
                    }
                    
                    vx = enemy_x[i] - people_x[p];
                    vy = enemy_y[i] - people_y[p];
                    
                    mag = vx * vx + vy * vy;
                    
                    if (!collisionScanLine(enemy_x[i], enemy_y[i], enemy_x[i] + vx, enemy_y[i] + vy) && (mag < mindist)) {
                        mag = mindist;
                        closest = p;
                    }
                }
                
                if (mindist == 0) {
                    i++;
                    continue;
                }
                
                if (closest < 0) {
                    Player closestPlayer = getClosestPlayer(enemy_x[i], enemy_y[i]);
                    if (closestPlayer != null) {
                        // just move toward player
                        vx = closestPlayer.x - enemy_x[i];
                        vy = closestPlayer.y - enemy_y[i];
                    } else {
                        vx = vy = 0;
                    }
                } else {
                    vx = people_x[closest] - enemy_x[i];
                    vy = people_y[closest] - enemy_y[i];
                }
                
                mag = Utils.fastLen(vx, vy) + 1;
                
                vx = vx * (ENEMY_BRAIN_SPEED + game_level / 3) / mag;
                vy = vy * (ENEMY_BRAIN_SPEED + game_level / 3) / mag;
                
                if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_BRAIN_RADIUS)) {
                    vx = -vx;
                    vy = -vy;
                }
                enemy_x[i] += vx;
                enemy_y[i] += vy;
                
                enemy_next_update[i] = frame_num + ENEMY_BRAIN_UPDATE_SPACING - (game_level / 2) + Utils.randRange(-3, 3);
                
                break;
                
            case ENEMY_INDEX_ZOMBIE_N:
            case ENEMY_INDEX_ZOMBIE_E: // ---------------------------
            case ENEMY_INDEX_ZOMBIE_S:
            case ENEMY_INDEX_ZOMBIE_W:
                
                enemy_type[i] = ENEMY_INDEX_ZOMBIE_N + enemyDirectionHeuristic(enemy_x[i], enemy_y[i], ENEMY_ZOMBIE_HEURISTIC_FACTOR);
                
                vx = ENEMY_ZOMBIE_SPEED * move_dx[enemy_type[i] - ENEMY_INDEX_ZOMBIE_N];
                vy = ENEMY_ZOMBIE_SPEED * move_dy[enemy_type[i] - ENEMY_INDEX_ZOMBIE_N];
                
                if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_ZOMBIE_RADIUS)) {
                    if (isPerimiterWall(collision_info_v0, collision_info_v1)) {
                        // dont remove the edge, this is a perimiter edge,
                        // reverse direction of zombie
                        if (enemy_type[i] < ENEMY_INDEX_ZOMBIE_S)
                            enemy_type[i] += 2;
                        else
                            enemy_type[i] -= 2;
                    } else
                        removeEdge(collision_info_v0, collision_info_v1);
                } else {
                    addZombieTracer(enemy_x[i], enemy_y[i]);
                    enemy_x[i] += vx;
                    enemy_y[i] += vy;
                }
                
                enemy_next_update[i] = frame_num + ENEMY_ZOMBIE_UPDATE_FREQ;
                
                break;
                
            case ENEMY_INDEX_TANK_NE:
            case ENEMY_INDEX_TANK_SE: // --------------------------
            case ENEMY_INDEX_TANK_SW:
            case ENEMY_INDEX_TANK_NW:
                
                vx = ENEMY_TANK_SPEED * move_diag_dx[enemy_type[i] - ENEMY_INDEX_TANK_NE];
                vy = ENEMY_TANK_SPEED * move_diag_dy[enemy_type[i] - ENEMY_INDEX_TANK_NE];
                
                if (collisionScanCircle(enemy_x[i] + vx, enemy_y[i] + vy, ENEMY_TANK_RADIUS)) {
                    Wall info = wall_lookup[this.collision_info_v0][this.collision_info_v1];
                    if (info.type != WALL_TYPE_NORMAL || this.isPerimiterWall(collision_info_v0, collision_info_v1)) {
                        // reverse direction of tank
                        if (enemy_type[i] < ENEMY_INDEX_TANK_SW)
                            enemy_type[i] += 2;
                        else
                            enemy_type[i] -= 2;
                    } else {
                        removeEdge(collision_info_v0, collision_info_v1);
                    }
                } else {
                    enemy_x[i] += vx;
                    enemy_y[i] += vy;
                    
                    // look for changing direction for no reason
                    if (Utils.randRange(0, 5) == 0)
                        enemy_type[i] = Utils.randRange(ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_NW);
                }
                
                enemy_next_update[i] = frame_num + ENEMY_TANK_UPDATE_FREQ;
                
                if (Utils.randRange(0, ENEMY_TANK_FIRE_FREQ) == 0)
                    addTankMissle(enemy_x[i], enemy_y[i]);
                
                break;

            } // end switch
            
            i++;
            
        } // end for
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isPerimiterWall(int v0, int v1) {
        return isPerimiterVertex(v0) && isPerimiterVertex(v1);
    }
    
    private Player getClosestPlayer(int x, int y) {
        Player closestPlayer = null;
        int closestDist = Integer.MAX_VALUE;
        for (int ii=0; ii<num_players; ii++) {
            Player player = players[ii];
            if (player.state == PLAYER_STATE_ALIVE) {
                int dist = Utils.fastLen(x-player.x, y-player.y);
                if (closestPlayer == null || dist<closestDist) {
                    closestPlayer = player;
                    closestDist = dist;
                } 
            }
        }    
        return closestPlayer;
    }
    
    // -----------------------------------------------------------------------------------------------
    // return 0,1,2,3 [NESW] for a direction.
    // random is a value between 0.0 and 1.0
    // when random == 1.0, then a totally random direction is choosen
    // when random == 0.0, then the direction toward the player is choosen
    private int enemyDirectionHeuristic(int x, int y, float randomChance) {
        int randDir = Utils.randRange(0, 3); // pick a random direction
        
        Player player = getClosestPlayer(x,y);
        if (player != null) {
            // figure out the dir to the player
            int dx = player.x - x;
            int dy = player.y - y;
            int playerDir = getDirection(dx, dy);
            
            // get a rand value between 0 and 1
            if (Utils.randFloat(1) < randomChance)
                return randDir;
            
            return playerDir;
        }
        return 0;
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the enemy missle at index
    private void removeEnemyMissle(int index) {
        num_enemy_missles--;
        MissleFloat m1 = enemy_missle[index];
        MissleFloat m2 = enemy_missle[num_enemy_missles];
        m1.copy(m2);
    }
    
    // -----------------------------------------------------------------------------------------------
    // enemy fires a missle, yeah
    private void enemyFireMissle(Player player, int enemy) {
        if (num_enemy_missles == MAX_ENEMY_MISSLES)
            return;
        
        float dy;
        
        MissleFloat m = enemy_missle[num_enemy_missles++];
        
        m.x = (float) enemy_x[enemy];
        m.y = (float) enemy_y[enemy];
        
        m.dx = (float) (player.x - enemy_x[enemy]) / (30.0f + Utils.randFloat(10.0f));
        dy = (float) (player.y - enemy_y[enemy]);
        m.dy = (dy * dy) + (2 * dy);
        
        m.dy = -5.0f;
        if (player.x < enemy_x[enemy])
            m.dx = -4.0f;
        else
            m.dx = 4.0f;
        m.duration = ENEMY_PROJECTILE_DURATION;
    }
    
    // -----------------------------------------------------------------------------------------------
    // player fires a missle, yeah
    private void addPlayerMissle(Player player) {
        if (player.num_missles == PLAYER_MAX_MISSLES)
            return;
        
        float vx = player.target_x - player.x;
        float vy = player.target_y - player.y;
        float mag = (float) Math.sqrt(vx * vx + vy * vy);
        float scale = PLAYER_MISSLE_SPEED / mag;
        vx *= scale;
        vy *= scale;
        
        int vxi = Math.round(vx);
        int vyi = Math.round(vy);
        
        if (!this.collisionScanLine(player.x, player.y, player.x + vxi, player.y + vyi)) {
            MissleInt m = player.missles[player.num_missles++];
            m.init(player.x, player.y, vxi, vyi, PLAYER_MISSLE_DURATION);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the player missle at index
    private void removePlayerMissle(Player player, int index) {
        player.num_missles--;
        MissleInt m1 = player.missles[index];
        MissleInt m2 = player.missles[player.num_missles];
        m1.copy(m2);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updatePlayerMissles(Player player) {
        
        boolean done;
        int e, x0, y0, x1, y1;
        MissleInt m, m2;
        
        for (int i = 0; i < player.num_missles; /* increment later */ ) {
            m = player.missles[i];
            if (m.duration <= 0) {
                removePlayerMissle(player, i);
                continue;
            }
            
            if (!isOnScreen(m.x, m.y)) {
                removePlayerMissle(player, i);
                continue;
            }
            
            m.x += m.dx;
            m.y += m.dy;
            m.duration--;
            
            // do collision scans and response
            
            // look for collision with walls
            if (collisionScanLine(m.x, m.y, m.x + m.dx, m.y + m.dy)) {
                x0 = maze_verts_x[collision_info_v0];
                y0 = maze_verts_y[collision_info_v0];
                x1 = maze_verts_x[collision_info_v1];
                y1 = maze_verts_y[collision_info_v1];
                
                final int v0 = collision_info_v0;
                final int v1 = collision_info_v1;
                final int dx = x1 - x0;
                final int dy = y1 - y0;
                
                float bounceVariation = 0;
                
                final boolean isPerimiterWall = 
                    this.isPerimiterVertex(v0) && this.isPerimiterVertex(v1);
                
                Wall info = wall_lookup[v0][v1];
                
                boolean doBounce = false; // flag to indicate bounce the missle
                switch (info.type) {
                case WALL_TYPE_DOOR:
                    if (isMegaGunActive(player) && info.state == DOOR_STATE_LOCKED) {
                        
                        int mdx = (x0+x1)/2;
                        int mdy = (y0+y1)/2;
                        
                        if (Utils.isCircleIntersectingLineSeg(m.x, m.y, m.x + m.dx, m.y + m.dy, mdx, mdy, this.PLAYER_MISSLE_SPEED/4)) {
                            addPlayerMsg(player, "LOCK Destroyed");
                            info.state = DOOR_STATE_CLOSED;
                            removePlayerMissle(player, i);
                            continue;
                        }
                    }
                    doBounce = true;
                    break;
                case WALL_TYPE_NORMAL:
                    if (isMegaGunActive(player)) {
                        if (!isPerimiterWall)
                            wallNormalDamage(info, 1);
                        removePlayerMissle(player, i);
                        continue;
                    } else {
                        doBounce = true;
                    }
                    break;  
                case WALL_TYPE_ELECTRIC: 
                    if (getFrameNumber() < info.frame)
                        break;
                    if (isMegaGunActive(player)) {
                        info.frame = getFrameNumber() + WALL_ELECTRIC_DISABLE_FRAMES;
                    }
                    removePlayerMissle(player, i);
                    continue;
                case WALL_TYPE_INDESTRUCTABLE:
                    doBounce = true;
                    break;
                case WALL_TYPE_PORTAL:
                    // TODO: Teleport missles too other portal?
                    removePlayerMissle(player, i);
                    continue;
                case WALL_TYPE_RUBBER:
                    doBounce = true;
                    info.frequency = Math.min(info.frequency + RUBBER_WALL_FREQENCY_INCREASE_MISSLE, RUBBER_WALL_MAX_FREQUENCY);
                    bounceVariation = info.frequency * 100;
                    
                    break;
                default: Utils.unhandledCase(info.type);
                }
                
                if (doBounce) {
                    bounceVectorOffWall(m.dx, m.dy, dx, dy, int_array);
                    if (bounceVariation != 0) {
                        float [] f_array = { int_array[0], int_array[1] };
                        
                        float degrees = bounceVariation * (Utils.flipCoin() ? -1 : 1); 
                        CMath.rotateVector(f_array, degrees);
                        m.dx = Math.round(f_array[0]);
                        m.dy = Math.round(f_array[1]);
                    } else {
                        m.dx = int_array[0];
                        m.dy = int_array[1];
                    }                    
                    // need to check for a collision again
                    if (collisionScanLine(m.x, m.y, m.x + m.dx, m.y + m.dy)) {
                        this.removePlayerMissle(player, i);
                        continue;
                    }
                    
                }
            }
            
            done = false;
            
            // look for collision with enemies
            for (e = 0; e < num_enemies; e++) {
                if (!isOnScreen(enemy_x[e], enemy_y[e]))
                    continue;

                if (enemy_type[e] == ENEMY_INDEX_JAWS || enemy_type[e] == ENEMY_INDEX_LAVA)
                    continue;
                
                if (Utils.isCircleIntersectingLineSeg(m.x, m.y, m.x + m.dx, m.y + m.dy, enemy_x[e], enemy_y[e], enemy_radius[enemy_type[e]])) {
                    float factor = (ENEMY_THUG_PUSHBACK - (float) difficulty) / PLAYER_MISSLE_SPEED;
                    final int dx = Math.round(player.missles[i].dx * factor);
                    final int dy = Math.round(player.missles[i].dy * factor);
                    
                    if (!enemyHit(player, e, dx, dy)) {
                        // bounce the projectile off (if not megaGun)
                        if (!isMegaGunActive(player)) {
                            
                        }
                    }
                    if (!isMegaGunActive(player)) {
                        removePlayerMissle(player, i);
                        done = true;
                        break;
                    }
                }
            }
            
            if (done)
                continue;
            
            // look for collisions with enemy_tank_missles
            for (e = 0; e < num_tank_missles; e++) {
                m2 = tank_missle[e];
                
                if (!isOnScreen(m2.x, m2.y))
                    continue;
                
                if (Utils.isPointInsideCircle(m.x, m.y, m2.x, m2.y, TANK_MISSLE_RADIUS)
                        || Utils.isPointInsideCircle(m.x + m.dx, m.y + m.dy, m2.x, m2.y, TANK_MISSLE_RADIUS)) {
                    removePlayerMissle(player, i);
                    removeTankMissle(e);
                    done = true;
                    break;
                }
            }
            
            if (done)
                continue;
            
            // look for collisions with snake_missles
            final float mx = Math.min(m.x, m.x + m.dx)-2;
            final float my = Math.min(m.y, m.y + m.dy)-2;
            final float mw = Math.abs(m.dx)+4;
            final float mh = Math.abs(m.dy)+4;          
            
            for (e = 0; e < num_snake_missles; e++) {
                MissleSnake s = snake_missle[e];
                
                if (!isOnScreen(s.x, s.y))
                    continue;
                
                int cResult = this.collisionMissleSnakeRect(s, mx, my, mw, mh);
                if (cResult == 0 || isMegaGunActive(player)) {
                    killSnakeMissle(e);
                    removePlayerMissle(player, i);
                    done = true;
                    break;
                } else if (cResult > 0) {
                    // split the snakle
                    // removeSnakeMissle(e);
                    
                    int nIndex = addSnakeMissle(Math.round(mx + mw / 2), Math.round(my + mh / 2));
                    if (nIndex < 0)
                        break;
                    
                    MissleSnake newSnake = this.snake_missle[nIndex];
                    
                    // assign
                    int cc = 0;
                    for (int ii = cResult; ii < MAX_SNAKE_MISSLES && s.dir[ii] >= 0; ii++) {
                        newSnake.dir[cc++] = s.dir[ii];
                        s.dir[ii] = -1;
                    }
                    
                    // The missle lives on!
                    // removePlayerMissle(e);
                    // done = true;
                    break;
                }
            }
            
            i++;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // return true if the wall is destroyed
    private boolean wallNormalDamage(Wall info, int amount) {
        info.health -= amount;
        if (info.health <= 0) {
            this.removeEdge(info.v0, info.v1);
            return true;
        }
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updateRobotMissles() {
        MissleFloat mf;
        
        // update enemy missles, cant collide with walls
        for (int i = 0; i < num_enemy_missles;) {
            mf = enemy_missle[i];
            
            // remove the missle if below the player, but not too soon
            //if (mf.duration > 30 && mf.y > player_y + playerRadius + 30) {
            //    removeEnemyMissle(i);
            //    continue;
            //}
            
            if (mf.duration == 0) {
                removeEnemyMissle(i);
                continue;
            }
            
            mf.x += mf.dx;
            mf.y += mf.dy;
            
            // TODO : make gravity more severe when player is close or very
            // below our y
            mf.dy += ENEMY_PROJECTILE_GRAVITY;
            mf.duration--;
            
            for (int ii=0; ii<num_players; ii++) {
                Player player = players[ii];
                final int playerRadius = getPlayerRadius(player);
                
                if (isBarrierActive(player)) {
                    float dx1 = mf.x - player.x;
                    float dy1 = mf.y - player.y;
                    float dot1 = dx1 * mf.dx + dy1 * mf.dy;
                    if (dot1 <= 0
                            && Utils.isPointInsideCircle(Math.round(mf.x), Math.round(mf.y), player.x, player.y, PLAYER_RADIUS_BARRIER + ENEMY_PROJECTILE_RADIUS)) {
                        mf.dx *= -1;
                        mf.dy *= -1;
                    }
                    
                } else if (Utils.isPointInsideCircle(Math.round(mf.x), Math.round(mf.y), player.x, player.y, playerRadius + ENEMY_PROJECTILE_RADIUS)) {
                    playerHit(player, HIT_TYPE_ROBOT_MISSLE, i);
                }
            }
            i++;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updateTankMissles() {
        int x0, y0, x1, y1;
        MissleInt m;
        
        // update Tank Missles
        for (int i = 0; i < num_tank_missles;) {
            m = tank_missle[i];
            
            if (m.duration == 0) {
                removeTankMissle(i);
                continue;
            }
            
            if (!isOnScreen(m.x, m.y)) {
                removeTankMissle(i);
                continue;
            }
            
            boolean done = false;
            for (int ii=0; ii<num_players; ii++) {
                Player player = players[ii];
                int playerRadius = getPlayerRadius(player);                

                if (isBarrierActive(player)) {
                    int dx1 = m.x - player.x;
                    int dy1 = m.y - player.y;
                    int dot1 = dx1 * m.dx + dy1 * m.dy;
                    if (dot1 <= 0 && Utils.isPointInsideCircle(m.x, m.y, player.y, player.y, PLAYER_RADIUS_BARRIER + TANK_MISSLE_RADIUS)) {
                        m.dx *= -1;
                        m.dy *= -1;
                    }
                } else if (Utils.isPointInsideCircle(m.x, m.y, player.x, player.y, playerRadius + TANK_MISSLE_RADIUS)) {
                    playerHit(player, HIT_TYPE_TANK_MISSLE, i);
                    done = true;
                    break;
                }
            }
            
            if (done)
                continue;
            
            // look for collision with walls
            if (collisionScanCircle(m.x + m.dx, m.y + m.dy, TANK_MISSLE_RADIUS)) {
                x0 = maze_verts_x[collision_info_v0];
                y0 = maze_verts_y[collision_info_v0];
                x1 = maze_verts_x[collision_info_v1];
                y1 = maze_verts_y[collision_info_v1];
                
                // do the bounce off algorithm
                bounceVectorOffWall(m.dx, m.dy, x1 - x0, y1 - y0, int_array);
                
                m.dx = int_array[0];
                m.dy = int_array[1];
            }
            
            m.x += m.dx;
            m.y += m.dy;
            m.duration--;
            i++;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updateMissles() {
        for (int i=0; i<num_players; i++)
            updatePlayerMissles(players[i]);
        updateRobotMissles();
        updateTankMissles();
        for (int i=0; i<num_players; i++)
            updateSnakeMissles(players[i]);
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw all missles to the screen
    private void drawPlayerMissles(Player player, AGraphics g) {
        
        int thickness = 1;
        if (isMegaGunActive(player)) {
            g.setColor(g.CYAN);
            thickness = 2;
        } else {
            g.setColor(g.BLUE);
        }
        
        // Draw player's missles as lines
        for (int i = 0; i < player.num_missles; i++) {
            MissleInt m = player.missles[i];
            
            int x = m.x - screen_x;
            int y = m.y - screen_y;
            
            g.drawLine(x, y, x + m.dx, y + m.dy, thickness);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw all missles to the screen
    private void drawEnemyMissles(AGraphics g) {
        // Draw the enemy missles as orange dots
        g.setColor(g.ORANGE);
        int x, y;
        for (int i = 0; i < num_enemy_missles; i++) {
            MissleFloat m = enemy_missle[i];
            
            x = Math.round(m.x) - ENEMY_PROJECTILE_RADIUS - screen_x;
            y = Math.round(m.y) - ENEMY_PROJECTILE_RADIUS - screen_y;
            
            g.drawFilledOval(x, y, ENEMY_PROJECTILE_RADIUS * 2, ENEMY_PROJECTILE_RADIUS * 2);
        }
        
        for (int i = 0; i < num_tank_missles; i++) {
            MissleInt m = tank_missle[i];
            
            x = m.x - TANK_MISSLE_RADIUS - screen_x;
            y = m.y - TANK_MISSLE_RADIUS - screen_y;
            
            g.setColor(g.GREEN);
            g.drawOval(x, y, TANK_MISSLE_RADIUS * 2, TANK_MISSLE_RADIUS * 2);
            g.setColor(g.YELLOW);
            g.drawOval(x + 2, y + 2, TANK_MISSLE_RADIUS * 2 - 4, TANK_MISSLE_RADIUS * 2 - 4);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // draw all missles to the screen
    private void drawMissles(AGraphics g) {
        for (int i=0 ;i<num_players; i++)
            drawPlayerMissles(players[i], g);
        drawEnemyMissles(g);
        drawSnakeMissles(g);
    }
    
    // -----------------------------------------------------------------------------------------------
    // Update the player powerup
    private void updatePlayerPowerup(Player player) {
        
        if (player.powerup_duration++ > PLAYER_POWERUP_DURATION
                && (player.powerup != POWERUP_GHOST || !collisionScanCircle(player.x, player.y, getPlayerRadius(player)))) {
            // disable powerup
            player.powerup = -1;
            //mgun_collision_v0 = mgun_collision_v1 = -1;
            //mgun_hit_count = 0;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isStunned(Player player) {
        if (player.state == PLAYER_STATE_SPECTATOR)
            return false;
        float mag2 = player.stun_dx*player.stun_dx + player.stun_dy*player.stun_dy;
        return mag2 > 0.1f;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void setPlayerStunned(int playerIndex, float dx, float dy, float force) {
        float len = Utils.fastLen(dx, dy);
        if (len < 1)
            len = 1;
        Player player = players[playerIndex];
        float len_inv = 1.0f / len * force;
        player.stun_dx = dx * len_inv;
        player.stun_dy = dy * len_inv;
        player.hulk_charge_frame = 0;
        for (int angle = 0; angle<360; angle += 45)
            this.addParticle(player.x, player.y, PARTICLE_TYPE_PLAYER_STUN, Utils.randRange(10,20), playerIndex, angle);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void getCollisionInfo(int [] wallPts) {
        wallPts[0] = maze_verts_x[collision_info_v0]; // p0.x
        wallPts[1] = maze_verts_y[collision_info_v0]; // p0.y
        wallPts[2] = maze_verts_x[collision_info_v1]; // p1.x
        wallPts[3] = maze_verts_y[collision_info_v1]; // p1.y
    }
    
    // -----------------------------------------------------------------------------------------------
    private void playerTouchDoor(Player player, Wall door) {
        switch (door.state) {
        case DOOR_STATE_LOCKED:
            if (!isHulkActive(player) && player.keys>0) {
                addPlayerMsg(player, "UNLOCKING DOOR");
                door.state = DOOR_STATE_CLOSED;
                door.frame = getFrameNumber();
                player.keys--;
            }
            break;
        case DOOR_STATE_CLOSING: 
            door.state = DOOR_STATE_OPENING;
            {
                int framesElapsed = getFrameNumber() - door.frame;
                framesElapsed = this.DOOR_SPEED_FRAMES - framesElapsed;
                door.frame = getFrameNumber() - framesElapsed;
            }
            
            break;
        case DOOR_STATE_CLOSED:
            door.state = DOOR_STATE_OPENING;
            door.frame = getFrameNumber();
            break;
        case DOOR_STATE_OPENING: break;
        case DOOR_STATE_OPEN: break;
        default: Utils.unhandledCase(door.state); break;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isWallActive(Wall info) {
        if (info == null || info.type == WALL_TYPE_NONE)
            return false;
        if (info.type == WALL_TYPE_ELECTRIC && info.frame > getFrameNumber())
            return false;
        return true;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Update the player
    private void updatePlayer(Player player) {
        
        final int playerRadius = getPlayerRadius(player);
        Arrays.fill(player.barrier_electric_wall, -1);
        
        if (player.next_state_frame <= getFrameNumber()) {
            switch (player.state){
                case PLAYER_STATE_TELEPORTED:
                    player.dx = player.dy = 0;
                case PLAYER_STATE_SPAWNING:
                    player.state = PLAYER_STATE_ALIVE; 
                    break;
                case PLAYER_STATE_EXPLODING:
                    if (player.lives > 0) {
                        player.lives --;
                        resetPlayer(player);
                        player.state = PLAYER_STATE_SPAWNING;
                    } else {
                        player.state = PLAYER_STATE_SPECTATOR;
                    }
                    break;
            }
        }

        // see if the player has changed the cell location
        int sx = player.cellXY[0];
        int sy = player.cellXY[1];
        
        computeCell(player.x, player.y, player.cellXY);
        if (player.cellXY[0] != sx || player.cellXY[1] != sy) {
            int [] endXY = new int[2];
            computeCell(this.end_x, this.end_y, endXY);
            player.path.clear();
            this.findPath(maze_cells, player.cellXY[0], player.cellXY[1], endXY[0], endXY[1], player.path);
        }
        
        // look for player trying to fire a missle
        if (player.state == PLAYER_STATE_ALIVE && player.firing) {
            if (player.last_shot_frame + getPlayerShotFreq(player) < getFrameNumber()) {
                player.last_shot_frame = getFrameNumber();
                if (isHulkActive(player)) {
                    // charge!
                    playerHulkCharge(player);
                } else {
                    addPlayerMissle(player);
                }
            }
        }
        
        if (player.powerup >= 0)
            updatePlayerPowerup(player);
        
        if (isHulkActive(player)) {
            if (player.scale < PLAYER_HULK_SCALE)
                player.scale += PLAYER_HULK_GROW_SPEED;
        }
        else if (player.scale > 1.0f) {
            player.scale -= PLAYER_HULK_GROW_SPEED;
        }
        
        player.scale = Utils.clamp(player.scale, 1.0f, PLAYER_HULK_SCALE);      
        
        float dx = 0;
        float dy = 0;

        if (player.state != PLAYER_STATE_EXPLODING) {
            if (isHulkActiveCharging(player)) {
                dx = player.hulk_charge_dx;
                dy = player.hulk_charge_dy;
            } else {
                dx = player.dx;
                dy = player.dy;
            }
            
            // try to move the player
            if (isStunned(player)) {
                dx = player.stun_dx;
                dy = player.stun_dy;
                player.stun_dx *= 0.9f;
                player.stun_dy *= 0.9f;
                if (dx*dx + dy*dy < 0.1) {
                    player.stun_dx = player.stun_dy = 0;
                    dx = dy = 0;
                }
            } 
        }
        
        if (dx != 0 || dy != 0) {
            if (isVisibilityEnabled())
                updatePlayerVisibility(player);
            player.movement++;
            player.dir = getPlayerDir(dx, dy);
            
            int px = Math.round(player.x + dx);
            int py = Math.round(player.y + dy);
            
            // do collision detect against walls
            // working good
            int collisionRadius = playerRadius;
            
            boolean collision = collisionScanCircle(px, py, collisionRadius);

            if (!isPerimiterWall(collision_info_v0, collision_info_v1)) {
                if (canPassThroughWalls(player))
                    collision = false;
            }
            
            if (collision) {
            
                //out.println("Player hit wall type [" + getWallTypeString(collision_info_wall_type) + "] info [" + collision_info_wall_info + "]");
                Wall info = wall_lookup[collision_info_v0][collision_info_v1];
                
                if (info.type == WALL_TYPE_ELECTRIC && isBarrierActive(player)) {
                    // no collision
                } else {
                
                    if (doPlayerHitWall(player, info, dx, dy))
                        return;
                    
                    int wallx0 = maze_verts_x[collision_info_v0];
                    int wally0 = maze_verts_y[collision_info_v0];
                    int wallx1 = maze_verts_x[collision_info_v1];
                    int wally1 = maze_verts_y[collision_info_v1];
                    
                    float [] pos = { px, py };
                    
                    this.fixPositionFromWall(pos, wallx0, wally0, wallx1, wally1, playerRadius);
                    
                    // reassign
                    px = Math.round(pos[0]);
                    py = Math.round(pos[1]);
                    
                    // now search the other walls
                    for (int i=1; i<collision_verts.length; i++) {
                        int v1 = collision_verts[i];
                        if (v1 == collision_info_v1)
                            continue;
                        
                        if (v1 < 0 || v1 >= MAZE_NUM_VERTS)
                            continue;
                        
                        info = wall_lookup[collision_info_v0][v1];
                        if (!isWallActive(info))
                            continue;
                        
                        int x1 = maze_verts_x[v1];
                        int y1 = maze_verts_y[v1];
                        
                        if ((info.type == WALL_TYPE_DOOR && collisionDoorCircle(info, wallx0, wally0, x1, y1, px, py, playerRadius)) ||
                                Utils.isCircleIntersectingLineSeg(wallx0, wally0, x1, y1, px, py, playerRadius)) {
                            
                            if (doPlayerHitWall(player, info, dx, dy))
                                return;
                            
                            // check the dot product of orig wall and this wall
                            int dx0 = wallx1 - wallx0;
                            int dy0 = wally1 - wally0;
                            int dx1 = x1 - wallx0;
                            int dy1 = y1 - wally0;
                            int dot = dx0*dx1 + dy0+dy1;
                            if (dot > 0)
                                return;
                            
                            this.fixPositionFromWall(pos, wallx0, wally0, x1, y1, playerRadius);
                            // reassign
                            px = Math.round(pos[0]);
                            py = Math.round(pos[1]);
                            break;
                        }
                    }
                }
            }
            
            int new_dx = px - player.x;
            int new_dy = py - player.y;
            
            player.target_x += new_dx;
            player.target_y += new_dy;
            
            if (getFrameNumber() % 4 == 0) {                
                if (isHulkActiveCharging(player)) {
                    addPlayerTracer(player, player.x, player.y, player.dir, G.GREEN);
                } else if (player.powerup == this.POWERUP_SUPER_SPEED) {
                    addPlayerTracer(player, player.x, player.y, player.dir, G.YELLOW);
                }
            }
            
            player.x = px;
            player.y = py;
            
            if (enemy_robot_speed < (ENEMY_ROBOT_MAX_SPEED + difficulty) && player.movement % (ENEMY_ROBOT_SPEED_INCREASE - difficulty * 10) == 0)
                enemy_robot_speed++;
        } else {
            player.movement = 0;
        }
        
        screen_x = player.x - screen_width / 2;
        screen_y = player.y - screen_height / 2;
        
        if (game_type == GAME_TYPE_CLASSIC) {
            if (total_enemies == 0 && num_particles == 0) {
                game_level++;
                buildAndPopulateLevel();
            }
        } else {
            if (Utils.isPointInsideCircle(player.x, player.y, end_x, end_y, playerRadius + 10)) {
                game_level++;
                buildAndPopulateLevel();
            }
        }
        
    }
    
    private int getPlayerIndex(Player player) {
        for (int i=0; i<num_players; i++)
            if (players[i] == player)
                return i;
        assert(false);
        return -1;
    }
    
    // -----------------------------------------------------------------------------------------------
    // return true when the collision is a hit, false if we should
    // fix the position
    private boolean doPlayerHitWall(Player player, Wall info, float dx, float dy) {
        
        int playerIndex = getPlayerIndex(player);
        
        switch (info.type) {
        case WALL_TYPE_ELECTRIC:
            if (playerHit(player, HIT_TYPE_ELECTRIC_WALL, 0)) {
                return true;
            }           
            break;
        case WALL_TYPE_PORTAL:
            if (!isHulkActive(player))
                playerTeleport(player, info.p0, info.p1);
            else
                return false;
            break;
            
        case WALL_TYPE_DOOR:
            playerTouchDoor(player, info);
            return false;
            
        case WALL_TYPE_NORMAL:
            if (isHulkActiveCharging(player)) { 
                // do damage on the wall
                final int damage = Utils.rand() % (this.WALL_NORMAL_HEALTH/4) + 5; 
                if (wallNormalDamage(info, damage))
                    break;
                this.setPlayerStunned(playerIndex, -Math.round(dx), -Math.round(dy), 10);
            }
            return false;
            
        case WALL_TYPE_RUBBER:
            this.setPlayerStunned(playerIndex, -Math.round(dx), -Math.round(dy), 10);
            return false;
            
        default:
            return false;
        
        }
        return true;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void fixPositionFromWall(float [] pos, float x0, float y0, float x1, float y1, float radius) {
        // get distanse^2 too wall
        float d2 = Utils.distSqPointSegment(pos[0], pos[1], x0, y0, x1, y1);
        
        // compute new dx,dy along collision edge
        float vx = x0 - x1;
        float vy = y0 - y1;
        
        // compute normal of wall
        float nx = -vy;
        float ny = vx;
        
        // compute vector from w0 too p
        float pwdx = pos[0] - x0;
        float pwdy = pos[1] - y0;
        
        // compute dot of pd n
        float dot = nx * pwdx + ny * pwdy;
        
        // reverse direction of normal if neccessary
        if (dot < 0) {
            nx *= -1;
            ny *= -1;
        }
        
        // normalize the normal
        float nmag = (float) Math.sqrt(nx * nx + ny * ny);
        float nmag_inv = 1.0f / nmag;
        
        nx *= nmag_inv;
        ny *= nmag_inv;
        
        // compute vector along n to adjust p
        float d_ = radius - (float) Math.sqrt(d2);
        float nx_ = nx * d_;
        float ny_ = ny * d_;
        
        // compute p prime, the new position, a correct distance
        // from the wall
        float px_ = (float) pos[0] + nx_;
        float py_ = (float) pos[1] + ny_;
        
        // reassign
        pos[0] = px_;
        pos[1] = py_;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void playerHulkCharge(Player player) {
        player.hulk_charge_frame = getFrameNumber();
        
        //addPlayerMsg("HULK SMASH!!!");
        player.firing = false;
        
        player.hulk_charge_dx = player.hulk_charge_dy = 0;
        
        if (player.dx != 0 || player.dy != 0) {
            //Utils.println("*** CHARGING ***");
            if (player.dx != 0) {
                if (player.dx < 0)
                    player.hulk_charge_dx = player.dx - PLAYER_HULK_CHARGE_SPEED_BONUS;
                else
                    player.hulk_charge_dx = player.dx + PLAYER_HULK_CHARGE_SPEED_BONUS;
            }
            if (player.dy != 0) {
                if (player.dy < 0)
                    player.hulk_charge_dy = player.dy - PLAYER_HULK_CHARGE_SPEED_BONUS;
                else
                    player.hulk_charge_dy = player.dy + PLAYER_HULK_CHARGE_SPEED_BONUS;
            }
        } else {
            int speed = getPlayerSpeed(player);
            switch (player.dir) {
            case DIR_UP: player.hulk_charge_dy = -speed; break; 
            case DIR_RIGHT: player.hulk_charge_dx = speed; break; 
            case DIR_DOWN: player.hulk_charge_dy = speed; break;
            case DIR_LEFT: player.hulk_charge_dx = -speed; break;
            default: Utils.unhandledCase(player.dir); break;
            }
        }
    }
    
    private final int PLAYER_HULK_CHARGE_FRAMES = 20;
    
    private boolean isHulkActiveCharging(Player player) {
        return isHulkActive(player) && getFrameNumber() - player.hulk_charge_frame < PLAYER_HULK_CHARGE_FRAMES;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void enemyTeleport(int enemyIndex, int v0, int v1) {
        final int radius = enemy_radius[enemyIndex] + 10;
        // get wall defined by v0, v1
        final int x0 = maze_verts_x[v0];
        final int y0 = maze_verts_y[v0];
        final int x1 = maze_verts_x[v1];
        final int y1 = maze_verts_y[v1];
        // midpoint
        final int mx = (x0+x1)/2;
        final int my = (y0+y1)/2;
        // delta
        final int dx = x1-x0;
        final int dy = y1-y0;
        // normal
        final int nx = -dy;
        final int ny = dx;
        final int len = Utils.fastLen(nx, ny)-1;
        final float len_inv = 1.0f/len;
        
        final int newX = mx + Math.round(len_inv * nx * radius);
        final int newY = my + Math.round(len_inv * ny * radius);
        
        enemy_x[enemyIndex] = newX;
        enemy_y[enemyIndex] = newY;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void playerTeleport(Player player, int v0, int v1) {
        Utils.println("PLAYER TELEPORT v0 = " + v0 + " v1 = " + v1);
        //player.teleported = true;
        player.state = this.PLAYER_STATE_TELEPORTED;
        player.next_state_frame = getFrameNumber() + 1;
        final int radius = getPlayerRadius(player) + 10;
        // get wall defined by v0, v1
        final int x0 = maze_verts_x[v0];
        final int y0 = maze_verts_y[v0];
        final int x1 = maze_verts_x[v1];
        final int y1 = maze_verts_y[v1];
        // midpoint
        final int mx = (x0+x1)/2;
        final int my = (y0+y1)/2;
        // delta
        final int dx = x1-x0;
        final int dy = y1-y0;
        // normal
        final int nx = -dy;
        final int ny = dx;
        final int len = Utils.fastLen(nx, ny)-1;
        final float len_inv = 1.0f/len;
        // make the player's motion change to the closest to that of the normal
        if (player.dx != 0 || player.dy != 0) {
            player.dx = player.dy = 0;
            // clear the key_down flags
            int speed = getPlayerSpeed(player);
            if (Math.abs(nx) > Math.abs(ny)) {
                player.dx = nx < 0 ? -speed : speed; 
            } else {
                player.dy = ny < 0 ? -speed : speed;
            }
        }
        
        final int newX = mx + Math.round(len_inv * nx * radius);
        final int newY = my + Math.round(len_inv * ny * radius);
        player.x = newX;
        player.y = newY;
    }
    
    private int getPlayerShotFreq(Player player) {
        if (isMegaGunActive(player))
            return PLAYER_SHOT_FREQ_MEGAGUN;
        return PLAYER_SHOT_FREQ;
    }
    
    // enum
    private final int HIT_TYPE_ENEMY            = 0;
    private final int HIT_TYPE_TANK_MISSLE      = 1;
    private final int HIT_TYPE_SNAKE_MISSLE     = 2;
    private final int HIT_TYPE_ROBOT_MISSLE     = 3;    
    private final int HIT_TYPE_ELECTRIC_WALL    = 4;
    
    // -----------------------------------------------------------------------------------------------
    // Player hit event
    // return true if player takes damage, false if barrier ect.
    private boolean playerHit(Player player, int hitType, int index) {
        if (isInvincible(player))
            return false;
        
        //final float playerRadius = getPlayerRadius(player);
        int playerIndex = getPlayerIndex(player);
        
        switch (hitType) {
        
        case HIT_TYPE_ELECTRIC_WALL:
            if (isHulkActive(player)) {
                setPlayerStunned(playerIndex, -player.dx, -player.dy, 30);
                return false;
            } else if (isBarrierActive(player)) {
                return false;
            } else {
                player.state = PLAYER_STATE_EXPLODING;
                player.next_state_frame = getFrameNumber() + PLAYER_DEATH_FRAMES;
                return true;
            }
        case HIT_TYPE_ENEMY:
            
            if (isHulkActive(player) || player.scale > 1.2f) {
                if (isHulkActiveCharging(player) && enemyHit(player, index, player.dx, player.dy)) {
                    if (Utils.randRange(0,6)==0)
                        addPlayerMsg(player, "HULK SMASH!");
                } else {
                    if (Utils.rand() % 10 == 0) {
                        addPlayerMsg(player, "Hulk confused ???");
                    }
                    
                    this.setPlayerStunned(playerIndex, player.x-enemy_x[index], player.y-enemy_y[index], ENEMY_ROBOT_FORCE);
                }
                return false;
            }
            // fall through
            
        case HIT_TYPE_TANK_MISSLE:
        case HIT_TYPE_ROBOT_MISSLE:
            if (isHulkActive(player)) {
                // unhulk
                setDebugEnabled(Debug.HULK, false);
                if (player.powerup == POWERUP_HULK)
                    player.powerup = -1;
                // bounce the missle
                this.enemy_missle[index].dx *= -1;
                this.enemy_missle[index].dy *= -1;
                // stun player
                setPlayerStunned(playerIndex, Math.round(enemy_missle[index].dx), 
                        Math.round(enemy_missle[index].dy),
                        ENEMY_PROJECTILE_FORCE);
                
                return false;
            } else  if (!isGhostActive(player)) {
                player.hit_type = hitType;
                player.hit_index = index;
                player.state = PLAYER_STATE_EXPLODING;
                player.next_state_frame = getFrameNumber() + PLAYER_DEATH_FRAMES;
                return true;
            }
            break;
        case HIT_TYPE_SNAKE_MISSLE:
            // TODO: Snake is attached to player, slowing player down
            //snake_missle[index].state = SNAKE_STATE_ATTACHED;
            return true;
        default:
            Utils.unhandledCase(hitType);
        break;
        }
        
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isVisibilityEnabled() {
        return game_visibility;
    }
    
    // -----------------------------------------------------------------------------------------------
    //boolean isElectricWallActive(WallInfo info) {
    //  return info.frame < getFrameNumber();
    //}
    
    // -----------------------------------------------------------------------------------------------
    private boolean isInvincible(Player player) {
        return isDebugEnabled(Debug.INVINCIBLE);
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isGhostActive(Player player) {
        return isDebugEnabled(Debug.GHOST) || player.powerup == POWERUP_GHOST;
    }
    
    private boolean canPassThroughWalls(Player player) {
        return isGhostActive(player) || isSpectator(player);
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isSpectator(Player player) {
        return player.state == PLAYER_STATE_SPECTATOR;
    }

    // -----------------------------------------------------------------------------------------------
    private boolean isBarrierActive(Player player) {
        return isDebugEnabled(Debug.BARRIER) || player.powerup == POWERUP_BARRIER;
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isMegaGunActive(Player player) {
        return player.powerup == this.POWERUP_MEGAGUN;
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean isHulkActive(Player player) {
        return isDebugEnabled(Debug.HULK) || player.powerup == POWERUP_HULK;
    }
    
    private final int DIR_UP = 0;
    private final int DIR_RIGHT  = 1;
    private final int DIR_DOWN = 2;
    private final int DIR_LEFT  = 3;
    
    // -----------------------------------------------------------------------------------------------
    // return 0,1,2,3 for player direction [NESW]
    private int getPlayerDir(float dx, float dy) {
        if (dx < 0)
            return DIR_LEFT;
        if (dx > 0)
            return DIR_RIGHT;
        if (dy < 0)
            return DIR_UP;
        return DIR_DOWN;
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getPlayerRadius(Player player) {
        int radius = Math.round(player.scale * PLAYER_RADIUS);
        return radius;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the Player
    private void drawPlayer(Player player, AGraphics g, int px, int py, int dir) {
        AColor color = null;
        if (player.scale == PLAYER_HULK_SCALE) {
            color = g.GREEN;
        } else if (player.scale > 1.0f) {
            int invScale = Math.round(255.0f * 1.0f-(player.scale-1.0f));
            color = g.makeColor(invScale, 255, invScale);
        }
        
        if (game_state == this.GAME_STATE_PLAY) {
            int numFrames = this.PLAYER_POWERUP_DURATION - PLAYER_POWERUP_WARNING_FRAMES;
            if (player.powerup > 0 && player.powerup_duration > numFrames) {
                if (getFrameNumber() % 8 < 4)
                    color = g.RED;
            }
        }
        
        drawPlayerBody(player, g, px+1, py, dir, color);
        drawPlayerEyes(player, g, px+1, py, dir);
        drawPlayerBarrier(player, g, px+1, py);
        /*
        if (isStunned(player)) {
            for (int i=0; i<5; i++)
                this.addParticle(player.x, player.y, PARTICLE_TYPE_PLAYER_STUN, Utils.randRange(10,20), player);
        } */  
        if (Utils.DEBUG_ENABLED) {
            g.setColor(g.BLUE);
            g.drawRect(px-1, py-1,3,3);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the Player's Body
    // TODO: Avoid using new by caching all the colors we want
    private void drawPlayerBody(Player player, AGraphics g, int px, int py, int dir, AColor optionalColor) {
        
        AColor priColor = g.LIGHT_GRAY;
        AColor secColor = g.DARK_GRAY;
        if (isGhostActive(player)) {
            if (optionalColor == null) {
                priColor = secColor = g.makeColor(200, 200, 200, 100); // lightgray
            } else {
                priColor = secColor = g.makeColor(optionalColor.getRed(),
                                                optionalColor.getGreen(),
                                                optionalColor.getBlue(),
                                                100.0f / 255);
            }
        } else {
            if (optionalColor != null) {
                priColor = secColor = optionalColor;
            }
        }       
        
        final float scale = player.scale;
        
        g.setColor(priColor);
        int walk = Math.round(((float) (player.movement % 3) - 1) * scale);
        
        int f1 = Math.round(1.0f * scale);
        int f2 = Math.round(2.0f * scale);
        int f3 = Math.round(3.0f * scale);
        int f4 = Math.round(4.0f * scale);
        int f6 = Math.round(6.0f * scale);
        int f8 = Math.round(8.0f * scale);
        int f10 = Math.round(10.0f * scale);
        int f12 = Math.round(12.0f * scale);
        int f14 = Math.round(14.0f * scale);
        int f16 = Math.round(16.0f * scale);
        int f20 = Math.round(20.0f * scale);
        int f22 = Math.round(22.0f * scale);
        int f24 = Math.round(24.0f * scale);
        // draw head
        g.drawFilledRect(px - f10, py - f10, f20, f4);
        g.drawFilledRect(px - f8, py - f12, f16, f8);
        // draw body
        g.drawFilledRect(px - f4, py - f14, f8, f22);
        if (dir == 0 || dir == 2) {
            g.drawFilledRect(px - f6, py - f2, f12, f6);
            g.drawFilledRect(px - f12, py, f24, f4);
            // draw arms
            g.drawFilledRect(px - f12, py + f4, f4, f6);
            g.drawFilledRect(px + f8, py + f4, f4, f6);
            // draw legs
            g.drawFilledRect(px - f6, py + f6 + walk, f4, f10);
            g.drawFilledRect(px + f2, py + f6 - walk, f4, f10);
            g.drawFilledRect(px - f8, py + f12 + walk, f2, f4);
            g.drawFilledRect(px + f6, py + f12 - walk, f2, f4);
        } else if (dir == 1) {
            // body
            g.drawFilledRect(px - f6, py - f2, f10, f10);
            // legs
            g.drawFilledRect(px - f6, py + f8 + walk, f4, f8);
            g.drawFilledRect(px - f2, py + f12 + walk, f2, f4);
            g.setColor(secColor);
            g.drawFilledRect(px + f1, py + f8 - walk, f4, f8);
            g.drawFilledRect(px + f3, py + f12 - walk, f2, f4);
            // arm
            g.setColor(priColor);
            g.drawFilledRect(px - f4, py - f2, f4, f6);
            g.drawFilledRect(px - f2, py + f2, f4, f4);
            g.drawFilledRect(px, py + f4, f4, f4);
        } else {
            // body
            g.drawFilledRect(px - f4, py - f2, f10, f10);
            // legs
            g.drawFilledRect(px + f2, py + f8 + walk, f4, f8);
            g.drawFilledRect(px, py + f12 + walk, f2, f4);
            g.setColor(secColor);
            g.drawFilledRect(px - f6, py + f6 - walk, f4, f8);
            g.drawFilledRect(px - f8, py + f10 - walk, f2, f4);
            // arm
            g.setColor(priColor);
            g.drawFilledRect(px, py - f2, f4, f6);
            g.drawFilledRect(px - f2, py + f2, f4, f4);
            g.drawFilledRect(px - f4, py + f4, f4, f4);
        }
        
        // yell: "HULK SMASH!" when run over people, robot and walls (not thugs,
        // they bump back)
        // also, cant shoot when hulk
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawPlayerEyes(Player player, AGraphics g, int px, int py, int dir) {
        
        final float scale = player.scale;
        
        //int f1 = Math.round(1.0f * scale);
        int f2 = Math.round(2.0f * scale);
        //int f3 = Math.round(3.0f * scale);
        int f4 = Math.round(4.0f * scale);
        //int f6 = Math.round(6.0f * scale);
        int f8 = Math.round(8.0f * scale);
        int f10 = Math.round(10.0f * scale);
        //int f12 = Math.round(12.0f * scale);
        //int f14 = Math.round(14.0f * scale);
        int f16 = Math.round(16.0f * scale);
        //int f20 = Math.round(20.0f * scale);
        //int f22 = Math.round(22.0f * scale);
        int f24 = Math.round(24.0f * scale);
        if (dir == 2) {
            // draw the eye
            g.setColor(g.BLACK);
            g.drawFilledRect(px - f8, py - f10, f16, f4);
            g.setColor(g.RED);
            int index = getFrameNumber() % 12;
            if (index > 6)
                index = 12 - index;
            g.drawFilledRect(px - f8 + (index * f2), py - f10, f4, f4);
        } else if (dir == 1) {
            g.setColor(g.BLACK);
            g.drawFilledRect(px, py - f10, f10, f4);
            g.setColor(g.RED);
            int index = getFrameNumber() % 12;
            if (index < 4)
                g.drawFilledRect(px + (index * f2), py - f10, f4, f4);
            else if (index >= 8)
                g.drawFilledRect(px + (f24 - (index * f2)), py - f10, f4, f4);
        } else if (dir == 3) {
            g.setColor(g.BLACK);
            g.drawFilledRect(px - f10, py - f10, f10, f4);
            g.setColor(g.RED);
            int index = getFrameNumber() % 12;
            if (index < 4)
                g.drawFilledRect(px - f10 + (index * f2), py - f10, f4, f4);
            else if (index >= 8)
                g.drawFilledRect(px - f10 + (f24 - (index * f2)), py - f10, f4, f4);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawPlayerBarrierElectricWall2(Player player, AGraphics g, int x, int y) {

        // we are touching an electric wall, so we become 1 unit.
        // the end points of the wall are in the array:
        int wx0 = player.barrier_electric_wall[0];
        int wy0 = player.barrier_electric_wall[1];
        int wx1 = player.barrier_electric_wall[2];
        int wy1 = player.barrier_electric_wall[3];

        // compute deltas betwen p and endpofloats
        float dx0 = x-wx0;
        float dy0 = y-wy0;
        float dx1 = x-wx1;
        float dy1 = y-wy1;
        
        float radius = PLAYER_RADIUS_BARRIER;
        
        for (int c=0; c<2; c++) {
            float x0 = 1;
            float y0 = 0;
            float r0 = Utils.randFloatX(3) + radius;
            float sr = r0;
            
            for (int i = 0; i < STATIC_FIELD_SECTIONS - 1; i++) {
                float x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T;
                float y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T;
                float r1 = Utils.randFloatX(3) + radius;
                
                int lx0 = Math.round(x0 * r0);
                int ly0 = Math.round(y0 * r0);
                int lx1 = Math.round(x1 * r1);
                int ly1 = Math.round(y1 * r1);
                
                g.drawLine(x+lx0, y+ly0, x+lx1, y+ly1);
                
                if (Utils.rand() % 5 == 0) {
                    
                    float dot0 = lx0*dx0 + ly0*dy0;
                    float dot1 = lx0*dx1 + ly0*dy1;             
                    
                    if (dot0 <= 0) {
                        this.drawElectricWall_r(g, x+lx0, y+ly0, wx0, wy0, 2);
                    }
                    
                    if (dot1 <= 0) {
                        this.drawElectricWall_r(g, x+lx0, y+ly0, wx1, wy1, 2);
                    }
                }
                
                x0 = x1;
                y0 = y1;
                r0 = r1;
            }
            
            int lx0 = Math.round(x0 * r0);
            int ly0 = Math.round(y0 * r0);
            int lx1 = Math.round(sr);
            int ly1 = Math.round(0);
            
            g.drawLine(x+lx0, y+ly0, x+lx1, y+ly1);
            
            float dot0 = lx0*dx0 + ly0*dy0;
            float dot1 = lx0*dx1 + ly0*dy1;
            
            if (dot0 <= 0) {
                this.drawElectricWall_r(g, x+lx0, y+ly0, wx0, wy0, 2);
            }
            
            if (dot1 <= 0) {
                this.drawElectricWall_r(g, x+lx0, y+ly0, wx1, wy1, 2);
            }

            g.drawLine(x + lx0, y + ly0, x + lx1, y + ly1);
            
        }
        
        
    }

    // -----------------------------------------------------------------------------------------------
    private void drawPlayerBarrierElectricWall3(Player player, AGraphics g, int x, int y) {

        // we are touching an electric wall, so we become 1 unit.
        // the end points of the wall are in the array:
        int wx0 = player.barrier_electric_wall[0];
        int wy0 = player.barrier_electric_wall[1];
        int wx1 = player.barrier_electric_wall[2];
        int wy1 = player.barrier_electric_wall[3];

        float radius = PLAYER_RADIUS_BARRIER;
        
        for (int c=0; c<2; c++) {
            float x0 = 1;
            float y0 = 0;
            float r0 = Utils.randFloatX(3) + radius;
            float sr = r0;

            // dest point of field to connect wall pts to
            int bx0=0;
            int by0=0;
            int bx1=0;
            int by1=0;

            // closest static field pt
            float bestDot0 = Float.MAX_VALUE;
            float bestDot1 = Float.MAX_VALUE;
            
            for (int i = 0; i < STATIC_FIELD_SECTIONS; i++) {
                float x1 = x0 * STATIC_FIELD_COS_T - y0 * STATIC_FIELD_SIN_T;
                float y1 = y0 * STATIC_FIELD_SIN_T + x0 * STATIC_FIELD_COS_T;
                float r1 = Utils.randFloatX(3) + radius;
                
                if (i == STATIC_FIELD_SECTIONS-1) {
                    r1 = sr;
                }
                
                int lx0 = x+Math.round(x0 * r0);
                int ly0 = y+Math.round(y0 * r0);
                int lx1 = x+Math.round(x1 * r1);
                int ly1 = y+Math.round(y1 * r1);
                
                g.drawLine(lx0, ly0, lx1, ly1);
                
                int dx0 = lx0-wx0;
                int dy0 = ly0-wy0;
                int dx1 = lx1-wx1;
                int dy1 = ly1-wy1;
                
                float dot0 = dx0*dx0+dy0*dy0;
                float dot1 = dx1*dx1+dy1*dy1;
                
                if (dot0 < bestDot0) {
                    bestDot0 = dot0;
                    bx0 = lx0;
                    by0 = ly0;
                }
                
                if (dot1 < bestDot1) {
                    bestDot1 = dot1;
                    bx1 = lx0;
                    by1 = ly0;
                }
                    
                x0 = x1;
                y0 = y1;
                r0 = r1;
            }

            this.drawElectricWall_r(g, bx0, by0, wx0, wy0, 2);
            this.drawElectricWall_r(g, bx1, by1, wx1, wy1, 2);
            
        }
        
        
    }

    // this version draw bezier curves around the player, looks ok, could be better
    private void drawPlayerBarrierElectricWall1(Player player, AGraphics g, int px, int py) {
        // we are touching an electric wall, so we become 1 unit.
        // the end points of the wall are in the array:
        float wx0 = player.barrier_electric_wall[0];
        float wy0 = player.barrier_electric_wall[1];
        float wx3 = player.barrier_electric_wall[2];
        float wy3 = player.barrier_electric_wall[3];

        // compute deltas betwen p and endpoints
        float dx0 = px-wx0;
        float dy0 = py-wy0;
        float dx1 = px-wx3;
        float dy1 = py-wy3;
        
        // get perp to delta
        float nx0 = -dy0;
        float ny0 = dx0;
        float nx1 = -dy1;
        float ny1 = dx1;
        
        // get length of normals
        float d0 = (float)Math.sqrt(nx0*nx0+ny0*ny0);
        float d1 = (float)Math.sqrt(nx1*nx1+ny1*ny1);
        
        if (d0 > 0.01f && d1 > 0.01f) {
            float d0_inv = 1.0f/d0;
            float d1_inv = 1.0f/d1;
        
            float radius = PLAYER_RADIUS_BARRIER;
            nx0 = nx0 * d0_inv * radius; 
            ny0 = ny0 * d0_inv * radius;
            nx1 = nx1 * d1_inv * radius;
            ny1 = ny1 * d1_inv * radius;
        
            float wx1 = wx0+dx0+nx0;
            float wy1 = wy0+dy0+ny0;
            float wx2 = wx3+dx1-nx1;
            float wy2 = wy3+dy1-ny1;
            
            Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,wx0,wy0,wx1,wy1,wx2,wy2,wx3,wy3);
            drawBezierField(g);
            drawBezierField(g);
            drawBezierField(g);
            wx1 = wx0+dx0-nx0;
            wy1 = wy0+dy0-ny0;
            wx2 = wx3+dx1+nx1;
            wy2 = wy3+dy1+ny1;
            Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,wx0,wy0,wx1,wy1,wx2,wy2,wx3,wy3);
            drawBezierField(g);
            drawBezierField(g);
            drawBezierField(g);
        }

    }
    
    final int playerBarrierElectricWallVersion = 3;
    
    // -----------------------------------------------------------------------------------------------
    private void drawPlayerBarrier(Player player, AGraphics g, int px, int py) {
        if (isBarrierActive(player)) {
            
            g.setColor(g.YELLOW);           
            if (player.barrier_electric_wall[0] >= 0) {
                switch (playerBarrierElectricWallVersion) {
                    case 1:
                        drawPlayerBarrierElectricWall1(player, g, px, py);
                        break;
                    case 2:
                        drawPlayerBarrierElectricWall2(player, g, px, py);
                        break;
                    case 3:
                        drawPlayerBarrierElectricWall3(player, g, px, py);
                        break;
                }
                
            } else {
                // draw 3 times for effect
                drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER);
                drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER - 1);
                drawStaticField(g, px, py, PLAYER_RADIUS_BARRIER + 1);
            }
        }
    }
    
    private void drawBezierField(AGraphics g) {
        int x0,y0,x1,y1;
        x0 = bezier_pts_x[0];
        y0 = bezier_pts_y[0];
        for (int i=1; i <bezier_pts_x.length; i++) {
            if (i < bezier_pts_x.length/2) {
                x1 = bezier_pts_x[i]+Utils.randRange(-i,i);
                y1 = bezier_pts_y[i]+Utils.randRange(-i,i);
            } else {
                int ii = bezier_pts_x.length-i;
                x1 = bezier_pts_x[i]+Utils.randRange(-ii,ii);
                y1 = bezier_pts_y[i]+Utils.randRange(-ii,ii);
            }       
            g.drawLine(x0,y0,x1,y1);
            x0 = x1;
            y0 = y1;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the goal (End)
    private void drawEnd(AGraphics g, int x, int y) {
        
        // draw an X
        g.setColor(g.CYAN);
        
        int minRadius = 10;
        int maxRadius = 22;
        int numRings = 3;
        int ringSpacing = 4;
        
        int animSpeed = 5; // higher is slower
        for (int i=0; i<numRings; i++) {
            int f = (getFrameNumber()/animSpeed)+(i*ringSpacing);
            int r = maxRadius - (f%(maxRadius-minRadius)+minRadius);
            g.drawOval(x-r,y-r,r*2,r*2);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getDirection(int dx, int dy) {
        // return 0=N, E=1, S=2, W=3
        int dir = -1;
        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx < 0)
                dir = DIR_LEFT;
            else
                dir = DIR_RIGHT;
        } else {
            if (dy < 0)
                dir = DIR_UP;
            else
                dir = DIR_DOWN;
        }
        return dir;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawElectricWall_r(AGraphics g, int x0, int y0, int x1, int y1, int num) {
        if (num>0) {
            // find midpoint
            int mx = (x0+x1)/2 + Utils.randRange(-num,num);
            int my = (y0+y1)/2 + Utils.randRange(-num,num);
            drawElectricWall_r(g, x0,y0,mx,my,num-1);
            drawElectricWall_r(g, mx,my,x1,y1,num-1);
        } else {
            g.drawLine(x0,y0,x1,y1);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private int getBreakingWallOffset(int p, int num) {
        int min = num*3+10;
        int max = num*4+20;
        return ((p*113)%(max-min))*(p%2 == 0 ? 1 : -1);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawBreakingWall_r(AGraphics g, int x0, int y0, int x1, int y1, int num, int f) {
        if (num>0) {
            int xoff = getBreakingWallOffset(f, num);
            int yoff = getBreakingWallOffset(f, num);
            // find midpoint
            int mx = (x0+x1)/2 + xoff;
            int my = (y0+y1)/2 + yoff;
            drawBreakingWall_r(g, x0,y0,mx,my,num-1,f);
            drawBreakingWall_r(g, mx,my,x1,y1,num-1,f);
        } else {
            g.drawLine(x0, y0, x1, y1, 3);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean collisionBreakingWallLine_r(int lx0, int ly0, int lx1, int ly1, int wx0, int wy0, int wx1, int wy1, int num, int f, int [] newDelta) {
        if (num > 0) {
            int xoff = getBreakingWallOffset(f, num);
            int yoff = getBreakingWallOffset(f, num);
            int mx = (wx0+wx1)/2 + xoff;
            int my = (wy0+wy1)/2 + yoff;
            if (collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, wx0, wy0, mx, my, num-1, f, newDelta))
                return true;
            if (collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, mx, my, wx1, wy1, num-1, f, newDelta))
                return true;
            
        } else {
            if (Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy1, wx1, wy1)) {
                newDelta[0] = wx1 - wx0;
                newDelta[1] = wy1 - wy0;
            }
        }
        
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawLineFade_r(AGraphics g, int x0, int y0, int x1, int y1, AColor outer, AColor inner, int num, float factor) {
        int mx = (x0+x1)/2;
        int my = (y0+y1)/2;
        if (num > 0) {
            AColor cm = outer.interpolateTo(g, inner, factor);//Utils.interpolate(outer, inner, factor);
            drawLineFade_r(g, x0, y0, mx, my, outer, cm, num-1, factor);
            drawLineFade_r(g, mx, my, x1, y1, cm, inner, num-1, factor);
        } else {
            g.setColor(outer);
            g.drawLine(x0,y0,x1,y1,3);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawPortalWall(AGraphics g, int x0, int y0, int x1, int y1) {
        int mx=(x0+x1)/2;
        int my=(y0+y1)/2;
        drawLineFade_r(g, x0, y0, mx, my, g.LIGHT_GRAY, this.throbbing_white, 3, 0.5f);
        drawLineFade_r(g, mx, my, x1, y1, this.throbbing_white, g.LIGHT_GRAY, 3, 0.5f);
    }
    
    // -----------------------------------------------------------------------------------------------
    /*
    private void drawRubberWall(AGraphics g, int x0, int y0, int x1, int y1) {
        float x2=0, y2=0;
        float vx = (x1-x0)*0.5f;
        float vy = (y1-y0)*0.5f;
        float nx = 0, ny = 0;
        g.setColor(AColor.BLUE);
        
        float factor1 = 0.1f;
        float factor2 = 0.15f;
        
        int iterations = 9;
        int thickness = 2;
        
        switch (getFrameNumber()%8) {
        case 0:
        case 4:
            g.drawLine(x0, y0, x1, y1, thickness); 
            return;
        case 1:
        case 3:
            nx = -vy*factor1;
            ny =  vx*factor1;
            break;
        case 2:
            nx = -vy*factor2;
            ny =  vx*factor2;
            break;          
        case 7:
        case 5:
            nx =  vy*factor1;
            ny = -vx*factor1;
            break;
        case 6:
            nx =  vy*factor2;
            ny = -vx*factor2;
            break;
        }
        
        x2 = x0 + vx + nx;
        y2 = y0 + vy + ny;
        
        Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,x0,y0,x2,y2,x2,y2,x1,y1);
        
        g.drawLineStrip(bezier_pts_x, bezier_pts_y, thickness);
        if (Utils.DEBUG_ENABLED) {
            int x = Math.round(x2);
            int y = Math.round(y2);
            g.drawRect(x,y,1,1);
        }
    }*/

    // -----------------------------------------------------------------------------------------------
    private void drawRubberWall(AGraphics g, int x0, int y0, int x1, int y1, float frequency) {
        float x2=0, y2=0;
        float vx = (x1-x0)*0.5f;
        float vy = (y1-y0)*0.5f;
        float nx = 0, ny = 0;
        g.setColor(g.BLUE);
        final int thickness = 2;
        
        if (frequency < EPSILON) {
            g.drawLine(x0, y0, x1, y1, thickness); 
        } else {
        
            float factor = CMath.sine(getFrameNumber()*50) * frequency;
            
            nx = -vy*factor;
            ny =  vx*factor;
            
            x2 = x0 + vx + nx;
            y2 = y0 + vy + ny;
            
            Utils.computeBezierCurvePoints(bezier_pts_x, bezier_pts_y,x0,y0,x2,y2,x2,y2,x1,y1);
            
            g.drawLineStrip(bezier_pts_x, bezier_pts_y, thickness);
            if (Utils.DEBUG_ENABLED) {
                int x = Math.round(x2);
                int y = Math.round(y2);
                g.drawRect(x,y,1,1);
            }
        }
    }

    private int [] bezier_pts_x = new int[10];
    private int [] bezier_pts_y = new int[10];
    
    // -----------------------------------------------------------------------------------------------
    private void drawDoor(AGraphics g, Wall door, int x0, int y0, int x1, int y1) {
        g.setColor(DOOR_COLOR);
        final int framesElapsed = (getFrameNumber() - door.frame) + 1;
        
        int dx = (x1-x0);
        int dy = (y1-y0);
        
        int mx = (x1+x0)/2;
        int my = (y1+y0)/2;
        
        switch (door.state) {
        case DOOR_STATE_CLOSED:
            g.drawLine(x0, y0, x1, y1, this.DOOR_THICKNESS);
            return;
        case DOOR_STATE_LOCKED:
            g.drawLine(x0, y0, x1, y1, this.DOOR_THICKNESS);
            g.setColor(g.RED);
            g.drawDisk(mx, my, 10);
            return;
        case DOOR_STATE_OPEN:
            dx /= 4;
            dy /= 4;
            g.drawLine(x0, y0, x0+dx, y0+dy, DOOR_THICKNESS);
            g.drawLine(x1, y1, x1-dx, y1-dy, DOOR_THICKNESS);
            if (framesElapsed >= this.DOOR_OPEN_FRAMES) {
                door.state = DOOR_STATE_CLOSING;
                door.frame = getFrameNumber();
            }
            return;
        }
        
        int [] delta = getDoorDelta(dx, dy, framesElapsed, door.state);
        
        dx = delta[0];
        dy = delta[1];
        
        g.drawLine(x0, y0, x0+dx, y0+dy, DOOR_THICKNESS);
        g.drawLine(x1, y1, x1-dx, y1-dy, DOOR_THICKNESS);
        
        if (framesElapsed >= this.DOOR_SPEED_FRAMES) {
            if (door.state == DOOR_STATE_OPENING)
                door.state = DOOR_STATE_OPEN;
            else
                door.state = DOOR_STATE_CLOSED;
            door.frame = getFrameNumber();
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private int [] getDoorDelta(int dx, int dy, int framesElapsed, int doorState) {
        float len = Utils.fastLen(dx, dy);
        if (len < 1)
            len = 1;
        
        final float len_inv = 1.0f/len;
        float l = (DOOR_SPEED_FRAMES_INV*framesElapsed)*(len*0.25f); 
        
        if (doorState == DOOR_STATE_OPENING)
            l = (len/4) - l;
        
        int [] delta = new int[2];
        
        delta[0] = Math.round((0.25f*dx) + len_inv*dx*l); 
        delta[1] = Math.round((0.25f*dy) + len_inv*dy*l);
        
        return delta;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawDebugWallInfo(AGraphics g) {
        g.setColor(g.GREEN);
        for (int i = 1; i < MAZE_NUM_VERTS; i++) {
            for (int j = 0; j < i; j++) {
                
                int x0 = maze_verts_x[i];
                int y0 = maze_verts_y[i];
                int x1 = maze_verts_x[j];
                int y1 = maze_verts_y[j];
                
                if (!isOnScreen(x0, y0) && !isOnScreen(x1, y1))
                    continue;
                
                x0 -= screen_x;
                y0 -= screen_y;
                x1 -= screen_x;
                y1 -= screen_y;
                
                int mx = (x0+x1)/2;
                int my = (y0+y1)/2;
                
                Wall info = wall_lookup[i][j];
                if (info == null)
                    continue;
                
                if (info.type == WALL_TYPE_NONE) {
                    //g.drawLine(x0, y0, x1, y1);
                } else {                
                    String str = info.toString();
                    //g.drawString(str, mx, my);
                    g.drawString(str, mx, my);
                }
            }
        }
    }
    
    private void drawWall(AGraphics g, Wall info, int x0, int y0, int x1, int y1) {
        switch (info.type) {
        case WALL_TYPE_NORMAL:  
            // translate and draw the line
            if (info.health < WALL_NORMAL_HEALTH) {
                // give a number between 0-1 that is how much health we have
                float health = (1.0f/WALL_NORMAL_HEALTH)*info.health;
                int c = Math.round(255.0f * health);
                g.setColor(g.makeColor(255, c, c));
                // if wall is healthy, num==0
                // if wall is med, num 1
                // if wall is about to break, num = 2
                int num = Math.round((1.0f - health) * WALL_BREAKING_MAX_RECURSION);                                
                // make these values consistent based on garbage input
                //int xoff = (info.v0 * (-1 * info.v1 % 2) % 30);
                //int yoff = (info.v1 * (-1 * info.v0 % 2) % 30);
                drawBreakingWall_r(g, x0, y0, x1, y1, num, info.v0 + info.v1);
            } else {
                g.setColor(g.LIGHT_GRAY);
                g.drawLine(x0, y0, x1, y1, MAZE_WALL_THICKNESS);
            }
            break;
        case WALL_TYPE_ELECTRIC:
            g.setColor(g.YELLOW);
            boolean done = false;
            for (int i=0; i<num_players; i++) {
                Player player = players[i];
                if (isBarrierActive(player) && Utils.isCircleIntersectingLineSeg(x0,y0,x1,y1,player.x-screen_x, player.y-screen_y, PLAYER_RADIUS_BARRIER)) {
                    // draw the electric wall at the perimeter of the barrier, so the electric
                    // wall and the barrier become 1 unit.  TODO: make ememies die from electric walls.
                    // the actual rendering is done by drawPlayerBarrier
                    player.barrier_electric_wall[0] = x0;
                    player.barrier_electric_wall[1] = y0;
                    player.barrier_electric_wall[2] = x1;
                    player.barrier_electric_wall[3] = y1;
                    done = true;
                    break;
                }
                
            } 
            
            if (!done) {// draw twice for effect
                drawElectricWall_r(g, x0, y0, x1, y1,3);
                drawElectricWall_r(g, x0, y0, x1, y1,3);
            }
            break;
        case WALL_TYPE_INDESTRUCTABLE:
            g.setColor(g.DARK_GRAY);
            g.drawLine(x0, y0, x1, y1, MAZE_WALL_THICKNESS+2);
            break;
        case WALL_TYPE_PORTAL:  
            drawPortalWall(g, x0, y0, x1, y1);                  
            break;
        case WALL_TYPE_RUBBER:  
            drawRubberWall(g, x0, y0, x1, y1, info.frequency);
            break;
        case WALL_TYPE_DOOR:
            drawDoor(g, info, x0, y0, x1, y1);
            break;
        case WALL_TYPE_PHASE_DOOR:
            g.setColor(g.GREEN);
            g.drawLine(x0, y0, x1, y1, 1);
            break;              
        default: 
            Utils.unhandledCase(info.type);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Render the maze from screen_x/y
    private void drawMaze(AGraphics g) {
        int x0, x1, y0, y1;
        
        for (int i = 1; i < MAZE_NUM_VERTS; i++) {
            for (int j = 0; j < i; j++) {
                Wall info = wall_lookup[i][j];

                if (!isWallActive(info))
                    continue;
                
                x0 = maze_verts_x[info.v0];
                y0 = maze_verts_y[info.v0];
                x1 = maze_verts_x[info.v1];
                y1 = maze_verts_y[info.v1];
                
                if (!isOnScreen(x0, y0) && !isOnScreen(x1, y1))
                    continue;

                if (this.isVisibilityEnabled()) {
                    Player player = getPlayer();
                    if (info.visible == false) {
                        // see if player can 'see' the wall
                        int mx = (x0+x1)/2;
                        int my = (y0+y1)/2;
                        if (canSee(player.x, player.y, mx, my)) {
                            info.visible = true;                        
                        } else {
                            continue;
                        }
                    }
                }
                
                x0 -= screen_x;
                y0 -= screen_y;
                x1 -= screen_x;
                y1 -= screen_y;
                
                drawWall(g, info, x0, y0, x1, y1);
                
                if (info.frequency > 0) {
                    info.frequency -= RUBBER_WALL_FREQENCY_COOLDOWN;
                }
                
                // if (debug_draw_edge_arrows) {
                if (isDebugEnabled(Debug.DRAW_MAZE_INFO)) {
                    // draw a little arrow head to show the direction of the
                    // edge
                    vec[0] = (float) (x1 - x0);
                    vec[1] = (float) (y1 - y0);
                    float mag = (float) Math.sqrt(vec[0] * vec[0] + vec[1] * vec[1]);
                    if (mag > EPSILON) {
                        vec[0] *= 10 / mag;
                        vec[1] *= 10 / mag;
                        
                        CMath.rotateVector(vec, 150);
                        g.drawLine(x1, y1, x1 + Math.round(vec[0]), y1 + Math.round(vec[1]), MAZE_WALL_THICKNESS);
                        
                        CMath.rotateVector(vec, 60);
                        int x2 = x1 + Math.round(vec[0]);
                        int y2 = y1 + Math.round(vec[1]);
                        g.drawLine(x1, y1, x2, y2, MAZE_WALL_THICKNESS);
                    }
                }
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // layout an even grid of vertices, with some noise
    private void buildVertices(int max_x, int max_y, int noise) {
        int x = 0, y = 0, index;
        verts_min_x = 0;
        verts_min_y = 0;
        verts_max_x = 0;
        verts_max_y = 0;
        
        for (int i = 0; i < MAZE_NUMCELLS_Y + 1; i++) {
            for (int j = 0; j < MAZE_NUMCELLS_X + 1; j++) {
                index = i * (MAZE_NUMCELLS_X + 1) + j;
                maze_verts_x[index] = x + Utils.randRange(-noise, noise);
                maze_verts_y[index] = y + Utils.randRange(-noise, noise);
                x += MAZE_CELL_WIDTH;
                // compute the actual world dimension
                if (verts_min_x > maze_verts_x[index])
                    verts_min_x = maze_verts_x[index];
                if (verts_max_x < maze_verts_x[index])
                    verts_max_x = maze_verts_x[index];
                if (verts_min_y > maze_verts_y[index])
                    verts_min_y = maze_verts_y[index];
                if (verts_max_y < maze_verts_y[index])
                    verts_max_y = maze_verts_y[index];
            }
            x = 0;
            y += MAZE_CELL_HEIGHT;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // delete all edges in the maze graph and digraph
    private void clearAllWalls() {
        for (int i = 1; i < MAZE_NUM_VERTS-1; i++) {
            for (int j = 0; j < i; j++) {
                if (wall_lookup[i][j] != null)
                    wall_lookup[i][j].clear();
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // add an edge to the wall_lookup
    private void addWall(int v0, int v1) {
        //println("Add wall between " + v0 + " and " + v1);
        Wall wall = getWall(v0, v1);
        wall.clear();
        wall.v0 = v0;
        wall.v1 = v1;
        wall.type = WALL_TYPE_INDESTRUCTABLE;
    }

    // -----------------------------------------------------------------------------------------------
    private Wall getWall(int v0, int v1) {
        Utils.assertTrue(wall_lookup[v0][v1] == wall_lookup[v1][v0]);
        return wall_lookup[v0][v1];
    }
    
    // -----------------------------------------------------------------------------------------------
    private void initWall(int v0, int v1) {
        Wall wall = getWall(v0, v1);
        if (wall == null) {
            wall = wall_lookup[v0][v1] = wall_lookup[v1][v0] = new Wall();
            addWall(v0, v1);
            wall_lookup[v0][v1].type = WALL_TYPE_NORMAL;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // build a new maze and populate with generators. Quantity based on
    // game_level
    private void buildAndPopulateLevel() {
        enemy_robot_speed = ENEMY_ROBOT_SPEED;
        total_enemies = MAX_ENEMIES;
        people_picked_up = 0;
        addEnemy(0, 0, ENEMY_INDEX_BRAIN, true);
        if (game_type == GAME_TYPE_CLASSIC) {
            buildAndPopulateClassic();
        } else
            buildAndpopulateRobocraze();
    }
    
    // -----------------------------------------------------------------------------------------------
    // rearrange the enemies away from the player
    private void shuffleEnemies() {
        int x, y, d=1000, r;
        int wid = verts_max_x - verts_min_x;
        int hgt = verts_max_y - verts_min_y;
        
        for (int i = 0; i < num_enemies; i++) {
            r = enemy_radius[enemy_type[i]];
            do {
                x = Utils.randRange(r, wid - r);
                y = Utils.randRange(r, hgt - r);
                Player player = getClosestPlayer(x,y);
                if (player != null)
                    d = Utils.fastLen(player.x - x, player.y - y);
            } while (d < 100 - game_level);
            enemy_x[i] = x;
            enemy_y[i] = y;
        }
    }
    
    private void resetPlayer(Player player) {
        player.num_missles = 0;
        player.num_tracers = 0;
        player.x = player.start_x;
        player.y = player.start_y;
        player.powerup = -1;
        player.scale = 1.0f;
        player.last_shot_frame = 0;
        player.stun_dx = player.stun_dy = 0;
        player.hit_index = -1;
        player.hit_type = -1;
        player.next_state_frame = getFrameNumber() + this.PLAYER_SPAWN_FRAMES;
        player.state = PLAYER_STATE_SPAWNING;
    }
    
    private void resetLevel() {
        num_tank_missles = 0;
        num_enemy_missles = 0;
        num_snake_missles = 0;
        num_powerups = 0;
        num_enemies = 0;
        num_zombie_tracers = 0;
        num_particles = 0;
        num_msgs = 0;
        people_points = PEOPLE_START_POINTS;
        enemy_robot_speed = ENEMY_ROBOT_SPEED + difficulty + (game_level / 4);
        
        for (int i=0; i<num_players; i++) {
            resetPlayer(players[i]);
        }
        
        //setFrameNumber(0);
        //game_start_frame = 0;
        game_start_frame = getFrameNumber();        
    }
    
    // -----------------------------------------------------------------------------------------------
    // this func is called after a player dies to remove missles, msgs and so forth
    // also called to initialize the level. this contains the data initialization in
    // one place.
    /*
    private void resetLevel(boolean clearEnemies) {
        // remove any existing enemies and missles
        num_tank_missles = 0;
        for (int i=0; i<num_players; i++) {
            players[i].num_missles = 0;
            players[i].num_tracers = 0;
            players[i].x = players[i].start_x;
            players[i].y = players[i].start_y;
            players[i].powerup = -1;
            players[i].scale = 1.0f;
            players[i].last_shot_frame = 0;
        }
        num_enemy_missles = 0;
        num_snake_missles = 0;
        num_powerups = 0;
        
        if (clearEnemies)
            num_enemies = 0;
        
        num_zombie_tracers = 0;
        
        num_particles = 0;
        num_msgs = 0;
        
        people_points = PEOPLE_START_POINTS;
        
        enemy_robot_speed = ENEMY_ROBOT_SPEED + difficulty + (game_level / 4);
        
        for (int i=0; i<num_players; i++) {
            players[i].hit_index = -1;
            players[i].hit_type = -1;
            players[i].next_state_frame = getFrameNumber() + this.PLAYER_SPAWN_FRAMES;
            players[i].state = PLAYER_STATE_SPAWNING;
        }
        
        //setFrameNumber(0);
        //game_start_frame = 0;
        game_start_frame = getFrameNumber();
    }*/
    
    // -----------------------------------------------------------------------------------------------
    private void buildAndPopulateClassic() {
        int wid = WORLD_WIDTH_CLASSIC;
        int hgt = WORLD_HEIGHT_CLASSIC;
        
        // build the vertices
        buildVertices(wid, hgt, 0);
        clearAllWalls();
        
        // put players at the center
        int sx = wid/2;
        int sy = hgt/2;
        for (int i=0; i<num_players; i++) {
            Player player = players[i];
            player.start_x = player.x = sx;
            player.start_y = player.y = sy;
            sx += PLAYER_RADIUS * 2 + 10;
        }        
        // add all the perimiter edges
        int bottom = (MAZE_NUMCELLS_X + 1) * MAZE_NUMCELLS_Y;
        for (int i = 0; i < MAZE_NUMCELLS_X; i++) {
            addWall(i, i + 1);
            addWall(bottom + i, bottom + i + 1);
        }
        for (int i = 0; i < MAZE_NUMCELLS_Y; i++) {
            addWall(i * (MAZE_NUMCELLS_X + 1), (i + 1) * (MAZE_NUMCELLS_X + 1));
            addWall(i * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X, (i + 1) * (MAZE_NUMCELLS_X + 1) + MAZE_NUMCELLS_X);
        }
        
        // start the timer for the highlighted player
        resetLevel();
        
        // add some robots
        
        int count = MAX_ENEMIES / 2 + (difficulty - 1) * 4 + (game_level / 2);
        for (int i = 0; i < count; i++)
            addEnemy(0, 0, Utils.randRange(ENEMY_INDEX_ROBOT_N, ENEMY_INDEX_ROBOT_W), true);
        
        // Add some Thugs
        
        count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + game_level + Utils.randRange(-5, 4 + game_level);
        for (int i = 0; i < count; i++)
            addEnemy(0, 0, Utils.randRange(ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_W), true);
        
        // Add Some Brains And / Or Tanks
        if (game_level > 1 && game_level % 2 == 0) {
            // add brains
            count = ENEMY_GEN_INITIAL + (difficulty - 1) * 4 + game_level + Utils.randRange(-5, 5 + game_level);
            
            for (int i = 0; i < count; i++)
                addEnemy(0, 0, ENEMY_INDEX_BRAIN, true);
        } else if (game_level > 2 && game_level % 2 == 1) {
            // Add Some tanks
            count = ENEMY_GEN_INITIAL + (difficulty - 1) * 2 + game_level + Utils.randRange(-5, 5 + game_level);
            
            for (int i = 0; i < count; i++)
                addEnemy(0, 0, Utils.randRange(ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_NW), true);
        }
        
        shuffleEnemies();
        addPeople();
        buildRandomWalls();
        
    }
    
    private boolean [][] usedCells;
    
    // -----------------------------------------------------------------------------------------------
    private void addEnemyAtRandomCell(int type, int dx, int dy) {
        int ex = 0;
        int ey = 0;
        
        final int max_tries = 1000;
        
        int tries=0; 
        for ( ; tries<max_tries; tries++) {
            int cx = Utils.rand() % MAZE_NUMCELLS_X;
            int cy = Utils.rand() % MAZE_NUMCELLS_Y;
            
            if (usedCells[cx][cy])
                continue;
            
            ex = cx * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2;
            ey = cy * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2;
            
            boolean ok = true;
            for (int i=0; i<num_players; i++) {
                Player player = players[i];
                if (ex == player.start_x && ey == player.start_y) {
                    ok = false;
                    break;
                }
            }    
            
            if (!ok)
                continue;
            
            if (ex == end_x && ey == end_y)
                continue;
            
            usedCells[cx][cy] = true;
            break;
        }
        
        if (tries < max_tries) {        
            this.addEnemy(ex, ey, type, true);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void addWallCount(int [] count, int v0, int v1) {
        assert(v0>=0 && v0<MAZE_NUM_VERTS);
        assert(v1>=0 && v0<MAZE_NUM_VERTS);
        assert(count.length == MAZE_NUM_VERTS);
        assert(v0 != v1);
        count[v0]++;
        assert(count[v0] <= 8);
        count[v1]++;
        assert(count[v1] <= 8);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void computeWallEnding(int [] count, int v0, int v1) {
        
        Wall wall = getWall(v0, v1);
        if (wall != null && wall.type != WALL_TYPE_NONE) {
            if (count[v0] == 2 || count[v1] == 2)
                wall.ending = true;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void buildAndpopulateRobocraze() {
        
        // build the vertices
        buildVertices(MAZE_WIDTH, MAZE_HEIGHT, MAZE_VERTEX_NOISE);
        
        // create a temp matrix of cells.  This will hold the maze information.
        // each cell is a 4bit value with north, south, east west bits.
        //int[][] maze_cells = new int[MAZE_NUMCELLS_X][];
        
        int i, j;
        
        // init all maze cells such that all the walls are set
        for (i = 0; i < MAZE_NUMCELLS_X; i++) {
            maze_cells[i] = new int[MAZE_NUMCELLS_Y];
            for (j = 0; j < MAZE_NUMCELLS_Y; j++)
                maze_cells[i][j] = WALL_NORTH | WALL_SOUTH | WALL_EAST | WALL_WEST;
        }
        
        // pick a start and end point
        int cell_start_x = Utils.randRange(0, MAZE_NUMCELLS_X - 1);
        int cell_start_y = Utils.randRange(0, MAZE_NUMCELLS_Y - 1);
        int cell_end_x = Utils.randRange(0, MAZE_NUMCELLS_X - 1);
        int cell_end_y = Utils.randRange(0, MAZE_NUMCELLS_Y - 1);
        
        // continue searching until we are not on top of each other
        while (Math.abs(cell_end_x - cell_start_x) < (MAZE_NUMCELLS_X / 2) || 
                Math.abs(cell_end_y - cell_start_y) < (MAZE_NUMCELLS_Y / 2)) {
            cell_end_x = Utils.randRange(0, MAZE_NUMCELLS_X - 1);
            cell_end_y = Utils.randRange(0, MAZE_NUMCELLS_Y - 1);
        }
        
        // Start the recursive maze generation algorithm.  this method knocks
        // down walls until all cells have been touched.
        mazeSearch_r(maze_cells, cell_start_x, cell_start_y, cell_end_x, cell_end_y);
        
        // now that we have a maze organized as a group of cells, convert to
        // graph
        clearAllWalls();
        
        int [] vertex_wall_count = new int[MAZE_NUM_VERTS];
        Arrays.fill(vertex_wall_count, 0);
        
        for (i = 0; i < MAZE_NUMCELLS_X; i++) {
            for (j = 0; j < MAZE_NUMCELLS_Y; j++) {
                // compute the vertex indices associated with this cell
                int upleft = i + j * (MAZE_NUMCELLS_X + 1);
                int upright = i + 1 + j * (MAZE_NUMCELLS_X + 1);
                int downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1);
                int downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1);
                
                //.println("cells[i][j]=" + cells[i][j] + ", i=" + i + ", j=" + j + ", upleft=" + upleft + ", upright=" + upright + ", downleft=" + downleft + ", downright=" + downright);
                //.println("wall count=" + Arrays.toString(vertex_wall_count));
                
                if ((maze_cells[i][j] & WALL_NORTH) != 0) {
                    addWall(upleft, upright);
                    addWallCount(vertex_wall_count, upleft, upright);
                }
                
                if ((maze_cells[i][j] & WALL_EAST) != 0) {
                    addWall(upright, downright);
                    addWallCount(vertex_wall_count, upright, downright);
                }
                
                if ((maze_cells[i][j] & WALL_SOUTH) != 0) {
                    addWall(downleft, downright);
                    addWallCount(vertex_wall_count, downleft, downright);
                }
                
                if ((maze_cells[i][j] & WALL_WEST) != 0) {
                    addWall(upleft, downleft);
                    addWallCount(vertex_wall_count,downleft,upleft);
                }
            }
        }
        
        //if (Utils.DEBUG_ENABLED)
        //  Utils.println("vertex wall count : " + Utils.toString(vertex_wall_count));
        
        // now visit all the wall again and set the 'ending' flag for those walls
        // with a vertex that has a wall_count == 1.
        for (i = 0; i < MAZE_NUMCELLS_X; i++) {
            for (j = 0; j < MAZE_NUMCELLS_Y; j++) {
                // compute the vertex indices associated with this cell
                int upleft = i + j * (MAZE_NUMCELLS_X + 1);
                int upright = i + 1 + j * (MAZE_NUMCELLS_X + 1);
                int downleft = i + (j + 1) * (MAZE_NUMCELLS_X + 1);
                int downright = i + 1 + (j + 1) * (MAZE_NUMCELLS_X + 1);

                computeWallEnding(vertex_wall_count, upleft, upright);
                computeWallEnding(vertex_wall_count, upright, downright);
                computeWallEnding(vertex_wall_count, downleft, downright);
                computeWallEnding(vertex_wall_count, downleft,upleft);
            }
        }
        
        usedCells = new boolean[MAZE_NUMCELLS_X][MAZE_NUMCELLS_Y];

        usedCells[cell_start_x][cell_start_y] = true;
        usedCells[cell_end_x][cell_end_y] = true;
        
        // position the player at the center starting cell
        int sx = cell_start_x * MAZE_CELL_WIDTH + MAZE_CELL_WIDTH / 2;
        int sy = cell_start_y * MAZE_CELL_HEIGHT + MAZE_CELL_HEIGHT / 2;
        
        for (int ii=0; ii<num_players; ii++) {
            Player player = players[ii];
            player.x = sx;
            player.y = sy;
            switch (ii%4) {
                case 0:
                    sy += PLAYER_RADIUS * 2 + 10;
                    break;
                case 1:
                    sx += PLAYER_RADIUS * 2 + 10;
                    break;
                case 2:
                    sy -= PLAYER_RADIUS * 2 + 10;
                    break;
                case 3:
                    sx -= PLAYER_RADIUS * 2 + 10;
                    break;
            }
            // remember the starting point
            player.start_x = player.x;
            player.start_y = player.y;
        }        
        // compute maze coord position of ending cell
        end_x = cell_end_x * MAZE_CELL_WIDTH + (MAZE_CELL_WIDTH / 2);
        end_y = cell_end_y * MAZE_CELL_HEIGHT + (MAZE_CELL_HEIGHT / 2);
        
        resetLevel();
        
        // Now add a few generators and thugs
        int gen_x, gen_y;
        int num_gens = ENEMY_GEN_INITIAL + game_level + difficulty;
        
        // make sure we dont try to add more gens then there are cells
        if (num_enemies > (MAZE_NUMCELLS_X * MAZE_NUMCELLS_Y - 2))
            num_enemies = MAZE_NUMCELLS_X * MAZE_NUMCELLS_Y - 2;
        
        num_enemies = 0;
    
        // make sure only 1 generator per cell
        for (i = 0; i < num_gens; i++) {
            this.addEnemyAtRandomCell(ENEMY_INDEX_GEN, Utils.randRange(-10,10), Utils.randRange(-10, 10));
        }
        
        // add some jaws
        for (i=0; i<3+game_level; i++) {
            this.addEnemyAtRandomCell(ENEMY_INDEX_JAWS, Utils.randRange(-20,20), Utils.randRange(-20, 20));
        }
        
        // add some lava
        for (i=0; i<4+game_level+this.difficulty; i++) {
            this.addEnemyAtRandomCell(ENEMY_INDEX_LAVA, Utils.randRange(-10,10), Utils.randRange(-10, 10));
        }
        
        // now place some thugs by the generators
        for (i = 0; i < num_gens; i++) {
            gen_x = enemy_x[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_THUG_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_THUG_RADIUS);
            gen_y = enemy_y[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_THUG_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_THUG_RADIUS);
            addEnemy(gen_x, gen_y, Utils.randRange(ENEMY_INDEX_THUG_N, ENEMY_INDEX_THUG_W), true);
        }
        
        // now place some brains or tanks
        if (game_level > 1) {
            if (game_level % 2 == 0) {
                for (i = 0; i < num_gens; i++) {
                    gen_x = enemy_x[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_BRAIN_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_BRAIN_RADIUS);
                    gen_y = enemy_y[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_BRAIN_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_BRAIN_RADIUS);
                    addEnemy(gen_x, gen_y, ENEMY_INDEX_BRAIN, true);
                }
            } else {
                for (i = 0; i < num_gens; i++) {
                    gen_x = enemy_x[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_TANK_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_TANK_RADIUS);
                    gen_y = enemy_y[i] + Utils.randRange(-ENEMY_SPAWN_SCATTER + ENEMY_TANK_RADIUS, ENEMY_SPAWN_SCATTER - ENEMY_TANK_RADIUS);
                    addEnemy(gen_x, gen_y, Utils.randRange(ENEMY_INDEX_TANK_NE, ENEMY_INDEX_TANK_NW), true);
                }
            }
        }
        
        
        addPeople();
        buildRandomWalls();
    }
    
    // -----------------------------------------------------------------------------------------------
    private void buildRandomWalls() {
        // portal tracking
        int pv0 = -1;
        int pv1 = -1;
        
        int [] weights = getWallChanceForLevel();
        
        for (int v0=1; v0<MAZE_NUM_VERTS; v0++) {
            for (int v1=0; v1<v0; v1++) {
                Wall wall = wall_lookup[v0][v1];
                
                if (wall == null)
                    continue;
                
                final boolean perim = isPerimiterVertex(v0) && isPerimiterVertex(v1);
                
                if (perim) {
                    wall.type = WALL_TYPE_INDESTRUCTABLE;
                    continue;
                }
                
                if (this.game_type == this.GAME_TYPE_CLASSIC) {
                    continue;
                }
                
                if (wall.type == WALL_TYPE_NONE) {
                    if (Utils.rand() % 100 < game_level) {
                        wall.initDoor(DOOR_STATE_CLOSED, v0, v1);
                    }
                    continue;
                }
                
                // if this is an ending wall, then skip
                // an ending wall has 1 vertex with only 1 wall on it (itself)
                
                if (wall.ending)
                    continue;
                
                wall.type = Utils.chooseRandomFromSet(weights);
                switch (wall.type) {
                case WALL_TYPE_NORMAL:
                    wall.health = this.WALL_NORMAL_HEALTH; 
                    break;
                case WALL_TYPE_DOOR:
                    wall.state = DOOR_STATE_LOCKED;
                    break;
                case WALL_TYPE_PORTAL:
                    if (pv0 < 0) {
                        pv0 = v0;
                        pv1 = v1;
                    } else {
                        wall.p0 = pv0;
                        wall.p1 = pv1;
                        Wall other = wall_lookup[pv0][pv1];
                        other.p0 = v0;
                        other.p1 = v1;
                        pv0 = pv1 = -1;
                    }
                    break;
                }
            }   
        }
        if (pv0>=0) {
            // undo the portal
            wall_lookup[pv0][pv1].type = WALL_TYPE_INDESTRUCTABLE;
        }
    }
    
    // this version picks cells at random and makes the mazes much more difficult
    private void mazeSearch_r2(int [][] cells, int x, int y, int end_x, int end_y) {
        ArrayList<int[]> list = new ArrayList<int[]>(256);
        list.add(new int [] { x,y });
        while (!list.isEmpty()) {
            int index = Utils.rand() % list.size();
            int [] xy = list.remove(index);

            x = xy[0];
            y = xy[1];
            
            // get an array of directions in descending order of priority
            int[] dir_list = directionHeuristic(x, y, end_x, end_y);

            for (int i = 0; i < 4; i++) {
                int nx = x + move_dx[dir_list[i]];
                int ny = y + move_dy[dir_list[i]];
                
                if (nx < 0 || ny < 0 || nx >= MAZE_NUMCELLS_X || ny >= MAZE_NUMCELLS_Y)
                    continue;
                
                // Ignore cells already touched
                if (cells[nx][ny] != 15)
                    continue;
                
                // break wall
                cells[x][y] &= ~(1 << dir_list[i]);
                
                int dir2 = (dir_list[i] + 2) % 4;
                
                cells[nx][ny] &= ~(1 << dir2);
                //mazeSearch_r(cells, nx, ny, end_x, end_y);
                list.add(new int [] { nx, ny});
            }
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // Recursive DFS search routine to create a maze
    // this version makes mazes easy to solve
    private void mazeSearch_r(int[][] cells, int x, int y, int end_x, int end_y) {
        // this check isnt really neccessary
        if (x < 0 || y < 0 || x >= MAZE_NUMCELLS_X || y >= MAZE_NUMCELLS_Y)
            return;
        
        // .println("Searching " + x + " " + y + " " + direction);
        
        // get an array of directions in descending order of priority
        int[] dir_list = directionHeuristic(x, y, end_x, end_y);
        
        for (int i = 0; i < 4; i++) {
            int nx = x + move_dx[dir_list[i]];
            int ny = y + move_dy[dir_list[i]];
            
            if (nx < 0 || ny < 0 || nx >= MAZE_NUMCELLS_X || ny >= MAZE_NUMCELLS_Y)
                continue;
            
            // Ignore cells already touched
            if (cells[nx][ny] != 15)
                continue;
            
            // break wall
            cells[x][y] &= ~(1 << dir_list[i]);
            
            int dir2 = (dir_list[i] + 2) % 4;
            
            cells[nx][ny] &= ~(1 << dir2);
            
            // DFS Recursive search
            if (game_level - difficulty > 10)
                mazeSearch_r2(cells, nx, ny, end_x, end_y);
            else
                mazeSearch_r(cells, nx, ny, end_x, end_y);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    // remove the edge from both graphs
    private void removeEdge(int v0, int v1) {
        wall_lookup[v0][v1].type = WALL_TYPE_NONE;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Return true if this vertex runs along the perimiter of the maze
    private boolean isPerimiterVertex(int vertex) {
        int x = vertex % (MAZE_NUMCELLS_X + 1);
        int y = vertex / (MAZE_NUMCELLS_X + 1);
        if (x == 0 || x == MAZE_NUMCELLS_X || y == 0 || y == MAZE_NUMCELLS_Y)
            return true;
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    // return 4 dim array of ints in range 0-3, each elem occurs only once
    private int[] directionHeuristic(int x1, int y1, int x2, int y2) {
        // resulting list
        int[] d = new int[4];
        
        // Use the transform array (normally used to render) to keep our weights
        // (trying to save mem)
        for (int i = 0; i < 4; i++) {
            d[i] = i;
            transform[i] = 0.5f;
        }
        
        if (y1 < y2) // tend to move north
            transform[0] += Utils.randFloat(0.4f) - 0.1f;
        else if (y1 > y2) // tend to move south
            transform[2] += Utils.randFloat(0.4f) - 0.1f;
        
        if (x1 < x2) // tend to move west
            transform[3] += Utils.randFloat(0.4f) - 0.1f;
        else if (x1 > x2) // tend to move east
            transform[1] += Utils.randFloat(0.4f) - 0.1f;
        
        // Now bubble sort the list (descending) using our weights to determine order.
        // Elems that have the same weight will be determined by a coin flip
        
        // temporaries
        float t_f;
        int t_i;
        
        // bubblesort
        for (int i = 0; i < 3; i++) {
            for (int j = i; j < 4; j++) {
                if (transform[i] < transform[j] || (transform[i] == transform[j] && Utils.flipCoin())) {
                    // swap elems in BOTH arrays
                    t_f = transform[i];
                    transform[i] = transform[j];
                    transform[j] = t_f;
                    
                    t_i = d[i];
                    d[i] = d[j];
                    d[j] = t_i;
                }
            }
        }
        
        return d;
    }
    
    // -----------------------------------------------------------------------------------------------
    // return true when x,y is within screen bounds
    private boolean isOnScreen(int x, int y) {
        if (game_type == GAME_TYPE_CLASSIC)
            return true;
        if (x < screen_x || y < screen_y || x > screen_x + screen_width || y > screen_y + screen_height)
            return false;
        return true;
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updatePlayerVisibility_r(int [] verts, int d) {
        for (int i=0; i<4; i++) {
            verts[i] += cell_dv[d];
            if (verts[i] < 0 || verts[i] >= MAZE_NUM_VERTS)
                return;
        }
        
        for (int i=0; i<4; i++) {
            int ii = (i+1)%4;
            Wall info = wall_lookup[verts[i]][verts[ii]];
            Utils.assertTrue(info != null);
            info.visible = true;
        }
        
        int dd = (d+1)%4;
        Wall info = wall_lookup[verts[d]][verts[dd]];
        if (canSeeThroughWall(info))
            updatePlayerVisibility_r(verts, d);
    }
    
    // -----------------------------------------------------------------------------------------------
    private void updatePlayerVisibility(Player player) {
        int [] verts = new int[5];
        this.computePrimaryQuadrant(player.x, player.y, verts);
        if (Arrays.equals(player.primary_verts, verts))
            return; // player verts hasnet changed, no update needed
        
        //Utils.copyElems(player.primary_verts, verts);
        System.arraycopy(verts, 0, player.primary_verts, 0, player.primary_verts.length);
        
        for (int i=0; i<4; i++) {
            int ii = (i+1) % 4;
            
            Wall info = wall_lookup[verts[i]][verts[ii]];
            Utils.assertTrue(info != null);
            info.visible = true;
            
            if (canSeeThroughWall(info)) {
                // recursize search in this direction
                updatePlayerVisibility_r(verts, i);
                //Utils.copyElems(verts, player.primary_verts);
                System.arraycopy(player.primary_verts, 0, verts, 0, player.primary_verts.length);
            } 
        }
    }
    
    private final int [] cell_dv = { -(MAZE_NUMCELLS_X+1), 1, MAZE_NUMCELLS_X+1, -1 };      
    
    // -----------------------------------------------------------------------------------------------
    private boolean canSeeThroughWall(Wall info) {
        if (info.type != WALL_TYPE_NONE && info.type != WALL_TYPE_ELECTRIC) {
            if (info.type != WALL_TYPE_DOOR || (info.state == DOOR_STATE_CLOSED || info.state == DOOR_STATE_LOCKED)) {
                return false;
            }
        }
        return true;
    }
    
    // -----------------------------------------------------------------------------------------------
    // return true if there are no walls between sx, sy
    private boolean canSee(int sx, int sy, int ex, int ey) {
        
        int [] sv = new int[5];
        int [] ev = new int[5];
        computePrimaryQuadrant(sx, sy, sv);
        computePrimaryQuadrant(ex, ey, ev);
        // compute
        
        int max = 90;
        
        while (true) {
            // if sv == ev, then we are done
            if (Arrays.equals(sv, ev))
                return true;            
            
            int dx = ex-sx;
            int dy = ey-sy;
            
            int d;
            
            //d = (dx*dx) + (dy*dy);
            
            //if (d < 100*100)
            //  return true;
            
            if (Math.abs(dx) < max && Math.abs(dy) < max)
                return true;
            
            d = this.getDirection(dx, dy);
            int dd = (d+1) % 4;
            
            Wall info = wall_lookup[sv[d]][sv[dd]];
            
            Utils.assertTrue(info != null);
            
            // allow see through electric walls and open(ing)/closing doors         
            if (!canSeeThroughWall(info))
                return false;
            
            int new_sx = 0;
            int new_sy = 0;
            for (int i=0; i<4; i++) {
                sv[i] += cell_dv[d];
                if (sv[i] < 0 || sv[i] >= MAZE_NUM_VERTS)
                    return false;
                new_sx += this.maze_verts_x[sv[i]];
                new_sy += this.maze_verts_y[sv[i]];
            }
            sx = new_sx/4;
            sy = new_sy/4;
        }
    }       
    
    private void computeCell(int px, int py, int [] cellXY) {
        final int cell_x = px * (MAZE_NUMCELLS_X) / (verts_max_x - verts_min_x);
        final int cell_y = py * (MAZE_NUMCELLS_Y) / (verts_max_y - verts_min_y);
        cellXY[0] = cell_x;
        cellXY[1] = cell_y;
    }
    
    
    // -----------------------------------------------------------------------------------------------
    private void computeBaseQuad(int px, int py, int [] result) {
        final int cell_x = px * (MAZE_NUMCELLS_X) / (verts_max_x - verts_min_x);
        final int cell_y = py * (MAZE_NUMCELLS_Y) / (verts_max_y - verts_min_y);
        
        result[0] = cell_x + cell_y * (MAZE_NUMCELLS_X + 1);
        result[1] = result[0] + 1;
        result[3] = result[1] + MAZE_NUMCELLS_X;
        result[2] = result[3] + 1;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Put into result the set of vertices that represent the quadrant the point P is inside
    //
    //        r[0]            r[1]
    //
    //
    //
    //
    //
    //        r[2]            r[3]
    //
    // r[4] is not used.
    // P can be any point inside the quadrant
    private void computePrimaryQuadrant(int px, int py, int [] result) {
        computePrimaryVerts(px, py, result);
        
        final int x0 = maze_verts_x[result[0]];
        final int y0 = maze_verts_y[result[0]]; 
        
        final int pdx = px-x0;
        final int pdy = py-y0;
        /*
        for (int i=1; i<=4; i++) {
            int v = result[i];
            if (v<0 || v>=this.MAZE_NUM_VERTS)
                continue;
            int x = maze_verts_x[v];
            int y = maze_verts_y[v];
            int dx = x-x0;
            int dy = y-y0;
            int dot = dx*pdx+dy*pdy;
            if (dot<=0)
                continue;
            int nx = -dy;
            int ny = dx;
            int dot2 = nx*pdx+ny*pdy;
            if (dot2 < 0) {
                nx=-nx;
                ny=-ny;
            }
            px = x0 + (dx+nx)*2/3;
            py = y0 + (dy+ny)*2/3;
            break;          
        }*/
        
        
        // determine what quadrant inside the 'wheel' P is in
        for (int i=1; i<=4 ; i++) {
            int ii = i+1;
            if (ii>4)
                ii=1;
            int v1 = result[i];
            int v2 = result[ii];
            if (v1<0 || v2<0 || v1>=this.MAZE_NUM_VERTS || v2 >= this.MAZE_NUM_VERTS)
                continue;
            int x1 = maze_verts_x[v1];
            int y1 = maze_verts_y[v1];
            int x2 = maze_verts_x[v2];
            int y2 = maze_verts_y[v2];
            int dx1 = x1-x0;
            int dy1 = y1-y0;
            int dx2 = x2-x0;
            int dy2 = y2-y0;
            int nx1 = -dy1;
            int ny1 = dx1;
            int nx2 = -dy2;
            int ny2 = dx2;
            int d1 = nx1*pdx + ny1*pdy;
            int d2 = nx2*pdx + ny2*pdy;
            if (d1>=0 && d2<=0) {
                px = x0 + (dx1+dx2)/2;
                py = y0 + (dy1+dy2)/2;
                break;
            }
        }
        computeBaseQuad(px, py, result);
    }
    
    // -----------------------------------------------------------------------------------------------
    // Put into result to set of vertices that represent a 'wheel'
    //           r[4]
    //            ^
    //            |
    //            |
    //   r[3]<- -r[0]- ->r[1]
    //            |
    //            |
    //            v
    //           r[2]
    //
    // P can be and coord such that dist(P-r[0]) is less than dist(P-r[1,2,3,4])
    //
    // if any point lies outside the world rect, it is < 0
    private void computePrimaryVerts(int px, int py, int [] result) {
        Utils.assertTrue(result.length == 5);
        
        computeBaseQuad(px, py, result);
        
        // these are the four in our region, find the closest
        int nearest = computeClosestPrimaryVertex(px, py, result);
        Arrays.fill(result, -1);
        
        // now build a tree where the closest is elem[0] and elems[1-3] are the 4
        // possible walls that extend from [0].  Set too -1 for those where wall not available
        
        result[0] = nearest;
        // if nearest is on the left edge
        int x = nearest % (MAZE_NUMCELLS_X+1);
        if (x > 0) {
            result[3] = nearest-1;
        }       
        if (x < MAZE_NUMCELLS_X) {
            result[1] = nearest+1;          
        } 
        
        result[0] = nearest;
        result[2] = nearest + MAZE_NUMCELLS_X+1;
        if (result[2] >= this.MAZE_NUM_VERTS)
            result[2] = -1;
        result[4] = nearest - (MAZE_NUMCELLS_X+1); // no test because will automatically be negative
    }
    
    // -----------------------------------------------------------------------------------------------
    // put the 4 primary verticies associated with this world point into result
    // this is an optimization to reduce time for collision scans
    // we take advanrage of the cell based maze to only scan for collisions in
    // the emediate vicinity of an object. There can only be 4 points in the
    // vicinity, we call these the 'primary vertices'
    private int computeClosestPrimaryVertex(int px, int py, int [] primary) {
        
        int nearest = -1;
        long bestDist = Integer.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            if (primary[i] < 0 || primary[i] >= MAZE_NUM_VERTS)
                continue;
            long dx = px - maze_verts_x[primary[i]];
            long dy = py - maze_verts_y[primary[i]];
            long d = (dx * dx) + (dy * dy);
            if (d < bestDist) {
                nearest = primary[i];
                bestDist = d;
            }
        }
        Utils.assertTrue(nearest >= 0);
        return nearest;
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean initCollision(int v0, int v1, Wall info) {
        collision_info_v0 = v0;
        collision_info_v1 = v1;
        collision_info_wallinfo = info;
        return true;
    }
    
    private final int [] collision_verts = new int[5];
    
    // -----------------------------------------------------------------------------------------------
    // return true when a line intersects any edge in the graph
    private boolean collisionScanLine(int x0, int y0, int x1, int y1) {
        int x2, y2, x3, y3, v0, v1;
        computePrimaryVerts(x0, y0, collision_verts);
        //int [] newDelta = new int[2];
        
        v0 = collision_verts[0];
        Utils.assertTrue(v0 >= 0);
        for (int i=1; i<collision_verts.length; i++) {
            v1 = collision_verts[i];
            
            if (v1<0 || v1>=MAZE_NUM_VERTS)
                continue;
            
            Wall info = wall_lookup[v0][v1]; 
            
            if (!isWallActive(info))
                continue;
            
            x2 = maze_verts_x[v0];
            y2 = maze_verts_y[v0];
            x3 = maze_verts_x[v1];
            y3 = maze_verts_y[v1];
            
            switch (info.type) {
            case WALL_TYPE_DOOR:
                if (collisionDoorLine(info, x0, y0, x1, y1, x2, y2, x3, y3))
                    return initCollision(v0, v1, info);
                break;
                
                // fall through
            
            default:
                if (Utils.isLineSegsIntersecting(x0, y0, x1, y1, x2, y2, x3, y3)) {
                    return initCollision(v0, v1, info);
                }
                break;
            }
            
        }
        return false;
    }
    
    private boolean collisionBreakingWallLine(Wall info, int lx0, int ly0, int lx1, int ly1, int wx0, int wy0, int wx1, int wy1, int [] newDelta) {
        float health = (1.0f/WALL_NORMAL_HEALTH)*info.health;
        // if wall is healthy, num==0
        // if wall is med, num 1
        // if wall is about to break, num = 2
        int num = Math.round((1.0f - health) * WALL_BREAKING_MAX_RECURSION);                                
        // make these values consistent based on garbage input
        //int xoff = (info.v0 * (-1 * info.v1 % 2) % 30);
        //int yoff = (info.v1 * (-1 * info.v0 % 2) % 30);
        return collisionBreakingWallLine_r(lx0, ly0, lx1, ly1, wx0, wy0, wx1, wy1, num, info.v0 + info.v1, newDelta);       
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean collisionDoorLine(Wall door, int lx0, int ly0, int lx1, int ly1, int wx0, int wy0, int wx1, int wy1) {
        
        final int framesElapsed = getFrameNumber() - door.frame;
        
        int dx = (wx1-wx0);
        int dy = (wy1-wy0);
        
        switch (door.state) {
        case DOOR_STATE_LOCKED:
        case DOOR_STATE_CLOSED:
            return Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy0, wx1, wy1);
        case DOOR_STATE_OPEN:
            dx /= 4;
            dy /= 4;
            break;
        default: 
        {
            int [] delta = getDoorDelta(dx, dy, framesElapsed, door.state);
            dx = delta[0];
            dy = delta[1];
        }
        break;
        }
        
        return Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx0, wy0, wx0+dx, wy0+dy) ||
                Utils.isLineSegsIntersecting(lx0, ly0, lx1, ly1, wx1, wy1, wx1-dy, wy1-dy);
    }
    
    // -----------------------------------------------------------------------------------------------
    private boolean collisionDoorCircle(Wall door, int x0, int y0, int x1, int y1, int px, int py, int radius) {
        final int framesElapsed = getFrameNumber() - door.frame;
        
        int dx = (x1-x0);
        int dy = (y1-y0);
        
        switch (door.state) {
        case DOOR_STATE_LOCKED:
        case DOOR_STATE_CLOSED:
            return Utils.isCircleIntersectingLineSeg(x0, y0, x1, y1, px, py, radius);           
        case DOOR_STATE_OPEN:
            dx /= 4;
            dy /= 4;
            break;
        default: 
        {
            int [] delta = getDoorDelta(dx, dy, framesElapsed, door.state);
            dx = delta[0];
            dy = delta[1];
        }
        break;
        }
        
        return Utils.isCircleIntersectingLineSeg(x0, y0, x0+dx, y0+dy, px, py, radius) ||
        Utils.isCircleIntersectingLineSeg(x1, y1, x1-dx, y1-dy, px, py, radius);
    }
    
    // -----------------------------------------------------------------------------------------------
    // return true when a sphere with radius at px,py is colliding with a wall
    private boolean collisionScanCircle(int px, int py, int radius) {
        
        // compute the 4 vertices associated with this vertex
        computePrimaryVerts(px, py, collision_verts);
        
        final int v0 = collision_verts[0];
        Utils.assertTrue(v0 >= 0);
        
        for (int i=1; i<collision_verts.length; i++) {
            final int v1 = collision_verts[i];
            
            if (v1<0 || v1 >= MAZE_NUM_VERTS)
                continue;
            
            Wall info = wall_lookup[v0][v1];
            
            if (!isWallActive(info))
                continue;
            
            final int x0 = maze_verts_x[v0];
            final int y0 = maze_verts_y[v0];
            final int x1 = maze_verts_x[v1];
            final int y1 = maze_verts_y[v1];
            
            if (info.type == WALL_TYPE_DOOR) {
                if (collisionDoorCircle(info, x0, y0, x1, y1, px, py, radius))
                    return initCollision(v0, v1, info);
            } else if (Utils.isCircleIntersectingLineSeg(x0, y0, x1, y1, px, py, radius)) {
                return initCollision(v0, v1, info);
            }
            
        }
        return false;
    }
    
    // -----------------------------------------------------------------------------------------------
    // put vector V bounced off wall W into result
    private void bounceVectorOffWall(int vx, int vy, int wx, int wy, int[] result) {
        // look for best case
        if (wx == 0) {
            result[0] = -vx;
            result[1] = vy;
            return;
        } else if (wy == 0) {
            result[0] = vx;
            result[1] = -vy;
            return;
        }
        
        // do the bounce off algorithm (Thanx AL)
        // get normal to the wall
        float nx = -(float) wy;
        float ny = (float) wx;
        
        // compute N dot V
        float ndotv = nx * (float) vx + ny * (float) vy;
        
        // make sure the normal is facing toward missle by comparing dot
        // products
        if (ndotv > 0.0f) {
            // reverse direction of N
            nx = -nx;
            ny = -ny;
        }
        
        ndotv = Math.abs(ndotv);
        
        // compute N dot N
        float ndotn = (nx * nx + ny * ny);
        
        // compute projection vector
        int px = Math.round((nx * ndotv) / ndotn);
        int py = Math.round((ny * ndotv) / ndotn);
        
        // assign new values to missle motion
        result[0] = vx + 2 * px;
        result[1] = vy + 2 * py;
    }
    
    public void initGraphics(AGraphics g) {
        this.G = g;
        initImages(g);
        initColors(g);
    }
    
    private int imageKey = -1;
    private int imageLogo = -1;
    
    // -----------------------------------------------------------------------------------------------
    private void initImages(AGraphics g) {
        if (imageKey >= 0)
            return;
        imageKey  = g.loadImage("images/key.gif", g.BLACK);
        imageLogo = g.loadImage("images/logo.gif", g.BLACK);
        animJaws = g.loadImageCells("images/jaws.gif" , 32, 32, 8, 9, true, g.BLACK);
        animLava = g.loadImageCells("images/lavapit.gif", 32, 32, 8, 25, true, g.BLACK);
        animPeople = new int[PEOPLE_NUM_TYPES][];       
        animPeople[0] = g.loadImageCells("images/people.gif", 32, 32, 4, 16, true, g.BLACK);
        animPeople[1] = g.loadImageCells("images/people2.gif", 32, 32, 4, 16, true, g.BLACK);
        animPeople[2] = g.loadImageCells("images/people3.gif", 32, 32, 4, 16, true, g.BLACK);
    }
    
    private void initImages_png(AGraphics g) {
        imageKey  = g.loadImage("pngs/key.png", g.BLACK);
        imageLogo = g.loadImage("pngs/logo.png", g.BLACK);
        animJaws = g.loadImageCells("pngs/jaws.png" , 32, 32, 8, 9, true, g.BLACK);
        animLava = g.loadImageCells("pngs/lavapit.png", 32, 32, 8, 25, true, g.BLACK);
        animPeople = new int[PEOPLE_NUM_TYPES][];       
        animPeople[0] = g.loadImageCells("pngs/people.png", 32, 32, 4, 16, true, g.BLACK);
        animPeople[1] = g.loadImageCells("pngs/people2.png", 32, 32, 4, 16, true, g.BLACK);
        animPeople[2] = g.loadImageCells("pngs/people3.png", 32, 32, 4, 16, true, g.BLACK);
    }

    
    // -----------------------------------------------------------------------------------------------
    private void drawIntroPlayers(AGraphics g, int frame) {
        
        // draw the player modes
        int x1 = screen_width / 3;
        int x2 = screen_width / 2;
        int x3 = screen_width * 3/4;
        
        int sy = 50;
        int dy = (screen_height-sy) / 5;
        AColor textColor = g.RED;            
        int dir = this.DIR_DOWN;
        
        sy += this.PLAYER_RADIUS/2;     
        
        Player player = players[0];
        drawPlayer(player, g, x1, sy, dir);
        sy += dy;
        
        player.powerup = POWERUP_HULK;
        player.scale = this.PLAYER_HULK_SCALE;
        drawPlayer(player, g, x1, sy, dir);
        player.scale = 1;
        g.setColor(textColor);
        g.drawString("HULK", x2, sy);
        g.drawString("Charge Enemies", x3, sy);
        sy += dy;
        
        player.powerup = POWERUP_GHOST;
        drawPlayer(player, g, x1, sy, dir);
        g.setColor(textColor);
        g.drawString("GHOST", x2, sy);
        g.drawString("Walk through walls", x3, sy);
        sy += dy;
        
        player.powerup = this.POWERUP_BARRIER;
        drawPlayer(player, g, x1, sy, dir);
        g.setColor(textColor);
        g.drawString("BARRIER", x2, sy);
        g.drawString("Protects against missles", x3, sy);
        sy += dy;
        
        player.powerup = -1; // must be last!
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawIntroWallTypes(AGraphics g, int frame) {
        
        // draw the player modes
        int x1 = screen_width / 3;
        int x2 = screen_width / 2;      
        int sy = 50;
        int dy = (screen_height-sy) / WALL_NUM_TYPES;
        AColor textColor = g.RED;        
        
        Wall info = new Wall();
        info.health = 100;
        info.frequency = RUBBER_WALL_MAX_FREQUENCY;
        int wallSize = dy/2-3;
        sy += wallSize;
        
        for (int i=this.WALL_TYPE_NORMAL; i<this.WALL_NUM_TYPES; i++) {
            info.type = i;
            this.drawWall(g, info, x1-wallSize, sy-wallSize, x1+wallSize, sy+wallSize);
            
            g.setColor(textColor);
            g.drawString(getWallTypeString(i), x2, sy);
            
            sy += dy;
        }
        
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawIntroPowerups(AGraphics g, int frame) {
        
        // draw the player modes
        int x1 = screen_width / 3;
        int x2 = screen_width / 2;
        int sy = 50;        
        int dy = (screen_height-sy) / (this.POWERUP_NUM_TYPES+1);       
        AColor textColor = g.WHITE;        
        
        sy += 16;
        //this.drawStickFigure(g, x1, sy, this.PEOPLE_RADIUS);
        //this.drawPeople(g)
        int type = (getFrameNumber() / 30) % PEOPLE_NUM_TYPES;
        int dir  = (getFrameNumber() / 60) % 4;
        g.setColor(g.WHITE);
        this.drawPerson(g, x1, sy, 32, type, dir);
        g.setColor(textColor);
        g.drawString("HUMAN", x2, sy);
        sy += dy;
        
        for (int i=0; i<this.POWERUP_NUM_TYPES; i++) {
            drawPowerup(g, i, x1, sy);
            
            g.setColor(textColor);
            g.drawString(this.getPowerupTypeString(i), x2, sy);
            sy += dy;
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    private void drawIntroEnemies(AGraphics g, int frame) {
        
        // draw the player modes
        int x1 = screen_width / 3;
        int x2 = screen_width / 2;
        int x3 = screen_width * 3 / 4;
        int sy = 50;
        AColor textColor = g.RED;
        int dy = (screen_height-sy) / 6;
        
        this.drawRobot(g, x1, sy, this.DIR_DOWN);
        g.setColor(textColor);
        g.drawString("ROBOT", x2, sy);
        g.drawString(String.valueOf(this.ENEMY_ROBOT_POINTS), x3, sy);
        sy += dy;
        
        this.drawBrain(g, x1, sy, 10);
        g.setColor(textColor);
        g.drawString("BRAIN", x2, sy);
        g.drawString(String.valueOf(this.ENEMY_BRAIN_POINTS), x3, sy);
        sy += dy;
        
        this.drawGenerator(g, x1, sy);
        g.setColor(textColor);
        g.drawString("GENERATOR", x2, sy);      
        g.drawString(String.valueOf(this.ENEMY_GEN_POINTS), x3, sy);
        sy += dy;
        
        this.drawTank(g, x1, sy, this.DIR_DOWN);
        g.setColor(textColor);
        g.drawString("TANK", x2, sy);
        g.drawString(String.valueOf(this.ENEMY_TANK_POINTS), x3, sy);
        sy += dy;
        
        this.drawThug(g, x1, sy, this.DIR_DOWN);
        g.setColor(textColor);
        g.drawString("THUG", x2, sy);
        g.drawString("INDESTRUCTABLE", x3, sy);
        sy += dy;
        
        this.drawEnd(g, x1, sy);
        g.setColor(textColor);
        g.drawString("NEXT LEVEL PORTAL", x2, sy);
        sy += dy;
    }
    
    // -----------------------------------------------------------------------------------------------
    // Draw the intro screen showing the elems of the game and controls
    private void drawIntro(AGraphics g) {
        
        if (imageLogo >= 0) {
            g.setColor(g.WHITE);
            g.drawImage(imageLogo, 0, 0, 256, 256);
        }
        
        int introSpacingFrames = 100;
        int numIntros = 4;
        
        int frame = getFrameNumber() % (introSpacingFrames * numIntros);
        
        if (frame < introSpacingFrames) {
            drawIntroPlayers(g, 0);
        } else if (frame < 2*introSpacingFrames) {
            drawIntroWallTypes(g, frame - introSpacingFrames);
        } else if (frame < 3*introSpacingFrames) {
            drawIntroPowerups(g, frame - 2*introSpacingFrames);
        } else if (frame < 4*introSpacingFrames) {
            drawIntroEnemies(g, frame- 3*introSpacingFrames);
        }
        
        // draw buttons for selecting the gametype
        int y = 0;
        for (int i = 0; i < BUTTONS_NUM; i++) {
            if (button_active == i) {
                g.setColor(g.RED);
                g.drawFilledRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT);
            }
            boolean outline = false;
            switch (Button.values()[i]) {
                case Classic:
                    if (this.game_type == this.GAME_TYPE_CLASSIC) {
                        outline = true;
                    }
                    break;
                case RoboCraze:
                    if (this.game_type == this.GAME_TYPE_ROBOCRAZE) {
                        outline = true;
                    }
                    break;
                case Easy:
                    if (difficulty == DIFFICULTY_EASY) {
                        outline = true;
                    }
                    break;
                case Medium:
                    if (difficulty == DIFFICULTY_MEDIUM) {
                        outline = true;
                    }
                    break;
                case Hard:
                    if (difficulty == DIFFICULTY_HARD) {
                        outline = true;
                    }
                    break;
                case START:
                    break;
            }
            if (outline) {
                g.setColor(g.GREEN);
                g.drawRect(button_x[i] + 1, button_y[i] + 1, BUTTON_WIDTH - 2, BUTTON_HEIGHT - 2);
                g.drawRect(button_x[i] + 2, button_y[i] + 2, BUTTON_WIDTH - 4, BUTTON_HEIGHT - 4);
            }
            g.setColor(g.CYAN);
            g.drawRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT);
            g.drawString(Button.values()[i].name(), button_x[i] + 5, button_y[i] + 5);
            y = button_y[i];
        }
        
        y += BUTTON_HEIGHT + 20;
        int x = button_x[0];
        String details = "Use 'WASD' keys to move\nUse mouse to fire";
        g.setColor(g.CYAN);
        g.drawString(details, x, y);
        
    }
    
    private void drawIntro_old(AGraphics g) {
        
        int left = 10;
        int top = 20;
        int v_spacing = 20;
        int h_spacing = screen_width / 3;
        
        AColor textColor = g.BLUE;
        
        // Game name ect
        g.setColor(g.RED);
        g.drawString("RoboCraze 2.0", left, top);
        top += v_spacing;
        g.setColor(textColor);
        g.drawString("ccaron 1/2008", left, top);
        top += v_spacing * 2;
        // draw the player
        float frameNum = getFrameNumber() % 200;
        Player player = players[0];
        if (frameNum < 100) {
            g.drawString("Player", left, top);
            player.scale = 1.0f + frameNum/100.0f;
        } else {
            g.drawString("Hulk", left, top);
            player.scale = PLAYER_HULK_SCALE - (frameNum-100)/100.0f;;
        }
        drawPlayer(player, g, left + h_spacing, top - PLAYER_RADIUS, 2);
        top += v_spacing + PLAYER_RADIUS * 2;
        // draw a people
        g.setColor(textColor);
        g.drawString("Person (" + PEOPLE_START_POINTS + " - " + PEOPLE_MAX_POINTS + " points)", left, top);
        g.setColor(g.RED);
        drawStickFigure(g, left + h_spacing, top - PEOPLE_RADIUS, PEOPLE_RADIUS);
        top += v_spacing + PEOPLE_RADIUS * 2;
        // draw a generator
        drawGenerator(g, left + h_spacing, top - ENEMY_GEN_RADIUS);
        g.setColor(textColor);
        g.drawString("Generator (" + ENEMY_GEN_POINTS + " points)", left, top);
        top += v_spacing + ENEMY_GEN_RADIUS;
        // draw an enemy guy
        g.setColor(textColor);
        g.drawString("Enemy Guy (" + ENEMY_ROBOT_POINTS + " points)", left, top);
        g.setColor(g.LIGHT_GRAY);
        drawRobot(g, left + h_spacing, top - ENEMY_ROBOT_RADIUS, 2);
        top += v_spacing + ENEMY_ROBOT_RADIUS * 2;
        // draw a thug
        drawThug(g, left + h_spacing, top - ENEMY_THUG_RADIUS, 1);
        g.setColor(textColor);
        g.drawString("Enemy Thug (Indestructable)", left, top);
        top += v_spacing + ENEMY_THUG_RADIUS * 2;
        // draw a brain
        g.setColor(textColor);
        g.drawString("Enemy Brain (" + ENEMY_BRAIN_POINTS + " points)", left, top);
        drawBrain(g, left + h_spacing, top - ENEMY_BRAIN_RADIUS, ENEMY_BRAIN_RADIUS);
        top += v_spacing + ENEMY_BRAIN_RADIUS * 2;
        // draw a tank
        g.setColor(textColor);
        g.drawString("Enemy Tank (" + ENEMY_TANK_POINTS + " points)", left, top);
        g.setColor(g.DARK_GRAY);
        drawTank(g, left + h_spacing, top - ENEMY_TANK_RADIUS, 1);
        top += v_spacing + ENEMY_TANK_RADIUS * 2;
        // draw a tank gen
        g.setColor(textColor);
        g.drawString("Tank Generator (" + ENEMY_TANK_GEN_POINTS + " points)", left, top);
        g.setColor(g.GREEN);
        drawTankGen(g, left + h_spacing, top - ENEMY_TANK_GEN_RADIUS);
        top += v_spacing + ENEMY_TANK_GEN_RADIUS * 2;
        // draw the ending X
        g.setColor(g.RED);
        drawEnd(g, left + h_spacing, top);
        g.setColor(textColor);
        g.drawString("GOAL", left, top - 4);
        top += v_spacing * 2;
        
        // draw some instructions on how to play
        left = screen_width / 2;
        top = 20;
        g.setColor(g.LIGHT_GRAY);
        g.drawString("Use [AWDS] to move", left, top);
        top += v_spacing * 2;
        g.drawString("Use Mouse to aim and fire missles", left, top);
        top += v_spacing * 2;
        g.drawString("Extra man every " + PLAYER_NEW_LIVE_SCORE + " points", left, top);
        
        // draw throbbing white text indicating to the player how to start
        top += v_spacing * 2;
        g.setColor(throbbing_white);
        g.drawString("Click on game type to play", left, top);
        
        // draw buttons for selecting the gametype
        for (int i = 0; i < BUTTONS_NUM; i++) {
            if (button_active == i) {
                g.setColor(g.RED);
                g.drawFilledRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT);
            }
            if (difficulty + 2 == i) {
                g.setColor(g.GREEN);
                g.drawRect(button_x[i] + 1, button_y[i] + 1, BUTTON_WIDTH - 2, BUTTON_HEIGHT - 2);
                g.drawRect(button_x[i] + 2, button_y[i] + 2, BUTTON_WIDTH - 4, BUTTON_HEIGHT - 4);
            }
            g.setColor(g.CYAN);
            g.drawRect(button_x[i], button_y[i], BUTTON_WIDTH, BUTTON_HEIGHT);
            g.drawString(Button.values()[i].name(), button_x[i] + 5, button_y[i] + BUTTON_HEIGHT - 5);
        }
    }
    
    // -----------------------------------------------------------------------------------------------
    public void drawGame(AGraphics g) {
        
        
        frame_number += 1;
        g.clearScreen(g.BLACK);
        
        switch (game_state) {
        case GAME_STATE_INTRO: // ---------------------------------------------------------
            drawIntro(g);
            break;
            
        case GAME_STATE_PLAY: // ----------------------------------------------------------

            updatePlayers();
            updatePeople();
            updateMissles();
            updateEnemies();
            updatePowerups();
            
            updateAndDrawZombieTracers(g);
            for (int i=0; i<num_players; i++)
                updateAndDrawPlayerTracers(players[i], g);
            drawMaze(g);
            updateAndDrawMessages(g);
            updateAndDrawParticles(g);
            drawEnemies(g);
            drawPeople(g);
            drawPowerups(g);
            for (int i=0; i<num_players; i++) {
                Player player = players[i];
                switch (player.state){
                    case PLAYER_STATE_SPAWNING:
                        drawPlayerHighlight(players[0], g);
                    case PLAYER_STATE_ALIVE:
                    case PLAYER_STATE_TELEPORTED:
                        drawPlayer(player, g, player.x - screen_x, player.y - screen_y, player.dir);
                        break;
                    case PLAYER_STATE_EXPLODING:
                        drawPlayerExploding(player, g);
                        break;
                    case PLAYER_STATE_SPECTATOR:
                        break;
                        
                }
                
            }
            drawMissles(g);
            if (this.game_type != GAME_TYPE_CLASSIC)
                drawEnd(g, end_x - screen_x, end_y - screen_y);
            drawPlayerInfo(players[0], g);
            if (Utils.DEBUG_ENABLED) { 
                drawDebug(g);
                drawDebugButtons(g);
            }
            
            break;
            
        case GAME_STATE_GAME_OVER: // ---------------------------------------------------------
            
            drawMaze(g);
            drawEnemies(g);
            drawPeople(g);
            drawMissles(g);
            if (this.game_type != GAME_TYPE_CLASSIC)
                drawEnd(g, end_x - screen_x, end_y - screen_y);
            drawPlayerInfo(players[0], g);
            drawGameOver(g);
            
            break;
            
        } // end switch
        
        updateThrobbingWhite();
        //updateFPS(g);
    }

    /*
    private int frameCount = 0;
    private long frameStartTime = 0;
    private int FPS = 0;

    private void updateFPS(AGraphics g) {
        long t = getClock();
        if (frameStartTime == 0) {
            frameStartTime = t;
            frameCount = 1;
        } else {
            long dt = t - frameStartTime;
            if (dt >= 1000) {
                FPS = frameCount;
                frameStartTime = t;
                frameCount = 0;
            }
            frameCount++;
        }
        g.setColor(g.WHITE);
        float x = screen_width - 30;
        int y = 10;
        float w = g.drawJustifiedString(x, y, Justify.RIGHT, "FPS:");
        this.drawNumberString(g, Math.round(x+w), y, Justify.RIGHT, FPS);
    }*/
    
    // -----------------------------------------------------------------------------------------------
    private boolean isInMaze(int x, int y) {
        int padding = 10;
        int minx = padding;
        int miny = padding;
        int maxx = WORLD_WIDTH_CLASSIC - padding;
        int maxy = WORLD_HEIGHT_CLASSIC - padding;
        if (game_type != GAME_TYPE_CLASSIC) {
            minx = MAZE_VERTEX_NOISE + padding;
            miny = MAZE_VERTEX_NOISE + padding;
            maxx = MAZE_WIDTH - MAZE_VERTEX_NOISE - padding;
            maxy = MAZE_HEIGHT - MAZE_VERTEX_NOISE - padding;
        }
        
        boolean inside = (x > minx) && (x < maxx) && (y > miny) && (y < maxy);
        return inside;
    }
    
    // -----------------------------------------------------------------------------------------------
    // This is placeed close too keyTyped so we can update when necc
    private void drawDebugButtons(AGraphics g) {
        int w = screen_width / (Debug.values().length);
        int x = w / 2;
        int y = screen_height - 20;
        for (Debug d : Debug.values()) {
            if (isDebugEnabled(d))
                g.setColor(g.RED);
            else
                g.setColor(g.CYAN);
            g.drawJustifiedString(x, y, Justify.CENTER, d.indicator);
            x += w;
        }
    }
    
    private int debug_enabled_flag = 0;
    
    // enum --
    public enum Debug {
        DRAW_MAZE_INFO("Maze Info"),
        DRAW_PLAYER_INFO("Player Info"),
        DRAW_ENEMY_INFO("Enemy Info"), 
        INVINCIBLE("Invincible"), // cant die
        GHOST("Ghost"), // cant die and walk through walls
        BARRIER("Barrier"), // barrier allows walk through walls and protection from projectiles
        HULK("Hulk"), // can smash through walls and enemies
        PATH("Path"), // show path to the end
        
        ;
        Debug(String indicator) {
            this.indicator = indicator;
        }
        final String indicator;
    };
    
    public boolean isDebugEnabled(Debug debug) {
        return (debug_enabled_flag & (1 << debug.ordinal())) != 0; 
    }
    
    public void setDebugEnabled(Debug debug, boolean enabled) {
        if (enabled) {
            debug_enabled_flag |= (1<<debug.ordinal());
        } else {
            debug_enabled_flag &= ~(1<<debug.ordinal());
        }
    }
    
    public void setPlayerMovement(int dx, int dy) {
        Player player = getPlayer();
        if (dx < 0) {
            player.dx = -PLAYER_SPEED;
        } else if (dx > 0) {
            player.dx = PLAYER_SPEED;
        } else {
            player.dx = 0;
        }
        
        if (dy < 0) {
            player.dy = -PLAYER_SPEED;
        } else if (dy > 0) {
            player.dy = PLAYER_SPEED;
        } else {
            player.dy = 0;
        }
    }
    public boolean isPlayerSpectator() {
        return getPlayer().state == PLAYER_STATE_SPECTATOR;
    }

    // -----------------------------------------------------------------------------------------------
    private void setIntroButtonPositionAndDimension() {
        int x = 8;
        int y = 50;
        
        for (int i=0; i<BUTTONS_NUM; i++) {
            button_x[i] = x;
            button_y[i] = y;
            
            y += this.BUTTON_HEIGHT + 5;
        }
    }

    long baseTime = System.nanoTime();
    
    private long getClock() {
        long t = System.nanoTime() - baseTime;
        return t / 1000000;
    }
    
    private long downTime = 0;

    public void setCursorPressed(boolean pressed) {
        if (pressed) {
            switch (game_state) {
                case GAME_STATE_INTRO:
                    downTime = getClock();
                    break;
                case GAME_STATE_PLAY:
                //case GAME_STATE_PLAYER_HIT:
                case GAME_STATE_GAME_OVER:
                     players[0].firing = true;
                     break;
            }
        } else {
            switch (game_state) {
                case GAME_STATE_INTRO:
                    if (downTime > 0 && getClock() - downTime < 1000) {
                        newGame();
                    }
                    downTime = 0;
                    break;
                case GAME_STATE_PLAY:
                case GAME_STATE_GAME_OVER:
                    players[0].firing = false;
                    break;
                    
            }
        }
    }
    
    private void newPlayerGame(Player player) {
        player.lives = PLAYER_START_LIVES;
        player.score = 0;
        
    }
    
    private void newGame() {
        if (button_active >= 0 && button_active < this.BUTTONS_NUM) {
            switch (Button.values()[button_active]) {
                case Classic:
                    game_type = this.GAME_TYPE_CLASSIC;
                    break;
                case RoboCraze:
                    game_type = this.GAME_TYPE_ROBOCRAZE;
                    break;
                case Easy:
                    difficulty = DIFFICULTY_EASY;
                    break;
                case Medium:
                    difficulty = DIFFICULTY_MEDIUM;
                    break;
                case Hard:
                    difficulty = DIFFICULTY_HARD;
                    break;
                case START:
                    button_active = -1;
                    game_level = 1;
                    game_state = GAME_STATE_PLAY;
                    
                    for (int i=0; i<num_players; i++)
                        newPlayerGame(players[i]);
                    
                    if (Utils.DEBUG_ENABLED)
                        Utils.setRandomSeed(0);
                    else
                        Utils.setRandomSeed(getClock());
                    
                    buildAndPopulateLevel();                                    
                    break;
            }
        }
        
    }
    
    public void setCursor(int x, int y) {
        switch (game_state) {
            case GAME_STATE_INTRO: {
                button_active = -1;
                for (int i = 0; i < BUTTONS_NUM; i++) {
                    if (x > button_x[i] && x < button_x[i] + BUTTON_WIDTH && y > button_y[i] && y < button_y[i] + BUTTON_HEIGHT) {
                        button_active = i;
                    }
                }
                break;
                    
            }
            case GAME_STATE_PLAY:
            case GAME_STATE_GAME_OVER:
                players[0].target_x = Math.round(screen_x + x);
                players[0].target_y = Math.round(screen_y + y);
                break;
                
        }                
    }
    
    public int getGameLevel() {
        return game_level;
    }
    
    public boolean getGameVisibility() {
        return this.game_visibility;
    }
}
