package com.tomst.lolly.core;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.github.mikephil.charting.data.Entry;

//import com.tomst.lolly.core.SingleLiveEvent;

import java.util.ArrayList;


public class DmdViewModel extends ViewModel
{
    private final SavedStateHandle state;
    // Holds the names of one or more datasets the user will want to visualize
    private final MutableLiveData<String> messageContainerGraph;
    private int fIdx = 0;
    private TDeviceType fDevType;
    private ArrayList<Entry> valT1 = new ArrayList<>();
    private ArrayList<Entry> valT2 = new ArrayList<>();
    private ArrayList<Entry> valT3 = new ArrayList<>();
    private ArrayList<Entry> valHA = new ArrayList<>();


    public void SetDeviceType(TDeviceType val){
        fDevType = val;
    }
    // vrati data, ktera sem nahral TMD adapter pomoci AddMereni nize.

    public ArrayList<Entry> getT1(){
        return valT1;
    }


    public ArrayList<Entry> getT2(){
        return valT2;
    }


    public ArrayList<Entry> getT3(){
        return valT3;
    }


    public ArrayList<Entry> getHA(){
        return valHA;
    }


    public void AddMereni(TMereni mer)
    {
        if (mer.dev == TDeviceType.dUnknown)
        {
            if ((mer.t2 > -150) || (mer.t3 > -150))
            {
                mer.dev = TDeviceType.dLolly4;
            }
            else if ((mer.t1 > -150) && (mer.hum > 0))
            {
                mer.dev = TDeviceType.dAD;
            }
            else if (mer.t1 > -150)
            {
                mer.dev = TDeviceType.dTermoChron;
            }
            else
            {
                throw new UnsupportedOperationException("AddMereni, Unknown device");
            }
        }

        switch (mer.dev)
        {
            case dLolly3:
                valT1.add(new Entry(fIdx, (float)mer.t1 ));
                valT2.add(new Entry(fIdx, (float)mer.t2 ));
                valT3.add(new Entry(fIdx, (float)mer.t3 ));
                valHA.add(new Entry(fIdx, (float)mer.hum));
                break;

            case dLolly4:
                valT1.add(new Entry(fIdx, (float)mer.t1 ));
                valT2.add(new Entry(fIdx, (float)mer.t2 ));
                valT3.add(new Entry(fIdx, (float)mer.t3 ));
                valHA.add(new Entry(fIdx, (float)mer.hum));
                break;

            case dAD:
                valT1.add(new Entry(fIdx, (float)mer.t1 ));
                valHA.add(new Entry(fIdx, (float)mer.hum));
                break;

            case dAdMicro:
                valT1.add(new Entry(fIdx, (float)mer.t1 ));
                valHA.add(new Entry(fIdx, (float)mer.hum));
                break;

            case dTermoChron:
                valT1.add(new Entry(fIdx, (float)mer.t1 ));
                break;

            default:
                throw new UnsupportedOperationException("AddMereni, Unknown device");
        }
        fIdx ++;
    }


    public void ClearMereni()
    {
        valT1.clear();
        valT2.clear();
        valT3.clear();
        valHA.clear();
        fIdx = 0;
    }


    public DmdViewModel(SavedStateHandle state)
    {
        this.state = state;
        messageContainerGraph = state.getLiveData("Starting value");
       // messageContainerGraph = new SingleLiveEvent<>();
    }


    public void sendMessageToGraph(String msg)
    {
        messageContainerGraph.setValue(msg);
    }


    public LiveData<String> getMessageContainerGraph()
    {
        return messageContainerGraph;
    }
}