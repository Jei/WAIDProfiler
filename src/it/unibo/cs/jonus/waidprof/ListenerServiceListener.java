/**
 * 
 */
package it.unibo.cs.jonus.waidprof;

/**
 * @author jei
 *
 */
public interface ListenerServiceListener {
	
	public void sendCurrentEvaluation(VehicleInstance evaluation);
	
	public void sendPredictedVehicle(String vehicle);

}
