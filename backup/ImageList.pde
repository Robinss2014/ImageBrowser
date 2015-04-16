/**
 * @file ImageList.java
 * @author Sisi Wei, 915565877
 * @date 15 Mar 15
 * @description A imageList for the ImageBrowser2.
 */

import java.util.Arrays;
import java.io.*;
import android.os.Environment;
import android.content.res.AssetManager;

import android.media.*;
import android.content.res.*;

class ImageList {
	private AssetManager am;
	private PImage img;

    public ArrayList<PImage> photos;
    String [] fileNames;
    File sketchDir;

    private String srcDir;
    private ListIterator<PImage> cursor;
    private ArrayList<int[]> dimens;
    private ArrayList<ArrayList<String>> allTags;
    ArrayList<String> curTags;

    /**
     * Retrieves the number of images in the PhotoList.
     * @return the number of images
     */
    int size() {
        return photos.size();
    }

    /**
     * Class constructor. Image scaling is applied.
     * @param imgDir the name of the directory containing the image files
     * @param scaleWidth the maximum scaled image width
     * @param scaleHeight the maximum scaled image height
     */
    ImageList(String imgDir, int scaleWidth, int scaleHeight) {
        srcDir = imgDir;
		am = getAssets();

        sketchDir = getFilesDir();

        fileNames = new String[2];
        photos = new ArrayList<PImage>();

        int i;
        allTags = new ArrayList<ArrayList<String>>();

		try {
		    fileNames = am.list(srcDir);
		    System.out.println("Numbers of Images in data/" + srcDir + " : " + fileNames.length);
		  }
		  catch (IOException e) {
		    System.out.println("Error: folder Images not found");
		    return;
		}

		if (fileNames.length >= 1 ) {

			for (i=0; i<fileNames.length; i++) {

				curTags = new ArrayList<String>();
	            allTags.add(curTags);

				if(fileNames[i].endsWith(".png") || fileNames[i].endsWith(".jpg")){
					img = loadImage(srcDir + '/' + fileNames[i]);
					img.loadPixels();
				    photos.add(img);

                    readTags(i,curTags);
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
    void add(PImage image, int[] corner) {
        cursor.add(image);
    }

    /**
     * Retrieves the scaled dimensions of an image.
     * @param offset the relative position from the current cursor location
     * @return an integer array containing the scaled dimensions of the
     *   image at the offset position
     * @return null if the PhotoList is empty
     */
    int[] getScaling(int offset) {
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
    int[] getScaling(int offset, int maxWidth, int maxHeight) {
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
    void scaleImages(int maxWidth, int maxHeight) {
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
    PImage next() {
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
     * Retrieves the PImage in the given position relative to the current
     *   cursor position.
     * @param offset the relative position of the PImage
     * @return the selected PImage
     * @return null if the PhotoList is empty
     */
    PImage getImg(int offset) {
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
    PImage prev() {
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
    ArrayList<String> getTags() {
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
    boolean saveTags(ArrayList<String> tags) {
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
    File getSketchDir() {
        File extDir = Environment.getExternalStorageDirectory();
        String sketchName = this.getClass().getSimpleName();
        return new File(extDir, sketchName);
    }
}
