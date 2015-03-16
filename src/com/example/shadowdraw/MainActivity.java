package com.example.shadowdraw;

import com.example.shadowdraw.DrawingView;
import com.example.shadowdraw.R;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;





public class MainActivity extends Activity {

	private DrawingView drawView;
	private float smallBrush, mediumBrush, largeBrush;
	private long start,end;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        
       //   Generate gen = new Generate();
       //   gen.generate();
         	
         
       
       
        drawView = (DrawingView)findViewById(R.id.drawing);
        
        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);
        
        
        
        drawView.setBrushSize(smallBrush);
        drawView.setLastBrushSize(smallBrush);
        
        
        //drawView.startNew();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
