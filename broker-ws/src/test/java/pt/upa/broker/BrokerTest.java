package pt.upa.broker.ws;

import org.junit.*;
import static org.junit.Assert.*;

import pt.upa.transporter.ws.*;
import upa.ws.handler.*;

public class BrokerTest {

    // static members


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {

    }

    @AfterClass
    public static void oneTimeTearDown() {

    }


    // members

    BrokerPort port;


    // initialization and clean-up for each test

    @Before
    public void setUp() {
        port = new BrokerPort(false, null);
        AuthenticationHandler.setName("UpaBroker");
    }

    @After
    public void tearDown() {
        port = null;
    }


    // tests

    @Test
    public void pingTest() {

        final String name = "friend";
        final String expected = "Hello friend. Im a Broker";

        String response = port.ping(name);

        assertEquals(expected, response);
    }


    @Test
    public void requestTransportSuccess(){
        
        int validPrice = 49;
        String validOrigin = "Lisboa";
        String validDestination = "Leiria";
        try{

            String resultId = port.requestTransport(validOrigin, validDestination, validPrice);
            TransportView resultTransport = port.viewTransport(resultId);

            boolean expectedId = resultTransport.getId().equals("0"); //Unico ate agora
            boolean expectedOrigin = resultTransport.getOrigin().equals(validOrigin); 
            boolean expectedDestination = resultTransport.getDestination().equals(validDestination);
            boolean expectedPrice = resultTransport.getPrice() <= validPrice;

            // STATE DOESNT MATTER / Se demorou entretanto tempo pode ja nao estar booked, ter passado para outro estado
            // COMPANY DOESNT MATTER / CANT CONTROL

            boolean expectedResult = expectedId && expectedOrigin && expectedDestination && expectedPrice;
            assertTrue(expectedResult);

        }catch(UnknownLocationFault_Exception | UnknownTransportFault_Exception | InvalidPriceFault_Exception e){
            e.printStackTrace();
            fail();
        }catch(UnavailableTransportFault_Exception | UnavailableTransportPriceFault_Exception e){
            System.out.println("You need more transporters to test this functionality\n None of the current has given you an offer");
            return;
        }
    }

    @Test
    public void requestTransportNegativePrice() {

        int invalidPrice = -100;
        String validOrigin = "Lisboa";
        String validDestination = "Leiria";

        try{

            String resultId = port.requestTransport(validOrigin, validDestination, invalidPrice);

    
        }catch(InvalidPriceFault_Exception e){
            return;
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }

    }

    @Test
    public void requestTransportUnknownLocations(){

        int dontCarePrice = 100;
        String validOrigin = "L1ZB0a";
        String validDestination = "L3ir14";

        try{
            String resultId = port.requestTransport(validOrigin, validDestination, dontCarePrice);
        }catch(UnknownLocationFault_Exception e){
            return;
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void resultTransportImpossiblePrice(){

        int impossiblePrice = 1000;
        String validOrigin = "Lisboa";
        String validDestination = "Leiria";

        try{
            String resultId = port.requestTransport(validOrigin, validDestination, impossiblePrice);
        }catch(InvalidPriceFault_Exception e){
            return;
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }

    }

    
}