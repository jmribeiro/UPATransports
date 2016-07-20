package pt.upa.transporter.ws;

import org.junit.*;
import static org.junit.Assert.*;

/**
 *  Unit Test example
 *  
 *  Invoked by Maven in the "test" life-cycle phase
 *  If necessary, should invoke "mock" remote servers 
 */
public class TransporterTest {

    // static members


    // one-time initialization and clean-up

    @BeforeClass
    public static void oneTimeSetUp() {

    }

    @AfterClass
    public static void oneTimeTearDown() {

    }


    // members

    TransporterPort portOdd, portEven;

    // initialization and clean-up for each test

    @Before
    public void setUp() {
        portOdd = new TransporterPort("UpaTransporter1");
        portEven = new TransporterPort("UpaTransporter2");
    }

    @After
    public void tearDown() {
        portOdd = null;
        portEven = null;
    }


    // tests
    @Test
    public void pingTest() {
        
        final String name = "friend";
        final String expected = "Hello "+name+". I'm a Transporter.";

        String response = portOdd.ping(name);

        assertEquals(expected, response);
    }


    @Test
    public void requestJobTestSucessOddPriceEven() {

        String origin, destination;
        int price;

        origin = "Lisboa";
        destination = "Leiria";
        price = 50;

        try{
    
            JobView result = portOdd.requestJob(origin, destination, price);

            boolean sameJobId = result.getJobIdentifier().equals("0");
            boolean sameOrigin = result.getJobOrigin() == origin;
            boolean sameDestination = result.getJobDestination() == destination;
            boolean rightPrice = result.getJobPrice() > price;
            boolean sameCompany = result.getCompanyName() == "UpaTransporter1";
            boolean sameState = result.getJobState().equals(JobStateView.PROPOSED);

            assertTrue(sameJobId && sameCompany && sameOrigin && sameDestination && rightPrice && sameState);
    
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
        
    }

    @Test
    public void requestJobTestSucessOddPriceOdd(){
        
        String origin = "Lisboa";
        String destination = "Leiria";
        int price = 49;

        try{

            JobView result = portOdd.requestJob(origin, destination, price);


            boolean sameJobId = result.getJobIdentifier().equals("0");
            boolean sameOrigin = result.getJobOrigin() == origin;
            boolean sameDestination = result.getJobDestination() == destination;
            boolean rightPrice = result.getJobPrice() < price;
            boolean sameCompany = result.getCompanyName() == "UpaTransporter1";
            boolean sameState = result.getJobState().equals(JobStateView.PROPOSED);
        
            assertTrue(sameJobId && sameCompany && sameOrigin && sameDestination && rightPrice && sameState);

        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test 
    public void requestJobTestSuccessEvenPriceEven() {
        
        String origin, destination;
        int price;

        price = 50;
        origin = "Lisboa";
        destination = "Leiria";

        try{
    
            JobView result = portEven.requestJob(origin, destination, price);

            boolean sameJobId = result.getJobIdentifier().equals("0");
            boolean sameOrigin = result.getJobOrigin() == origin;
            boolean sameDestination = result.getJobDestination() == destination;
            boolean rightPrice = result.getJobPrice() < price;
            boolean sameCompany = result.getCompanyName() == "UpaTransporter2";
            boolean sameState = result.getJobState().equals(JobStateView.PROPOSED);
        
            assertTrue(sameJobId && sameCompany && sameOrigin && sameDestination && rightPrice && sameState);
    
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
        
    }

    @Test
    public void requestJobTestSucessEvenPriceOdd() {
        
        String origin, destination;
        int price;

        origin = "Lisboa";
        destination = "Leiria";
        price = 49;

        try{
    
            JobView result = portEven.requestJob(origin, destination, price);

            boolean sameJobId = result.getJobIdentifier().equals("0");
            boolean sameOrigin = result.getJobOrigin() == origin;
            boolean sameDestination = result.getJobDestination() == destination;
            boolean rightPrice = result.getJobPrice() > price;
            boolean sameCompany = result.getCompanyName() == "UpaTransporter2";
            boolean sameState = result.getJobState().equals(JobStateView.PROPOSED);

            assertTrue(sameJobId && sameCompany && sameOrigin && sameDestination && rightPrice && sameState);
    
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
        
    }
    @Test
    public void requestJobHighPrice(){
        
        String validOrigin = "Lisboa";
        String validDestination = "Leiria";
        boolean resultPrice;

        int highPrice = 120;
        
        try{
            JobView result = portOdd.requestJob(validOrigin, validDestination, highPrice);
            assertTrue(result==null);
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
        
    }

    @Test
    public void requestJobNegativePrice(){
        
        String validOrigin = "Lisboa";
        String validDestination = "Leiria";
        
        int negativePrice = -1;

        try{
            JobView result = portOdd.requestJob(validOrigin, validDestination, negativePrice);
        }catch(BadPriceFault_Exception e){
            return;
        }catch(BadLocationFault_Exception e){
            e.printStackTrace();
            fail();
        }
        fail();

    }

    // test EmptyLocation
    @Test
    public void requestJobTestEmptyLocation() {

        String emptyOrigin, emptyDestination;
        int price;

        emptyOrigin = "";
        emptyDestination = "";
        price = 0;

        try{
            JobView result = portOdd.requestJob(emptyOrigin, emptyDestination, price);
        }catch(BadLocationFault_Exception e){
            return;
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }


        
    
    }


    // test LowPrice
    @Test
    public void requestJobTestLowPrice() {

        JobView mock = new JobView();

        String validOrigin, validDestination;
        int price;

        validOrigin = "Beja";
        validDestination = "Lisboa";
        price = 6;

        mock.setCompanyName("UpaTransporter1");
        mock.setJobIdentifier("0");
        mock.setJobOrigin(validOrigin);
        mock.setJobDestination(validDestination);
        mock.setJobState(JobStateView.PROPOSED);

        try{

            JobView result = portOdd.requestJob(validOrigin, validDestination, price);
            boolean lowPrice = result.getJobPrice() < price;
            assertTrue(lowPrice);

        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void decideJobAcceptSuccess(){
        String validId = "0";
        boolean accept = true;
        
        try{
            portOdd.requestJob("Lisboa", "Leiria", 45);
            JobView result = portOdd.decideJob(validId, accept);

            if(result.getJobState() != JobStateView.ACCEPTED){
                fail();
            }
            
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }

    }
    
    @Test
    public void decideJobRejectSuccess(){

        String validId = "0";
        boolean accept = false;

        try{
            portOdd.requestJob("Lisboa", "Leiria", 45);
            JobView result = portOdd.decideJob(validId, accept);

            if(result.getJobState() != JobStateView.REJECTED){
                fail();
            }

        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void decideJobBadId(){
        String invalidId = "9999";
        try{
            JobView result = portOdd.decideJob(invalidId, false);
        }catch(BadJobFault_Exception e){
            return;
        }catch(Exception e){
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void jobStatusValidJobId(){
        String validId = "0";
        JobView result = portOdd.jobStatus(validId);
        
        if(result==null){
            fail();
        }
        
    }

    @Test
    public void jobStatusBadId(){
        String invalidId = "9999";
        
        JobView result = portOdd.jobStatus(invalidId);
        
        if(result==null){
            return;
        }
    }

}