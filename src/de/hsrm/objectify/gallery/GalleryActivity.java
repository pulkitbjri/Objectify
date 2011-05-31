package de.hsrm.objectify.gallery;

import java.io.File;
import java.io.FilenameFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Gallery;
import android.widget.ImageView;
import android.widget.Toast;
import de.hsrm.objectify.R;
import de.hsrm.objectify.database.DatabaseAdapter;
import de.hsrm.objectify.database.DatabaseProvider;
import de.hsrm.objectify.ui.BaseActivity;
import de.hsrm.objectify.utils.ExternalDirectory;

/**
 * An activity with a gallery included, showing all images made within this app
 * and displaying some info about the 3D objects.
 * 
 * @author kwolf001
 * 
 */
public class GalleryActivity extends BaseActivity {

	private static final String TAG = "GalleryActivity";
	private Gallery gallery;
	private ImageView currentImage;
	private GalleryAdapter adapter;
	private Context context;
	private Uri galleryUri;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.gallery);
		setupActionBar(getString(R.string.gallery), 0);
		addNewActionButton(R.drawable.ic_title_show, R.string.show, new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
			}
		});
		addNewActionButton(R.drawable.ic_title_delete, R.string.delete, new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				AlertDialog.Builder builder = new AlertDialog.Builder(context);
				builder.setMessage(getString(R.string.chosen_object_really_delete))
					.setCancelable(false)
					.setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							long id = gallery.getSelectedItemId();
							deleteFromDatabaseAndSD(id);
							Toast.makeText(context, getString(R.string.deleted_successfully), Toast.LENGTH_SHORT).show();
							dialog.cancel();
						}
					})
					.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
						
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.cancel();
						}
					});
				AlertDialog alert = builder.create();
				alert.show();
				
				
			}
		});
				
		context = this;
		
		gallery = (Gallery) findViewById(R.id.object_gallery);
		currentImage = (ImageView) findViewById(R.id.current_object_image);
		galleryUri = DatabaseProvider.CONTENT_URI.buildUpon().appendPath("gallery").build();
		Cursor cursor = this.managedQuery(galleryUri, null, null, null, null);
		adapter = new GalleryAdapter(this, cursor);
		gallery.setAdapter(adapter);
		gallery.setOnItemSelectedListener(galleryItemSelectedListener());
		
		if (adapter.getCount() == 0) {
			showMessageAndExit(getString(R.string.gallery), getString(R.string.no_objects_saved));
		}
	}
	
	private OnItemSelectedListener galleryItemSelectedListener() {
		OnItemSelectedListener listener = new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View v, int position, long id) {
				String[] args = { String.valueOf(id) };
				Cursor c = getContentResolver().query(galleryUri, null, DatabaseAdapter.GALLERY_ID_KEY+"=?", args, null);
				c.moveToFirst();
				byte[] bb = c.getBlob(DatabaseAdapter.GALLERY_IMAGE_COLUMN);
				currentImage.setImageBitmap(BitmapFactory.decodeByteArray(bb, 0, bb.length));
				currentImage.setScaleType(ImageView.ScaleType.CENTER);
				c.close();
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		};
		
		return listener;
	}

	/**
	 * Deletes all images with the given id from the storage and database and
	 * calls adapter to refresh itself.
	 * 
	 * @param id
	 *            gallery id in database
	 */
	private void deleteFromDatabaseAndSD(long id) {
		ContentResolver cr = getContentResolver();
		Cursor c = cr.query(galleryUri, null, DatabaseAdapter.GALLERY_ID_KEY+"=?", new String[] { String.valueOf(id) }, null);
		c.moveToFirst();
		final String image_suffix = c.getString(DatabaseAdapter.GALLERY_SUFFIX_COLUMN);
		File imageDir = new File(ExternalDirectory.getExternalImageDirectory());
		File[] images = imageDir.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String filename) {
				return (filename.contains(image_suffix));
			}
		});
		for (File img : images) {
			img.delete();
		}
		
		cr.delete(galleryUri, DatabaseAdapter.GALLERY_ID_KEY+"=?", new String[] { String.valueOf(id) } );
		c.close();
		adapter.cursor.requery();
		adapter.notifyDataSetChanged();
		if (adapter.getCount() == 0) {
			finish();
		}
	}
	
	/**
	 * Shows specific Dialog to user and finishes current Activity
	 * 
	 * @param title
	 *            title of dialog
	 * @param msg
	 *            message of dialog
	 */
	private void showMessageAndExit(String title, String msg) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		builder.setTitle(title).setMessage(msg).setCancelable(false).setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				((Activity) context).finish();
			}
		});
		AlertDialog alert = builder.create();
		alert.show();
	}
}
