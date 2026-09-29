package com.example.data.umrah

object UmrahContentData {

    // 1. IHRAM PREPARATION CHECKLIST (9 Items)
    val IHRAM_CHECKLIST_BN = listOf(
        "নখ, মোচ ও শরীরের অতিরিক্ত পশম কেটে উত্তমরূপে গোসল করুন।",
        "শরীরে আতর লাগান। ইহরামের কাপড়ে আতর লাগাবেন না।",
        "ইহরামের লুঙ্গি ও চাদর পরে নিন। উভয় কাঁধ ঢেকে চাদর পরুন।",
        "মহিলাগণ স্বাভাবিক কাপড় পরবেন। চেহারার পর্দার জন্য ক্যাপ-বিশিষ্ট নেকাব ব্যবহার করুন।",
        "মাথা ঢেকে ইহরামের নিয়তে দু'রাকাত নামায পড়ুন। এই দু'রাকাত নামায পড়া মুস্তাহাব।",
        "নামাযের পর পুরুষরা মাথা থেকে কাপড় সরিয়ে নিন।",
        "ওমরার নিয়ত করে মৃদু আওয়াজে তালবিয়া পড়ুন। মহিলাগণ নিচু আওয়াজে পড়ুন।",
        "দরূদ শরীফ পড়ুন।",
        "ইহরামের পর থেকে বেশি বেশি তালবিয়া পড়ুন।"
    )

    val IHRAM_CHECKLIST_EN = listOf(
        "Clip nails, trim mustache, remove unwanted body hair, and perform a full ghusl (bath).",
        "Apply perfume on your body (do not apply perfume on the Ihram garments).",
        "Put on the Ihram garments (Izār and Ridā'). Keep both shoulders covered initially.",
        "Women wear normal modest clothing, using a cap-style niqab so fabric does not touch the face.",
        "Perform 2 rak'ahs sunnah prayer with head covered before making the Ihram intention.",
        "After prayer, men uncover their heads.",
        "Make the intention (Niyyah) for Umrah and recite the Talbiyah softly (women recite quietly).",
        "Send peace and blessings upon the Prophet (Salawat / Durood).",
        "Recite the Talbiyah frequently throughout your journey until reaching the Ka'bah."
    )

    val IHRAM_CHECKLIST = IHRAM_CHECKLIST_BN

    // Ihram Niyyah & Talbiyah
    const val IHRAM_NIYYAH_ARABIC = "لَبَّيْكَ اللَّهُمَّ عُمْرَةً"
    const val IHRAM_NIYYAH_BN = "হে আল্লাহ, আমি উমরার জন্য আপনার ডাকে সাড়া দিচ্ছি।"
    const val IHRAM_NIYYAH_EN = "Here I am, O Allah, for Umrah."

    const val TALBIYAH_ARABIC = "لَبَّيْكَ اللَّهُمَّ لَبَّيْكَ، لَبَّيْكَ لَا شَرِيكَ لَكَ لَبَّيْكَ، إِنَّ الْحَمْدَ وَالنِّعْمَةَ لَكَ وَالْمُلْكَ، لَا شَرِيكَ لَكَ"
    const val TALBIYAH_BN = "আমি হাজির হে আল্লাহ, আমি হাজির। আমি হাজির, আপনার কোন শরীক নেই, আমি হাজির। নিশ্চয়ই সমস্ত প্রশংসা ও নেয়ামত আপনারই এবং রাজত্বও আপনারই, আপনার কোন শরীক নেই।"
    const val TALBIYAH_EN = "Here I am at Your service, O Allah, here I am. Here I am, You have no partner, here I am. Truly, all praise, grace, and sovereignty belong to You. You have no partner."

