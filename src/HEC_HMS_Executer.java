import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

public class HEC_HMS_Executer {

	// Hardcoded paths needed to run HEC_HMS and HEC_DSSVue

	private static String configFileForSiron =  "Siron_sub_basin.basin";
	public  static String workingFolderPAthForSiron= "C:\\Users\\Dragos\\Documents\\Water models\\Siron-new\\Siron\\";

	private static String workingFolderPathForHEC_HMS = "D:\\Program Files (x86)\\HEC\\HEC-HMS\\4.2.1\\";
	private static String runScriptForHEC_HMS = "run_compute.cmd";

	private static String workingFolderPathForHEC_DSSVue = "C:\\Users\\Dragos\\Documents\\Water models\\HEC-DSSVue 2.0.1\\";
	private static String runScriptForHEC_DSSVue = "ReadSironFlow.cmd";
	private static String runScriptForHEC_DSSVueObserved = "ReadSironFlowObserved.cmd";

	public static boolean DEBUG = false;

	double[] configParametersForSiron = null;
	
	
	// HARDCODDED lines that will be changed in "Siron_sub_basin.basin" with new values from each generation
	String[] configParametersNamesForSiron = {"     Curve Number: ","     Snyder Tp: ", "     Recession Factor: "};

	Vector<Double> flowDataObserved = null;
	boolean showPlot = false;

	
	// In some rare cases the .met file is broken and needs to be replaced. The only usage of this counter is for statistics
	public static int counterOfMetFileRegeneration = 0;

	
	// Create a new exception that will be caught in case the ".met" file is broken
	public class ExecCommandExecption extends Exception
	{
		public ExecCommandExecption(String message)
		{
			super(message);
		}
	}


	// constructor which receives the following parameters: parameters to run simulation on HEC-HMS, 
	// and a showPlot variable to determine if is necessary to create a plot of the output
	public  HEC_HMS_Executer(double[] configParametersForSiron, boolean showPlot)
	{
		this.configParametersForSiron = configParametersForSiron;
		this.showPlot = showPlot;
	}

	// The run function will run HEC-HMS, HED-DSSVue to create a simulation and obtain data
	// also if showPlot is set, will create a plot of the data after finnishing

