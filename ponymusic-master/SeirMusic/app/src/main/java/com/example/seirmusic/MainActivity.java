package com.example.seirmusic;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;


import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.seirmusic.adapter.MainListAdapter;
import com.example.seirmusic.dao.MusicListDataBase;
import com.example.seirmusic.impl.MusicListDao;
import com.example.seirmusic.impl.OnMusicService;
import com.example.seirmusic.manager.MusicManager;
import com.example.seirmusic.model.Music;
import com.example.seirmusic.model.MusicList;
import com.example.seirmusic.ui.DataBaseMusic;
import com.example.seirmusic.ui.LoginActivityMian;
import com.example.seirmusic.ui.MyMusicActivity;
import com.example.seirmusic.ui.PlayerActivity;
import com.example.seirmusic.ui.loginActivity;
import com.example.seirmusic.utils.ApplicationTrans;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    //数据源
    private List<Music> mMusicList = new ArrayList<>();
    private RecyclerView mMainListView;

    private MainListAdapter mMainListAdapter;

    private ProgressDialog mProgressDialog;

    private String s;
    //------------------------------------------------------------------------
    private MusicListDao musicListDao;
    private MusicListDataBase musicListDataBase;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();

    }

    private void initView() {
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage(getString(R.string.text_main_re_scan_muisc));
        //屏幕外点击无效
        mProgressDialog.setCancelable(false);

        mMainListView = findViewById(R.id.mMusicListView);
        /*
        ListView:列表  GridView：宫格
        RecyclerView：管理view的
         */

        mMainListView.setLayoutManager(new LinearLayoutManager(this));
        //适配器

        mMainListAdapter = new MainListAdapter(this, mMusicList);
        mMainListView.setAdapter(mMainListAdapter);

        Intent intent = getIntent();
        s = intent.getStringExtra("nameUser");
        Log.i("ismain", "onOptionsItemSelected:   我的名字 " + s);
        mMainListAdapter.setName(s);

        //判断当前权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int isCheck = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
//            Toast.makeText(this,"ischeck:"+isCheck,Toast.LENGTH_SHORT).show();
            if (isCheck != 0) {
                //请求权限
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 10001);
            } else {
                bindService();
            }

        } else {
            bindService();
        }

    }

    private void bindService() {


        MusicManager.getInstance().bindMusicService(this, new OnMusicService() {
            @Override
            public void bindSucceed() {
                Log.d(TAG, "绑定成功");
                mMusicList.addAll(MusicManager.getInstance().getClientImpl().getMusicList());
                if (mMusicList.size() <= 0) {
                    //没有数据
                    MusicManager.getInstance().getClientImpl().startScanMusic();
                } else {
                    //得到数据，刷新数据源
                    mMainListAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void scanMusicStart() {

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mProgressDialog.show();
                    }
                });
            }

            @Override
            public void scanMusicStop() {
                mProgressDialog.dismiss();
                Log.d(TAG, "搜索中");
                mMusicList.addAll(MusicManager.getInstance().getClientImpl().getMusicList());
                if (mMusicList.size() <= 0) {
                    //没有数据,以搜索过一遍
                    Toast.makeText(MainActivity.this, getString(R.string.text_main_null_muisc), Toast.LENGTH_SHORT).show();
                } else {
                    //得到数据，刷新数据源  插入数据库表
                    //------------------------------------------
                    for (Music m : mMusicList) {
                        MusicList musicList = new MusicList(m.getMusicName(), m.getMusicSinger(), m.getAlbum(), m.getPath(), m.getLrcPath());
                        musicListDataBase = Room.databaseBuilder(MainActivity.this, MusicListDataBase.class, "musiclist_database").allowMainThreadQueries().build();
                        musicListDao = musicListDataBase.getMusicListDao();
                        musicListDao.insertMusic(musicList);
                    }

                    mMainListAdapter.notifyDataSetChanged();
                }


            }

        });
        Log.d(TAG, MusicManager.getInstance().toString());
        if (!MusicManager.getInstance().isBindService()) {
            Log.d(TAG, "onCreate: 完成");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        MusicManager.getInstance().unBindService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.mian_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //顶部栏 导航
        switch (item.getItemId()) {
            case R.id.menu_scan:
                //搜索音乐
                if (!MusicManager.getInstance().getClientImpl().isScan()) {
                    if (mMusicList.size() > 0) {
                        mMusicList.clear();
                    }
                    MusicManager.getInstance().getClientImpl().startScanMusic();
                }
                break;

            case R.id.menu_play:
                //跳转到音乐播放界面
                Log.d(TAG, "onOptionsItemSelected: 传过来姓名没有？" + s);
                if (s != null) {
                    Intent intent2 = new Intent(this, MyMusicActivity.class);
                    Log.i("zhuyetiaozhuan", "onOptionsItemSelected: ");
                    intent2.putExtra("nameUser", s);
                    startActivity(intent2);
                    finish();
                    break;
                }

            case R.id.menu_login:
                Intent intent1 = new Intent(this, loginActivity.class);
                startActivity(intent1);
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
