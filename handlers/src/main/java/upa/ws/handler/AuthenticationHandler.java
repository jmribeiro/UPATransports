package upa.ws.handler;

import java.util.*;

import javax.xml.namespace.*;
import javax.xml.soap.*;
import javax.xml.ws.handler.*;
import javax.xml.ws.handler.soap.*;

import javax.crypto.Cipher;

import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import javax.xml.transform.*;
import java.security.PrivateKey;
import java.security.PublicKey;

import java.io.*;

import javax.xml.ws.*;
import java.nio.charset.StandardCharsets;

import upa.ws.Crypto;

import pt.upa.ca.cli.*;
import pt.upa.ca.*;

//provides helper methods to print byte[]
import static javax.xml.bind.DatatypeConverter.printHexBinary;
import static javax.xml.bind.DatatypeConverter.parseHexBinary;

import java.security.cert.*;
import java.security.InvalidKeyException;

import org.w3c.dom.Element;

/**
 * This SOAPHandler outputs the contents of inbound and outbound messages.
 */

public class AuthenticationHandler implements SOAPHandler<SOAPMessageContext> {

    public static String name = null;
    private final static String CA_CERTIFICATE_FILE = "ca-certificate.pem.txt";
	
    public static void setName(String n){
        if(name == null){
            name = n;
        }
    }

    public Set<QName> getHeaders() {
        return null;
    }
    
    public boolean handleMessage(SOAPMessageContext smc) {
        return authenticate(smc);
    }

    public boolean handleFault(SOAPMessageContext smc) {
        return authenticate(smc);
    }

    // nothing to clean up
    public void close(MessageContext messageContext) {
    }

