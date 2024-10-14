package com.polyhedraldevelopment.seismic.algorithms.sampler.arithmetic;

import com.polyhedraldevelopment.seismic.algorithms.sampler.Sampler;


public class AdditionSampler extends BinaryArithmeticSampler {
    public AdditionSampler(Sampler left, Sampler right) {
        super(left, right);
    }

    @Override
    public double operate(double left, double right) {
        return left + right;
    }

    @Override
    public double[] operateDerivative(double[] left, double[] right) {
        int dimensions = left.length;
        double[] out = new double[dimensions];
        for(int i = 0; i < dimensions; i++) {
            out[i] = left[i] + right[i];
        }
        return out;
    }
}