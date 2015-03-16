package com.example.shadowdraw;

import android.graphics.Bitmap;
import processing.core.PImage;

public class BitmapToPImage {
	PImage getPImage(Bitmap b){
		return new PImage(b);
	}
}
