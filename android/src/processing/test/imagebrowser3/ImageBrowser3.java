package processing.test.imagebrowser3;

import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.io.*; 
import java.util.*; 
import android.util.DisplayMetrics; 
import apwidgets.*; 
import android.text.InputType; 
import android.content.Context; 
import android.view.inputmethod.EditorInfo; 
import android.view.inputmethod.InputMethodManager; 
import android.content.res.AssetManager; 
import ketai.ui.*; 
import android.view.MotionEvent; 
import java.util.Arrays; 
import java.io.*; 
import android.os.Environment; 
import android.content.res.AssetManager; 
import android.media.*; 
import android.content.res.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class ImageBrowser3 extends PApplet {

/**
 * @file ImageBrowser3.java
 * @author Sisi Wei, 915565877
 * @date 20 Mar 15
 * @description Simple image browser, extended to include animations,audio and gesture.
 */
















float density = 0f;
int width = 0;
int height = 0;

int thumbWidth = 310, thumbHeight = 230;
int selectedIndex = 0;
float transitBy = 0f, baseTransRate = 25f/1000f;
int mode = 0;

float scale;
int zoomWidth;
int zoomHeight;

float translateX=0, translateY=0;
float prevPinchX = -1, prevPinchY = -1;
int prevPinchFrame = -1;

ImageList imageList;
ArrayList<String> tagList;
String defaultTag;

AssetManager am;
KetaiGesture gesture;

// APWidgets 
APWidgetContainer widgetContainer, widgetContainerTmp;
APMediaPlayer player;
APEditText tagInput;
APButton saveButton;
APButton cancelButton;
APButton nextButton;
APButton prevButton;
APButton zoomButton;


public void setup() {
    // get the density of the mobile phone
    // Might be used in scaling the images
    DisplayMetrics displaymetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
    density = displaymetrics.density;

    width = displayWidth;
    height = displayHeight;

   
    orientation(LANDSCAPE);
    frameRate(25);
    
    gesture = new KetaiGesture(this);
    scale = 1.0f;
    zoomWidth = 4*width/5;
    zoomHeight = 4*height/5;

    widgetContainer = new APWidgetContainer( this );
    widgetContainerTmp = new APWidgetContainer( this );

    textSize(height/8);
    //create a textfield from x- and y-pos., width and height
    tagInput = new APEditText(2*width/3, 0, width/3, height/8+10); 
    tagInput.setImeOptions(EditorInfo.IME_ACTION_DONE); //Enables a Done button
    tagInput.setCloseImeOnDone(true);
    
    //create new button from x- and y-pos. and label. size determined by text content
    saveButton = new APButton(5*width/6, height/8, "Save"); 
    cancelButton = new APButton(5*width/6, height/4, "Cancel"); 
    zoomButton = new APButton(5*width/6, 3*height/8, "Zoom"); 
    prevButton = new APButton(50, height-150, "<--"); 
    nextButton = new APButton(width-250, height-150, "-->"); 

    //place widgets in the container
    widgetContainer.addWidget(tagInput); 
    widgetContainer.addWidget(saveButton); 
    widgetContainer.addWidget(cancelButton); 
    widgetContainer.addWidget(zoomButton);
    widgetContainerTmp.addWidget(nextButton); 
    widgetContainerTmp.addWidget(prevButton);

    widgetContainer.hide();    // Hide the widgetContainer in Mode 0

    player = new APMediaPlayer(this); // create new APMediaPlayer
    player.setVolume(1.0f, 1.0f); //Set left and right volumes. Range is from 0.0 to 1.0;
        
    imageList = new ImageList("img", thumbWidth, thumbHeight); // initialize the imageList

}

/** 
 * To show the virtual keyboard on the android device
 */
public void showVirtualKeyboard() {
    InputMethodManager imm = (InputMethodManager)   
    getSystemService( Context.INPUT_METHOD_SERVICE );

    imm.showSoftInput( tagInput.getView(), InputMethodManager.SHOW_IMPLICIT );
}

/* 
 * To hide the virtual keyboard on the android device
 */
public void hideVirtualKeyboard() {
  InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
  imm.hideSoftInputFromWindow(tagInput.getView().getWindowToken(), 0);
}

/** 
 * onClickWidget is called when a widget is clicked/touched
 */
public void onClickWidget(APWidget widget){
    if(widget == tagInput){
        println("Enter tags please");
    }else if(widget == saveButton){ 
        saveButton(); // To save the tags
    }else if(widget == cancelButton){ 
        cancelButton(); // Hide the virtual keyboard
    }else if(widget == zoomButton){ 
        zoomButton(); // go to zoom mode
    }else if(widget == nextButton){ 
    	if(mode == 0){
    		playSound("pacman.mp3");
    	}else{
    		playSound("Type Single Key (44100 Hz).mp3");
    	}
    	nextButton(); // scroll image to right
    }else if(widget == prevButton){ 
        if(mode == 0){
        	playSound("pacman.mp3");
    	}else{
    		playSound("Type Single Key (44100 Hz).mp3");
    	}
    	prevButton(); // scroll image to left
    }
  
}

/**
 * UI Call back, listen to the double tap gesture
 * @param x  the x position of tap
 * @param y  the y position of tap
 */
public void onDoubleTap(float x, float y){
  if(mode == 0) {
        int dimen[];
        int imgCenterX;
        int imgCenterY = height / 2;

        //check each image on image bar
        short i, j = -2;
        do {
            dimen = imageList.getScaling(j);
            imgCenterX = width / 2 + (thumbWidth + 20) * j;

            if(
                mouseX < imgCenterX + dimen[0]/2
                && mouseX > imgCenterX - dimen[0]/2
                && mouseY < imgCenterY + dimen[1]
                && mouseY > imgCenterY - dimen[1]
            ) {
                println("Go to Mode 1");
                playSound("whistleup.mp3");

                i = 0;

                if(j < 0) {
                    do {
                        imageList.prev();

                        i--;
                    } while(i > j);
                }

                else if(j > 0) {
                    do {
                        imageList.next();

                        i++;
                    } while(i < j);
                }

                mode = 1;
                selectedIndex = 0;
                getTags();

                break;
            }

            j++;
        } while(j < 3);
    }

    else if(mode == 1){
    	int dimen[];
        int imgCenterX =  width / 2;
        int imgCenterY = height / 2;

        dimen = imageList.getScaling(selectedIndex, 4*width/5, 4*height/5);

        if(
            mouseX < imgCenterX + dimen[0]/2
            && mouseX > imgCenterX - dimen[0]/2
            && mouseY < imgCenterY + dimen[1]
            && mouseY > imgCenterY - dimen[1]
        ) {
            println("Go back to Mode 0");
            playSound("whistleup.mp3");
		    goBack(0);
		}
    }

}

/**
 * UI Call back, listen to the pinch gesture
 * @param x  the x of center
 * @param y  the y of center
 * @param d  the direction change
 */
public void onPinch(float x, float y, float d)
{
	if(mode == 2){

        float tmpZoomWidth = constrain(zoomWidth+d, width/8, 3*width/2);

        scale = tmpZoomWidth/zoomWidth;

		if (prevPinchX >= 0 && prevPinchY >= 0
		&& (frameCount - prevPinchFrame < 10)) {
			translateX += (x - prevPinchX);
			translateY += (y - prevPinchY);
		}
		prevPinchX = x;
		prevPinchY = y;
		prevPinchFrame = frameCount;
        println("Pinch " + x + " " + y + " " + d);

        zoomWidth = (int)(scale*zoomWidth);
        zoomHeight = (int)(scale*zoomHeight);
	}
}

/**
 * UI Call back, The coordinates of the start of the gesture, 
 *        end of gesture and velocity in pixels/sec.
 * @param x  the x position of the start gesture
 * @param y  the y position of the start gesture
 * @param px the x position of the end gesture
 * @param py the y position of the end gesture
 * @param v  the velocity
 */
public void onFlick( float x, float y, float px, float py, float v){
    if(mode == 0 || mode == 1){
        float deltaX = px-x;
        float deltaY = py-y;

        if(deltaX >0 && abs(deltaY)<height/2 && abs(deltaX)>width/5){
	        println("Swipe to left");
	        prevButton();

        }else if(deltaX <0 && abs(deltaY)<height/2 && abs(deltaX)>width/5){
	        println("Swipe to right");
	        nextButton();
        }
    }

}
/**
 * UI callback. Saves the tags for the current image to file when the save
 *   button is clicked.
 */
public void saveButton() {
	if( mode == 1){
		tagInput(tagInput.getText());
		imageList.saveTags(tagList);  // save the tags into tagList
		tagInput.setText("");
	}else{
		println("Not in the mode 1");
	}
}

/**
 * UI callback. Hide the virtual keyboard when the cancel
 *   button is clicked.
 */
public void cancelButton() {
    if(mode == 1){
        hideVirtualKeyboard(); //hide the virtual keyboard
    }else if(mode == 2){  
        goBack(1); // Go back to Mode 1
    }
}

/**
 * UI callback. Change to mode 2 when the zoom
 *   button is clicked.
 */
public void zoomButton() {
    zoomWidth = 4*width/5;
    zoomHeight = 4*height/5;

    translateX = 0f;
    translateY = 0f;
        
  if(mode == 1){
      println("Go to Mode 2");
      zoomWidth = 4*width/5;
      zoomHeight = 4*height/5;
      mode = 2; // change to mode 2
  }else{
      println("Already in Zoom mode");
  }

}

/**
 * UI callback. Move to the next image when the next
 *   button is clicked.
 */
public void nextButton(){
    if(transitBy == 0f) {
        if(mode == 0){
        	if(selectedIndex<2)
        		selectedIndex++;
        	imageList.next5();
            transitBy = 1f;
        }else{
        	imageList.next();
            transitBy = 1f;

            if(mode == 1) {
                getTags();
            }
        }
    }
}

/**
 * UI callback. Move to the previous image when the prev
 *   button is clicked.
 */
public void prevButton(){
    if(transitBy  == 0f) {
        if(mode == 0){
        	if(selectedIndex>-2)
        		selectedIndex--;
        	imageList.prev5();
            transitBy = -1f;
        }

        else{
        	imageList.prev();
            transitBy = -1f;

            if(mode == 1) {
                getTags();
            }
        }
    }
}

/**
 * UI callback. Go back to Mode 0 when the cancel
 *   button is clicked.
 */
public void goBack(int i){
    println("Go Back to Mode " + i);
    if(i==0){
        playSound("whistledown.mp3");
        transitBy = 0f;
        mode = 0;
    }else if(i==1){
		playSound("whistledown.mp3");
        mode = 1;
    }
}

/**
 * UI callback. Adds the text field input to the current image tag list.
 * @param input the tag to add
 */
public void tagInput(String input) {
    if(!input.trim().isEmpty()) {
        if(tagList.isEmpty()) {
            defaultTag = "";
        }
        
        tagList.add(input);
        defaultTag += input += ",";
    }
}

/**
 * Retrieves the tags for the current image and displays them below the image.
 */
public void getTags() {
    tagList = imageList.getTags();

    if(!tagList.isEmpty()) {
        defaultTag = "";

        for(String tag : tagList) {
            defaultTag += tag += ",";
        }

    }else {
        defaultTag = "No tags";
    }
}

public void draw() {
    background(50);

    if(imageList.size() > 0) {
        if(mode == 0) {
            widgetContainer.hide();
            widgetContainerTmp.show();
            mode0();
        }
        else if(mode == 1) {
            mode1();
            widgetContainer.show();
            widgetContainerTmp.show();
            showVirtualKeyboard();
        }
        else if(mode == 2){
        	widgetContainer.show();
            widgetContainerTmp.hide();
            mode2();
        }
    }

    else {
        textSize(32);
        textAlign(CENTER);
        text("No images to display!", width / 2, height / 2);
    }
}


/**
 * Mode 0: Updates thumbnail positions and animations.
 */
public void mode0() {
    short i;
    int[] dimens;

    //scroll thumbnails left
    if(transitBy < 0) {
        i = -2;
        do {
            dimens = imageList.getScaling(i);

            displayImage(
                imageList.getImg(i),
                (int)(width / 2 + (thumbWidth + 20) * (i + transitBy)),
                height / 2,
                dimens
            );

            i++;
        } while(i < 3 - Math.floor(transitBy));

        transitBy += 10f * baseTransRate;

        if(transitBy >= 0f) {
            transitBy = 0f;
        }
    }

    //scroll thumbnails right
    else if(transitBy > 0) {
        i = (short)(-2 - Math.ceil(transitBy));
        do {
            dimens = imageList.getScaling(i);

            displayImage(
                imageList.getImg(i),
                (int)(width / 2 + (thumbWidth + 20) * (i + transitBy)),
                height / 2,
                dimens
            );

            i++;
        } while(i < 3);

        transitBy -= 10f * baseTransRate;

        if(transitBy <= 0f) {
            transitBy = 0f;
        }
    }

    else {
        i = -2;
        do {
            dimens = imageList.getScaling(i);

            displayImage(
                imageList.getImg(i),
                width / 2 + (thumbWidth + 20) * i,
                height / 2,
                dimens
            );

            i++;
        } while(i < 3);
    }
}

/**
 * Mode 1: Updates and displays the large images.
 */
public void mode1() {
    short i;
    background(50);

    //scroll image to one on the left
    if(transitBy < 0) {
        i = 0;
        do {
            displayImage(
                imageList.getImg(selectedIndex + i),
                (int)(width * (i + transitBy) + width / 2),
                height / 2,
                imageList.getScaling(selectedIndex + i, 4*width/5, 4*height/5)
            );
            i++;
        } while(i < 2);

        transitBy += 3f * baseTransRate;
        if(transitBy >= 0f) {
            transitBy = 0f;
        }
    }

    //scroll image to one on the right
    else if(transitBy > 0) {
        i = -1;
        do {
            displayImage(
                imageList.getImg(selectedIndex + i),
                (int)(width * (i + transitBy) + width / 2),
                height / 2,
                imageList.getScaling(selectedIndex + i, 4*width/5, 4*height/5)
            );
            i++;
        } while(i < 1);

        transitBy -= 3f * baseTransRate;
        if(transitBy <= 0f) {
            transitBy = 0f;
        }
    }

    else {
        displayImage(
            imageList.getImg(selectedIndex),
            width / 2,
            height / 2,
            imageList.getScaling(selectedIndex, 4*width/5, 4*height/5)
        );

        fill(0xFFFF0099);
        textSize(height/15);
        textAlign(LEFT, BOTTOM);
        text(defaultTag, width/15, height/12);
    }
}

/**
 * Mode 2 :Updates and displays the zooming images.
 */
public void mode2() {
    background(50);
    displayImage(
      imageList.getImg(selectedIndex),
      (int)(width / 2 + translateX),
      (int)(height / 2 + translateY),
      imageList.getScaling(selectedIndex, zoomWidth, zoomHeight)
    );
}

/**
 * Rewind and play the sound.
 * @param sound the sound name of the soundclip.
 */
public void playSound(String sound){
    player.seekTo(0); //"rewind"
    player.setMediaFile("sound/" + sound);
    player.start();
    player.setLooping(false);
}

/**
 * Draws an image on the screen with a colored border.
 * @param photo the PImage to display
 * @param centerX the x-coordinate of the image center
 * @param centerY the y-coordinate of the image center
 * @param dimens an array containing the xy coordinates of the top left and
 *   bottom right corners
 */
public void displayImage (PImage photo, int centerX, int centerY, int[] dimens) {
    //draw scaled image
    imageMode(CENTER);
    image(photo, centerX, centerY, dimens[0], dimens[1]);
}

/** 
 * To release the MediaPlayer when the app closes
 */
public void onDestroy() {
  super.onDestroy(); //call onDestroy on super class
  if(player!=null) { //must be checked because or else crash when return from landscape mode
    player.release(); //release the player
  }
}

//these still work if we forward MotionEvents below
public void mouseDragged(){

}
public void mousePressed(){

}
public void mouseReleased() {

}

public boolean surfaceTouchEvent(MotionEvent event) {
  //call to keep mouseX, mouseY, etc updated
  super.surfaceTouchEvent(event);
  //forward event to class for processing
  return gesture.surfaceTouchEvent(event);
}
/**
 * @file ImageList.java
 * @author Sisi Wei, 915565877
 * @date 20 Mar 15
 * @description A ImageList class for the ImageBrowser3. Features including load images,
 *              read and save tag files on Android.
 *              This class is modified on a ImageList from Github. Here is the link:
 *              https://github.com/darthyoshi/Processing/blob/master/P0/src/p0/P0.java
 */









class ImageList {
	private AssetManager am;
	private PImage img;

    private ArrayList<PImage> photos;
    private String [] fileNames;
    private File sketchDir;

    private String srcDir;
    private ListIterator<PImage> cursor;
    private ArrayList<int[]> dimens;
    private ArrayList<ArrayList<String>> allTags;
    private ArrayList<String> curTags;

    /**
     * Retrieves the number of images in the PhotoList.
     * @return the number of images
     */
    public int size() {
        return photos.size();
    }

    /**
     * Class constructor. Image scaling is applied.
     * @param imgDir the name of the directory containing the image files
     * @param scaleWidth the maximum scaled image width
     * @param scaleHeight the maximum scaled image height
     */
    ImageList(String imgDir, int scaleWidth, int scaleHeight) {
        int i;
        photos = new ArrayList<PImage>();
        allTags = new ArrayList<ArrayList<String>>();
        fileNames = new String[2];

        srcDir = imgDir;
		am = getAssets();
        sketchDir = getFilesDir();
        
        // read image files in the data/srcDir
		try {
		    fileNames = am.list(srcDir);
		    System.out.println("Numbers of Images in data/" + srcDir + " : " + fileNames.length);
		  }
		  catch (IOException e) {
		    System.out.println("Error: folder Images not found");
		    return;
		}

        // load image and put image into arraylist
		if (fileNames.length >= 1 ) {

			for (i=0; i<fileNames.length; i++) {

				curTags = new ArrayList<String>();
	            allTags.add(curTags);

				if(fileNames[i].endsWith(".png") || fileNames[i].endsWith(".jpg")){
					img = loadImage(srcDir + '/' + fileNames[i]);
					img.loadPixels();
				    photos.add(img);

                    readTags(i,curTags); // read tags 
				}
		    }

		    curTags.trimToSize();
		    println("Images are loaded into photos.");
		}

		allTags.trimToSize();
        photos.trimToSize();
        cursor = photos.listIterator();
        scaleImages(scaleWidth, scaleHeight);
    }

    /**
     * Adds a PImage to the PhotoList.
     * @param image the PImage to add
     */
    public void add(PImage image, int[] corner) {
        cursor.add(image);
    }

    /**
     * Retrieves the scaled dimensions of an image.
     * @param offset the relative position from the current cursor location
     * @return an integer array containing the scaled dimensions of the
     *   image at the offset position
     * @return null if the PhotoList is empty
     */
    public int[] getScaling(int offset) {
        int[] result = null;

        if(photos.size() > 0) {
            result = dimens.get(calcIndex(offset));
        }

        return result;
    }

    /**
     * Calculates the scaled dimensions of an image.
     * @param offset the relative position from the current cursor location
     * @param maxWidth the maximum scaled width
     * @param maxHeight the maximum scaled height
     * @return an integer array containing the scaled dimensions of the
     *   image at the offset position
     * @return null if the PhotoList is empty
     */
    public int[] getScaling(int offset, int maxWidth, int maxHeight) {
        int[] result = null;

        if(photos.size() > 0) {
            result = scale(
                photos.get(calcIndex(offset)),
                maxWidth, maxHeight
            );
        }

        return result;
    }

    /**
     * Calculates the scaled dimensions of an image.
     * @param img the image to scale
     * @param targetWidth the target scaled width
     * @param targetHeight the target scaled height
     * @return an array containing the scaled width and height of the image
     */
    private int[] scale(PImage img, int targetWidth, int targetHeight) {
        int[] result = new int[2];
        img.loadPixels();
        img.loadPixels(); // just in case
        float aspect = 1f * img.width / img.height;

        result[0] = targetWidth;
        result[1] = (int)(targetWidth / aspect);

        //image is too tall
        if(result[1] > targetHeight/* || img.height > maxHeight*/) {
            result[1] = targetHeight;
            result[0] = (int)(result[1] * aspect);
        }

        return result;
    }

    /**
     * Determines the scaled dimensions for each image.
     * @param maxWidth the maximum scaled width
     * @param maxHeight the maximum scaled height
     */
    public void scaleImages(int maxWidth, int maxHeight) {
        ListIterator<PImage> iter = photos.listIterator();

        dimens = new ArrayList<int[]>();

        while(iter.hasNext()) {
            dimens.add(scale(iter.next(), maxWidth, maxHeight));
        }

        dimens.trimToSize();
    }

    /**
     * Retrieves the next PImage and increments the cursor position by one.
     *   If the current cursor position is at the end of the list, the
     *   cursor will be moved to the head of the list.
     * @return the next PImage
     * @return null if the PhotoList is empty
     */
    public PImage next() {
        PImage result = null;

        if(photos.size() > 0) {
            if(!cursor.hasNext()) {
                cursor = photos.listIterator();
            }

            result = cursor.next();
        }

        return result;
    }

    /**
     * Retrieves the next 5 PImages and increments the cursor position by five.
     *   If the current cursor position is at the end of the list, the
     *   cursor will be moved to the head of the list.
     * @return the next 5 PImage
     * @return null if the PhotoList is empty
     */
    public PImage next5() {
        PImage result = null;
        int count = 0;

        if(photos.size() > 0) {

            while(count < 4){

                if(!cursor.hasNext()) {
                    cursor = photos.listIterator();
                }

                cursor.next();
                count++;
            }
            result = cursor.next();
        }
        return result;
    }

    /**
     * Retrieves the PImage in the given position relative to the current
     *   cursor position.
     * @param offset the relative position of the PImage
     * @return the selected PImage
     * @return null if the PhotoList is empty
     */
    public PImage getImg(int offset) {
        PImage result = null;

        if(photos.size() > 0) {
            result = photos.get(calcIndex(offset));
        }

        return result;
    }

    /**
     * Retrieves the previous PImage and decrements the cursor position by
     *   one. If the current cursor position is at the head of the list, the
     *   cursor will be moved to the end of the list.
     * @return the previous PImage
     * @return null if the PhotoList is empty
     */
    public PImage prev() {
        PImage result = null;

        if(photos.size() > 0) {
            if(!cursor.hasPrevious()) {
                cursor = photos.listIterator(photos.size());
            }

            result = cursor.previous();
        }

        return result;
    }

     /**
     * Retrieves the previous 5 PImage and decrements the cursor position by
     *   five. If the current cursor position is at the head of the list, the
     *   cursor will be moved to the end of the list.
     * @return the previous PImage
     * @return null if the PhotoList is empty
     */
    public PImage prev5() {
        PImage result = null;
        int count = 0;

        if(photos.size() > 0) {

            while(count < 4){
                if(!cursor.hasPrevious()) {
                    cursor = photos.listIterator(photos.size());
                }
                cursor.previous();
                count++;
            }
            result = cursor.previous();
        }
        return result;
    }

    /**
     * Calculates the index of an image in the PhotoList.
     * @param offset the position of the PImage in relation to the current
     *   cursor position
     * @return the retrieved index
     * @return -1 if the PhotoList is empty
     */
    private int calcIndex(int offset) {
        int index = -1;

        if(photos.size() == 1) {
            index = 0;
        }

        else if(photos.size() > 1) {
            if(offset < 0) {
                index = offset + cursor.previousIndex() + 1;

                if(index < 0) {
                    index += photos.size();
                }
            }

            else {
                index = (offset + cursor.nextIndex()) % photos.size();
            }
        }

        return index;
    }

    /**
     * Returns the tags for the current image.
     * @return an ArrayList of Strings
     */
    public ArrayList<String> getTags() {
        int index = calcIndex(0);

        return (index >= 0 ? allTags.get(index) : null);
    }

    /**
     * Reads the tags for an image from a File.
     * @param file the File from which to read the tags
     * @param tagList the ArrayList in which to store the read tags
     */
    private void readTags(int index, ArrayList<String> tagList) {
        
        println("Start to read tags for image[" + index + "]");

        File sketchDir = getFilesDir();
        try {
            FileReader input = new FileReader(
                        sketchDir.getAbsolutePath() 
                        + "/"
                        + fileNames[index]
                        + "-tags.txt"
                        );
            BufferedReader bInput = new BufferedReader(input);
            String ns = bInput.readLine();
            while(ns != null) {
                tagList.add(ns);
                ns = bInput.readLine();
            }

            println("current tags:");
            for (int i=0; i<tagList.size(); i++) {
                println(tagList.get(i));
            }

            println("read tags for image[" + index + "] is over");

        }catch (Exception e) {
            println(e.getMessage());
        }
    }

    /**
     * Saves the tags for the current image to file.
     * @param tags the tags to save
     * @return true is successful
     */
    public boolean saveTags(ArrayList<String> tags) {
        int index = calcIndex(0);
        boolean result = false;

        if(index >= 0) {
            File sketchDir = getFilesDir();
            java.io.File outFile;

            try {
                outFile =new java.io.File(
                        sketchDir.getAbsolutePath()
                        + "/"
                        + fileNames[index]
                        + "-tags.txt"
                );

                if (!outFile.exists()){
                    outFile.createNewFile();
                }

                FileWriter outWriter = new FileWriter(
                                    sketchDir.getAbsolutePath()
                                    + "/"
                                    + fileNames[index]
                                    + "-tags.txt"
                                );

                for(int i=0; i<tags.size(); i++){
                    outWriter.write(tags.get(i) + "\n" );
                }

                outWriter.flush();

                result = true;

            }catch (Exception e) {
                println(e.getMessage());
            }
        }

        return result;
    }

    /**
     * Get the directory for the sketch 
     * @return a file
     */
    public File getSketchDir() {
        File extDir = Environment.getExternalStorageDirectory();
        String sketchName = this.getClass().getSimpleName();
        return new File(extDir, sketchName);
    }
}

  public int sketchWidth() { return displayWidth; }
  public int sketchHeight() { return displayHeight; }
}
