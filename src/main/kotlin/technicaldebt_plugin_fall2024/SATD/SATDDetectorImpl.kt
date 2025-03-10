package technicaldebt_plugin_fall2024.SATD

import satd_detector.core.utils.SATDDetector

/**
 * Maintains a wrapper implementation for the SATDDetector project:
 * https://github.com/Tbabm/SATDDetector-Core
 */
class SATDDetectorImpl : SATDDetector() {

    // Wrapper implementation for this implementation
    private val detector: SATDDetector = SATDDetector()

    override fun isSATD(satd: String): Boolean {
        return detector.isSATD(satd)
    }
}