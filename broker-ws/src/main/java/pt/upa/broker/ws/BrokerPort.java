package pt.upa.broker.ws;

import javax.jws.WebService;
import javax.jws.HandlerChain;
import javax.xml.registry.*;

import java.util.*;
import java.util.concurrent.*;

import static javax.xml.ws.BindingProvider.ENDPOINT_ADDRESS_PROPERTY;
import java.util.Map;
import javax.xml.ws.BindingProvider;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;

import pt.upa.transporter.ws.*;

@WebService(
    endpointInterface="pt.upa.broker.ws.BrokerPortType",
    wsdlLocation="broker.1_0.wsdl",
    name="Broker",
    portName="BrokerPort",
    targetNamespace="http://ws.broker.upa.pt/",
    serviceName="BrokerService"
)

public class BrokerPort implements BrokerPortType {

    private List<TransportView> _transports;
    private Map<String, TransporterPortType> _transportCompanies;
    private Map<String, String> _transportToJob;
    private List<String> _validCities;
    private int autoincrement = 0;

    private boolean _primary;
    private BrokerPort _secondary;

    public BrokerPort(boolean primary, BrokerPort secondary){

        _primary = primary;

        if(primary){
            _secondary = secondary;
        }

        _transports = Collections.synchronizedList(new ArrayList<TransportView>());

        //GET FROM UDDI WITH WILDCARD
        contactCompanies();

        _transportToJob = new ConcurrentSkipListMap<String, String>();
        _validCities = Collections.synchronizedList(new ArrayList<String>());

            _validCities.add("Porto");
            _validCities.add("Braga");
            _validCities.add("Viana do Castelo");
            _validCities.add("Vila Real");
            _validCities.add("Bragança");

            _validCities.add("Lisboa");
            _validCities.add("Leiria");
            _validCities.add("Santarém");
            _validCities.add("Castelo Branco");
            _validCities.add("Coimbra");
            _validCities.add("Aveiro");
            _validCities.add("Viseu");
            _validCities.add("Guarda");

            _validCities.add("Setúbal");
            _validCities.add("Évora");
            _validCities.add("Portalegre");
            _validCities.add("Beja");
            _validCities.add("Faro");
    }

    public synchronized String ping(String name){
        return "Hello " +name+ ". Im a Broker";
    }

    public synchronized String requestTransport(String origin, String destination, int price) throws InvalidPriceFault_Exception, UnavailableTransportFault_Exception, UnavailableTransportPriceFault_Exception, UnknownLocationFault_Exception{
        
        boolean validOrigin = _validCities.contains(origin);
        boolean validDestination = _validCities.contains(destination);

        // Zonas Desconhecidas
        if(!validOrigin || !validDestination){
            throw new UnknownLocationFault_Exception("Invalid Cities", new UnknownLocationFault());
        }

        // Preço negativo
        if(price < 0 || price >= 1000){
            throw new InvalidPriceFault_Exception("Price must be bigger than 0", new InvalidPriceFault());
        }


        JobView bestOffer = null;
        int bestPrice = 99999;

        // Base para o transporte (Começa em REQUESTED)
        TransportView transport = new TransportView();
        transport.setState(TransportStateView.REQUESTED);

        // Usado para caso se encontre melhor oferta avisar a antiga que nao queremos a proposta
        TransporterPortType previousCompany = null;

        contactCompanies();
        
        for(TransporterPortType company : _transportCompanies.values()){
            
            try{
            
                JobView currentOffer = company.requestJob(origin, destination, price); 
                
                // NULL => Preço > 100 / Cidades fora das zonas -> Ignorar oferta
                if(currentOffer==null){
                    continue;
                // Preço recebido mais pequeno que o melhor até agora
                }else if(currentOffer.getJobPrice() < bestPrice){

                    // Avisar a companhia anterior que ja nao queremos
                    if(previousCompany != null){
                        try{
                            previousCompany.decideJob(bestOffer.getJobIdentifier(), false);    
                        }catch(BadJobFault_Exception e){
                            // DEVERIA SER UNREACHABLE, se nao for, como tratar?
                        }
                    }

                    previousCompany=company;
                    
                    //Atualizar a melhor oferta
                    bestOffer = currentOffer;
                    bestPrice = bestOffer.getJobPrice();
                    
                }

            }catch(BadLocationFault_Exception e){
                throw new UnknownLocationFault_Exception("Invalid Cities", new UnknownLocationFault());
            }catch(BadPriceFault_Exception e){
                throw new InvalidPriceFault_Exception("Price must be bigger than 0", new InvalidPriceFault());
            }
        }

        // Foi sempre preço > 100 / Cidades fora das zonas
        if(bestOffer == null){ 
            throw new UnavailableTransportFault_Exception("There are no available transporters for your request", new UnavailableTransportFault());
        }
        
        // Criar transporte
        transport.setId(""+autoincrement++);
        transport.setOrigin(bestOffer.getJobOrigin());
        transport.setDestination(bestOffer.getJobDestination());
        transport.setPrice(bestOffer.getJobPrice());
        transport.setTransporterCompany(bestOffer.getCompanyName());
        transport.setState(TransportStateView.BUDGETED);


        // Preço da oferta > pedido
        if(bestOffer.getJobPrice() > price){ 
            
            try{
                // Avisar a companhia que nao queremos
                _transportCompanies.get(bestOffer.getCompanyName()).decideJob(bestOffer.getJobIdentifier(), false);
            }catch(BadJobFault_Exception e){
                throw new UnavailableTransportFault_Exception("Something went wrong while canceling, All the offers are above your budget", new UnavailableTransportFault());
            }
            // Atualizar o estado do transporte
            transport.setState(TransportStateView.FAILED);

            // Adicionar o transporte mal sucedido à lista (historico)
            _transports.add(transport);
            _transportToJob.put(transport.getId(), bestOffer.getJobIdentifier());

            /* REPLICA */    
            if(_primary){
                _secondary.updateStatus(transport);
            }
            /* REPLICA */

            // Avisar o cliente com Fault
            throw new UnavailableTransportPriceFault_Exception("All the offers are above your budget", new UnavailableTransportPriceFault());
        
        }else{

            try{
                // Avisar a companhia que queremos o servico
                _transportCompanies.get(bestOffer.getCompanyName()).decideJob(bestOffer.getJobIdentifier(), true);
            }catch(BadJobFault_Exception e){
                throw new UnavailableTransportFault_Exception("Something went wrong while booking, canceling the transport", new UnavailableTransportFault());
            }

            // Atualizar o estado do transporte
            transport.setState(TransportStateView.BOOKED);

            // Adicionar o transporte mal sucedido à lista (historico)
            _transports.add(transport);
            _transportToJob.put(transport.getId(), bestOffer.getJobIdentifier());

            /* REPLICA */    
            if(_primary){
                _secondary.updateStatus(transport);
            }
            /* REPLICA */

            //Retornar o id para o cliente
            return transport.getId();
        }
    }

