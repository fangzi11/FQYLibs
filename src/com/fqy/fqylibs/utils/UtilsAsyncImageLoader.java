package com.fqy.fqylibs.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.util.LruCache;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

/**
 * 异步加载图片工具类
 * 
 * @Title: UtilsAsyncImageLoader.java
 * @Package com.fqy.fqylibs.utils
 * @author: Fang Qingyou
 * @date 2015年7月11日上午9:24:13
 * @version V1.0
 */

public class UtilsAsyncImageLoader {
	private LruCache<String, Drawable> imageCache;
	private static String FILEPATH;
	private static String IMAGEENDWITH = ".jpg";

	private ExecutorService service = Executors.newSingleThreadExecutor();

	public UtilsAsyncImageLoader(String filePath) {

		if (imageCache == null) {// 初始化，4Mb内存
			imageCache = new LruCache<String, Drawable>(4 * 1024 * 1024);
		}

		FILEPATH = Environment.getExternalStorageDirectory().getAbsolutePath()
				+ "/" + filePath + "/imageCache/";
		makeDir();

	}

	/**
	 * 创建文件夹
	 * 
	 * @author: Fang Qingyou
	 * @date 2015年7月16日下午3:58:10
	 */
	private static void makeDir() {
		File fileDir = new File(FILEPATH);
		File fileNoMFedia = new File(FILEPATH + ".nomedia");
		if (!fileDir.exists()) {
			fileDir.mkdirs();
			try {// 屏蔽资源
				if (!fileNoMFedia.exists()) {
					fileNoMFedia.createNewFile();
				}
			} catch (Exception e) {

				e.printStackTrace();
			}
		}
	}

	/**
	 * 从URL 加载图片，无缓存
	 * 
	 * @author: Fang Qingyou
	 * @date 2015年7月14日下午1:23:47
	 * @param view
	 * @param urlPath
	 * @return
	 */
	@SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
	public static <T extends View> Drawable loadImageFromUrl(T view,
			String urlPath) {
		InputStream stream = null;
		File file = null;
		FileOutputStream outputStream = null;

		try {
			stream = new URL(urlPath).openStream();

			file = new File(FILEPATH + UtilsMD5.GetMD5Code16(urlPath)
					+ IMAGEENDWITH);

			if (!file.exists()) {
				file.createNewFile();
			} else {
				file.delete();
				file.createNewFile();
			}

			outputStream = new FileOutputStream(file);

			byte[] bs = new byte[1024];
			int flag;
			while ((flag = stream.read(bs)) != -1) {
				outputStream.write(bs, 0, flag);
			}

			// 压缩大小
			Bitmap smallBitmap = UtilsImage.getSmallBitmap(file
					.getAbsolutePath());

			// 压缩质量
			smallBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream);

			return new BitmapDrawable(view.getResources(), smallBitmap);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} finally {
			try {

				if (outputStream != null) {
					outputStream.close();
				}

				if (stream != null) {
					stream.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private String fileName;
	private File file;

	/**
	 * 从URL 加载图片，有缓存
	 * 
	 * @author: Fang Qingyou
	 * @date 2015年7月16日下午4:02:22
	 * @param imageUrl
	 * @param imageView
	 */
	public void loadImageFromUrl(final String imageUrl,
			final ImageView imageView) {

		Drawable drawable = null;

		fileName = FILEPATH + UtilsMD5.GetMD5Code16(imageUrl) + IMAGEENDWITH;
		file = new File(fileName);
		if (file.exists()) {
			drawable = new BitmapDrawable(imageView.getResources(),
					UtilsImage.getSmallBitmap(fileName));
			if (drawable != null) {
				imageCache.put(fileName, drawable);
				imageView.setVisibility(View.VISIBLE);
				imageView.setImageDrawable(drawable);
				return;
			}
		}

		drawable = imageCache.get(fileName);

		if (drawable != null) {
			imageView.setVisibility(View.VISIBLE);
			imageView.setImageDrawable(drawable);
			return;
		}

		final Handler handler = new Handler() {
			public void handleMessage(Message msg) {
				imageView.setImageDrawable((Drawable) msg.obj);
			}
		};
		service.submit(new Runnable() {

			@Override
			public void run() {
				Drawable drawable = loadImageFromUrl(imageView, imageUrl);

				if (drawable != null) {
					imageCache.put(fileName, drawable);

					Message message = handler.obtainMessage(0, drawable);

					handler.sendMessage(message);
				}
			}

		});

	}

}
