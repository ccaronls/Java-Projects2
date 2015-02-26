AjaxEngine = new function(){
    
    /**
     * Post request to the server
     *  @param url                  - target relative url
     *  @param req                  - instance of XMLHttpRequest object to be used
     *  @param args                 - list of url-format name/value pairs
     *  @param handlerFunctionName  - name of the call-back function, that will be envoked once request is done
     */
    
    this.getXmlHttpRequest = function(){
        
        if (window.XMLHttpRequest){
            req = new XMLHttpRequest();
        }else if (window.ActiveXObject){
            req = new ActiveXObject("Microsoft.XMLHTTP");
        }else{
            alert("AJAX Not supported");
            return null;
        }
        return req;
    }
    
    this.postRequest = function(url, req, args, handlerFunctionName) {
        
        try{
            req.onreadystatechange = function(){AjaxEngine.handleResponse(req, handlerFunctionName);};
            req.open("POST", url, true);
            req.setRequestHeader('Content-Type', 'application/x-www-form-urlencoded');
            req.send(args);
        }
        catch(e){
            debug("postRequest Failed : " + e);
        }
    }
    
    this.postXmlRequest = function(url, req, xmlBody, handlerFunctionName) {
        try {
            req.onreadystatechange = function(){AjaxEngine.handleResponse(req, handlerFunctionName);};
            req.open("POST", url, true);
            req.setRequestHeader('Content-Type', 'text/xml');
            req.send("<?xml version='1.0' encoding='UTF-8'?>\n" + xmlBody);
        } catch (e) {
            debug("postRequest Failed : " + e);
        }
    }
    
    /**
     * Waits until request has posted, and envokes the callback function
     *  @param req                  - instance of XMLHttpRequest object to be used
     *  @param handlerFunctionName  - name of the call-back function
     */
    this.handleResponse = function(req, handlerFunctionName) {
        if (req.readyState == 4) {
            if (req.status == 200){
                temp = new Object()
                temp.doit = handlerFunctionName;
                try{
                    temp.doit();
                } catch (e){
                    debug("Exception caught : " + e);
                }
            }
            else
                alert("There was a problem retrieving the XML data:\n" + req.statusText);
        }
    }
}