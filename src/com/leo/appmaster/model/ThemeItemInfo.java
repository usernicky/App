package com.leo.appmaster.model;

import com.leo.appmaster.Constants;

import android.graphics.drawable.Drawable;

public class ThemeItemInfo {
	/**
	 * theme name
	 */
	public String themeName;
	/**
	 * theme package name
	 */
	public String packageName;
	/**
	 * local , online, defalut,
	 */
	public String label;
	/**
	 * local , online, default
	 */
	public int themeType = Constants.THEME_TYPE_DEFAULT;
	/**
	 * theme preview
	 */
	public Drawable themeImage;
	/**
	 * theme preview url
	 */
	public String previewUrl;
	/**
	 * theme apk download url
	 */
	public String downloadUrl;
	/**
	 * the theme is used
	 */
	public boolean curUsedTheme;
	/**
	 * theme apk size
	 */
	public int size;
	/**
	 * hot , new, and so on...
	 */
	public int tag = Constants.THEME_TAG_NONE;
	
	public long installTime = Long.MAX_VALUE;

	public Drawable themeLogo;
	
	@Override
	public String toString() {
		return "ThemeInfo: themeName = " + themeName + "   packageName = "
				+ packageName + "    previewUrl = " + previewUrl
				+ "    downloadUrl = " + downloadUrl + "   tag = " + tag;
	}

}