    public synchronized TransportView viewTransport(String id) throws UnknownTransportFault_Exception{
        
        for(TransportView transport : _transports){
            if(transport.getId().equals(id)){
                updateTransportStatus(transport);

                /* REPLICA */    
                if(_primary){
                    _secondary.updateViewTransportStatus(id);
                }
                /* REPLICA */

                return transport;
            }
        }
        throw new UnknownTransportFault_Exception("Transport doesn't exist", new UnknownTransportFault());
    }

    public synchronized List<TransportView> listTransports(){
        for(TransportView transport : _transports){
            updateTransportStatus(transport);
        }
        return _transports;
    }

    public synchronized void clearTransports(){
        _transports = Collections.synchronizedList(new ArrayList<TransportView>());

        if(_primary){
            _secondary.clearTransports();
        }

    }

    private void contactCompanies(){

        //TreeMap com String (nome da transportadora) VS Transportadora (port)
        _transportCompanies = new ConcurrentSkipListMap<String, TransporterPortType>();

        try{

            // Contacting UDDI at http://localhost:9090
            UDDINaming uddiNaming = new UDDINaming("http://localhost:9090");

            // Looking for UpaTransporter1
            Collection<String> endpointAddresses = uddiNaming.list("UpaTransporter%");

            TransporterService service = new TransporterService();
            
            for(String currentAddress : endpointAddresses){

                // Creating Stub
                TransporterPortType actualPort = service.getTransporterPort();
                
                // Setting endpoint address
                BindingProvider bindingProvider = (BindingProvider) actualPort;
                
                Map<String, Object> requestContext = bindingProvider.getRequestContext();
                requestContext.put(ENDPOINT_ADDRESS_PROPERTY, currentAddress);
                
                String currentName = "UpaTransporter"+currentAddress.charAt(20);
                System.out.println("Found "+currentName);

                bindingProvider.getRequestContext().put("name", "UpaBroker");

                _transportCompanies.put(currentName,actualPort);
            } 
        }catch(JAXRException e){
            System.out.println("UDDI is down... Failed to contact transporters");
        }

    }

    private void updateTransportStatus(TransportView transport){

        TransporterPortType company = _transportCompanies.get(transport.getTransporterCompany());
        JobView job = company.jobStatus(_transportToJob.get(transport.getId()));

        switch(job.getJobState()){

            case HEADING:
                transport.setState(TransportStateView.HEADING);
                break;
            case ONGOING:
                transport.setState(TransportStateView.ONGOING);
                break;
            case COMPLETED:
                transport.setState(TransportStateView.COMPLETED);
                break;
            default:
                return;
        }
        
    }

    public void updateStatus(TransportView transport){
        if(!_primary){
            contactCompanies();
            _transports.add(transport);
            _transportToJob.put(transport.getId(), transport.getTransporterCompany()); 
        }
    }

    public void updateViewTransportStatus(String id){
        if(!_primary){
            for(TransportView transport : _transports){
                if(transport.getId().equals(id)){
                    updateTransportStatus(transport);
                }
            }
        }   
    }
}