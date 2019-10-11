package com.kimjio;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;

public class PSSBundleSplitter {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: PSSBundleSplitter [PSS BUNDLE FILE]");
            System.exit(1);
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.err.println("File not found...");
            System.exit(1);
        }

        System.out.format("FOUND: %s\n", file.getPath());

        long size;
        long start = System.nanoTime();
        byte[] buf;
        int startOffset = 0;
        int endOffset = 16;
        int allOffset = 0;
        int offset = 0;
        int fileIndex = 0;

        System.out.print("Open stream... ");

        FileChannel channelIn = null;
        ByteBuffer byteIn = null;
        try {
            channelIn = new FileInputStream(file.getCanonicalPath()).getChannel();
            byteIn = ByteBuffer.allocate((int) channelIn.size());
            System.out.println("OK");
        } catch (IOException e) {
            System.err.println("Fail");
            System.err.println(e.toString());
            System.exit(1);
        }
        FileChannel channelOut = null;
        ByteBuffer byteOut = null;

        try {
            if ((size = channelIn.read(byteIn)) != -1) {
                byteIn.flip();

                while (endOffset < size) {

                    buf = Arrays.copyOfRange(byteIn.array(), startOffset, endOffset);

                    startOffset += 16;
                    endOffset += 16;

                    if (checkHeader(buf)) {
                        if (channelOut != null) {
                            channelOut.close();
                            System.out.format("\rMOVIE_%02d.pss created. size: %s \n", fileIndex++, formatSize(offset));
                        }
                        offset = 0;
                        channelOut = new FileOutputStream(String.format("%s/MOVIE_%02d.pss", file.getParent(), fileIndex)).getChannel();
                        byteOut = ByteBuffer.allocate(16);
                    } else if (allOffset == 0) {
                        System.err.println("Not pss file");
                        System.exit(1);
                    }

                    byteOut.clear();
                    byteOut.put(buf);
                    byteOut.flip();
                    channelOut.write(byteOut);

                    allOffset += 16;
                    offset += 16;

                    System.out.write(String.format("\rFile: MOVIE_%02d.pss, Size: %s, Current file offset: %08X, All offset: %08X", fileIndex, formatSize(offset), offset, allOffset).getBytes());
                    System.out.flush();
                }
                System.out.format("\rMOVIE_%02d.pss created. size: %s \n", fileIndex++, formatSize(offset));
                System.out.println();
            }
        } catch (IOException e) {
            System.err.println("IO ERROR");
            System.err.println(e.toString());
        } finally {
            System.out.println("Closing stream...");
            try {
                if (channelOut != null) channelOut.close();
                channelIn.close();
            } catch (IOException ignore) {
            }
        }

        System.out.format("DONE. %d file created. TOTAL: %.2f sec.\n", fileIndex, (System.nanoTime() - start) / 1e9);
    }

    private static String formatSize(long size) {
        String formatted;

        double kb = size / 1024d;
        double mb = kb / 1024d;

        if ((int) mb > 0) {
            formatted = String.format("%.2f MB", mb);
        } else if ((int) kb > 0) {
            formatted = String.format("%.2f KB", kb);
        } else {
            formatted = String.format("%d B", size);
        }

        return formatted;
    }

    private static boolean checkHeader(byte[] buf) {
        //00 00 01 BA 44 00 04 00
        return (buf[0] == 0 && buf[1] == 0 && buf[2] == 1 && buf[3] == (byte) 0xBA && buf[4] == 0x44 && buf[5] == 0 && buf[6] == 4 && buf[7] == 0);
    }
}
