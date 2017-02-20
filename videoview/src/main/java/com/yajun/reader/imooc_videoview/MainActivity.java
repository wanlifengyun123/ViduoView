package com.yajun.reader.imooc_videoview;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends Activity {

    private RelativeLayout mVideoLayout;
    private CustomVideoView mVideoView;
    private SeekBar mSbPlay;

    private LinearLayout mControllerLayout;
    private ImageView mIvPause;
    private TextView mTvCurrentTime,mTvTotalTime;
    private SeekBar mSbVoice;
    private ImageView mIvVolume;
    private ImageView mIvScreen;

    private static final int UPDATE_UI = 1;
    private int mScreenWidth,mScreenHeight;

    /**
     * 横屏时，显示音量控件，控制声音大小
     */
    private AudioManager mAudioManager;
    private boolean isFullScreen = false;

    // 手势是否合法
    private boolean isAdJust = false;
    // 仿误触,临界值
    private int threshould= 54;
    private float lastX = 0;
    private float lastY = 0;

    /** 刷新UI */
    private Handler mUIHander = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == UPDATE_UI){
                //获取当前播放的进度值
                int mCurrentPosition = mVideoView.getCurrentPosition();
                // 获取总的视频时间
                int mTotalTime = mVideoView.getDuration();

                updateTextTimeWithFormat(mTvCurrentTime,mCurrentPosition);
                updateTextTimeWithFormat(mTvTotalTime,mTotalTime);
                // 设置总的长度
                mSbPlay.setMax(mTotalTime);
                mSbPlay.setProgress(mCurrentPosition);
                // 自动刷新时间
                mUIHander.sendEmptyMessageDelayed(UPDATE_UI,500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        initUI();
        setPlayerEvent();
        // 网络视频播放
        mVideoView.setVideoURI(Uri.parse("http://v1.mukewang.com/2bb22bcf-8b7d-4ca7-b1d7-47b6f06cd60e/L.mp4"));
        mVideoView.start();
        mUIHander.sendEmptyMessage(UPDATE_UI);



//        /**
//         * MediaController 控制视频播放
//         * 控制播放的停止/暂停
//         */
//        MediaController mController = new MediaController(this);
//        // 设置VideoView 和 MediaController的关联
//        mVideoView.setMediaController(mController);
//        // 设置MediaController and VideoView的关联
//        mController.setMediaPlayer(mVideoView);
    }

    private void initUI() {

        mVideoLayout = (RelativeLayout) findViewById(R.id.mVideoLayout);

        mControllerLayout = (LinearLayout) findViewById(R.id.controller_layout);
        mVideoView  = (CustomVideoView) findViewById(R.id.mVideoView);
        mSbPlay = (SeekBar) findViewById(R.id.play_seek);
        mIvPause = (ImageView) findViewById(R.id.pause_img);
        mTvCurrentTime = (TextView) findViewById(R.id.time_current_tv);
        mTvTotalTime = (TextView) findViewById(R.id.time_total_tv);
        mSbVoice = (SeekBar) findViewById(R.id.volume_seek);
        mIvVolume = (ImageView) findViewById(R.id.volume_img);
        mIvScreen = (ImageView) findViewById(R.id.screen_img);

        if(mVideoView.isPlaying()){
            mIvPause.setImageResource(R.drawable.mediacontroller_play);
        }else {
            mIvPause.setImageResource(R.drawable.mediacontroller_pause);
        }

        mScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mScreenHeight = getResources().getDisplayMetrics().heightPixels;

        // 获取设备最大音量
        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取设备当前音量
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mSbVoice.setMax(streamMaxVolume);
        mSbVoice.setProgress(streamVolume);

    }

    /**
     * 设置VideoView的宽高
     */
    private void setVideoViewScale(int width,int height){
        ViewGroup.LayoutParams layoutParams = mVideoView.getLayoutParams();
        layoutParams.width = width;
        layoutParams.height = height;
        mVideoView.setLayoutParams(layoutParams);

        ViewGroup.LayoutParams layoutParams1 = mVideoLayout.getLayoutParams();
        layoutParams1.width = width;
        layoutParams1.height = height;
        mVideoLayout.setLayoutParams(layoutParams1);

    }

    private void setPlayerEvent() {
        // 控制视频播放暂停
        mIvPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mVideoView.isPlaying()){
                    mIvPause.setImageResource(R.drawable.mediacontroller_play);
                    // 暂停播放
                    mVideoView.pause();
                    mUIHander.removeMessages(UPDATE_UI);
                }else {
                    mIvPause.setImageResource(R.drawable.mediacontroller_pause);
                    //开始播放
                    mVideoView.start();
                    mUIHander.sendEmptyMessage(UPDATE_UI);
                }
            }
        });
        // 控制视频进度改变监听
        mSbPlay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 更新当前拖动时的 时间显示
                updateTextTimeWithFormat(mTvCurrentTime,progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // 开始拖动时，停止UI刷新
                mUIHander.removeMessages(UPDATE_UI);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // 停止推动时，得到seekBar 的进度
                int progress = seekBar.getProgress();
                // 设置视频的播放进度，使和SeekBar进度一致
                mVideoView.seekTo(progress);
                // 重新刷新
                mUIHander.sendEmptyMessage(UPDATE_UI);
            }
        });
        // 音量控制
        mSbVoice.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // 设置设备当前音量
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,progress,0);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        // 横竖屏切换
        mIvScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isFullScreen){
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }else {
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
        });
        // VideoView 的手势事件，音量和亮度
        mVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();
                Log.d("VidioView","x:" + x + ",y:" + y);
                switch (event.getAction()){
                    case MotionEvent.ACTION_DOWN:
                        lastX = x;
                        lastY = y;
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d("VidioView","lastX:" + lastX + ",lastY:" + lastY);
                        float detlaX = lastX - x ;
                        float detlaY = lastY - y ;
                        float absMoveX = Math.abs(detlaX);
                        float absMoveY = Math.abs(detlaY);
                        Log.d("VidioView","detlaX:" + detlaX + ",detlaY:" + detlaY);
                        // 手势合法性验证
                        if(absMoveX > threshould && absMoveY > threshould){
                            if(absMoveX < absMoveY){
                                isAdJust = true;
                            }else {
                                isAdJust = false;
                            }
                        } else if(absMoveX < threshould && absMoveY > threshould){
                            isAdJust = true;
                        } else if(absMoveX > threshould && absMoveY < threshould){
                            isAdJust = false;
                        }
                        Log.d("VideoView","手势合法性验证 : " + isAdJust);
                        if(isAdJust){
                            // 合法的情况下 ，判断是调节音量还是亮度
                            if(x < mScreenWidth / 2){
                                // 调节亮度
                                if(detlaY > 0 ){
                                    // down
                                }else {
                                    // up
                                }
                                changeBrightess(detlaY);
                            } else {
                                // 调节声音
                                if(detlaY > 0 ){
                                    // down
                                }else {
                                    // up
                                }
                                changeVolume(detlaY);
                            }
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        break;
                }
                return true;
            }
        });
    }

    private void changeVolume(float detlaY){
        // 获取设备最大音量
        int streamMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 获取设备当前音量
        int streamVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int index = (int) (detlaY / mScreenHeight * streamMaxVolume );
        int volume = Math.max(streamVolume + index,0);
        // 设置设备当前音量
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC,volume,0);
        mSbVoice.setProgress(volume);
    }

    private void changeBrightess(float detlaY){
        WindowManager.LayoutParams attributes = getWindow().getAttributes();
        float screenBrightness = attributes.screenBrightness;
        float index = detlaY / mScreenHeight;
        screenBrightness += index;
        if(screenBrightness > 1.0f){
            screenBrightness = 1.0f;
        }
        if(screenBrightness < 0.01f){
            screenBrightness = 0.01f;
        }
        attributes.screenBrightness = screenBrightness;
        getWindow().setAttributes(attributes);

    }

    /**
     * 更新当前进度的时间 并格式化
     * @param textView
     * @param millsecond
     */
    private void updateTextTimeWithFormat(TextView textView,int millsecond){
        int second = millsecond/1000;
        int hh = second / 3600;
        int mm = (second % 3600) / 60;
        int ss = second % 60;
        String str;
        if(hh != 0 ){
            str = String.format("%02d:%02d:%02d",hh,mm,ss);
        }else {
            str = String.format("00:%02d:%02d",mm,ss);
        }
        textView.setText(str);
    }

    /**
     * 监听屏幕方向改变
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // 当屏幕的方向为横屏的时候
        if(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE){
            // 拉伸屏幕到屏幕大小
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);
            // 横屏显示音量调节控件
            mIvVolume.setVisibility(View.VISIBLE);
            mSbVoice.setVisibility(View.VISIBLE);

            isFullScreen = true;
            // 引导识别半屏还是全屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else {
            // 当屏幕的方向为竖屏的时候 height = 240dp
            setVideoViewScale(ViewGroup.LayoutParams.MATCH_PARENT,ScreenUtil.dp2px(this,240));
            mIvVolume.setVisibility(View.GONE);
            mSbVoice.setVisibility(View.GONE);

            isFullScreen = false;
            // 引导识别半屏还是全屏
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mUIHander.removeMessages(UPDATE_UI);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
