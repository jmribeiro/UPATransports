package pt.upa.broker.ws.cli;

import pt.upa.broker.ws.*;
import java.util.*;
import java.net.*;

public class BrokerClient {

	private BrokerPortType _port;
	private boolean _exit;
	private Scanner sc = new Scanner(System.in);

	public BrokerClient(BrokerPortType port){

		if(port == null){
			System.out.println("Broker Server is offline");
			_exit = true;
		}
		_port = port;
		_exit = false;
	}

	public void run(){
		
		while(!_exit){
			displayMenu();
			int i = sc.nextInt();
			handleRequest(i);
		}
	}

	private void displayMenu(){
		System.out.println("\nUpa Broker 2016 - Grupo 24 LEIC-T");
		System.out.println("++++++++++++++++++++++++++");
		System.out.println("1 - Book New Transport");
		System.out.println("2 - View Transport Status");
		System.out.println("3 - List all transports");
		System.out.println("0 - Exit Application");
		System.out.println("++++++++++++++++++++++++++");
		System.out.print(">>> ");
	}

	private void handleRequest(int i){
		switch(i){
			case 0:
				_exit = true;
				break;
			case 1:
				System.out.println("What city of origin? ");
				String origin = sc.next();
				System.out.println("What city of destination? ");
				String destination = sc.next();
				System.out.println("Transport price? ");
				int maxPrice = sc.nextInt();
				bookTransport(origin, destination, maxPrice);
				break;
			case 2:
				System.out.println("Which transport do you wish to check on? ");
				String id = sc.next();
				viewTransportStatus(id);
				break;
			case 3:
				listTransports();
				break;
			default:
				break;
		}
	}

	private void viewTransportStatus(String id){
		try{
			TransportView transport = _port.viewTransport(id);
			printTransport(transport);
		}catch(UnknownTransportFault_Exception e){
			System.out.println(e.getMessage());
		}

	}

	private void bookTransport(String origin, String destination, int maxPrice){
		String newTransportId = "";
		try{
			newTransportId = _port.requestTransport(origin, destination, maxPrice);
			System.out.println("Your transport id is "+newTransportId);
		}catch(UnknownLocationFault_Exception | InvalidPriceFault_Exception | UnavailableTransportPriceFault_Exception | UnavailableTransportFault_Exception e){
			System.out.println(e.getMessage());
		}
	}

	private void listTransports(){
		List<TransportView> transports = _port.listTransports();

		for(TransportView t : transports){
			printTransport(t);
		}
	}

	private void printTransport(TransportView transport){
		System.out.print("\n++++++++++++++++++++++++++++++++");
		System.out.println("\n"+transport.getTransporterCompany()+ " - Transport nº " +transport.getId());
		System.out.print("\nFrom: "+transport.getOrigin());
		System.out.println("\tTo: "+transport.getDestination());
		System.out.println("\nPrice: "+transport.getPrice()+"€");
		System.out.println("\nStatus: "+transport.getState().value());
		System.out.println("++++++++++++++++++++++++++++++++\n");
	}


}

