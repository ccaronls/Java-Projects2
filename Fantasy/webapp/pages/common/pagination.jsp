<%
StringBuffer display = new StringBuffer();

try {

	// current list item index to start listing from
	int pageOffset = Integer.parseInt(session.getAttribute("pageOffset"));
	// records per page to display
	int pageCount = Integer.parseInt(session.getAttribute("pageCount"));
	// total records for this list
	int pageTotal = Integer.parseInt(session.getAttribute("pageTotal"));
	// action to call (e.g. getMyList.do)
	String href = actionConfig.path();

	if (pageOffset == 0) {
		// display a non link indicating we are at the beginning of the list
		display.append("Start &nbsp;|");
	} else {
		// diaplay a link to goto the previous page
		display.append("&lt; Prev &nbsp;|");
	}
	
	// figure out how many pages total there are
	int numPages = pageTotal / pageCount + 1;
	
	// figure out what page we are on
	int currentPage = pageOffset / pageCount + 1;
	
	int numDisplayed = 0; 
	for (int i=currentPage; i<numPages; i++) {
	
		
	
		if (numDisplayed++ > 4)
			break; // dont show more than 4 page links
	}
	
	
} catch (Exception e) {
}

%>