    private static boolean authenticate(SOAPMessageContext smc){
        
        try{
        	Boolean outbound = (Boolean)smc.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY);

            SOAPMessage msg = smc.getMessage();
            SOAPPart sp = msg.getSOAPPart();
            SOAPEnvelope se = sp.getEnvelope();

            if(outbound){
                sign(se);
                return true;             
            }else{

        		boolean valid = verify(se);

                if(valid){
                    System.out.println("\n\nMESSAGE FROM "+ parseEntity(se.getHeader()) +" IS AUTHENTIC\n\n");
                    return true;
                }else{
                    System.out.println("\n\nMESSAGE FROM "+ parseEntity(se.getHeader()) +" IS NOT AUTHENTIC\n\n");
                    return false;
                }
        	}

        }catch(Exception e){
            System.out.println("\n\nAuthenticator "+name+" failed to handle message\n\n");
            e.printStackTrace();
            return false;
        }
    }

    private static void sign(SOAPEnvelope envelope) throws SOAPException, Exception{
    	   
        System.out.println("\n\nAuthenticator: "+ name +" now signing an outbound message...\n\n");
        
        SOAPHeader header = envelope.getHeader();
        SOAPBody body = envelope.getBody();

        // 1 - Adicionar header com nome entidade
        addEntity(envelope);

        // 2 - Adicionar header com o digest do body
        addDigest(envelope);

    }

    private static boolean verify(SOAPEnvelope envelope) throws SOAPException{
        
        System.out.println("\n\nAuthenticator: "+ name +" now verifying an incoming message...\n\n");
        
        SOAPHeader header = envelope.getHeader();
        SOAPBody body = envelope.getBody();
        
        // 1 - Retirar elemento Entity e por em String
        String message = getSoapBodyString(body);

        String entityName = parseEntity(header);
        String recievedDigest = parseDigest(header);
        byte[] digestBytes = parseHexBinary(recievedDigest);

        //Header nem tinha os elementos
        if(entityName==null || recievedDigest == null){
            return false;
        }

        // 2 - Retirar a PublicKey
        PublicKey publicKey = getEntityPublicKey(entityName);
        
        // 3 - Verificar
        try{
            boolean var = Crypto.verifyDigitalSignature(digestBytes, message.getBytes(), publicKey);
            return var;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
      
    }
    /* 
        Método auxiliar para 
        adicionar a String correspondente à entidade emissora ao header
    */
    private static void addEntity(SOAPEnvelope envelope) throws SOAPException{
        addToHeader(envelope, "Entity", name, "namespace", "uri");
    }

    /* 
        Método auxiliar para 
        adicionar a String correspondente ao digest enviado pela entidade emissora ao header
    */
    private static void addDigest(SOAPEnvelope envelope) throws SOAPException, Exception{
        try{
            String soapBodyString = getSoapBodyString(envelope.getBody());
            byte[] soapBodyBytes = soapBodyString.getBytes();

            PrivateKey privateKey = getMyPrivateKey();

            byte[] digestBytes = Crypto.makeDigitalSignature(soapBodyBytes, privateKey);
            String digestString = printHexBinary(digestBytes);

            addToHeader(envelope, "Digest", digestString, "namespace", "uri");
        }catch(InvalidKeyException e){
            e.printStackTrace();
        }
    }
    
    /* 
        Método auxiliar para 
        adicionar um elemento ao header com um determinado valor
    */
    private static void addToHeader(SOAPEnvelope envelope, String elementName, String elementValue, String namespace, String uri) throws SOAPException{
        
        SOAPHeader sh = envelope.getHeader();
        
        if (sh == null)
            sh = envelope.addHeader();

        // add header element (name, namespace prefix, namespace)
        Name name = envelope.createName(elementName, namespace, uri);
        SOAPHeaderElement element = sh.addHeaderElement(name);
        
        element.addTextNode(elementValue);
    }

    /* 
        Método auxiliar para 
        devolver a chave privada da entidade que o chama (a partir da variável name)
    */
    private static PrivateKey getMyPrivateKey(){
        
        String CERTIFICATE_FILE = name+".cer";

        String KEYSTORE_FILE =  name+".jks";
        String KEYSTORE_PASSWORD = "password";

        String KEY_ALIAS = name;
        String KEY_PASSWORD = "password";

        try{
            PrivateKey privateKey = Crypto.getPrivateKeyFromKeystore(
                KEYSTORE_FILE,
                KEYSTORE_PASSWORD.toCharArray(),
                KEY_ALIAS,
                KEY_PASSWORD.toCharArray()
            );

            return privateKey;
        }catch(Exception e){
            System.out.println("\n\nAuthenticator: "+ name +" couldn't access the keystore...\nHave you checked if all the files are installed?\n\n");
            e.printStackTrace();
        }

        return null;
    }

    /* 
        Método auxiliar para 
        retirar a String correspondente à Entidade emissora
    */
    private static String parseEntity(SOAPHeader header){
        String entityName = getElementContent(header, "namespace:Entity");
        return entityName;
    }

    /* 
        Método auxiliar para 
        retirar a String correspondente ao Digest enviado pela entidade emissora
    */
    private static String parseDigest(SOAPHeader header){
        String digest = getElementContent(header, "namespace:Digest");
        return digest;
    }

    /* 
        Método auxiliar para 
        dado um header, 
        retirar a String correspondente ao conteúdo do elemento
    */
    public static String getElementContent(SOAPHeader header, String elementName){
        
        Iterator it = header.getChildElements();
        while (it.hasNext()) {
            Node node = (Node)it.next();
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element)node ;
                if(element.getNodeName().equals(elementName)){
                    return element.getTextContent();
                }
            }
        }
        return null;
    }

    /* 
        Método auxiliar para 
        devolver o conteúdo do SOAPBody em String
    */
    private static String getSoapBodyString(SOAPBody body){

        try{
            DOMSource source = new DOMSource(body);
            StringWriter stringResult = new StringWriter();
            TransformerFactory.newInstance().newTransformer().transform(source, new StreamResult(stringResult));
            return  stringResult.toString();
        }catch(Exception e){
            e.printStackTrace();
            return "";
        }
    }

    /* 
        Método auxiliar para 
        converter o Digest byte array em String (UTF8)
    */
    private static String getDigestString(byte[] digestBytes){
        return new String(digestBytes, StandardCharsets.UTF_8);
    }

    /* 
        Método auxiliar para 
        dado uma entidade,
        devolver a sua chave pública
    */
    private static PublicKey getEntityPublicKey(String entityName){

        PublicKey publicKey = null;
        
        CAClient certificateClient = new CAClient("http://localhost:9090", "CA");
        
        Certificate certificate = certificateClient.requestCertificate(entityName);
        
        boolean valid = validCertificate(certificate);
        
        if(certificate==null || !valid){
            return null;
        }else{
            return Crypto.getPublicKeyFromCertificate(certificate);
        }
    }

    /*
        Método auxiliar para 
        dado um certificado,
        dizer se é ou não valido segundo o certificado da CA
    */
    public static boolean validCertificate(Certificate cer){
        try{
            //Verificar se o certificado é valido
            Certificate caCertificate = Crypto.readCertificateFile(CA_CERTIFICATE_FILE);
            PublicKey caPublicKey = caCertificate.getPublicKey();
            return Crypto.verifySignedCertificate(cer, caPublicKey);
        }catch(Exception e){
            return false;
        }
    }
}