    // Ihram Prohibitions (নিষিদ্ধ কার্যাবলী)
    val IHRAM_PROHIBITIONS_BN = listOf(
        Pair("চুল ও নখ কাটা বা ছাঁটা", "ইহরাম অবস্থায় শরীরের কোনো অংশের চুল, দাড়ি, মোচ বা নখ কাটা কিংবা উপড়ানো সম্পূর্ণ নিষিদ্ধ।"),
        Pair("সুগন্ধি বা আতর ব্যবহার", "ইহরাম বাঁধার পর শরীরে, কাপড়ে বা খাবারে সুগন্ধি, আতর, সেন্ট বা সুগন্ধিযুক্ত সাবান ব্যবহার করা যাবে না।"),
        Pair("পুরুষদের জন্য সেলাই করা কাপড়", "পুরুষদের শরীরের অঙ্গের মাপে তৈরিকৃত সেলাইযুক্ত জামা, প্যান্ট, গেঞ্জি, আন্ডারওয়্যার ইত্যাদি পরা নিষিদ্ধ। কেবল দুটি সেলাইবিহীন চাদর পরিধান করতে হবে।"),
        Pair("মাথা বা মুখমণ্ডল ঢাকা (পুরুষদের জন্য)", "পুরুষদের মাথা, মুখ বা কান টুপি, পাগড়ি বা চাদর দিয়ে ঢেকে রাখা নিষিদ্ধ।"),
        Pair("মহিলাদের মুখ কাপড়ে স্পর্শ করানো", "মহিলাদের স্বাভাবিক সেলাই করা পোশাক পরিধান বৈধ, তবে মুখমণ্ডলে স্পর্শ করে এমন পর্দা করা নিষিদ্ধ। প্রয়োজনে ক্যাপযুক্ত নেকাব ব্যবহার করতে হবে যাতে কাপড় মুখে না লাগে।"),
        Pair("স্ত্রী সহবাস ও যৌন আচরণ", "ইহরাম অবস্থায় সহবাস, স্পর্শ, আলিঙ্গন বা যৌন উদ্দীপক কোনো কথাবার্তা বা আচরণ সম্পূর্ণরূপে নিষিদ্ধ।"),
        Pair("স্থলচর প্রাণী শিকার করা", "যেকোনো বন্য প্রাণী শিকার করা, তাড়ানো বা শিকারে কাউকে সহায়তা করা সম্পূর্ণ হারাম।"),
        Pair("ঝগড়া-বিবাদ ও অশ্লীল কথা", "কারো সাথে বিবাদ, গালাগালি, মন্দ কথা বা কোনো পাপ কাজে লিপ্ত হওয়া কঠোরভাবে নিষিদ্ধ।"),
        Pair("পুরুষদের জন্য ঢাকা জুতো পরা", "পুরুষদের পায়ের পাতার ওপরের উঁচু হাড় ও গোড়ালি ঢেকে যায় এমন বুট বা বন্ধ জুতো পরা নিষিদ্ধ। এমন স্যান্ডেল পরতে হবে যাতে পায়ের পাতা ও গোড়ালি উন্মুক্ত থাকে।")
    )

    val IHRAM_PROHIBITIONS_EN = listOf(
        Pair("Cutting Hair and Nails", "Cutting, trimming, or plucking hair from any part of the body and clipping nails is strictly prohibited in Ihram."),
        Pair("Using Perfume or Scent", "Applying perfume, scented soaps, attar, or scented lotions to the body or garments is prohibited after entering Ihram."),
        Pair("Sewn Clothes (For Men)", "Men are prohibited from wearing stitched/tailored clothes conforming to limbs (shirts, trousers, underwear). Only two unstitched white cloths are permitted."),
        Pair("Covering Head and Face (For Men)", "Men must not cover their head, face, or ears with hats, turbans, caps, or cloths."),
        Pair("Touching Face with Cloth (For Women)", "Women wear normal clothes but must not wear a tight face-covering veil that directly touches the facial skin."),
        Pair("Marital Relations & Intimacy", "Sexual intercourse, intimate touching, romantic foreplay, or sensual speech is strictly forbidden."),
        Pair("Hunting Land Game", "Hunting, chasing, harming wild land animals, or aiding in hunting is strictly forbidden."),
        Pair("Arguments & Foul Speech", "Quarreling, using abusive language, swearing, arguing, or committing sins is forbidden."),
        Pair("Covered Footwear (For Men)", "Men must not wear closed shoes or boots that cover the ankles and instep bone. Footwear should leave the top arch and ankles exposed.")
    )