	public synchronized double run()
	{

		double rms = Double.MAX_VALUE;

		
		try {
			

			// update configuration file "Siron_sub_basin.basin" with the new values of the parameters
			// The values should match the order in configParametersNamesForSiron list
			

			updateConfigurationFile();

			// execute HEC_HMS and in case the Met_1.met or Siron_sub_basin.basin are broken recreate them
			boolean runWithoutError = true;
			do 
			{
				try
				{
					
					execCommand(workingFolderPathForHEC_HMS, workingFolderPAthForSiron + runScriptForHEC_HMS);


				} catch (ExecCommandExecption e) {

					// Met file is broken need to fix it and try again
					// Recreate it from template Met_1_My.met

					Path metTemplateSource = Paths.get(workingFolderPAthForSiron, "Met_1_My.met");
					Path metFileDestination = Paths.get(workingFolderPAthForSiron, "Met_1.met");
					
					//update configuration file

					updateConfigurationFile();
					
					CopyOption[] options = new CopyOption[]{
						      StandardCopyOption.REPLACE_EXISTING,
						      StandardCopyOption.COPY_ATTRIBUTES
						    }; 
					
					// overwrite the met file  with the met file source (template)
					Files.copy(metTemplateSource, metFileDestination, options);
					e.printStackTrace();
					
					runWithoutError = false;
					counterOfMetFileRegeneration++;
				}
			}while(!runWithoutError);
			
			try {

				// execute HEC_DSS_VUE for simulated and observed data

				Vector<Double> flowData = null;
				Vector<String> outputStreamFromHEC_DSSVue;

				outputStreamFromHEC_DSSVue = execCommand(workingFolderPathForHEC_DSSVue, workingFolderPathForHEC_DSSVue + runScriptForHEC_DSSVue);
				flowData = parseDSSOutput(outputStreamFromHEC_DSSVue);

				if(flowDataObserved == null)
				{
					Vector<String> outputStreamFromHEC_DSSVueObserved = execCommand(workingFolderPathForHEC_DSSVue, workingFolderPathForHEC_DSSVue + runScriptForHEC_DSSVueObserved);
					flowDataObserved = parseDSSOutput(outputStreamFromHEC_DSSVueObserved);
				}

				// compute the error with RMSD
				rms = computeError(flowData, flowDataObserved);
				
				// show the plot of observed and simulated data 
				if(showPlot)
					new ChartCreater("RMSD  = " + rms , flowData, flowDataObserved);

			} catch (ExecCommandExecption e) {

				e.printStackTrace();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return rms;

	}


	// use the RMSD formula to compute the error
	private synchronized double computeError(Vector<Double> flowData, Vector<Double> flowDataObserved )
	{
		double rms = 0;
		for(int i= 0; i < flowData.size(); i++)
		{
			rms += Math.pow(flowData.get(i) - flowDataObserved.get(i), 2);
		}

		rms = rms/flowData.size();
		rms = Math.sqrt(rms);
		return rms;
	}



	// extract the data from the raw HEC-DSSVue output using data patterns
	private synchronized Vector<Double> parseDSSOutput(Vector<String> outputStream)
	{
		Vector<Double> data = new Vector<Double>();

		for(int i = 0; i < outputStream.size(); i++ )
		{

			if(outputStream.get(i).contains("20 April 1987") || outputStream.get(i).contains("21 April 1987") )
			{
				data.add(Double.parseDouble(outputStream.get(i).split(" ")[7]));
			}
		}

		return data;
	}


	// execute a command inside a working folder
	public synchronized Vector<String> execCommand(String workingFolderName, String executableName) throws IOException, ExecCommandExecption
	{
		if(DEBUG)
			System.out.println("Execute: " + workingFolderName + " " +  executableName );

		Vector<String> outputStream = new Vector<String>();

		ProcessBuilder pb = new ProcessBuilder(executableName);


		// set the working folder
		File workingFolder = new File(workingFolderName);
		pb.directory(workingFolder);

		Process proc = pb.start();


		BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

		BufferedReader stdError = new BufferedReader(new InputStreamReader(proc.getErrorStream()));

		// read the output from the command

		String s = null;


		if(DEBUG == true)
			System.out.println("Here is the standard output of the command:\n");

		while ((s = stdInput.readLine()) != null)
		{
			if( !s.equals(""))
			{
				if(DEBUG == true)
					System.out.println(s);
				outputStream.add(s);
			}
		}


		stdInput.close();

		// read data from standard error output from the attempted command
		if(DEBUG)
			System.out.println("Here is the standard error of the command (if any):\n");


		String errorMessage = "";
		while ((s = stdError.readLine()) != null)
		{
			System.out.println(s);
			errorMessage += s;
		}


		stdError.close();
		
		// wait for the process to close
		try {

			proc.waitFor();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		// if the error stream is not empty this might be caused by the fact that the met file and .subasin file are broken
		// throw exception so that we can fix it
		if(!errorMessage.equals(""))
			throw new ExecCommandExecption(errorMessage);
		return outputStream;

	}

	
	// update the configuration file by reading the data from a template 
	// and overwriting the configuration file with the new parameters
	// this method also fixes most of the cases when the .subasin configuaration file is broken
	
	private synchronized void updateConfigurationFile() throws IOException
	{

		if (configParametersForSiron == null)
			return;

		
		// obtain path objects for the configuation file and the template
		Path sironConfigFilePathSource = Paths.get(workingFolderPAthForSiron, "Siron_sub_basinMy.basin");
		Path sironConfigFilePathDestination = Paths.get(workingFolderPAthForSiron, configFileForSiron);

		// read all the data from the template
		List<String> fileContent = new ArrayList<>(Files.readAllLines(sironConfigFilePathSource, StandardCharsets.UTF_8));

		// change the values of the parameters for siron
		
		for (int i = 0; i < fileContent.size(); i++) 
		{
			for(int j = 0; j < configParametersNamesForSiron.length; j++)
			{
				if (fileContent.get(i).contains(configParametersNamesForSiron[j])) 
				{
					fileContent.set(i, configParametersNamesForSiron[j] + configParametersForSiron[j]);
					continue;
				}
			}
		}

		// write data to the .basin file
		Files.write(sironConfigFilePathDestination, fileContent, StandardCharsets.UTF_8);
	}

}
