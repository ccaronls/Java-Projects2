package cc.fantasy.struts.util;

public class Pagination {

	private String listURL;
	private int offset; // current list item shown
	private int max; // max to shown per page
	private int total; // total list items
	
	public Pagination(String listURL, int offset, int max, int total) {
		this.listURL = listURL;
		this.offset = offset;
		this.max = max;
		this.total = total;
	}
	
	public String generateHeader(String key) {
		return "<a href=\"" + listURL + "?offset=" + offset + "&key=" + key + "\">" + key + "</a>";
	}
	
	/**
	 * Generate the 
	 * @return
	 */
	public String generatePagination() {
		StringBuffer buffer = new StringBuffer();

		// compute the number of pages displayable
		int numPages = total / max;
		if (total % max != 0)
			numPages++;

		int index = 0;
		
		// for each page, generate a link
		for (int i=0; i<numPages; i++) {
			if (i > 0) {
				buffer.append(" | ");
			}
			if (index == offset) {
				// not a link since we are on this page
				buffer.append(String.valueOf(i+1)).append("\n");
			} else {
				buffer.append("<href=\"")
					.append(listURL)
					.append("?offset=")
					.append(String.valueOf(index))
					.append(">")
					.append(String.valueOf(i+1))
					.append("</a>\n");
			}
			
			index += max;
		}
		
		return buffer.toString();
	}
	
}
