package app;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

public class MailClass{
	public static final String from="msgtodineshbabus@gmail.com";
	public static final String pass="wigrjwajzszhyxzs";
	public static void sendMail(String to,String sub,String body) throws MessagingException{
		Properties props=new Properties();
		props.put("mail.smtp.auth","true");
		props.put("mail.smtp.starttls.enable","true");
		props.put("mail.smtp.host","smtp.gmail.com");
		props.put("mail.smtp.port","587");
		
		Session session=Session.getInstance(props, new Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(from, pass);
			}
		});
		session.setDebug(true);
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));
		message.setSubject(sub);
		message.setText(body);
		Transport.send(message);
	}
}