package com.example.util

import java.io.IOException

class NoInternetException(message: String = "কোনো ইন্টারনেট সংযোগ নেই। দয়া করে আপনার নেটওয়ার্ক কানেকশন চেক করুন অথবা অফলাইন মোড ব্যবহার করুন।") : IOException(message)