    val IHRAM_PROHIBITIONS = IHRAM_PROHIBITIONS_BN

    // 2. TAWAF CHECKLISTS
    val TAWAF_PREP_CHECKLIST_BN = listOf(
        "প্রস্তুতি গ্রহণ",
        "পবিত্রতার সহিত ওযু করে নিন।",
        "হাজরে আসওয়াদের কোণ বরাবর দাঁড়ান।",
        "উমরার তাওয়াফের নিয়ত করে নিন।",
        "তাওয়াফের শুরুতেই তালবিয়া পড়া বন্ধ করুন।",
        "তাওয়াফ শুরুর আগে ইযতিবা তথা ডান কাঁধ উন্মুক্ত রেখে চাদরটি বাম কাঁধের ওপর রাখুন (পুরুষদের জন্য সুন্নত)।"
    )

    val TAWAF_PREP_CHECKLIST_EN = listOf(
        "Preparation & Readiness",
        "Ensure you are in a state of Wudu (ritual purity).",
        "Align yourself facing the corner of the Black Stone (Hajar al-Aswad).",
        "Make the silent intention (Niyyah) in your heart for Umrah Tawaf.",
        "Stop reciting the Talbiyah before beginning Tawaf.",
        "Perform Idtiba' (expose your right shoulder by passing the upper garment under the right armpit and over the left shoulder - for men)."
    )

    val TAWAF_PREP_CHECKLIST = TAWAF_PREP_CHECKLIST_BN

    val TAWAF_ROUND_CHECKLIST_BN = listOf(
        "এরপর 'বিসমিল্লাহি আল্লাহু আকবার' বলে হাজরে আসওয়াদের দিকে হাত দ্বারা ইশারা (ইস্তিলাম) করুন এবং হাতের তালুতে চুমু দিন।",
        "সবুজ বাতির সমান্তরালে বাইতুল্লাহকে নিজের বাম দিকে রেখে তাওয়াফ শুরু করুন।",
        "প্রথম ৩ চক্করে পুরুষদের রমল করা সুন্নত—কাঁধ দুলিয়ে কিছুটা বীরদর্পে দ্রুত গতিতে চলুন (লাফাবেন না)।",
        "পরের ৪ চক্করে স্বাভাবিক গতিতে হাঁটুন। তবে ৭ চক্কর শেষ হওয়া পর্যন্ত ইযতিবা বজায় রাখুন।",
        "প্রতি চক্করে রুকনে ইয়ামানী ও হাজরে আসওয়াদের মাঝের অংশে নির্ধারিত দোয়া পড়ুন এবং হাজরে আসওয়াদে ইস্তিলাম করুন।"
    )

    val TAWAF_ROUND_CHECKLIST_EN = listOf(
        "Point your right hand towards the Black Stone saying 'Bismillāhi Allāhu Akbar' (Istilām) and kiss your palm.",
        "Keep the Ka'bah to your left side and begin circumambulating anti-clockwise.",
        "Men perform Ramal during the first 3 circuits—brisk, spirited walking with quick steps and puffed chest (without running).",
        "Walk normally during the remaining 4 circuits, maintaining Idtiba' until the 7th circuit is finished.",
        "Recite the recommended Du'a between Rukn al-Yamani and the Black Stone, performing Istilam at each lap."
    )

    val TAWAF_ROUND_CHECKLIST = TAWAF_ROUND_CHECKLIST_BN

    const val TAWAF_RUKN_YAMANI_DUA_ARABIC = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ"
    const val TAWAF_RUKN_YAMANI_DUA_BN = "হে আমাদের রব! আপনি আমাদেরকে দুনিয়াতে কল্যাণ দান করুন এবং আখেরাতেও কল্যাণ দান করুন এবং আমাদেরকে জাহান্নামের আগুন থেকে রক্ষা করুন।"
    const val TAWAF_RUKN_YAMANI_DUA_EN = "Our Lord, give us in this world that which is good and in the Hereafter that which is good, and protect us from the punishment of the Fire."

