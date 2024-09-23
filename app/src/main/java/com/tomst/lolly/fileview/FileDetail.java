package com.tomst.lolly.fileview;


//import java.util.List;
import java.time.LocalDateTime;

public class FileDetail
{
    private String name;
    private int iconID;
    private long FileSize;
    private String fullName;
    private boolean isSelected;
    private boolean isUploaded;
    private long  iCount; // pocet udalosti v souboru
    private LocalDateTime iFrom,iInto; // data stazena od - do
    private double latitude, longitude; // souradnice lizatka
    private double minT1, maxT1;
    private double minT2, maxT2;
    private double minT3, maxT3;
    private double minHum,maxHum;

    // getter & setter
    public double getMinT1() { return minT1; }
    public double getMaxT1() {
        return maxT1;
    }
    public double getMinT2() { return minT2; }
    public double getMaxT2() { return maxT2; }
    public double getMinT3() { return minT3; }
    public double getMaxT3() { return maxT3; }
    public double getMinHum() { return minHum; }
    public double getMaxHum() { return maxHum; }
    public void setMinT1(double minT1) { this.minT1 = minT1; }
    public void setMaxT1(double maxT1) { this.maxT1 = maxT1; }
    public void setMinT2(double minT2) { this.minT2 = minT2; }
    public void setMaxT2(double maxT2) { this.maxT2 = maxT2; }
    public void setMinT3(double minT3) { this.minT3 = minT3; }
    public void setMaxT3(double maxT3) { this.maxT3 = maxT3; }
    public void setMinHum(double minHum) { this.minHum = minHum; }
    public void setMaxHum(double maxHum) { this.maxHum = maxHum; }

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
    public long getFileSize() { return FileSize; }
    public long getiCount() {return iCount; }
    public LocalDateTime getiFrom() {return iFrom; }
    public LocalDateTime getiInto() {
        return iInto;
    }
    public double getLatitude() {return latitude; }
    public double getLongitude() {return longitude; }
    public void setFileSize(long size) { FileSize = size; }
    public void setIconID(int iconID) { this.iconID = iconID; }
    public void setName(String name) { this.name = name; }
    public void setFull(String fullName) { this.fullName = fullName; }
    public void setSelected(boolean select)  { isSelected = select;}
    public boolean isSelected() { return isSelected; }
    public boolean isUploaded() { return isUploaded; }
    public void setUploaded(boolean uploaded) { this.isUploaded = uploaded; }

    private void  ClearMembers()
    {
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

    // konstruktor pro bazalni fungovani, bez doplnujicich informaci
    public FileDetail(String filename, int iconID)
    {
        ClearMembers();
        this.iconID = iconID;
        this.name = filename;
    }


    public FileDetail(String filename)
    {
        ClearMembers();
        this.name = filename;
    }

    public FileDetail(String filename, String FullName,  int iconID)
    {
        ClearMembers();
        this.name = filename;
        this.iconID = iconID;
        this.fullName = FullName;
    }

}
