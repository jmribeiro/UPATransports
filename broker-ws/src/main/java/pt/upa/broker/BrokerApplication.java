package pt.upa.broker;

import javax.xml.ws.*;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

import pt.upa.broker.ws.*;

import upa.ws.handler.*;

public class BrokerApplication {

	public static void main(String[] args) throws Exception {
		System.out.println(BrokerApplication.class.getSimpleName() + " starting...");
		
		// Check arguments
		if (args.length < 4) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s uddiURL wsName wsURL%n URL2", BrokerApplication.class.getName());
			return;
		}

		String uddiURL = args[0];
		String name = args[1];
		String url = args[2];
		String url2 = args[3];

		AuthenticationHandler.setName(name);

		Endpoint endpoint = null;
		Endpoint secondaryEndpoint = null;

		UDDINaming uddiNaming = null;

		try {

			BrokerPort secondary = new BrokerPort(false, null);
			BrokerPort port = new BrokerPort(true, secondary);

			endpoint = Endpoint.create(port);
			secondaryEndpoint = Endpoint.create(secondary);

			// publish endpoint
			System.out.printf("Starting %s%n", url);
			endpoint.publish(url);

			// publish endpoint
			System.out.printf("Starting %s%n", url2);
			secondaryEndpoint.publish(url2);

			// publish to UDDI
			System.out.printf("Publishing '%s' to UDDI at %s%n", name, uddiURL);
			uddiNaming = new UDDINaming(uddiURL);
			uddiNaming.rebind(name, url);

			Manager m = new Manager(port, secondary, name, url2, uddiNaming);
			Thread secondaryThread = new Thread(m);
			secondaryThread.start();

			// wait
			System.out.println("Primary Awaiting connections");
			System.out.println("Press enter to shutdown");
			System.in.read();

			m.stopRunning();

		} catch (Exception e) {
			System.out.printf("Caught exception: %s%n", e);
			e.printStackTrace();

		} finally {
			try {
				if (endpoint != null || secondaryEndpoint != null) {
					// stop endpoint
					endpoint.stop();
					secondaryEndpoint.stop();

					System.out.printf("Stopped %s%n", url);
					System.out.printf("Stopped %s%n", url2);
				}
			} catch (Exception e) {
				System.out.printf("Caught exception when stopping: %s%n", e);
			}
			try {
				if (uddiNaming != null) {
					// delete from UDDI
					uddiNaming.unbind(name);
					System.out.printf("Deleted '%s' from UDDI%n", name);
				}
			} catch (Exception e) {
				System.out.printf("Caught exception when deleting: %s%n", e);
			}
		}
	}

	private static void clearUddi(String uddiURL) throws Exception{
		UDDINaming uddiNaming = new UDDINaming(uddiURL);
		uddiNaming.unbind("UpaBroker");
		uddiNaming.unbind("CA");
		uddiNaming.unbind("UpaTransporter1");
		uddiNaming.unbind("UpaTransporter2");
	}


}





