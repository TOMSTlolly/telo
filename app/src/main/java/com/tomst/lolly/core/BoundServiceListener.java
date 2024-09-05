package com.tomst.lolly.core;

public interface BoundServiceListener
{
    public void sendProgress(int progress);
    public void finishedDownloading();
}
