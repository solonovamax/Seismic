/*
 * Copyright (c) 2020-2024 Polyhedral Development
 *
 * The Terra Core Addons are licensed under the terms of the MIT License. For more details,
 * reference the LICENSE file in this module's root directory.
 */

package com.polyhedraldevelopment.seismic.algorithms.normalizer;


import com.polyhedraldevelopment.seismic.type.sampler.Sampler;


/**
 * Normalizer to linearly scale data's range.
 */
public class LinearNormalizer extends Normalizer {
    private final double min;
    private final double max;

    public LinearNormalizer(Sampler sampler, double min, double max) {
        super(sampler);
        this.min = min;
        this.max = max;
    }

    @Override
    public double normalize(double in) {
        return (in - min) * (2 / (max - min)) - 1;
    }
}
