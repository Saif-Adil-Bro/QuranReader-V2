package com.example.data.model

import androidx.annotation.Keep

@Keep
enum class AdmissionStatus(val labelBn: String, val labelEn: String) {
    PENDING("অনুমোদনের অপেক্ষায়", "Pending Approval"),
    APPROVED("ভর্তি নিশ্চিত", "Approved"),
    REJECTED("বাতিল", "Rejected")
}

@Keep
enum class StudentStatus(val labelBn: String, val labelEn: String) {
    ACTIVE("অধ্যয়নরত", "Active"),
    INACTIVE("নিষ্ক্রিয়", "Inactive"),
    GRADUATED("ফারেগ / উত্তীর্ণ", "Graduated"),
    TC("ছাড়পত্র প্রাপ্ত", "Transferred")
}

@Keep
enum class AttendanceStatus(val labelBn: String, val labelEn: String) {
    PRESENT("উপস্থিত", "Present"),
    ABSENT("অনুপস্থিত", "Absent"),
    LATE("দেরিতে উপস্থিতি", "Late"),
    ON_LEAVE("ছুটি মঞ্জুর", "On Leave")
}

@Keep
enum class LeaveStatus(val labelBn: String, val labelEn: String) {
    PENDING("বিবেচনাধীন", "Pending"),
    APPROVED("মঞ্জুরকৃত", "Approved"),
    REJECTED("নামঞ্জুর", "Rejected")
}

/**
 * ১. ছাত্র ব্যবস্থাপনা মডেল
 */
@Keep
data class AcademicStudent(
    val id: String = "",
    val rollNo: String = "",
    val nameBn: String = "",
    val nameEn: String = "",
    val fatherName: String = "",
    val motherName: String = "",
    val guardianPhone: String = "",
    val jamatClass: String = "হিফজুল কুরআন",
    val section: String = "ক",
    val bloodGroup: String = "A+",
    val dob: String = "",
    val address: String = "",
    val admissionDate: String = "",
    val isHifzStudent: Boolean = false,
    val status: StudentStatus = StudentStatus.ACTIVE,
    val notes: String = ""
)

/**
 * ২. ভর্তি ব্যবস্থাপনা মডেল
 */
@Keep
data class AdmissionApplication(
    val id: String = "",
    val applicantNameBn: String = "",
    val applicantNameEn: String = "",
    val guardianName: String = "",
    val guardianPhone: String = "",
    val appliedJamat: String = "",
    val previousInstitute: String = "",
    val applicationDate: String = "",
    val status: AdmissionStatus = AdmissionStatus.PENDING,
    val remarks: String = ""
)

/**
 * ৩. জামাত ও রুটিন মডেল
 */
@Keep
data class RoutinePeriod(
    val periodNo: Int = 1,
    val periodNameBn: String = "১ম ঘণ্টা",
    val periodNameEn: String = "1st Period",
    val subjectBn: String = "কুরআন ও তাজবীদ",
    val subjectEn: String = "Quran & Tajweed",
    val teacherName: String = "মাওলানা আব্দুল্লাহ",
    val startTime: String = "০৮:০০",
    val endTime: String = "০৮:৪৫"
)

@Keep
data class JamatClassRoutine(
    val id: String = "",
    val jamatNameBn: String = "",
    val jamatNameEn: String = "",
    val section: String = "ক",
    val classTeacher: String = "",
    val totalStudents: Int = 0,
    val periods: List<RoutinePeriod> = emptyList()
)

/**
 * ৪. হিফজুল কুরআন মডেল
 */
@Keep
data class HifzDailyRecord(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNo: String = "",
    val date: String = "",
    val sabaqPara: String = "",
    val sabaqSurah: String = "",
    val sabaqAyahRange: String = "",
    val sabqiPara: String = "",
    val manzilPara: String = "",
    val tajweedRating: Int = 5, // 1 to 5 stars
    val ustadRemarks: String = "মাশাআল্লাহ, ভালো তিলাওয়াত"
)

/**
 * ৫. হাজিরা ও ছুটি মডেল
 */
@Keep
data class DailyAttendance(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNo: String = "",
    val jamatClass: String = "",
    val date: String = "",
    val status: AttendanceStatus = AttendanceStatus.PRESENT,
    val inTime: String = "০৭:৫০",
    val note: String = ""
)

@Keep
data class LeaveApplication(
    val id: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val rollNo: String = "",
    val jamatClass: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val totalDays: Int = 1,
    val reason: String = "",
    val status: LeaveStatus = LeaveStatus.PENDING,
    val appliedDate: String = ""
)

/**
 * ৬. পরীক্ষা ও মূল্যায়ন মডেল
 */
@Keep
data class ExamSubjectMark(
    val subjectNameBn: String = "",
    val subjectNameEn: String = "",
    val fullMarks: Int = 100,
    val obtainedMarks: Int = 85
)

@Keep
data class StudentExamResult(
    val studentId: String = "",
    val studentName: String = "",
    val rollNo: String = "",
    val marksList: List<ExamSubjectMark> = emptyList(),
    val totalObtained: Int = 0,
    val totalFullMarks: Int = 0,
    val percentage: Double = 0.0,
    val grade: String = "মুমতাজ (A+)",
    val meritPosition: Int = 1,
    val remarks: String = "উত্তম ফলাফল"
)

@Keep
data class AcademicExam(
    val id: String = "",
    val examTitleBn: String = "১ম সাময়িক পরীক্ষা ২০২৬",
    val examTitleEn: String = "1st Term Exam 2026",
    val jamatClass: String = "",
    val academicYear: String = "২০২৬",
    val examDate: String = "",
    val isResultPublished: Boolean = true,
    val results: List<StudentExamResult> = emptyList()
)
