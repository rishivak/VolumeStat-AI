package com.algo.helper;

import com.algo.utils.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class DownloadInstruments {
//    curl https://api.kite.trade/instruments/NFO \
//            -o src/main/resources/instruments_nfo.csv


    public static void main(String[] args) throws Exception {

        // ✅ ABSOLUTE PATH (your system)
        String filePath =
                "C:\\Users\\ssharma2\\Documents\\zerodha-orderbook-algo\\" +
                        "zerodha-orderbook-algo\\src\\main\\resources\\instruments_nfo.csv";

        // ✅ Ensure directory exists
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        URL url = new URL("https://api.kite.trade/instruments/NFO");

        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(file)) {

            in.transferTo(out);
        }

        Log.info("Downloaded instruments_nfo.csv to:");
        Log.info(file.getAbsolutePath());
    }
}