    val TAWAF_FINAL_CHECKLIST_BN = listOf(
        "সপ্তম চক্করের পর অষ্টম বার হাজরে আসওয়াদকে ইশারা (ইস্তিলাম) করুন। এর মাধ্যমে তাওয়াফ সম্পন্ন হলো। এরপর উভয় কাঁধ ঢেকে নিন।",
        "মাকামে ইবরাহীমের পেছনে (বা মাতাফ/মসজিদুল হারামের যেকোনো সুবিধাজনক স্থানে) দু'রাকাত ওয়াজিব তাওয়াফ নামায আদায় করুন।",
        "যমযম পানি দাঁড়িয়ে কাবার দিকে মুখ করে তৃপ্তি সহকারে পান করুন এবং মাথায় সামান্য পানি মাখুন।"
    )

    val TAWAF_FINAL_CHECKLIST_EN = listOf(
        "After the 7th circuit, perform Istilam towards the Black Stone for the 8th time to conclude Tawaf. Cover both shoulders.",
        "Offer 2 rak'ahs of Wajib Tawaf prayer behind Maqam Ibrahim (or anywhere accessible within the Grand Mosque).",
        "Drink Zamzam water generously facing the Ka'bah, supplicate with heartfelt prayers, and wipe some over your head."
    )

    val TAWAF_FINAL_CHECKLIST = TAWAF_FINAL_CHECKLIST_BN

    // 3. SA'I CHECKLISTS
    val SAI_PREP_CHECKLIST_BN = listOf(
        "সাঈর জন্য প্রস্তুত হয়ে সাফা পাহাড়ে গমন করুন।",
        "সাফা পাহাড়ে উঠে কাবার দিকে মুখ করে হাত তুলে তাহলীল ও তাকবীর বলুন।",
        "সাফা ও মারওয়ার পবিত্র আয়াত ও সুন্নাহসম্মত দোয়া পাঠ করে আন্তরিক মোনাজাত করুন।"
    )

    val SAI_PREP_CHECKLIST_EN = listOf(
        "Proceed to Mount Safa for Sa'i with focus and reverence.",
        "Climb Safa until Ka'bah is visible (or oriented towards it), raise your hands, proclaim Takbir and Tahlil.",
        "Recite the Qur'anic verse of Safa & Marwah and make heartfelt personal supplications 3 times."
    )

    val SAI_PREP_CHECKLIST = SAI_PREP_CHECKLIST_BN

    val SAI_ROUND_CHECKLIST_BN = listOf(
        "সাফা থেকে মারওয়ার দিকে শান্তভাবে হাঁটা শুরু করুন (১ম চক্কর)।",
        "সবুজ বাতি (মাইলান আখদারাইন) চিহ্নিত অংশে পুরুষরা মাঝারি গতিতে দৌড়ান/দ্রুত চলুন। মহিলারা স্বাভাবিকভাবে হাঁটুন।",
        "মারওয়া পাহাড়ে পৌঁছালে একটি চক্কর পূর্ণ হবে। কাবার দিকে ফিরে হাত তুলে দোয়া-মোনাজাত করুন।",
        "মারওয়া থেকে আবার সাফার দিকে চলুন (২য় চক্কর)। এভাবে ক্রমান্বয়ে ৭টি চক্কর সম্পন্ন করুন।"
    )

    val SAI_ROUND_CHECKLIST_EN = listOf(
        "Begin walking steadily from Safa toward Marwah (Lap 1).",
        "Between the green light markers, men jog lightly / walk briskly, while women continue at a normal pace.",
        "Reaching Mount Marwah completes one full lap. Face the Ka'bah and make sincere supplications.",
        "Walk from Marwah back to Safa (Lap 2). Repeat this alternating path until 7 laps are completed."
    )

    val SAI_ROUND_CHECKLIST = SAI_ROUND_CHECKLIST_BN

