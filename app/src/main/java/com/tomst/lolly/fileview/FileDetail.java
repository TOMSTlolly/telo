package com.tomst.lolly.fileview;


import java.util.List;

public class FileDetail
{
    private String name;
    private int iconID;
    private int FileSize;
    private String fullName;
    private boolean isSelected;
    private boolean isUploaded;


    public FileDetail(String filename, int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName =  "";
        this.isSelected = false;
        this.isUploaded = false;
    }


    public FileDetail(String filename, String FullName,  int iconID)
    {
        this.name = filename;
        this.iconID = iconID;
        this.fullName = FullName;
        this.isSelected = false;
        this.isUploaded = false;
    }

    public String getName() { return name; }


    public int getIconID() { return iconID; }


    public String getFull() { return fullName; }


    public int getFileSize() { return FileSize; }


    public void setSelected(boolean select)
    {
        isSelected = select;
    }


    public boolean isSelected() { return isSelected; }

    public boolean isUploaded() { return isUploaded; }

    public void setUploaded(boolean uploaded) { this.isUploaded = uploaded; }

}
