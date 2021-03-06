package edu.cmu.ml.proppr.learn;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import edu.cmu.ml.proppr.Trainer;
import edu.cmu.ml.proppr.examples.PosNegRWExample;
import edu.cmu.ml.proppr.util.math.ParamVector;
import edu.cmu.ml.proppr.util.SRWOptions;
import edu.cmu.ml.proppr.util.StatusLogger;
import edu.cmu.ml.proppr.util.math.SimpleParamVector;
import gnu.trove.iterator.TIntDoubleIterator;
import gnu.trove.map.TIntDoubleMap;

/**
 * The AdaGrad version of SRW (see below for description)
 * @author rosecatherinek
 * 
 * 
 * Random walk learning
 * 
 * Flow of information:
 * 
 * 	 Train on example =
 *     load (initialize example parameters and compute M/dM)
 *     inference (compute p/dp)
 *     sgd (compute empirical loss gradient and apply to parameters)
 * 
 *   Accumulate gradient = 
 *     load  (initialize example parameters and compute M/dM)
 *     inference (compute p/dp)
 *     gradient (compute empirical loss gradient)
 * 
 * @author krivard
 *
 */
public class AdaGradSRW extends SRW {	
	private static final Logger log = Logger.getLogger(AdaGradSRW.class);
	private static final double MIN_GRADIENT = Math.sqrt(Double.MIN_VALUE)*10;
	// This makes AdaGradSRW stateful, but SRW should only ever be used by one thread at a time
	private ParamVector<String,?> totSqGrad = null;
	public AdaGradSRW() {
		super(new SRWOptions());
	}
	public AdaGradSRW(SRWOptions params) {
		super(params);
		totSqGrad = new SimpleParamVector<String>(new ConcurrentHashMap<String,Double>());
	}
    public void setTotSqGrad(ParamVector<String,?> t) {
    	this.totSqGrad = t;
    }
    
    @Override
	public SRW copy() {
    	SRW cop = super.copy();
    	((AdaGradSRW) cop).setTotSqGrad(this.totSqGrad);
    	return cop;
    }

	/**
	 * Modify the parameter vector by taking a gradient step along the dir suggested by this example.
	 * 
	 * AdaGrad: use the adaptive learning rate
	 * 
	 * @param params
	 * @param example
	 */
    @Override
	public void trainOnExample(ParamVector<String,?> params, PosNegRWExample example, StatusLogger status) {
		if (log.isDebugEnabled()) 
			log.debug("Training on "+example);
		else if (log.isInfoEnabled() && status.due(2))
			log.info(Thread.currentThread()+" Training on "+example);
		
		initializeFeatures(params, example.getGraph());
		regularizer.prepareForExample(params, example.getGraph(), params);
		load(params, example);
		inference(params, example, status);
		agd(params, example);
	}

    @Override
	protected double learningRate(String feature) {
	    if (!totSqGrad.containsKey(feature)) return 0.0;
		return c.eta / Math.sqrt(this.totSqGrad.get(feature));
	}

	/**
	 * AdaGrad Descent Algo
	 * 
	 * edits params using totSqGrad as well
	 * 
	 * @author rosecatherinek
	 */
	protected void agd(ParamVector<String,?> params, PosNegRWExample ex) {
		TIntDoubleMap gradient = gradient(params,ex);
		// apply gradient to param vector
		for (TIntDoubleIterator grad = gradient.iterator(); grad.hasNext(); ) {
			grad.advance();
			// avoid underflow since we're summing the square
			if (Math.abs(grad.value())<MIN_GRADIENT) continue;
			String feature = ex.getGraph().featureLibrary.getSymbol(grad.key());

			if (trainable(feature)){
				Double g = grad.value();
				
				//first update the running total of the square of the gradient
				totSqGrad.adjustValue(feature, g * g);
				
				//now get the running total
//				Double rt = totSqGrad.get(feature);
				
				//w_{t+1, i} = w_{t, i} - \eta * g_{t,i} / \sqrt{ G,i }
				
//				Double descentVal = - c.eta * g / Math.sqrt(rt);

				params.adjustValue(feature, - learningRate(feature) * g);
				
				if (params.get(feature).isInfinite()) {
					log.warn("Infinity at "+feature+"; gradient "+grad.value()+"; rt "+totSqGrad.get(feature));
				}
			}
		}
	}

}