    const val SAI_SAFA_MARWAH_DUA_ARABIC = "إِنَّ الصَّفَا وَالْمَرْوَةَ مِنْ شَعَائِرِ اللَّهِ فَمَنْ حَجَّ الْبَيْتَ أَوِ اعْتَمَرَ فَلَا جُنَاحَ عَلَيْهِ أَنْ يَطَّوَّفَ بِهِمَا وَمَنْ تَطَوَّعَ خَيْرًا فَإِنَّ اللَّهَ شَاكِرٌ عَلِيمٌ"
    const val SAI_SAFA_MARWAH_DUA_BN = "নিশ্চয়ই সাফা ও মারওয়া আল্লাহর নিদর্শনসমূহের অন্তর্ভুক্ত। অতএব যে ব্যক্তি এই ঘরের হজ বা উমরাহ করবে, তার জন্য এই দুইয়ের তাওয়াফ (সাঈ) করায় কোনো দোষ নেই। আর যে ব্যক্তি স্বতস্ফূর্তভাবে কোনো সৎকাজ করবে, তবে নিশ্চয়ই আল্লাহ পরম গুণগ্রাহী, সর্বজ্ঞ।"
    const val SAI_SAFA_MARWAH_DUA_EN = "Indeed, Safa and Marwah are among the symbols of Allah. So whoever makes Hajj to the House or performs Umrah - there is no blame upon him for walking between them. And whoever volunteers good - then indeed, Allah is appreciative and All-Knowing."

    val SAI_FINAL_CHECKLIST_BN = listOf(
        "মারওয়ায় সমাপ্তি: ৭ম চক্করটি মারওয়া পাহাড়ে গিয়ে শেষ হবে। এরপর আর সাফার দিকে যেতে হবে না।",
        "মারওয়া পাহাড়ে দাঁড়িয়ে শেষবারের মতো কিবলামুখী হয়ে আল্লাহর শুকরিয়া ও দোআ-মোনাজাত করুন।"
    )

    val SAI_FINAL_CHECKLIST_EN = listOf(
        "Conclusion at Marwah: Your 7th lap finishes at Mount Marwah. Do not return to Safa.",
        "Face the Qiblah at Mount Marwah and offer your concluding prayers of gratitude and supplication."
    )

    val SAI_FINAL_CHECKLIST = SAI_FINAL_CHECKLIST_BN

    // 4. HALQ / QASR CONTENT
    const val HALQ_DEF_TITLE_BN = "হলক (মাথা মুণ্ডন)"
    const val HALQ_DEF_TITLE_EN = "Halq (Shaving Head)"

    const val HALQ_DEF_DESC_BN = "রেজার বা ব্লেড দিয়ে মাথা পুরোপুরি ন্যাড়া করা। এটি পুরুষদের জন্য অত্যন্ত সুপারিশকৃত ও অধিক সওয়াবপূর্ণ।"
    const val HALQ_DEF_DESC_EN = "Completely shaving the entire head with a razor. This is highly recommended for men and earns greater reward."

    const val QASR_DEF_TITLE_BN = "কসর (চুল ছাঁটাই)"
    const val QASR_DEF_TITLE_EN = "Qasr (Trimming Hair)"

    const val QASR_DEF_DESC_BN = "মাথার সব দিক থেকে সমানভাবে অন্তত আঙুলের এক কড় পরিমাণ চুল ছাঁটা। পুরুষদের জন্য অনুমতিপ্রাপ্ত এবং মহিলাদের জন্য বাধ্যতামূলক।"
    const val QASR_DEF_DESC_EN = "Trimming hair equally from all sides (about 1 inch / fingertip length). Permissible for men and obligatory for women."

    val HALQ_MEN_GUIDELINES_BN = listOf(
        "কসরের চেয়ে হলক উত্তম, কারণ রাসূলুল্লাহ (সা.) মাথা মুণ্ডনকারীদের জন্য ৩ বার এবং চুল ছাঁটাইকারীদের জন্য ১ বার রহমতের দোয়া করেছেন।",
        "কসর বেছে নিলে মাথার সব প্রান্ত ও অংশ থেকে সমানভাবে চুল ছাঁটতে হবে।",
        "যাদের মাথায় চুল কম বা নেই, তারাও মাথায় প্রতীকীভাবে রেজার বুলিয়ে রীতিনীতি পূর্ণ করবেন।"
    )

