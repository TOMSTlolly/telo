package com.tomst.lolly.core;

import static com.tomst.lolly.core.shared.aft;
import static com.tomst.lolly.core.shared.convToMicro;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

public class DecodeTmsFormat {
    static private byte[] fBuf ;
    static private byte INBUF = 16;
    // static private int fMicroInter = 1279;
    // static private double fMicroSlope = (8890.0 / ( 34000.0 - 1279.0));
    static private int tmpNan = -200;

    static public void SetSafeAddress(int Val)
    {
        SafeAddress = Val;
        fMereni.Address = Val;
    }
    static public int GetSafeAddress(){
        return SafeAddress;
    }

    static private int SafeAddress = 0;  // pri vycitani paketu nastavuju start pro kontrolu, ze mi dosel cely paket

    private static String OFFPATTERN = "yyyy.MM.dd HH:mm";
    private static DateTimeFormatter dateTimeFormatter;
    public static Instant lastDateTrace;
    static private int fIdx =0;

    //private OnGeekEventListener mListener; // listener field
    private static final byte ADR_INDEX = 0;
    private static final byte CMD_INDEX = 1;
    private static final byte PAR_INDEX = 2;


    private static Handler handler = null;
    public static void SetHandler(Handler han){
        handler = han;
    }

    private static TDeviceType devType;
    public static void SetDeviceType(TDeviceType dev){
        devType = dev;

        if (fMereni == null)
            throw new UnsupportedOperationException("Please init fMereni in pars.java first");
        fMereni.dev = dev;
    }


    //@ struct
    static private TMereni fMereni;
    static {
        fMereni = new TMereni();
    }

     static {
        //fMereni = new TMereni();
        fMereniList = new java.util.ArrayList<TMereni>();
    }

     //  mezibuffer pro vyparsovane data, odeslu, az kdyz mi sedi napoctena adresa na konci
    private static List<TMereni> fMereniList;

