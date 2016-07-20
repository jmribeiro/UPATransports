package pt.upa.broker;

import javax.xml.ws.*;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

import pt.upa.broker.ws.*;

public class Manager implements Runnable {

	private boolean _running;
	private BrokerPort _primary, _secondary;
	private String _name, _url;
	private UDDINaming _uddiNaming;

	public Manager(BrokerPort primary, BrokerPort secondary, String name, String url, UDDINaming uddiNaming){
		_running = true;	
		_primary = primary;	
		_secondary = secondary;
		_name = name;
		_url = url;
		_uddiNaming = uddiNaming;
	}

	public void run(){
		
		try{

			String reply = _primary.ping("Live Checker");

			while(reply.equals("Hello Live Checker. Im a Broker") && _running){

				Thread.sleep(3000);
				reply = _primary.ping("Live Checker");
				System.out.println(reply);
			}

		}catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		if(_running){
			//Primary Crashed
			secondaryTakeOver();
		}

		System.out.println("Exiting secondary...");

	}

	public synchronized void stopRunning(){
		_running = false;
	}

	public void secondaryTakeOver(){

		try {

			// publish to UDDI
			System.out.printf("Publishing '%s' to UDDI at %s%n", _name);
			_uddiNaming.rebind(_name, _url);
		} catch (Exception e) {
			System.out.printf("Caught exception: %s%n", e);
			e.printStackTrace();
		}
	}

}