    val HALQ_MEN_GUIDELINES_EN = listOf(
        "Halq (shaving) is superior to Qasr, as Prophet Muhammad (PBUH) made supplication of mercy 3 times for those who shave and once for those who trim.",
        "If choosing Qasr (trimming), hair must be cut evenly from all parts of the head (at least fingertip length).",
        "Those with little or no hair should still pass a clean razor gently over the head to fulfill the ritual."
    )

    val HALQ_WOMEN_GUIDELINES_BN = listOf(
        "মহিলাদের জন্য মাথা মুণ্ডন (হলক) করা কঠোরভাবে নিষিদ্ধ।",
        "মহিলাগণ তাদের সব চুল এক সাথে একত্র করে আঙুলের এক কড় (প্রায় এক ইঞ্চি) পরিমাণ নিচ থেকে ছেঁটে কসর করবেন।"
    )

    val HALQ_WOMEN_GUIDELINES_EN = listOf(
        "Shaving the head (Halq) is strictly prohibited for women.",
        "Women must gather their hair together and trim a fingertip's length (about 1 inch) from the bottom."
    )

    const val HALQ_WARNING_TEXT_BN = "সাঈ শেষ না হওয়া পর্যন্ত চুল কাটবেন না। হলক বা কসর সম্পন্ন হওয়ার পরেই কেবল ইহরামের সকল নিষেধাজ্ঞা সমাপ্ত হবে।"
    const val HALQ_WARNING_TEXT_EN = "Do not cut hair until Sa'i is completely finished. All Ihram prohibitions remain active until Halq or Qasr is done."

    const val HALQ_DEF_TITLE = HALQ_DEF_TITLE_BN
    const val HALQ_DEF_DESC = HALQ_DEF_DESC_BN
    const val QASR_DEF_TITLE = QASR_DEF_TITLE_BN
    const val QASR_DEF_DESC = QASR_DEF_DESC_BN
    val HALQ_MEN_GUIDELINES = HALQ_MEN_GUIDELINES_BN
    val HALQ_WOMEN_GUIDELINES = HALQ_WOMEN_GUIDELINES_BN
    const val HALQ_WARNING_TEXT = HALQ_WARNING_TEXT_BN

    const val HALQ_DUA_ARABIC = "الْحَمْدُ لِلَّهِ الَّذِي قَضَى عَنَّا نُسُكَنَا"
    const val HALQ_DUA_BN = "সমস্ত প্রশংসা আল্লাহর জন্য যিনি আমাদেরকে আমাদের ইবাদত (উমরাহ) সম্পন্ন করার তাওফীক দিয়েছেন।"
    const val HALQ_DUA_EN = "All praise is due to Allah Who has fulfilled our rites of devotion (Umrah) for us."
    const val HALQ_DUA = HALQ_DUA_BN

    // 5. POST-UMRAH DUAS
    data class UmrahDuaItem(
        val title: String,
        val arabic: String,
        val translation: String,
        val reference: String = ""
    )

    val POST_UMRAH_DUAS_BN = listOf(
        UmrahDuaItem(
            title = "উমরাহ কবুল হওয়ার দোয়া",
            arabic = "رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
            translation = "হে আমাদের রব! আপনি আমাদের পক্ষ থেকে কবুল করুন। নিশ্চয়ই আপনি সর্বশ্রোতা, সর্বজ্ঞ।",
            reference = "সূরা আল-বাকারা: ১২৭"
        ),
        UmrahDuaItem(
            title = "যমযম পান করার দোয়া",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ",
            translation = "হে আল্লাহ! নিশ্চয়ই আমি আপনার কাছে উপকারী জ্ঞান, প্রশস্ত রিযিক এবং সকল রোগ থেকে নিরাময় প্রার্থনা করছি।",
            reference = "সুনান আদ-দারাকুতনি"
        ),
        UmrahDuaItem(
            title = "উভয় জগতের কল্যাণের দোয়া",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            translation = "হে আমাদের রব! আপনি আমাদেরকে দুনিয়াতে কল্যাণ দান করুন এবং আখেরাতেও কল্যাণ দান করুন এবং আমাদেরকে জাহান্নামের আগুন থেকে রক্ষা করুন।",
            reference = "সূরা আল-বাকারা: ২০১"
        )
    )

