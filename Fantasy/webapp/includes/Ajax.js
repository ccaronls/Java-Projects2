Ajax = new function() {

    /*
    this.updateFrameImage = function(sel, prop)
    {
        var url = jsprops.get("image.update.frame");
        var args = prop + "=" + sel[sel.selectedIndex].value;
        //url = url + "?" + prop + "=" + sel[sel.selectedIndex].value;
        var req = AjaxEngine.getXmlHttpRequest();
        var callback = Ajax.updateFrameImageCallback;
        if (prop == 'frame2')
            callback = Ajax.updateFrame2ImageCallback;
        else if (prop == 'brush')
            callback = Ajax.updateStrokesImageCallback;
        AjaxEngine.postRequest(url, req, args, callback);
    }
    
    this.updateFrameImageCallback = function()
    {
        var images = document.getElementsByTagName("img");
        var sel = document.forms[2].frame;
        var image = sel[sel.selectedIndex].value;
        images[1].src = "images/mediaGallery/frames/" + image;
        images[1].alt = image;
    }
    
    this.updateFrame2ImageCallback = function()
    {
        var images = document.getElementsByTagName("img");
        var sel = document.forms[2].frame2;
        var image = sel[sel.selectedIndex].value;
        images[2].src = "images/mediaGallery/frames/" + image;
        images[2].alt = image;
    }
    
    this.updateStrokesImageCallback = function()
    {
        var images = document.getElementsByTagName("img");
        var sel = document.forms[2].stroke;
        var image = sel[sel.selectedIndex].value;
        images[1].src = "images/mediaGallery/strokes/" + image;
        images[1].alt = image;
    }
*/
    this.submitTeamRank_cb = function() {
        goURL('team.ranks.save.done.url');
        showMessage("Ranks Saved");
    }

    this.submitTeamRank = function(numPlayers) {
        var url = jsprops.get('team.rank.submit.url');
        var req = AjaxEngine.getXmlHttpRequest();
        
        var reqXml = "<list>\n";
        for (i=1; i<= numPlayers; i++) {
            var idElem   = document.getElementById("id" + i);
            reqXml += " <int>" + idElem.value + "</int>\n";
        }
        reqXml += "</list>\n";
        AjaxEngine.postXmlRequest(url, req, reqXml, Ajax.submitTeamRank_cb);
    }
    
    this.submitLeaguePositions_cb = function() {
    	goURL('league.position.stats.done.url');
    }

    this.saveLeaguePositions = function(position, numStats) {
        var url = jsprops.get('league.position.stats.submit.url');
        var args = "position=" + position;
        var req = AjaxEngine.getXmlHttpRequest();
        
        var xml = "<stats>\n";
        for (i=0; i<numStats; i++) {
            var elem = document.getElementById(i);
            var textId = elem.innerHTML;
            var textElem = document.getElementById(textId);
            var mult = textElem.value;
            xml += " <LeagueStat>\n";
            xml += "  <name>" + textId + "</name>\n";
            xml += "  <multiplier>" + mult + "</multiplier>\n";
            xml += " </LeagueStat>\n";
        }
        xml += "</stats>";
        AjaxEngine.postXmlRequest(url + "?" + args, req, xml, Ajax.submitLeaguePositions_cb);
    }

    // Start: Change user active status
    /*
    this.activated = false;
    this.idActivated = -1;
    this.setUserActive_cb = function() {    	
    	var elem = document.getElementById("item"   + this.idActivated);    	
    	if (this.activated) {
    		showMessage(jsprops.get('message.user.activated'));
    		elem.innerHTML = jsprops.get('button.deactivate');
    	} else {
    		showMessage(jsprops.get('message.user.deactivated'));
    		elem.innerHTML = jsprops.get('button.activate');
    	}
    	document.getElementById("active" + this.idActivated).innerHTML = this.activated;
    }
    this.setUserActive = function(id) {
    	var activate = document.getElementById("active" + id);
    	this.activated = !activate;
    	this.idActivated = id;
    	var args="userId=" + id + "&activate=" + this.activated;
    	var url =jsprops.get('user.setactive.url');
    	var req = AjaxEngine.getXmlHttpRequest();
    	AjaxEngine.postRequest(url, req, args, Ajax.setUserActive_cb);
    }
    */
    // End: Change user active status


}