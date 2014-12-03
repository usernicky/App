
package com.leo.appmaster.model;

public class AppDetailInfo extends BaseAppInfo {
    /*
     * app cache info
     */
	public CacheInfo mCacheInfo;
    /*
     * all user permission of app
     */
    public AppPermissionInfo mPermissionInfo;
    /*
     * app traffic info
     */
    public TrafficInfo mTrafficInfo;
    /*
     * app power comsumption info
     */
    public double mPowerComsuPercent;

    public String sourceDir;
    
    public boolean isChecked;
    
    public boolean isBackuped;

    public AppDetailInfo() {
        mCacheInfo = new CacheInfo();
        mPermissionInfo = new AppPermissionInfo();
        mTrafficInfo = new TrafficInfo();
    }


}
