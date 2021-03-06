package com.android.splus.sdk._uc;
import cn.uc.gamesdk.UCCallbackListener;
import cn.uc.gamesdk.UCCallbackListenerNullException;
import cn.uc.gamesdk.UCFloatButtonCreateException;
import cn.uc.gamesdk.UCGameSDK;
import cn.uc.gamesdk.UCGameSDKStatusCode;
import cn.uc.gamesdk.UCLogLevel;
import cn.uc.gamesdk.UCLoginFaceType;
import cn.uc.gamesdk.UCOrientation;
import cn.uc.gamesdk.info.FeatureSwitch;
import cn.uc.gamesdk.info.GameParamInfo;
import cn.uc.gamesdk.info.OrderInfo;
import cn.uc.gamesdk.info.PaymentInfo;

import com.android.splus.sdk.apiinterface.APIConstants;
import com.android.splus.sdk.apiinterface.DateUtil;
import com.android.splus.sdk.apiinterface.IPayManager;
import com.android.splus.sdk.apiinterface.InitBean;
import com.android.splus.sdk.apiinterface.InitBean.InitBeanSuccess;
import com.android.splus.sdk.apiinterface.InitCallBack;
import com.android.splus.sdk.apiinterface.LoginCallBack;
import com.android.splus.sdk.apiinterface.LoginParser;
import com.android.splus.sdk.apiinterface.LogoutCallBack;
import com.android.splus.sdk.apiinterface.MD5Util;
import com.android.splus.sdk.apiinterface.NetHttpUtil;
import com.android.splus.sdk.apiinterface.NetHttpUtil.DataCallback;
import com.android.splus.sdk.apiinterface.RechargeCallBack;
import com.android.splus.sdk.apiinterface.RequestModel;
import com.android.splus.sdk.apiinterface.UserAccount;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.res.Configuration;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Properties;

public class _UC implements IPayManager {

    private static final String TAG = "_UC";

    private static _UC _mUC;

    // 平台参数
    private Properties mProperties;

    private String mAppId;

    private String mGameId;

    private InitBean mInitBean;

    private InitCallBack mInitCallBack;

    private Activity mActivity;

    private LoginCallBack mLoginCallBack;

    private RechargeCallBack mRechargeCallBack;

    private LogoutCallBack mLogoutCallBack;


    // 下面参数仅在测试时用
    private UserAccount mUserModel;

    private int mUid = 0;

    private String mPassport;

    private String mSessionid;

    //测试环境
    private boolean mDebugMode = true;

    private boolean mLogined = false;

    private ProgressDialog mProgressDialog;

    private float mMoney ;

    private String mPayway="_DCN" ;
    private Integer mServerId;
    private Integer mRoleId;
    private String mRoleName;
    private String mPext;

    /**
     * @Title: _UC
     * @Description:( 将构造函数私有化)
     */
    private _UC() {

    }

    /**
     * @Title: getInstance(获取实例)
     * @author xiaoming.yuan
     * @data 2014-2-26 下午2:30:02
     * @return _UC 返回类型
     */
    public static _UC getInstance() {

        if (_mUC == null) {
            synchronized (_UC.class) {
                if (_mUC == null) {
                    _mUC = new _UC();
                }
            }
        }
        return _mUC;
    }

    @Override
    public void setInitBean(InitBean bean) {
        this.mInitBean = bean;
        this.mProperties = mInitBean.getProperties();
    }

