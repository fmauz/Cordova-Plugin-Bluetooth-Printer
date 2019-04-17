package com.ru.cordova.printer.bluetooth;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.text.DateFormat;


import java.util.Arrays;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Bitmap.Config;
import android.util.Xml.Encoding;
import android.util.Base64;
import java.util.ArrayList;
import java.util.List;


public class BluetoothPrinter extends CordovaPlugin {

    byte[] normalWidth = {0x1b, 0x40, 0x1d, 0x21, 0x00 };
    byte[] doubleWidth = {0x1b, 0x40, 0x1d, 0x21, 0x10 };
    byte[] double2xWidth = {0x1b, 0x40, 0x1d, 0x21, 0x11 };

    byte[] alignLeft = {0x1b, 0x40, 0x1b, 0x61, 0x00 };
    byte[] alignCenter = {0x1b, 0x40, 0x1b, 0x61, 0x01 };
    byte[] alignRight = {0x1b, 0x40, 0x1b, 0x61, 0x02 };

    byte[] alignCenterWidth1x = {0x1b, 0x40, 0x1d, 0x21, 0x10, 0x1b, 0x61, 0x01 };
    byte[] alignCenterWidth2x = {0x1b, 0x40, 0x1d, 0x21, 0x11, 0x1b, 0x61, 0x01 };
    byte[] alignCenterWidth3x = {0x1b, 0x40, 0x1d, 0x21, 0x21, 0x1b, 0x61, 0x01 };

    byte[] alignLeftWidth1x = {0x1b, 0x40, 0x1d, 0x21, 0x10, 0x1b, 0x61, 0x00 };
    byte[] alignRightWidth1x = {0x1b, 0x40, 0x1d, 0x21, 0x10, 0x1b, 0x61, 0x02 };

    private static final String LOG_TAG = "BluetoothPrinter";
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    InputStream mmInputStream;
    Thread workerThread;
    byte[] readBuffer;
    int readBufferPosition;
    int counter;
    volatile boolean stopWorker;
    Bitmap bitmap;

