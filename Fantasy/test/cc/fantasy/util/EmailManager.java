package cc.fantasy.util;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;

public class EmailManager {
    
    private final Log log = LogFactory.getLog(getClass());
    
    private JavaMailSenderImpl      mailSender;    
    private MimeMessage message;
    private MimeMessageHelper mimeMessageHelper;
    private String[] ccTo;   
    private String[] to;
    private String from;
    private String[] bccTo;
    private String   subject;
    private String   text;
    private HashMap  idFileMap;
    private boolean  html;
    private boolean  isMultipart;
    private boolean  inline;

    /**
     * 
     * @param ccTo
     */
    public EmailManager(String[] ccTo) {
        this.ccTo = ccTo;
    }
    
    /**
     * 
     * 
     */
    public EmailManager() {        
    }
    
    /**
     *
     * @param to
     * @param text
     * @param subject
     *
     * This method sends mail to the to adress specifid along with given text and subject.
     */
    public void sendEmail(String[] to, String text, String subject){
        this.to             = to;
        this.text           = text;
        this.subject        = subject;
        this.sendEmail();
    }       
    
    /**
     * This method sends email with the specified parameters.
     *
     */
    public void sendEmail(){
        try {
            message = mailSender.createMimeMessage();
            mimeMessageHelper = new MimeMessageHelper(message, isMultipart());
            setTo();
            setFrom();
            setBccTo();
            setccTo();
            setSubject();            
            setText();
            setInlineResources();
            mailSender.send(message);
            
        } catch (MessagingException e) {
            log.error(e.getMessage());
        }       
    }
    
    /**
     * 
     * @throws MessagingException
     */
    private void setInlineResources() throws MessagingException{
        if(isInline() && (idFileMap != null) && (!idFileMap.isEmpty())){
            FileSystemResource res = null;
            for(Iterator i = idFileMap.entrySet().iterator(); i.hasNext();){
                Entry entry = (Entry)i.next();
                res = new FileSystemResource(new File(entry.getValue().toString())); 
                mimeMessageHelper.addInline(entry.getKey().toString(), res);
            }
        }
    }  
    
    /**
     * 
     * @throws MessagingException
     */
    private void setTo() throws MessagingException{
        if((this.to != null) && (this.to.length > 0))
            mimeMessageHelper.setTo(this.to);
    }
    
    /**
     *
     * @throws MessagingException
     */
    private void setFrom() throws MessagingException{
        if((this.from != null) && (this.from.length() > 0))
            mimeMessageHelper.setFrom(this.from);
    }
    
    /**
     * 
     * @throws MessagingException
     */
    private void setBccTo() throws MessagingException{
        if((this.bccTo != null) && (this.bccTo.length > 0))
            mimeMessageHelper.setBcc(this.bccTo);
    }
    
    /**
     * 
     * @throws MessagingException
     */
    private void setccTo() throws MessagingException{
        if((this.ccTo != null) && (this.ccTo.length > 0))
            mimeMessageHelper.setCc(this.ccTo);
    }
    
    /**
     * 
     * @throws MessagingException
     */
    private void setSubject() throws MessagingException{
        if((this.subject != null) && (this.subject.length() > 0))
            mimeMessageHelper.setSubject(this.subject);
    }
    
    /**
     * 
     * @throws MessagingException
     */
    private void setText() throws MessagingException{
        if((this.text != null) && (this.text.length() > 0))
            mimeMessageHelper.setText(text, isHtml());
    }
    
    /**
     * 
     * @return
     */
    public JavaMailSenderImpl getMailSender() {
        return mailSender;
    }
    /**
     * 
     * @param mailSender
     */
    public void setMailSender(JavaMailSenderImpl mailSender) {
        this.mailSender = mailSender;
    }   
    
    /**
     * 
     * @return
     */
    public String[] getCcTo() {
        return ccTo;
    }
    
    /**
     * 
     * @param ccTo
     */
    public void setCcTo(String[] ccTo) {
        this.ccTo = ccTo;
        log.debug("CCTO: " + ccTo.toString());
    }
    
    /**
     * 
     * @return
     */
    public boolean isInline() {
        return inline;
    }
    /**
     * 
     * @param inline
     */
    public void setInline(boolean inline) {
        this.inline = inline;
    }
    
    /**
     * 
     * @return
     */
    public HashMap getIdFileMap() {
        return idFileMap;
    }
    
    /**
     * 
     * @param idFileMap
     */
    public void setIdFileMap(HashMap idFileMap) {
        this.idFileMap = idFileMap;
    }
    /**
     * 
     * @return
     */
    public String[] getBccTo() {
        return bccTo;
    }
    
    /**
     * 
     * @param bccTo
     */
    public void setBccTo(String[] bccTo) {
        this.bccTo = bccTo;
    }
    /**
     * 
     * @return
     */
    public String[] getTo() {
        return to;
    }
    
    /**
     * 
     * @param to
     */
    public void setTo(String[] to) {
        this.to = to;
    }
    
    /**
     *
     * @return
     */
    public String getFrom() {
        return from;
    }
    
    /**
     *
     * @param from
     */
    public void setFrom(String from) {
        this.from = from;
    }
    
    /**
     * 
     * @return
     */
    public String getSubject() {
        return subject;
    }
    
    /**
     * 
     * @param subject
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    /**
     * 
     * @return
     */
    public boolean isMultipart() {
        return isMultipart;
    }
    
    /**
     * 
     * @param isMultipart
     */
    public void setMultipart(boolean isMultipart) {
        this.isMultipart = isMultipart;
    }
    
    /**
     * 
     * @return
     */
    public boolean isHtml() {
        return html;
    }
    
    /**
     * 
     * @param html
     */
    public void setHtml(boolean html) {
        this.html = html;
    }
    
    /**
     * 
     * @return
     */
    public String getText() {
        return text;
    }
    
    /**
     * 
     * @param text
     */
    public void setText(String text) {
        this.text = text;
    }
    
    
    
}
