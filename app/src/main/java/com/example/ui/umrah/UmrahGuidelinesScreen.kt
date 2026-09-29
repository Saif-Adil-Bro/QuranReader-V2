package com.example.ui.umrah

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UmrahGuidelinesScreen(
    isEnglish: Boolean = false,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEnglish) "Umrah Guidelines" else "উমরাহ নির্দেশিকা",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEnglish) "Back" else "ফিরে যান",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F181F)
                )
            )
        },
        containerColor = Color(0xFF0F181F)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF16232D)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(Color(0xFF10B981).copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = if (isEnglish) "What is Umrah & Its Significance" else "উমরাহ কী ও এর গুরুত্ব",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isEnglish)
                                    "Umrah is an intensely blessed Sunnah and voluntary worship. The Prophet (PBUH) said: 'An Umrah to another Umrah is an expiation for the sins committed between them.' (Sahih al-Bukhari)"
                                else
                                    "উমরাহ একটি অত্যন্ত বরকতময় সুন্নাত ও নফল ইবাদত। রাসূলুল্লাহ (সা.) বলেছেন: 'এক উমরাহর পর আরেক উমরাহ এদের মধ্যবর্তী সকল গুনাহের কাফফারা স্বরূপ।' (সহীহ বুখারী)",
                                fontSize = 13.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = if (isEnglish) "4 Essential Pillars / Steps of Umrah" else "উমরার ৪টি মৌলিক রুকন / স্তর",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            val steps = if (isEnglish) {
                listOf(
                    Pair("1. Ihram & Niyyah", "Cleanse and bathe, put on unstitched Ihram garments before crossing the Miqat, formulate the intention (Niyyah), and begin reciting the Talbiyah."),
                    Pair("2. Tawaf of Ka'bah (7 Circuits)", "Starting from the Black Stone corner with Ka'bah on your left, circumambulate anti-clockwise for 7 circuits."),
                    Pair("3. Sa'i between Safa & Marwah (7 Laps)", "Commence at Mount Safa and finish at Mount Marwah, completing 7 laps total of walking/jogging."),
                    Pair("4. Halq or Qasr (Exiting Ihram)", "Men shave their head completely (Halq) or trim hair evenly (Qasr), while women trim about a fingertip length to release Ihram.")
                )
            } else {
                listOf(
                    Pair("১. ইহরাম ও নিয়ত", "মীকাত অতিক্রম করার পূর্বেই গোসল/ওযু করে সেলাইবিহীন ইহরাম পরিধান করে নিয়ত ও তালবিয়াহ পাঠ শুরু করা।"),
                    Pair("২. কাবার তাওয়াফ (৭ চক্কর)", "হাজরে আসওয়াদ কোণ থেকে শুরু করে বাইতুল্লাহকে বামে রেখে ঘড়ির কাঁটার বিপরীত দিকে ৭ চক্কর প্রদক্ষিণ করা।"),
                    Pair("৩. সাফা ও মারওয়া সাঈ (৭ চক্কর)", "সাফা পাহাড় থেকে শুরু করে মারওয়া পাহাড়ে মোট ৭ চক্কর দৌড়ানো/হাঁটা সম্পন্ন করা।"),
                    Pair("৪. হলক বা কসর", "পুরুষদের পুরো মাথা ন্যাড়া করা বা সমানভাবে চুল ছাঁটা এবং মহিলাদের আঙুলের এক কর পরিমাণ চুল কেটে ইহরাম ত্যাগ করা।")
                )
            }

            items(steps.size) { index ->
                val (title, desc) = steps[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A2733)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = desc,
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            item {
                Text(
                    text = if (isEnglish) "Important Mawaqit (Miqat Boundaries)" else "গুরুত্বপূর্ণ মীকাতসমূহ (Mawaqit)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            val mawaqit = if (isEnglish) {
                listOf(
                    Pair("Dhul Hulayfah (For Medina travelers)", "Located on the way from Medina to Mecca (now known as Masjid ash-Shajarah or Abyar Ali)."),
                    Pair("Al-Juhfah (For Syria, Egypt & West)", "Near Rabigh. Air travelers flying to Jeddah enter Ihram prior to crossing this airspace."),
                    Pair("Qarn al-Manazil (For Riyadh & Gulf)", "Near Ta'if (known as Al-Sayl al-Kabir)."),
                    Pair("Yalamlam (For Yemen & South Asia)", "For pilgrims from Bangladesh, India, Pakistan flying direct to Jeddah, Ihram is entered before boarding or before crossing this aerial marker."),
                    Pair("Dhat 'Irq (For Iraq & East)", "North-eastern Miqat station for overland/eastern routes.")
                )
            } else {
                listOf(
                    Pair("যুল হুলায়ফা (মদিনাবাসীদের জন্য)", "মদিনা থেকে মক্কার পথে অবস্থিত (বর্তমানে মসজিদ আশ-শাজারাহ বা আবইয়ার আলী নামে পরিচিত)।"),
                    Pair("আল-জুহফাহ (সিরিয়া ও মিশরের জন্য)", "রাবেগ শহরের নিকটবর্তী। বিমানযাত্রীরা জেদ্দা অবতরণের পূর্বে এই আকাশসীমা অতিক্রম করার আগেই ইহরাম বাঁধেন।"),
                    Pair("ক্বারনুল মানাযিল (রিয়াদ ও উপসাগরীয়দের জন্য)", "তায়েফের নিকটবর্তী আল-সায়ল আল-কবীর।"),
                    Pair("ইয়ালামলাম (ইয়েমেন ও দক্ষিণ এশিয়ার জন্য)", "বাংলাদেশ, ভারত, পাকিস্তান থেকে যারা বিমানে সরাসরি জেদ্দা যান, তারা সাধারণত বিমানে ওঠার আগে বা আকাশপথে এই মীকাত অতিক্রমের আগেই ইহরাম পরিধান করেন।"),
                    Pair("যাতু ইরক (ইরাক ও ইরানবাসীদের জন্য)", "মক্কার উত্তর-পূর্বাঞ্চলীয় মীকাত পয়েন্ট।")
                )
            }

            items(mawaqit.size) { index ->
                val (name, info) = mawaqit[index]
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF16232D)
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = info,
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8),
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}
