package com.example.util

data class QariItem(
    val id: String,
    val nameEnglish: String,
    val nameBengali: String
)

object QariData {
    const val DEFAULT_QARI_ID = "ar.alafasy"

    val list: List<QariItem> = listOf(
        QariItem("ar.alafasy", "Mishary Rashid Alafasy", "মিশারি রশিদ আলাফাসি"),
        QariItem("ar.abdulbasitmurattal", "Abdul Basit Murattal", "আব্দুল বাসিদ মুরাত্তাল"),
        QariItem("ar.abdulbasitmujawwad", "Abdul Basit Mujawwad", "আব্দুল বাসিদ মুজাওওয়াদ"),
        QariItem("ar.abdullahbasfar", "Abdullah Basfar", "আব্দুল্লাহ বাসফার"),
        QariItem("ar.abdurrahmaansudais", "Abdurrahmaan As-Sudais", "আব্দুর রহমান আস-সুদাইস"),
        QariItem("ar.hudhaify", "Ali Al-Hudhaify", "আলী আল-হুদাইফি"),
        QariItem("ar.husary", "Mahmoud Khalil Al-Husary", "মাহমুদ খলিল আল-হুসারি"),
        QariItem("ar.husarymujawwad", "Mahmoud Khalil Al-Husary Mujawwad", "মাহমুদ খলিল আল-হুসারি মুজাওওয়াদ"),
        QariItem("ar.mahermuaiqly", "Maher Al Muaiqly", "মাহের আল-মুআইকিলী"),
        QariItem("ar.minshawi", "Mohamed Siddiq al-Minshawi", "মুহাম্মাদ সিদ্দিক আল-মিনশাবি"),
        QariItem("ar.minshawimujawwad", "Mohamed Siddiq al-Minshawi Mujawwad", "মুহাম্মাদ সিদ্দিক আল-মিনশাবি মুজাওওয়াদ"),
        QariItem("ar.muhammadayyoub", "Muhammad Ayyoub", "মুহাম্মাদ আইয়ুব"),
        QariItem("ar.muhammadjibreel", "Muhammad Jibreel", "মুহাম্মাদ জিবরিল"),
        QariItem("ar.saoodshuraym", "Saood as-Shuraym", "সাউদ আশ-শুরাইম"),
        QariItem("ar.parhizgar", "Shahriar Parhizgar", "শাহরিয়ার পারহিজগার"),
        QariItem("ar.hanirifai", "Hani Ar-Rifai", "হানি আর-রিফাই"),
        QariItem("ar.ahmedajamy", "Ahmed ibn Ali al-Ajamy", "আহমেদ ইবনে আলী আল-আজমী"),
        QariItem("ar.yasserdosari", "Yasser Al-Dosari", "ইয়াসির আদ-দোসারী"),
        QariItem("ar.nasseralqatami", "Nasser Al Qatami", "নাসের আল কাতামী"),
        QariItem("ar.khalifaaltunaiji", "Khalifa Al Tunaiji", "খলিফা আল তুনাইজি"),
        QariItem("ar.ibrahimakhdar", "Ibrahim Al Akhdar", "ইব্রাহিম আল আখদার"),
        QariItem("ar.salahbudair", "Salah Al Budair", "সলাহ আল বুদাইর"),
        QariItem("ar.saadalgahmadi", "Saad Al-Ghamdi", "সাদ আল-গামদী"),
        QariItem("ar.faresabbad", "Fares Abbad", "ফারিস আব্বাদ")
    )

    fun getQariNameEnglish(id: String): String {
        return list.find { it.id == id }?.nameEnglish ?: "Mishary Rashid Alafasy"
    }

    fun getQariNameBengali(id: String): String {
        return list.find { it.id == id }?.nameBengali ?: "মিশারি রশিদ আলাফাসি"
    }

    fun getQariDisplayName(id: String): String {
        val item = list.find { it.id == id }
        return if (item != null) "${item.nameEnglish} (${item.nameBengali})" else "Mishary Rashid Alafasy"
    }
}
