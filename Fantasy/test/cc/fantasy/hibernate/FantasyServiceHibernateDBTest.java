package cc.fantasy.hibernate;

import cc.fantasy.service.FantasyContext;
import cc.fantasy.service.FantasyServiceTest;
import cc.fantasy.service.IFantasyService;

public class FantasyServiceHibernateDBTest extends FantasyServiceTest {

    @Override
    protected IFantasyService getService() {
        return FantasyContext.getService();
    }
    
    
}
