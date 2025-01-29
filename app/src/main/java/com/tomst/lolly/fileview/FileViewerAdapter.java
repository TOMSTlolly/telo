package com.tomst.lolly.fileview;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;

import com.tomst.lolly.R;
import com.tomst.lolly.core.Constants;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


public class FileViewerAdapter extends BaseAdapter
{
    private final Context mContext;
    private List<FileDetail> mAllFiles;
    private int selectedPosition = -1;

    public FileViewerAdapter(Context mContext, List<FileDetail> mAllFiles)
    {
        this.mContext = mContext;
        this.mAllFiles = mAllFiles;
    }

    @Override
    public int getCount()
    {
        return mAllFiles.size();
    }

    @Override
    public FileDetail getItem(int position)
    {
        return mAllFiles.get(position);
    }


    @Override
    public long getItemId(int position)
    {
        return position;
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater myInflater = LayoutInflater.from(mContext);
            convertView = myInflater.inflate(R.layout.rowitem, parent, false);
            holder = new ViewHolder();

            //id_imageView_card_tracktype
            holder.trackTypeIcon = convertView.findViewById(R.id.id_imageView_card_tracktype);  // pravy horni roh ikona
            holder.cloudIcon = convertView.findViewById(R.id.cloudIcon);
            holder.imageView = convertView.findViewById(R.id.iconID);
            holder.checkBox  = convertView.findViewById(R.id.checkBox);
            holder.trackName = convertView.findViewById(R.id.id_textView_card_TrackName);
            holder.annotation= convertView.findViewById(R.id.id_textView_card_TrackDesc);
            holder.from = convertView.findViewById(R.id.id_file_from);
            holder.into = convertView.findViewById(R.id.id_file_into);
            holder.count = convertView.findViewById(R.id.id_file_count);
            holder.size = convertView.findViewById(R.id.id_file_size);
            holder.mintx = convertView.findViewById(R.id.id_min_tx);
            holder.maxtx = convertView.findViewById(R.id.id_max_tx);
            holder.minhum = convertView.findViewById(R.id.id_min_hum);
            holder.maxhum = convertView.findViewById(R.id.id_max_hum);

            convertView.setTag(holder);
        }
        else {
            holder = (ViewHolder) convertView.getTag();
        }

        FileDetail currentFile = mAllFiles.get(position);

        holder.trackName.setText(currentFile.getNiceName());  // tohle zobrazuju v titulce !!!
        holder.imageView.setImageResource(0);

        // doplnim statistiku, pokud je to znamy datovy soubor
        if (currentFile.errFlag==Constants.PARSER_OK)
        {
           // DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.of("UTC"));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            String formattedFrom = currentFile.getiFrom().toString();
            String formattedInto = currentFile.getiInto().toString();

            //holder.trackName.setText(currentFile.getNiceName());  // tohle zobrazuju v titulce !!!
            holder.count.setText(String.valueOf(currentFile.getiCount()));
            holder.annotation.setText("Parser OK");
            holder.from.setText(formattedFrom);
            holder.into.setText(formattedInto);
            holder.mintx.setText(String.valueOf(currentFile.getMinT1()));
            holder.maxtx.setText(String.valueOf(currentFile.getMaxT1()));
            holder.minhum.setText(String.valueOf(currentFile.getMinHum()));
            holder.maxhum.setText(String.valueOf(currentFile.getMaxHum()));
            holder.size.setText(String.valueOf(currentFile.getFileSize()));

            // zbyla cast stareho holderu
            holder.trackTypeIcon.setImageResource(R.drawable.ic_expand_arrow)  ;

            holder.imageView.setImageResource(R.drawable.cog);
            holder.imageView.setImageResource(R.drawable.cog);
            holder.checkBox.setText(currentFile.getName());
            holder.checkBox.setChecked(currentFile.isSelected());
            holder.annotation.setTextColor(ContextCompat.getColor(mContext, R.color.default_text_color));
        }
        else
        {
            //holder.cloudIcon.setImageResource(R.drawable.ic_bookmark);
            holder.imageView.setImageResource(R.drawable.ic_file_download);
            holder.annotation.setText("Parser Error");
            holder.annotation.setTextColor(ContextCompat.getColor(mContext, R.color.color_accent));
        }

        holder.imageView.setVisibility(View.VISIBLE);

        holder.checkBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
        Log.d("FILEVIEWER", getFullName(position) + " selected = " + isChecked);
        currentFile.setSelected(isChecked);
        });

        // Change background color based on selection state
        if (position == selectedPosition) {
            convertView.setBackgroundColor(Color.LTGRAY);
        } else {
            convertView.setBackgroundColor(Color.TRANSPARENT);
        }

        return convertView;
    }

    private static class ViewHolder {
        ImageView cloudIcon;
        ImageView imageView;

        ImageView trackTypeIcon;    // tohle je ikona trasy

        CheckBox checkBox;
        TextView trackName;
        TextView annotation;

        TextView from;
        TextView into;
        TextView count;
        TextView size;

        TextView maxtx;
        TextView mintx;
        TextView maxhum;
        TextView minhum;
    }

    public void setSelectedPosition(int position) {
        selectedPosition = position;
        notifyDataSetChanged();
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