    private static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return dateFormat.format(date);
    }

    // vstup je casova zona v gsm formatu
    private static String getGTMS(int gtm){
        int d = Math.abs(gtm / 4);
        int m = Math.abs(gtm % 4) ;
        boolean sig  ;

        sig = gtm >0;
        char s = '+';
        if (!sig) {
            s = '-';
        }

        String ret = "";
        switch(m){
            case 0:
                ret = String.format("%c%02d:00",s,d);
                break;

            case 1:
                ret = String.format("%c%02d:15",s,d);
                break;

            case 2:
                ret = String.format("%c%02d:30",s,d);
                break;

            case 3:
                ret = String.format("%c%02d:45",s,d);
                break;

            default:
                throw new ArithmeticException("Illegal GTM (gsm format) input value");

        }
        return(ret);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static Date EncodeDateTime(TMereni AMereni){
        Date dat = new Date();

        //float gtm = ZoneOffset.of("+02:00");
        String gtms = getGTMS(AMereni.gtm);
        System.out.println("gtm = " + gtms);

        OffsetDateTime ret = OffsetDateTime.of(AMereni.year+2000,AMereni.month,AMereni.day,AMereni.hh,AMereni.mm,AMereni.ss,0, ZoneOffset.of(gtms));
        System.out.println("Muj cas "+ret);
        return(dat);
    }

    // vypise obsah bufferu
    private static void dumps(byte[] b){
        String line ="";
        int j;
        for (int i = 0; i<b.length;i++) {
            j = b[i] & 0xff;
            line = line + Integer.toHexString(j) +",";
            //System.out.println(j);
        }
        System.out.println(line);
    }

    // vycisti buffer
    private static void clear(byte[] b)
    {
        for (int i = 0; i<b.length;i++){
            //b[i]=(byte)i;
            b[i]=0;
        }
    }

    public static int copyInt(String reply,int start, int count)
    {
        int i;
        //i = Integer.parseInt(reply.substring(start,start+count));
        String s = reply.substring(start-1,start+count-1);
        i = Integer.parseInt(s);

        return(i);
    }

    public static int copyIntGTM(String reply,int start, int count)
    {
        int i;
        //i = Integer.parseInt(reply.substring(start,start+count));
        String s = reply.substring(start-1,start+count-1);

        // hex value, convert to int
        i = Integer.parseInt(s,16);

        // negative UTC offsets are greater than 0x80
        if (i > 0x80) {
            i = (i - 80) * -1;
        }

        return(i);
    }

    private static int copyHex(String reply, int start, int count)
    {
        String s = reply.substring(start-1,start+count-1);
        int i = Integer.parseInt(s,16);
        return (i);
    }


    public void disassembleDate(String reply, TMereni Mereni){
        //DD 2021 04 28 13 00 00 08
        //12 3456 78 90 12 34 56 78
        int dif = copyInt(reply,3,4);

        Mereni.year  = dif;
        Mereni.month = copyInt(reply,7,2);
        Mereni.day   = copyInt(reply,9,2); //copyByte(ReplyChunk,10,2);
        Mereni.hh    = copyInt(reply,11,2);
        Mereni.mm    = copyInt(reply,13,2);
        Mereni.ss    = copyInt(reply,15,2);
        Mereni.gtm   = copyIntGTM(reply,17,2);
    }

    public String copys(String reply,int start,int count){
        return( reply.substring(start-1,start+count-1) );
    }



    private double con(int b1){
        double ret = 0.0;
        if ((b1 & 8)>0)
            ret = (double)1/2;

        if ((b1 & 4)>0)
            ret = ret + (double)1/4;

        if ((b1 & 2)>0)
            ret = ret + (double)1/8;

        if ((b1 & 1)>0)
            ret = ret + (double)1/16;

        return(ret);
    }

    private int ComplementTwo(int hb,int lb,int shl){
        int ret = 0;

        lb = lb & 0xF0;
        if (shl>0)
            lb = lb << 4;

        ret = hb * 256 + lb;
        ret = 0xFFFF - ret;
        ret = ret +1;
        return(ret);
    }

    private int hi(int val){
        int ret = val / 256;
        return(ret);
    }

    private int lo(int val){
        int ret = val & 0xFF;
        return(ret);
    }

    private double convertTemp(int ATemp){
        ATemp = ATemp * 16;
        int t1 = (ATemp & 0xFF00) >> 8;
        int b1 = ATemp & 0x00FF;

        b1 = b1 & 0xF0;

        // je to zaporna hodnota ?
        double ret = 0.0;
        if ((t1 & 0x80)>0){
            int d = ComplementTwo(t1,b1,0);
            t1 = hi(d);
            b1 = lo(d);
            b1 = b1 >> 4;
            ret = t1 + con(b1);
            ret = -ret ;
        }
        else {
            b1 = b1 >> 4;
            ret = t1 + con(b1);
        }

        return(ret);
    }

    public void disassembleData(String reply, TMereni Mereni){
        // @D 13 00 7A5ADCC5319E3
        // 12 34 56 7890123456789
        //reply = "D133000FADCFF31972";
        //reply = "D13157A5ADCC5318E2";
        String s = copys(reply,9,3);
        if (s.equals("ADC"))
        {
            Mereni.hh = copyInt(reply,2,2);
            Mereni.mm = copyInt(reply,4,2);
            Mereni.ss = 0;

            s = copys(reply,6,2)+copys(reply,12,2);
            Mereni.adc = Integer.parseInt(s,16);
            Mereni.hum = convToMicro(Mereni.adc);

            // zkonvertuj teplotu
            Mereni.Err = 0;
            int tt1 = copyHex(reply,15,3);
            if (!(isTempOK(tt1)))
                Mereni.Err = 0x10 + tError(tt1);
            else
                Mereni.t1 = convertTemp(tt1);

            Mereni.t2 = tmpNan;
            Mereni.t3 = tmpNan;
        }
        else {
            Mereni.hh = copyInt(reply,2,2);
            Mereni.mm = copyInt(reply,4,2);
            Mereni.ss = 0;
            if (Mereni.mm>60){
                Mereni.mvs = 1;
                Mereni.mm = Mereni.mm - 60;
            }

            // konverze humidity
            Mereni.hum = copyHex(reply,6,3);
            Mereni.Err = 0;
            if (Mereni.hum > 0xFFFF)
                Mereni.Err = 2;

            int tt1 = copyHex(reply,9,3);
            int tt2 = copyHex(reply,12,3);
            int tt3 = copyHex(reply,15,3);

            Mereni.t1 = tmpNan;
            Mereni.t2 = tmpNan;
            Mereni.t3 = tmpNan;

            if (!(isTempOK(tt1)))
                Mereni.Err = 0x10 + tError(tt1);
            else
                Mereni.t1 = convertTemp(tt1);
            if (!(isTempOK(tt2)))
                Mereni.Err = 0x10 + tError(tt2);
            else
                Mereni.t2 = convertTemp(tt2);
            if (!(isTempOK(tt3)))
                Mereni.Err = 0x10 + tError(tt3);
            else
                Mereni.t3 = convertTemp(tt3);

            // otresove cidlo
            Mereni.mvs = copyInt(reply,18,1);

            // ulozim typ zarizeni
        }
    }

    private int tNumber (int AValue){
        int ret = (AValue & 0xFF00) / 256; // cislo teplomeru
        return (ret);
    }

    private int tError(int AValue){
        int ret = (AValue & 0x0F);   // cislo chyby
        return (ret);
    }

    private boolean isTempOK(int AValue){
        //int hb,hm,hl,b;

        // 0xAE1
        //hb = ( AValue & 0xf0 ) / 16;  // tady bych mel mit 0x0A
        //b  = ( AValue & 0X0F )     ;  // a tady 0xE1

        if ((AValue & 0xFF0) == 0xAE0)
            return(false);

        if (AValue == 0x7FF)
            return(false);

        if ((AValue==0x800))
            return(false);

        return(true);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void InitConstants(){
        fBuf = new byte [INBUF];
        //TMereni fMereni = new TMereni();
        //fMicroInter = 1279;
        //fMicroSlope = (double)(8890.0 / (34000.0 - 1279.0));
        fIdx = 0;

        fMereni.month = 0;

        dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    }

    /*** Konstruktor, nastavim defaultni hodnoty ***/
    @RequiresApi(api = Build.VERSION_CODES.O)
    public DecodeTmsFormat(){
        //RandomAccessFile file = new RandomAccessFile("/Users/pankaj/Downloads/myfile.txt", "r");
        InitConstants();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public boolean dpacket(String reply)
    {
        if (SafeAddress <0)
            throw new UnsupportedOperationException("Please check that you've set start address before read");

        int iCnt = 0;
        String str = "";
        reply =reply.replaceAll("(\\r|\\n)", "");

        int StartAddr = fMereni.Address;
        StartAddr = SafeAddress;
        fMereniList.clear();

        try {
            int i = 0;
            for (String val : reply.split("@")) {
                // schovavam posledni string

                if (val.length() == 0)
                    continue;

                // ignoruj echo
                if (val.startsWith("<<"))
                    continue;

                if (val.startsWith("D")) {
                    // return from "D" command, we could probably omit the test above

                    if (val.startsWith("DD")) {
                        // je to datum
                        disassembleDate(val, fMereni);

                        if (fMereni.dtm != null)
                          lastDateTrace = fMereni.dtm.toInstant(ZoneOffset.UTC);
                    } else {
                        disassembleData(val, fMereni);

                        if (fMereni.month == 0) {
                            continue;
                        }
                        else {
                            fMereni.dtm = LocalDateTime.of(fMereni.year, fMereni.month, fMereni.day, fMereni.hh, fMereni.mm, fMereni.ss, 0);
                        }
                        fMereni.idx = fIdx;

                        //  append  parsed measure into queue
                        //sendMeasure(fMereni);
                        fMereniList.add(fMereni);

                        fIdx++;

                    }
                }
                else {
                    // address
                    String[] view = val.split(";");
                    int AdrAfter = StrToHex(aft(view[ADR_INDEX],"="));

                    // command
                    char cmd = (view[CMD_INDEX]).charAt(0);

                    // parameter
                    switch(cmd){
                        case 'M':
                            //i = -1;
                            break;
                        case 'D':
                            break;
                        case 'C':
                            break;
                        case '&':
                            // hacking wrong address increase for  @E=$000000;&;&93%01.50#92201235 packet
                            if  ((SafeAddress >=0) && (i ==0))
                                i = 0;
                            break;
                        case '?':
                            break;
                        default:
                            break;
                    }
                    // pro vystup z vystup z prohlizecky bez dat kaslu na kontrolu adresy
                    // mezi dvouma vystupama ze ctecky nevim, jestli je  offset +1 * 8
                    boolean ret = (i==0);
                    if (!ret) {
                        // ukazali se mi nejake  udalosti s @D na zacatku
                        int FinComp= (8 * (i + 1) + StartAddr) ;
                        ret = (AdrAfter == FinComp);
                        if (ret)
                            sendWholePacket();
                    }
                   if (ret){
                        SafeAddress = AdrAfter;
                        fMereni.Address = AdrAfter;
                    }
                    return ret;
                    //E=$000010;M;01
                    //E=$06D5F8;C;2023/10/20,09:14:49+04
                    //E=$06E700;D;2023/10/26,00:00:00+04
                    //E=$06D5D8;&;&93%01.80#94232790
                }
                i++;
            }
            //return (str);
         }

        catch (Exception e) {
            Log.e("TAG",e.toString());
        };

        return false;
    }

   private void sendWholePacket(){
        if (handler == null)
            return;

        for (TMereni mer : fMereniList) {
            sendMeasure(mer);
        }
    }


    // prevede hexa "$AABBCC" na integer
    private int StrToHex(String val)
    {
        return Integer.parseInt(val.substring(1),16);
    }



    private void sendMeasure (TMereni mer) { // Handle sending message back to handler
        if (handler == null)
            return;

        Message message = handler.obtainMessage();
        message.obj = new TMereni(mer);
        handler.sendMessage(message);
    }


}
