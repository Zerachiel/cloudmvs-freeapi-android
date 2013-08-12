package com.mobileacuity.android.cloudmvsdemo;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.DateUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;

public class SearchCloud extends Thread
{
	// Too see all logging use "adb shell setprop log.tag.CloudSearching DEBUG" - default is WARN and above
	private static final String TAG = "CloudSearching";
	
	static String mars3Url = null;
	static Mac sha1 = null;
	
	//recommended values for image sent to server:
	private final int JPEG_QUALITY = 40;
	//crops 25% of image height, creating 480x480 image from VGA.
	private final int CROPPING_PERCENTAGE = 25;
	
	private static final int SOCKET_TIMEOUT = 20000;
	private static final String CUSTOMER = "test";
	private static final String SECRET = "testsecret";

	public static final int WORKING = 0;
	public static final int MATCH = 1;
	public static final int NOMATCH = 2;
	public static final int NON200 = 3;
	public static final int FAIL = 4;
	public static final int TIMEOUT = 5;
	public static final int REFUSED = 6;
	
	private SearchingActivity searchingUI = null;
	
	private Context context = null;
	
	// Public state used by SearchingActivity
	public int state = WORKING;
	public String resultStr = null;
	public Result[] results = null;
	
	private Bitmap image = null;
	
	public SearchCloud(Context cont, Bitmap image) {
		super();
		this.context = cont;
		this.image = image;

		start();
	}
			
	@Override
	public void run() {
		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Background thread starting.");
		
    	try {
	        
	        if(CROPPING_PERCENTAGE!=0) {
	        	//note- width is actually height
	        	int preWidth = image.getWidth();
	        	int crop = (int)(preWidth * (CROPPING_PERCENTAGE/200.0f));
	        	int width = preWidth - 2*crop;
	        	image = Bitmap.createBitmap(image, crop, 0, width, image.getHeight());
	        }
	        
	        ByteArrayOutputStream out = new ByteArrayOutputStream();
	    	image.compress(CompressFormat.JPEG, JPEG_QUALITY, out);
	    	byte[] jpgImage = out.toByteArray();
	    	image=null;
	    	out=null;

	    	if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "About to POST "+jpgImage.length+" bytes of JPG image.");

	    	HttpClient http = new DefaultHttpClient();
	    	http.getParams().setIntParameter("http.socket.timeout", SOCKET_TIMEOUT);
	    	ResponseHandler<String> responseHandler = new BasicResponseHandler();
			TelephonyManager telephonyManager = (TelephonyManager)context.getSystemService(SearchingActivity.TELEPHONY_SERVICE);
			
	    	 if (sha1==null) sha1 = makeSha1();
	    	
	    	// Request signature
			String date = DateUtils.formatDate(new Date());
			String identity = telephonyManager.getDeviceId();
			String toSign = CUSTOMER+"POST"+mars3Url+date+"identity"+identity+jpgImage.length;
			byte[] sigBytes = sha1.doFinal(toSign.getBytes("UTF8"));
			String sig = Base64.encodeToString(sigBytes, Base64.NO_WRAP);	// Can't have newlines in header values!
			if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, toSign+" --> "+sig);
			
			// Build the request
        	HttpPost post = new HttpPost(mars3Url+"?identity="+identity);
        	ByteArrayEntity entity = new ByteArrayEntity(jpgImage);
        	entity.setContentType("image/jpeg");
        	post.setEntity(entity);
        	post.setHeader("Accept", "application/json");
        	post.setHeader("Date", date);
        	post.setHeader("Authorization", "MAAPIv1 test "+sig);

        	// Run the POST
			String response = http.execute(post, responseHandler);
			jpgImage = null;
			
			// Unpack the response
			results = Result.unpackJsonResultsList(response);
			if (results.length==0) {
				if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Match failed!");
				state = NOMATCH;
			} else {
				if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Number of matches: "+results.length);
				state = MATCH;
			}
			
		}catch (HttpResponseException e) {
			    int response = e.getStatusCode();
			    if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Non 200 HTTP response: "+response, e);
				if (response==400) {
					state = REFUSED;
				} else {
					state = NON200;
					resultStr=""+e.getStatusCode();
				}
		}catch (SocketTimeoutException e) {
			    if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Timeout during search request.");
			    state = TIMEOUT;
		}catch (Exception e) {
			    if (Log.isLoggable(TAG, Log.ERROR)) Log.e(TAG, "Exception thrown uploading image: "+e.getMessage(), e);
				state = FAIL;
				resultStr = e.getMessage();
		}

    	// Deal with the outcome in the UI
    	while(searchingUI==null) {};
    	searchingUI.runBackgroundDone();

		if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "Background thread ending.");
	}
	
	private Mac makeSha1() throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeyException
	{
		Mac result = Mac.getInstance("HmacSHA1");
		byte[] keyBytes = SECRET.getBytes("UTF8");
		SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA1");
		result.init(signingKey);
		return result;
	}
	
	protected void setSearchingUI(SearchingActivity ui) {
		searchingUI = ui;
	}
}