    @Override
    public void init(Activity activity, Integer gameid, String appkey, InitCallBack initCallBack, boolean useUpdate, Integer orientation) {
        this.mInitCallBack = initCallBack;
        this.mActivity = activity;
        mLogined=false;
        mInitBean.initSplus(activity, initCallBack ,new InitBeanSuccess() {

            @Override
            public void initBeaned(boolean initBeanSuccess) {
                String uc_serverId = "0";
                if (mProperties != null) {
                    mAppId = mProperties.getProperty("uc_appid") == null ? "0" : mProperties.getProperty("91_appid");
                    mGameId = mProperties.getProperty("uc_gameId") == null ? "0" : mProperties.getProperty("uc_gameId");
                    uc_serverId = mProperties.getProperty("uc_serverId") == null ? "0" : mProperties.getProperty("uc_serverId");
                }
                try {
                    GameParamInfo gpi = new GameParamInfo();// 下面的值仅供参考
                    gpi.setCpId(Integer.valueOf(mAppId));
                    gpi.setGameId(Integer.valueOf(mGameId));
                    gpi.setServerId(Integer.valueOf(uc_serverId));
                    // gpi.setChannelId(2); // 渠道号统一处理，已不需设置，此参数已废弃，服务端此参数请设置值为2

                    // 在九游社区设置显示查询充值历史和显示切换账号按钮，
                    // 在不设置的情况下，默认情况情况下，生产环境显示查询充值历史记录按钮，不显示切换账户按钮
                    // 测试环境设置无效
                    gpi.setFeatureSwitch(new FeatureSwitch(true, false));
                    if (mInitBean.getOrientation() == Configuration.ORIENTATION_LANDSCAPE) {
                        // 设置SDK登录界面为横屏，个人中心及充值页面默认为强制竖屏，无法修改
                        UCGameSDK.defaultSDK().setOrientation(UCOrientation.LANDSCAPE);
                    } else {
                        // 设置SDK登录界面为竖屏
                        UCGameSDK.defaultSDK().setOrientation(UCOrientation.PORTRAIT);
                    }

                    // 设置登录界面：
                    // USE_WIDGET - 简版登录界面
                    // USE_STANDARD - 标准版登录界面
                    UCGameSDK.defaultSDK().setLoginUISwitch(UCLoginFaceType.USE_WIDGET);
                    // setUIStyle已过时，不需调用。
                    // UCGameSDK.defaultSDK().setUIStyle(UCUIStyle.STANDARD);
                    //对于需要支持账户切换/退出账号的游戏，必须在此设置退出侦听器
                    UCGameSDK.defaultSDK().setLogoutNotifyListener(mLogoutNotify);
                    UCGameSDK.defaultSDK().initSDK(mActivity, UCLogLevel.ERROR, mDebugMode, gpi, mInitCallback);
                } catch (UCCallbackListenerNullException e) {
                    e.printStackTrace();
                    mInitCallBack.initFaile(e.getLocalizedMessage());
                } catch (Exception e) {
                    e.printStackTrace();
                    mInitCallBack.initFaile(e.getLocalizedMessage());
                }
            }
        });

    }

    UCCallbackListener<String> mInitCallback = new UCCallbackListener<String>() {

        @Override
        public void callback(int code, String msg) {
            Log.e("UCGameSDK", "UCGameSDK初始化接口返回数据 msg:" + msg + ",code:" + code);
            switch (code) {
                // 初始化成功,可以执行后续的登录充值操作
                case UCGameSDKStatusCode.SUCCESS:
                    // 调用sdk登录接口
                    mInitCallBack.initSuccess("初始化完成", null);
                    break;
                    // 初始化失败
                case UCGameSDKStatusCode.INIT_FAIL:
                    // 调用sdk初始化接口
                    mInitCallBack.initFaile("初始化失败");
                    break;
            }
        }

    };


    @Override
    public void login(Activity activity, LoginCallBack loginCallBack) {
        this.mActivity = activity;
        this.mLoginCallBack = loginCallBack;
        mLogined=false;
        try {
            UCGameSDK.defaultSDK().login(activity, loginCallbackListener);
        } catch (UCCallbackListenerNullException e) {
            e.printStackTrace();
            loginCallBack.loginFaile(e.getLocalizedMessage());
        }
    }

