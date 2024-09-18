package com.tomst.lolly.fileview;


//import java.util.List;
import java.time.LocalDateTime;

public class FileDetail
{
    private String name;
    private int iconID;
    private int FileSize;
    private String fullName;
    private boolean isSelected;
    private boolean isUploaded;
    private int iCount; // pocet udalosti v souboru
    private LocalDateTime iFrom,iInto; // data stazena od - do
    private double latitude, longitude; // souradnice lizatka

    // settery pro doplnujici informace
    public void setAnnotation(String annotation){ fullName = annotation; }
    public void setCount(int count){ iCount = count; }
    public void setFrom(LocalDateTime from){ iFrom = from; }
    public void setInto(LocalDateTime into){ iInto = into; }
    public void setLatitude(double lat){ latitude = lat; }
    public void setLongitude(double lon){ longitude = lon; }

    public String getName() { return name; }
    public int getIconID() { return iconID; }
    public String getFull() { return fullName; }
    public int getFileSize() { return FileSize; }
    public int getiCount() {return iCount; }
    public LocalDateTime getiFrom() {return iFrom; }
    public LocalDateTime getiInto() {return iInto; }
    public double getLatitude() {return latitude; }
    public double getLongitude() {return longitude; }
    public void setFileSize(int size) { FileSize = size; }
    public void setIconID(int iconID) { this.iconID = iconID; }
    public void setName(String name) { this.name = name; }
    public void setFull(String fullName) { this.fullName = fullName; }
    public void setSelected(boolean select)  { isSelected = select;}
    public boolean isSelected() { return isSelected; }
    public boolean isUploaded() { return isUploaded; }
    public void setUploaded(boolean uploaded) { this.isUploaded = uploaded; }

    // konstruktor pro bazalni fungovani, bez doplnujicich informaci
    public FileDetail(String filename, int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName =  "";
        this.isSelected = false;
        this.isUploaded = false;

        this.iCount = 0;        //  pocet udalosti
        this.iFrom = null;    //  data stazeni od
        this.iInto = null;       // data stazeni do
        this.latitude = 0;     //  souradnice lizatka
        this.longitude = 0;  //
        this.FileSize = 0;      // velikost v kbytes
    }


    public FileDetail(String filename, String FullName,  int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName = FullName;
        this.isSelected = false;
        this.isUploaded = false;
    }



}
