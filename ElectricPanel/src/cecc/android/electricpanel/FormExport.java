package cecc.android.electricpanel;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import cc.lib.android.EmailHelper;
import cc.lib.utils.HtmlUtils;
import cecc.android.lib.CECCBaseActivity;
import cecc.android.lib.PagedFormExporter;
import android.app.Activity;
import android.net.Uri;
import android.text.Html;
import android.util.Log;

public class FormExport extends PagedFormExporter {
	
	private final Form form;
	private final SimpleDateFormat fmt = new SimpleDateFormat("EEEE MMMM d, yyyy", Locale.getDefault());
	private boolean hasCommentsPage = false;
	
	FormExport(BaseActivity activity, Form form, Signature [] signatures) {
		super(activity, signatures);
		this.form = form;
		int numPages = 1;
		if (form.comments != null && form.comments.length() > 0) {
			hasCommentsPage=true;
			numPages++;
		}
		if (signatures.length > 0) {
			numPages += (signatures.length-1) / 3 + 1;
		}
		setNumPages(numPages);
	}
	
	private void header() {
		header("Electrical Panel Certification", fmt.format(form.editDate));
		beginTable(0);
		entry("Cusomter:", form.customer);
		entry("Project:", form.project);
		entry("Location:", form.location);
		entry("Representative:", form.representative);
		entry("Plan:", form.plan);
		entry("Type:", form.type);
		endTable();
		html.append("<br/><table width=\"100%\"><tr>");
		if (form.passed) {
			html.append("<td><font color=\"green\">").append("Passed").append("</font></td>\n");			
		} else {
			html.append("<td><font color=\"red\">").append("FAILED").append("</font></td>\n");
		}
		if (form.torqued) {
			html.append("<td><font color=\"green\">").append("Torqued to Specification").append("</font></td>\n");
		} else {
			html.append("<td><font color=\"red\">").append("Not Torqued").append("</font></td>\n");
		}
		html.append("</tr></table>\n");
	}
	
	@Override
	protected String getFooter() {
		return ("Generated by Electric Panel Utility - ceccsolutions.com");
	}
	
	private void commentsPage() {
		start();
		header();
		entry("Comments", "");
		html.append("<br/><h3>Comments</h3>\n").append(form.comments).append("\n");
		end();
	}
	
	private void mainPage() {
		start();
		header();
		html.append("<br/><table width=\"100%\">\n");
		html.append("<tr>\n");
		final int dim = Math.round(CECCBaseActivity.IMAGE_CAPURTE_DIM / getActivity().getResources().getDisplayMetrics().density);

		for (int i=0; i<2; i++) {
			html.append("<tr>");
    		for (int ii=0; ii<3; ii++) {
    			html.append("<td>\n");
    			int index = (i*3) + ii;
    			Image image = form.getImageForIndex(index);
    			if (image != null) {
    				Uri uri = Uri.fromFile(new File(((BaseActivity)getActivity()).getImagesPath(), image.path));
    				html.append(String.format("<img width=\"%d\" height=\"%d\" src=\"", dim, dim)).append(uri.toString()).append("\">\n");
    			} else {
    				html.append(String.format("<img width=\"%d\" height=\"%d\" src=\"\" alt=\"No Image\"/>\n", dim, dim));
    			}
    			html.append("</td>\n");
    		}
    		html.append("</tr><tr>\n");
    		for (int ii=0; ii<3; ii++) {
    			html.append("<td>\n");
    			int index = (i*3) + ii;
    			Image image = form.getImageForIndex(index);
    			if (image != null && image.data != null) {
    				html.append(HtmlUtils.wrapTextForTD(Html.fromHtml(image.data).toString(), 30));
    			}    			
    			html.append("</td>\n");
    		}
    		html.append("</tr>\n");
		}
		html.append("</table>\n");
		end();
		Log.d("HTML Page 1", html.toString());
	}

	private void signaturesPage(Signature [] signatures, int offset, int page) {
		start();
		header();
		int count = 0;
		for (int i=offset; i<signatures.length; i++) {
			if (count ++ == 3)
				break;
			Signature s = signatures[i];
			html.append("</br><table width=\"100%\"><tr>\n");
			html.append("<td width=\"50%\">");
			html.append(s.fullName == null || s.fullName.isEmpty() ? "Not Specified" : s.fullName).append("</td>\n");
			html.append("<td align=\"right\" width=\"50%\">").append(fmt.format(s.date)).append("</td>\n");
			html.append("</tr></table>");
//			html.append("</br>").append(s.fullName).append("     ").append(fmt.format(s.date));
			Uri uri = Uri.fromFile(s.signatureFile);
			int imgWid = Math.round((float)getWidth() / getActivity().getResources().getDisplayMetrics().density);
			html.append(String.format("</br><img width=\"%d\" src=\"", imgWid)).append(uri.toString()).append("\">\n");
		}
		end();
		Log.d("HTML Page " + page, html.toString());
	}

	@Override
	protected void onEmailAttachmentReady(File attachment) {
		Activity activity = getActivity();
		EmailHelper.sendEmail(activity, attachment, BuildConfig.DEBUG ? "ccaronls@yahoo.com" : null, activity.getString(R.string.emailSubjectSignedReport), activity.getString(R.string.email_body_signed_form));		
	}

	@Override
	protected void genPage(int pageNum, Signature[] signatures) {
		if (pageNum == 0) {
			mainPage();
		} else if (pageNum == 1 && hasCommentsPage) {
			commentsPage();
		} else {
			signaturesPage(signatures, (pageNum-(hasCommentsPage ? 2 : 1))*3, pageNum);
		}
	}
	
	
	@Override
	protected void onError(Exception e) {
		((CECCBaseActivity)getActivity()).newDialogBuilder().setTitle(R.string.popup_title_error)
			.setMessage(getActivity().getString(R.string.generate_page_error_msg) + "\n\nERROR:" + e.getMessage()).show();
	}
	
	
	
}
