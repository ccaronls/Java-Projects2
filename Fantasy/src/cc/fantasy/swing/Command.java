package cc.fantasy.swing;

public enum Command {

    // Login
    NEW_USER("New User"),
    SAVE_USER_ADMIN("Create Admin"),
    SAVE_USER_BASIC("Create Player"),
    SAVE_USER_LEAGUEMGR("Create League Manager"),
    SAVE_USER("SAVE"),
    CANCEL_EDIT_USER("CANCEL"),
    LOGIN("Login"),
    LOGOUT("LOGOUT"),
    TOGGLE_USER_ACTIVE("--"),
    
	// admin ops
    VIEW_FRANCHISES("All Franchises"),
    VIEW_LEAGUES("All Leagues"),
    VIEW_TEAMS("All Teams"),
    VIEW_USERS("All Users"),
    
    // franchises
    DELETE_FRANCHISE_CONFIRM("DELETE"),
    DELETE_FRANCHISE("--"),
    SAVE_FRANCHISE("SAVE"),
    SAVE_FRANCHISE_PLAYER("SAVE"),
    CANCEL_EDIT_FRANCHISE_PLAYER("CANCEL"),
    CANCEL_EDIT_FRANCHISE_POSITION("CANCEL"),
    VIEW_FRANCHISE_DETAILS("Details"),
    NEW_FRANCHISE("New Franchise"),
    NEW_POSITION("NEW POSITION"),
    SAVE_FRANCHISE_POSITION("SAVE"),
    EDIT_POSITION("EDIT"),
    DELETE_POSITION("DELETE"),
    SHOW_IMPORT_POSITION("IMPORT"),
    IMPORT_POSITION_DATA("IMPORT"),
    UPDATE_FRANCHISE("UPDATE"),
    CHOOSE_SPREADSHEET_FILE("<choose>"),
    CHOOSE_FRANCHISE_CATEGORY("<choose>"),
    SET_FRANCHISE_CATEGORY("--"),
    TOGGLE_FRANCHISE_ACTIVE("--"),
    NEW_PLAYER("NEW PLAYER"),
    EDIT_PLAYER("EDIT"),
    DELETE_FRANCHISE_PLAYER("DELETE"),
    EDIT_FRANCHISE_POSITION_STATS("--"),
    NEW_FRANCHISE_POSITION_STAT("NEW STAT"),
    EDIT_FRANCHISE_PLAYER_POSITIONS("--"),
    REMOVE_PLAYER_POSITION("Remove"),
    TOGGLE_PLAYER_POSITION_ENABLED("--"),
    
    // League
    EDIT_LEAGUE("EDIT"),
    SAVE_STAT("SAVE"),
    DELETE_STAT("DELETE"),
    EDIT_TEAM("EDIT"),
    JOIN_LEAGUE("JOIN LEAGUE"),
    EDIT_LEAGUE_POSITIONS("POSITIONS"),
    EDIT_LEAGUE_POSITION_STATS("--"),
    SAVE_LEAGUE("SAVE"),
    EDIT_LEAGUE_POSITIONS_DONE("SAVE"),
    EDIT_LEAGUE_DONE("DONE"),
    EDIT_LEAGUE_POSITION_STATS_DONE("DONE"),
    NEW_TEAM("JOIN LEAGUE"),
    EDIT_TEAM_POSITION("--"),
    SAVE_TEAM("SAVE"),
    EDIT_TEAM_DONE("DONE"),
    SAVE_TEAM_PLAYER_RANKINGS("SAVE"),
    INCREMENT_MULTIPLIER("--"),
    MONITOR_TEAM("MONITOR"),
    CHANGE_TEAM_PLAYER("CHANGE PLAYER"),
    SET_TEAM_PLAYER("--"),
    
    // mgr ops
    NEW_LEAGUE("New League"),
    VIEW_USER_LEAGUES("My Leagues"),
    VIEW_USER_TEAMS("My Teams"),
    VIEW_USER("--"),
    
    // League ops
    LEAGUE_DETAILS("DETAILS"),
    RUN_LEAGUE_DRAFT("RUN DRAFT"),
    SET_FRANCHISE("--"),
    OK("Ok"),
    CANCEL("CANCEL"),
    CHOOSE_FRANCHISE("<choose>"),

    // Sorting ops
    SORT_USER_LIST("--"),
    SORT_TEAM_LIST("--"),
    SORT_FRANCHISE_LIST("--"),
    SORT_LEAGUE_LIST("--"),
    ;
    
    private final String text;
    
    private Command(String text) {
        this.text = text;
    }
    
    public String getText() { 
        return text;
    }
}
