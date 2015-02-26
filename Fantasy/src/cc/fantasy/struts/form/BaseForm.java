package cc.fantasy.struts.form;

import java.util.Date;
import java.util.Locale;

import javax.servlet.ServletRequest;

import org.apache.struts.action.ActionMapping;
import org.apache.struts.validator.ValidatorForm;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.League;
import cc.fantasy.model.User;
import cc.fantasy.service.FantasyContext;
import cc.fantasy.service.IFantasyService;
import cc.fantasy.service.Search;
import cc.fantasy.struts.util.Common;

public class BaseForm extends ValidatorForm {

    // every list type has its own search (maybe more?)
    private Search search = new Search();

    // these are here to make <html:button property="unused"> happy
    public String getUnused() { 
    	return ""; 
    }
    
    public void setUnused(String unused) {
    }    
    
	public Search getSearch() {
		return search;
	}
	public void setSearch(Search search) {
		this.search = search;
	}
    
    public IFantasyService getService() {
    	return FantasyContext.getService();
    }
    
    public User getUser(int userId) {
    	return getService().getUser(userId);
    }
    
    public Franchise getFranchise(int franchiseId) {
    	return getService().getFranchise(franchiseId);
    }
    
    public League getLeague(int leagueId) {
    	return getService().getLeague(leagueId);
    }
    
    public void reset(ActionMapping mapping, ServletRequest request) {
        super.reset(mapping, request);
        this.request = request;
    }
    
    public String getDateString(Date date) {
    	Locale locale = Locale.ENGLISH;
    	if (request != null) {
    		locale = request.getLocale();
    	} else {
    		System.err.println("WARNING: Null request getting LOCALE, default to ENGLISH");
    	}
    	return Common.dateToString(date, locale);
    }
    
    private ServletRequest request;
    
}
