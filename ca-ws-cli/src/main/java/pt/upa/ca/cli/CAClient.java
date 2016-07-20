package pt.upa.ca.cli;

import pt.upa.ca.*;

import java.util.*;
import javax.xml.ws.*;
import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

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
import java.security.cert.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Collection;

import java.io.*;

public class CAClient{
	
	private CA _port;

	public CAClient(String uddiURL, String name){
		try{
			contactServer(uddiURL, name);
		}catch(Exception e){
			_port = null;
		}
	}

	public Certificate requestCertificate(String entityName) {
		if(_port!=null){
			try{
				byte[] bytes = _port.requestCertificate(entityName);
				
				CertificateFactory certFactory = CertificateFactory.getInstance("X.509");

				InputStream in = new ByteArrayInputStream(bytes);
				
				X509Certificate cert = (X509Certificate)certFactory.generateCertificate(in);
				
				return cert;
			}catch(Exception e){
				return null;
			}
		}else{
			return null;
		}
	}

	private void contactServer(String uddiURL, String name) throws Exception{

		//System.out.printf("Contacting UDDI at %s%n", uddiURL);
		UDDINaming uddiNaming = new UDDINaming(uddiURL);

		System.out.println("\n\nContacting the CA");
		String endpointAddress = uddiNaming.lookup(name);

		if (endpointAddress == null) {
			System.out.println("CA not found! - Check if WS is up");
			return;
		} else {
			System.out.println("Found CA!");
		}

		CAImplService service = new CAImplService();
		CA port = service.getCAImplPort();

		BindingProvider bindingProvider = (BindingProvider) port;
		Map<String, Object> requestContext = bindingProvider.getRequestContext();
		requestContext.put(ENDPOINT_ADDRESS_PROPERTY, endpointAddress);

		_port = port;
	}

}