    public BluetoothPrinter() {
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("list")) {
            listBT(callbackContext);
            return true;
        } else if (action.equals("connect")) {
            String name = args.getString(0);
            if (findBT(callbackContext, name)) {
                try {
                    connectBT(callbackContext);
                } catch (IOException e) {
                    Log.e(LOG_TAG, e.getMessage());
                    e.printStackTrace();
                }
            } else {
                callbackContext.error("Bluetooth Device Not Found: " + name);
            }
            return true;
        } else if (action.equals("disconnect")) {
            try {
                disconnectBT(callbackContext);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("print") || action.equals("printImage")) {
            try {
                String msg = args.getString(0);
                printImage(callbackContext, msg);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("printText")) {
            try {
                String msg = args.getString(0);
                printText(callbackContext, msg);
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("printPoule")) {
            try {
                JSONObject poule = args.getJSONObject(0);
                printBeautyPoule(callbackContext, poule );
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }else if (action.equals("printCanceled")) {
            try {
                JSONObject poule = args.getJSONObject(0);
                String printType = args.getString(1);
                printCanceled(callbackContext, poule, printType );
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }else if (action.equals("printPayment")) {
            try {
                JSONObject poule = args.getJSONObject(0);
                String printType = args.getString(1);
                printPayment(callbackContext, poule, printType );
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("printResult")) {
            try {
                JSONObject poule = args.getJSONObject(0);
                String printType = args.getString(1);
                printResult(callbackContext, poule, printType);
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }else if (action.equals("printReport")) {
            try {
                JSONObject poule = args.getJSONObject(0);
                String printType = args.getString(1);
                printReport(callbackContext, poule, printType );
            } catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        } else if (action.equals("printPOSCommand")) {
            try {
                String msg = args.getString(0);
                printPOSCommand(callbackContext, hexStringToBytes(msg));
            } catch (IOException e) {
                Log.e(LOG_TAG, e.getMessage());
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    JSONObject findItem( JSONArray collection, String key, int value ){
        try{
            for(int i=0; i<collection.length(); i++) {
                JSONObject item = collection.getJSONObject(i);
                if( item.getInt(key) == value ){
                    return item;
                }
            }
        }catch(JSONException e){

        }
        return null;
    }

    String getHeader(JSONObject poule){
        String pouleMsg = "";
        try{
            pouleMsg += poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getString("name") + "\n\n";
            pouleMsg += "Via do cliente" + "\n\n";
            pouleMsg += "Numero da Poule: " + poule.getJSONObject("jb_poule").getString("poule_id") + "\n";
            pouleMsg += "Horario: " + poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getJSONObject("jb_schedule_session").getString("shift") + "\n\n";
        }catch(JSONException e){

        }
        return pouleMsg;
    }

    String getBarcode(JSONObject poule){
        String pouleMsg = "";
        try{
            pouleMsg += "CODIGO DE BARRAS:\n 00000000" + poule.getJSONObject("jb_poule").getString("poule_id") + "\n\n";
        }catch(JSONException e){

        }
        return pouleMsg;
    }

    boolean printCashFlow( CallbackContext callbackContext, JSONObject poule){
        return true;
    }


    boolean printPayment(CallbackContext callbackContext, JSONObject poule, String paperType ){
        try {
            String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(alignCenterWidth2x);
            }
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getString("name") + "\n\n").getBytes() );

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(normalWidth);
                mmOutputStream.write(alignCenter);
            }
            mmOutputStream.write( "Via do ponto\n\n".getBytes() );

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(alignLeftWidth1x);
            }
            mmOutputStream.write( "Pule: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getString("poule_id") + "\n").getBytes() );
            mmOutputStream.write( "Data: ".getBytes() );
            String target = poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getString("date").split("T")[0];
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date result =  df.parse(target);
            String dateSchedule = new SimpleDateFormat("dd/MM/yyyy").format(result);

            mmOutputStream.write( (dateSchedule + "\n").getBytes() );
            mmOutputStream.write( "Extracao: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getJSONObject("jb_schedule_session").getString("shift") + "\n").getBytes() );
            mmOutputStream.write( "Ponto: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getString("name") + "\n").getBytes() );

            Double amount_award = poule.getJSONObject("jb_poule").getDouble("amount_award");
            JSONArray children = poule.getJSONObject("jb_poule").getJSONArray("children");
            for(int i=0; i<children.length(); i++) {
                amount_award += children.getJSONObject(i).getDouble("amount_award");
            }
            
            if( poule.getJSONObject("jb_poule").getString("canceled_at") != "null" ){
                mmOutputStream.write( "Status: Cancelada\n".getBytes() );
            }else{
                mmOutputStream.write( "Premio: ".getBytes() );

                NumberFormat z = NumberFormat.getCurrencyInstance();
                mmOutputStream.write( (z.format( amount_award ) + "\n").getBytes() );

                if( poule.getJSONObject("jb_poule").getString("pay_at") != "null" ){
                    mmOutputStream.write( "Status: Paga\n".getBytes() );
                }
            }

            mmOutputStream.write("\n".getBytes());

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(normalWidth);
                mmOutputStream.write(alignCenter);
            }
            mmOutputStream.write( ( "Data da impressao\n").getBytes() );
            mmOutputStream.write( (currentDate).getBytes() );
            mmOutputStream.write("\n\n\n\n".getBytes());

            callbackContext.success("Data Sent");
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    boolean printCanceled(CallbackContext callbackContext, JSONObject poule, String paperType ){
        try {
            String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(alignCenterWidth2x);
            }
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getString("name") + "\n\n").getBytes() );

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(normalWidth);
                mmOutputStream.write(alignCenter);
            }
            mmOutputStream.write( "Via do ponto\n\n".getBytes() );

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(alignLeftWidth1x);
            }
            mmOutputStream.write( "Pule: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getString("poule_id") + "\n").getBytes() );
            mmOutputStream.write( "Data: ".getBytes() );
            String target = poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getString("date").split("T")[0];
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date result =  df.parse(target);
            String dateSchedule = new SimpleDateFormat("dd/MM/yyyy").format(result);

            mmOutputStream.write( (dateSchedule + "\n").getBytes() );
            mmOutputStream.write( "Extracao: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getJSONObject("jb_schedule_session").getString("shift") + "\n").getBytes() );
            mmOutputStream.write( "Ponto: ".getBytes() );
            mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getString("name") + "\n").getBytes() );

            if( poule.getJSONObject("jb_poule").getString("canceled_at") != "null" ){
                mmOutputStream.write( "Status: Cancelada\n".getBytes() );
            }else{
                mmOutputStream.write( "Premio: ".getBytes() );

                NumberFormat z = NumberFormat.getCurrencyInstance();
                mmOutputStream.write( (z.format( poule.getJSONObject("jb_poule").getDouble("amount_award") ) + "\n").getBytes() );

                if( poule.getJSONObject("jb_poule").getString("pay_at") != "null" ){
                    mmOutputStream.write( "Status: Paga\n".getBytes() );
                }
            }

            mmOutputStream.write("\n".getBytes());

            if( paperType.equals("80mm") ) {
                mmOutputStream.write(normalWidth);
                mmOutputStream.write(alignCenter);
            }
            mmOutputStream.write( ( "Data da impressao\n").getBytes() );
            mmOutputStream.write( (currentDate).getBytes() );
            mmOutputStream.write("\n\n\n\n".getBytes());

            callbackContext.success("Data Sent");
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    boolean printPoule(CallbackContext callbackContext, JSONObject poule, String paperType ){
      try {
          String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
          JSONArray children = poule.getJSONObject("jb_poule").getJSONArray("children");

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth3x);
          }
          mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getString("name") + "\n").getBytes() );

          if( poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getInt("id") == 5 ){
              if( paperType.equals("80mm") ) {
                  mmOutputStream.write(alignCenterWidth1x);
              }
              mmOutputStream.write( "Tradicao em novos tempos\n\n".getBytes() );
          }else{
              mmOutputStream.write( "\n".getBytes() );
          }

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(normalWidth);
              mmOutputStream.write(alignCenter);
          }
          mmOutputStream.write( "Via do cliente\n\n".getBytes() );

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignLeftWidth1x);
          }
          mmOutputStream.write( "Pule: ".getBytes() );
          mmOutputStream.write( (poule.getJSONObject("jb_poule").getString("poule_id") + "\n").getBytes() );
          mmOutputStream.write( "Data: ".getBytes() );
          String target = poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getString("date").split("T")[0];
          DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
          Date result =  df.parse(target);
          String dateSchedule = new SimpleDateFormat("dd/MM/yyyy").format(result);

