package com.chibird.impressionist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ActionMenuView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity implements OnMenuItemClickListener {

    private static int RESULT_LOAD_IMAGE = 1;
    private  ImpressionistView _impressionistView;

    // These images are downloaded and added to the Android Gallery when the 'Download Images' button is clicked.
    // This was super useful on the emulator where there are no images by default
    // Additional images by:
    // Mountains: http://bojrk.tumblr.com/post/123164161919
    // Flowers: http://russia-instagram.tumblr.com/post/122189793049
    // Clouds: http://sassy-queer.tumblr.com/post/135509432174
    private static String[] IMAGE_URLS ={
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/BoliviaBird_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/BolivianDoor_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/MinnesotaFlower_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/PeruHike_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/ReginaSquirrel_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/SucreDog_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/SucreStreet_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/SucreStreet_PhotoByJonFroehlich2(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/SucreWine_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/WashingtonStateFlower_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/JonILikeThisShirt_Medium.JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/JonUW_(853x1280).jpg",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/MattMThermography_Medium.jpg",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/PinkFlower_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/PinkFlower2_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/PurpleFlowerPlusButterfly_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/WhiteFlower_PhotoByJonFroehlich(Medium).JPG",
            "http://www.cs.umd.edu/class/spring2016/cmsc434/assignments/IA08-AndroidII/Images/YellowFlower_PhotoByJonFroehlich(Medium).JPG",
            "http://66.media.tumblr.com/80d30a65e7b762a983c360854e2f8752/tumblr_nzm44uDdNF1uqv28uo1_500.jpg",
            "http://66.media.tumblr.com/83a862f473814224d2fde0497c5347b7/tumblr_nqw76t7u9i1st2k7zo1_500.jpg",
            "http://67.media.tumblr.com/9f7f0443507509b9e7b8aae703636627/tumblr_nqw76t7u9i1st2k7zo2_1280.jpg",
            "http://66.media.tumblr.com/506b57883cad76abf2ea301c35bb5d8b/tumblr_nqd3qnDQGc1s1rffco1_1280.jpg",
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _impressionistView = (ImpressionistView)findViewById(R.id.viewImpressionist);
        ImageView imageView = (ImageView)findViewById(R.id.viewImage);
        _impressionistView.setImageView(imageView);

    }

    /**
     * Saves the bitmap to the gallery.
     */
    public void onButtonSave(View v){
        Bitmap bitmapImage = _impressionistView.getBitmap();
        ContextWrapper cw = new ContextWrapper(getApplicationContext());

        File externalStorageDirFile = Environment.getExternalStorageDirectory();
        String externalStorageDirStr = Environment.getExternalStorageDirectory().getAbsolutePath();
        boolean checkStorage = FileUtils.checkPermissionToWriteToExternalStorage(MainActivity.this);
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "savedImage" +Math.random()*1000 + ".jpg");


        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            // Use the compress method on the BitMap object to write image to the OutputStream
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                FileUtils.addImageToGallery(file.getAbsolutePath(), getApplicationContext());
                Toast.makeText(getApplicationContext(), "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void onButtonClickClear(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Clear Painting?")
                .setMessage("Do you really want to clear your painting?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        Toast.makeText(MainActivity.this, "Painting cleared", Toast.LENGTH_SHORT).show();
                        _impressionistView.clearPainting();
                    }})
                .setNegativeButton(android.R.string.no, null).show();
    }

    public void onButtonClickSetBrush(View v) {
        PopupMenu popupMenu = new PopupMenu(this, v);
        popupMenu.setOnMenuItemClickListener(this);
        popupMenu.inflate(R.menu.popup_menu);
        popupMenu.show();
    }

    public void onButtonBase(View v){
        _impressionistView.setBaseColors();
    }

    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuCircle:
                Toast.makeText(this, "Circle Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Circle);
                return true;
            case R.id.menuSquare:
                Toast.makeText(this, "Square Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Square);
                return true;
            case R.id.menuLine:
                Toast.makeText(this, "Line Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.Line);
                return true;
            case R.id.menuCircleSplatter:
                Toast.makeText(this, "Circle Splatter Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.CircleSplatter);
                return true;
            case R.id.menuLineSplatter:
                Toast.makeText(this, "Smear Brush", Toast.LENGTH_SHORT).show();
                _impressionistView.setBrushType(BrushType.LineSplatter);
                return true;
        }
        return false;
    }


    /**
     * Downloads test images to use in the assignment. Feel free to use any images you want. I only made this
     * as an easy way to get images onto the emulator.
     *
     * @param v
     */
    public void onButtonClickDownloadImages(View v){

        // Without this call, the app was crashing in the onActivityResult method when trying to read from file system
        FileUtils.verifyStoragePermissions(this);

        // Amazing Stackoverflow post on downloading images: http://stackoverflow.com/questions/15549421/how-to-download-and-save-an-image-in-android
        final BasicImageDownloader imageDownloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {

            @Override
            public void onError(String imageUrl, BasicImageDownloader.ImageError error) {
                Log.v("BasicImageDownloader", "onError: " + error);
            }

            @Override
            public void onProgressChange(String imageUrl, int percent) {
                Log.v("BasicImageDownloader", "onProgressChange: " + percent);
            }

            @Override
            public void onComplete(String imageUrl, Bitmap downloadedBitmap) {
                File externalStorageDirFile = Environment.getExternalStorageDirectory();
                String externalStorageDirStr = Environment.getExternalStorageDirectory().getAbsolutePath();
                boolean checkStorage = FileUtils.checkPermissionToWriteToExternalStorage(MainActivity.this);
                String guessedFilename = URLUtil.guessFileName(imageUrl, null, null);

                // See: http://developer.android.com/training/basics/data-storage/files.html
                // Get the directory for the user's public pictures directory.
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), guessedFilename);
                try {
                    boolean compressSucceeded = downloadedBitmap.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(file));
                    FileUtils.addImageToGallery(file.getAbsolutePath(), getApplicationContext());
                    Toast.makeText(getApplicationContext(), "Saved to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        });

        for(String url: IMAGE_URLS){
            imageDownloader.download(url, true);
        }
    }

    /**
     * Loads an image from the Gallery into the ImageView
     *
     * @param v
     */
    public void onButtonClickLoadImage(View v){

        // Without this call, the app was crashing in the onActivityResult method when trying to read from file system
        FileUtils.verifyStoragePermissions(this);

        Intent i = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

        startActivityForResult(i, RESULT_LOAD_IMAGE);
    }

    /**
     * Called automatically when an image has been selected in the Gallery
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri imageUri = data.getData();

            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                ImageView imageView = (ImageView) findViewById(R.id.viewImage);

                // destroy the drawing cache to ensure that when a new image is loaded, its cached
                imageView.destroyDrawingCache();
                imageView.setImageBitmap(bitmap);
                imageView.setDrawingCacheEnabled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }
}
