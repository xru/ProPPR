package edu.cmu.ml.praprolog.learn;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import edu.cmu.ml.praprolog.util.MuParamVector;
import edu.cmu.ml.praprolog.util.ParamVector;
import edu.cmu.ml.praprolog.util.SimpleParamVector;

public class LocalL2PosNegLossSRWTest extends L2PosNegLossSRWTest {
	@Override
	public void initSrw() {
		srw = new LocalL2PosNegLossTrainedSRW();
		srw.setMu(0);
	}
	@Override	
	public ParamVector makeParams(Map<String,Double> foo) {
		return new MuParamVector(foo);
	}
	@Override
	public ParamVector makeParams() {
		return new MuParamVector();
	}

}