          mmOutputStream.write( (dateSchedule + "\n").getBytes() );
          mmOutputStream.write( "Extracao: ".getBytes() );

          String schedule = poule.getJSONObject("jb_poule").getJSONObject("jb_schedule").getJSONObject("jb_schedule_session").getString("shift");
          Double totalAmount = poule.getJSONObject("jb_poule").getDouble("amount_award");

          for(int i=0; i < children.length(); i++) {
              JSONObject jbPouleChildren = children.getJSONObject(i);
              totalAmount += jbPouleChildren.getDouble("amount_award");
              schedule += ", " + jbPouleChildren.getJSONObject("jb_schedule").getJSONObject("jb_schedule_session").getString("shift");
          }

          mmOutputStream.write( (schedule + "\n").getBytes() );


          mmOutputStream.write( "Ponto: ".getBytes() );
          mmOutputStream.write( (poule.getJSONObject("jb_poule").getJSONObject("station").getString("name") + "\n").getBytes() );

          if( poule.getJSONObject("jb_poule").getString("canceled_at") != "null" ){
              mmOutputStream.write( "Status: Cancelada\n".getBytes() );
          }else{
                if( totalAmount > 0 ){
                    mmOutputStream.write( "Premio: ".getBytes() );
                    
                    NumberFormat z = NumberFormat.getCurrencyInstance();
                    mmOutputStream.write( (z.format( totalAmount ) + "\n").getBytes() );
                }

              if( poule.getJSONObject("jb_poule").getString("pay_at") != "null" ){
                  mmOutputStream.write( "Status: Paga\n".getBytes() );
              }
          }

          mmOutputStream.write("\n".getBytes());

          Double total = 0.0;
          Double totalEst = 0.0;

