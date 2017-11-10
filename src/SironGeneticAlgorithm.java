
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.moeaframework.Executor;
import org.moeaframework.util.Vector;

import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.core.variable.RealVariable;
import org.moeaframework.problem.AbstractProblem;

public class SironGeneticAlgorithm {

	public static class MyHidroClass extends AbstractProblem {

				
		// Define 1 objective and 3 decision variables to correspond to the following parameters:
		
		// SCS Curve Number – Curve Number limitari init 59.2 50 72

		//Snyder Unit Hydrograph- Snyder Lag hr 1.97 1.64 2.46

		//Recession – Recession Constant n/a 0.25 0.01 1
		
		public MyHidroClass() {
			super(3, 1);
		}

		/**
		 * Constructs a new solution and defines the bounds of the decision
		 * variables.
		 */

		@Override
		public Solution newSolution() {
			Solution solution = new Solution(getNumberOfVariables(), 
					getNumberOfObjectives());

			solution.setVariable(0, new RealVariable(59.2,   50,   72  ));
			solution.setVariable(1, new RealVariable(1.97, 1.64, 2.46  ));
			solution.setVariable(2, new RealVariable(0.25, 0.01,    1  ));


			return solution;
		}

		/**
		 * Extracts the decision variables from the solution, evaluates the
		 * fitness of the function, and saves the resulting objective value back to
		 * the solution. 
		 */
		@Override
		public  void evaluate(Solution solution) {
			synchronized(this)
			{
				
				// sExtracts the decision variables from the solution
				double[] configParametersForSiron = EncodingUtils.getReal(solution);
				double[] f = new double[numberOfObjectives];

				
				// evaluates the fitness of the function. In this particular case we use RMSD
				HEC_HMS_Executer hydroSystemComunicator = new HEC_HMS_Executer(configParametersForSiron,false);

				// f[0] will contain the resulting objective value
				f[0] = hydroSystemComunicator.run();

				System.out.println(f[0]);

				solution.setObjectives(f);

			}
		}

	}


	// this will extract the solution and print the values of the parameters
	
	static void printSolution(Solution solution)
	{
		double[] x = EncodingUtils.getReal(solution);

		
		for(int i= 0; i < x.length; i++)
		{
					System.out.format("%.4f %n", x[i]);
		}

	}
	
	
	// To Obtain a graphic representation of the solution the HEC_HMS_Executer class can 
	// create a plot with the data of observed and simulated data from the model
	public static void showPlot(double[] configParametersForSiron)
	{
		// setting DEBUG to true will print every output of every batch command
		HEC_HMS_Executer.DEBUG = false;
		// setting true the second parameter will make HEC_HMS to create a plot of the data  
		HEC_HMS_Executer hecHmsExecutor = new HEC_HMS_Executer(configParametersForSiron, true);

		double rms = hecHmsExecutor.run();
	}

	public static void main(String[] args) {
		
		// to create plots run the function showPlot with the following array of parameters
		
		double[][] configParametersForSiron = {
		//		{59.2,1.97,0.25}, // initial
		//		{51.3011 ,1.9525 ,0.3333}, //100
		//		{50.3080,1.8026,0.2616}, //500
		//		{50.0037,1.8243 ,0.2577}, // 1000
		//		{50.0002,1.8026 , 0.2476} // 5000 
		};
		
		
		for(int i  = 0; i < configParametersForSiron.length; i++)
		{
			showPlot(configParametersForSiron[i]);
		}

		// use startTime and endTime to compute the run time of the program
		
		long startTime = System.currentTimeMillis();

		// execute NSGAII with a certain amount of evaluations
		HEC_HMS_Executer.DEBUG = false;
		NondominatedPopulation result = new Executor()
				.withProblemClass(MyHidroClass.class)
				.withAlgorithm("NSGAII")
				.withMaxEvaluations(1)
				.run();

		//display the results
		System.out.format("Final Result %n");

		for (Solution solution : result) {
			System.out.format("%.4f %n",solution.getObjective(0));
			printSolution(solution);
		}
		
		long endTime   = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		
		// print the time in seconds
		System.out.format("totalTime : %d %n", totalTime/1000);
		
		System.out.println("counterOfMetFileRegeneration: " + HEC_HMS_Executer.counterOfMetFileRegeneration);

		

	}

}
