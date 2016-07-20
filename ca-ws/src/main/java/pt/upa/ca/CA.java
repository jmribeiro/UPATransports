package pt.upa.ca;

import javax.jws.WebService;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

@WebService
public interface CA {
	String ping();
	byte[] requestCertificate(String entityName);
}
