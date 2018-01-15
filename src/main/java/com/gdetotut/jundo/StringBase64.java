package com.gdetotut.jundo;

public class StringBase64 {
    final String value;
    StringBase64(String value) {
        if(null == value) {
            throw new NullPointerException("value");
        }
        this.value = value;
    }
}
