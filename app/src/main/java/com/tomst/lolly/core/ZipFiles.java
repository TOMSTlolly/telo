package com.tomst.lolly.core;

import android.content.Context;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.tomst.lolly.core.OnProListener; // DŮLEŽITÝ IMPORT

public class ZipFiles {
    List<String> filesListInDir = new ArrayList<String>();

    private void zipFile(File fileToZip, String fileName, ZipOutputStream zos) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileToZip)) {
            ZipEntry zipEntry = new ZipEntry(fileName);
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
        }
    }
    /**
     * Zazipuje adresář a reportuje průběh pomocí OnProListener.
     * @param sourceDir Zdrojový adresář (java.io.File)
     * @param zipFilePath Cesta k cílovému ZIP souboru
     * @param listener Váš OnProListener pro sledování průběhu
     * @return true, pokud bylo zipování úspěšné
     */
    public boolean zipDirectory(File sourceDir, String zipFilePath, OnProListener listener) {
        File[] files = sourceDir.listFiles();
        if (files == null || files.length == 0) {
            return false; // Nic k zipování
        }

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            int filesZipped = 0;
            int totalFiles = files.length;

            for (File file : files) {
                // Zipujeme pouze soubory, ne pod-adresáře pro jednoduchost
                if (file.isFile()) {
                    zipFile(file, file.getName(), zos);
                }
                filesZipped++;

                // Vypočítáme a reportujeme procentuální progres
                if (listener != null) {
                    int progress = (int) ((filesZipped / (float) totalFiles) * 100);
                    // Voláme metodu z vašeho rozhraní
                    listener.OnProEvent(progress);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Zazipuje DocumentFile adresář a reportuje průběh pomocí OnProListener.
     * @param directory Zdrojový adresář (DocumentFile)
     * @param zipFilePath Cesta k cílovému ZIP souboru
     * @param context Context aplikace
     * @param listener Váš OnProListener pro sledování průběhu
     * @return true, pokud bylo zipování úspěšné
     */
    public boolean zipDocumentFileDirectory(DocumentFile directory, String zipFilePath, Context context, OnProListener listener) {
        DocumentFile[] files = directory.listFiles();
        if (files.length == 0) return false;

        try (FileOutputStream fos = new FileOutputStream(zipFilePath);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            int filesZipped = 0;
            int totalFiles = files.length;

            for (DocumentFile file : files) {
                if (file.isFile()) {
                    // Kód pro přidání DocumentFile do ZIPu
                    try (ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(file.getUri(), "r");
                         FileInputStream fis = new FileInputStream(pfd.getFileDescriptor())) {

                        ZipEntry zipEntry = new ZipEntry(file.getName());
                        zos.putNextEntry(zipEntry);

                        byte[] bytes = new byte[4096];
                        int length;
                        while ((length = fis.read(bytes)) >= 0) {
                            zos.write(bytes, 0, length);
                        }
                        zos.closeEntry();
                    }
                }
                filesZipped++;
                // Vypočítáme a reportujeme procentuální progres
                if (listener != null) {
                    int progress = (int) ((filesZipped / (float) totalFiles) * 100);
                    // Voláme metodu z vašeho rozhraní
                    listener.OnProEvent(progress);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }




    /**
     * This method populates all the files in a directory to a List
     * @param dir
     * @throws IOException
     */
    private void populateFilesList(File dir) throws IOException {
        File[] files = dir.listFiles();
        for(File file : files){
            if(file.isFile()) {
                if (file.getName().contains(".csv"))
                    filesListInDir.add(file.getAbsolutePath());
            }
            else populateFilesList(file);
        }
    }

    /**
     * This method compresses the single file to zip format
     * @param file
     * @param zipFileName
     */
    private static void zipSingleFile(File file, String zipFileName) {
        try {
            //create ZipOutputStream to write to the zip file
            FileOutputStream fos = new FileOutputStream(zipFileName);
            ZipOutputStream zos = new ZipOutputStream(fos);
            //add a new Zip Entry to the ZipOutputStream
            ZipEntry ze = new ZipEntry(file.getName());
            zos.putNextEntry(ze);
            //read the file and write to ZipOutputStream
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, len);
            }

            //Close the zip entry to write to zip file
            zos.closeEntry();
            //Close resources
            zos.close();
            fis.close();
            fos.close();
            System.out.println(file.getCanonicalPath()+" is zipped to "+zipFileName);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
