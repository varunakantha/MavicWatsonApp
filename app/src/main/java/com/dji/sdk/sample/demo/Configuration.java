//package com.dji.sdk.sample.demo;
//
//import android.net.Uri;
//
//import com.nuance.speechkit.PcmFormat;
//
///**
// * All Nuance Developers configuration parameters can be set here.
// *
// * Copyright (c) 2015 Nuance Communications. All rights reserved.
// */
//public class Configuration {
//
//    //All fields are required.
//    //Your credentials can be found in your Nuance Developers portal, under "Manage My Apps".
//    public static final String APP_KEY = "f5d31899ebe006c328214cd2747afa2496b83324344fbea9fe5bd8c28689127b5d2b14480b1b0b333c5705b0d1ffd567b8088bec584a9556a56d192bf72ba654";
//    public static final String APP_ID = "NMDPTRIAL_sajithkumara89_gmail_com20180319061743";
//    public static final String SERVER_HOST = "sslsandbox-nmdp.nuancemobility.net";
//    public static final String SERVER_PORT = "443";
//
//    public static final String LANGUAGE = "eng-USA";
//
//    public static final Uri SERVER_URI = Uri.parse("nmsps://" + APP_ID + "@" + SERVER_HOST + ":" + SERVER_PORT);
//
//    //Only needed if using NLU
//    public static final String CONTEXT_TAG = "!NLU_CONTEXT_TAG!";
//
//    public static final PcmFormat PCM_FORMAT = new PcmFormat(PcmFormat.SampleFormat.SignedLinear16, 16000, 1);
//    public static final String LANGUAGE_CODE = (Configuration.LANGUAGE.contains("!") ? "eng-USA" : Configuration.LANGUAGE);
//
//}
//
//
//
