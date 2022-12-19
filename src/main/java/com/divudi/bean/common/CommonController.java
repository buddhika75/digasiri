/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.divudi.bean.common;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;

/**
 *
 * @author buddhika
 */
@Named(value = "commonController")
@SessionScoped
public class CommonController implements Serializable {

    @Inject
    private SessionController sessionController;

    /**
     * Creates a new instance of CommonController
     */
    public CommonController() {
    }

    public Date getCurrentDateTime() {
        return new Date();
    }

    public boolean sameDate(Date date1, Date date2) {
        Calendar d1 = Calendar.getInstance();
        d1.setTime(date1);
        DateTime first = new DateTime(date1);
        DateTime second = new DateTime(date2);
        LocalDate firstDate = first.toLocalDate();
        LocalDate secondDate = second.toLocalDate();
        return firstDate.equals(secondDate);
    }
    
    public Date retiermentDate(Date dob) {
        if (dob==null) {
            dob=new Date();
        }
        Calendar cal = Calendar.getInstance();
        cal.setTime(dob);
        cal.add(Calendar.YEAR, 50);
        // System.out.println("cal = " + cal.getTime());
        return cal.getTime();
    }

    public double dateDifferenceInMinutes(Date fromDate, Date toDate) {
        long timeInMs = toDate.getTime() - fromDate.getTime();
        return timeInMs / (1000 * 60);
    }

    public void printReportDetails(Date fromDate, Date toDate, Date startTime, String url) {

        String s;
        s = "***************";
        s += "\n Report User :" + getSessionController().getLoggedUser().getWebUserPerson().getName();
        s += "\n Report Description : " + url;
        if (fromDate != null) {
            s += "\n Report Form :" + fromDate;
        }
        if (toDate != null) {
            s += " To :" + toDate;
        }
        s += "\n Report Start Time : " + startTime + " End Time :" + new Date();
        s += "\n ***************";

        System.err.println(s);

    }
    //----------Date Time Formats
    public String getDateFormat(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd");
        s=d.format(date);
        return s;
    }
    
    public String getDateFormat2(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("YYYY-MMM-dd");
        s=d.format(date);
        return s;
    }
    
    public String getTimeFormat12(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("hh:mm:ss a");
        s=d.format(date);
        return s;
    }
    
    public String getTimeFormat24(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("HH:mm:ss");
        s=d.format(date);
        return s;
    }
    
    public String getDateTimeFormat12(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd hh:mm:ss a");
        s=d.format(date);
        return s;
    }
    
    public String getDateTimeFormat24(Date date){
        String s="";
        DateFormat d = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
        s=d.format(date);
        return s;
    }
    public Date getConvertDateTimeFormat24(String dateString) throws ParseException{
        DateFormat d = new SimpleDateFormat("dd-MM-yyyy");
        Date date=d.parse(dateString);
        // System.out.println("date = " + date);
        // System.out.println("dateString = " + dateString);
        return date;
    }
    
    public String getDouble(double d){
        String s="";
        NumberFormat myFormatter = new DecimalFormat("##0.00");
        s=myFormatter.format(d);
        // System.out.println("s = " + s);
        return s;
    }
    
    //----------Date Time Formats

    public SessionController getSessionController() {
        return sessionController;
    }

    public void setSessionController(SessionController sessionController) {
        this.sessionController = sessionController;
    }

    public void sendEmail1(String messageHeading, String messageBody) {
        sendEmail1("arogya.tangalle@gmail.com", messageHeading, messageBody, "lakmedipro@gmail.com", "Bud7NilG");
    }
    
    public void sendEmail1(String toEmail, String messageHeading, String messageBody) {
        sendEmail1(toEmail, messageHeading, messageBody, "lakmedipro@gmail.com", "Bud7NilG");
    }
    
    public void sendEmail1(String toEmail, String messageHeading, String messageBody, String fromUserName, String fromPassword) {
        final String username = fromUserName;
        final String password = fromPassword;
        Properties props = new Properties();
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        Session session = Session.getInstance(props,
                new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromUserName));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(toEmail));
            message.setSubject(messageHeading);
            message.setText(messageBody);
//            BodyPart msbp1 = new MimeBodyPart();
//            msbp1.setText("Final Lab report of patient");
            
//            MimeBodyPart msbp2 = new MimeBodyPart();
//            DataSource source = new FileDataSource("LabReport.pdf");
//            msbp2.setDataHandler(new DataHandler(source));
//            msbp2.setFileName("/Labreport.pdf");

//            Multipart multipart = new MimeMultipart();
//            multipart.addBodyPart(msbp1);
//            multipart.addBodyPart(msbp2);

//            message.setContent(multipart);

            Transport.send(message);

            // System.out.println("Send Successfully");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }

    }

    
}
