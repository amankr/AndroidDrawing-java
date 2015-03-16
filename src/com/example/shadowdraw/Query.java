package com.example.shadowdraw;

import java.io.File;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import processing.core.PImage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.AsyncTask;
import android.util.Log;
import bice.Bice;

import minhash.Entry;
import minhash.Index;
import minhash.ResultSet;

public class Query {
	Index index;
	Generate gen;
	static int patchSize = 64*2;
    static int patchStride = 32*1;
    int [] id;
    private boolean resultReady,queryStarted;
    
    private queryBackground prev;
    
    private Context context;
    
	public Query(Context context_){
		gen = new Generate();
		index = gen.loadIndex();
		resultReady = false;
		queryStarted = false;
		context = context_;
		prev = null;
		id = new int[10];
		for(int i=0;i<8;++i){
			id[i] = findImageId(index.imageNames,"a"+i+".jpg");
		}
		
	}
	
	public void findAndSet(Bitmap b,Canvas c,Paint p){
		Log.e("AssyncQuerry ","1");
		

		if(queryStarted == true && prev != null){
			prev.cancel(true);
			prev = null;
		}
		
		resultReady = false;
		queryStarted = true;
		//Bitmap bx = BitmapFactory.decodeResource(context.getResources(),R.drawable.invert1);
		Joint temp1 = new Joint(b,c,p);
		//Joint temp2 = new Joint(bx,c,p);
		
		/*c.drawColor(Color.WHITE);
		Paint p1=new Paint();
		p.setColor(Color.BLACK);
		c.drawBitmap(bx, 100, 100,p1);
		*/
		
		
		queryBackground backQuery = new queryBackground();
		prev = backQuery;
		backQuery.execute(temp1);
		
	}
	
	
	
	int findImageId(List<String> imageNames, String name) {
		
	    int id = imageNames.indexOf(name); // if there's no prefix directory
	    if(id >= 0) return id;
	    for(int i = 0; i < imageNames.size(); i++) { // if maybe there's some extra stuff
	      if(name.equals(new File(imageNames.get(i)).getName())) return i;
	    }
	    return -1;
	}
	
	
	
	private class queryBackground extends AsyncTask<Joint,Void,Void>{

		@Override
		protected Void doInBackground(Joint... q) {
			
			// TODO Auto-generated method stub
			Log.e("AssyncQuerry ","2");
			
			PImage img = new BitmapToPImage().getPImage(q[0].b);
			img.loadPixels();
			
			Log.e("AssyncQuerry","again "+img.pixels.length);
				
			
			int x = find(img);
			
			if(! isCancelled()){
				/*
				Bitmap b;
				if(x==id1){
					b = BitmapFactory.decodeResource(context.getResources(),R.drawable.invert1);
				}else if(x==id2){
					b = BitmapFactory.decodeResource(context.getResources(),R.drawable.invert2);
				}
				else{
					b = BitmapFactory.decodeResource(context.getResources(),R.drawable.invert3);
				}
				*/
				//q[0].c.drawBitmap(b, 100, 100, p);
				
				
				Log.e("AssyncQuerry ","3");
				queryStarted = false;
				resultReady = true;
				
				/*
				q[0].c.drawColor(Color.WHITE);
				Paint p=new Paint();
				p.setColor(Color.BLACK);
				q[0].c.drawBitmap(b, 100, 100,p);
				q[0].c.drawBitmap(q[0].b, 0, 0, q[0].p);
				*/
				
				Log.e("AssyncQuerry ","Result Done");
			}
			return null;
		}
		
		/*
		@Override
		protected void onPostExecute(Joint result){
			
		}
		*/
		
		@Override
		protected void onCancelled(){
			Log.e("AssyncQuerry ","Cancelled");
		}
		private int find(PImage img){
			// For now , just checking for 3 images we have
			Bice bice = Bice.shadowdrawVersion();
		    bice.setImage(img);
		    List<BitSet> descriptors = new ArrayList<BitSet>();
		    for(int y = 0; y < img.height-patchSize; y += patchStride) {
		      for(int x = 0; x < img.width-patchSize; x += patchStride) {
		    	if(isCancelled()) return -1;
		        BitSet desc = bice.calc(x,y,patchSize);
		        if(desc != null) { descriptors.add(desc); }
		      }
		    }
		    
		    Integer [] c = new Integer[]{0,0,0,0,0,0,0,0,0,0};
		    
		    
		    for(BitSet desc : descriptors) {
		    	ResultSet found = index.findEntry(desc);
		    	
		    	for(List<Entry> es : found.getResults()) {
		    		for(Entry e : es) {
		    			if(isCancelled()) return -1;
		    			
		    			c[e.imgId]++;
		    			/*
		    			if(e.imgId == id1)
		    				++c1;
		    			else if(e.imgId == id2)
		    				++c2;
		    			else if(e.imgId == id3)
		    				++c3;
		    			*/	
		    		}
		    	}	
		    }
		    resultReady = true;
		    int maximum=0;
		    String log="";
		    for(int i=0;i<10;++i){
		    	log += " "+i+":"+c[i];
		    	maximum = Math.max(maximum,c[i]);
		    }
		    Log.e("AssyncQuerry",log);
		    return maximum;
		    //Log.e("AssyncQuerry",c1.toString()+" "+c2.toString()+" "+c3.toString());
		    //return Math.max(c1,Math.max(c2,c3));
		}
	}
	
	private class Joint{
		public Bitmap b;
		public Canvas c;
		public Paint p;
		public Joint(Bitmap _b,Canvas _c,Paint _p){
			b = _b;
			c = _c;
			p = _p;
		}
	}

}
