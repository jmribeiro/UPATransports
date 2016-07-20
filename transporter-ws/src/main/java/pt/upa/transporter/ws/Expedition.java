package pt.upa.transporter.ws; 

import java.util.Random;

public class Expedition implements Runnable{

	private JobView _job;
	
	public Expedition(JobView job){
		_job = job;
	}

	public void run(){
		try{

			System.out.print("\n++++++++++++++++++++++++++++++++");
			System.out.println("\n"+_job.getCompanyName()+ " - Job nº " +_job.getJobIdentifier());
			System.out.print("\nFrom: "+_job.getJobOrigin());
			System.out.println("\tTo: "+_job.getJobDestination());
			System.out.println("\nPrice: "+_job.getJobPrice()+"€");


			Random generator = new Random();

			System.out.println("Status: ACCEPTED\n");
			Thread.sleep(generator.nextInt(5000));

			System.out.println("Status: HEADING\n");
			_job.setJobState(JobStateView.HEADING);
			Thread.sleep(generator.nextInt(5000));

			System.out.println("Status: ONGOING\n");
			_job.setJobState(JobStateView.ONGOING);
			Thread.sleep(generator.nextInt(5000));

			System.out.println("Status: COMPLETED\n");
			_job.setJobState(JobStateView.COMPLETED);
			
			System.out.println("++++++++++++++++++++++++++++++++\n");
		}catch(InterruptedException e){

		}


	}
}
