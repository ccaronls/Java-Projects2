package cc.fantasy.service;

import java.util.*;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.Team;
import cc.fantasy.model.User;
import cc.fantasy.model.type.LeagueStatus;

public interface IFantasyService {
    
    // User Management
    
    User getUser(int id);
    User getUser(String userName, String passWord);
    List<User> getUsers(Search<UserSearchKey> search);
    void addOrUpdateUser(User user);
    int getUserCount();

    // Franchise Management    
    Franchise getFranchise(int id);
    List<Franchise> getFranchises(Search<FranchiseSearchKey> search);
    void addOrUpdateFranchise(Franchise franchise);
    int getFranchiseCount();
    void deleteFranchise(Franchise f);
    List<String> getAllFranchiseCategories();
    
    // League Management    
    List<League> getLeagues(Search<LeagueSearchKey> search); 
    List<League> getLeagues(User user, Search<LeagueSearchKey> search);
    League getLeague(int id);
    void addOrUpdateLeague(League league);
    List<League> getLeagues(Franchise franchise, Search<LeagueSearchKey> search);
    List<League> getLeaguesByStatus(LeagueStatus status, Search<LeagueSearchKey> search);
    int getLeaguesCount(Franchise franchise);
    int getLeaguesCount(User user);
    void addOrUpdateLeagues(List<League> leagues);
    
    // Team Management
    Team getTeam(int id);
    List<Team> getTeams(User user, Search<TeamSearchKey> search);
    List<Team> getTeamsByLeague(int leagueId, Search<TeamSearchKey> search);
    int getTeamsCount(League league);
    int getTeamsCount(User user);
    Team getLeagueLeader(int leagueId);
    int getTeamRank(Team team);
    void addOrUpdateTeam(Team team);
    void addOrUpdateTeams(List<Team> teams);
}
