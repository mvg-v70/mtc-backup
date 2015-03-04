package com.mvgv70.mtc_backup;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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
  private final static String RADIO_XML_PATH = "/data/data/com.microntek.radio/shared_prefs/";
  private final static String RADIO_XML_NAME = "com.microntek.radio_preferences.xml";
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
  
  // диалог подтверждкения копирования
  private void confirmDialog(int buttonId, String text) 
  {
    operButton = buttonId;
  	AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setTitle(text);
    builder.setPositiveButton("OK", 
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.d(TAG,"OK");
          switch (operButton) {
            case R.id.btnRadioSave:
              RadioSave();
              break;
            case R.id.btnRadioRestore:
              RadioRestore();
              break;
            case R.id.btnMusicSave:
              MusicSave();
              break;
            case R.id.btnMusicRestore:
              MusicRestore();
              break;
          }
        }
      });
    builder.setNegativeButton("Cancel", 
      new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
          Log.d(TAG,"Cancel");
          dialog.cancel();
        }
      });
    // show
    builder.create();
    builder.show();
  }
  
  // нажатие кнопки
  private void buttonPressed(int id)
  {
    String text = "";
    switch (id) {
      case R.id.btnRadioSave:
        text = getString(R.string.query_save_radio);
        break;
      case R.id.btnRadioRestore:
        text = getString(R.string.query_restore_music);
        break;
      case R.id.btnMusicSave:
        text = getString(R.string.query_save_music);
        break;
      case R.id.btnMusicRestore:
        text = getString(R.string.query_restore_music);
        break;
	  }
    confirmDialog(id, text);
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
  private void MusicSave()
  {
    Log.d(TAG,"MusicSave()");
    copyFile(MUSIC_XML_PATH+MUSIC_XML_NAME, GPS_SD+BACKUP_DIR+"/"+MUSIC_XML_NAME);
  }
  
  // восстановление настроек mp3
  private void MusicRestore()
  {
    Log.d(TAG,"MusicSave()");
    copyFile(GPS_SD+BACKUP_DIR+"/"+MUSIC_XML_NAME, MUSIC_XML_PATH+MUSIC_XML_NAME);
  }
}