    val POST_UMRAH_DUAS_EN = listOf(
        UmrahDuaItem(
            title = "Supplication for Acceptance of Umrah",
            arabic = "رَبَّنَا تَقَبَّلْ مِنَّا إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
            translation = "Our Lord, accept this from us. Indeed, You are the All-Hearing, the All-Knowing.",
            reference = "Surah Al-Baqarah: 127"
        ),
        UmrahDuaItem(
            title = "Supplication when Drinking Zamzam",
            arabic = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ",
            translation = "O Allah, I ask You for beneficial knowledge, abundant provision, and healing from every illness.",
            reference = "Sunan al-Daraqutni"
        ),
        UmrahDuaItem(
            title = "Supplication for Good in Both Worlds",
            arabic = "رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ",
            translation = "Our Lord, give us in this world that which is good and in the Hereafter that which is good, and protect us from the punishment of the Fire.",
            reference = "Surah Al-Baqarah: 201"
        )
    )

    val POST_UMRAH_DUAS = POST_UMRAH_DUAS_BN

    // 6. ZAMZAM WATER ETIQUETTES
    val ZAMZAM_ETIQUETTES_BN = listOf(
        "পান করার সময় পবিত্র কাবার দিকে মুখ করা।",
        "পান করার শুরুতে 'বিসমিল্লাহ' বলা।",
        "এক নিঃশ্বাসে পান না করে ধীরে ধীরে তিন নিঃশ্বাসে পান করা।",
        "পরিতৃপ্ত হয়ে প্রচুর পরিমাণে যমযম পানি পান করা।",
        "পান শেষে 'আলহামদুলিল্লাহ' বলে আল্লাহর শুকরিয়া আদায় করা ও দোয়া করা।"
    )

    val ZAMZAM_ETIQUETTES_EN = listOf(
        "Face the direction of the Holy Ka'bah while drinking.",
        "Begin by reciting 'Bismillah'.",
        "Drink in three measured sips rather than gulping in one breath.",
        "Drink plentifully until fully satisfied.",
        "Conclude with 'Alhamdulillah' and make sincere personal prayers."
    )

    val ZAMZAM_ETIQUETTES = ZAMZAM_ETIQUETTES_BN

    const val ZAMZAM_RECOMMENDED_DUA_ARABIC = "اللَّهُمَّ إِنِّي أَسْأَلُكَ عِلْمًا نَافِعًا وَرِزْقًا وَاسِعًا وَشِفَاءً مِنْ كُلِّ دَاءٍ"
    const val ZAMZAM_RECOMMENDED_DUA_BN = "হে আল্লাহ! নিশ্চয়ই আমি আপনার কাছে উপকারী জ্ঞান, প্রশস্ত রিযিক এবং সকল রোগ থেকে নিরাময় প্রার্থনা করছি।"
    const val ZAMZAM_RECOMMENDED_DUA_EN = "O Allah, I ask You for beneficial knowledge, abundant sustenance, and a cure for every ailment."

