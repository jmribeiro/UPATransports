package pt.upa.broker.ws.it;

import org.junit.*;
import static org.junit.Assert.*;

import pt.upa.broker.ws.*;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import java.util.*;
import javax.xml.ws.BindingProvider;
import pt.ulisboa.tecnico.sdis.ws.uddi.*;
import javax.xml.registry.JAXRException;

/**
 *  Integration Test example
 *  
 *  Invoked by Maven in the "verify" life-cycle phase
 *  Should invoke "live" remote servers 
 */
public class ApplicationIT {

    // static members


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {

    }

    @AfterClass
    public static void oneTimeTearDown() {

    }


    // members
    BrokerPortType _port;

    // initialization and clean-up for each test

    @Before
    public void setUp() {
        try{
            String uddiURL = "http://localhost:9090";
            String name = "UpaBroker";

            System.out.printf("INTEGRATION TEST: Contacting UDDI at %s%n", uddiURL);
            UDDINaming uddiNaming = new UDDINaming(uddiURL);

            System.out.printf("INTEGRATION TEST: Looking for '%s'%n", name);
            String endpointAddress = uddiNaming.lookup(name);

            if (endpointAddress == null) {
                System.out.println("INTEGRATION TEST: Not found!");
                return;
            } else {
                System.out.printf("INTEGRATION TEST: Found %s%n", endpointAddress);
            }

            System.out.println("INTEGRATION TEST: Creating stub ...");
            BrokerService service = new BrokerService();
            _port = service.getBrokerPort();

            System.out.println("INTEGRATION TEST: Setting endpoint address ...");
            BindingProvider bindingProvider = (BindingProvider) _port;
            Map<String, Object> requestContext = bindingProvider.getRequestContext();
            requestContext.put(ENDPOINT_ADDRESS_PROPERTY, endpointAddress);
        }catch(JAXRException e){
            System.out.println("INTEGRATION TEST: Failed To Setup");
        }

    }

    @After
    public void tearDown() {
        System.out.println("INTEGRATION TEST: SHUTTING DOWN...\n");
        _port.clearTransports();
        _port = null;
    }

    // test1
    @Test(expected = UnknownLocationFault_Exception.class)
    public void unknownLocation() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        dispatch("L1zboa", "p0rrt0", 50);
    }

    // test2
    @Test(expected = InvalidPriceFault_Exception.class)
    public void negativePrice() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        dispatch("Lisboa", "Porto", -1);
        
    }

    // test3
    @Test
    public void T1_oddPrice() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        boolean res = dispatch("Lisboa", "Faro", 51);
        assertTrue(res);
    }

    // test4
    @Test(expected = UnavailableTransportPriceFault_Exception.class)
    public void T1_evenPrice() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        dispatch("Lisboa", "Faro", 50);
    }

    // test5
    @Test(expected = UnavailableTransportPriceFault_Exception.class)
    public void T2_oddPrice() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        dispatch("Lisboa", "Porto", 51);
    }

    // test6
    @Test
    public void T2_evenPrice() throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        assertTrue(dispatch("Lisboa", "Porto", 50));

    }

    private boolean dispatch(String origin, String destination, int price) throws UnknownTransportFault_Exception, InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        
        boolean success;

        List<TransportView> beforeList = _port.listTransports();
        System.out.println("Transports before requesting");
        printList(beforeList);

        System.out.println("Requesting transport from "+origin+" to "+destination+" at max "+price+"€");
        String id = _port.requestTransport(origin, destination, price);

        System.out.println("Viewing Transport Status");
        TransportView transport = _port.viewTransport(id);

        List<TransportView> afterList = _port.listTransports();
        System.out.println("Transports after requesting");
        printList(afterList);

        return transport!=null && transport.getOrigin().equals(origin) && transport.getDestination().equals(destination) && transport.getPrice() <= price;

    }

    private void printList(List<TransportView> list){
        for(TransportView t : list){
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