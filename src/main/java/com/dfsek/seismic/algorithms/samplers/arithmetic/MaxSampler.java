package com.dfsek.seismic.algorithms.samplers.arithmetic;

import com.dfsek.seismic.algorithms.samplers.NoiseSampler;


public class MaxSampler extends BinaryArithmeticSampler {
    public MaxSampler(NoiseSampler left, NoiseSampler right) {
        super(left, right);
    }

    @Override
    public double operate(double left, double right) {
        return Math.max(left, right);
    }
}
