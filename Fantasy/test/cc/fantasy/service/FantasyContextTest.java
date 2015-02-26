package cc.fantasy.service;

import java.util.List;

import org.apache.log4j.Category;

import junit.framework.TestCase;

public class FantasyContextTest extends TestCase {

    private Category log = Category.getInstance(getClass());

    public void test() {
        IFantasyService service = FantasyContext.getService();
        
        Search search = new Search();
        search.setMax(1);
        log.info(service.getAllFranchiseCategories());
        List list = service.getFranchises(search);
        assertEquals(list.size(), 1);
        log.info(list);
        list = service.getLeagues(search);
        assertEquals(list.size(), 1);
        log.info(list);
        list = service.getUsers(search);
        assertEquals(list.size(), 1);
        log.info(list);
        list = service.getTeams(null, search);
        assertEquals(list.size(), 1);
        log.info(list);
        
    }
    
}
