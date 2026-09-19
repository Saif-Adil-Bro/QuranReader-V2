import java.net.URL
import java.io.InputStreamReader
import java.io.BufferedReader

fun main() {
    val url = URL("https://api.quran.com/api/v4/verses/by_chapter/67?words=true&word_fields=text_uthmani&per_page=1000")
    val connection = url.openConnection()
    connection.setRequestProperty("Accept", "application/json")
    val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
    var line: String? = reader.readLine()
    val sb = StringBuilder()
    while (line != null) {
        sb.append(line)
        line = reader.readLine()
    }
    val json = sb.toString()
    println("Ayah 2 words count: " + json.split("\"verse_number\":2")[1].split("\"words\":[")[1].split("]")[0].split("},{").size)
}
