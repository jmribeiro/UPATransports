package pt.upa.ca;

import javax.jws.WebService;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Collection;

import java.io.*;

@WebService(endpointInterface = "pt.upa.ca.CA")
public class CAImpl implements CA {

	public String ping(){
		return "Hello";
	}

	public synchronized byte[] requestCertificate(String entityName){
		try{
			Certificate c = readCertificateFile(entityName+".cer");
			return c.getEncoded();
		}catch(Exception e){
			return null;
		}		
	}

	/**
	 * Reads a certificate from a file
	 * 
	 * @return
	 * @throws Exception
	*/
	public static Certificate readCertificateFile(String certificateFilePath) throws Exception {
		FileInputStream fis;

		try {
			fis = new FileInputStream(certificateFilePath);
		} catch (FileNotFoundException e) {
			System.err.println("Certificate file <" + certificateFilePath + "> not found.");
			return null;
		}
		BufferedInputStream bis = new BufferedInputStream(fis);

		CertificateFactory cf = CertificateFactory.getInstance("X.509");

		if (bis.available() > 0) {
			Certificate cert = cf.generateCertificate(bis);
			return cert;
			// It is possible to print the content of the certificate file:
			// System.out.println(cert.toString());
		}
		bis.close();
		fis.close();
		return null;
	}

}