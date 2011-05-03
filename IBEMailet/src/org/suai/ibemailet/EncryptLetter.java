/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.suai.ibemailet;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMessage;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;
import org.apache.mailet.RFC2822Headers;
import org.suai.idbased.Client;
import org.suai.idbased.PKG;
import org.suai.idbased.Util;
import org.apache.james.security.InitJCE;
import org.suai.idbased.Cryptocontainer;

/**
 *
 * @author foxneig
 */
public class EncryptLetter extends GenericMailet {
    private MailetConfig config;
    private String mpk_path;
    private String msk1_path;
    private String msk2_path;
    private PKG pkg;
    private Client client;


    @Override
    public void destroy () {
        System.out.println ("Destroy");

        }

    @Override
    public String getMailetInfo() {
        return "IdBasedEncrypt Mailet";
    }

    @Override
    public MailetConfig getMailetConfig() {
        return config;
    }

    @Override
    public void init(MailetConfig config) throws MessagingException {
        
            System.out.println("Init IdBasedEncryptMailet");
    
              
           

            

            
            
            super.init(config);
            MailetContext context = config.getMailetContext();
            mpk_path = getInitParameter("mpkPath");
            msk1_path = getInitParameter("msk1Path");
            msk2_path = getInitParameter("msk2Path");
            pkg = new PKG();
        try {
            pkg.MPK = Util.readKeyData(new FileInputStream(mpk_path));
        } catch (IOException ex) {
            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            pkg.P = Util.readKeyData(new FileInputStream(msk1_path));
        } catch (IOException ex) {
            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            pkg.Q = Util.readKeyData(new FileInputStream(msk2_path));
        } catch (IOException ex) {
            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
        }
            pkg.getSecretExponent();
       




    }

    @Override
    public void service(Mail mail) throws MessagingException {
        client = new Client();
        byte [] encrypted = null;
        ByteArrayInputStream is = null;
        MimeMessage message = mail.getMessage();
        String contentType = message.getContentType();
        System.out.println (contentType);
        MailAddress from = mail.getSender();
        Collection to = mail.getRecipients();
        Iterator<MailAddress> iterator = to.iterator();
        String recip = iterator.next().toString();
        String sender = from.toString();
        System.out.println ("E-mail FROM: " + sender);
        System.out.println ("E-mail TO: "+recip);
        if (message.isMimeType("text/plain")) {
            try {
                String text = (String) message.getContent();
                System.out.print(text);
                is = new ByteArrayInputStream(text.getBytes());
            } catch (IOException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            }
            
                System.out.println ("Encrypt mail body...");
            try {
                encrypted = client.encryptData(is, client.genPkID(recip, pkg.MPK), pkg.MPK, pkg.signKeyExtract(sender), pkg.e);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NoSuchPaddingException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InvalidKeyException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IOException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalBlockSizeException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            } catch (BadPaddingException ex) {
                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
            }
                System.out.println ("Done");
             //   String ciphertext = new String (encrypted);
              //  System.out.println (ciphertext);

                //message.setContent(new String(encrypted), contentType);
                message.setText(new String (encrypted));
                //message.setHeader(RFC2822Headers.CONTENT_TYPE, contentType);
                message.saveChanges();


        }
        else if (message.isMimeType("multipart/mixed") ||message.isMimeType("multipart/related")  ) {

            try {
                // здесь надо сохранить аттачи
                Multipart mp = (Multipart) message.getContent();

                System.out.println ("PartsNum: "+mp.getCount());

                for (int i = 0, n = mp.getCount(); i < n; i++) {
                    Part part = mp.getBodyPart(i);

                    if (part.isMimeType("text/plain")) {
                     System.out.println ("Try to encrypt text");
                        try {

                            encrypted = client.encryptData(part.getInputStream(), client.genPkID(recip, pkg.MPK), pkg.MPK, pkg.signKeyExtract(sender), pkg.e);
                        } catch (NoSuchAlgorithmException ex) {
                            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (NoSuchPaddingException ex) {
                            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (InvalidKeyException ex) {
                            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalBlockSizeException ex) {
                            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BadPaddingException ex) {
                            Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                        }
                     part.setContent(new String(encrypted), part.getContentType());
                     boolean removeBodyPart = mp.removeBodyPart((BodyPart) part);
                     System.out.println ("Removed: "+removeBodyPart);
                     mp.addBodyPart((BodyPart) part,i);
                     message.setContent(mp);
//
;

                    }
                    else {

                    String disposition = part.getDisposition();
                    System.out.println ("Disposition "+disposition);
                    if ((disposition != null) && ((disposition.equals(Part.ATTACHMENT) || (disposition.equals(Part.INLINE))))) {

                        System.out.println ("Try to encrypt attache");
                            try {
                                try {
                                    encrypted = client.encryptData(part.getInputStream(), client.genPkID(recip, pkg.MPK), pkg.MPK, pkg.signKeyExtract(sender), pkg.e);
                                } catch (NoSuchPaddingException ex) {
                                    Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (InvalidKeyException ex) {
                                    Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (IllegalBlockSizeException ex) {
                                    Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                                } catch (BadPaddingException ex) {
                                    Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            } catch (NoSuchAlgorithmException ex) {
                                Logger.getLogger(EncryptLetter.class.getName()).log(Level.SEVERE, null, ex);
                            }
                        part.setContent(encrypted, part.getContentType());
                        part.setFileName("encrypted"+i);
                        boolean removeBodyPart = mp.removeBodyPart((BodyPart) part);
                        System.out.println ("Removed: "+removeBodyPart);
                        mp.addBodyPart((BodyPart) part,i);

                        message.setContent(mp);
                        




                     System.out.println ("Attache is encrypted");



                    }
                    }
                }
            } catch (IOException ex) {
               log("Cannot to get attaches");
            }
        }
      //  message.setHeader(RFC2822Headers.CONTENT_TYPE, contentType);
        message.saveChanges();
        System.out.println ("Ended");






    }


}