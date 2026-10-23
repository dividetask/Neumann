package neumann.core.data

/** Supplies the text of a data file by name (e.g. "rules.yaml"). */
fun interface DataSource {
    fun read(fileName: String): String
}
