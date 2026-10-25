package com.ticketbooking.utils;

public final class StringUtils {
    public static Boolean isNullOrEmpty(String str){
        return str == null || str.trim().isEmpty();
    }

    public static Boolean isNotNullOrEmpty(String str){
        return !(isNullOrEmpty(str));
    }

    public static String generateUUID(){
        return java.util.UUID.randomUUID().toString();
    }
}
