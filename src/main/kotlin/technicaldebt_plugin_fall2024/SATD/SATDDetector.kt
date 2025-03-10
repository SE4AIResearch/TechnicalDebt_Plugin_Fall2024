package technicaldebt_plugin_fall2024.SATD

interface SATDDetector {

    /**
     * Given a string comment, determine if the comment contains any SATD
     * @param satd a String comment
     * @return True if the comment contains any SATD, else False
     */
    fun isSATD(satd: String): Boolean
}