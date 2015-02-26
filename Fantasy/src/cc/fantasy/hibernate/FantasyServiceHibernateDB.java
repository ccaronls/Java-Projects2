package cc.fantasy.hibernate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Category;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

import cc.fantasy.exception.ErrorCode;
import cc.fantasy.exception.FantasyException;
import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.type.LeagueStatus;
import cc.fantasy.model.Team;
import cc.fantasy.model.User;
import cc.fantasy.service.FranchiseSearchKey;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.service.LeagueSearchKey;
import cc.fantasy.service.Search;
import cc.fantasy.service.TeamSearchKey;
import cc.fantasy.service.UserSearchKey;

public class FantasyServiceHibernateDB extends HibernateDaoSupport implements IFantasyService {

    private Category log = Category.getInstance(getClass());

    public void addOrUpdateFranchise(Franchise franchise) {
        getHibernateTemplate().saveOrUpdate(franchise);
    }

    public void addOrUpdateLeague(League league) {
        getHibernateTemplate().saveOrUpdate(league);
    }

    public void addOrUpdateLeagues(List<League> leagues) {
        getHibernateTemplate().saveOrUpdateAll(leagues);
    }

    public void addOrUpdateUser(User user) {
        getHibernateTemplate().saveOrUpdate(user);
    }

    public void deleteFranchise(Franchise f) {
        throw new RuntimeException("Not implemented");
    }

    public List<String> getAllFranchiseCategories() {
        Session session = getSession();
        try {
            return session.createQuery("select unique category from Franchise").list();
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return new ArrayList();
        } finally {
            session.close();
        }
    }

    public Franchise getFranchise(int id) {
        return getById(Franchise.class, id);
    }

    public int getFranchiseCount() {
        return getCount(Franchise.class);
    }
    
    private int getCount(Class clazz) {
        return getCount(clazz, new Criterion[0]);
    }
    
    private int getCount(Class clazz, Criterion crit) {
        return getCount(clazz, new Criterion[] { crit });
    }

    //private int getCount(Class clazz, Criterion c0, Criterion c1) {
    //    return getCount(clazz, new Criterion[] { c0, c1 });
    //}
    
    private <T> T getById(Class<T> clazz, int id) {
        T o = (T)getHibernateTemplate().get(clazz, new Integer(id));
        if (o == null)
            throw new FantasyException(ErrorCode.INVALID_ID, clazz.getSimpleName() + " [" + id + "]");
        return o;
    }

    private int getCount(Class clazz, Criterion [] crit) {
        Session session = getSession();
        try {
            Criteria criteria = session.createCriteria(clazz).setProjection(Projections.rowCount());
            for (int i=0; i<crit.length; i++) {
                criteria.add(crit[i]);
            }
            
            String num = criteria.uniqueResult().toString();
            return Integer.parseInt(num);
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
            return 0;
        } finally {
            session.close();
        }
    }
    
    private List getList(Search search, Class clazz, String q) {
        if (search == null)
            return getHibernateTemplate().find("from " + clazz.getName() + " " + q);
        
        Session session = getSession();
        try {
            Query query = session.createQuery("select count (*) from " + clazz.getName() + " " + q);
            int total = Integer.parseInt(query.uniqueResult().toString());
            search.setTotal(total);
            
            String type = clazz.getSimpleName().toLowerCase();
            
            query = session.createQuery("select " + type + " from " + clazz.getName() + " type " + q);
            query.setFirstResult(search.getOffset()).setMaxResults(search.getMax());
            
            return query.list();
            
        } finally {
            session.close();
        }
    }

    public List<Franchise> getFranchises(Search<FranchiseSearchKey> search) {
        return getList(search, Franchise.class, "");
    }

    public League getLeague(int id) {
        return getById(League.class, id);
    }
    
    public List<League> getLeagues(Search<LeagueSearchKey> search) {
        return getList(search, League.class, "");
    }

    public List<League> getLeagues(Franchise franchise, Search<LeagueSearchKey> search) {
        return getList(search, League.class, "where franchiseId='" + franchise.getId() + "'");
    }

    public List<League> getLeagues(User user, Search<LeagueSearchKey> search) {
        return getList(search, League.class, "where userId='" + user.getId() + "'");
    }
    
    public int getLeaguesCount(User user) {
        return getCount(League.class);
	}

	public List<League> getLeaguesByStatus(LeagueStatus status, Search<LeagueSearchKey> search) {
        String q = "";
        Date now = new Date();
        switch (status) {
        case OPEN: q = "where draft > '" + now + "'"; break;
        case CLOSED: q = "where end > '" + now + "'"; break;
        case DONE: q = "where end < '" + now + "'"; break;
        }
        return getList(search, League.class, q);
    }

    public int getLeaguesCount(Franchise franchise) {
        return getCount(League.class, Restrictions.eq("franchiseId", franchise.getId()));
    }

    public List<Team> getTeams(User user, Search<TeamSearchKey> search) {
        return getList(search, Team.class, "where userId='" + user.getId() + "'");
    }

    public User getUser(int id) {
        return getById(User.class, id);
    }

    public User getUser(String userName, String passWord) {
        
        Session session = getSession();
        try {
            Query q = session.createQuery("select user from " + User.class.getName() + " user where USER_NAME='" + userName + "'");
            User user = (User)q.uniqueResult();
            if (user.getPassWord().equals(passWord))
                return user;
        } catch (Exception e) {
            e.printStackTrace();
            throw new FantasyException(ErrorCode.USERNAME_NOT_FOUND, userName);
        } finally {
            session.close();
        } 
        
        throw new FantasyException(ErrorCode.PASSWORD_NOT_VALID_FOR_USER, userName);
    }

    public int getUserCount() {
        return getCount(User.class);
    }

    public List<User> getUsers(Search<UserSearchKey> search) {
        return getList(search, User.class, "");
    }

    public void addOrUpdateTeam(Team team) {
        getHibernateTemplate().saveOrUpdate(team);
    }

    public void addOrUpdateTeams(List<Team> teams) {
        getHibernateTemplate().saveOrUpdateAll(teams);
    }

    public Team getLeagueLeader(int leagueId) {
        // TODO Auto-generated method stub
        return null;
    }

    public Team getTeam(int id) {
        return getById(Team.class, id);
    }

    public int getTeamRank(Team team) {
        // TODO Auto-generated method stub
        return 0;
    }

    public List<Team> getTeamsByLeague(int leagueId, Search<TeamSearchKey> search) {
        return getList(search, Team.class, "where leagueId='" + leagueId + "'");
    }

    public int getTeamsCount(League league) {
        return getCount(League.class);
    }

    public int getTeamsCount(User user) {
        return getCount(Team.class, Restrictions.eq("userId", user.getId()));
    }

    
}
