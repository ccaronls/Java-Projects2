package cc.fantasy.struts.action;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import cc.fantasy.model.Franchise;
import cc.fantasy.model.Position;
import cc.fantasy.model.Stat;
import cc.fantasy.service.Search;
import cc.fantasy.struts.form.FranchiseForm;
import cc.fantasy.update.FranchiseUpdate;

public class FranchiseAction extends BaseAction {

    public ActionForward searchFranchisesAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        FranchiseForm fForm = (FranchiseForm)form;        
        Search search = getSearch(request, "franchiseSearch");        
        fForm.setFranchises(getService().getFranchises(search));        
        return mapping.findForward(SUCCESS);
    }
    
    public ActionForward editFranchiseAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

        log.info("editFranchiseAction");
        
        FranchiseForm fForm = (FranchiseForm)form;        
    	String franchiseId = request.getParameter("franchiseId");
        fForm.setCategories(getService().getAllFranchiseCategories());

    	if (franchiseId != null) {

    		int id = Integer.parseInt(franchiseId);
            log.debug("Fetching franchise '" + id + "'");
    		Franchise f = getService().getFranchise(id);
    		fForm.setCategory(f.getCategory());
    		fForm.setName(f.getName());
    		
    		fForm.setPlayers(new ArrayList(f.getPlayers()));
    		fForm.setPositions(new ArrayList(f.getPositions()));
    		
            return mapping.findForward("edit");
            
    	} else {
            log.info("creating new franchise");
    		fForm.setFranchiseId(-1);

            return mapping.findForward("new");
    	}
    	
    }


    public ActionForward editFranchisePlayerAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        log.debug("editFranchisePlayerAction");
    	
    	
    	
        return mapping.findForward(SUCCESS);
    }

    public ActionForward deletePositionStatAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        log.debug("deletePositionStatAction");
        
        FranchiseForm fForm = (FranchiseForm)form;        
        String statName = request.getParameter("stat");

        Franchise franchise = getService().getFranchise(fForm.getFranchiseId());
        Position position = franchise.getPosition(fForm.getPosition());
        Stat stat = position.getStat(statName);
        if (stat != null) {
            position.getStats().remove(stat);
            position.getOmitStats().add(statName);
            getService().addOrUpdateFranchise(franchise);
        }
        
        return mapping.findForward(SUCCESS);
    }
    
    
    public ActionForward saveFranchiseAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
    	
        log.debug("saveFranchiseAction");
        
        FranchiseForm fForm = (FranchiseForm)form;
        
        Franchise f = null;
        
        if (fForm.getFranchiseId() >= 0)
        	f = getService().getFranchise(fForm.getFranchiseId());
        else
        	f = new Franchise();
        
    	try {

    		f.setActive(fForm.getActive());
    		f.setName(fForm.getName());
    		f.setCategory(fForm.getCategory());
    		f.setPlayers(fForm.getPlayers());
    		f.setPositions(fForm.getPositions());
    		
    		getService().addOrUpdateFranchise(f);
            
            fForm.setFranchiseId(f.getId());
    		
    	} catch (Exception e) {
            setErrorMessage(request, e);           
    	}
    	
    	return mapping.findForward(SUCCESS);
    }
    
    public ActionForward importFranchisePositionSetup(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        FranchiseForm fForm = (FranchiseForm)form;
        fForm.setFranchiseId(Integer.parseInt(request.getParameter("franchiseId")));
        
        String positionId = request.getParameter("position");
        if (positionId != null) {
            Position pos = getService().getFranchise(fForm.getFranchiseId()).getPosition(positionId);
            fForm.setPosition(positionId);
            fForm.setPosition(pos.getName());
            fForm.setPositionNameLong(pos.getLongName());
            fForm.setPlayerColumn(pos.getPlayerColumn());
            fForm.setStats(pos.getStats());
        } 
        
        return mapping.findForward(SUCCESS);
    }

    public ActionForward importFranchisePositionAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {

        log.debug("importFranchisePositionAction");
        
        FranchiseForm fForm = (FranchiseForm)form;
        
        try {
            Franchise franchise = getService().getFranchise(fForm.getFranchiseId());
            FranchiseUpdate update = new FranchiseUpdate(getService());
            
            Position position = new Position();
            position.setName(fForm.getPosition());
            position.setLongName(fForm.getPositionNameLong());
            log.debug("Loading from " + fForm.getSpreadSheetFile());
            position.setPlayerColumn(fForm.getPlayerColumn());
            
            position.setSpreadSheetFile(fForm.getSpreadSheetFile());
            update.importData(franchise, position, null);
            fForm.setStats(position.getStats());
            franchise.addPosition(position);

        } catch (Exception e) {
            this.setErrorMessage(request, e);
        }
        
        return mapping.findForward(SUCCESS);
    }

    /*
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    public ActionForward searchTeamsAction(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) {
        return mapping.findForward(SUCCESS);
    }
    */

}
