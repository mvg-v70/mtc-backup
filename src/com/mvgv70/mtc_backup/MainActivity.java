package com.mvgv70.mtc_backup;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {
	
  private final static String TAG = "mtc-backup";
  private final static String GPS_SD = "/mnt/external_sd/";
  private final static String BACKUP_DIR = "mtc-backup";
  @SuppressLint("SdCardPath")
  private final static String RADIO_XML_PATH = "/data/data/com.microntek.radio/shared_prefs/";
  private final static String RADIO_XML_NAME = "com.microntek.radio_preferences.xml";
  @SuppressLint("SdCardPath")
  private final static String MUSIC_XML_PATH = "/data/data/com.microntek.music/shared_prefs/";
  private final static String MUSIC_XML_NAME = "com.microntek.music_preferences.xml";
  private int operButton = 0;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    // создать каталог на карте
    File file = new File(GPS_SD,BACKUP_DIR);
    if (!file.isDirectory())
    {
      if (file.mkdirs())
        Toast.makeText(this, getString(R.string.toast_dir_create_success), Toast.LENGTH_SHORT).show();
      else
        Toast.makeText(this, getString(R.string.toast_dir_create_failure), Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onClick(View v) 
  {
    buttonPressed(v.getId());
  }
  
  // всплывающее уведомление
  private void showToast(String msg)
  {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
  }
  
  // выполнение команды с привилегиями root
  private boolean executeCmd(String cmd)
  {
    Log.d(TAG,"> "+cmd);
    // su (as root)
    Process process = null;
  	DataOutputStream os = null;
    InputStream err = null;
  	boolean errflag = true;
  	try 
  	{
  	  process = Runtime.getRuntime().exec("su");
  	  os = new DataOutputStream(process.getOutputStream());
  	  err = process.getErrorStream();
  	  os.writeBytes(cmd+" \n");
      os.writeBytes("exit \n");
      os.flush();
      os.close();
      process.waitFor();
      // анализ ошибок
      byte[] buffer = new byte[1024];
      int len = err.read(buffer);
      if (len > 0)
      {
        String errmsg = new String(buffer,0,len);
        Log.e(TAG,errmsg);
      } 
      else
        errflag = false;
    } 
  	catch (IOException e) 
  	{
      Log.e(TAG,"IOException: "+e.getMessage());
    }
  	catch (InterruptedException e) 
  	{
      Log.e(TAG,"InterruptedException: "+e.getMessage());
    }
  	return (!errflag);
  }
  
  // копирование файла с привилегиями root
  private void copyFile(String source, String dest)
  {
    Log.d(TAG,"source="+source);
    Log.d(TAG,"dest="+dest);
    if (executeCmd("cp -f "+source+" "+dest))
      showToast(getString(R.string.toast_copy_success));
    else
      showToast(getString(R.string.toast_copy_failure));
  }
  
  // диалог подтверждения копирования настроек радио
  private void confirmDialogRadio(int buttonId, String text) 
  {
    operButton = buttonId;
  	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(text);
    // OK
    builder.setPositiveButton("OK", 
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.d(TAG,"radio, id="+id);
          switch (operButton) {
            case R.id.btnRadioSave:
              RadioSave();
              break;
            case R.id.btnRadioRestore:
              RadioRestore();
              break;
          }
        }
    });
    // show
    builder.setCancelable(true);
    builder.create();
    builder.show();
  }
  
  //диалог подтверждения копирования настроек музыки
  private void confirmDialogMusic(int buttonId, String text) 
  {
    operButton = buttonId;
    String[] owners = getResources().getStringArray(R.array.owners);
 	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(text);
    // items
    builder.setItems(owners,
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
           Log.d(TAG,"item_id="+id);
           switch (operButton) {
             case R.id.btnMusicSave:
               MusicSave(id);
               break;
             case R.id.btnMusicRestore:
               MusicRestore(id);
               break;
           }
         }
    });
    // show
    builder.setCancelable(true);
    builder.create();
    builder.show();
  }
  
  // нажатие кнопки
  private void buttonPressed(int id)
  {
    String text = "";
    // !!!
    Log.d(TAG,"btnRadioSave="+R.id.btnRadioSave);
    Log.d(TAG,"btnRadioRestore="+R.id.btnRadioRestore);
    Log.d(TAG,"btnMusicSave="+R.id.btnMusicSave);
    Log.d(TAG,"btnMusicRestore="+R.id.btnMusicRestore);
    //
    switch (id) {
      case R.id.btnRadioSave:
        text = getString(R.string.query_save_radio);
        confirmDialogRadio(id, text);
        break;
      case R.id.btnRadioRestore:
        text = getString(R.string.query_restore_radio);
        confirmDialogRadio(id, text);
        break;
      case R.id.btnMusicSave:
        text = getString(R.string.query_save_music);
        confirmDialogMusic(id, text);
        break;
      case R.id.btnMusicRestore:
        text = getString(R.string.query_restore_music);
        confirmDialogMusic(id, text);
        break;
	  }
  }
  
  // сохранение настроек радио
  private void RadioSave()
  {
    Log.d(TAG,"RadioSave()");
    copyFile(RADIO_XML_PATH+RADIO_XML_NAME, GPS_SD+BACKUP_DIR+"/"+RADIO_XML_NAME);
  }
  
  // восстановление настроек радио
  private void RadioRestore()
  {
    Log.d(TAG,"RadioRestore()");
    copyFile(GPS_SD+BACKUP_DIR+"/"+RADIO_XML_NAME, RADIO_XML_PATH+RADIO_XML_NAME);
  }
  
  // сохранение настроек mp3
  private void MusicSave(int id)
  {
    Log.d(TAG,"MusicSave()");
    copyFile(MUSIC_XML_PATH+MUSIC_XML_NAME, GPS_SD+BACKUP_DIR+"/"+id+"#"+MUSIC_XML_NAME);
  }
  
  // восстановление настроек mp3
  private void MusicRestore(int id)
  {
    Log.d(TAG,"MusicRestore()");
    copyFile(GPS_SD+BACKUP_DIR+"/"+id+"#"+MUSIC_XML_NAME, MUSIC_XML_PATH+MUSIC_XML_NAME);
  }
  
}
