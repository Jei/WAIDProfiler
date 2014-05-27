/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

/**
 * @author jei
 *
 */
public interface ListenerServiceListener {
	
	public void sendCurrentEvaluation(Evaluation evaluation);
	
	public void sendPredictedVehicle(String vehicle);

}
