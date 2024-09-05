package com.tomst.lolly.fileview;

import android.content.Context;
import android.media.Image;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.tomst.lolly.R;

import java.util.ArrayList;
import java.util.List;


public class FileViewerAdapter extends BaseAdapter
{
    private final Context mContext;
    private List<FileDetail> mAllFiles;

    public FileViewerAdapter(Context mContext, List<FileDetail> mAllFiles)
    {
        this.mContext = mContext;
        this.mAllFiles = mAllFiles;
    }

    @Override
    public int getCount()
    {
        if (mAllFiles == null || mAllFiles.size() == 0)
        {
            return -1;
        }

        return mAllFiles.size();
    }

    @Override
    public FileDetail getItem(int position)
    {
        if (mAllFiles == null || mAllFiles.size() == 0)
        {
            return null;
        }

        return mAllFiles.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return 0;
    }


    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        View view = convertView;
        String name = "";
        if (view == null)
        {
            LayoutInflater myInflater = LayoutInflater.from(mContext);
            view = myInflater.inflate(
                    R.layout.rowitem, parent, false
            );

            FileDetail currentFile = mAllFiles.get(position);
            ImageView cloudIcon = (ImageView) view.findViewById(R.id.cloudIcon);
            if (currentFile.isUploaded())
            {
               cloudIcon.setVisibility(View.VISIBLE);
            }
            else
            {
                cloudIcon.setVisibility(View.GONE);
            }

            ImageView imageView = (ImageView) view.findViewById(R.id.iconID);
//            TextView textView = (TextView) view.findViewById(R.id.president);
            CheckBox checkBox = (CheckBox) view.findViewById(R.id.checkBox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
            {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b)
                {
                    Log.d("FILEVIEWER", getFullName(position) + " selected = " + b);
                    mAllFiles.get(position).setSelected(b);
                }
            });

            name = mAllFiles.get(position).getName();
            checkBox.setText(name);
            imageView.setImageResource(mAllFiles.get(position).getIconID());
            checkBox.setChecked(false);
        }

        return view;
    }

    public String getShortName(int position)
    {
        return mAllFiles.get(position).getName();
    }


    public String getFullName(int position)
    {
        return mAllFiles.get(position).getFull();
    }


    public boolean isSelected(int position)
    {
        return mAllFiles.get(position).isSelected();
    }

    public boolean isUploaded(int position)
    {
        return mAllFiles.get(position).isUploaded();
    }

    public List<FileDetail> getAllFiles()
    {
        return mAllFiles;
    }


    public ArrayList<String> collectSelected()
    {
        ArrayList<String> selected = new ArrayList<>();
        for (FileDetail fileDetail : mAllFiles)
        {
            if (fileDetail.isSelected())
            {
                selected.add(fileDetail.getFull());
            }
        }

        return selected;
    }
}