    // 登录接口回调。从这里可以获取登录结果。
    UCCallbackListener<String> loginCallbackListener = new UCCallbackListener<String>() {

        @Override
        public void callback(int code, String msg) {
            Log.e("UCGameSDK", "UCGameSdk登录接口返回数据:code=" + code + ",msg=" + msg);
            // 登录成功。此时可以获取sid。并使用sid进行游戏的登录逻辑。
            // 客户端无法直接获取UCID
            if (code == UCGameSDKStatusCode.SUCCESS) {
                mLogined=true;
            }

            // 登录失败。应该先执行初始化成功后再进行登录调用。
            if (code == UCGameSDKStatusCode.NO_INIT) {
                // 没有初始化就进行登录调用，需要游戏调用SDK初始化方法
                mLoginCallBack.loginFaile("SDK未初始化");
            }
            // 登录退出。该回调会在登录界面退出时执行。
            if (code == UCGameSDKStatusCode.LOGIN_EXIT) {
                // 登录界面关闭，游戏需判断此时是否已登录成功进行相应操作
                if(mLogined){
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    Integer gameid = mInitBean.getGameid();
                    String partner = mInitBean.getPartner();
                    String referer = mInitBean.getReferer();
                    long unixTime = DateUtil.getUnixTime();
                    String deviceno=mInitBean.getDeviceNo();
                    String signStr =deviceno+gameid+partner+referer+unixTime+mInitBean.getAppKey();
                    String sign=MD5Util.getMd5toLowerCase(signStr);
                    // 获取sid。（注：ucid需要使用sid作为身份标识去SDK的服务器获取）
                    String sid = UCGameSDK.defaultSDK().getSid();

                    params.put("deviceno", deviceno);
                    params.put("gameid", gameid);
                    params.put("partner",partner);
                    params.put("referer", referer);
                    params.put("time", unixTime);
                    params.put("sign", sign);
                    params.put("partner_sessionid", sid);
                    params.put("partner_uid",  "");
                    params.put("partner_token", "");
                    params.put("partner_nickname", "");
                    params.put("partner_username", "");
                    params.put("partner_appid", mAppId);
                    String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.LOGIN_URL);
                    System.out.println(hashMapTOgetParams);
                    showProgressDialog(mActivity);
                    NetHttpUtil.getDataFromServerPOST(mActivity,new RequestModel(APIConstants.LOGIN_URL, params, new LoginParser()),mLoginDataCallBack);
                }else{
                    mLoginCallBack.backKey("登录退出");
                }
            }
        }

    };


    private DataCallback<JSONObject> mLoginDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            closeProgressDialog();
            Log.d(TAG, "mLoginDataCallBack---------"+paramObject.toString());
            try {
                if (paramObject != null && paramObject.optInt("code") == 1) {
                    JSONObject data = paramObject.optJSONObject("data");
                    mUid = data.optInt("uid");
                    mPassport = data.optString("passport");
                    mSessionid = data.optString("sessionid");
                    mUserModel=new UserAccount() {

                        @Override
                        public Integer getUserUid() {
                            return mUid;

                        }

                        @Override
                        public String getUserName() {
                            return mPassport;

                        }

                        @Override
                        public String getSession() {
                            return mSessionid;

                        }
                    };
                    mLoginCallBack.loginSuccess(mUserModel);

                } else {
                    mLoginCallBack.loginFaile(paramObject.optString("msg"));
                }
            } catch (Exception e) {
                mLoginCallBack.loginFaile(e.getLocalizedMessage());
            }
        }

        @Override
        public void callbackError(String error) {
            closeProgressDialog();
            mLoginCallBack.loginFaile(error);
        }

    };

    @Override
    public void recharge(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, RechargeCallBack rechargeCallBack) {
        rechargeByQuota(activity, serverId, serverName, roleId, roleName, outOrderid, pext, 0f, rechargeCallBack);
    }

    @Override
    public void rechargeByQuota(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String outOrderid, String pext, Float money, RechargeCallBack rechargeCallBack) {
        this.mActivity=activity;
        this.mRechargeCallBack=rechargeCallBack;

        this.mServerId=serverId;
        this.mRoleId=roleId;
        this.mRoleName=roleName;
        this.mPext=pext;
        this.mMoney=money;

        HashMap<String, Object> params = new HashMap<String, Object>();
        Integer gameid = mInitBean.getGameid();
        String partner = mInitBean.getPartner();
        String referer = mInitBean.getReferer();
        long unixTime = DateUtil.getUnixTime();
        String deviceno=mInitBean.getDeviceNo();
        String signStr =gameid+serverName+deviceno+referer+partner+mUid+mMoney+mPayway+unixTime+mInitBean.getAppKey();
        String sign=MD5Util.getMd5toLowerCase(signStr);

        params.put("deviceno", deviceno);
        params.put("gameid", gameid);
        params.put("partner",partner);
        params.put("referer", referer);
        params.put("time", unixTime);
        params.put("sign", sign);
        params.put("uid",mUid);
        params.put("passport",mPassport);
        params.put("serverId",serverId);
        params.put("serverName",serverName);
        params.put("roleId",roleId);
        params.put("roleName",roleName);
        params.put("money",mMoney);
        params.put("pext",pext);
        params.put("payway",mPayway);
        params.put("outOrderid",outOrderid);
        String hashMapTOgetParams = NetHttpUtil.hashMapTOgetParams(params, APIConstants.PAY_URL);
        System.out.println(hashMapTOgetParams);
        NetHttpUtil.getDataFromServerPOST(activity, new RequestModel(APIConstants.PAY_URL, params,new LoginParser()),mRechargeDataCallBack);

    }

    private DataCallback<JSONObject> mRechargeDataCallBack = new DataCallback<JSONObject>() {

        @Override
        public void callbackSuccess(JSONObject paramObject) {
            Log.d(TAG, "mRechargeDataCallBack---------"+paramObject.toString());

            if (paramObject != null && (paramObject.optInt("code") == 1||paramObject.optInt("code") == 24)) {
                JSONObject data = paramObject.optJSONObject("data");
                String orderid=data.optString("orderid");
                PaymentInfo pInfo = new PaymentInfo(); //创建Payment对象，用于传递充值信息
                //设置成功提交订单后是否允许用户连续充值，默认为true。
                pInfo.setAllowContinuousPay(false);
                //设置 充值自定义参数，此参数不作任何处理，在充值完成后通知游戏服务器充值结果时原封不动传给游戏服务器。此参数为可选参数，默认为空。
                pInfo.setCustomInfo(mPext);
                //设置充值的游戏服务器ID，此为可选参数，默认是0，不设置或设置为0 时，会使用初始化时设置的服务器ID。必须使用正确的ID值（UC分配的serverId）才可以打开支付页面。如使用正确ID仍无法打开时，请在开放平台检查是否已经配置了对应环境 对应ID的回调地址，如无请配置，如有但仍无法支付请联系UC技术接口人。
                pInfo.setServerId(mServerId);
                //设置用户的游戏角色的ID，此为可选参数
                pInfo.setRoleId(String.valueOf(mRoleId));
                //设置用户的游戏角色名字，此为可选参数
                pInfo.setRoleName(mRoleName);
                //设置用户的游戏角色等级，此为可选参数
                //  pInfo.setGrade("12");
                //设置允许充值的金额，此为可选参数，默认为0。如果设置了此金额不为0，则表示只允许用户按指定金额充值；如果不指定金额或指定为0，则表示用户在充值时可以自由选择或输入希望充入的金额。
                pInfo.setAmount(mMoney);
                // 设置CP自有的订单号，此为可选参数
                pInfo.setTransactionNumCP(orderid);
                try {
                    UCGameSDK.defaultSDK().pay(mActivity, pInfo,mUCPayCallbackListener);
                } catch (UCCallbackListenerNullException e) {
                    //异常处理
                    mRechargeCallBack.rechargeFaile(e.getLocalizedMessage());
                }

            }else {
                Log.d(TAG, paramObject.optString("msg"));
                mRechargeCallBack.rechargeFaile(paramObject.optString("msg"));
            }

        }

        @Override
        public void callbackError(String error) {
            Log.d(TAG, error);
            mRechargeCallBack.rechargeFaile(error);

        }

    };

    UCCallbackListener<OrderInfo> mUCPayCallbackListener= new UCCallbackListener<OrderInfo>(){

        @Override
        public void callback(int statudcode, OrderInfo orderInfo) {
            if (statudcode == UCGameSDKStatusCode.NO_INIT) {
                //没有初始化就进行登录调用，需要游戏调用SDK初始化方法
                Toast.makeText(mActivity, "请您先登录", Toast.LENGTH_LONG).show();
            }
            if (statudcode == UCGameSDKStatusCode.SUCCESS){
                //成功充值
                mRechargeCallBack.rechargeSuccess(mUserModel);
                //                if (orderInfo != null) {
                //                    String ordered = orderInfo.getOrderId();//获取订单号
                //                    float amount = orderInfo.getOrderAmount();//获取订单金 额
                //                    int payWay = orderInfo.getPayWay();//获取充值类型，具体 可参考支付通道编码列表
                //                    String payWayName = orderInfo.getPayWayName();//充值类 型的中文名称
                //                }
            }
            if (statudcode == UCGameSDKStatusCode.PAY_USER_EXIT) {
                //用户退出充值界面。
                mRechargeCallBack.backKey("退出充值界面");
            }

        }


    };

    @Override
    public void exitSDK() {
        UCGameSDK.defaultSDK().exitSDK();
    }


    @Override
    public void logout(Activity activity, LogoutCallBack logoutCallBack) {
        this.mActivity=activity;
        this.mLogoutCallBack=logoutCallBack;
        try {
            UCGameSDK.defaultSDK().logout();
        } catch (UCCallbackListenerNullException e) {
            //未设置退出侦听器
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    UCCallbackListener<String> mLogoutNotify = new UCCallbackListener<String>() {
        @Override
        public void callback(int statuscode, String msg) {
            // 此处需要游戏客户端注销当前已经登录的游戏角色信息
            String s = "游戏接收到用户退出通知。" + msg + statuscode;
            Log.e("UCGameSDK", s);
            // 未成功初始化
            if (statuscode == UCGameSDKStatusCode.NO_INIT) {
                // 调用SDK初始化接口
                Toast.makeText(mActivity, "未成功初始化", Toast.LENGTH_LONG).show();
            }
            // 未登录成功
            if (statuscode == UCGameSDKStatusCode.NO_LOGIN) {
                // 调用SDK登录接口
                try {
                    UCGameSDK.defaultSDK().login(mActivity, loginCallbackListener);
                } catch (UCCallbackListenerNullException e) {
                    Log.e(TAG,"",e);
                }
            }
            // 退出账号成功
            if (statuscode == UCGameSDKStatusCode.SUCCESS) {
                // 执行销毁悬浮按钮接口
                UCGameSDK.defaultSDK().destoryFloatButton(mActivity);
                mLogoutCallBack.logoutCallBack();
            }
            // 退出账号失败
            if (statuscode == UCGameSDKStatusCode.FAIL) {
                // 调用SDK退出当前账号接口
                UCGameSDK.defaultSDK().exitSDK();
            }
        }

    };
    @Override
    public void setDBUG(boolean logDbug) {
    }

    @Override
    public void enterUserCenter(Activity activity, LogoutCallBack logoutCallBack) {
        this.mActivity=activity;
        this.mLogoutCallBack=logoutCallBack;
        try {
            UCGameSDK.defaultSDK().enterUserCenter(activity, new UCCallbackListener<String>() {
                @Override
                public void callback(int statucode, String dumpdata) {
                    if (statucode == UCGameSDKStatusCode.NO_INIT) {
                        Toast.makeText(mActivity, "未成功初始化", Toast.LENGTH_LONG).show();
                        //没有初始化，需要进行初始化操作
                    }else if (statucode == UCGameSDKStatusCode.NO_LOGIN) {
                        //没有登录，需要先登录
                        Toast.makeText(mActivity, "先登录", Toast.LENGTH_LONG).show();
                    }else if (statucode == UCGameSDKStatusCode.SUCCESS) {
                        //用户管理界面正常关闭，返回游戏的界面逻辑
                    }
                }
            });
        } catch (UCCallbackListenerNullException e) {
            //处理异常
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void sendGameStatics(Activity activity, Integer serverId, String serverName, Integer roleId, String roleName, String level) {
        UCGameSDK.defaultSDK().notifyZone(serverName, String.valueOf(roleId), roleName);
        //登录游戏角色成功后调用此段
        try {
            JSONObject jsonExData = new JSONObject();
            jsonExData.put("roleId", roleId);
            jsonExData.put("roleName", roleName);
            jsonExData.put("roleLevel", level);
            jsonExData.put("zoneId", serverId);
            jsonExData.put("zoneName", serverName);
            UCGameSDK.defaultSDK().submitExtendData("loginGameRole", jsonExData);
        } catch (Exception e) {
            //处理异常
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void enterBBS(Activity activity) {
        this.mActivity=activity;
        try {
            UCGameSDK.defaultSDK().enterUserCenter(activity, new UCCallbackListener<String>() {
                @Override
                public void callback(int statucode, String dumpdata) {
                    if (statucode == UCGameSDKStatusCode.NO_INIT) {
                        Toast.makeText(mActivity, "未成功初始化", Toast.LENGTH_LONG).show();
                        //没有初始化，需要进行初始化操作
                    }else if (statucode == UCGameSDKStatusCode.NO_LOGIN) {
                        //没有登录，需要先登录
                        Toast.makeText(mActivity, "先登录", Toast.LENGTH_LONG).show();
                    }else if (statucode == UCGameSDKStatusCode.SUCCESS) {
                        //用户管理界面正常关闭，返回游戏的界面逻辑
                    }
                }
            });
        } catch (UCCallbackListenerNullException e) {
            //处理异常
            Log.d(TAG, e.getLocalizedMessage());
        }
    }

    @Override
    public void creatFloatButton(Activity activity, boolean showlasttime, int align, float position) {
        try {
            //创建悬浮按钮
            UCGameSDK.defaultSDK().createFloatButton(activity,
                            new UCCallbackListener<String>() {
                @Override
                public void callback(int statuscode, String data) {
                    if(statuscode == UCGameSDKStatusCode.SDK_OPEN){
                        //将要打开SDK界面
                    }else if(statuscode == UCGameSDKStatusCode.SDK_CLOSE){
                        //将要关闭SDK界面，返回游戏画面
                    }
                }
            });
            //显示悬浮图标
            UCGameSDK.defaultSDK().showFloatButton(activity, 100, 50, true);
        } catch (UCCallbackListenerNullException e) {
            Log.d(TAG, e.getLocalizedMessage());
            //SDK界面消息侦听器为空
        } catch (UCFloatButtonCreateException e) {
            Log.d(TAG, e.getLocalizedMessage());
        }

    }

    @Override
    public void onResume(Activity activity) {
    }

    @Override
    public void onPause(Activity activity) {
    }

    @Override
    public void onStop(Activity activity) {
    }

    @Override
    public void onDestroy(Activity activity) {
        UCGameSDK.defaultSDK().destoryFloatButton(activity);
    }

    /**
     * @return void 返回类型
     * @Title: showProgressDialog(设置进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:36
     */
    protected void showProgressDialog(Activity activity) {
        if (! activity.isFinishing()) {
            try {
                this.mProgressDialog = new ProgressDialog(activity);// 实例化
                // 设置ProgressDialog 的进度条style
                this.mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);// 设置进度条风格，风格为圆形，旋转的
                this.mProgressDialog.setTitle("登陆");
                this.mProgressDialog.setMessage("加载中...");// 设置ProgressDialog 提示信息
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setIndeterminate(false);
                // 设置ProgressDialog 的进度条是否不明确
                this.mProgressDialog.setCancelable(false);
                this.mProgressDialog.setCanceledOnTouchOutside(false);
                this.mProgressDialog.show();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    /**
     * @return void 返回类型
     * @Title: closeProgressDialog(关闭进度条)
     * @author xiaoming.yuan
     * @data 2013-7-12 下午10:09:30
     */
    protected void closeProgressDialog() {
        if (this.mProgressDialog != null && this.mProgressDialog.isShowing())
            this.mProgressDialog.dismiss();
    }

}
