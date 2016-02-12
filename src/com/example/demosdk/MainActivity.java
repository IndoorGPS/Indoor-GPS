package com.example.demosdk;

import java.util.ArrayList;
import java.util.List;

import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.widget.ImageView;
import android.widget.Toast;


import com.ladiesman217.engine.EngineDetect; 
import com.ladiesman217.engine.EngineCommon;
import com.ladiesman217.engine.EngineCommon.*;

public class MainActivity extends Activity {

	private WifiManager wifiManager = null;
	private List<WifiAP> wifiAPList = new ArrayList<WifiAP>();	 
    private IntentFilter intentfilter=new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
	

    private PointLm current_position = new PointLm(-1,1);
    private ImageView imgview;
    private Bitmap map;
    private int ratio = 1;
    private int floorplan = 0;
    private int qstate = 0;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);	
		imgview = (ImageView) findViewById(R.id.imageview);
    	//====================================================================================================
		// Please download "Indoor GPS Premium" from Google Play to export your WiFi Map which is needed by 
		// Detection SDK
		// [detailed information]
		//  https://sites.google.com/site/ladiesman217indoorgps/sdk/how-to-use
    	//====================================================================================================
		/* select the path you place the exported WiFi Map folder "Ladiesman217" */
		// Ex. 
		// if the created folder is in "/storage/emulated/0/Ladiesman217/",
		// then the input folder string should be "/storage/emulated/0"
        String dir = Environment.getExternalStorageDirectory().getAbsolutePath();
        EngineCommon.Initialize(this, dir);	
    	//====================================================================================================
		InitMap();
	}
	
    @Override
    protected void onDestroy() {
        super.onDestroy();
    	DeinitMap();
    }
    
    @Override
    protected void onResume() {
    	super.onResume();  
    	InitWifi();
    }
    
    @Override
    protected void onPause() {
    	super.onPause(); 
    	DeinitWifi();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}	

	void InitWifi()
	{
        wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
		if (!wifiManager.isWifiEnabled()) {
    		wifiManager.setWifiEnabled(true);    			
		}
		EnableWifi(); 
	}
	
	void DeinitWifi()
	{
		DisableWifi();
		EngineCommon.ResetParams();		
	}
    
    void EnableWifi() {
		registerReceiver(mReceiver, intentfilter);
		wifiManager.startScan(); 
    }
    
    void DisableWifi() {
		unregisterReceiver(mReceiver);
    }
    
    void ScanWifi() {
		wifiManager.startScan(); 
    }
	
	void InitMap()
	{
		// get screen size
		DisplayMetrics dm = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(dm);
		int widthScreen = dm.widthPixels;
		int heightScreen = dm.heightPixels;			

		// load image
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;
		Bitmap bmp = null;
		bmp = BitmapFactory.decodeFile(EngineCommon.GetPhotoPath(floorplan), options);
		int widthMap = bmp.getWidth();
		int heightMap = bmp.getHeight();
		
		
		// load down sampling image
		int ratioW = widthScreen/widthMap;
		int ratioH = heightScreen/heightMap;
		ratio = ratioW>ratioH ? ratioW : ratioH;
		if (ratio ==0) {
			ratio = 1;
		}
		options = new BitmapFactory.Options();
		options.inScaled = false;
		options.inSampleSize = ratio;
		map = BitmapFactory.decodeFile(EngineCommon.GetPhotoPath(floorplan), options);
	    imgview.setImageBitmap(map);
	}
	
	void DeinitMap()
	{
        imgview = null;   		
	}
	
	void DrawPosition(int x, int y)
	{
	    Paint paint = new Paint();
	    paint.setAntiAlias(true);
		if (floorplan==-1) {
			// not in WiFi Map area
		    paint.setColor(Color.GRAY);	
			
		} else {
			// in WiFi Map area
		    paint.setColor(Color.BLUE);			
		}

		Bitmap mutable = map.copy(Bitmap.Config.ARGB_8888, true);
	    Canvas canvas = new Canvas(mutable);
	    canvas.drawCircle(x, y, 15, paint);
	    imgview.setImageBitmap(mutable);
	}
    //---------------------------------------------------------------------------------------------------------------------
    private BroadcastReceiver mReceiver=new BroadcastReceiver(){
    	public void onReceive(Context context, Intent intent) {
    	if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {

    		wifiAPList.clear();  
	    	for(final ScanResult result:wifiManager.getScanResults()) {	    		
	    		WifiAP wifi = new WifiAP(result.BSSID,result.SSID,result.level);
	    		wifiAPList.add(wifi);
	    	}

	    	//=============================================
			DetectParam param = new DetectParam();
			param.wifiAPList = wifiAPList;
			param.wifiPtrLm = current_position;
			param.qstate = 0;
			// Note: currently, floorplan is always "0". This parameter is prepared for future multiple-floorplan case.
			floorplan = EngineDetect.DetectPosition(param);
			qstate = param.qstate;
			
			if (qstate == 100) {
				/* SDK will output position based on original photo size */
				/* since we down sample original photo size by value "ratio, the distance should also be down shrinked  */
				DrawPosition(current_position.x/ratio, current_position.y/ratio);	 
				
				Log.e("XXX","x="+current_position.x+",y="+current_position.y);		
			}
			else {
				String msg = String.format("Loading ... (%%%d)", qstate);
    			Toast.makeText(MainActivity.this, msg,Toast.LENGTH_SHORT).show();	
			}
	    	//=============================================
			ScanWifi();	
    	}
    }};

}
