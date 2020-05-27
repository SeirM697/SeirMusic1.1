package com.example.seirmusic.ui;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.example.seirmusic.R;
import com.example.seirmusic.dao.MyStarsMyListDataBase;
import com.example.seirmusic.impl.MyStarsMusicListDao;
import com.example.seirmusic.manager.MusicManager;
import com.example.seirmusic.model.Music;
import com.example.seirmusic.model.MyStrasMusicList;
import com.example.seirmusic.utils.ApplicationTrans;
import com.example.seirmusic.utils.TimeUtils;
import com.example.seirmusic.utils.UtilBitmap;
import com.google.android.exoplayer2.Player;
import com.hw.lrcviewlib.LrcDataBuilder;
import com.hw.lrcviewlib.LrcRow;
import com.hw.lrcviewlib.LrcView;
import com.pili.pldroid.player.PLOnCompletionListener;

import java.io.File;
import java.util.List;
import java.util.Random;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * Copyright (C)
 * <p>
 * FileName: PlayerActivity
 * <p>
 * Author: SeirM
 * <p>
 * Date: 2020/4/14 15:15
 * <p>
 * Description: 播放
 */
public class PlayerActivity extends AppCompatActivity implements View.OnClickListener {


    private static final String TAG = "player";
    private LinearLayout llContent;
    private TextView tv_music_name;
    private TextView tvMusicSinger;
    private CircleImageView profileImage;
    private ImageView ivPlayModel;
    private TextView tvCurrentTime;
    private SeekBar sProgress;
    private TextView tvAllTime;
    private ImageView ivPrev;
    private ImageView ivControl;
    private ImageView ivNext;

    private ImageView img_stars_music;


    private int position = -1;
    private ObjectAnimator circleRotateAnim;
    private LrcView mLrcView;
    private Random random;


