package com.github.SE4AIResearch.technicaldebt_plugin_fall2024.SATD;

import satd_detector.core.utils.SATDDetector;

/**
 * Maintains a wrapper implementation for the SATDDetector project:
 * https://github.com/Tbabm/SATDDetector-Core
 */
public class SATDDetectorImpl implements com.github.SE4AIResearch.technicaldebt_plugin_fall2024.SATD.SATDDetector {

    // Wrapper implementation for this implementation
    private SATDDetector detector;

    public SATDDetectorImpl() {
        this.detector = new SATDDetector();
    }

    @Override
    public boolean isSATD(String satd) {
        return this.detector.isSATD(satd);
    }
}