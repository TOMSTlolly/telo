package com.tomst.lolly.core;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class SecondaryCardChannel {
    private ParcelFileDescriptor pfdInput, pfdOutput;
    private FileInputStream fis;
    private FileOutputStream fos;
    private long position;

    public SecondaryCardChannel(Uri treeUri, Context context) {
        try {
            pfdInput = context.getContentResolver().openFileDescriptor(treeUri, "r");
            pfdOutput = context.getContentResolver().openFileDescriptor(treeUri, "rw");
            fis = new FileInputStream(pfdInput.getFileDescriptor());
            fos = new FileOutputStream(pfdOutput.getFileDescriptor());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static RandomAccessFile openLocalFile(Uri uri) {
        try
        {
            if (uri.getPath() == null) {
                return null;
        }
            return new RandomAccessFile((uri.getPath()), "r");
        } catch (FileNotFoundException e) {
            if (!TextUtils.isEmpty(uri.getQuery()) || !TextUtils.isEmpty(uri.getFragment())) {
                return null;
            }
        }
        return null;
    }

    public int readLast2048Bytes(ByteBuffer buffer) {
        try {
            FileChannel fch = fis.getChannel();
            long fileSize = fch.size();
            long i=0;
            long startPosition =  1;
            int bytesRead=0;
            while (i<fileSize){
                fch.position(startPosition);
                position = fch.position();
                bytesRead = fch.read(buffer);
                Log.d("TOMST", bytesRead + " bytes read from " + position);
                startPosition = position + 2048;
            }

            return bytesRead;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int read(ByteBuffer buffer, long startPosition) {
        try {
            FileChannel fch = fis.getChannel();
            fch.position(startPosition);
            int bytesRead = fch.read(buffer);
            position = fch.position();
            return bytesRead;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int write(ByteBuffer buffer) {
        try {
            FileChannel fch = fos.getChannel();
            fch.position(position);
            int bytesWrite = fch.write(buffer);
            position = fch.position();
            return bytesWrite;
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public long position() throws IOException {
        return position;
    }

    public SecondaryCardChannel position(long newPosition) throws IOException {
        position = newPosition;
        return this;
    }

    public long size() throws IOException {
        return fis.getChannel().size();
    }

    public void force(boolean metadata) throws IOException {
        fos.getChannel().force(metadata);
        pfdOutput.getFileDescriptor().sync();
    }

    public long truncate(long size) throws Exception {
        FileChannel fch = fos.getChannel();
        try {
            fch.truncate(size);
            return fch.size();
        } catch (Exception e){ // Attention! Truncate is broken on removable SD card of Android 5.0
            e.printStackTrace();
            return -1;
        }
    }

    public void close() throws IOException {
        FileChannel fch = fos.getChannel();
        fch.close();

        fos.close();
        fis.close();
        pfdInput.close();
        pfdOutput.close();
    }
}