    private String userName;
    private String path;
    private String musicName;
    private String lrcpath;
    private MyStarsMyListDataBase myStarsMyListDataBase;
    private MyStarsMusicListDao myStarsMusicListDao;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        initSystemUI();
        intView();
    }

    //初始化状态栏
    private void initSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void intView() {
        mLrcView = findViewById(R.id.mLrcView);


        llContent = (LinearLayout) findViewById(R.id.ll_content);
        tv_music_name = (TextView) findViewById(R.id.tv_music_name1);
        tvMusicSinger = (TextView) findViewById(R.id.tv_music_singer);
        profileImage = (CircleImageView) findViewById(R.id.profile_image);
        ivPlayModel = (ImageView) findViewById(R.id.iv_play_model);
        tvCurrentTime = (TextView) findViewById(R.id.tv_current_time);
        sProgress = (SeekBar) findViewById(R.id.s_progress);
        tvAllTime = (TextView) findViewById(R.id.tv_all_time);
        ivPrev = (ImageView) findViewById(R.id.iv_prev);
        ivControl = (ImageView) findViewById(R.id.iv_control);
        ivNext = (ImageView) findViewById(R.id.iv_next);
        img_stars_music = findViewById(R.id.img_stars_music);


        position = getIntent().getIntExtra("position", -1);


        ivPrev.setOnClickListener(this);
        ivNext.setOnClickListener(this);
        ivControl.setOnClickListener(this);
        profileImage.setOnClickListener(this);
        mLrcView.setOnClickListener(this);
        ivPlayModel.setOnClickListener(this);
        img_stars_music.setOnClickListener(this);

        /**
         * 图片旋转
         */
        circleRotateAnim = ObjectAnimator.ofFloat(profileImage, "rotation", 0.0f, 360.0f);
        //8秒一圈
        circleRotateAnim.setDuration(8000);
        //循环次数
        circleRotateAnim.setRepeatCount(Animation.INFINITE);
        //持续
        circleRotateAnim.setRepeatMode(ObjectAnimator.RESTART);
        //差值器默认
        circleRotateAnim.setInterpolator(new LinearInterpolator());

        MusicManager.getInstance().setOnMusicProgress(new MusicManager.OnMusicProgressListener() {
            @Override
            public void onProgress(long Progress, long all) {
                sProgress.setProgress((int) Progress);
                //设置进度最大值
                sProgress.setMax((int) all);
                //1s后通知 进行刷新
                tvCurrentTime.setText(TimeUtils.formatDuring(Progress));
                //进度条总时长
                tvAllTime.setText(TimeUtils.formatDuring(all));
                mLrcView.smoothScrollToTime(Progress);

            }
        });
        MusicManager.getInstance().setOnCompletionListener(new PLOnCompletionListener() {
            @Override
            public void onCompletion() {
                musicPlay(false);
            }
        });

        //musicPlay(false);
        sProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                //进度条播放
                MusicManager.getInstance().setCurrentPosition(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        if (position != -1) {
            //获取相关信息
//            if (MusicManager.getInstance().isBindService()) {
//                Music music = MusicManager.getInstance().getClientImpl().getMusicList().get(position);
//                tv_music_name.setText(music.getMusicName());
//
//                Log.d(TAG, "intView: 姓名" + music.getMusicName());
            Log.i("shangfanganniu", "intView: position  ");
            startMusic(position);
        }

    }

    //播放音乐
    private void startMusic(int position) {
        Music music = MusicManager.getInstance().getClientImpl().getMusicList().get(position);
        //播放  播放实际上是播放的路径
        MusicManager.getInstance().startPlay(music.getPath());
        path = music.getPath();
        musicName = music.getMusicName();
        lrcpath = music.getLrcPath();
        //进度条
        sProgress.setProgress(0);
        sProgress.setMax((int) MusicManager.getInstance().getDuration());


        //中间暂停按钮
        ivControl.setBackgroundResource(R.drawable.ic_pause);
        Log.i("startmusic", "startMusic:这个房吗能进行吗?");
        tv_music_name.setText(music.getMusicName());
        tvMusicSinger.setText(music.getMusicSinger());
        circleRotateAnim.start();


        if (music.getMusicCover() != null) {
            setMusicCover(music.getMusicCover());
        } else {
            setMusicCover(BitmapFactory.decodeResource(getResources(), R.drawable.iv_music_test_cover));
        }
        loadLrc(music);
    }

    //歌词解析类
    private void loadLrc(Music music) {
        if (!TextUtils.isEmpty(music.getLrcPath())) {
            List<LrcRow> lrcRows = new LrcDataBuilder().Build(new File(music.getLrcPath()));
            mLrcView.getLrcSetting()
                    .setTimeTextSize(40)//时间字体大小
                    .setSelectLineColor(Color.parseColor("#ffffff"))//选中线颜色
                    .setSelectLineTextSize(25)//选中线大小
                    .setHeightRowColor(Color.parseColor("#aaffffff"))//高亮字体颜色
                    .setNormalRowTextSize(40)//正常行字体大小
                    .setHeightLightRowTextSize(40)//高亮行字体大小
                    .setTrySelectRowTextSize(40)//尝试选中行字体大小
                    .setTimeTextColor(Color.parseColor("#ffffff"))//时间字体颜色
                    .setTrySelectRowColor(Color.parseColor("#55ffffff"));//尝试选中字体颜色

            mLrcView.commitLrcSettings();
            mLrcView.setLrcData(lrcRows);
        }

    }

    //设置专辑
    private void setMusicCover(Bitmap musicCover) {

        profileImage.setImageBitmap(musicCover);
        Bitmap mBitmap = UtilBitmap.blurImageView(this, profileImage, 20);
        if (mBitmap != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                llContent.setBackground(new BitmapDrawable(getResources(), mBitmap));
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_prev:
                musicPlay(true);
                break;
            case R.id.iv_next:
                musicPlay(false);
                break;

            case R.id.profile_image:
                //歌词
                mLrcView.setVisibility(View.VISIBLE);
                profileImage.setVisibility(View.GONE);
                break;
            case R.id.mLrcView:
                mLrcView.setVisibility(View.GONE);
                profileImage.setVisibility(View.VISIBLE);
                break;
            case R.id.iv_control:
                if (MusicManager.getInstance().MEDIA_STATUS == MusicManager.getInstance().MEDIA_STATUS_PLAY) {
                    MusicManager.getInstance().pausePlay();
                    ivControl.setBackgroundResource(R.drawable.ic_play);
                    circleRotateAnim.pause();
                } else if (MusicManager.getInstance().MEDIA_STATUS == MusicManager.getInstance().MEDIA_STATUS_PAUSE) {
                    MusicManager.getInstance().continuePlay();
                    ivControl.setBackgroundResource(R.drawable.ic_pause);
                    circleRotateAnim.start();
                }
                break;

            case R.id.iv_play_model:
                //顺序 - 随机 -单曲
                if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_ORDER) {
                    MusicManager.MEDIA_PLAY_MODE = MusicManager.MEDIA_PLAY_MODE_RANDOM;
                    Toast.makeText(this, "随机播放", Toast.LENGTH_SHORT).show();
                    ivPlayModel.setImageResource(R.drawable.select_random);
                } else if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_RANDOM) {
                    MusicManager.MEDIA_PLAY_MODE = MusicManager.MEDIA_PLAY_MODE_SINGLE;
                    Toast.makeText(this, "单曲循环", Toast.LENGTH_SHORT).show();
                    ivPlayModel.setImageResource(R.drawable.select_single);
                } else if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_SINGLE) {
                    MusicManager.MEDIA_PLAY_MODE = MusicManager.MEDIA_PLAY_MODE_ORDER;
                    Toast.makeText(this, "顺序播放", Toast.LENGTH_SHORT).show();
                    ivPlayModel.setImageResource(R.drawable.select_repeat);
                }
                break;

            case R.id.img_stars_music:
                //跳转到我的收藏
                userName = ((ApplicationTrans) getApplication()).getValue();
                if (userName == null) {
                    Toast.makeText(this, "未登录，请先登录", Toast.LENGTH_SHORT).show();
                    break;
                } else {
                    userName = getIntent().getStringExtra("userName");
                    MyStrasMusicList myStrasMusicList = new MyStrasMusicList(userName, musicName, path, lrcpath);
                    Log.i("stars", "onClick: " + myStrasMusicList.toString());
                    myStarsMyListDataBase = Room.databaseBuilder(PlayerActivity.this, MyStarsMyListDataBase.class,
                            "mystarsmusiclist_database").allowMainThreadQueries().build();
                    myStarsMusicListDao = myStarsMyListDataBase.getMyStarsMusicListDao();
                    myStarsMusicListDao.insertMusi(myStrasMusicList);
                    Toast.makeText(this, "收藏成功", Toast.LENGTH_SHORT).show();
                    break;
                }

        }
    }

    private void musicPlay(boolean isPrev) {
        //判断播放模式
        if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_ORDER) {
            if (isPrev) {
                position = position - 1;
                if (position >= 0) {
                    startMusic(position);
                }
            } else {
                position = position + 1;
                if (position <= (MusicManager.getInstance().getClientImpl().getMusicList().size() - 1)) {
                    startMusic(position);
                }
            }
        } else if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_SINGLE) {
            startMusic(position);
        } else if (MusicManager.MEDIA_PLAY_MODE == MusicManager.MEDIA_PLAY_MODE_RANDOM) {
            if (random != null) {
                int _n = random.nextInt(MusicManager.getInstance().getClientImpl().getMusicList().size());
                startMusic(_n);
            }
        }
    }
}

