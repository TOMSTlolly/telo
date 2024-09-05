package com.tomst.lolly.core;


import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;


/**
 * Wraps file creation and IO operations into a concise interface
 * specifically for operating on TOMST dendrometer datasets.
 */
public class CSVFile
{
    // public constants
    /**
     * Modes allow control over how to open a file. These are useful for
     * deciphering if a file is intended to be overwritten or appended to.
     */
    public static final char APPEND_MODE = 'a';
    public static final char READ_MODE = 'r';
    public static final char WRITE_MODE = 'w';

    // private constants
    private static final String TAG = "CSV";
    private static final int LINE_LENGTH = 0;
    private static final String DELIM = ";";

    // positional consts for indexing
    private static final byte POINT_LEN = 9;
    private static final byte DATETIME_INDEX = 1;
    private static final byte TEMP1_INDEX = 3;
    private static final byte TEMP2_INDEX = 4;
    private static final byte TEMP3_INDEX = 5;
    private static final byte HUMIDITY_INDEX = 6;
    private static final byte MVS_INDEX = 7;

    // operational members
    private final char mode;
    private File file;
    private FileOutputStream writer;
    private Scanner reader;


    /**
     * Instantiates a new CSVFile with which a CSV file's contents can be
     * written or read.
     *
     * @param file File object representing a file in the filesystem
     * @throws IOException
     */
    private CSVFile(File file, char mode)
    {
        this.mode = mode;
        this.file = file;

        try
        {
            if (mode == READ_MODE)
            {
                this.reader = new Scanner(file);
            }
            else
            {
                this.writer = new FileOutputStream(file, (mode == APPEND_MODE));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    // opening, closing, and creating new files
    /**
     * Creates a new file in the filesystem. Shorthand for creating a file and
     * calling CSVFile.open().
     *
     * @param path The path at which to create the file, including the file name
     * @return Reference to a CSVFile through which a file can be interacted
     */
    public static CSVFile create(String path)
    {
        CSVFile csvFile = null;

        try
        {
            File file = new File(path);

            if (file.createNewFile())
            {
                Log.d(TAG, "Created file named: " + path);

                csvFile = open(path, WRITE_MODE);
            }
            else
            {
                Log.d(TAG, path + " file already exists.");
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return csvFile;
    }


    /**
     *
     */
    public static boolean exists(String path)
    {
        return (new File(path)).exists();
    }


    /**
     * Opens a file for IO operations. The file specified must exist.
     *
     * @param path The path at which to open the file, include the file name
     * @return Reference to a CSVFile through which a file can be interacted
     */
    public static CSVFile open(String path, char mode)
    {
        File file = new File(path);
        return new CSVFile(file, mode);
    }


    /**
     * Closes a CSVFile object.
     *
     * @apiNote This function can be called directly; otherwise, the garbage
     * collector calls this function on destruction
     */
    public void close()
    {
        try
        {
            if (mode == WRITE_MODE || mode == APPEND_MODE)
            {
                this.writer.close();
            }
            else
            {
                this.reader.close();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Removes a file at the given path from the filesystem.
     *
     * @param path File path from which a file is to be removed
     */
    public static void delete(String path)
    {
        if (!new File(path).delete())
        {
            Log.e(TAG, path + " could not be delete!");
        }
    }


    // reading and writing to files
    /**
     * Copies contents of a source file in a destination file given the file
     * paths.
     * @apiNote Provides a more succinct way of a file.
     *
     * @param srcPath File with contents to copy
     * @param destPath File into which source file contents will be copied
     */
    public static void copy(String srcPath, String destPath)
    {
        CSVFile srcFile = CSVFile.open(srcPath, READ_MODE);
        CSVFile destFile = CSVFile.open(destPath, WRITE_MODE);

        String srcContents = srcFile.readAllLines();
        destFile.write(srcContents);
    }


    /**
     * Writes to a file.
     *
     * @param buffer Contents to be written
     */
    public void write(String buffer)
    {
        try
        {
            this.writer.write(buffer.getBytes());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }


    /**
     * Reads the next line from file.
     *
     * @return Line read from the file
     */
    public String readLine()
    {
        // TODO: maybe should let the user do this?
        if (this.reader.hasNextLine())
        {
            return this.reader.nextLine();
        }

        return "";
    }


    /**
     * Reads contents of a file.
     *
     * @return Contents of a file
     */
    public String readAllLines()
    {
        String line = "";
        String contents = "";

        while((line = readLine()) != "")
        {
            contents += line;
        }

        return contents;
    }

    /**
     * Reformats the CSV file into a parallel format (i.e. data is listed in
     * columns: meter1;meter2;meter3;...).
     *
     * @param path Path of file to convert
     * @return Code which communicates if the operation was successful (0) or
     * if the operation failed
     * @apiNote This function write in-place! Please copy and then convert if
     * you wish to preserve the source file.
     * @apiNote Failure codes and their descriptions:
     * 1 = File specified does not exist
     * 2 = Destination file already exists
     */
    public static int toParallel(String path)
    {
        if (!CSVFile.exists(path))
        {
            return 1;
        }

        String split_path[] = path.split("\\.");
        String dest_path = split_path[0] + "_parallel.csv";
        if (CSVFile.exists(dest_path))
        {
            Log.d(TAG, dest_path + " already exists!");
            return 2;
        }

        CSVFile src = CSVFile.open(path, READ_MODE);
        String currentLine;
        String[] split;
        ArrayList<String[]> serials = new ArrayList<String[]>();
        ArrayList<ArrayList<String>> data =
                new ArrayList<ArrayList<String>>();

        currentLine = src.readLine();  // consume number of files
        currentLine = src.readLine();
        split = currentLine.split(DELIM);
        while (split.length > 1)
        {
            serials.add(split);
            split = src.readLine().split(DELIM);
        }
        data.ensureCapacity(25000);

        int lineIdx = 0;
        while ((currentLine = src.readLine()) != "")
        {
            if (currentLine.split(DELIM).length == 1)
            {
                lineIdx = 0;
            }
            else
            {
                if (data.size() == lineIdx)
                {
                    data.add(new ArrayList<String>());
                }

                data.get(lineIdx).add(currentLine);
                lineIdx += 1;
            }
        }
        src.close();

        CSVFile dest = CSVFile.create(dest_path);  // toParallel not USED
        // write header
        dest.write(serials.size() + ";\n");
        String line;
        for (int i = 0; i < serials.size(); i += 1)
        {
            line = "";

            for (int j = 0; j < serials.get(i).length; j += 1)
            {
                line += serials.get(i)[j] + DELIM;
            }

            dest.write(line + "\n");
        }

        // write column names
        line = "";
        for (int i = 0; i < serials.size(); i += 1)
        {
            line += "line number" + "!"
                + serials.get(i)[0] + "!"
                + "date_MMDDYY_HHMM" + "!"
                + "Temperature1" + "!"
                + "Temperature1" + "!"
                + "Temperature1" + "!"
                + "Diam_or_Moisture" + "!"
                + "mvs" + "!"
                + "8" + "!";
        }
        dest.write(line + "\n");

        // write data
        for (int l = 0; l < data.size(); l += 1)
        {
            line = "";
            for (int d = 0; d < data.get(l).size(); d += 1)
            {
                line += data.get(l).get(d);
            }
            dest.write(line + "\n");
        }
        dest.close();

        return 0;
    }


    /**
     * Reformats the CSV file into a serial structure (i.e. data from each
     * dendrometer is listed one after the other). This is the default
     * structure of merged files.
     *
     * @path Path of file to convert
     * @return Code which communicates if the operation was successful (0) or
     * if the operation failed
     * @apiNote This function write in-place! Please copy and then convert if
     * you wish to preserve the source file.
     * @apiNote Failure codes and their descriptions:
     * 1 = File specified does not exist
     * 2 = Destination file already exists
     */
    public static int toSerial(String path)
    {
        if (!CSVFile.exists(path))
        {
            return 1;
        }

        String split_path[] = path.split("_parallel");
        String dest_path = split_path[0] + ".csv";
        if (CSVFile.exists(dest_path))
        {
            Log.d(TAG, dest_path + " already exists!");
            return 2;
        }

        CSVFile src = CSVFile.open(path, READ_MODE);
        String currentLine = "";
        String[] split;
        ArrayList<String> serials = new ArrayList<String>();
        ArrayList<ArrayList<String>> data = new ArrayList<ArrayList<String>>();

        currentLine = src.readLine();
        int numDataSets = Integer.parseInt(currentLine.split(DELIM)[0]);
        for (int i = 0; i < numDataSets; i += 1)
        {
            currentLine = src.readLine();
            serials.add(currentLine);
            data.add(new ArrayList<String>());
        }
        src.readLine();  // consume column names - already have in `serials`

        while ((currentLine = src.readLine()) != "")
        {
            split = currentLine.split(DELIM);
            for (int i = 0; i < numDataSets; i += 1)
            {
                if ((i * POINT_LEN) < split.length)
                {
                    data.get(i).add(
                            split[i * POINT_LEN + 0] + DELIM
                                + split[i * POINT_LEN + DATETIME_INDEX] + DELIM
                                + split[i * POINT_LEN + 2] + DELIM
                                + split[i * POINT_LEN + TEMP1_INDEX] + DELIM
                                + split[i * POINT_LEN + TEMP2_INDEX] + DELIM
                                + split[i * POINT_LEN + TEMP3_INDEX] + DELIM
                                + split[i * POINT_LEN + HUMIDITY_INDEX] + DELIM
                                + split[i * POINT_LEN + MVS_INDEX] + DELIM
                                + split[i * POINT_LEN + 8] + DELIM
                    );
                }
                // magic numbers are place holders until I figure out what data
                // is at those indices
            }
        }
        src.close();

        String line = "";
        CSVFile dest = CSVFile.create(dest_path);  //toSerial not USED
        // write header
        dest.write(serials.size() + DELIM + "\n");
        for (int i = 0; i < serials.size(); i += 1)
        {
            dest.write(serials.get(i) + "\n");
        }

        // write data
        for (int s = 0; s < serials.size(); s += 1)
        {
            dest.write(serials.get(s).split(DELIM)[0] + DELIM + "\n");

            for (int d = 0; d < data.get(s).size(); d += 1)
            {
                dest.write(data.get(s).get(d) + "\n");
            }
        }
        dest.close();

        return 0;
    }
}