          JSONArray jbPouleItems = poule.getJSONObject("jb_poule").getJSONArray("jb_poule_items");
          JSONArray pouleItems = poule.getJSONObject("jb_poule").getJSONArray("poule_items");

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth1x);
          }
          mmOutputStream.write( "Jogos\n\n".getBytes() );


          for(int i=0; i<jbPouleItems.length(); i++) {
              if( paperType.equals("80mm") ) {
                  mmOutputStream.write(alignLeftWidth1x);
              }
              JSONObject jbPouleItem = jbPouleItems.getJSONObject(i);
              JSONObject pouleItem = findItem( pouleItems, "id", jbPouleItem.getInt("poule_item_id") );
              if( jbPouleItem != null ){
                  String firstGameLine = "";
                  String lastGameLine = "";
                  boolean firstLine = true;
                  String[] lines = jbPouleItem.getString("query_game").split(" ");
                  for( String line : lines ){
                      if( !line.isEmpty() && line != "" ){
                          if( firstLine ) {
                              firstLine = line.indexOf("/") < 0;
                          }
                          if( firstLine ){
                              firstGameLine += line + " ";
                          }else{
                              lastGameLine += line + " ";
                          }
                      }
                  }

                  mmOutputStream.write( (firstGameLine + "\n").getBytes() );
                  mmOutputStream.write( (lastGameLine + "\n").getBytes() );
                  if( paperType.equals("80mm") ) {
                      mmOutputStream.write(alignRightWidth1x);
                  }
              }

              if( pouleItem != null ){
                  total += pouleItem.getDouble("amount");

                  NumberFormat z = NumberFormat.getCurrencyInstance();
                  mmOutputStream.write( (z.format( pouleItem.getDouble("amount") ) + "\n\n").getBytes() );
              }
          }

          mmOutputStream.write("\n".getBytes());
          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignLeftWidth1x);
          }
          mmOutputStream.write( (jbPouleItems.length() + " Jogo").getBytes() );
          if( jbPouleItems.length() > 1){
              mmOutputStream.write("s".getBytes());
          }

          mmOutputStream.write( "\n".getBytes() );

          NumberFormat z = NumberFormat.getCurrencyInstance();
          mmOutputStream.write( ("Total: " + z.format( total * (children.length() + 1)  ) + "\n\n").getBytes() );
          
          mmOutputStream.write("\n".getBytes());
          if( paperType.equals("80mm") ) {
              mmOutputStream.write(normalWidth);
              mmOutputStream.write(alignCenter);
          }

          try{
              String printMessage = poule.getJSONObject("jb_poule").getJSONObject("station").getJSONObject("division").getJSONArray("game_divisions").getJSONObject(0).getString("print_message");
              mmOutputStream.write( (printMessage + "\n").getBytes() );
          }catch(Exception e){
              mmOutputStream.write("\n".getBytes());
          }

          mmOutputStream.write( (currentDate + "\n\n").getBytes() );

          if( poule.getJSONObject("jb_poule").getJSONObject("poule_status").getInt("id") == 1 ){
              if( paperType.equals("80mm") ) {
                  mmOutputStream.write(alignCenterWidth3x);
              }
              mmOutputStream.write( "BOA SORTE !!!".getBytes() );
          }

          mmOutputStream.write("\n\n\n\n".getBytes());

          callbackContext.success("Data Sent");
          return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    boolean printBeautyPoule(CallbackContext callbackContext, JSONObject poule ){
        byte[] initializePrinter = {0x1B, 0x40 };
        byte[] FONT_1X = {0x1D, 0x21, 0x00 };
        byte[] FONT_2X = {0x1D, 0x21, 0x11 };
        byte[] FONT_2M = {0x1D, 0x21, 0x01 };
        byte[] FONT_3X = {0x1D, 0x21, 0x21 };
        byte[] ALIGN_LEFT = {0x1B, 0x61, 0x00 };
        byte[] ALIGN_CENTER = {0x1B, 0x61, 0x01 };
        byte[] ALIGN_RIGHT = {0x1B, 0x61, 0x02 };

        try {
            mmOutputStream.write(initializePrinter);

            mmOutputStream.write(ALIGN_CENTER);
            mmOutputStream.write(FONT_2X);
            mmOutputStream.write("Desenvolvimento\n".getBytes());
            mmOutputStream.write("2700154\n\n".getBytes());

            mmOutputStream.write(ALIGN_LEFT);
            mmOutputStream.write(FONT_1X);
            mmOutputStream.write("PONTO DE TESTE\n".getBytes());
            mmOutputStream.write("Extracao\n".getBytes("UTF-8"));
            mmOutputStream.write("  28/03/2019 21:00\n".getBytes());
            mmOutputStream.write("  28/03/2019 18:00\n".getBytes());
            mmOutputStream.write("\n".getBytes());
            
            mmOutputStream.write(ALIGN_CENTER);
            mmOutputStream.write(FONT_2X);
            mmOutputStream.write("Jogos\n\n".getBytes());

            mmOutputStream.write(FONT_1X);
            mmOutputStream.write(ALIGN_LEFT);
            mmOutputStream.write("M 1100 1233 1113 3321\n".getBytes());
            mmOutputStream.write("1/1 1/5\n".getBytes());
            mmOutputStream.write(ALIGN_RIGHT);
            mmOutputStream.write("R$ 10,00\n".getBytes());

            mmOutputStream.write(ALIGN_LEFT);
            mmOutputStream.write("MC 1100 1233 1113 3321\n".getBytes());
            mmOutputStream.write("1/1 1/5\n".getBytes());

            mmOutputStream.write(ALIGN_RIGHT);
            mmOutputStream.write("R$ 10,00\n".getBytes());

            mmOutputStream.write("----------------\n".getBytes());
            mmOutputStream.write("Total ".getBytes());
            mmOutputStream.write("R$ 20,00\n\n".getBytes());

            mmOutputStream.write(ALIGN_CENTER);
            mmOutputStream.write(FONT_1X);
            mmOutputStream.write("Confira o seu jogo!\n".getBytes());
            mmOutputStream.write("6 dias para reclamacoes\n".getBytes());
            mmOutputStream.write("20/01/2019 01:14:55\n".getBytes());
            mmOutputStream.write("Via do cliente\n".getBytes());
            mmOutputStream.write("Boa Sorte!\n".getBytes());

            mmOutputStream.write("\n\n\n".getBytes());

            callbackContext.success("Data Sent");
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }


    boolean printResult(CallbackContext callbackContext, JSONObject result, String paperType){
      try {
          String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth3x);
          }

          mmOutputStream.write( (result.getString("division") + "\n").getBytes() );

          if( result.getString("division").equals("Nova BH") ){
              if( paperType.equals("80mm") ) {
                  mmOutputStream.write(alignCenterWidth1x);
              }
              mmOutputStream.write( "Tradicao em novos tempos\n\n".getBytes() );
          }else{
              mmOutputStream.write( "\n".getBytes() );
          }

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth1x);
          }
          mmOutputStream.write( ( result.getString("station") + "\n\n").getBytes() );

          mmOutputStream.write( (result.getString("date") + "\n").getBytes() );
          mmOutputStream.write( (result.getString("shift") + "\n\n").getBytes() );

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(doubleWidth);
          }

          mmOutputStream.write( ("1. " + result.getString("result_1") + "\n").getBytes() );
          mmOutputStream.write( ("2. " + result.getString("result_2") + "\n").getBytes() );
          mmOutputStream.write( ("3. " + result.getString("result_3") + "\n").getBytes() );
          mmOutputStream.write( ("4. " + result.getString("result_4") + "\n").getBytes() );
          mmOutputStream.write( ("5. " + result.getString("result_5") + "\n").getBytes() );
          mmOutputStream.write( ("6.  " + result.getString("result_6") + "\n").getBytes() );
          mmOutputStream.write( ("7.  " + result.getString("result_7") + "\n").getBytes() );

          if( !(result.getString("result_8").isEmpty() || result.getString("result_8") == null || result.getString("result_8") == "") ){
              mmOutputStream.write( ("8.   " + result.getString("result_8") + "\n\n").getBytes() );
          }

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenter);
          }
          mmOutputStream.write( ( "Data da impressao\n").getBytes() );
          mmOutputStream.write( (currentDate + "\n\n").getBytes() );

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth3x);
          }
          mmOutputStream.write( "BOA SORTE !!!".getBytes() );

          mmOutputStream.write( "\n\n\n".getBytes() );

          callbackContext.success("Data Sent");
          return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    boolean printReport(CallbackContext callbackContext, JSONObject result, String paperType ){
      try {
          String currentDate = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth2x);
          }
          mmOutputStream.write( (result.getString("division") + "\n\n").getBytes() );


          JSONObject headerOBJ = result.getJSONObject("header");
          Iterator<?> keys = headerOBJ.keys();

          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignLeftWidth1x);
          }
          while( keys.hasNext() ) {
              String key = (String)keys.next();
              mmOutputStream.write( (key + headerOBJ.getString(key) ).getBytes() );
              mmOutputStream.write( "\n".getBytes() );
          }

          mmOutputStream.write( "\n".getBytes() );
          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenterWidth2x);
          }
          mmOutputStream.write( (result.getString("title") + "\n").getBytes() );

          mmOutputStream.write( "\n".getBytes() );

          JSONObject headerOBJ2 = result.getJSONObject("values");
          Iterator<?> keys2 = headerOBJ2.keys();

          while( keys2.hasNext() ) {
              String key = (String)keys2.next();
              if( headerOBJ2.getString(key).equals("CENTER") ){
                  if( paperType.equals("80mm") ) {
                      mmOutputStream.write(alignCenterWidth1x);
                  }
                  mmOutputStream.write( key.getBytes() );
              }else{
                  if( paperType.equals("80mm") ) {
                      mmOutputStream.write(alignLeftWidth1x);
                  }
                  mmOutputStream.write( key.getBytes() );
                  mmOutputStream.write( headerOBJ2.getString(key).getBytes() );
                  mmOutputStream.write( "\n\n".getBytes() );
              }
          }

          JSONArray items = result.getJSONArray("items");
          for(int i=0; i<items.length(); i++) {
              JSONObject item = items.getJSONObject(i);
              Iterator<?> keys3 = item.keys();

              while( keys3.hasNext() ) {
                  String key = (String)keys3.next();
                  if( key.equals("title") ){
                      if( paperType.equals("80mm") ) {
                          mmOutputStream.write(alignLeftWidth1x);
                      }
                      mmOutputStream.write( item.getString(key).getBytes() );
                  }else{
                      if( paperType.equals("80mm") ) {
                          mmOutputStream.write(alignLeftWidth1x);
                      }
                      mmOutputStream.write( key.getBytes() );
                      mmOutputStream.write( item.getString(key).getBytes() );
                  }
                  mmOutputStream.write( "\n\n".getBytes() );
              }

          }

          mmOutputStream.write( "\n".getBytes() );
          if( paperType.equals("80mm") ) {
              mmOutputStream.write(alignCenter);
          }
          mmOutputStream.write( ( "Data da impressao\n").getBytes() );
          mmOutputStream.write( (currentDate + "\n").getBytes() );

          mmOutputStream.write( "\n\n\n".getBytes() );

          callbackContext.success("Data Sent");
          return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    //This will return the array list of paired bluetooth printers
    void listBT(CallbackContext callbackContext) {
        BluetoothAdapter mBluetoothAdapter = null;
        String errMsg = null;
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                errMsg = "No bluetooth adapter available";
                Log.e(LOG_TAG, errMsg);
                callbackContext.error(errMsg);
                return;
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                JSONArray json = new JSONArray();
                for (BluetoothDevice device : pairedDevices) {
                    /*
                     Hashtable map = new Hashtable();
                     map.put("type", device.getType());
                     map.put("address", device.getAddress());
                     map.put("name", device.getName());
                     JSONObject jObj = new JSONObject(map);
                     */
                    json.put(device.getName());
                }
                callbackContext.success(json);
            } else {
                callbackContext.error("No Bluetooth Device Found");
            }
            //Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
        } catch (Exception e) {
            errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
    }

    // This will find a bluetooth printer device
    boolean findBT(CallbackContext callbackContext, String name) {
        try {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null) {
                Log.e(LOG_TAG, "No bluetooth adapter available");
            }
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                this.cordova.getActivity().startActivityForResult(enableBluetooth, 0);
            }
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equalsIgnoreCase(name)) {
                        mmDevice = device;
                        return true;
                    }
                }
            }
            Log.d(LOG_TAG, "Bluetooth Device Found: " + mmDevice.getName());
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    // Tries to open a connection to the bluetooth printer device
    boolean connectBT(CallbackContext callbackContext) throws IOException {
        try {
            // Standard SerialPortService ID
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            mmSocket.connect();
            mmOutputStream = mmSocket.getOutputStream();
            mmInputStream = mmSocket.getInputStream();
            beginListenForData();
            //Log.d(LOG_TAG, "Bluetooth Opened: " + mmDevice.getName());
            callbackContext.success("Bluetooth Opened: " + mmDevice.getName());
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    // After opening a connection to bluetooth printer device,
    // we have to listen and check if a data were sent to be printed.
    void beginListenForData() {
        try {
            final Handler handler = new Handler();
            // This is the ASCII code for a newline character
            final byte delimiter = 10;
            stopWorker = false;
            readBufferPosition = 0;
            readBuffer = new byte[1024];
            workerThread = new Thread(new Runnable() {
                public void run() {
                    while (!Thread.currentThread().isInterrupted() && !stopWorker) {
                        try {
                            int bytesAvailable = mmInputStream.available();
                            if (bytesAvailable > 0) {
                                byte[] packetBytes = new byte[bytesAvailable];
                                mmInputStream.read(packetBytes);
                                for (int i = 0; i < bytesAvailable; i++) {
                                    byte b = packetBytes[i];
                                    if (b == delimiter) {
                                        byte[] encodedBytes = new byte[readBufferPosition];
                                        System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                        /*
                                         final String data = new String(encodedBytes, "US-ASCII");
                                         readBufferPosition = 0;
                                         handler.post(new Runnable() {
                                         public void run() {
                                         myLabel.setText(data);
                                         }
                                         });
                                         */
                                    } else {
                                        readBuffer[readBufferPosition++] = b;
                                    }
                                }
                            }
                        } catch (IOException ex) {
                            stopWorker = true;
                        }
                    }
                }
            });
            workerThread.start();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //This will send data to bluetooth printer
    boolean printText(CallbackContext callbackContext, String msg) throws IOException {
        try {
            mmOutputStream.write(msg.getBytes());
            // tell the user data were sent
            //Log.d(LOG_TAG, "Data Sent");
            callbackContext.success("Data Sent");
            return true;

        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    //This will send data to bluetooth printer
    boolean printImage(CallbackContext callbackContext, String msg) throws IOException {
        try {

            final String encodedString = msg;
            final String pureBase64Encoded = encodedString.substring(encodedString.indexOf(",") + 1);
            final byte[] decodedBytes = Base64.decode(pureBase64Encoded, Base64.DEFAULT);

            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);

            bitmap = decodedBitmap;
            int mWidth = bitmap.getWidth();
            int mHeight = bitmap.getHeight();

            bitmap = resizeImage(bitmap, 48 * 8, mHeight);

            byte[] bt = decodeBitmap(bitmap);

            mmOutputStream.write(bt);
            // tell the user data were sent
            //Log.d(LOG_TAG, "Data Sent");
            callbackContext.success("Data Sent");
            return true;

        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    boolean printPOSCommand(CallbackContext callbackContext, byte[] buffer) throws IOException {
        try {
            mmOutputStream.write(buffer);
            // tell the user data were sent
            Log.d(LOG_TAG, "Data Sent");
            callbackContext.success("Data Sent");
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    // disconnect bluetooth printer.
    boolean disconnectBT(CallbackContext callbackContext) throws IOException {
        try {
            stopWorker = true;
            mmOutputStream.close();
            mmInputStream.close();
            mmSocket.close();
            callbackContext.success("Bluetooth Disconnect");
            return true;
        } catch (Exception e) {
            String errMsg = e.getMessage();
            Log.e(LOG_TAG, errMsg);
            e.printStackTrace();
            callbackContext.error(errMsg);
        }
        return false;
    }

    //New implementation, change old
    public static byte[] hexStringToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    private static byte charToByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }

    //New implementation
    private static Bitmap resizeImage(Bitmap bitmap, int w, int h) {
        Bitmap BitmapOrg = bitmap;
        int width = BitmapOrg.getWidth();
        int height = BitmapOrg.getHeight();

        if (width > w) {
            float scaleWidth = ((float) w) / width;
            float scaleHeight = ((float) h) / height + 24;
            Matrix matrix = new Matrix();
            matrix.postScale(scaleWidth, scaleWidth);
            Bitmap resizedBitmap = Bitmap.createBitmap(BitmapOrg, 0, 0, width,
                    height, matrix, true);
            return resizedBitmap;
        } else {
            Bitmap resizedBitmap = Bitmap.createBitmap(w, height + 24, Config.RGB_565);
            Canvas canvas = new Canvas(resizedBitmap);
            Paint paint = new Paint();
            canvas.drawColor(Color.WHITE);
            canvas.drawBitmap(bitmap, (w - width) / 2, 0, paint);
            return resizedBitmap;
        }
    }

    private static String hexStr = "0123456789ABCDEF";

    private static String[] binaryArray = {"0000", "0001", "0010", "0011",
        "0100", "0101", "0110", "0111", "1000", "1001", "1010", "1011",
        "1100", "1101", "1110", "1111"};

    public static byte[] decodeBitmap(Bitmap bmp) {
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        List<String> list = new ArrayList<String>(); //binaryString list
        StringBuffer sb;
        int bitLen = bmpWidth / 8;
        int zeroCount = bmpWidth % 8;
        String zeroStr = "";
        if (zeroCount > 0) {
            bitLen = bmpWidth / 8 + 1;
            for (int i = 0; i < (8 - zeroCount); i++) {
                zeroStr = zeroStr + "0";
            }
        }

        for (int i = 0; i < bmpHeight; i++) {
            sb = new StringBuffer();
            for (int j = 0; j < bmpWidth; j++) {
                int color = bmp.getPixel(j, i);

                int r = (color >> 16) & 0xff;
                int g = (color >> 8) & 0xff;
                int b = color & 0xff;
                // if color close to white，bit='0', else bit='1'
                if (r > 160 && g > 160 && b > 160) {
                    sb.append("0");
                } else {
                    sb.append("1");
                }
            }
            if (zeroCount > 0) {
                sb.append(zeroStr);
            }
            list.add(sb.toString());
        }

        List<String> bmpHexList = binaryListToHexStringList(list);
        String commandHexString = "1D763000";
        String widthHexString = Integer.toHexString(bmpWidth % 8 == 0 ? bmpWidth / 8 : (bmpWidth / 8 + 1));
        if (widthHexString.length() > 2) {
            Log.d(LOG_TAG, "DECODEBITMAP ERROR : width is too large");
            return null;
        } else if (widthHexString.length() == 1) {
            widthHexString = "0" + widthHexString;
        }
        widthHexString = widthHexString + "00";

        String heightHexString = Integer.toHexString(bmpHeight);
        if (heightHexString.length() > 2) {
            Log.d(LOG_TAG, "DECODEBITMAP ERROR : height is too large");
            return null;
        } else if (heightHexString.length() == 1) {
            heightHexString = "0" + heightHexString;
        }
        heightHexString = heightHexString + "00";

        List<String> commandList = new ArrayList<String>();
        commandList.add(commandHexString + widthHexString + heightHexString);
        commandList.addAll(bmpHexList);

        return hexList2Byte(commandList);
    }

    public static List<String> binaryListToHexStringList(List<String> list) {
        List<String> hexList = new ArrayList<String>();
        for (String binaryStr : list) {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < binaryStr.length(); i += 8) {
                String str = binaryStr.substring(i, i + 8);

                String hexString = myBinaryStrToHexString(str);
                sb.append(hexString);
            }
            hexList.add(sb.toString());
        }
        return hexList;

    }

    public static String myBinaryStrToHexString(String binaryStr) {
        String hex = "";
        String f4 = binaryStr.substring(0, 4);
        String b4 = binaryStr.substring(4, 8);
        for (int i = 0; i < binaryArray.length; i++) {
            if (f4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }
        for (int i = 0; i < binaryArray.length; i++) {
            if (b4.equals(binaryArray[i])) {
                hex += hexStr.substring(i, i + 1);
            }
        }

        return hex;
    }

    public static byte[] hexList2Byte(List<String> list) {
        List<byte[]> commandList = new ArrayList<byte[]>();

        for (String hexStr : list) {
            commandList.add(hexStringToBytes(hexStr));
        }
        byte[] bytes = sysCopy(commandList);
        return bytes;
    }

    public static byte[] sysCopy(List<byte[]> srcArrays) {
        int len = 0;
        for (byte[] srcArray : srcArrays) {
            len += srcArray.length;
        }
        byte[] destArray = new byte[len];
        int destLen = 0;
        for (byte[] srcArray : srcArrays) {
            System.arraycopy(srcArray, 0, destArray, destLen, srcArray.length);
            destLen += srcArray.length;
        }
        return destArray;
    }

}
