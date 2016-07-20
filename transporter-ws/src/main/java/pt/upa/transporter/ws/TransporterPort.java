package pt.upa.transporter.ws;

import javax.jws.WebService;
import javax.jws.HandlerChain;
import java.util.*;
import java.util.Collections;

@WebService(
    endpointInterface="pt.upa.transporter.ws.TransporterPortType",
    wsdlLocation="transporter.1_0.wsdl",
    name="Transporter",
    portName="TransporterPort",
    targetNamespace="http://ws.transporter.upa.pt/",
    serviceName="TransporterService"
)

public class TransporterPort implements TransporterPortType {

    private List<JobView> _jobs;
    private List<String> _north, _center, _south;
        
    // false = par
    // true = impar
    private boolean _parity;

    private String _name;
    private int autoincrement = 0;

    public TransporterPort(String name){

        _jobs = Collections.synchronizedList(new ArrayList<JobView>());
        _name = name;

        int number = Integer.parseInt(""+name.charAt(name.length()-1));
        
        if(number%2==0){
            _parity = false;
        }else{
            _parity = true;
        }

        if(_north == null || _center == null || _south == null || _north.isEmpty() || _center.isEmpty() || _south.isEmpty()){

            _north = Collections.synchronizedList(new ArrayList<String>());
            _center = Collections.synchronizedList(new ArrayList<String>());
            _south = Collections.synchronizedList(new ArrayList<String>());

            _north.add("Porto");
            _north.add("Braga");
            _north.add("Viana do Castelo");
            _north.add("Vila Real");
            _north.add("Bragança");

            _center.add("Lisboa");
            _center.add("Leiria");
            _center.add("Santarém");
            _center.add("Castelo Branco");
            _center.add("Coimbra");
            _center.add("Aveiro");
            _center.add("Viseu");
            _center.add("Guarda");

            _south.add("Setúbal");
            _south.add("Évora");
            _south.add("Portalegre");
            _south.add("Beja");
            _south.add("Faro");
        }

    }

    public synchronized String ping(String name){
    	return "Hello "+name+". I'm a Transporter.";
    }

    public synchronized JobView requestJob(String origin, String destination, int price) throws BadLocationFault_Exception, BadPriceFault_Exception{
        //1 - Verificar origin e destination unknown
        boolean validOrigin = _north.contains(origin) || _center.contains(origin) || _south.contains(origin);
        boolean validDestination = _north.contains(destination) || _center.contains(destination) || _south.contains(destination);
        
        if(!validOrigin || !validDestination){
            throw new BadLocationFault_Exception("invalid locations", new BadLocationFault());
        }
        JobView offer;
        Random generator;

        boolean originInRange = _center.contains(origin) || _south.contains(origin);
        boolean destinationInRange = _center.contains(destination) || _south.contains(destination);
        
        //2 - Verificar preço
        if(price<0){
            throw new BadPriceFault_Exception("price must be >= 0", new BadPriceFault());
        }else if(price > 100 || !originInRange || !destinationInRange){ //OU REGIAO NAO VALIDA
            return null;
        }else if(price <=10){
            offer = new JobView();
            generator = new Random();

            offer.setCompanyName(_name);
            offer.setJobIdentifier(new String(""+autoincrement++));
            offer.setJobOrigin(origin);
            offer.setJobDestination(destination);
            offer.setJobState(JobStateView.PROPOSED);
            offer.setJobPrice(generator.nextInt(price));
            _jobs.add(offer);

            return offer;

        }else{
            offer = new JobView();
            generator = new Random();

            offer.setCompanyName(_name);
            offer.setJobIdentifier(new String(""+autoincrement++));
            offer.setJobOrigin(origin);
            offer.setJobDestination(destination);
            offer.setJobState(JobStateView.PROPOSED);

            if( (price%2 == 0 && _parity == true) || (price%2 != 0 && _parity == false)) { // Par
                offer.setJobPrice(generator.nextInt((100 - price) + 1) + price);
                _jobs.add(offer);
                return offer;  
            }else{ // Impar
                offer.setJobPrice(generator.nextInt(price));
                _jobs.add(offer);

                return offer;
            }
        }

    }

    public synchronized JobView decideJob(String id, boolean accept) throws BadJobFault_Exception{
        
        for(JobView job : _jobs){
            if(job.getJobIdentifier().equals(id)){
                if(job.getJobState() == JobStateView.PROPOSED){
                    if(accept){
        
                        job.setJobState(JobStateView.ACCEPTED);

                        Expedition expedition = new Expedition(job);
                        Thread t = new Thread(expedition);
                        t.start();

                    }else{
                        job.setJobState(JobStateView.REJECTED);
                    }  
                    return job;
                }else{
                    throw new BadJobFault_Exception("There is no such proposed job.", new BadJobFault());
                }
            }
        }
        throw new BadJobFault_Exception(id+" is not a valid ID", new BadJobFault());
    }

    public synchronized JobView jobStatus(String id){

        JobView newJob = new JobView(); //ir buscar com id
        boolean validId = true;

        for(JobView job : _jobs){
            if(job.getJobIdentifier().equals(id)){
                newJob = job;
            }
        }

        if(validId){
            return newJob;

        }else{
            return null; 
        }
    }

    public synchronized List<JobView> listJobs(){
    	return _jobs;
    }

    public synchronized void clearJobs(){
        _jobs = Collections.synchronizedList(new ArrayList<JobView>());
    }

}