    // Helper functions for localized access
    fun getIhramChecklist(isEn: Boolean): List<String> = if (isEn) IHRAM_CHECKLIST_EN else IHRAM_CHECKLIST_BN
    fun getIhramProhibitions(isEn: Boolean): List<Pair<String, String>> = if (isEn) IHRAM_PROHIBITIONS_EN else IHRAM_PROHIBITIONS_BN
    fun getTawafPrepChecklist(isEn: Boolean): List<String> = if (isEn) TAWAF_PREP_CHECKLIST_EN else TAWAF_PREP_CHECKLIST_BN
    fun getTawafRoundChecklist(isEn: Boolean): List<String> = if (isEn) TAWAF_ROUND_CHECKLIST_EN else TAWAF_ROUND_CHECKLIST_BN
    fun getTawafFinalChecklist(isEn: Boolean): List<String> = if (isEn) TAWAF_FINAL_CHECKLIST_EN else TAWAF_FINAL_CHECKLIST_BN
    fun getSaiPrepChecklist(isEn: Boolean): List<String> = if (isEn) SAI_PREP_CHECKLIST_EN else SAI_PREP_CHECKLIST_BN
    fun getSaiRoundChecklist(isEn: Boolean): List<String> = if (isEn) SAI_ROUND_CHECKLIST_EN else SAI_ROUND_CHECKLIST_BN
    fun getSaiFinalChecklist(isEn: Boolean): List<String> = if (isEn) SAI_FINAL_CHECKLIST_EN else SAI_FINAL_CHECKLIST_BN
    fun getHalqMenGuidelines(isEn: Boolean): List<String> = if (isEn) HALQ_MEN_GUIDELINES_EN else HALQ_MEN_GUIDELINES_BN
    fun getHalqWomenGuidelines(isEn: Boolean): List<String> = if (isEn) HALQ_WOMEN_GUIDELINES_EN else HALQ_WOMEN_GUIDELINES_BN
    fun getPostUmrahDuas(isEn: Boolean): List<UmrahDuaItem> = if (isEn) POST_UMRAH_DUAS_EN else POST_UMRAH_DUAS_BN
    fun getZamzamEtiquettes(isEn: Boolean): List<String> = if (isEn) ZAMZAM_ETIQUETTES_EN else ZAMZAM_ETIQUETTES_BN
    fun getHalqDefTitle(isEn: Boolean): String = if (isEn) HALQ_DEF_TITLE_EN else HALQ_DEF_TITLE_BN
    fun getHalqDefDesc(isEn: Boolean): String = if (isEn) HALQ_DEF_DESC_EN else HALQ_DEF_DESC_BN
    fun getQasrDefTitle(isEn: Boolean): String = if (isEn) QASR_DEF_TITLE_EN else QASR_DEF_TITLE_BN
    fun getQasrDefDesc(isEn: Boolean): String = if (isEn) QASR_DEF_DESC_EN else QASR_DEF_DESC_BN
    fun getHalqWarningText(isEn: Boolean): String = if (isEn) HALQ_WARNING_TEXT_EN else HALQ_WARNING_TEXT_BN
    fun getHalqDuaTranslation(isEn: Boolean): String = if (isEn) HALQ_DUA_EN else HALQ_DUA_BN
    fun getIhramNiyyahTranslation(isEn: Boolean): String = if (isEn) IHRAM_NIYYAH_EN else IHRAM_NIYYAH_BN
    fun getTalbiyahTranslation(isEn: Boolean): String = if (isEn) TALBIYAH_EN else TALBIYAH_BN
    fun getTawafRuknYamaniDuaTranslation(isEn: Boolean): String = if (isEn) TAWAF_RUKN_YAMANI_DUA_EN else TAWAF_RUKN_YAMANI_DUA_BN
    fun getSaiSafaMarwahDuaTranslation(isEn: Boolean): String = if (isEn) SAI_SAFA_MARWAH_DUA_EN else SAI_SAFA_MARWAH_DUA_BN

    fun getChecklistText(stepKey: String, itemIndex: Int, isEn: Boolean): String {
        val list = when (stepKey) {
            "IHRAM" -> getIhramChecklist(isEn)
            "TAWAF_PREP" -> getTawafPrepChecklist(isEn)
            "TAWAF_ROUND" -> getTawafRoundChecklist(isEn)
            "TAWAF_FINAL" -> getTawafFinalChecklist(isEn)
            "SAI_PREP" -> getSaiPrepChecklist(isEn)
            "SAI_ROUND" -> getSaiRoundChecklist(isEn)
            "SAI_FINAL" -> getSaiFinalChecklist(isEn)
            "HALQ" -> listOf(if (isEn) "Halq / Qasr completed" else "হলক / কসর সম্পন্ন করা হয়েছে")
            else -> emptyList()
        }
        return list.getOrNull(itemIndex) ?: if (isEn) "Ritual item ${itemIndex + 1}" else "নিয়মাবলী ${itemIndex + 1}"
    }
}
