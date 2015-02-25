package cc.game.soc.ai;

enum NodeType {
    // indicates the root of the tree
    ROOT,
    MOVE_CHOICE,
    SETTLEMENT_CHOICE,
    ROAD_CHOICE,
    SHIP_CHOICE,
    SHIP_MOVE_CHOICE,
    CITY_CHOICE,
    CITY_WALL_CHOICE,
    ROBBER_CHOICE,
    TRADE_CHOICE,
    RESOURCE_CHOICE,
    GIVEUP_CARD, 
    TAKE_PLAYER_CARD,
    KNIGHT_CHOICE,
    DEVELOPMENT_AREA_CHOICE,
    IMPROVE_CITY,
}
